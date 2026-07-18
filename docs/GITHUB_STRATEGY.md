# GitHub Upload Strategy

## Repository setup
- **Repository name:** `encryption-decryption-tool-java-gui`
- **Description:** "Desktop AES-256-GCM encryption/decryption tool built in Java Swing, with PBKDF2 password-based key derivation, text and file encryption, and an educational Caesar-cipher comparison mode."
- **Tags/topics:** `java`, `swing`, `cryptography`, `aes-gcm`, `encryption`, `decryption`, `pbkdf2`, `security`, `desktop-application`, `student-project`, `cybersecurity`

## Folder organization on GitHub
Push the structure exactly as in the README: `src/`, `docs/`, `sample_files/`,
`outputs/`, `screenshots/`. Keep `encrypted_files/` and `decrypted_files/`
out of git by default (runtime output), but commit ONE example pair into
`outputs/` (e.g. a sample ciphertext string, not a real secret) so visitors
can see real output without running it.

## .gitignore essentials
Already included in this project — makes sure you don't upload:
- Compiled `.class` files / `build/` directory
- IDE files (`.idea/`, `*.iml`, `.classpath`, `.project`, `.settings/`)
- OS files (`.DS_Store`, `Thumbs.db`)
- Runtime-generated encrypted/decrypted files (commit a curated sample separately instead)
- **Never commit real passwords, real personal files, or actual secrets used in testing**

## Commit strategy
Commit in the same phase order you built the project — see `PROOF_PLAN.md`
for the exact breakdown and messages to use. A readable, incremental commit
history is itself part of the proof-of-work value of this repo.

## Meaningful commit message examples
```
feat: add CryptoUtils with AES-GCM encryption and PBKDF2 key derivation
feat: add CryptoOperationException for unified error handling
feat: add InputValidator for text/password/file validation
feat: add FileEncryptionService for whole-file encryption and decryption
feat: add Swing MainWindow with Text and File tabs
feat: add password strength indicator and show/hide toggle
feat: add educational CaesarCipher Learning Mode for teaching contrast
docs: add README with architecture, crypto design notes, and setup steps
test: verify text/file round-trip, wrong-password rejection, and IV randomness
```

## Security notes (important for a crypto-focused repo)
- Never commit a real password you actually use elsewhere, even in a test
  screenshot or sample ciphertext.
- Don't commit any real personal files through `sample_files/` — use
  clearly fake/placeholder content only.
- If you ever add key storage or a "remember password" feature in the
  future, never store the plaintext password — hash/encrypt appropriately
  and keep the mechanism out of source control.
- Mention clearly in your README (already done) that `CaesarCipher` is
  educational-only and never used for real security in the app - this
  avoids anyone mistakenly relying on it.
