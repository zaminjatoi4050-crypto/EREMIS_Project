package com.eremis.utils;

import com.eremis.config.AppConfig;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Lightweight AES-GCM helper for sensitive payment data.
 */
public final class EncryptionUtil {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private EncryptionUtil() {}

    public static String encrypt(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            byte[] iv = new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to encrypt sensitive data.", e);
        }
    }

    public static String decrypt(String encryptedValue) {
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedValue);
            byte[] iv = new byte[IV_LENGTH];
            byte[] payload = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            System.arraycopy(combined, iv.length, payload, 0, payload.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(payload), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to decrypt sensitive data.", e);
        }
    }

    public static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) return "";
        String digits = accountNumber.replaceAll("\\s+", "");
        if (digits.length() <= 4) return "****";
        return "**** **** **** " + digits.substring(digits.length() - 4);
    }

    private static SecretKeySpec secretKey() throws Exception {
        String secret = AppConfig.getInstance().getPaymentEncryptionKey();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("Missing payment encryption key. Set app.payment.encryptionKey or EREMIS_PAYMENT_KEY.");
        }
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] key = sha.digest(secret.getBytes(StandardCharsets.UTF_8));
        byte[] normalized = new byte[32];
        System.arraycopy(key, 0, normalized, 0, normalized.length); // AES-256
        return new SecretKeySpec(normalized, "AES");
    }
}