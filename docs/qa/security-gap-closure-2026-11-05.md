# Security gap closure — 5 November 2026

This follow-up supersedes the HTTP, backup-encryption and production-signing status in the original audit. App version: 1.0.1 (code 2), database schema unchanged at 31.

## Changes

- **Portal transport:** HTTPS only, TLS 1.2/1.3, AEAD ciphers with ephemeral key exchange, Secure/HttpOnly/SameSite cookies and HTTPS origin checks. The device-local EC private key stays in Android Keystore. The certificate has the current IP as its Subject Alternative Name. The phone shows its SHA-256 fingerprint for out-of-band browser pairing; there is no HTTP fallback. A local self-signed certificate requires manual verification before entering the access code. Encryption alone does not authenticate an unverified first connection.
- **Portable backups:** new backup and recovery exports require a confirmed password of 12–256 characters. AES-256-GCM authenticates the encrypted content and versioned header; PBKDF2-HMAC-SHA256 uses 600,000 iterations, random 128-bit salt and random 96-bit nonce. Unsupported versions/work factors, oversized input, wrong passwords and tampering are rejected. No password is stored in preferences, saved instance state or the backup. Legacy unencrypted backups remain readable for migration.
- **Internal recovery:** recovery files use a separate AES-256 Android Keystore key with GCM authentication and atomic replacement. Startup upgrades an existing plaintext recovery copy atomically; failure retains the original. Portable recovery exports are decrypted internally and then encrypted with the user's chosen password. Losing app data or uninstalling also removes access to device-bound internal recovery; keep a portable backup.
- **Documents:** portal PDF/XLSX/CSV downloads offer password protection as `.archimansecure` files, with password confirmation sent in a POST body rather than a URL. The native Protect document files tool encrypts/decrypts these containers and arbitrary exported files. Filenames are inside the authenticated ciphertext. Standard interoperable files remain available only through an explicit standard-file choice; old or decrypted copies are still readable and are not remotely erased.
- **Production identity:** release builds support a separate owner-controlled signing key. A random-credential identity was generated in ignored `.signing/`, restricted to the current Windows account. The bootstrap refuses to overwrite keys, and credentials are not printed. `assembleProduction` requires signing configuration and rejects the default debug alias. The separate audit build still uses a development key and package. Back up the private signing directory offline; do not publish it or raw private audit evidence.
- **Import parser safety:** linear structural preflight rejects nesting deeper than 32 levels or more than 100,000 object/array containers before recursive JSON parsing. This applies to archive manifests, database metadata, embedded finance/details/tax JSON and company packages. The same outer-JSON budget is checked before archive export, so the app refuses an oversized structure instead of writing a backup that its importer would reject. File-byte limits alone do not prevent recursion or object-allocation attacks.
- **Maintenance and testing:** CI includes regression/lint/release checks, exact production Maven-version advisory scanning, Android API 26/35 emulator jobs and configurable repeated isolated device regressions. External actions are pinned to verified upstream commits. Advisory lookup fails on missing/truncated results instead of reporting success. Storage tests inject an unwritable restore location and an interrupted/full export destination, checking all practice tables remain unchanged.

## Validation

The initial device run caught an Android TLS-provider incompatibility: Conscrypt uses an already-hashed ECDSA signing operation. The key policy was corrected to allow that operation; the subsequent full device regression passed HTTPS login, pinned fingerprint and hostname verification, encrypted PDF download/decryption and wrong-password rejection. Final results and repeated-run evidence are recorded below after verification.

