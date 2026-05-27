-- =============================================
-- 费控云差旅报销单模块 数据库初始化脚本
-- 版本：V1.0
-- 日期：2025-05-26
-- 字符集：utf8mb4
-- 存储引擎：InnoDB
-- =============================================

-- 初始化SQL环境配置
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
SET SQL_MODE = 'NO_AUTO_VALUE_ON_ZERO';
SET AUTOCOMMIT = 0;

-- =============================================
-- 1. 创建数据库（若不存在）
-- =============================================
CREATE DATABASE IF NOT EXISTS fk_reim_db
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_unicode_ci;

USE fk_reim_db;

-- =============================================
-- 2. 创建核心业务表（先删除旧表，避免重复执行报错）
-- =============================================

-- 2.1 报销单主表（所有关联表的主表）
DROP TABLE IF EXISTS fk_reim_main;
CREATE TABLE fk_reim_main (
                              id VARCHAR(32) NOT NULL COMMENT '主键ID（UUID生成）',
                              reim_no VARCHAR(32) NOT NULL COMMENT '报销单号（规则：REIM+年月日+流水号）',
                              reim_status INT(4) NOT NULL DEFAULT 0 COMMENT '单据状态：0-草稿 1-已完成 2-已作废',
                              reimbursement_title VARCHAR(500) NULL DEFAULT NULL COMMENT '报销标题',
                              business_trip_reason VARCHAR(500) NULL DEFAULT NULL COMMENT '出差事由',
                              reimburser_id VARCHAR(32) NULL DEFAULT NULL COMMENT '报销人ID',
                              reimburser_no VARCHAR(20) NULL DEFAULT NULL COMMENT '报销人工号',
                              reimburser_name VARCHAR(50) NULL DEFAULT NULL COMMENT '报销人姓名',
                              reim_department_id VARCHAR(32) NULL DEFAULT NULL COMMENT '报销部门ID',
                              reim_department_no VARCHAR(20) NULL DEFAULT NULL COMMENT '报销部门编号',
                              reim_department_name VARCHAR(100) NULL DEFAULT NULL COMMENT '报销部门名称',
                              reim_company_id VARCHAR(32) NULL DEFAULT NULL COMMENT '费用归属公司ID',
                              reim_company_no VARCHAR(20) NULL DEFAULT NULL COMMENT '费用归属公司编号',
                              reim_company_name VARCHAR(100) NULL DEFAULT NULL COMMENT '费用归属公司名称',
                              business_type_id VARCHAR(32) NULL DEFAULT NULL COMMENT '业务类型ID',
                              business_type_no VARCHAR(20) NULL DEFAULT NULL COMMENT '业务类型编号',
                              business_type_name VARCHAR(100) NULL DEFAULT NULL COMMENT '业务类型名称',
                              subsidy_total DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '补助总金额',
                              meal_allowance DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '餐费补助',
                              transportation_allowance DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '交通补助',
                              phone_allowance DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '通讯补助',
                              remarks VARCHAR(2000) NULL DEFAULT NULL COMMENT '备注信息',
                              payee_id_card VARCHAR(255) NULL DEFAULT NULL COMMENT '收款人身份证号（密文）',
                              payee_bank_account VARCHAR(255) NULL DEFAULT NULL COMMENT '收款人银行账号（密文）',
                              version INT(11) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号（用于并发控制）',
                              create_user_id VARCHAR(32) NULL DEFAULT NULL COMMENT '创建人ID',
                              create_user_no VARCHAR(20) NULL DEFAULT NULL COMMENT '创建人工号',
                              create_user_name VARCHAR(50) NULL DEFAULT NULL COMMENT '创建人姓名',
                              creation_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                              update_user_id VARCHAR(32) NULL DEFAULT NULL COMMENT '最后更新人ID',
                              update_user_name VARCHAR(50) NULL DEFAULT NULL COMMENT '最后更新人姓名',
                              update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
                              del_flag INT(4) NOT NULL DEFAULT 0 COMMENT '删除标志：0-正常 1-已删除',

                              PRIMARY KEY (id),
                              UNIQUE KEY uk_reim_no (reim_no) COMMENT '报销单号唯一索引',
                              KEY idx_reimburser_id (reimburser_id) COMMENT '报销人个人报销单查询索引',
                              KEY idx_reim_status (reim_status) COMMENT '单据状态筛选索引',
                              KEY idx_reim_company_id (reim_company_id) COMMENT '公司数据隔离索引',
                              KEY idx_creation_time (creation_time) COMMENT '创建时间范围查询索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报销单主表';

-- 2.2 补录行程表（与报销单主表一对多）
DROP TABLE IF EXISTS fk_reim_trip;
CREATE TABLE fk_reim_trip (
                              id VARCHAR(32) NOT NULL COMMENT '主键ID',
                              reim_id VARCHAR(32) NOT NULL COMMENT '报销单ID（关联fk_reim_main.id）',
                              traveler_id VARCHAR(32) NOT NULL COMMENT '出行人ID',
                              traveler_no VARCHAR(20) NULL DEFAULT NULL COMMENT '出行人工号',
                              traveler_name VARCHAR(50) NULL DEFAULT NULL COMMENT '出行人姓名',
                              departure_city_id VARCHAR(32) NOT NULL COMMENT '出发城市ID',
                              departure_city_name VARCHAR(100) NULL DEFAULT NULL COMMENT '出发城市名称',
                              arrive_city_id VARCHAR(32) NOT NULL COMMENT '到达城市ID',
                              arrive_city_name VARCHAR(100) NULL DEFAULT NULL COMMENT '到达城市名称',
                              arrive_city_level VARCHAR(10) NULL DEFAULT NULL COMMENT '到达城市等级：1-一线 2-二线 3-三线',
                              departure_date DATE NOT NULL COMMENT '出发日期',
                              arrive_date DATE NOT NULL COMMENT '到达日期',
                              trip_days INT(11) NULL DEFAULT NULL COMMENT '行程天数（到达日期-出发日期+1）',
                              trip_desc VARCHAR(500) NULL DEFAULT NULL COMMENT '行程说明',
                              creation_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                              del_flag INT(4) NOT NULL DEFAULT 0 COMMENT '删除标志：0-正常 1-已删除',

                              PRIMARY KEY (id),
                              KEY idx_reim_id (reim_id) COMMENT '关联报销单主表索引',
                              KEY idx_traveler_id (traveler_id) COMMENT '出行人行程查询索引',
                              UNIQUE KEY uk_trip_unique (reim_id, traveler_id, departure_date, arrive_date) COMMENT '防止同一报销单下同一人重复行程',
                              KEY idx_date_range (departure_date, arrive_date) COMMENT '行程时间范围查询索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='补录行程表';

-- 2.3 补助信息表（与补录行程表一对一）
DROP TABLE IF EXISTS fk_reim_subsidy;
CREATE TABLE fk_reim_subsidy (
                                 id VARCHAR(32) NOT NULL COMMENT '主键ID',
                                 reim_id VARCHAR(32) NOT NULL COMMENT '报销单ID（关联fk_reim_main.id）',
                                 trip_id VARCHAR(32) NOT NULL COMMENT '行程ID（关联fk_reim_trip.id）',
                                 traveler_id VARCHAR(32) NOT NULL COMMENT '出行人ID',
                                 traveler_name VARCHAR(50) NULL DEFAULT NULL COMMENT '出行人姓名',
                                 trip_start_date DATE NULL DEFAULT NULL COMMENT '出差开始日期',
                                 trip_end_date DATE NULL DEFAULT NULL COMMENT '出差结束日期',
                                 subsidy_days INT(11) NULL DEFAULT NULL COMMENT '补助天数',
                                 subsidy_city_id VARCHAR(32) NULL DEFAULT NULL COMMENT '补助城市ID',
                                 subsidy_city_name VARCHAR(100) NULL DEFAULT NULL COMMENT '补助城市名称',
                                 apply_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '申请金额（标准金额合计）',
                                 subsidy_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '补助金额（实际补助金额合计）',
                                 meal_subsidy DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '餐费补助金额',
                                 transport_subsidy DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '交通补助金额',
                                 phone_subsidy DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '通讯补助金额',
                                 creation_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 del_flag INT(4) NOT NULL DEFAULT 0 COMMENT '删除标志：0-正常 1-已删除',

                                 PRIMARY KEY (id),
                                 KEY idx_reim_id (reim_id) COMMENT '关联报销单主表索引',
                                 KEY idx_trip_id (trip_id) COMMENT '关联补录行程表索引',
                                 KEY idx_traveler_id (traveler_id) COMMENT '出行人补助查询索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='补助信息表';

-- 2.4 补助日历表（与补助信息表一对多）
DROP TABLE IF EXISTS fk_reim_calendar;
CREATE TABLE fk_reim_calendar (
                                  id VARCHAR(32) NOT NULL COMMENT '主键ID',
                                  reim_id VARCHAR(32) NOT NULL COMMENT '报销单ID（关联fk_reim_main.id）',
                                  subsidy_id VARCHAR(32) NOT NULL COMMENT '补助信息ID（关联fk_reim_subsidy.id）',
                                  trip_date DATE NOT NULL COMMENT '出差日期',
                                  week_day VARCHAR(10) NULL DEFAULT NULL COMMENT '星期（如：星期一）',
                                  subsidy_city_id VARCHAR(32) NULL DEFAULT NULL COMMENT '补助城市ID',
                                  subsidy_city_name VARCHAR(100) NULL DEFAULT NULL COMMENT '补助城市名称',
                                  meal_checked INT(4) NOT NULL DEFAULT 0 COMMENT '餐费是否勾选：0-否 1-是',
                                  meal_standard DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '餐费标准金额',
                                  meal_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '餐费补助金额',
                                  transport_checked INT(4) NOT NULL DEFAULT 0 COMMENT '交通是否勾选：0-否 1-是',
                                  transport_standard DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '交通标准金额（固定40元/天）',
                                  transport_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '交通补助金额',
                                  phone_checked INT(4) NOT NULL DEFAULT 0 COMMENT '通讯是否勾选：0-否 1-是',
                                  phone_standard DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '通讯标准金额（固定40元/天）',
                                  phone_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '通讯补助金额',
                                  creation_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

                                  PRIMARY KEY (id),
                                  KEY idx_reim_id (reim_id) COMMENT '关联报销单主表索引',
                                  KEY idx_subsidy_id (subsidy_id) COMMENT '关联补助信息表索引',
                                  KEY idx_trip_date (trip_date) COMMENT '出差日期查询索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='补助日历表';

-- 2.5 费用分摊表（与报销单主表一对多）
DROP TABLE IF EXISTS fk_reim_split;
CREATE TABLE fk_reim_split (
                               id VARCHAR(32) NOT NULL COMMENT '主键ID',
                               reim_id VARCHAR(32) NOT NULL COMMENT '报销单ID（关联fk_reim_main.id）',
                               sort_no INT(11) NOT NULL DEFAULT 1 COMMENT '排序号（第1行不可编辑）',
                               company_id VARCHAR(32) NULL DEFAULT NULL COMMENT '费用归属公司ID',
                               company_name VARCHAR(100) NULL DEFAULT NULL COMMENT '费用归属公司名称',
                               project_id VARCHAR(32) NULL DEFAULT NULL COMMENT '项目ID',
                               project_name VARCHAR(100) NULL DEFAULT NULL COMMENT '项目名称',
                               split_ratio DECIMAL(10,4) NOT NULL DEFAULT 0.0000 COMMENT '分摊比例（如0.3000表示30%）',
                               split_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '分摊金额',
                               creation_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                               del_flag INT(4) NOT NULL DEFAULT 0 COMMENT '删除标志：0-正常 1-已删除',

                               PRIMARY KEY (id),
                               KEY idx_reim_id (reim_id) COMMENT '关联报销单主表索引',
                               KEY idx_company_id (company_id) COMMENT '费用归属公司查询索引',
                               KEY idx_project_id (project_id) COMMENT '项目分摊查询索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='费用分摊表';

-- 2.6 异动日志表
DROP TABLE IF EXISTS fk_reim_log;
CREATE TABLE fk_reim_log (
    id VARCHAR(32) NOT NULL COMMENT '主键ID',
    reim_id VARCHAR(32) NOT NULL COMMENT '报销单ID',
    action VARCHAR(100) NOT NULL COMMENT '操作类型',
    old_status INT(4) NULL DEFAULT NULL COMMENT '原状态',
    new_status INT(4) NULL DEFAULT NULL COMMENT '新状态',
    details VARCHAR(1000) NULL DEFAULT NULL COMMENT '详情或脱敏数据',
    operator_id VARCHAR(32) NULL DEFAULT NULL COMMENT '操作人ID',
    operator_name VARCHAR(50) NULL DEFAULT NULL COMMENT '操作人姓名',
    creation_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_reim_id (reim_id) COMMENT '按报销单查询日志索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报销单异动日志表';

-- =============================================
-- 3. 插入测试数据（事务保证原子性，要么全部成功要么全部回滚）
-- =============================================
START TRANSACTION;

-- 先清空旧数据（按依赖倒序删除）
DELETE FROM fk_reim_split;
DELETE FROM fk_reim_calendar;
DELETE FROM fk_reim_subsidy;
DELETE FROM fk_reim_trip;
DELETE FROM fk_reim_main;

-- 3.1 插入报销单主表数据（3种状态：已完成、草稿、已作废）
INSERT INTO fk_reim_main (
    id, reim_no, reim_status, reimbursement_title, business_trip_reason,
    reimburser_id, reimburser_no, reimburser_name,
    reim_department_id, reim_department_no, reim_department_name,
    reim_company_id, reim_company_no, reim_company_name,
    business_type_id, business_type_no, business_type_name,
    subsidy_total, meal_allowance, transportation_allowance, phone_allowance,
    remarks, payee_id_card, payee_bank_account, version, create_user_id, create_user_no, create_user_name,
    update_user_id, update_user_name
) VALUES
      (
          'reim001', 'REIM20250520001', 1, '北京产品调研差旅费', '赴北京参加产品研讨会并调研竞品',
          'emp1001', '1001', '张三',
          'dept001', 'D001', '产品部',
          'comp001', 'C001', '字节跳动科技有限公司',
          'biz001', 'B001', '市场调研',
          540.00, 300.00, 120.00, 120.00,
          '参加2025年5月21-23日北京产品研讨会，期间自行解决交通和用餐',
          '110105199001011234', '622202100012345678',
          1, 'emp1001', '1001', '张三',
          'emp1001', '张三'
      ),
      (
          'reim002', 'REIM20250521002', 0, '武汉客户拜访差旅费', '拜访武汉本地客户洽谈合作',
          'emp1002', '1002', '李四',
          'dept002', 'D002', '销售部',
          'comp001', 'C001', '字节跳动科技有限公司',
          'biz002', 'B002', '客户拜访',
          0.00, 0.00, 0.00, 0.00,
          '',
          '420102198512125678', '621483100098765432',
          0, 'emp1002', '1002', '李四',
          'emp1002', '李四'
      ),
      (
          'reim003', 'REIM20250522003', 2, '宜昌项目验收差旅费', '赴宜昌进行项目验收',
          'emp1003', '1003', '王五',
          'dept003', 'D003', '技术部',
          'comp001', 'C001', '字节跳动科技有限公司',
          'biz003', 'B003', '项目实施',
          390.00, 150.00, 120.00, 120.00,
          '项目验收延期，已取消本次出差',
          '420502199208089012', '622700100045678901',
          2, 'emp1003', '1003', '王五',
          'emp1003', '王五'
      );

-- 3.2 插入补录行程表数据
INSERT INTO fk_reim_trip (
    id, reim_id, traveler_id, traveler_no, traveler_name,
    departure_city_id, departure_city_name, arrive_city_id, arrive_city_name, arrive_city_level,
    departure_date, arrive_date, trip_days, trip_desc
) VALUES
      (
          'trip001', 'reim001', 'emp1001', '1001', '张三',
          'city027', '武汉', 'city010', '北京', '1',
          '2025-05-21', '2025-05-23', 3, '武汉飞北京参加产品研讨会'
      ),
      (
          'trip002', 'reim002', 'emp1002', '1002', '李四',
          'city027', '武汉', 'city027', '武汉', '2',
          '2025-05-25', '2025-05-25', 1, '武汉本地客户拜访'
      ),
      (
          'trip003', 'reim003', 'emp1003', '1003', '王五',
          'city027', '武汉', 'city0717', '宜昌', '3',
          '2025-05-18', '2025-05-20', 3, '武汉高铁到宜昌进行项目验收'
      );

-- 3.3 插入补助信息表数据
INSERT INTO fk_reim_subsidy (
    id, reim_id, trip_id, traveler_id, traveler_name,
    trip_start_date, trip_end_date, subsidy_days,
    subsidy_city_id, subsidy_city_name,
    apply_amount, subsidy_amount, meal_subsidy, transport_subsidy, phone_subsidy
) VALUES
      (
          'subsidy001', 'reim001', 'trip001', 'emp1001', '张三',
          '2025-05-21', '2025-05-23', 3,
          'city010', '北京',
          540.00, 540.00, 300.00, 120.00, 120.00
      ),
      (
          'subsidy002', 'reim002', 'trip002', 'emp1002', '李四',
          '2025-05-25', '2025-05-25', 1,
          'city027', '武汉',
          160.00, 0.00, 0.00, 0.00, 0.00
      ),
      (
          'subsidy003', 'reim003', 'trip003', 'emp1003', '王五',
          '2025-05-18', '2025-05-20', 3,
          'city0717', '宜昌',
          390.00, 390.00, 150.00, 120.00, 120.00
      );

-- 3.4 插入补助日历表数据
INSERT INTO fk_reim_calendar (
    id, reim_id, subsidy_id, trip_date, week_day,
    subsidy_city_id, subsidy_city_name,
    meal_checked, meal_standard, meal_amount,
    transport_checked, transport_standard, transport_amount,
    phone_checked, phone_standard, phone_amount
) VALUES
      ('cal001', 'reim001', 'subsidy001', '2025-05-21', '星期三', 'city010', '北京', 1, 100.00, 100.00, 1, 40.00, 40.00, 1, 40.00, 40.00),
      ('cal002', 'reim001', 'subsidy001', '2025-05-22', '星期四', 'city010', '北京', 1, 100.00, 100.00, 1, 40.00, 40.00, 1, 40.00, 40.00),
      ('cal003', 'reim001', 'subsidy001', '2025-05-23', '星期五', 'city010', '北京', 1, 100.00, 100.00, 1, 40.00, 40.00, 1, 40.00, 40.00),
      ('cal004', 'reim002', 'subsidy002', '2025-05-25', '星期日', 'city027', '武汉', 0, 80.00, 0.00, 0, 40.00, 0.00, 0, 40.00, 0.00),
      ('cal005', 'reim003', 'subsidy003', '2025-05-18', '星期日', 'city0717', '宜昌', 1, 50.00, 50.00, 1, 40.00, 40.00, 1, 40.00, 40.00),
      ('cal006', 'reim003', 'subsidy003', '2025-05-19', '星期一', 'city0717', '宜昌', 1, 50.00, 50.00, 1, 40.00, 40.00, 1, 40.00, 40.00),
      ('cal007', 'reim003', 'subsidy003', '2025-05-20', '星期二', 'city0717', '宜昌', 1, 50.00, 50.00, 1, 40.00, 40.00, 1, 40.00, 40.00);


-- ----------------------------
-- Table structure for fk_async_task
-- ----------------------------
DROP TABLE IF EXISTS `fk_async_task`;
CREATE TABLE `fk_async_task` (
  `id` varchar(32) NOT NULL COMMENT '任务ID(UUID生成)',
  `task_name` varchar(100) NOT NULL COMMENT '任务名称',
  `task_type` varchar(50) NOT NULL COMMENT '任务类型(EXPORT等)',
  `status` int(4) NOT NULL DEFAULT 0 COMMENT '任务状态(0排队中 1处理中 2成功 3失败)',
  `progress` int(4) NOT NULL DEFAULT 0 COMMENT '执行进度百分比(0-100)',
  `file_url` varchar(1000) DEFAULT NULL COMMENT '处理成功后生成的文件下载地址',
  `error_msg` varchar(2000) DEFAULT NULL COMMENT '任务处理异常时的错误提示',
  `operator_id` varchar(32) NOT NULL COMMENT '提交人ID',
  `creation_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `completion_time` datetime DEFAULT NULL COMMENT '完成时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='6.1.8 异步任务中心表';

-- ----------------------------
-- Table structure for fk_mq_message
-- ----------------------------
DROP TABLE IF EXISTS `fk_mq_message`;
CREATE TABLE `fk_mq_message` (
  `id` varchar(32) NOT NULL COMMENT '主键ID',
  `business_id` varchar(32) NOT NULL COMMENT '业务单据ID',
  `topic` varchar(100) NOT NULL COMMENT 'MQ发送主题',
  `message_content` text NOT NULL COMMENT 'JSON格式的消息内容',
  `status` int(4) NOT NULL DEFAULT 0 COMMENT '发送状态(0待发送 1发送成功 2发送失败 3死信)',
  `retry_count` int(4) NOT NULL DEFAULT 0 COMMENT '重试次数',
  `max_retry` int(4) NOT NULL DEFAULT 3 COMMENT '最大重试次数',
  `next_retry_time` datetime DEFAULT NULL COMMENT '下次重试时间',
  `creation_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='6.1.7 MQ可靠消息投递表';

-- 3.5 插入费用分摊表数据
INSERT INTO fk_reim_split (
    id, reim_id, sort_no, company_id, company_name,
    project_id, project_name, split_ratio, split_amount
) VALUES
      (
          'split001', 'reim001', 1, 'comp001', '字节跳动科技有限公司',
          'proj001', '产品研发项目', 1.0000, 540.00
      ),
      (
          'split002', 'reim002', 1, 'comp001', '字节跳动科技有限公司',
          'proj002', '销售拓展项目', 1.0000, 0.00
      ),
      (
          'split003', 'reim003', 1, 'comp001', '字节跳动科技有限公司',
          'proj003', '宜昌项目', 0.6000, 234.00
      ),
      (
          'split004', 'reim003', 2, 'comp002', '字节跳动（宜昌）有限公司',
          'proj003', '宜昌项目', 0.4000, 156.00
      );

COMMIT;

-- =============================================
-- 4. 数据一致性校验（执行后查看结果是否全部为"一致/正确"）
-- =============================================
SELECT '=== 报销单总金额校验 ===' AS 校验项;
SELECT
    m.reim_no,
    m.subsidy_total AS 主表总金额,
    SUM(s.subsidy_amount) AS 补助表总金额,
    CASE WHEN m.subsidy_total = SUM(s.subsidy_amount) THEN '一致' ELSE '不一致' END AS 校验结果
FROM fk_reim_main m
         LEFT JOIN fk_reim_subsidy s ON m.id = s.reim_id
GROUP BY m.id, m.reim_no, m.subsidy_total;

SELECT '=== 费用分摊比例校验 ===' AS 校验项;
SELECT
    m.reim_no,
    SUM(sp.split_ratio) AS 总分摊比例,
    CASE WHEN SUM(sp.split_ratio) = 1.0000 THEN '正确' ELSE '错误' END AS 校验结果
FROM fk_reim_main m
         LEFT JOIN fk_reim_split sp ON m.id = sp.reim_id
GROUP BY m.id, m.reim_no;

SELECT '=== 补助明细金额校验 ===' AS 校验项;
SELECT
    s.id AS 补助ID,
    s.subsidy_amount AS 补助表金额,
    SUM(c.meal_amount + c.transport_amount + c.phone_amount) AS 日历表金额,
    CASE WHEN s.subsidy_amount = SUM(c.meal_amount + c.transport_amount + c.phone_amount) THEN '一致' ELSE '不一致' END AS 校验结果
FROM fk_reim_subsidy s
         LEFT JOIN fk_reim_calendar c ON s.id = c.subsidy_id
GROUP BY s.id, s.subsidy_amount;

-- 恢复SQL环境配置
SET FOREIGN_KEY_CHECKS = 1;
SET AUTOCOMMIT = 1;

-- =============================================
-- 执行完成提示
-- =============================================
SELECT '费控云差旅报销单数据库初始化完成！共创建5张表，插入3条报销单、3条行程、3条补助、7条日历、4条分摊数据' AS 执行结果;