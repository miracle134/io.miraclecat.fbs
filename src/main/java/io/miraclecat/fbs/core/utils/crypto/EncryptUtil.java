package io.miraclecat.fbs.core.utils.crypto;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * packageName    : io.miraclecat.fbs.core.utils.crypto
 * fileName       : EncryptUtil
 * author         : MiracleCat 
 * date           : 25. 8. 27.
 * description    :
 */
@Slf4j
public class EncryptUtil {

    private static final int IV_SIZE = 12;
    private static final int TAG_LENGTH_BIT = 128;
    private static final String AES_CIPHER = "AES/GCM/NoPadding";
    private static final String RSA_CIPHER = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";

    // 시스템 키 (환경변수에서 로드)
    private static final SecretKey SYSTEM_AES_KEY;
    private static final PublicKey SYSTEM_RSA_PUBLIC_KEY;
    private static final PrivateKey SYSTEM_RSA_PRIVATE_KEY;

    static {
        try {
            String aesKey = System.getProperty("AES_KEY");
            String rsaPublicKey = System.getProperty("RSA_PUBLIC_KEY");
            String rsaPrivateKey = System.getProperty("RSA_PRIVATE_KEY");

            // 키가 없으면 경고 로그만 출력하고 null로 설정
            if (aesKey == null) {
                log.warn("VM 변수 AES_KEY가 설정되지 않았습니다. 시스템 키를 사용할 수 없습니다.");
                SYSTEM_AES_KEY = null;
            } else {
                SYSTEM_AES_KEY = new SecretKeySpec(Base64.getDecoder().decode(aesKey), "AES");
            }

            if (rsaPublicKey == null || rsaPrivateKey == null) {
                log.warn("VM 변수 RSA 키가 설정되지 않았습니다. 시스템 키를 사용할 수 없습니다.");
                SYSTEM_RSA_PUBLIC_KEY = null;
                SYSTEM_RSA_PRIVATE_KEY = null;
            } else {
                KeyFactory factory = KeyFactory.getInstance("RSA");
                X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(Base64.getDecoder().decode(rsaPublicKey));
                PKCS8EncodedKeySpec priSpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(rsaPrivateKey));
                SYSTEM_RSA_PUBLIC_KEY = factory.generatePublic(pubSpec);
                SYSTEM_RSA_PRIVATE_KEY = factory.generatePrivate(priSpec);
            }
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            log.error("암호화 키 초기화 중 오류 발생", e);
            throw new RuntimeException("암호화 키 초기화 실패", e);
        }
    }

    // =============================================================================
    // Base64 인코딩/디코딩
    // =============================================================================

    /**
     * 주어진 문자열을 Base64로 인코딩합니다.
     */
    public static String base64Encode(String data) {
        if (data == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Base64로 인코딩된 문자열을 디코딩합니다.
     */
    public static String base64Decode(String base64Data) {
        if (base64Data == null) {
            return null;
        }
        return new String(Base64.getDecoder().decode(base64Data), StandardCharsets.UTF_8);
    }

    /**
     * Base64로 인코딩된 문자열을 바이트 배열로 디코딩합니다.
     */
    public static byte[] base64DecodeToBytes(String base64Data) {
        if (base64Data == null) {
            return null;
        }
        return Base64.getDecoder().decode(base64Data);
    }

    // =============================================================================
    // AES 암호화/복호화 (시스템 키 사용)
    // =============================================================================

    /**
     * 시스템 AES 키를 사용하여 문자열을 암호화합니다.
     */
    public static String encryptAES(String plainText) throws Exception {
        if (SYSTEM_AES_KEY == null) {
            throw new IllegalStateException("시스템 AES 키가 설정되지 않았습니다");
        }
        return encryptAES(plainText, SYSTEM_AES_KEY);
    }

    /**
     * 시스템 AES 키를 사용하여 암호화된 문자열을 복호화합니다.
     */
    public static String decryptAES(String encryptedText) throws Exception {
        if (SYSTEM_AES_KEY == null) {
            throw new IllegalStateException("시스템 AES 키가 설정되지 않았습니다");
        }
        return decryptAES(encryptedText, SYSTEM_AES_KEY);
    }

    // =============================================================================
    // AES 암호화/복호화 (사용자 키 사용)
    // =============================================================================

    /**
     * 지정된 AES 키를 사용하여 문자열을 암호화합니다.
     */
    public static String encryptAES(String plainText, SecretKey secretKey) throws Exception {
        if (plainText == null || secretKey == null) {
            throw new IllegalArgumentException("평문과 키는 null일 수 없습니다");
        }

        Cipher cipher = Cipher.getInstance(AES_CIPHER);
        
        byte[] iv = new byte[IV_SIZE];
        new SecureRandom().nextBytes(iv);
        
        GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);
        
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        
        // IV와 암호문을 결합하여 하나의 Base64 문자열로 반환
        byte[] ivAndCipherText = new byte[iv.length + encryptedBytes.length];
        System.arraycopy(iv, 0, ivAndCipherText, 0, iv.length);
        System.arraycopy(encryptedBytes, 0, ivAndCipherText, iv.length, encryptedBytes.length);
        
        return Base64.getEncoder().encodeToString(ivAndCipherText);
    }

    /**
     * 지정된 AES 키를 사용하여 암호화된 문자열을 복호화합니다.
     */
    public static String decryptAES(String encryptedText, SecretKey secretKey) throws Exception {
        if (encryptedText == null || secretKey == null) {
            throw new IllegalArgumentException("암호문과 키는 null일 수 없습니다");
        }

        byte[] ivAndCipherText = Base64.getDecoder().decode(encryptedText);
        
        // IV와 암호문 분리
        byte[] iv = new byte[IV_SIZE];
        byte[] cipherText = new byte[ivAndCipherText.length - IV_SIZE];
        System.arraycopy(ivAndCipherText, 0, iv, 0, IV_SIZE);
        System.arraycopy(ivAndCipherText, IV_SIZE, cipherText, 0, cipherText.length);
        
        Cipher cipher = Cipher.getInstance(AES_CIPHER);
        GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
        
        byte[] decryptedBytes = cipher.doFinal(cipherText);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    // =============================================================================
    // RSA 암호화/복호화
    // =============================================================================

    /**
     * 시스템 RSA 공개키를 사용하여 문자열을 암호화합니다.
     */
    public static String encryptRSA(String plainText) throws Exception {
        if (SYSTEM_RSA_PUBLIC_KEY == null) {
            throw new IllegalStateException("시스템 RSA 공개키가 설정되지 않았습니다");
        }
        return encryptRSA(plainText, SYSTEM_RSA_PUBLIC_KEY);
    }

    /**
     * 시스템 RSA 개인키를 사용하여 암호화된 문자열을 복호화합니다.
     */
    public static String decryptRSA(String encryptedText) throws Exception {
        if (SYSTEM_RSA_PRIVATE_KEY == null) {
            throw new IllegalStateException("시스템 RSA 개인키가 설정되지 않았습니다");
        }
        return decryptRSA(encryptedText, SYSTEM_RSA_PRIVATE_KEY);
    }

    /**
     * RSA 공개키를 사용하여 문자열을 암호화합니다.
     */
    public static String encryptRSA(String plainText, PublicKey publicKey) throws Exception {
        if (plainText == null || publicKey == null) {
            throw new IllegalArgumentException("평문과 공개키는 null일 수 없습니다");
        }

        Cipher cipher = Cipher.getInstance(RSA_CIPHER);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    /**
     * RSA 개인키를 사용하여 암호화된 문자열을 복호화합니다.
     */
    public static String decryptRSA(String encryptedText, PrivateKey privateKey) throws Exception {
        if (encryptedText == null || privateKey == null) {
            throw new IllegalArgumentException("암호문과 개인키는 null일 수 없습니다");
        }

        Cipher cipher = Cipher.getInstance(RSA_CIPHER);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    // =============================================================================
    // 키 생성 유틸리티
    // =============================================================================

    /**
     * AES 키를 생성합니다.
     */
    public static SecretKey generateAESKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        return keyGen.generateKey();
    }

    /**
     * RSA 키 페어를 생성합니다.
     */
    public static KeyPair generateRSAKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        return keyGen.generateKeyPair();
    }

    /**
     * 보안 랜덤 API 키를 생성합니다.
     */
    public static String generateSecureApiKey(int keySize) {
        SecureRandom secureRandom = new SecureRandom();
        byte[] bytes = new byte[keySize];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 기본 크기(32바이트)의 보안 API 키를 생성합니다.
     */
    public static String generateSecureApiKey() {
        return generateSecureApiKey(32);
    }

    /**
     * IV (Initialization Vector)를 생성합니다.
     */
    public static byte[] generateIV() {
        byte[] iv = new byte[IV_SIZE];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    public static SecretKey getSystemAESKey() {
        return SYSTEM_AES_KEY;
    }

    public static PublicKey getSystemRSAPublicKey() {
        return SYSTEM_RSA_PUBLIC_KEY;
    }

    public static PrivateKey getSystemRSAPrivateKey() {
        return SYSTEM_RSA_PRIVATE_KEY;
    }
}