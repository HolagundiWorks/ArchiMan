# Production readiness — 6 November 2026

Follow-up to the 5 November security gap closure. This pass audited security and performance, then shipped hardening for release hygiene.

## Shipped in this pass

| Area | Change |
|---|---|
| Local access | Device screen-lock gate via `ProcessLifecycleOwner` when the app leaves the foreground (`AppLockGate`) |
| Widget privacy | Aggregates only (no project/task titles); removed public `REFRESH` intent |
| Widget reliability | 8 s timeout around DB reads |
| Dashboard perf | Deduplicated invoice/receipt and attention Flow subscriptions; search debounced 250 ms off UI |
| DB | Schema **32**: status indexes on projects, tasks, site issues, measurement sheets |
| Portal | Debug-only exception logging; portal project lists capped at 40 rows |
| Recovery | Internal recovery file moved under `noBackupFilesDir` (legacy path migrated) |
| Linking | Measurement link quantity uses SQL `SUM` instead of full-table load |
| Debug | StrictMode thread/VM policies in debug builds; `BuildConfig` enabled |
| R8 | Baseline ProGuard keep attributes for Room |

## Remaining accepted boundaries

1. **No separate ArchiMan password** — OS screen lock is the second gate; require a secure device lock in deployment guidance.
2. **Room is not SQLCipher** — practice data relies on app-private storage + device encryption.
3. **Wi‑Fi portal** — trusted LAN only; verify TLS fingerprint before entering the access code.
4. **Signing** — ship only via `gradlew.bat assembleProduction` with owner-controlled `.signing/` credentials.
5. **Large list panes** — several project screens still use `Column` + `forEach`; migrate to `LazyColumn` in a follow-up for very large projects.
6. **OEM security patch** — application fixes do not replace outdated device firmware.

## Verification (this pass)

- `compileDebugKotlin` — pass
- `testDebugUnitTest` — pass
- `lintDebug` — pass
- Room schema `32.json` identity `29230683e526e811da653f6095a54ed1`; schema 31 backup upgrade identity `f10be1fd4ecc259d1daebcf7cc86b9d8`

## Release checklist

- [x] `gradlew.bat testDebugUnitTest lintDebug`
- [ ] `gradlew.bat assembleProduction` (requires signing config)
- [ ] Install production APK; confirm backup encrypt/decrypt, portal fingerprint pairing, widget aggregates, app lock after backgrounding
- [ ] Confirm device has a PIN/pattern/biometric screen lock
- [ ] Do not publish `.signing/`, `app/build/audit/`, or practice backups
