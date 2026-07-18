# Sample Text Encryption Output

**Plaintext:**
```
This is my secret note.
```

**Password used:** `MySecret123!` (example only - use your own real password when testing)

**Ciphertext (AES-256-GCM, Base64-encoded):**
```
QmFzZTY0RXhhbXBsZVNhbHRJVkFuZENpcGhlcnRleHRHb0hlcmVUaGlzSXNKdXN0QVBsYWNlaG9sZGVyU2FtcGxlT3V0cHV0Rm9yUmVhZG1lUHVycG9zZXM=
```
*(This is a placeholder example for README purposes - run the app yourself
to generate real output, which will look similar but be different every
single time due to the random salt and IV.)*

**Decrypted back with the same password:**
```
This is my secret note.
```

**Decrypted with the WRONG password:**
```
Decryption failed - wrong password or corrupted data.
```
