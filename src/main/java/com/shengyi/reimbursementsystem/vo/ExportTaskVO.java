package com.shengyi.reimbursementsystem.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
public class ExportTaskVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 任务状态：PROCESSING, SUCCESS, FAILED
     */
    private String status;

    /**
     * 进度百分比 (0-100)
     */
    private Integer progress;

    /**
     * 进度描述，如 "已导出 5000 / 10000 条"
     */
    private String message;

    /**
     * 生成的文件名（用于下载）
     */
    private String fileName;
}
