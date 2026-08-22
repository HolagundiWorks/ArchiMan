# Support and Release Operations

## Scope

AMB is an offline Measurement Book. Support and release processes must preserve that boundary: no rates, billing, commercial totals, background network access, or silent data upload.

## Privacy-safe support diagnostics

Open **Export & Quantities** and select **Share support diagnostics (.json)**. The generated report contains:

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

The first boundary check rejects active commercial terminology, source network permissions, enabled network/cloud dependencies, or cleartext traffic. The second invocation also checks the merged debug manifest produced by the build.

The build must pass all formula, catalog, draft, attachment, duplicate-merge, migration, foreign-key, workflow, lock, archive, and diagnostic-report tests. The resulting local milestone is `app/build/outputs/apk/debug/app-debug.apk`.

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

Authentication, organizations, RBAC, managed synchronization, fleet management, and a web review portal require a separately approved threat model and deployment architecture. They must not be introduced as incidental dependencies into the offline field application.
