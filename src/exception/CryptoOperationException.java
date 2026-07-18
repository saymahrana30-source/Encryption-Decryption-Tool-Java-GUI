package exception;

/**
 * CryptoOperationException.java
 * ------------------------------
 * A custom checked exception used throughout the crypto/service layer.
 *
 * WHY A CUSTOM EXCEPTION INSTEAD OF JUST LETTING RAW EXCEPTIONS BUBBLE UP:
 *   Java's crypto APIs throw several different low-level checked exceptions
 *   (NoSuchAlgorithmException, InvalidKeyException, AEADBadTagException,
 *   IOException, etc). The GUI layer doesn't need to know or care which
 *   specific one occurred - it just needs a single, user-friendly type it
 *   can catch and show a clear error message for. Wrapping everything in
 *   one CryptoOperationException keeps CryptoUtils/FileEncryptionService
 *   clean and keeps the GUI's error handling simple and consistent.
 *
 * A very common real cause of this exception: the user typed the WRONG
 * PASSWORD. With AES-GCM, an incorrect key makes tag verification fail,
 * which surfaces here as "Decryption failed - wrong password or corrupted
 * data" rather than a confusing low-level stack trace.
 */
public class CryptoOperationException extends Exception {

    public CryptoOperationException(String message) {
        super(message);
    }

    public CryptoOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
