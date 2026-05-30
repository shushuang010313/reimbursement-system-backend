package com.shengyi.reimbursementsystem.common;

/**
 * 全局用户上下文（基于 ThreadLocal）
 * 【答辩重点】这是基础架构底座的一部分，负责用户信息安全上下文透传。
 */
public class UserContext {
    // 【答辩重点】为什么用 ThreadLocal？
    // 因为 Tomcat/Spring 默认是单例多线程模型，每个 HTTP 请求由一个独立的线程处理。
    // ThreadLocal 能够提供“线程级别”的局部变量，确保不同用户的请求之间 userId 相互隔离，绝对不会发生串号、并发安全问题。
    private static final ThreadLocal<String> USER_ID_HOLDER = new ThreadLocal<>();

    public static void setUserId(String userId) {
        USER_ID_HOLDER.set(userId);
    }

    public static String getUserId() {
        return USER_ID_HOLDER.get();
    }

    /**
     * 【答辩重点】为什么要 clear()？
     * 如果评委问：用 ThreadLocal 会有什么隐患？
     * 答：会有内存泄漏风险和数据污染风险。因为 Tomcat 使用了线程池，线程会被复用。
     * 如果当前请求结束时不调用 remove() 清理，下一次该线程处理新请求时，就会读到上一个用户的脏数据。
     * 因此，必须在拦截器的 afterCompletion 中调用 clear()。
     */
    public static void clear() {
        USER_ID_HOLDER.remove();
    }
}
