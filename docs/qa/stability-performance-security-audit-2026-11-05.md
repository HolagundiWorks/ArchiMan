# ArchiMan stability, performance and security audit

Follow-up: [security gap closure and current limitations](security-gap-closure-2026-11-05.md). The results below describe the original audit; the follow-up supersedes its HTTP, portable-backup and production-signing status.

Audit date: 5 November 2026 (Asia/Kolkata). Scope: current native Android app and its local Admin web portal, including the company/letterhead, contractor schedule, measurement, import, billing, GST/TDS and backup workflows. Tests run against isolated databases and generated sample files. Phone records were backed up before installation.

## Test environment and interpretation

Physical device: Samsung Galaxy M11 (SM-M115F), Android API 31, 256 MB app heap. The device reports security patch **2023-11-01**, which is a significant environmental limitation as of the audit date; app-level fixes do not update the OS. Android minimum API 26 and target API 35 are checked statically; other OS versions and physical devices were not exercised. The baseline installation was a debug build. The final installed app is an optimised, non-debuggable local-test copy signed with the same development certificate to preserve its update identity and practice data. Production release output remains unsigned. An isolated `.audit` application uses release optimisation, a separate package/data directory and a development signing key solely for verification; it is not a production release.

This is a source, regression, malformed-input, concurrency and bounded-load audit. It is not certification, a full independent penetration test, a guarantee of optimal performance on every device, or a substitute for production monitoring.

## Confirmed findings and corrections

| ID | Priority | Finding | Correction / verification |
|---|---|---|---|
| A01 | Critical data integrity | Demo-company replacement cleared the current practice outside its insertion transaction; a late import failure could leave the practice empty. | Replacement now deletes/inserts/checks relationships in one Room transaction, including demo-schedule setup. Late-invalid and oversized files are rejected; full-table snapshots verify rollback. |
| A02 | High availability | Portal accepted sockets into an unbounded executor queue; its limit counted running workers rather than admitted clients. | Count connections before admission, bound the executor queue, cap at 12 clients, add socket/read deadlines, and close connected clients when stopped. Saturation/recovery tests exercise the limit. |
| A03 | High release hygiene | Release configuration used the development/debug signing key. | Production release is unsigned until the owner supplies a proper signing configuration. Only the explicitly separate audit build uses a development key. |
| A04 | High privacy | Automatic OS backup was enabled despite private bank, client and contractor information. | Disabled OS cloud backup and excluded app files, databases and preferences from supported cloud/device-transfer rules. Explicit in-app backup remains available. Previously created external/cloud copies are not retroactively erased. |
| A05 | High input safety | Monetary scientific notation could request excessive BigDecimal expansion. | Validate bounded plain-decimal syntax before conversion; reject huge exponents, non-finite and overlong amounts. Bank import errors also truncate hostile cell text. |
| A06 | High financial integrity | Repeated stage billing could create duplicate invoice drafts; review decisions could race between state validation and update. | Transactional stage-state checks and review transitions; collision-resistant invoice identifiers. Concurrent payments, duplicate stage billing and conflicting review actions are tested. |
| A07 | Medium protocol security | Ambiguous/duplicate headers, transfer encoding, absolute-target mismatches and unlimited form-key counts were accepted. | Reject ambiguous framing, invalid versions/lengths/headers, foreign authority and duplicate/excessive parameters. Smaller unauthenticated body limit; authenticated workspace uploads retain their bounded allowance. Parser fuzz and fragmented-read checks included. |
| A08 | Medium import integrity | Measurement CSV headers could overwrite project/floor/contractor context values. | Only documented measurement columns are accepted. Scope comes from the selected form context; unsupported/duplicate headers fail before posting. |
| A09 | Medium spreadsheet security | Native measurement CSV did not share the portal's formula-neutralisation logic. | One CSV cell encoder protects all export paths, including whitespace/BOM-prefixed formulas. Excel uses inline text cells; invalid XML control characters are removed while valid Unicode survives. |
| A10 | Medium restore stability | Valid-checksum backups could contain malformed finance JSON or invalid GST modes that later crashed readers. | Validate structured metadata before restoring. Reject malformed settings, bank/schedule/bill/estimate records and invalid tax modes. Checksum validity is not treated as proof of semantic safety or authorship. |
| A11 | Medium compatibility | URL decoding used a Java overload requiring API 33, although minimum API is 26; foreground-service constant lacked an older-API guard. | Use the compatible encoding-name overload and guard the service type. Android lint API error removed. |
| A12 | Medium scoping | Sheet export did not verify the requested project owned the sheet. | Enforce project ownership before export; negative cross-project test included. |
| A13 | Performance / availability | Schedule lookups loaded bank rows; bank UI composed all rows and parsed JSON repeatedly during sorting; dashboard aggregation contained nested searches. | Query finance records by kind, use primary-key settings lookup, lazy bank rows, background parsing/sorting and dashboard calculations, and indexed in-memory grouping/sets. |
| A14 | Performance | Measurement-book filtering scanned entries repeatedly and could compose every sheet/entry. | Group entries once; page sheets in batches of 25 and visible entries in batches of 100. Exports still include the full selected dataset. |
| A15 | Memory / export stability | Large metadata, spreadsheet/PDF output and high-resolution logos could cause excessive allocations on a 256 MB-heap device. | 5 MB company-import limit, 8 MB database metadata and 1 MB archive manifest limits, with a device-memory-aware total backup ceiling (32 MB on this 256 MB-heap phone; up to 64 MB on larger heaps), bounded spreadsheets, PDF size/page limits, validated/downsampled logos. Errors fail explicitly rather than silently truncating records. |
| A16 | Startup / stability | Demo catalogue asset parsing occurred on the UI dispatcher; some write failures escaped coroutine handlers. | Move asset parsing off the UI thread and return operation errors to the screen while propagating cancellation. |
| A17 | Import performance | Statement fingerprint hex encoding created a formatter for every digest byte. | Allocation-bounded hexadecimal encoding and reused per-import digest. Fingerprints remain identical, preserving duplicate detection. |

