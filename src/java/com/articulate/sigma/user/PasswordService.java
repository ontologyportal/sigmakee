package com.articulate.sigma.user;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import com.articulate.sigma.user.UserManager;

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
     * Hashes a password using SHA-1.
     * @deprecated
     * @param password the password to hash
     * @return the hashed password string
     */
    public static synchronized String encrypt(String password) {

        //if(debug>0) System.out.printf("UserDatabase.encrypt(%s)", password);
        StringBuilder sb = new StringBuilder();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(password.trim().getBytes());
            byte[] bytes = md.digest();
            for (int i = 0; i < bytes.length; i++) sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    /********************************************************************
     * Prints command-line usage options for UserManager.
     */
    public static void showHelp() {

        System.out.println("UserManager: ");
        System.out.println("-h    show this help message");
        System.out.println("-e    encyrpt password to sha256");
    }

    public static void main(String[] args) {

        if (args != null && args.length > 0) {
            if (args[0].equals("-h")) showHelp();
            if (args[0].equals("-e")) {
                String password = new String(System.console().readLine("    Enter Password to Encrypt: "));
                System.out.println("    Encrypted password: " + PasswordService.sha256Hex(password));
            }
            else showHelp();
        }
        else showHelp();
    }
}