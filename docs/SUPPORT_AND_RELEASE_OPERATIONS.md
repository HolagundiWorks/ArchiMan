# Support and Release Operations

> Current baseline: ArchiMan — Architectural Consultancy Management App, Room schema 24, reviewed 13 September 2026.

## Scope

ArchiMan is offline-first and quantity-only. Rates, valuation, bills, invoices, receipts, payments, accounting, background cloud access and silent data upload are not supported. A user-started, named-user HTTPS workspace on the current local Wi-Fi network is permitted. Editors and administrators may add controlled project records; viewers cannot write.

## Local Wi-Fi workspace connectivity

The phone receives its IP address from Wi-Fi DHCP; ArchiMan detects it and listens on HTTPS port `8765`. A successful **Workspace is available** screen proves address detection and TLS listener creation. If a desktop on the same subnet cannot ping/ARP the phone or connect to port `8765`, investigate router AP isolation, client isolation or WLAN partition before changing ArchiMan. A self-signed-certificate warning occurs only after network connectivity succeeds and is expected; compare the displayed SHA-256 fingerprint before proceeding.

Samsung/Bouncy Castle must use the Android Keystore directly for the TLS key manager. Android Keystore private keys are non-exportable handles and must never be copied into a BKS memory keystore.

## Privacy-safe support diagnostics

Open **Practice → Local Wi-Fi Workspace** and select **Share support diagnostics**. The generated report contains:

- application version, database schema, and Android SDK;
- counts of projects, contractors, work items, measurements, sheets, and archived sheets;
- counts of rows whose sheet or work-item reference cannot be resolved in the in-memory model;
- aggregate sheet counts by workflow status.

The report intentionally excludes names, database identifiers, member descriptions, dimensions, quantities, review comments, photo references, and filesystem paths. It is written to the application cache and disclosed only when the user chooses a destination in Android's share sheet. The file provider exposes only private cache artifacts; databases, managed photos, drafts, and external storage are outside its configured paths.

The report is diagnostic evidence, not a database backup. Use the backup and rollback runbook for recovery operations.

## Local release gate

From the repository root, run:

```powershell
./scripts/verify-product-boundary.ps1
& "$env:TEMP\codex-gradle-9.3.1\gradle-9.3.1\bin\gradle.bat" --no-daemon clean testDebugUnitTest assembleDebug
./scripts/verify-product-boundary.ps1
```

The first boundary check rejects active commercial terminology, unapproved network/cloud dependencies, or outbound cleartext configuration. It verifies the permissions required by the approved local Wi-Fi workspace. The second invocation also checks the merged debug manifest produced by the build.

The build must pass all formula, catalog, draft, attachment, duplicate-merge, migration, foreign-key, workflow, lock, archive, profile, consultancy-scope, coordination, site-control, portal-security and diagnostic-report tests. The resulting local milestone is `app/build/outputs/apk/debug/app-debug.apk`. The schema-24 baseline includes preserving 20→21, 21→22, 22→23 and 23→24 migration tests, immutable portal/coordination/site-issue audit verification, and explicit removal of the obsolete commercial schema.

## Continuous integration

`.github/workflows/android-quality-gate.yml` runs the same boundary check, unit/migration suite, and debug APK build on pushes, pull requests, and manual dispatch. It uploads the HTML unit-test report even after a failure and publishes the APK only after a successful build.

CI establishes source and build quality; it does not replace USB migration acceptance. A stable milestone still requires the device checklist in `IMPLEMENTATION_COMPLETION_AUDIT.md` before production use.

## Incident triage

1. Do not uninstall, clear storage, or overwrite the database.
2. Record the app version and generate the privacy-safe diagnostic report.
3. Capture the user-visible error and the exact workflow step.
4. If data integrity is suspected, create a consistent database/WAL/SHM backup using the rollback runbook.
5. Reproduce against a copy, never the only field database.
6. Escalate with the diagnostic report, build identifier, reproduction steps, and redacted logs.

## Deferred platform capabilities

Organisation-wide identity, project-specific permissions, managed synchronization, fleet management, remote/cloud access and concurrent conflict resolution remain deferred. The local workspace is user-started and same-Wi-Fi only; it currently exposes controlled task, approval and backlog quick entry rather than unrestricted database access.
