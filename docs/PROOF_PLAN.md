# 6-Day GitHub Proof-Building Plan

| Day | Focus | Files to commit | Commit message | Screenshot/proof to capture |
|---|---|---|---|---|
| 1 | Project setup + crypto core | Folder structure, `.gitignore`, `CryptoUtils.java` (encrypt/decrypt bytes) | `feat: initial setup with AES-GCM CryptoUtils core` | Project folder structure |
| 2 | Key derivation + exceptions | `CryptoOperationException.java`, PBKDF2 key derivation in `CryptoUtils.java` | `feat: add PBKDF2 password-based key derivation and custom exception type` | Console test output showing successful key derivation |
| 3 | Validation + file service | `InputValidator.java`, `FileEncryptionService.java` | `feat: add input validation and file encryption service` | Terminal test of file encrypt/decrypt round-trip |
| 4 | GUI - text tab | `MainWindow.java` (text tab only), `Main.java` | `feat: add Swing GUI with text encryption/decryption tab` | Screenshot of the Text Encryption tab with a successful encrypt |
| 5 | GUI - file tab + learning mode | Complete `MainWindow.java` with File tab and Caesar cipher toggle, `CaesarCipher.java` | `feat: add file encryption tab and educational Learning Mode` | Screenshot of File Encryption tab + Learning Mode comparison |
| 6 | Testing + documentation | `docs/TESTING.md`, final `README.md`, `docs/INTERVIEW_PREP.md` | `docs: add full README, testing notes, and interview prep` | Full README preview on GitHub, wrong-password error screenshot |

**Tip:** even if you build the whole project in one sitting, staging and
committing in this logical order still gives your repository an honest,
readable history that shows the actual build progression.
