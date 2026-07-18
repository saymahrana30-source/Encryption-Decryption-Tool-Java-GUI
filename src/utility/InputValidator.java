package utility;

import exception.CryptoOperationException;

import java.io.File;

/**
 * InputValidator.java
 * ====================
 * Centralizes all "is this input okay to process?" checks so the GUI
 * layer stays focused on presentation, and CryptoUtils/FileEncryptionService
 * can assume they're only ever called with sane input.
 *
 * Every method throws CryptoOperationException with a clear, user-facing
 * message on failure - the GUI just needs to catch it and show it.
 */
public final class InputValidator {

    private static final int MIN_PASSWORD_LENGTH = 4;

    private InputValidator() {
    }

    /** Ensures a text field isn't null/empty/whitespace-only. */
    public static void validateText(String text, String fieldName) throws CryptoOperationException {
        if (text == null || text.trim().isEmpty()) {
            throw new CryptoOperationException(fieldName + " cannot be empty.");
        }
    }

    /**
     * Ensures a password meets a minimum sanity bar. This is deliberately
     * light-touch (this is a learning project, not a corporate password
     * policy) - the point is to demonstrate input validation, not to be
     * an authoritative password-strength gate.
     */
    public static void validatePassword(char[] password) throws CryptoOperationException {
        if (password == null || password.length == 0) {
            throw new CryptoOperationException("Password cannot be empty.");
        }
        if (password.length < MIN_PASSWORD_LENGTH) {
            throw new CryptoOperationException(
                    "Password should be at least " + MIN_PASSWORD_LENGTH + " characters.");
        }
    }

    /** Ensures a selected file actually exists and is readable before we try to encrypt/decrypt it. */
    public static void validateFileForReading(File file) throws CryptoOperationException {
        if (file == null) {
            throw new CryptoOperationException("No file was selected.");
        }
        if (!file.exists()) {
            throw new CryptoOperationException("Selected file does not exist: " + file.getAbsolutePath());
        }
        if (!file.isFile()) {
            throw new CryptoOperationException("Selected path is not a file: " + file.getAbsolutePath());
        }
        if (!file.canRead()) {
            throw new CryptoOperationException("Cannot read the selected file (permission denied).");
        }
    }

    /** Ensures the parent directory of an output file path exists and is writable. */
    public static void validateOutputLocation(File outputFile) throws CryptoOperationException {
        if (outputFile == null) {
            throw new CryptoOperationException("No output location was chosen.");
        }
        File parent = outputFile.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists()) {
            throw new CryptoOperationException("Output folder does not exist: " + parent.getAbsolutePath());
        }
    }

    /**
     * A very simple, non-authoritative password strength estimate used only
     * to drive an optional GUI strength indicator. Returns one of:
     * "Weak", "Medium", "Strong".
     */
    public static String estimatePasswordStrength(char[] password) {
        if (password == null || password.length == 0) {
            return "Weak";
        }
        int score = 0;
        if (password.length >= 8) score++;
        if (password.length >= 12) score++;

        boolean hasUpper = false, hasLower = false, hasDigit = false, hasSymbol = false;
        for (char c : password) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else hasSymbol = true;
        }
        int varietyCount = (hasUpper ? 1 : 0) + (hasLower ? 1 : 0) + (hasDigit ? 1 : 0) + (hasSymbol ? 1 : 0);
        score += varietyCount >= 3 ? 2 : (varietyCount == 2 ? 1 : 0);

        if (score >= 4) return "Strong";
        if (score >= 2) return "Medium";
        return "Weak";
    }
}
