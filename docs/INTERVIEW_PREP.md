# Interview Preparation

## 1. "Explain your project."
I built a Java Swing desktop app that encrypts and decrypts text and files
using a password. Under the hood it uses AES-256 in GCM mode — an
authenticated cipher — with the encryption key derived from the user's
password via PBKDF2 with a random salt, rather than using the password
directly as a key. I also added a clearly-labeled "Learning Mode" using a
Caesar cipher, purely to demonstrate side-by-side why a toy cipher isn't
real security, alongside the actual AES-GCM implementation.

## 2. "Why AES-GCM instead of plain AES (e.g. AES-CBC)?"
GCM is an authenticated encryption mode - it produces both ciphertext and
an authentication tag. On decryption, if the ciphertext was tampered with
or the wrong key/password was used, tag verification fails and decryption
throws an exception instead of silently returning corrupted or incorrect
plaintext. Plain CBC mode gives you confidentiality but not built-in
integrity checking, so you'd need to add a separate MAC yourself (encrypt-
then-MAC) to get the same guarantee.

## 3. "Why not just use the password directly as the AES key?"
Human-chosen passwords have far less entropy than a proper cryptographic
key, and using one directly is vulnerable to dictionary and brute-force
attacks. PBKDF2 stretches the password through 200,000 rounds of hashing
combined with a random salt, which makes each guess an attacker tries
computationally expensive, and the salt ensures two users with the same
password still get completely different derived keys.

## 4. "What is a salt, and why does it need to be random and stored alongside the ciphertext?"
A salt is random data mixed into the key derivation process so that the
same password never produces the same key twice. It doesn't need to be
secret — only unpredictable and unique per encryption — so it's safe to
store next to the ciphertext. Without it, an attacker could precompute
password-to-key mappings once (a rainbow table) and reuse them against
every user; with a random salt per encryption, that precomputation attack
doesn't work.

## 5. "What is an IV (initialization vector), and what happens if you reuse one?"
The IV is essentially a nonce that ensures encrypting the same plaintext
twice with the same key produces different ciphertext each time. With GCM
specifically, reusing the same (key, IV) pair is catastrophic - it can
leak the authentication key and let an attacker forge valid ciphertexts.
That's why I generate a fresh random 12-byte IV with `SecureRandom` for
every single encryption call and never reuse one.

## 6. "How do you know decryption actually verifies integrity, not just decrypts?"
Because I use `AES/GCM/NoPadding` with a 128-bit authentication tag
appended to the ciphertext. If even one byte changes - wrong password
(wrong derived key) or a tampered/corrupted ciphertext - `Cipher.doFinal()`
throws `AEADBadTagException` before returning any plaintext at all. I
specifically catch that exception and surface it as a clear "wrong
password or corrupted data" message rather than a confusing stack trace.

## 7. "Why use char[] for the password field instead of String?"
`String` objects are immutable in Java and can live in memory (and
potentially in memory dumps or swap) for an unpredictable amount of time
until garbage collected. `char[]` can be explicitly overwritten
(`Arrays.fill(password, '\0')`) the moment we're done using it, which
meaningfully reduces the window where the plaintext password sits
recoverable in memory. `JPasswordField.getPassword()` returns `char[]` for
exactly this reason.

## 8. "What are the limitations of this implementation?"
No secure password recovery (by design - losing the password means losing
the data), synchronous file processing on the UI thread (fine for small
files, would need a background worker for very large ones), and the
Learning Mode Caesar cipher is explicitly insecure and only included for
teaching contrast, never used for the app's real security path.

## 9. "How would you extend this for production use?"
I'd add authenticated key exchange or hardware-backed key storage instead
of a typed password where possible, move large-file crypto work off the
UI thread with a `SwingWorker` and progress bar, add automated tests
(JUnit) covering tamper detection and round-trip correctness, and
potentially support hardware security modules or OS keychains for key
storage instead of deriving from a memorized password each time.

## 10. "Why does this project matter even with so many AI tools available now?"
Because AI-powered products still need to protect user data at rest and in
transit, and that protection is built on exactly this kind of primitive -
correct use of authenticated encryption and key derivation. Calling a
crypto library correctly (right mode, right key derivation, right IV
handling) is a skill that doesn't get automated away; getting any of these
details wrong is how real-world security vulnerabilities happen.
