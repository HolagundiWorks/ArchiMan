# Protected file formats

These formats are separate from the database schema. A schema-31 backup can be encrypted or legacy plaintext. No key or password is embedded in a portable file.

## Portable envelope, version 1

All integer fields use network byte order (big endian).

| Offset | Length | Value |
|---:|---:|---|
| 0 | 8 | ASCII `ARCHISEC` |
| 8 | 1 | Version `1` |
| 9 | 4 | PBKDF2 iteration count `600000` |
| 13 | 16 | Random salt |
| 29 | 12 | Random GCM nonce |
| 41 | remaining | AES-256-GCM ciphertext followed by its 16-byte authentication tag |

Derive a 32-byte AES key using PBKDF2-HMAC-SHA256, the supplied password and salt, and 600,000 iterations. Passwords are used exactly as entered, without trimming or Unicode normalization. The complete 41-byte header is GCM additional authenticated data. Salt and nonce are independently generated with `SecureRandom` for each encryption. Authenticate before interpreting the decrypted content. Reject unknown versions, unsupported work factors, oversized files and authentication failure; do not attempt format guessing after failed authentication.

An `.archimanbackup` envelope contains the existing checksummed backup ZIP as its plaintext. The ZIP holds `database.json`, `manifest.json` and optional hash-named attachments. Old unencrypted ZIP backups are accepted for migration, then new exports require a password. Checksums detect archive corruption; the outer GCM tag provides password-based authentication, not proof of which person authored the backup.

An `.archimansecure` envelope contains a document payload: a two-byte unsigned filename byte length, that many bytes of Java modified UTF-8 (the `DataOutputStream.writeUTF` format), then the original file contents. Filenames are limited to 180 UTF-16 code units, contain no path separators or control characters and are encrypted. Decryption asks the user to select a destination; filenames are never used to construct a filesystem path directly. The native Protect document files tool reads files created by the portal.

The app imposes a memory-aware plaintext file ceiling (up to 64 MiB, 32 MiB on a 256 MiB heap), plus small envelope overhead. Backup database metadata is limited to 8 MiB and manifests to 1 MiB. JSON imports reject nesting above 32 levels and more than 100,000 object/array containers before recursive parsing. Backup export checks the outer structural budget too.

## Internal recovery

Internal safety copies start with ASCII `ARCHIREC1`, followed by a 12-byte nonce and AES-256-GCM ciphertext/tag. The marker is authenticated additional data. The AES key is generated in Android Keystore under `archiman.recovery.aes.v1` and is not exported. These files are device/app-data-bound; copying the raw file does not create a portable backup. Use Save pre-restore recovery copy to produce a password-encrypted portable export. Uninstalling or clearing app data removes the key.

Neither protecting a file nor changing a password encrypts or deletes previously exported standard copies. Forgotten portable-file passwords cannot be recovered by the application.
