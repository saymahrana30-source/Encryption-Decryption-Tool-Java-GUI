# Testing Strategy

## Manual test cases

| # | Test case | Steps | Expected result |
|---|---|---|---|
| 1 | Basic text round-trip | Encrypt sample text with a password, then decrypt with the same password | Decrypted text exactly matches the original |
| 2 | Wrong password on decrypt | Encrypt text, then try to decrypt with a different password | Clear "Decryption failed - wrong password or corrupted data" message, no crash |
| 3 | Empty text input | Click Encrypt/Decrypt with the input box empty | "Input text cannot be empty" validation message, no crash |
| 4 | Empty password | Click Encrypt/Decrypt with the password field empty | "Password cannot be empty" validation message |
| 5 | Weak/short password | Enter a 1-2 character password | "Password should be at least 4 characters" validation message |
| 6 | Tampered ciphertext | Encrypt text, manually change one character in the pasted ciphertext, then decrypt | GCM tag verification fails - same "wrong password or corrupted data" message (this proves tamper detection works) |
| 7 | Invalid Base64 | Paste random non-Base64 text into the input box and click Decrypt | "Ciphertext is not valid Base64" message, no crash |
| 8 | File round-trip | Encrypt a sample file, then decrypt the resulting `.enc` file with the same password | Restored file's contents exactly match the original |
| 9 | File with wrong password | Try decrypting an `.enc` file with the wrong password | Same clear "wrong password" error, no corrupted output file written |
| 10 | Missing file selection | Click Encrypt File / Decrypt File with no file browsed | "No file was selected" validation message |
| 11 | Repeat encryption randomness | Encrypt the exact same text twice with the exact same password | The two ciphertexts are completely different (confirms random salt/IV per operation) |
| 12 | Learning Mode contrast | Encrypt the same text with Learning Mode on vs. off | Caesar output is a simple, recognizable letter shift; AES-GCM output is unrecognizable Base64 - demonstrates why Learning Mode is not secure |
| 13 | Show/hide password | Toggle the "Show" checkbox next to the password field | Password field switches between masked dots and plain text |
| 14 | Copy output | Click "Copy Output" after encrypting, then paste elsewhere | Clipboard contains the exact ciphertext shown in the output box |

## Automated verification performed during development

Before finalizing this project, the following was verified with a scripted
test harness run directly against the compiled source:

1. **Text round-trip** — encrypt then decrypt returns the exact original string. ✅
2. **Wrong password rejection** — decrypting with an incorrect password throws
   `CryptoOperationException` with a clear message rather than returning
   garbled text. ✅
3. **File round-trip** — a sample file, encrypted then decrypted, produces
   byte-identical content to the original. ✅
4. **Salt/IV randomness** — encrypting the same plaintext with the same
   password twice produces two different ciphertexts, confirming a fresh
   random salt and IV are generated on every operation (a requirement for
   GCM's security guarantees). ✅

## Optional JUnit test ideas

- `CryptoUtilsTest` — parameterized round-trip tests across various string
  lengths (empty-after-validation-bypass, single character, very long text,
  unicode/emoji characters).
- `CryptoUtilsTamperTest` — flip a single bit in a valid ciphertext byte
  array and assert `decryptBytes` throws `CryptoOperationException`.
- `InputValidatorTest` — assert each validation method throws for its
  respective invalid input and passes silently for valid input.
- `FileEncryptionServiceTest` — encrypt/decrypt a temp file created with
  JUnit's `@TempDir`, assert byte-for-byte equality after round-trip.
