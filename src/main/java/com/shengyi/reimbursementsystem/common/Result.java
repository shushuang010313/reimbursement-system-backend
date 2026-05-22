package com.shengyi.reimbursementsystem.common;

import lombok.Data;
import java.io.Serializable;

/**
 * 统一返回结果类
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 状态码：200 成功，其他为失败
     */
    private Integer code;

    /**
     * 返回信息
     */
    private String message;

    /**
     * 数据对象
     */
    private T data;

    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        return result;
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    public static <T> Result<T> error(ErrorCodeEnum errorCodeEnum) {
        Result<T> result = new Result<>();
        result.setCode(errorCodeEnum.getCode());
        result.setMessage(errorCodeEnum.getMessage());
        return result;
    }
}
