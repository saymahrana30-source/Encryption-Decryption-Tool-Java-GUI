# Encryption / Decryption Tool — Java GUI

A desktop application built in core Java (Swing) that encrypts and
decrypts text and files using a password, backed by **AES-256-GCM
authenticated encryption** and **PBKDF2 password-based key derivation**.
No external frameworks, runs fully offline, and includes an intentionally
insecure "Learning Mode" (Caesar cipher) purely to demonstrate the
difference between a toy cipher and real cryptography.

> **Scope note:** this project is for educational and defensive use only —
> protecting your own text/files with a password you choose. It does not
> include password cracking, unauthorized access, or any functionality
> designed to lock or damage someone else's data.

---

## 1. Overview

### Simple explanation
You type a password and some text (or pick a file). Click **Encrypt** and
it turns into unreadable scrambled data. Only someone who has the exact
same password can turn it back into the original with **Decrypt**.

### Technical explanation
- **Encryption** transforms readable data (**plaintext**) into unreadable
  data (**ciphertext**) using a cryptographic algorithm and a key.
  **Decryption** reverses that transformation — but only with the correct key.
- This tool derives that key from a **password** using **PBKDF2WithHmacSHA256**
  (200,000 iterations, random 16-byte salt) rather than using the password
  directly, which resists brute-force attacks far better than a raw password.
- The actual encryption uses **AES-256 in GCM mode** — an *authenticated*
  cipher. It doesn't just hide data, it also detects tampering: if the
  ciphertext is modified, or the wrong password is supplied, decryption
  fails loudly with a clear error instead of silently returning garbage.
- A fresh random **salt** and **IV (initialization vector)** are generated
  for every single encryption operation and stored alongside the
  ciphertext — this is what makes encrypting the same text twice produce
  two completely different results, which is a required property for GCM's
  security guarantees to hold.

### Workflow
```
User Enters Text or Selects File
        ↓
Selects Encryption Method (AES-GCM, or Learning Mode)
        ↓
Provides Password
        ↓
PBKDF2 derives a 256-bit key from the password + random salt
        ↓
AES-GCM encrypts the data using the key + random IV
        ↓
Ciphertext (Base64 text, or an .enc file) is generated
        ↓
User provides the correct password
        ↓
Key is re-derived, AES-GCM verifies + decrypts, original data is restored
```

---

## 2. Industry Relevance

The same pattern — password-based key derivation + authenticated
encryption — underpins:

| Domain | How it's used |
|---|---|
| Secure messaging apps | Signal, WhatsApp use authenticated encryption (AEAD ciphers like AES-GCM/ChaCha20-Poly1305) for every message |
| Banking software | Protecting transaction data and stored credentials at rest |
| Password managers | Deriving a master key from your master password (PBKDF2/Argon2) to encrypt your vault |
| Cloud storage | Client-side encryption before upload so the provider never sees plaintext |
| File protection systems | Encrypting sensitive documents before sharing or backup |
| Healthcare systems | HIPAA-driven encryption-at-rest requirements for patient data |
| Enterprise applications | Encrypting config secrets, tokens, and sensitive fields in databases |
| Secure data transmission | TLS 1.3 itself uses AES-GCM as its primary cipher suite |

**Technical value:** understanding key derivation, IV/nonce management, and
authenticated encryption is foundational for any security-adjacent role —
and is a common practical interview topic.

**Business value:** every product handling user data — messaging, storage,
fintech, healthcare — needs this exact capability; being able to build it
correctly (not just call a library blindly) is a strong signal.

**Why Java, even in an AI-driven era:** Java ships with a mature,
well-audited cryptography API (`javax.crypto`) built into the JDK itself,
is dominant in enterprise and financial systems, and remains the backbone
of countless secure backend and desktop tools — AI features still need
secure storage and transmission underneath them, which is exactly what
this project practices.

---

## 3. Tech Stack Used

This implements a blend of **Option B (Recommended)** and **Option C
(Advanced)** from the original brief: Swing GUI + AES-GCM (not just plain
AES) + PBKDF2 password-based key derivation + file encryption + a Caesar
cipher "Learning Mode" toggle for teaching contrast — while keeping the
codebase small enough for a student project. No database, no network
calls, no internet required.

| Piece | Purpose |
|---|---|
| `javax.crypto.Cipher` (`AES/GCM/NoPadding`) | The actual authenticated encryption |
| `javax.crypto.SecretKeyFactory` (`PBKDF2WithHmacSHA256`) | Password → 256-bit key derivation |
| `java.security.SecureRandom` | Cryptographically secure salt + IV generation |
| `java.util.Base64` | Makes encrypted text copy/paste-safe |
| `javax.swing` | GUI: `JTextArea`, `JPasswordField`, `JFileChooser`, `JTabbedPane` |
| `java.nio.file.Files` | File reading/writing for the File tab |

