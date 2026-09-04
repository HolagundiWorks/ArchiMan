# ArchiMan Implementation Completion Audit

- Audit updated: 4 September 2026
- Product: ArchiMan — Architectural Consultancy Management App
- Current database target: Room schema 19
- Source baseline: `321fa7d` plus the current schema-19 ERP-v3 reconciliation update

## Release identity

| Control | Current state | Result |
|---|---|---|
| User-facing brand | ArchiMan and full Architectural Consultancy Management App descriptor across app, portal, exports and diagnostics | Pass |
| Android upgrade identity | Package `com.aistudio.sitemeasure.qvtrpl` retained | Pass; intentional data-preservation decision |
| Database identity | `site_measurement.db` retained | Pass; intentional migration compatibility |
| Database version | Explicit migrations through schema 19, with no destructive fallback | Pass locally |

## Implemented product areas

| Area | Evidence in current implementation | Status |
|---|---|---|
| Portfolio and navigation | Projects, Directory, Work List and More; selected project uses Project, Work, Record and M-Book | Implemented |
| Profiles | Expanded company/practice and client profiles plus project profile | Implemented through schema 19 |
| Project brief, scope and site data | Consultancy profile, phase/stage/status, brief narratives, structured scope rows and collapsible site-data capture | Implemented foundation |
| Project planning | Tasks, schedule and specification/selection list with quantity-only purchase-order export | Implemented foundation |
| Site reporting | Structured meeting minutes and site inspections with optional photo | Implemented foundation |
| Drawing control | Drawing register, immutable revision records, transmittals and markup metadata | Implemented foundation; native DWG rendering is future work |
| Contractor and catalogue | Existing-contractor selection, standard contractor types, work hierarchy, PWD SR starter catalogue, qualification import and duplicate controls | Implemented foundation |
| Contractor rate books | Multiple contractor-owned books, PWD item import, project assignment and immutable measurement rate/amount snapshots | Implemented foundation |
| Measurement Book | Contractor-first selection, formula-dependent row columns, per-row descriptions/photos, duplication, drafts and metric/imperial conversion | Implemented |
| Review and audit | Draft/Submitted/Checked/Approved/Returned workflow, review events and approved-row database locks | Implemented |
| Local web access | Explicit, PIN-authenticated, read-only portal on the current Wi-Fi network with one-hour session timeout; support diagnostics available from the portal screen | Implemented |
| Legacy export cleanup | Obsolete portfolio Export Measurement Sheet destination removed; M-Book remains the measurement export owner | Implemented |
| Material 3 migration | Central theme, type/shape roles, outlined shared fields, current dividers/dropdowns and auto-mirrored icons | Implemented foundation |

## Product boundary

| Requirement | Result |
|---|---|
| No bills, invoices, receipts, payment collection, retention or accounting | Pass |
| Contractor rates exist only in versioned rate books and historical measurement snapshots | Pass |
| Offline-first local Room database; no silent cloud upload | Pass |
| Local portal is user-started, PIN-authenticated and read-only | Pass |
| Approval/compliance rules and jurisdiction profiles excluded | Pass |
| Launcher, Pomodoro and calculator excluded from ArchiMan | Pass |

## Verification evidence

- `testDebugUnitTest` and `assembleDebug` passed for the schema-19 branded baseline.
- 54 automated tests completed with zero failures, including migration 18→19.
- The product-boundary verification script passed.
- The Material 3 APK installed and launched successfully on Samsung SM-M115F with no app-process startup error.
- Debug APK output: `app/build/outputs/apk/debug/app-debug.apk`.
- USB upgrade installation succeeded on the Samsung SM-M115F; app-scoped logs confirmed `DB version upgrading from 18 to 19`, the activity launched and no app-process Room/SQLite/fatal error was reported. Full field-data reconciliation and hands-on interaction remain open.

## Remaining release acceptance

- [x] Install the current schema-19 APK as an in-place upgrade without clearing application data and launch it successfully.
- [ ] Confirm `PRAGMA user_version = 19` and zero foreign-key violations on a consistent DB/WAL/SHM snapshot.
- [ ] Verify expanded company/client profiles, project site data, Brief & Scope and current navigation on the target phone.
- [ ] Smoke-test contractor-first entry, conditional cells, per-row description/photo, row duplication, draft recovery and unit conversion.
- [ ] Smoke-test submit, return comment, resubmit, check, approve and approved-sheet locking.
- [ ] Verify TalkBack, large fonts, landscape and high-row-count performance.
- [ ] Produce a signed release build and managed backup/restore evidence before production rollout.

## Decision

The repository is technically accepted as a schema-19 development baseline: branding, navigation, schema migrations, build and automated tests pass. It is not yet marked production-released because schema-19 device migration, full interaction smoke testing and signed distribution evidence remain outstanding.
