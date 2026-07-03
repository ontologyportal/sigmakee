package com.articulate.sigma.user;

import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Locale;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.MessageDigest;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class PasswordService {

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final int RESET_TOKEN_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String PBKDF2_PREFIX = "pbkdf2_sha256";
    private static final int PBKDF2_ITERATIONS = 310000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;
    
    /********************************************************************
     * Generates a URL-safe random password reset token.
     * @return a URL-safe reset token
     */
    public static String generateResetToken() {

        byte[] bytes = new byte[RESET_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /********************************************************************
     * Hashes a password reset token before database storage.
     * @param token the raw reset token
     * @return a SHA-256 hex hash of the reset token
     */
    public static String hashResetToken(String token) {

        if (token == null) return "";
        return sha256Hex(token);
    }

    /********************************************************************
     * Hashes a password using salted PBKDF2-HMAC-SHA256.
     * Stored format: pbkdf2_sha256:iterations:base64salt:base64hash
     * @param password raw password
     * @return encoded salted password hash
     */
    public static String hashPassword(String password) {

        if (password == null || password.isEmpty()) throw new IllegalArgumentException("Password cannot be empty");
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(password.toCharArray(), salt, PBKDF2_ITERATIONS, HASH_BITS);
        return PBKDF2_PREFIX + ":" +
                PBKDF2_ITERATIONS + ":" +
                Base64.getEncoder().encodeToString(salt) + ":" +
                Base64.getEncoder().encodeToString(hash);
    }

    /********************************************************************
     * Verifies raw password against current salted hashes and legacy hashes.
     * @param password raw password
     * @param storedHash stored password hash
     * @return true if password matches
     */
    public static boolean verifyPassword(String password, String storedHash) {

        if (password == null || storedHash == null || storedHash.isEmpty()) return false;
        if (storedHash.startsWith(PBKDF2_PREFIX + ":")) {
            String[] parts = storedHash.split(":");
            if (parts.length != 4) return false;
            int iterations;
            try {
                iterations = Integer.parseInt(parts[1]);
            }
            catch (NumberFormatException e) {
                return false;
            }
            byte[] salt;
            byte[] expected;
            try {
                salt = Base64.getDecoder().decode(parts[2]);
                expected = Base64.getDecoder().decode(parts[3]);
            }
            catch (IllegalArgumentException e) {
                return false;
            }
            byte[] actual = pbkdf2(password.toCharArray(), salt, iterations, expected.length * 8);
            return MessageDigest.isEqual(expected, actual);
        }
        if (isLegacySha1Hash(storedHash)) return MessageDigest.isEqual(storedHash.getBytes(StandardCharsets.UTF_8), shaHex(password, "SHA-1").getBytes(StandardCharsets.UTF_8));
        if (isLegacySha256Hash(storedHash)) return MessageDigest.isEqual(storedHash.getBytes(StandardCharsets.UTF_8), shaHex(password, "SHA-256").getBytes(StandardCharsets.UTF_8));
        return false;
    }

    /********************************************************************
     * @return true if hash is an old unsalted SHA-256 hex digest
     */
    public static boolean isLegacySha256Hash(String storedHash) {

        return storedHash != null && storedHash.matches("^[a-fA-F0-9]{64}$");
    }

    private static String shaHex(String password, String algorithm) {

        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] bytes = md.digest(password.trim().getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        }
        catch (Exception e) {
            throw new RuntimeException("Failed legacy hash using " + algorithm, e);
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int hashBits) {

        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, hashBits);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return skf.generateSecret(spec).getEncoded();
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to hash password", e);
        }
    }

    /********************************************************************
     * @return true if stored hash should be upgraded after successful login
     */
    public static boolean needsPasswordRehash(String storedHash) {

        if (storedHash == null) return true;
        if (isLegacySha1Hash(storedHash) || isLegacySha256Hash(storedHash)) return true;
        if (!storedHash.startsWith(PBKDF2_PREFIX + ":")) return true;
        String[] parts = storedHash.split(":");
        if (parts.length != 4) return true;
        try {
            return Integer.parseInt(parts[1]) < PBKDF2_ITERATIONS;
        }
        catch (NumberFormatException e) {
            return true;
        }
    }

    /********************************************************************
     * Checks whether a stored hash is a legacy SHA-1 password hash.
     * @param storedHash the stored password hash
     * @return true if the hash appears to be a legacy SHA-1 hash
     */
    public static boolean isLegacySha1Hash(String storedHash) {

        return storedHash != null && storedHash.matches("^[a-fA-F0-9]{40}$");
    }

    /********************************************************************
     * Hashes a string with SHA-256 and returns lowercase hex.
     * @param value the value to hash
     * @return lowercase SHA-256 hex
     */
    private static String sha256Hex(String value) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return toHex(hash);
        }
        catch (NoSuchAlgorithmException nsae) {
            throw new RuntimeException("SHA-256 is not available", nsae);
        }
    }

    /********************************************************************
     * Converts bytes to lowercase hex.
     * @param bytes the bytes to encode
     * @return lowercase hex string
     */
    private static String toHex(byte[] bytes) {

        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    /********************************************************************
     * Hashes a password using the legacy SHA-1 algorithm.
     * @deprecated use hashPassword(String) for new password hashes
     * @param password the password to hash
     * @return the legacy SHA-1 password hash
     */
    @Deprecated
    public static synchronized String encrypt(String password) {

        return encryptLegacySha1(password);
    }

    /********************************************************************
     * Hashes a password using legacy SHA-1 for backward compatibility.
     * @param password the password to hash
     * @return the legacy SHA-1 password hash
     */
    private static String encryptLegacySha1(String password) {

        if (password == null) return "";

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(password.trim().getBytes(StandardCharsets.UTF_8));
            return toHex(md.digest());
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 is not available", e);
        }
    }

    /********************************************************************
     * Prints command-line usage options for PasswordService.
     */
    public static void showHelp() {

        System.out.println("PasswordService:");
        System.out.println("-h    show this help message");
        System.out.println("-e    hash password using SHA-256");
    }

    /********************************************************************
     * Runs command-line password hashing utilities.
     * @param args command-line arguments
     */
    public static void main(String[] args) {

        if (args != null && args.length > 0) {
            if ("-h".equals(args[0])) {
                showHelp();
            }
            else if ("-e".equals(args[0])) {
                String password = new String(System.console().readPassword("    Enter Password to Hash: "));
                System.out.println("    SHA-256 password hash: " + PasswordService.hashPassword(password));
            }
            else {
                showHelp();
            }
        }
        else {
            showHelp();
        }
    }
}