| A18 | High session isolation | Practice restoration/replacement could leave browser sessions or queued authenticated writes attached to the replacement records. | Stop the portal before replacement, and recheck server/session authorization inside the database transaction before workspace writes. Device regression queues a valid form behind a transaction, closes the server and verifies no write; restore and blank-practice replacement also verify the portal closes. |

## Remaining security boundaries

- The Wi-Fi portal uses local **HTTP**, not TLS. Access codes, session cookies and document contents can be observed or altered by someone able to intercept that network. Use only a trusted private network; do not expose/forward the port publicly. HTTPS or an authenticated encrypted tunnel remains necessary before untrusted-network use. Host/origin/CSRF checks do not encrypt traffic.
- Local Admin entry intentionally has no password, as requested. Anyone with access to the unlocked app can access practice data. The original debug build permitted private-data access through paired developer ADB. The final local-test build is non-debuggable and blocks `run-as`; it still uses the existing development certificate and is not a production signing setup. Production deployment needs a non-debuggable build, owner-managed signing and an appropriate device-lock policy.
- The database and user-exported `.archimanbackup`, CSV, XLSX and PDF files are not application-level encrypted. Android app-private storage and device protection provide the local boundary. Backup checksums detect corruption; they are not encryption or signatures.
- The dependency check only identifies advisories present for matched package/version records in OSV. No matches do not prove the absence of vulnerabilities. Existing dependency/toolchain update warnings should be addressed through a separate compatibility-tested upgrade.
- No broad Android-version matrix, long-duration battery soak, low-storage hardware fault injection, external network penetration test or production telemetry was available in this run.

## Reproduction

Local regression/lint/build checks: `gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest assembleRelease assembleAudit`.

Device runner: `adb shell am instrument -w com.holagundi.archiman.test/com.holagundi.archiman.BackupDeviceChecks`. It uses isolated sample data and includes backup/migration, record CRUD, agreements/PDF, financial settlement, contractor revisions, failed import rollback, concurrency, malformed portal requests, connection limits and 10,000-row workloads.

Generated evidence lives under ignored `app/build/audit/`, with device metrics in the app's test cache until copied. Do not publish raw private phone safety copies.

## References

- Android backup guidance: https://developer.android.com/privacy-and-security/risks/backup-best-practices
- Android Auto Backup behaviour and manufacturer caveats: https://developer.android.com/identity/data/autobackup
- Android startup metrics: https://developer.android.com/topic/performance/vitals/launch-time
- Vulnerability lookup service: https://osv.dev/

## Final results

Final measured results are recorded below. The final integration suite was rerun after the session-boundary fix and passed; the optimized non-debuggable APK was then reinstalled and the temporary instrumentation package removed.

### Automated checks

