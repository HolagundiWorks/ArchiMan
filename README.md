# ArchiMan

**ArchiMan** is an offline-first app for a freelance architect. The phone holds projects, clients, briefs, proposals, contracts, a task board, letterhead letters, fee invoices, site-visit reports, measurements and a running bill.

The Kotlin app lives in `app/`. The React files under `src/` are the earlier prototype and are not the product build.

## Stack

- Kotlin, Jetpack Compose, Material 3 with IBM Carbon blue and Gray 10/Gray 100 colours
- Room schema 31, Hilt, Navigation Compose, Coroutines
- Archi Calc and the measurement-sheet workflow ported from the prototype domain

## Run

Install JDK 17 and Android SDK 35, then:

```sh
gradlew.bat :app:assembleDebug
```

Open the `app` module in Android Studio and run it on a device or emulator. minSdk is 26. Backdrop blur is used on Android 12+; earlier devices get a solid translucent surface.

## Product surface

Projects, Clients, Invoices and Practice. An open project has Brief, Tasks, Letters, Proposal, Contract, Visits, Levels, Measure, M-Book, Invoice and Running bill. Measure includes Archi Calc, with metric and imperial lengths. Letters use the practice profile as the letterhead.

---

## Key Features & Architecture

The table below is older practice scope. The phone now keeps the freelance set only: projects, clients, briefs, proposals, contracts, tasks, letters, invoices, visits, measurement and running bills.

| Module | Features & Capabilities |
|---|---|
| **Projects & Portfolio** | Multi-project portfolio hub, client associations, built-up & plot areas, building level & room space hierarchy. Project creation contract automatically seeds a Ground Floor baseline. |
| **Measurement Book (M-Book)** | Quantity-only sheets grouped by contractor, level, and work item. Formal review lifecycle: `DRAFT` → `SUBMITTED` → `CHECKED` → `APPROVED` / `RETURNED` with audit comments. CSV export. Strictly quantity-only (no rates, amounts, or commercial billing). |
| **Record Measurement** | Contractor-first entry flow with real-time formula computation across `NOS`, `RUNNING_LENGTH`, `AREA`, `WALL_PLASTER` (with opening deductions), and `VOLUME`. Integrates directly with Archi Calc. |
| **Siteman Hub** | Site-manager command surface: Daily Site Logs (weather, manpower, work completed, materials received), HFC Drawing Package tagging & site documents, Aggregated Quantity Abstract with CSV export, Material Purchase Orders (quantity & technical spec only), and Archi Calc. |
| **Archi Calc** | Embedded AcharyaOS/Paperdesk calculation engine with expression evaluation, trigonometric functions (`sin`, `cos`, `tan`), powers, square roots, reciprocal, and a real-time 6-way length/area/volume conversion grid (`m`, `m²`, `m³`, `ft`, `ft²`, `ft³`). |
| **Site Reports & Quality** | Snag & NCR defect tracker with discovery (before) evidence, rectification (after) evidence, severity levels (`NORMAL`, `ATTENTION`, `CRITICAL`), and mandatory architect verification note signoff. |
| **Coordination & Matrix** | Multi-discipline Requests for Information (RFIs), technical Submittals, Site Instructions (SIs), and Consultants RACI responsibility matrix (Responsible, Accountable, Consulted, Informed). |
| **Drawings Register** | Master drawing register with disciplines, HFC tagging, revision genealogy (`REV-A`, `REV-B`), revision sources (client request, site condition, architect review), as-built flags, and transmittal generation. |
| **Practice & Data Portability** | Practice profile (Council of Architecture CoA registration, principal credentials, PAN, GSTIN), password-encrypted practice backup/export (`.archimanbackup`), restore from file, and diagnostics. |

---

## Backup and recovery

Use **Practice → Backup and restore → Save backup**, or open Backup and restore from the company opening screen. Save the file somewhere accessible if the phone is lost.

New `.archimanbackup` files are password-encrypted containers (AES-256-GCM with PBKDF2-HMAC-SHA256, 600,000 iterations). Inside, a ZIP package contains every practice database table, readable attachment files, a schema identity and SHA-256 integrity checks. Attachments that cannot be read are listed in the restore preview; their original references are retained. Choose a unique password of at least 12 characters and keep it separately: there is no password recovery. Older unencrypted ZIP backups still restore. The device-memory-aware limit is up to 64 MB (32 MB on a 256 MB app heap), with an 8 MB database-metadata limit. Schema-31 backups restore directly; known schema-28/29/30 backups are upgraded. This format is separate from the demo `.archimandb.json` package.

