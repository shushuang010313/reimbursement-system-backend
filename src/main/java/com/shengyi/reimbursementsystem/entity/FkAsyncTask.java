package com.shengyi.reimbursementsystem.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.shengyi.reimbursementsystem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * 异步任务中心表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("fk_async_task")
public class FkAsyncTask extends BaseEntity {

    /**
     * 任务名称
     */
    @TableField("task_name")
    private String taskName;

    /**
     * 任务类型(EXPORT等)
     */
    @TableField("task_type")
    private String taskType;

    /**
     * 任务状态(0排队中 1处理中 2成功 3失败)
     */
    @TableField("status")
    private Integer status;

    /**
     * 执行进度百分比(0-100)
     */
    @TableField("progress")
    private Integer progress;

    /**
     * 处理成功后生成的文件下载地址
     */
    @TableField("file_url")
    private String fileUrl;

    /**
     * 任务处理异常时的错误提示
     */
    @TableField("error_msg")
    private String errorMsg;

    /**
     * 提交人ID
     */
    @TableField("operator_id")
    private String operatorId;

    /**
     * 完成时间
     */
    @TableField("completion_time")
    private Date completionTime;
}
