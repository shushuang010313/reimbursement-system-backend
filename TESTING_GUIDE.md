# 差旅报销系统 - 五大亮点功能详细测试攻略

> 适用于：答辩演示 / 系统验收 / 功能回归测试  
> 配套 HTTP 测试脚本：`test_features.http` (可直接在 IntelliJ IDEA 中运行)

---

## 目录

1. [环境准备](#1-环境准备)
2. [亮点五：接口防抖与幂等性 (Redis + Lua)](#2-亮点五接口防抖与幂等性)
3. [亮点一：数据加密与脱敏 (AES + Jackson)](#3-亮点一数据加密与脱敏)
4. [亮点三：分布式最终一致性 (RabbitMQ DLX)](#4-亮点三分布式最终一致性)
5. [亮点四：百万级异步导出 (EasyExcel + ThreadPool)](#5-亮点四百万级异步导出)
6. [亮点二：乐观锁 + 分布式锁并发控制](#6-亮点二乐观锁--分布式锁)
7. [整合场景测试](#7-整合场景测试)
8. [异常与边界测试](#8-异常与边界测试)
9. [常见问题排查](#9-常见问题排查)

---

## 1. 环境准备

### 1.1 基础设施

| 组件 | 版本 | 地址 | 账号/密码 |
|------|------|------|-----------|
| MySQL | 8.0+ | localhost:3306 | root / 123456 |
| Redis | 6.0+ | 192.168.40.128:6379 | (无) / 123456 |
| RabbitMQ | 3.x | 192.168.40.128:5672 | itcast / 123456 |
| RabbitMQ管理界面 | - | http://192.168.40.128:15672 | itcast / 123456 |
| Swagger UI | - | http://localhost:8080/api/swagger-ui/index.html | - |

### 1.2 应用配置检查

确认 `application.yaml` 中以下配置与实际环境一致：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/fk_reim_db?...   # 确认库名和密码
    password: 123456                                    # 改成你自己的MySQL密码
  data:
    redis:
      host: 192.168.40.128                              # 确认Redis地址和密码
      password: 123456
  rabbitmq:
    host: 192.168.40.128                                # 确认RabbitMQ地址和凭证
    username: itcast
    password: 123456
```

### 1.3 数据库初始化

执行 `src/main/resources/init.sql` 脚本（会创建数据库、表结构、插入 3 条测试数据）。

### 1.4 启动应用

```bash
mvn spring-boot:run
```

确认控制台无异常，`ReimbursementSystemApplication` 启动成功。

### 1.5 测试工具

- **推荐**: IntelliJ IDEA 内置 HTTP Client（打开 `test_features.http` 文件）
- **备选**: Postman / Apifox（导入 Swagger 地址 `http://localhost:8080/api/swagger-ui/index.html`）

---

## 2. 亮点五：接口防抖与幂等性

### 2.1 功能概述

**代码位置：**
- `annotation/Idempotent.java` — 幂等注解，可配置超时时间和提示信息
- `interceptor/IdempotentAspect.java` — AOP 切面，执行 Redis Lua 原子脚本

**核心机制：**  
基于 Redis `SETNX` + Lua 脚本，以 `请求URI + 用户ID + 参数MD5` 作为唯一 Key。在注解配置的超时时间内（默认5秒），相同请求只有第一次能通过，后续返回业务异常。

### 2.2 测试步骤

#### Step 1：幂等性拦截验证（核心场景）

在 IDEA 中打开 `test_features.http`，找到 `2.1 【亮点五核心测试】`：

```
POST /api/fccapi/REIM_Save
Content-Type: application/json
user-id: user-001
{...完整请求体...}
```

**操作：** 快速连续点击两次 Execute（间隔 < 5秒）

**预期结果：**

| 次数 | HTTP 状态码 | code | message | 说明 |
|------|-------------|------|---------|------|
| 第1次 | 200 | 200 | 操作成功 | 正常保存，返回报销单ID |
| 第2次 | 500 | 500 | 正在保存报销单，请勿频繁点击 | 幂等性拦截 |

**验证点：** 检查 IDEA 控制台日志：
```
【接口幂等性拦截】发现重复请求拦截，URI: /api/fccapi/REIM_Save, Key: idempotent:/api/fccapi/REIM_Save:user-001:xxx
```

#### Step 2：不同参数不受影响

执行 `2.2 【亮点五扩展】`，修改 `reimbursementTitle` 为不同的值。

**预期：** 立即成功（因为参数MD5不同，生成了新的幂等Key）

#### Step 3：提交接口幂等性

执行 `2.3 【亮点五扩展】`，快速连点两次提交请求。

**预期：** 第一次提交成功，第二次被拦截提示 "提交处理中，请不要重复提交"

#### Step 4：Redis Key 过期验证（可选）

等待 5 秒后，再次发送与 Step1 完全相同参数的请求。

**预期：** 请求成功（5秒后 Redis Key 自动过期，不再拦截）

### 2.3 答辩演示话术

> "考虑到员工在网络卡顿时可能疯狂点击'提交'按钮，为了避免财务系统中出现重复的报销单，我们基于 Redis 和自定义注解 @Idempotent 实现了接口幂等性拦截器。底层使用 Lua 脚本保证了 SETNX + EXPIRE 两个操作的绝对原子性，从底层彻底杜绝了重复造单的风险。"

---

## 3. 亮点一：数据加密与脱敏

### 3.1 功能概述

**代码位置：**
- `annotation/JsonEncrypt.java` — 组合注解，同时触发 Jackson 序列化器
- `annotation/DesensitizeType.java` — 脱敏类型：ID_CARD / BANK_CARD / PHONE / DEFAULT
- `config/JsonEncryptSerializer.java` — Jackson 自定义序列化器，实现 `ContextualSerializer` 接口
- `utils/AESUtils.java` — AES/ECB/PKCS5Padding 加解密工具
- `interceptor/CryptoInterceptor.java` — MyBatis 拦截器，拦截 Executor.update/query 实现透明加解密

**核心机制：**  
入库时 MyBatis 拦截器加密 → 数据库存密文 → 出库时拦截器解密 → 返回前端时 Jackson 序列化器脱敏。三个环节各司其职。

### 3.2 测试步骤

#### Step 1：API 层脱敏验证（核心场景）

先执行保存请求 `3.1` 获取报销单ID，再执行 `3.1 【亮点一核心测试】` 查询详情：

```
GET /api/fccapi/REIM_GetDetail?id=<报销单ID>
```

**预期响应：**
```json
{
  "code": 200,
  "data": {
    "payeeIdCard": "110105********1234",
    "payeeBankAccount": "6222****3333"
  }
}
```

> **关键验证：** 返回的身份证不是完整18位！而是前6后4中间8个星号。

#### Step 2：数据库密文验证

在 IDEA 的 Database 工具窗口执行：

```sql
SELECT id, payee_id_card, payee_bank_account
FROM fk_reim_db.fk_reim_main
WHERE id = '<报销单ID>';
```

**预期结果：**
```
payee_id_card       | payee_bank_account
xYz9KlmN8pQr...==  | aB3dEfGh7JkL...==  (Base64格式密文)
```

> 如果看到的是明文身份证号，说明 `CryptoInterceptor` 未生效，检查 MyBatis-Plus 配置。

#### Step 3：完整链路验证

1. 执行 `3.3 【亮点一扩展】` 创建新报销单
2. 立即执行 `SELECT` 查数据库 → 确认是密文
3. 用 GetDetail API 查询 → 确认返回的是脱敏后数据
4. 验证 Java 内存中操作正常（如去更新该记录 → 可以正常更新，证明内存中是明文）

#### Step 4：脱敏类型枚举测试

| DesensitizeType | 测试值 | 预期脱敏结果 |
|-----------------|--------|-------------|
| ID_CARD (18位) | 110105199001011234 | 110105********1234 |
| ID_CARD (≥10位) | 4201021985121256 | 4201****1256 |
| BANK_CARD (≥8位) | 6222021001112222333 | 6222****2333 |
| PHONE (11位) | 13800138000 | 138****8000 |
| DEFAULT | 任意字符串 | \*\*\*\*\*\* |

### 3.3 答辩演示话术

> "考虑到企业真实财务系统中涉及身份证、银行卡等敏感隐私数据，我们在系统底层利用自定义注解 + MyBatis拦截器 + Jackson自定义序列化器，实现了'数据库透明加密 + 前端传输动态脱敏'的安全合规体系。三层防护：入库即加密、内存可明文操作、出站自动脱敏，完全符合《数据安全法》对个人敏感信息的保护要求。"

---

## 4. 亮点三：分布式最终一致性

### 4.1 功能概述

**代码位置：**
- `config/RabbitMQConfig.java` — 定义正常交换机/队列 + 死信交换机/队列
- `mq/BpmMessageListener.java` — 业务消费者 + 死信消费者（含演示异常）
- `job/BpmCompensationJob.java` — 定时补偿任务（每5分钟）
- `entity/FkMqMessage.java` — 本地消息表
- `application.yaml` — RabbitMQ listener retry 配置

**核心机制：**  
"本地消息表 + MQ 投递 + 消费者重试 + 死信队列兜底 + 定时补偿" 五层保障，实现 100% 不丢消息。

### 4.2 测试步骤

#### Step 1：创建测试报销单

执行 `4.1 【亮点三前置】` 保存一个草稿报销单（MQ测试专用）。

#### Step 2：提交触发 MQ 消息（核心场景）

执行 `4.2 【亮点三核心测试】` 提交报销单。

**⚠️ 不要关闭 IDEA 控制台！仔细观察日志时间线：**

| 时间 | 日志内容 | 说明 |
|------|---------|------|
| T+0s | `【BPM消费者】接收到报销单提交消息，准备推送审批流，报销单ID: xxx` | 消息被消费 |
| T+0s | `【BPM消费者】调用BPM系统接口发生网络超时异常！(演示用途)` | 模拟异常抛出 |
| T+2s | (Spring-RabbitMQ 自动重试第1次) initial-interval=2000ms | 指数退避开始 |
| T+6s | (Spring-RabbitMQ 自动重试第2次) 2000×2=4000ms | multiplier=2 |
| T+16s | (Spring-RabbitMQ 自动重试第3次) 不超过10000ms | max-interval |
| T+16s | `【严重告警】向BPM推送报销单(ID: xxx)失败，重试3次已耗尽！` | 进入 DLX |
| T+16s | `【严重告警】该消息已进入死信队列(DLQ)，系统已触发钉钉/邮件告警` | 人工兜底 |

#### Step 3：验证本地消息表

```sql
SELECT * FROM fk_reim_db.fk_mq_message
WHERE business_id = '<报销单ID>'
ORDER BY creation_time DESC;
```

**预期：** `topic = 'reim.submit.topic'`, `status = 1` (成功发送到MQ)

#### Step 4：验证死信队列（RabbitMQ 管理界面）

1. 浏览器打开 `http://192.168.40.128:15672`
2. 登录 (itcast / 123456)
3. 点击 Queues 标签
4. 找到 `reim.dlx.queue` → 查看 Ready 列

**预期：** Ready > 0，说明消息已经进入死信队列。

#### Step 5：验证补偿任务（可选，需等待5分钟）

观察控制台，等待约5分钟后出现：
```
开始执行BPM审批流推送补偿任务...
补偿推送报销单 xxx 到 MQ
BPM审批流推送补偿任务执行结束。
```

### 4.3 配置说明

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        retry:
          enabled: true
          max-attempts: 3           # 最大重试3次
          initial-interval: 2000    # 首次重试间隔 2s
          multiplier: 2             # 指数退避乘数
          max-interval: 10000       # 最大间隔 10s
        default-requeue-rejected: false  # 重试耗尽后不重新入队，直接进入DLX
```

### 4.4 答辩演示话术

> "在分布式环境下，为了保证财务报销状态的绝对一致性，我们引入了基于 RabbitMQ 的可靠消息投递机制。五层保障环环相扣：本地消息表保证事务一致性、消息投递确保异步解耦、消费者自动重试应对瞬时故障、死信队列防止消息丢失、定时补偿任务作为最后的扫尾兜底。如果网络断了或 BPM 系统挂了，消息不会丢失，等系统恢复后自动补偿推送，实现了 100% 不丢单。"

---

## 5. 亮点四：百万级异步导出

### 5.1 功能概述

**代码位置：**
- `controller/ExportController.java` — 提交任务 + 查询进度 + 下载文件
- `service/AsyncExportService.java` — 异步执行，分页读取 + 流式写入
- `config/ThreadPoolConfig.java` — 导出专用线程池（核心5/最大10/队列50/CallerRuns拒绝策略）
- `vo/ExportTaskVO.java` — 任务状态VO（Redis 缓存）
- `vo/ReimExportVO.java` — 导出VO（EasyExcel 注解映射）
- `entity/FkAsyncTask.java` — 数据库任务记录

**核心机制：**  
"@Async 异步 + 分页查库(1000条/页) + EasyExcel 流式写入 + Redis 进度追踪 + 数据库持久化"。导出结果（含脱敏数据）存储在服务器临时目录，前端通过 taskId 轮询状态后下载。

### 5.2 测试步骤

#### Step 1：提交导出任务

执行 `5.1 【亮点四 Step1】`：

```
POST /api/reim/export/async
```

**预期响应：**
```json
{
  "code": 200,
  "data": {
    "taskId": "a1b2c3d4e5f67890...",
    "status": "已提交"
  }
}
```

> **关键：** 接口应立即返回（< 100ms），不是等导出完毕才返回！

#### Step 2：轮询任务进度

执行 `5.2 【亮点四 Step2】`，**建议间隔2秒多次发送**：

```
GET /api/reim/export/status/<taskId>
```

**预期进度变化：**
```
第1次 (0s):   {"status": "PROCESSING", "progress": 0}
第2次 (2s):   {"status": "PROCESSING", "progress": 35}
第3次 (4s):   {"status": "PROCESSING", "progress": 70}
第4次 (6s):   {"status": "SUCCESS", "progress": 100, "downloadUrl": "..."}
```

#### Step 3：下载 Excel 文件

执行 `5.3 【亮点四 Step3】`：

```
GET /api/reim/export/download/<taskId>
```

IDEA 会弹出 Save As 对话框，另存为 `.xlsx` 后用 Excel 打开。

**验证 Excel 内容：**

| 列名 | 预期内容 |
|------|---------|
| 收款人身份证号(脱敏) | `110105********1234` （脱敏后!） |
| 收款人银行账号(脱敏) | `6222****1234` （脱敏后!） |
| 报销单号 | `REIM20250520001` |
| 单据状态 | `已完成` / `草稿` / `已作废` |

#### Step 4：验证数据库任务记录

```sql
SELECT id, task_name, task_type, status, progress, file_url, error_msg
FROM fk_reim_db.fk_async_task
ORDER BY creation_time DESC LIMIT 5;
```

**预期：**
- `task_type = 'EXPORT'`
- `status = 2` (成功)
- `progress = 100`

#### Step 5：高负载压力测试（可选）

如果有大量数据，可以验证分页 + 流式写入机制不会 OOM：
1. 通过 SQL 批量插入 10万+ 条数据
2. 提交导出任务
3. 观察 JVM 内存使用（JConsole / VisualVM）

**预期：** 内存使用平稳，不会出现陡增和 OOM。

### 5.3 线程池配置说明

```java
corePoolSize: 5       // 核心线程数
maxPoolSize: 10       // 最大线程数
queueCapacity: 50     // 阻塞队列容量
RejectedExecutionHandler: CallerRunsPolicy  // 队列满时回退到调用线程，自然限流
```

### 5.4 答辩演示话术

> "财务在月底往往需要导出大量的报销明细。为了保护系统内存，我们放弃了传统的 POI 同步导出，采用了阿里的 EasyExcel 结合自定义线程池实现异步导出。前端先获取一个任务ID，通过轮询接收导出进度，彻底解决了大批量财务数据导出导致的系统卡顿问题。同时导出文件中的数据也做了脱敏处理，防止 Excel 文件泄露敏感信息。"

---

## 6. 亮点二：乐观锁 + 分布式锁

### 6.1 功能概述

**代码位置：**
- `entity/ReimMain.java` — `@Version` 注解的 version 字段
- `service/impl/ReimMainServiceImpl.java` — `saveReimMain()` 乐观锁校验 + `submitReim()` 分布式锁

**核心机制：**
- 保存更新：MyBatis-Plus OptimisticLockerInnerInterceptor 自动在 SQL 追加 `WHERE version = ?`
- 提交：Redisson 分布式锁 + 乐观锁双重保障

### 6.2 测试步骤

#### Step 1：创建测试数据

执行 `6.1 【亮点二前置】` 保存新报销单（version 初始值为 0）。

#### Step 2：正确版本号更新 → 成功

执行 `6.2 【亮点二核心测试A】`，携带 version=0 更新：

**预期：** `code=200`，更新成功。数据库 version 自动变为 1。

#### Step 3：过期版本号更新 → 失败（核心场景）

执行 `6.3 【亮点二核心测试B】`，仍然携带 version=0（上一步已更新为1）：

**预期：**
```json
{
  "code": 400,
  "message": "数据已被修改，请刷新后重试"
}
```

**MySQL 日志验证：**
```sql
-- MyBatis-Plus 实际执行的 SQL:
UPDATE fk_reim_main SET ..., version = version + 1
WHERE id = ? AND version = 0;
-- version=0 的记录已不存在，affected rows = 0，触发乐观锁异常
```

### 6.3 答辩演示话术

> "在同一笔报销单可能被多人同时编辑的场景中，我们使用 MyBatis-Plus 的 @Version 乐观锁机制，在 SQL 层面自动追加版本号校验。同时结合 Redisson 分布式锁在提交环节做双重保障，确保在高并发场景下数据一致性万无一失。"

---

## 7. 整合场景测试

### 7.1 完整用户旅程

模拟一个真实用户的完整操作流程（按顺序执行）：

| 步骤 | 接口 | 目的 |
|------|------|------|
| 1 | `POST /REIM_Save` | 新建草稿报销单（含敏感信息） |
| 2 | `GET /REIM_GetDetail` | 查看详情，验证脱敏效果 |
| 3 | `POST /REIM_Save` | 修改报销单（携带正确 version） |
| 4 | `POST /REIM_Submit` | 提交报销单，触发 MQ 推送 |
| 5 | `观察控制台` | 见证 MQ 重试 → DLX 告警流程 |
| 6 | `POST /REIM_ExportAsync` | 提交导出任务 |
| 7 | `GET /REIM_ExportStatus` | 轮询直到完成 |
| 8 | `GET /reim/export/download` | 下载 Excel 验证脱敏 |

### 7.2 多用户并发场景

在 IDEA 中可以用不同的 `user-id` header 模拟不同用户：

```
user-id: user-001   # 张三，执行保存操作
user-id: user-002   # 李四，同时编辑同一报销单 → 触发乐观锁
```

---

## 8. 异常与边界测试

### 8.1 测试清单

| 测试项 | 接口 | 预期结果 |
|--------|------|---------|
| 缺少必填字段 | POST /REIM_Save (空字段) | code=400 "参数错误" |
| 查询不存在的报销单 | GET /REIM_GetDetail?id=xxx | code=404 "报销单不存在" |
| 查询不存在的导出任务 | GET /export/status/xxx | code=404 "任务不存在或已过期" |
| 提交不存在的报销单 | POST /REIM_Submit | code=404 "报销单不存在" |
| 提交缺少version | POST /REIM_Submit (无version) | code=400 "参数错误" |
| 重复提交已完成单 | POST /REIM_Submit (status=1) | code=400 "报销单状态不允许该操作" |
| 作废草稿单 | POST /REIM_Cancel (status=0) | code=400 "只有已完成的报销单可以作废" |
| 下载未完成的导出 | GET /export/download/xxx | RuntimeException "文件尚未生成或已过期" |

配合 `test_features.http` 的第七章执行，覆盖 8 个边界测试用例。

---

## 9. 常见问题排查

### 9.1 幂等性不生效

- 检查 Redis 是否启动：`redis-cli -h 192.168.40.128 -a 123456 ping` → 应返回 PONG
- 检查 `@Idempotent` 注解是否在 Controller 方法上
- 检查 AOP 是否生效，在 `IdempotentAspect` 中加断点调试

### 9.2 数据库加密不生效

- 检查 `CryptoInterceptor` 是否有 `@Component` 注解
- 检查 MyBatis-Plus 配置是否正确加载
- 查看控制台 SQL 日志，确认字段有被 UPDATE/INSERT

### 9.3 脱敏不生效

- 检查 `@JsonEncrypt` 注解是否在实体字段上
- 检查 `DesensitizeType` 是否正确设置
- Jackson 自定义序列化器需要实现 `ContextualSerializer` 接口

### 9.4 RabbitMQ 消息不消费

- 检查 RabbitMQ 是否启动：`http://192.168.40.128:15672`
- 检查 `application.yaml` 中 RabbitMQ 配置是否正确
- 检查队列绑定关系：在管理界面查看 Exchange → Queue 绑定

### 9.5 导出任务一直不完成

- 查看控制台是否有异常日志
- 检查 `FkAsyncTaskMapper` 和 `FkAsyncTask` 实体映射是否正常
- 检查数据库 `fk_async_task` 表是否存在
- 临时文件存储在 `java.io.tmpdir`，确保有写权限

### 9.6 数据库连接失败

```sql
-- 确认 MySQL 服务已启动
-- 确认数据库已创建
SHOW DATABASES;  -- 应包含 fk_reim_db

-- 确认数据表已创建
USE fk_reim_db;
SHOW TABLES;  -- 应包含: fk_reim_main, fk_reim_trip, fk_reim_subsidy, fk_reim_calendar, fk_reim_split, fk_reim_log, fk_async_task, fk_mq_message
```

---

## 附录 A：快速验证脚本（一键执行）

如果你不想逐个点击，可以在 IDEA 中按顺序执行以下 HTTP 请求（省略了请求体，完整版见 `test_features.http`）：

```
1. POST  /api/fccapi/REIM_Save            → 保存草稿
2. GET   /api/fccapi/REIM_GetDetail?id=xx  → 验证脱敏
3. POST  /api/fccapi/REIM_Submit           → 验证 MQ+DLX
4. POST  /api/reim/export/async            → 提交导出
5. GET   /api/reim/export/status/xx        → 轮询进度
6. GET   /api/reim/export/download/xx      → 下载 Excel
```

## 附录 B：答辩演示 Checklist

- [ ] 数据库 `payee_id_card` 存的是密文（SQL 直查验证）
- [ ] API 返回身份证号是脱敏的 `110105********1234`
- [ ] 5秒内重复提交被拦截（幂等性）
- [ ] 控制台看到 MQ 消费者异常 → 3次重试 → 死信告警日志
- [ ] RabbitMQ 管理界面看到 `reim.dlx.queue` 中有消息
- [ ] 本地消息表 `fk_mq_message` 有记录
- [ ] 提交导出任务后立即返回 taskId，不阻塞
- [ ] 轮询进度接口看到 progress 逐步增长
- [ ] 下载的 Excel 中敏感字段是脱敏的
- [ ] 乐观锁拦截成功（过期 version 更新失败）
