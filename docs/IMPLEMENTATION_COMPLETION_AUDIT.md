# AMB Pure Measurement Book Completion Audit

Audit date: 22 August 2026
Current database target: schema 10

## Product boundary

| Requirement | Evidence | Result |
|---|---|---|
| No rates, amounts, bills, invoices, or payments in active product | Commercial routes/entities/APIs removed; source audit finds commercial fields only in the intentional 4→5 removal migration and its preservation test | Pass |
| Quantity-only measurement formulas | `QuantityCalculator` is shared by canonical entry and covered by formula tests | Pass |
| Offline operation | LAN server removed, cleartext disabled, Firebase/Retrofit/OkHttp removed, and merged APK manifest contains no Internet/network permission | Pass |

## Canonical field workflow

| Requirement | Evidence | Result |
|---|---|---|
| Contractor-first selection | Canonical wizard/session requires contractor before work item | Pass |
| Contractor-specific items and standard trades | Contractor qualifications plus versioned JSON catalog for twelve standard trade types | Pass |
| Work type → item → formula hierarchy | Expandable Work List and persisted work types/formulas | Pass |
| Formula-dependent columns | Canonical row editor conditionally shows No, Length, Breadth, and Height | Pass |
| Per-row member description and optional photo | Row model, validation, managed attachment storage, and persistence tests | Pass |
| Duplicate one or selected rows | Canonical editor row and bulk duplication with Undo | Pass |
| System keyboard | Decimal keyboard with formula-aware Next/Done traversal | Pass |
| Draft recovery | Session-scoped auto-save and restoration tests | Pass |

## Data integrity and history

| Requirement | Evidence | Result |
|---|---|---|
| Preserve existing measurements | Explicit migrations 4→10 and preservation tests; last verified device migration at schema 8 retained six of six rows | Pass through schema 8 on device; schemas 9–10 pending USB verification |
| Sheet header/row split | `measurement_sheets`, linked rows, shared batch transaction, formula/UOM snapshots | Pass locally |
| Foreign-key integrity | Eight row relationships plus sheet-header relationships and `foreign_key_check` tests | Pass locally |
| Duplicate work-item merge | Preview, canonical selection, transactional repointing, aliases, archive, and database test | Pass |
| Review and approval | Draft/Submitted/Checked/Approved/Returned transitions, correction revisions, comments, and UI | Pass locally |
| Immutable approved records | SQLite update/delete triggers and lock tests | Pass locally |
| Non-destructive removal | Measurement deletion paths archive sheets and retain rows; audit event test | Pass locally |

## Operational controls

| Requirement | Evidence | Result |
|---|---|---|
| Encrypted platform backup | Android backup allowlist requires client-side encryption and includes DB, attachments, and drafts | Pass for supported Android backup/device-transfer channels |
| Backup/rollback runbook | `MIGRATION_BACKUP_AND_ROLLBACK.md` | Pass |
| Automated build/test gate | Current `testDebugUnitTest` and `assembleDebug` pass | Pass |
| Product-boundary regression gate | Local/CI script rejects commercial active source, network permissions/dependencies, and cleartext traffic | Pass |
| Privacy-safe support diagnostics | User-initiated metadata-only JSON report with explicit content exclusions and regression tests | Pass |
| CI artifacts | Workflow retains unit-test reports and the successfully built debug APK | Pass (workflow defined; remote execution depends on repository CI) |
| Stable USB deployment | Device is currently absent from `adb devices` | Pending |
| Multi-user authentication, organizations, RBAC, managed sync, device management, CI/CD release service | Phase 6 platform work | Future enterprise deployment scope |

## Release decision

The repository is a locally complete pure Measurement Book implementation through the schema-10 review workflow. It is not yet eligible for final device acceptance because schemas 9 and 10 have not been executed against the connected phone. Final acceptance requires APK installation, launch-log inspection, schema/count/sheet/audit/foreign-key verification from a WAL-consistent device database copy, and a short interaction smoke test.

## Schema-10 device acceptance checklist

- [ ] `adb devices -l` shows one authorized device.
- [ ] Install the debug APK with `adb install -r` without clearing application data.
- [ ] Launch `com.aistudio.sitemeasure.qvtrpl/com.example.MainActivity` and inspect startup logs for Room migration or fatal exceptions.
- [ ] Copy the database, WAL, and SHM files together before offline inspection.
- [ ] Confirm `PRAGMA user_version` is `10` and `PRAGMA foreign_key_check` returns no rows.
- [ ] Confirm the existing measurement count is unchanged from the pre-upgrade device count of six.
- [ ] Confirm every measurement has a valid sheet link and a non-empty formula/UOM snapshot.
- [ ] Confirm the review-event table and approved-record protection triggers exist.
- [ ] Smoke-test contractor-first entry, formula-dependent cells, per-row description, optional photo, row duplication, draft recovery, submission, return comment, approval, and approved-sheet locking.
