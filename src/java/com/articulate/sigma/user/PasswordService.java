package com.articulate.sigma.user;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;

public class PasswordService {

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final int RESET_TOKEN_BYTES = 32;

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
     * Hashes a password using SHA-256.
     * @param password the password to hash
     * @return lowercase SHA-256 hex
     */
    public static String hashPassword(String password) {

        if (password == null) return "";
        return sha256Hex(password.trim());
    }

    /********************************************************************
     * Checks whether a raw password matches either a SHA-256 or legacy SHA-1 hash.
     * @param password the raw password submitted by the user
     * @param storedHash the password hash stored in the database
     * @return true if the password matches
     */
    public static boolean verifyPassword(String password, String storedHash) {

        if (password == null || storedHash == null) return false;

        String normalizedHash = storedHash.trim().toLowerCase(Locale.ROOT);

        if (hashPassword(password).equals(normalizedHash)) return true;

        return encryptLegacySha1(password).equals(normalizedHash);
    }

    /********************************************************************
     * Checks whether a stored hash is a legacy SHA-1 password hash.
     * @param storedHash the stored password hash
     * @return true if the hash appears to be a legacy SHA-1 hash
     */
    public static boolean isLegacySha1Hash(String storedHash) {

        if (storedHash == null) return false;
        return storedHash.trim().matches("(?i)[0-9a-f]{40}");
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