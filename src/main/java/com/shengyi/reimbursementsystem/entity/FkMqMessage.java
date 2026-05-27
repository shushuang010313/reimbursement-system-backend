package com.shengyi.reimbursementsystem.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.shengyi.reimbursementsystem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * MQ可靠消息投递表 (本地消息表)
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("fk_mq_message")
public class FkMqMessage extends BaseEntity {

    /**
     * 业务单据ID
     */
    @TableField("business_id")
    private String businessId;

    /**
     * MQ发送主题
     */
    @TableField("topic")
    private String topic;

    /**
     * JSON格式的消息内容
     */
    @TableField("message_content")
    private String messageContent;

    /**
     * 发送状态(0待发送 1发送成功 2发送失败 3死信)
     */
    @TableField("status")
    private Integer status;

    /**
     * 重试次数
     */
    @TableField("retry_count")
    private Integer retryCount;

    /**
     * 最大重试次数
     */
    @TableField("max_retry")
    private Integer maxRetry;

    /**
     * 下次重试时间
     */
    @TableField("next_retry_time")
    private Date nextRetryTime;

    /**
     * 最后更新时间
     */
    @TableField("update_time")
    private Date updateTime;
}
