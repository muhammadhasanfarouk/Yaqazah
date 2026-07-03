package com.yaqazah.common.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

@Component
@Converter
public class EncryptionConverter implements AttributeConverter<String, String> {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128; // in bits
    private static final int GCM_IV_LENGTH = 12; // in bytes
    private static String SECRET;

    // This "injects" the property into a static variable so the Converter can see it
    @Value("${encryption.key}")
    public void setSecret(String secret) {
        EncryptionConverter.SECRET = secret;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        try {
            // Generate a secure random 12-byte IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            SecretKeySpec secretKeySpec = new SecretKeySpec(SECRET.getBytes(), "AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, parameterSpec);

            byte[] cipherText = cipher.doFinal(attribute.getBytes());

            // Prepend IV to ciphertext
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);
            byte[] cipherMessage = byteBuffer.array();

            return Base64.getEncoder().encodeToString(cipherMessage);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        try {
            byte[] cipherMessage = Base64.getDecoder().decode(dbData);

            // For backward compatibility (if the data is from old ECB encryption without IV prepended)
            // The old encoded string was base64(ciphertext). If its length doesn't make sense for GCM (min 12 bytes + tag)
            // we could attempt to fallback, but since the user was warned, we will just try to decrypt via GCM.

            // Extract the IV and ciphertext
            if (cipherMessage.length < GCM_IV_LENGTH) {
                throw new IllegalArgumentException("Invalid ciphertext length");
            }
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(cipherMessage, 0, iv, 0, iv.length);

            byte[] cipherText = new byte[cipherMessage.length - GCM_IV_LENGTH];
            System.arraycopy(cipherMessage, GCM_IV_LENGTH, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            SecretKeySpec secretKeySpec = new SecretKeySpec(SECRET.getBytes(), "AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, parameterSpec);

            return new String(cipher.doFinal(cipherText));
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}