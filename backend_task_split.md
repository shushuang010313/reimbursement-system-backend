# 费控云差旅报销单后端开发任务拆分规划（代码细节版）

基于团队配置（负责人您 + 1名组员），为了让开发边界清晰、减少 Git 冲突，这里从**具体的包、类、方法及核心逻辑**对任务进行深度拆解。

---

## 👨‍💻 开发者 A（您，后端负责人）：主单、分摊、底层拦截与审批对接

**职责定位：** 掌控整个报销单的生命周期管理，处理复杂的事务协调、并发控制以及对接第三方系统，同时维护底层的异常和日志规范。

### 需要编写的类
* **Controller**: `ReimMainController`, `ReimSplitController`
* **Service**: `IReimMainService` (及 Impl), `IReimSplitService` (及 Impl)
* **Mapper**: `ReimMainMapper`, `ReimSplitMapper`
* **Aspect/Job**: `ReimLogAspect`, `BpmCompensationJob`

### 具体需要完成的代码逻辑

1. **主单 CRUD 与复杂分页逻辑 (`ReimMainServiceImpl`)**
   * **分页查询 (`queryPageList(ReimPageQueryDTO dto)`)**：
     * 需在 `ReimMainMapper.xml` 编写动态 SQL（`<where>` 标签），处理 `reimNo`, `reimTitle`, `businessTripReason` 的模糊查询（`LIKE`），以及 `companyId`, `departmentId` 等的精确匹配。
     * **注意**：金额字段返回时需保留两位小数，返回 `Page<ReimMainVO>`。
   * **保存草稿 (`saveReimMain(ReimSaveDTO dto)`)**：
     * 自动生成报销单号（规则：`REIM` + 年月日 + 流水号）。
     * **并发控制**：在 MyBatis-Plus 中使用 `@Version` 注解字段处理更新时的乐观锁，捕获到更新失败需抛出友好提示。
   * **单据提交 (`submitReim(String reimId)`)**：
     * **分布式锁防重**：使用 `RedissonClient` 加锁 `fk:reim:lock:submit:{reimId}` 防止用户连点。
     * 数据合法性兜底校验（是否有行程、是否有金额等），不满足则抛出 `REIM_005`。
     * 将 `reimStatus` 变更为 `1 (已完成)`。

2. **费用分摊联动计算 (`ReimSplitServiceImpl`)**
   * **分摊比例联动计算 (`calculateSplitRatio(String reimId, List<ReimSplitDTO> list)`)**：
     * **核心算法**：前端修改第二行及以后的比例时，后端也必须做双重校验。计算第一行的比例 = `100% - Σ(第2+行分摊比例)`。
     * 校验 `Σ(分摊比例)` 不可大于 100%，否则抛出 `BusinessException(ErrorCodeEnum.REIM_004)`。
     * 使用 `BigDecimal` 进行乘法运算（计算具体金额），注意精度丢失问题，需使用 `RoundingMode.HALF_UP`。

3. **基础设施与工作流对接**
   * **异动日志切面 (`ReimLogAspect`)**：
     * 使用 Spring AOP (`@Around` 或 `@AfterReturning`) 拦截报销单状态变更方法。
     * 对比修改前后的字段值，生成日志明细并存入 `fk_reim_log` 表，敏感字段需打码。
   * **MQ 异步与补偿 Job (`BpmCompensationJob`)**：
     * 提交单据后，向 MQ Topic `reim.submit.topic` 发送消息触发审批流。
     * 编写 XXL-Job 定时任务，扫描表中 `status=待推送` 且 `update_time < NOW() - 5分钟` 的遗漏数据进行重新推送，重试超 3 次报警。

---

## 👨‍💻 开发者 B（您的组员）：行程、补助明细及补助日历计算引擎

**职责定位：** 负责由于员工出差行程引发的一系列复杂计算和明细表维护。这部分业务逻辑高度内聚，数据量大，需要注重性能。