Cryptographic implementation uses platform/JCA providers, not custom encryption algorithms. The small DER certificate encoder only creates the X.509 identity; its signature is verified before use. References: [Android KeyGenParameterSpec](https://developer.android.com/reference/android/security/keystore/KeyGenParameterSpec), [OWASP cryptographic storage](https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html), [OWASP PBKDF2 guidance](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html), [Java TLS configuration](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/javax/net/ssl/SSLServerSocket.html).

## Boundaries requiring separate action or evidence

- The current phone installation has the original development signing identity. Android will not accept the new unrelated production signer as an in-place update. The phone is kept on a non-debuggable compatibility-signed build so its records remain accessible. An ordinary unrelated-key update is rejected. Android supports signing lineages, but [AOSP does not recommend rotation on Android 12/API 31 and earlier](https://source.android.com/docs/security/features/apksigning/v3). For this phone, the chosen migration is a verified encrypted external backup followed by deliberate install/restore; this audit does not silently uninstall the existing practice or rotate its identity.
- The Samsung M11 reports Android security patch 2023-11-01. Application changes cannot supply an OEM security update. Use an officially supported update/device; no firmware flashing or device reset is performed.
- The live SQLite database relies on Android app-private storage and device encryption, not a separate SQLCipher layer. The app remains direct local Admin entry as requested. An unlocked or compromised device remains a data-access boundary.
- API 26/35 CI and long soak jobs are configured, not automatically counted as passed. Available physical testing uses the connected API-31 phone. A single phone cannot establish compatibility, battery behaviour or frame performance across other devices.
- Existing dependency/toolchain update notices are not eliminated by upgrading blindly across major versions. Exact resolved versions are now scanned and CI reruns the checks; compatibility-tested dependency upgrades remain maintenance work. Zero OSV matches are not a security guarantee.

### Final local verification

- 73 local regression tests pass; lint has zero errors and 36 remaining dependency/toolchain update warnings. Twenty-five unused resources were removed, count strings now use appropriate plurals/labels, and Compose Modifier ordering was corrected.
- Exact production dependency scan: 93 Maven versions checked, zero OSV advisory matches. Debug tooling is excluded from this release inventory; the earlier 129-package audit covered a broader configuration.
- Production build succeeds with the separate owner signing configuration. The private signing directory is ignored by Git, has inheritance disabled and grants access only to the current Windows account.
- Three repeated full physical-device regression cycles passed in 132.75 s, 127.70 s and 129.97 s (390.42 s total). This is a short repeated regression, not a multi-hour soak. Coverage includes device-bound recovery encryption/tamper rejection, encrypted download interoperability, storage-failure rollback and the original 10,000-row workloads.
- All 56 existing practice tables were compared again: zero changed or missing tables.
- Production APK signature verification reports the separate `ArchiMan Production` certificate. The phone compatibility copy is non-debuggable (`run-as` access refused) and preserves its existing update identity.
- No API-26/35 emulator runtime is installed on this workstation; those jobs need CI execution.

### Rendering and final UI checks

A first-navigation system sample recorded 384 rendered frames, 67 janky frames (17.45%), p95 46 ms and p99 125 ms. After explicit Android ART `speed` compilation and a warm restart, an equivalent sample recorded 385 frames, 21 janky frames (5.45%), p95 25 ms and p99 57 ms. This demonstrates a substantial first-run compilation effect on this phone; it is not a controlled Macrobenchmark or a code-change-only comparison. The phone's per-frame history exposed only two recent frames, so the aggregate system report is used for these percentiles. Smooth rendering on every device is not established.

Five cold-process launches after ART compilation measured first-frame times 516/518/498/518/517 ms (median 517 ms), and fully drawn times 830/822/820/832/840 ms (median 830 ms). Total sampled PSS was 48,246 KiB. Navigation, landscape/portrait recreation, recovery after background process termination, absence of temporary test packages and an empty crash buffer all passed. This measurement predates the final import-only structural-preflight patch; its import path is not exercised during normal startup.

The native backup password dialog displays confirmation and password-recovery guidance; its Continue button is disabled with blank credentials. No real practice export or replacement was performed in the UI check.

### Final import-safety regression

After adding the JSON structural budget on both archive write and import, the full physical-device suite passed again, including a valid-checksum archive containing 500 nested arrays. The importer rejected it before mutation and the practice snapshot remained identical. The final run parsed 10,000 bank CSV rows in 5.20 s, batch-imported them in 2.70 s, read revision-only records in 8.69 ms and aggregated 10,000 invoices/receipts in 1.02 s. Local verification totals are 73 tests, zero failures, zero lint errors and 36 dependency/toolchain notices.

Portable container details are documented in [the file format specification](../security-file-format.md). Raw local audit evidence and safety copies under ignored `app/build/audit/` contain private data and must not be published.
