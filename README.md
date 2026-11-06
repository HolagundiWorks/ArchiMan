# ArchiMan

**ArchiMan** is a **native Android** app (Kotlin + Jetpack Compose) for freelance architects. The phone is the system of record: projects, clients, practice profile, tasks, drawings, site work, measurements, fees and encrypted backups stay on-device.

There is no React / web product app in this repository. The only JavaScript package is `portal-ui/`, which builds static Carbon assets for the optional local HTTPS Wi‑Fi portal.

## Stack

| Layer | Choice |
|---|---|
| Language | Kotlin 17 |
| UI | Jetpack Compose, Material 3, IBM Carbon colours / IBM Plex Sans / Carbon icons |
| Architecture | Hilt, Coroutines, Navigation Compose |
| Storage | Room (schema 31), offline-first |
| Portal assets | Bundled Carbon web components under `app/src/main/assets/carbon/` (built from `portal-ui/`) |
| Application ID | `com.holagundi.archiman` |
| SDK | minSdk 26, targetSdk / compileSdk 35 |
| Version | 1.2.0 (versionCode 8) |

Primary code lives in `app/src/main/java/com/holagundi/archiman/` (`ui/`, `data/`, `domain/`, `portal/`).

## Run

Install JDK 17 and Android SDK 35, then:

```sh
gradlew.bat :app:assembleDebug
gradlew.bat :app:installDebug
```

Or open the `app` module in Android Studio and run on a device or emulator.

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.holagundi.archiman/.MainActivity
```

## Product surface

**Practice shell**

- **Dashboard** — practice at a glance, KPIs, fees overview, project mix, needs attention, recent projects; floating search and create actions
- **Projects** — portfolio list and filters
- **Practice** — profile, clients, invoices, finance, backup/restore, Wi‑Fi portal, demo company, appearance (System / Light / Dark)

**Inside a project**

Overview, brief, tasks (movable Kanban tiles), documents / drawings / letters, site visits & issues, coordination, Siteman, measurements (record + Measurement Book), fees (proposal, contract, invoices, running bill), levels & setup.

**Optional Wi‑Fi portal**

HTTPS-only desktop workspace on the local network for letterhead, catalogues, estimates, schedules and document export. Pair by comparing the certificate fingerprint shown on the phone.

## Features

| Area | Capabilities |
|---|---|
| **Projects & portfolio** | Multi-project hub, clients, levels & rooms; new projects seed a Ground Floor |
| **Dashboard & attention** | Open tasks, sheets to review, site issues, fee balances, project status mix |
| **Measurement Book** | Quantity sheets by contractor / level / item; review lifecycle `DRAFT` → `SUBMITTED` → `CHECKED` → `APPROVED` / `RETURNED`; CSV export |
| **Record measurement** | Contractor-first capture with live formulas (`NOS`, length, area, plaster, volume) and Archi Calc |
| **Tasks** | To do / Doing / Done tiles with move icons and record actions |
| **Siteman** | Daily logs, material orders, quantity abstract, site documents |
| **Site quality** | Snags / NCRs with evidence, severity and verification |
| **Coordination** | RFIs, submittals, site instructions |
| **Drawings** | Register, revisions, transmittals |
| **Fees** | Proposals, contracts, client invoices, receipts, GST/TDS-aware balances |
| **Backup** | Password-encrypted `.archimanbackup` (AES-256-GCM); restore with preview and pre-restore recovery |

## Backup and recovery

**Practice → Backup and restore → Save backup** (also available from the company opening screen). Keep a copy off the phone.

New backups are password-encrypted containers (AES-256-GCM, PBKDF2-HMAC-SHA256, 600,000 iterations) holding tables, readable attachments, schema identity and SHA-256 checks. Use a unique password of at least 12 characters — there is no recovery. Older unencrypted ZIP backups still restore. Schema-31 restores directly; known 28–30 backups upgrade. This format is separate from the demo `.archimandb.json` package.

## Validation

```sh
gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w com.holagundi.archiman.test/com.holagundi.archiman.BackupDeviceChecks
```

Device checks use an isolated database. They do not restore over the user’s practice.

Portal UI assets:

```sh
npm ci --prefix portal-ui --ignore-scripts
npm run build --prefix portal-ui
npm test --prefix portal-ui
```

## Security notes

- Portal is HTTPS-only (TLS 1.2/1.3). Match the browser certificate fingerprint with **Practice → Wi‑Fi portal** before trusting it.
- **Protect document files** can wrap PDF/Excel/CSV as `.archimansecure` containers.
- Production signing uses ignored `.signing/release.properties` or `ARCHIMAN_STOREFILE` / `ARCHIMAN_STOREPASSWORD` / `ARCHIMAN_KEYALIAS` / `ARCHIMAN_KEYPASSWORD`. See `scripts/New-ReleaseSigning.ps1`. Never commit signing material.

## Docs

- [Carbon colour system](docs/ux/carbon-colour-system.md)
- [Carbon components](docs/ux/carbon-components.md)
- [UX principles audit](docs/ux/ux-principles-audit-2026-11-06.md)
- [UX remediation](docs/ux/ux-remediation-2026-11-06.md)
- [Measurement capture 1.2.0](docs/ux/measurement-capture-1.2.0.md)
- [Project schedules & letterhead](docs/ux/project-schedules-letterhead-and-measurements.md)
- [Security file format](docs/security-file-format.md)

## License

MIT — see `LICENSE` and `NOTICE`.
