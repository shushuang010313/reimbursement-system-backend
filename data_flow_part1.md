# 报销单基础业务数据流向剖析（答辩专用）

本文档将帮助你彻底理清“基础业务模块”的数据流转，从前端入参一直追踪到数据库落表，每一层的数据结构都进行了梳理。

## 场景一：保存/修改报销主单 (`saveReimMain`)

> [!NOTE]
> 对应前端的“保存草稿”操作，此接口支持新增或更新。通过判断入参中是否有 `id` 来决定是 `insert` 还是 `update`。

### 1. 前端传参 (JSON Payload -> DTO)

前端以 JSON 格式提交数据，Spring MVC 将其反序列化为 `ReimSaveDTO` 对象。

```java
// ReimSaveDTO 核心结构
{
  "id": "如果为空则是新增，有值则是更新",
  "version": 1, // 乐观锁版本号（更新必传）
  "reimbursementTitle": "2026年5月出差报销",
  "reimburserId": "user-123", // 由当前登录用户信息带入
  "subsidyTotal": 500.00, // 报销总金额
  
  // 级联的行程明细列表
  "tripList": [
    {
       "departureCity": "北京",
       "arrivalCity": "上海",
       "tripAmount": 300.00
    }
  ],
  
  // 级联的分摊明细列表（在保存前，前端其实已经调用过试算引擎了，这里的金额和比例是算好的）
  "splitList": [
    {
       "sortNo": 1, // 第一行，兜底行
       "splitRatio": 0.4000,
       "splitAmount": 200.00
    },
    {
       "sortNo": 2, // 第二行
       "splitRatio": 0.6000,
       "splitAmount": 300.00
    }
  ]
}
```

### 2. Service 层处理逻辑

1. **DTO 转 Entity**：通过 `BeanUtils.copyProperties(dto, main)` 将数据拷贝至数据库实体。
2. **新增流程**：如果 `id` 为空，使用 Redis 的 `incr` 生成唯一流水号 `reimNo`，设置状态为 `0-草稿`。
3. **更新流程**：如果 `id` 不为空。
    - **乐观锁校验**：使用传入的 `version`。MyBatis Plus 执行 `update ... set version = version + 1 where id = ? and version = ?`。如果没更新到数据，抛出异常。
    - **明细处理策略**：全删全插（逻辑删除）。把老的明细 `del_flag` 设为 1，然后把前端传来的新明细列表保存入库。

### 3. 数据落表 (Entity)

- `fk_reim_main` (主单表)：存入主数据，`reim_status` 为 0。
- `fk_reim_trip` (行程表)：批量插入。
- `fk_reim_split` (分摊表)：批量插入。

---

## 场景二：提交报销单 (`submitReim`)

> [!IMPORTANT]
> 这是一个高并发下极其容易出问题的地方。也是你答辩的核心亮点（Redisson 防抖 + MQ 消息投递 + AOP 日志）。

### 1. 前端传参 (DTO)

```java
// ReimSubmitDTO 结构极简
{
  "id": "180000000000001",
  "version": 1 // 必须带上版本号，防止脏写
}
```

### 2. Service 层处理逻辑 (包含三大防线)

1. **第一道防线（网关拦截 - AOP）**：在进入 `submitReim` 方法前，先被 `IdempotentAspect` 拦截。提取 URI + 用户ID + 入参MD5 生成 Key。利用 Redis Lua 脚本执行 `setnx`。如果是连击，直接在 AOP 抛出异常返回，连 Service 都进不来。
2. **第二道防线（业务级防串流 - Redisson）**：进入 Service 后，根据单据 ID 拼接锁的 Key `fk:reim:lock:submit:{id}`。使用 `redissonClient.getLock()` 尝试加锁。确保同一张单据同时只有一个线程在进行提交操作。
3. **状态机与完整性校验**：必须是草稿状态（0），且 `subsidyTotal` 不能为空，必须包含行程。
4. **第三道防线（乐观锁）**：更新状态为 `1-已完成(待推送)`。
5. **本地消息表落库**：为了后续向 BPM 异步推送，生成一个 UUID，往 `fk_mq_message` 表插一条记录，状态设为 `0待发送`。
6. **发送 MQ 消息**：调用 `rabbitTemplate` 发送消息。如果发送成功，更新本地表为 `1发送成功`；如果抛出异常，更新为 `2发送失败`。

### 3. AOP 异动日志抓取 (隐式流转)

> [!TIP]
> 这一步不在 `ReimMainServiceImpl` 中，而是在切面里悄悄执行的。

1. 切面拦截到 `submitReim`。
2. 拿到提交前的 `oldMain` (此时是 0-草稿)。
3. 方法执行完后，拿到 `newMain` (此时是 1-已完成)。
4. 对比发现状态不一致，往 `fk_reim_log` 插入一条记录。

```java
// ReimLog 入库结构
{
   "reimId": "180000000000001",
   "action": "报销单提交",
   "oldStatus": 0,
   "newStatus": 1,
   "details": "{\"reimNo\":\"REIM202605280001\",\"subsidyTotal\":500.00}", // 转为 JSON 存入
   "operatorId": "user-123"
}
```

---

## 场景三：费用分摊试算引擎 (`calculateSplitRatio`)

> [!WARNING]
> 这是财务系统中最关键的算法环节：解决金额分配的精度丢失问题（著名的“一分钱问题”）。

### 1. 前后端交互流向

当用户在前端填写总金额 `100元`，并添加了3个分摊行（33.33%, 33.33%, 33.34%）时，前端会调用试算接口。

```java
// 入参: List<ReimSplitDTO>
[
  { "sortNo": 1, "splitRatio": null, "splitAmount": null }, // 第一行，不填比例和金额
  { "sortNo": 2, "splitRatio": 0.3333, "splitAmount": null },
  { "sortNo": 3, "splitRatio": 0.3333, "splitAmount": null }
]
```

### 2. 倒挤法试算逻辑

1. **查总金额**：根据主单ID查出总金额，假设是 `100.00`。
2. **正向计算“其它行”**：
   - 遍历列表，跳过 `sortNo == 1` 的行。
   - 第 2 行金额 = `100.00 * 0.3333 = 33.33`。累加其他行比例 (`0.3333`)，累加其他行金额 (`33.33`)。
   - 第 3 行金额 = `100.00 * 0.3333 = 33.33`。累加其他行比例 (`0.6666`)，累加其他行金额 (`66.66`)。
3. **倒挤计算“兜底行”（第一行）**：
   - 必须校验前置条件：`totalOtherRatio` 必须 `<= 1.0`，否则抛出异常。
   - 第一行比例 = `1.0 - 0.6666 = 0.3334`。
   - 第一行金额 = `100.00 - 66.66 = 33.34`。

### 3. 输出数据

最终返回给前端的 DTO 列表中，所有数据严丝合缝，不会因为精度丢失导致 `33.33 + 33.33 + 33.33 = 99.99` 的问题。前端拿到完整数据后，再调用 `saveReimMain` 进行入库操作。
