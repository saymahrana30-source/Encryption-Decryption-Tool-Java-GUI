package service;

import crypto.CryptoUtils;
import exception.CryptoOperationException;
import utility.InputValidator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * FileEncryptionService.java
 * ===========================
 * Handles all FILE-level operations: reading a file into memory, calling
 * CryptoUtils to encrypt/decrypt the raw bytes, and writing the result to
 * a new output file. Keeps file I/O concerns separate from both the
 * crypto math (CryptoUtils) and the GUI (MainWindow).
 *
 * OUTPUT FILE FORMAT: identical byte layout to text mode -
 *   [ 16-byte salt ][ 12-byte IV ][ ciphertext + 16-byte GCM tag ]
 * written as raw bytes (no Base64) since files don't need to be
 * copy-pasteable the way text does.
 */
public class FileEncryptionService {

    /**
     * Encrypts an input file and writes the encrypted bytes to outputFile.
     * By convention the caller names the output with a ".enc" extension.
     */
    public void encryptFile(File inputFile, File outputFile, char[] password) throws CryptoOperationException {
        InputValidator.validateFileForReading(inputFile);
        InputValidator.validateOutputLocation(outputFile);
        InputValidator.validatePassword(password);

        try {
            byte[] plaintext = Files.readAllBytes(inputFile.toPath());
            byte[] encrypted = CryptoUtils.encryptBytes(plaintext, password);
            Files.write(outputFile.toPath(), encrypted);
        } catch (IOException e) {
            throw new CryptoOperationException("Failed to read/write file during encryption: " + e.getMessage(), e);
        }
    }

    /**
     * Decrypts a previously-encrypted file and writes the restored bytes
     * to outputFile. If the password is wrong or the file was tampered
     * with, CryptoUtils will throw a clear "wrong password" exception
     * rather than writing corrupted output.
     */
    public void decryptFile(File inputFile, File outputFile, char[] password) throws CryptoOperationException {
        InputValidator.validateFileForReading(inputFile);
        InputValidator.validateOutputLocation(outputFile);
        InputValidator.validatePassword(password);

        try {
            byte[] encrypted = Files.readAllBytes(inputFile.toPath());
            byte[] plaintext = CryptoUtils.decryptBytes(encrypted, password);
            Files.write(outputFile.toPath(), plaintext);
        } catch (IOException e) {
            throw new CryptoOperationException("Failed to read/write file during decryption: " + e.getMessage(), e);
        }
    }

    /** Suggests a sensible default output filename for an encryption operation. */
    public static File suggestEncryptedOutputPath(File inputFile) {
        return new File(inputFile.getParentFile(), inputFile.getName() + ".enc");
    }

    /** Suggests a sensible default output filename for a decryption operation. */
    public static File suggestDecryptedOutputPath(File inputFile) {
        String name = inputFile.getName();
        String outName = name.endsWith(".enc") ? name.substring(0, name.length() - 4) : name + ".decrypted";
        return new File(inputFile.getParentFile(), outName);
    }
}