---

## 4. Java & Security Concepts Used

| Concept | Role in this project |
|---|---|
| Classes, encapsulation | `CryptoUtils` is a `final` utility class with a private constructor — all crypto logic lives in exactly one place |
| `Cipher` | The JCA class that performs the actual AES-GCM encrypt/decrypt operations |
| `SecretKeySpec` / `SecretKeyFactory` | Represent and derive the AES key used by `Cipher` |
| AES | The symmetric block cipher algorithm itself (256-bit key) |
| GCM / initialization vector | The authenticated mode of operation; the IV ensures identical plaintexts never produce identical ciphertext |
| Secure random values | `SecureRandom` (not `Random`!) generates unpredictable salts and IVs — critical for security |
| Password-based key derivation | PBKDF2 stretches a human password into a proper cryptographic key, resisting brute force |
| Base64 | Encodes raw encrypted bytes into printable text for the GUI text area |
| `JPasswordField` | Masks password input and returns a `char[]` (safer than `String` — can be explicitly wiped from memory) |
| `JFileChooser` | Native OS file-picker dialog for the File tab |
| Exception handling | Custom `CryptoOperationException` wraps all low-level crypto/IO exceptions into one clear, user-facing error type |
| Input validation | `InputValidator` centralizes empty-input, weak-password, and bad-file checks |
| `ActionListener` (lambdas) | Wires every button click to its handler method |

---

## 5. Architecture

**Input Module:** plaintext, ciphertext, password, selected file, chosen mode (AES-GCM or Learning Mode)

**Processing Module:** validate input → derive/generate key → generate IV → encrypt/decrypt data → encode/decode output → handle file I/O

**Output Module:** encrypted text, decrypted text, encrypted file (`.enc`), restored file, success/error message

```
                    ┌───────────────────────┐
                    │      MainWindow        │  (Swing GUI - Text & File tabs)
                    └───────────┬────────────┘
                                │ Encrypt / Decrypt click
                 ┌──────────────┼──────────────┐
                 ▼                             ▼
      CryptoUtils.encryptText/          FileEncryptionService
      decryptText (Base64 in/out)       .encryptFile/.decryptFile
                 │                             │
                 └───────────┬─────────────────┘
                              ▼
                  CryptoUtils.encryptBytes/decryptBytes
                              │
              ┌───────────────┼────────────────┐
              ▼                                ▼
     deriveKey() via PBKDF2          Cipher (AES/GCM/NoPadding)
     (password + random salt)        (derived key + random IV)
```

**Encryption flow:** validate → generate random salt + IV → derive key
(PBKDF2) → `Cipher.ENCRYPT_MODE` → pack `[salt][iv][ciphertext+tag]` →
Base64-encode (text) or write raw bytes (file).

**Decryption flow:** validate → unpack `[salt][iv][ciphertext+tag]` →
re-derive key from password + extracted salt → `Cipher.DECRYPT_MODE` with
extracted IV → GCM tag verification (fails loudly on wrong password/tampered
data) → return original bytes.

**GUI event flow:** button click → `ActionListener` lambda → validate via
`InputValidator` → call `CryptoUtils`/`FileEncryptionService` → catch
`CryptoOperationException` → update status label (green success / red error).

---

## 6. Folder Structure

```
Encryption-Decryption-Tool-Java-GUI/
│
├── src/
│   ├── gui/
│   │   └── MainWindow.java          # Swing GUI - Text & File tabs
│   ├── crypto/
│   │   ├── CryptoUtils.java         # AES-GCM + PBKDF2 core (the real encryption)
│   │   └── CaesarCipher.java        # Educational-only, NOT secure — "Learning Mode"
│   ├── service/
│   │   └── FileEncryptionService.java  # File read/encrypt/decrypt/write
│   ├── utility/
│   │   └── InputValidator.java      # Input/password/file validation + strength estimate
│   ├── exception/
│   │   └── CryptoOperationException.java  # Single user-facing error type
│   └── main/
│       └── Main.java                # Application entry point
├── sample_files/                    # Put test files here to try file encryption
├── encrypted_files/                 # Suggested output location for .enc files
├── decrypted_files/                 # Suggested output location for restored files
├── outputs/                         # Sample text encryption output for GitHub proof
├── screenshots/                     # Proof screenshots go here
├── docs/
│   ├── TESTING.md
│   ├── GITHUB_STRATEGY.md
│   ├── PROOF_PLAN.md
│   ├── SCREENSHOTS_CHECKLIST.md
│   └── INTERVIEW_PREP.md
├── README.md
└── .gitignore
```