### 需要编写的类
* **Controller**: `ReimTripController`, `ReimCalendarController`
* **Service**: `IReimTripService` (及 Impl), `IReimSubsidyService` (及 Impl), `IReimCalendarService` (及 Impl)
* **Component/Engine**: `SubsidyCalcEngine` (计算引擎组件)
* **Mapper**: `ReimTripMapper`, `ReimSubsidyMapper`, `ReimCalendarMapper`

### 具体需要完成的代码逻辑

1. **补录行程管理与级联 (`ReimTripServiceImpl`)**
   * **新增/修改行程 (`saveTrip(ReimTripDTO tripDTO)`)**：
     * **冲突拦截算法**：必须编写代码去查库，判断该出行人(`travelerId`) 在 `[departureDate, arriveDate]` 区间内，是否在系统里已有其他行程记录，如果有，立刻抛出 `REIM_003` (行程时间重复) 异常。
     * **级联触发动作**：在同一个 `@Transactional` 中，保存行程后，立刻调用 `IReimCalendarService` 去生成针对这几天的补助日历数据。
   * **删除行程 (`deleteTrip(String tripId)`)**：
     * 需级联删除相关的 `fk_reim_subsidy` 和 `fk_reim_calendar` 数据。

2. **补助计算核心引擎 (`SubsidyCalcEngine`)**
   * **城市等级匹配 (`matchCityLevel(String cityId)`)**：
     * 连接 Redis，从 `fk:reim:city:list` 中拉取城市数据（如果 Redis 没有则查库并塞入 Redis）。
     * 获取城市表中的 `csfllx` 字段，判断是 1（一线）、2（二线）还是 3（三线）。
   * **标准金额计算 (`calculateStandardAmount(String cityLevel, int days)`)**：
     * 编写策略代码或配置类计算：一线城市餐补100/天，二线80/天，三线50/天。交通和通讯统一为 40/天。

3. **补助日历生成与金额维护 (`ReimCalendarServiceImpl`)**
   * **日历批量生成算法 (`generateCalendar(...)`)**：
     * 核心逻辑：`tripDays = arriveDate - departureDate + 1`。循环天数，生成每一天的明细对象 `ReimCalendarDO`。
     * 判断每一天属于星期几（使用 `LocalDate.getDayOfWeek()`）。
     * **并发优化要求（考核点）**：如果用户一口气添加了非常长周期（如 30 天 * 10 个人）的行程，要求组员使用 `CompletableFuture.supplyAsync()` 将数据分组并行组装，最后 `saveBatch` 入库。
   * **补助金额勾选反算联动 (`updateCalendarStatus(List<CalendarDTO> dtoList)`)**：
     * 前端用户在页面上取消勾选某一天的某项补助时（例如不要周三的餐补），更新单日历条目的金额为 0。
     * **金额向上累加汇总**：触发统计该 `tripId` 下所有的实际补助总和 -> 赋值给 `fk_reim_subsidy` 表 -> 然后调用由 A 同学（您）提供的 `IReimMainService.updateTotalAmount()` 接口，去刷新主单总金额。

---

## 🤝 组内代码联调约定（防坑指南）

为了避免两人同时开发时踩坑，请作为组长明确以下约定：
1. **统一异常外抛**：组员在校验行程重叠、计算金额为负数等不合规情况时，一律使用 `throw new BusinessException(ErrorCodeEnum.XXX)` 抛出，由您的 `GlobalExceptionHandler` 统一收口给前端，**禁止组员在 Service 层直接 try-catch 并返回普通 String**。
2. **核心调用的事务边界**：比如“组员更新补助日历金额，最终会导致主单总金额改变”。这种跨 Service 的调用，**入口方法（通常是 Controller 层直接调用的那个 Service 方法）必须标注 `@Transactional(rollbackFor = Exception.class)`**，这样不管是您的主表报错，还是他的子表报错，都能一并回滚。