Restore previews the practice, date, project/record counts and unavailable attachments, then asks before replacing records. The app saves a pre-restore recovery package internally before modifying rows. Failed validation rolls back replacement. After a successful restore, reopen as Admin. **Save pre-restore recovery copy** exports the latest internal recovery package with password protection; keep an external copy because uninstalling removes internal recovery files.

## Validation

```sh
gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w com.holagundi.archiman.test/com.holagundi.archiman.BackupDeviceChecks
```

Device checks use an isolated database and temporary files. They verify complete table round-trip, attachments, recovery, failed-import rollback, partial payments, GST/TDS, contractor schedules, unit conversions, concurrency, TLS, encrypted documents and large-data workloads. They do not restore over the user's practice.

## Secure portal, files and production signing

The Wi-Fi portal is HTTPS-only (TLS 1.2/1.3). Before entering its access code, compare the browser certificate SHA-256 fingerprint with **Practice → Wi-Fi portal** on the phone. Approve its local self-signed certificate only after an exact out-of-band match. Pair again after the address changes or app data is cleared. No HTTP fallback is provided.

Portal downloads offer **Protect & download** or an explicit standard-file download. Under **Backup and restore → Protect document files**, encrypt or decrypt PDF, Excel, CSV and other files as `.archimansecure` containers. Standard files and older copies remain readable; protect them before sharing and remove unwanted copies yourself. Passwords are not saved in preferences or backups.

Production signing is configured from ignored `.signing/release.properties`, or environment variables `ARCHIMAN_STOREFILE`, `ARCHIMAN_STOREPASSWORD`, `ARCHIMAN_KEYALIAS`, `ARCHIMAN_KEYPASSWORD`. `scripts/New-ReleaseSigning.ps1` creates a separate owner key with random credentials and a restricted Windows directory; it refuses to overwrite an existing identity. Keep an offline backup of the entire private signing directory. Never commit or share it. Builds without signing configuration remain unsigned; partial configuration is rejected. Use `gradlew.bat assembleProduction` to require a valid non-debug signing identity. The development-signed phone installation needs a deliberate data-preserving migration before switching signing identities; do not uninstall without a verified external backup.

CI runs regression/lint/release builds, scans exact production Maven versions against OSV and defines Android API 26/35 emulator checks. The manual workflow accepts a longer soak duration. CI jobs must actually execute successfully before those environments are considered validated. Locally: `gradlew.bat writeSecurityInventory`, `python scripts/check_dependencies.py`, and `python scripts/device_soak.py --minutes 120` (with the debug app/test APK already installed).

The Admin portal includes company letterhead/logo design, client and contractor profiles, a CPWD-referenced item catalogue, estimates, project-wide revisioned contractor schedules, unit-converted measurements and tax-exclusive running bills. See [workflow details](docs/ux/project-schedules-letterhead-and-measurements.md).

## Earlier web prototype

The files under `src/` are a React + Vite prototype of the same workflows. They are not the Android product. Do not treat `npm run dev` as the app.

```sh
npm install
npm run dev
```

---

## License
MIT — See `LICENSE` and `NOTICE`.

## Appearance

Choose **System / Light / Dark** at the top of the Practice menu. The HTTPS portal has its own browser theme selector, including its sign-in page. Carbon blue highlights and separate success, warning, error and information colours are applied across both. See [Carbon colour roles](docs/ux/carbon-colour-system.md).

Version 1.1.0 adds IBM Plex Sans, official Carbon icons, square tiles and controls, labeled navigation, inline notifications and real project/fee charts. Android uses native Compose adapters; the portal bundles official Carbon components and charts, with searchable, sortable, paginated tables. See [components and maintenance](docs/ux/carbon-components.md).

Rebuild portal assets with `npm ci --prefix portal-ui --ignore-scripts` followed by `npm run build --prefix portal-ui`. Run browser checks with `npm test --prefix portal-ui`.

The [12-principle UX audit](docs/ux/ux-principles-audit-2026-11-06.md) assesses the current Android/portal workflows and prioritizes remaining save-state, measurement, billing, recovery and accessibility improvements.

The [1.1.1 UX fixes](docs/ux/ux-remediation-2026-11-06.md) add reliable save feedback, encrypted local editor drafts, repeat measurement capture, exact attention-item navigation, aligned review tables and a project-scoped contractor billing handoff.

The [1.2.0 measurement screen](docs/ux/measurement-capture-1.2.0.md) adds a fixed text/numeric keyboard with system-IME blocking, recorded-entry navigation and protected editing, top save/edit controls, visible dropdown arrows and contextual help.
