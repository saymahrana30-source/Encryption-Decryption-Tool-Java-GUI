package crypto;

import exception.CryptoOperationException;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * CryptoUtils.java
 * ================
 * The cryptographic core of the whole application. Every other class
 * (GUI, file service) calls into here - none of them touch the Cipher
 * API directly. That separation keeps the "hard to get right" security
 * code in exactly one place.
 *
 * ALGORITHM CHOICE: AES-256 in GCM mode ("AES/GCM/NoPadding")
 *   - GCM is an AUTHENTICATED encryption mode: it doesn't just hide the
 *     data, it also detects tampering. If even one byte of the ciphertext
 *     is modified, or the wrong password is supplied, decryption throws
 *     an AEADBadTagException instead of silently returning garbage. This
 *     is exactly the property a "wrong password" error message relies on.
 *   - This is the same category of cipher (AEAD) used by TLS 1.3, Signal,
 *     and most modern secure-messaging protocols.
 *
 * KEY DERIVATION: PBKDF2WithHmacSHA256, 200,000 iterations, random 16-byte salt
 *   - We never use the user's password directly as an AES key. Passwords
 *     are low-entropy and human-chosen; PBKDF2 stretches the password
 *     through many rounds of hashing (with a random salt) to produce a
 *     proper 256-bit key, and makes brute-force attacks far more
 *     expensive for an attacker.
 *   - The salt is random PER ENCRYPTION and stored alongside the
 *     ciphertext (it does not need to be secret) so decryption can
 *     re-derive the exact same key from the same password.
 *
 * IV (Initialization Vector): random 12 bytes, generated fresh EVERY TIME
 *   - GCM's security guarantee breaks down completely if the same
 *     (key, IV) pair is ever reused. We use SecureRandom to generate a
 *     new 12-byte IV for every single encryption operation, and store it
 *     alongside the ciphertext (also not secret).
 *
 * OUTPUT FORMAT (what actually gets stored/displayed):
 *   [ 16-byte salt ][ 12-byte IV ][ ciphertext + 16-byte GCM auth tag ]
 *   For TEXT mode, that whole byte blob is Base64-encoded into one
 *   printable string the user can copy/paste. For FILE mode, the same
 *   layout is written as raw bytes to the output file.
 */
public final class CryptoUtils {

    private static final String CIPHER_ALGO = "AES/GCM/NoPadding";
    private static final String KEY_DERIVATION_ALGO = "PBKDF2WithHmacSHA256";
    private static final int KEY_LENGTH_BITS = 256;
    private static final int PBKDF2_ITERATIONS = 200_000;
    private static final int SALT_LENGTH_BYTES = 16;
    private static final int IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private CryptoUtils() {
        // Utility class - never instantiated.
    }

    // ---------------------------------------------------------------
    // Key derivation
    // ---------------------------------------------------------------

    /**
     * Derives a 256-bit AES key from a password and salt using PBKDF2.
     * The SAME password + SAME salt will always produce the SAME key,
     * which is exactly what lets decryption reconstruct the key that
     * was used to encrypt - without ever storing the key itself.
     */
    private static SecretKeySpec deriveKey(char[] password, byte[] salt) throws CryptoOperationException {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGO);
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            spec.clearPassword(); // wipe the password copy held by the spec
            return new SecretKeySpec(keyBytes, "AES");
        } catch (GeneralSecurityException e) {
            throw new CryptoOperationException("Failed to derive encryption key from password.", e);
        }
    }

    // ---------------------------------------------------------------
    // Low-level byte[] encryption / decryption (shared by text + file)
    // ---------------------------------------------------------------

    /**
     * Encrypts arbitrary bytes with a fresh random salt + IV.
     * Returns: [salt][iv][ciphertext+tag] as a single byte array.
     */
    public static byte[] encryptBytes(byte[] plaintext, char[] password) throws CryptoOperationException {
        if (plaintext == null || password == null || password.length == 0) {
            throw new CryptoOperationException("Plaintext and password must not be empty.");
        }
        try {
            byte[] salt = new byte[SALT_LENGTH_BYTES];
            byte[] iv = new byte[IV_LENGTH_BYTES];
            SECURE_RANDOM.nextBytes(salt);
            SECURE_RANDOM.nextBytes(iv);

            SecretKeySpec key = deriveKey(password, salt);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);

            byte[] ciphertext = cipher.doFinal(plaintext);

            // Pack salt + iv + ciphertext into one array for easy storage/transport.
            ByteBuffer buffer = ByteBuffer.allocate(salt.length + iv.length + ciphertext.length);
            buffer.put(salt).put(iv).put(ciphertext);
            return buffer.array();

        } catch (GeneralSecurityException e) {
            throw new CryptoOperationException("Encryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * Decrypts a byte array produced by encryptBytes(). Extracts the salt
     * and IV from the front of the array, re-derives the key from the
     * supplied password, and verifies + decrypts the remaining ciphertext.
     */
    public static byte[] decryptBytes(byte[] packedData, char[] password) throws CryptoOperationException {
        if (packedData == null || packedData.length < SALT_LENGTH_BYTES + IV_LENGTH_BYTES) {
            throw new CryptoOperationException("Encrypted data is missing or corrupted.");
        }
        try {
            ByteBuffer buffer = ByteBuffer.wrap(packedData);
            byte[] salt = new byte[SALT_LENGTH_BYTES];
            byte[] iv = new byte[IV_LENGTH_BYTES];
            buffer.get(salt);
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            SecretKeySpec key = deriveKey(password, salt);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);

            return cipher.doFinal(ciphertext);

        } catch (AEADBadTagException e) {
            // This is THE most common real-world case: wrong password, or
            // the ciphertext was tampered with / corrupted in transit.
            throw new CryptoOperationException(
                    "Decryption failed - wrong password or corrupted data.", e);
        } catch (GeneralSecurityException e) {
            throw new CryptoOperationException("Decryption failed: " + e.getMessage(), e);
        }
    }

    // ---------------------------------------------------------------
    // Convenience methods for TEXT (String <-> Base64 string)
    // ---------------------------------------------------------------

    /** Encrypts a plaintext string and returns a printable Base64 ciphertext string. */
    public static String encryptText(String plaintext, char[] password) throws CryptoOperationException {
        byte[] packed = encryptBytes(plaintext.getBytes(StandardCharsets.UTF_8), password);
        return Base64.getEncoder().encodeToString(packed);
    }

    /** Decrypts a Base64 ciphertext string (produced by encryptText) back to plaintext. */
    public static String decryptText(String base64Ciphertext, char[] password) throws CryptoOperationException {
        byte[] packed;
        try {
            packed = Base64.getDecoder().decode(base64Ciphertext.trim());
        } catch (IllegalArgumentException e) {
            throw new CryptoOperationException("Ciphertext is not valid Base64 - check it was pasted correctly.", e);
        }
        byte[] plaintextBytes = decryptBytes(packed, password);
        return new String(plaintextBytes, StandardCharsets.UTF_8);
    }
}