---

## 7. Features

**Implemented:**
- Full Swing GUI with Text and File tabs
- AES-256-GCM authenticated encryption with PBKDF2 key derivation
- Base64-encoded, copy-paste-friendly ciphertext for text mode
- File selection via `JFileChooser`, encrypt/decrypt whole files
- Show/hide password toggle
- Password strength indicator (Weak/Medium/Strong)
- Copy output to clipboard, clear fields
- Clear success/error messaging, including a specific "wrong password"
  message powered by GCM's built-in tamper detection
- Input validation (empty text, weak/empty password, missing/unreadable file)
- "Learning Mode" Caesar cipher toggle, clearly labeled as insecure, for
  side-by-side teaching contrast with real AES-GCM
- Passwords are stored as `char[]` and explicitly zeroed out after each
  operation rather than lingering in memory as an immutable `String`

**Not implemented (documented as future improvements):** drag-and-drop file
selection, JavaFX version, packaged executable JAR, activity log, theme selector.

---

## 8. How to Run

### A. Command line

```bash
# 1. Compile everything into a build/ folder
javac -encoding UTF-8 -d build -sourcepath src src/main/Main.java

# 2. Run the GUI application
java -cp build main.Main
```

### B. IntelliJ IDEA
1. `File → New → Project from Existing Sources` → select this folder.
2. Mark `src` as the Sources Root.
3. Right-click `main/Main.java → Run 'Main.main()'`.

### C. Eclipse
1. `File → New → Java Project`, import the `src` folder.
2. Right-click `Main.java → Run As → Java Application`.

A window opens with two tabs: **Text Encryption** and **File Encryption**.

---

## 9. Virtual Simulation

### Text encryption test
1. Go to the **Text Encryption** tab.
2. Type sample plaintext, e.g. `This is my secret note.`
3. Enter a password, e.g. `MySecret123!`.
4. Click **Encrypt** → a Base64 ciphertext string appears in the output box.
5. Click **Copy Output**, then **Clear**.
6. Paste the ciphertext back into the input box.
7. Enter the **same** password.
8. Click **Decrypt** → the original text reappears exactly.
9. Now try decrypting with a **wrong** password → confirm you get a clear
   "Decryption failed - wrong password or corrupted data" message, not a
   crash or garbled text.

### File encryption test
1. Go to the **File Encryption** tab.
2. Click **Browse...** and select a small test file (e.g. from `sample_files/`).
3. Enter a password and click **Encrypt File** → an `<filename>.enc` file
   is created next to the original.
4. Click **Browse...** again, select the `.enc` file.
5. Enter the same password and click **Decrypt File** → a restored file is
   created and its contents match the original exactly.

### Learning Mode contrast
1. Check **Learning Mode (Caesar Cipher)**.
2. Encrypt the same sample text — notice the output is just shifted
   letters, and anyone could reverse it without knowing your password.
3. Uncheck Learning Mode and encrypt the same text again with AES-GCM —
   compare the two outputs to see the difference between a toy cipher and
   real cryptography.

This exact flow (text round-trip, wrong-password rejection, file
round-trip, and confirming random salt/IV means repeat encryptions never
match) was verified with an automated test harness against the included
source before this README was finalized.

---

## 10. Limitations

- No drag-and-drop file selection (uses the standard `JFileChooser` dialog).
- No packaged executable `.jar` included by default (can be added — see below).
- Large files are processed synchronously on the UI thread — fine for
  typical student test files, but very large files (100s of MB) would
  benefit from a background `SwingWorker`.
- Forgetting your password means the data cannot be recovered — this is by
  design (no password = no key = no decryption), not a bug.

## 11. Future Improvements

- Package as an executable `.jar` (`java -jar EncryptionTool.jar`)
- Drag-and-drop file support
- JavaFX version with a more modern UI
- Background `SwingWorker` for large file encryption with a progress bar
- Optional AES-CBC mode toggle for comparison/teaching purposes

## 12. Learning Outcomes

Building this project demonstrates practical understanding of: symmetric
encryption vs. authenticated encryption, password-based key derivation,
secure random value generation, safe handling of sensitive data in memory
(`char[]` vs `String`), Swing GUI event-driven programming, and clean
separation between UI, business logic, and cryptographic primitives.

## 13. Author

Built by Sam as a GTU Java course project and GitHub portfolio piece.

---

See `docs/` for the testing strategy, GitHub upload plan, screenshot
checklist, and interview preparation notes.
