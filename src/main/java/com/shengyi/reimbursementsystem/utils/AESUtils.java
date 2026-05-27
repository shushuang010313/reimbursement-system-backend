package com.shengyi.reimbursementsystem.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 简单的 AES 加解密工具类，用于数据库层面的透明加密
 */
public class AESUtils {

    // 默认密钥（16字节，实际生产环境中应配置在 application.yml 中并从环境变量获取）
    private static final String DEFAULT_SECRET_KEY = "ShengYi123456789";
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

    /**
     * 加密
     *
     * @param plainText 明文
     * @return Base64 编码的密文
     */
    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(DEFAULT_SECRET_KEY.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("AES encryption failed", e);
        }
    }

    /**
     * 解密
     *
     * @param cipherText Base64 编码的密文
     * @return 明文
     */
    public static String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) {
            return cipherText;
        }
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(DEFAULT_SECRET_KEY.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            byte[] decodedBytes = Base64.getDecoder().decode(cipherText);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 如果解密失败，可能是因为数据库中存的本身就是明文（旧数据），或者格式不对，退化为直接返回原字符串
            return cipherText;
        }
    }
}
