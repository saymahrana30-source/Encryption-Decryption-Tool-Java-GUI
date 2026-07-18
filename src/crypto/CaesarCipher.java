package crypto;

/**
 * CaesarCipher.java
 * =================
 * ⚠ EDUCATIONAL / "LEARNING MODE" ONLY - THIS IS NOT SECURE ENCRYPTION. ⚠
 *
 * A Caesar cipher just shifts every letter by a fixed number of positions
 * in the alphabet (e.g. shift 3: 'A' -> 'D', 'B' -> 'E', ...). It has only
 * 25 possible keys total, so it can be broken instantly by hand or by
 * brute force, and it leaks the language's letter-frequency pattern
 * directly through the ciphertext. It has ZERO real-world security value.
 *
 * It's included here purely so a student can SEE the difference between
 * a "toy" cipher and real authenticated encryption (CryptoUtils' AES-GCM)
 * side by side in the same app - flip to "Learning Mode" in the GUI, shift
 * some text, and see how trivially reversible it is. The main Encrypt /
 * Decrypt buttons always use AES-GCM; this class is never used for
 * anything the app calls "secure."
 */
public final class CaesarCipher {

    private CaesarCipher() {
    }

    public static String encrypt(String plaintext, int shift) {
        return shiftLetters(plaintext, shift);
    }

    public static String decrypt(String ciphertext, int shift) {
        return shiftLetters(ciphertext, -shift);
    }

    private static String shiftLetters(String text, int shift) {
        int normalizedShift = ((shift % 26) + 26) % 26; // handle negative shifts safely
        StringBuilder result = new StringBuilder(text.length());

        for (char c : text.toCharArray()) {
            if (Character.isUpperCase(c)) {
                result.append((char) ('A' + (c - 'A' + normalizedShift) % 26));
            } else if (Character.isLowerCase(c)) {
                result.append((char) ('a' + (c - 'a' + normalizedShift) % 26));
            } else {
                result.append(c); // leave digits, spaces, punctuation untouched
            }
        }
        return result.toString();
    }
}