- 63 local test methods passed with zero failures, including parser fuzzing over 1,000 random malformed inputs, fragmented headers, HTTP framing/parameter attacks, dangerous numeric input, spreadsheet injection, invalid XML/Unicode and archive metadata bounds.
- Full isolated device suite passed: prior functional tests plus failed-company-import rollback, malformed backup metadata rejection, oversized import rejection, reserved CSV scope columns, concurrent payments, double stage billing, twelve approval/return races, cross-project export rejection, login throttling, unauthenticated body budget, twelve-connection saturation/recovery server stop/client closure, stale queued session rejection, and portal shutdown before practice restore/replacement.
- Android lint: zero errors. 67 non-blocking warnings remain, chiefly dependency/AGP update notices, unused resources, plurals and a Compose modifier convention; these are not suppressed as a fake clean baseline.
- OSV query checked 129 resolved Maven package/version records: zero advisory matches, with 129 successful result entries. Coverage is limited to OSV's published matching records.
- Release and audit build paths pass minification/resource shrinking. Generated release manifest is non-debuggable and has automatic backup disabled. App service/provider components are non-exported; AndroidX's exported profile installer receiver is restricted by the system `android.permission.DUMP` permission. Production APK signature check confirms it remains unsigned.
- Comparison of **all 56 existing practice tables before/after the debug audit** found zero changed or missing tables. The final optimized installation used the matching signing certificate and Android's data-preserving replacement; dashboard smoke checks verify the practice opens. Temporary audit-package data is separate and removed afterward.

### Large-data timings on the phone

These are exploratory measurements in the debug instrumentation build, not statistically controlled Macrobenchmark results. Background load, ART compilation and thermal state can affect comparisons. Mutations use isolated data.

| Workload | Measured time |
|---|---:|
| 10,000 bank CSV rows, initial parser | 8.74 s |
| 10,000 bank CSV rows, optimized parser | 5.19 s |
| Batched bank import, 10,000 new rows | 2.71 s |
| Full finance read, median of five, 10,009 rows | 242.0 ms |
| Schedule-only read, median of five, 3 rows | 10.5 ms |
| 10,000 invoices plus receipts, exact cash/TDS aggregation | 1.00 s |

The bank-import repeat returned zero new rows. The dashboard stress check reconciled INR 9,000,000 cash plus INR 1,000,000 TDS to INR 10,000,000 in settled invoices, with zero outstanding. Bank, catalogue and dashboard calculations are kept away from UI-heavy eager rendering. These tests do not establish a leak-free long-running 10,000-row UI session.

The final regression rerun measured CSV parsing at 6.27 s, batched import at 2.69 s, broad finance reads at 251.7 ms, schedule reads at 8.5 ms and dashboard aggregation at 1.08 s. The variation illustrates why these timings are exploratory rather than fixed performance guarantees.

## Follow-up priorities

1. Before wider distribution: configure an owner-controlled production signing key and test installation/update recovery on supported Android versions. Replace local HTTP with HTTPS or an authenticated encrypted tunnel before allowing untrusted-network access.
2. Protect portable data: add password-encrypted backup/export options with recovery guidance; decide whether optional app unlock is appropriate for shared devices.
3. Extend reliability coverage: run a multi-hour measurement/billing/import/export soak, test interrupted imports/restores and low storage on disposable devices, and measure scrolling/frame times with Macrobenchmark on low- and mid-range devices.
4. Maintain dependencies: resolve the remaining update notices in a separate upgrade, rerun lint/regression tests and keep vulnerability checks in CI.

## Final installed-build verification

The final optimized, non-debuggable local-test build passed read-only Dashboard, Projects and Practice navigation, landscape/portrait recreation and recovery after the background app process was killed. The dashboard reopened successfully. Device crash buffer was empty at completion. Both temporary audit and test packages were absent; screen-rotation settings were restored.

Five cold-process launches with the existing practice measured first-frame times of 615, 577, 572, 536 and 562 ms: **572 ms median**, compared with the initial debug median of **4,505 ms**. Fully drawn times were 1,255, 1,149, 1,150, 1,110 and 1,129 ms: **1,149 ms median**. This comparison includes release optimization and differs in build type; it does not isolate individual code changes or include device reboot/disk-cache cold starts.

Final sampled total PSS was **40,480 KiB (39.5 MiB)**, compared with **104,864 KiB (102.4 MiB)** in the original debug snapshot. These are point-in-time memory samples, not peak or leak measurements. Unsigned release APK size fell from **12,785,162 bytes** without shrinking to **1,901,236 bytes** with optimization/resource shrinking, approximately **85% smaller**.
