# 报销主单与费用分摊功能实现计划 (feat-main-split)

本项目当前已成功切换到本地 `feat-main-split` 特性分支。根据 `backend_task_split.md` 中的要求，本计划严格对齐了开发者 A（您）负责的主单、分摊、底层拦截与审批对接逻辑。

---

## 📌 对齐后的开发任务

### 1. 主单 CRUD 与复杂分页逻辑 (`ReimMainServiceImpl`)
*   **分页查询 (`queryPageList`)**：
    *   将在 `ReimMainMapper.xml` 编写动态 SQL（`<where>` 标签）。
    *   **模糊查询（LIKE）**：`reimNo`, `reimTitle`, `businessTripReason`。
    *   **精确匹配**：`companyId`, `departmentId`。
    *   **要求**：金额字段返回时需保留两位小数，返回 `Page<ReimMainVO>`。
*   **保存草稿 (`saveReimMain`)**：
    *   **单号生成**：自动生成（规则：`REIM` + 年月日 + 流水号）。
    *   **并发控制**：基于 MyBatis-Plus `@Version` 字段处理乐观锁，更新失败抛出友好提示。
*   **单据提交 (`submitReim`)**：
    *   **分布式锁防重**：使用 `RedissonClient` 加锁 `fk:reim:lock:submit:{reimId}` 防止连点。
    *   **合法性校验**：必须有行程、有金额等，不满足抛出 `REIM_005`。
    *   **状态流转与 MQ**：状态变为 `1 (已完成)`，并向 MQ Topic `reim.submit.topic` 发送消息触发审批流。

### 2. 费用分摊联动计算 (`ReimSplitServiceImpl`)
*   **分摊比例联动计算 (`calculateSplitRatio`)**：
    *   **核心算法**：前端修改第二行及以后的比例时，后端进行双重校验，计算第一行的比例 = `100% - Σ(第2+行分摊比例)`。
    *   **异常拦截**：校验 `Σ(分摊比例)` 不可大于 100%，否则抛出 `BusinessException(ErrorCodeEnum.REIM_004)`。
    *   **精度控制**：使用 `BigDecimal` 进行乘法运算，使用 `RoundingMode.HALF_UP` 防止精度丢失。

### 3. 基础设施与工作流对接
*   **异动日志切面 (`ReimLogAspect`)**：
    *   使用 Spring AOP (`@Around` 或 `@AfterReturning`) 拦截报销单状态变更方法。
    *   对比修改前后的字段值，生成日志明细并存入 `fk_reim_log` 表（敏感字段需打码）。
*   **MQ 异步与补偿 Job (`BpmCompensationJob`)**：
    *   编写 XXL-Job 定时任务，扫描表中 `status=待推送` 且 `update_time < NOW() - 5分钟` 的遗漏数据。
    *   重新推送，重试超 3 次报警。

---

## 🛠️ 拟新增与修改的文件映射

### Mapper 层
*   `[NEW]` `ReimMainMapper.java` / `ReimSplitMapper.java`
*   `[NEW]` `ReimMainMapper.xml` (实现上述提及的多条件动态分页查询) / `ReimSplitMapper.xml`

### Service 层
*   `[NEW]` `ReimMainServiceImpl.java` (实现分页、保存、提交防重等逻辑)
*   `[NEW]` `ReimSplitServiceImpl.java` (实现精度与比例联动计算)

### Component 层
*   `[NEW]` `ReimLogAspect.java`
*   `[NEW]` `BpmCompensationJob.java`

### Controller 层
*   `[MODIFY]` `ReimMainController.java`
*   `[MODIFY]` `ReimSplitController.java`

---

## 🙋‍♂️ 开放问题与确认

> [!NOTE]
> 根据对齐后的要求，我需要和您确认以下实现细节以确保不偏离您的本地环境配置：
> 1. **单号生成的流水号部分**：在没有独立 Redis 序列号服务前，我们目前是使用基于内存/数据库的简易递增策略，还是必须立即引入 Redis 的 `INCR` 指令？
> 2. **Redisson 与 MQ (如 RocketMQ/RabbitMQ)**：文档中明确提及了 `RedissonClient` 和 MQ Topic。我们是要在这个版本里直接引入对应的 Maven 依赖并编写真实对接代码，还是先写接口占位及使用本地模拟（如 `ReentrantLock` 和本地事件总线）？
