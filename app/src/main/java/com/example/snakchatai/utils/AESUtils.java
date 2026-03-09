package com.example.snakchatai.utils;

import android.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class AESUtils {
    // ⚠️ Is key ko kisi ko mat batana, ye 16 chars ki honi chahiye
    private static final String AES_KEY = "SnakeChat_Secret";

    public static String encrypt(String text) {
        try {
            SecretKeySpec sks = new SecretKeySpec(AES_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, sks);
            byte[] encrypted = cipher.doFinal(text.getBytes());
            return Base64.encodeToString(encrypted, Base64.DEFAULT);
        } catch (Exception e) {
            return text; // Fallback to plain text if error
        }
    }

    public static String decrypt(String encryptedText) {
        try {
            SecretKeySpec sks = new SecretKeySpec(AES_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, sks);
            byte[] decoded = Base64.decode(encryptedText, Base64.DEFAULT);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted);
        } catch (Exception e) {
            return encryptedText; // If it's already plain or error
        }
    }
}