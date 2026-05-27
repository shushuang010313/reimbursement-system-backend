package com.shengyi.reimbursementsystem.common;

/**
 * 全局用户上下文（基于 ThreadLocal）
 */
public class UserContext {
    private static final ThreadLocal<String> USER_ID_HOLDER = new ThreadLocal<>();

    public static void setUserId(String userId) {
        USER_ID_HOLDER.set(userId);
    }

    public static String getUserId() {
        return USER_ID_HOLDER.get();
    }

    public static void clear() {
        USER_ID_HOLDER.remove();
    }
}
