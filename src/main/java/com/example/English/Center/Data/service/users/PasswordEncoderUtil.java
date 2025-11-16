package com.example.English.Center.Data.service.users;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PasswordEncoderUtil {
    public static String encodePassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(password.getBytes());

            byte[] bytes = md.digest();

            // Chuyển sang dạng hex string
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b & 0xff));
            }

            return sb.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Không thể mã hóa mật khẩu", e);
        }
    }
}

