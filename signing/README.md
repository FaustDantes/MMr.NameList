# Signing key

`release.jks.enc` is the app's release keystore, encrypted with AES-256 (OpenSSL, PBKDF2, 600 000 iterations).
The password is in the GitHub secret `SIGNING_PASSWORD`. Keep a private copy of the password too.
Without it, no new APK can update the installed app. See [ADR-008](../docs/decisions.md#adr-008-encrypted-keystore-in-the-repository).

- Key alias: `namelist`
- Store password and key password: both equal to `SIGNING_PASSWORD`
- Certificate SHA-256: `3A:7C:B4:DB:FF:A0:68:64:B3:85:8D:C2:1C:D4:D2:48:F0:A0:B2:68:78:20:52:4A:EC:5C:B2:8E:54:A9:98:F3`

Decrypt locally:

```
openssl enc -d -aes-256-cbc -pbkdf2 -iter 600000 -in signing/release.jks.enc -out release.jks
```

Never commit the decrypted `release.jks`. `.gitignore` blocks it.
