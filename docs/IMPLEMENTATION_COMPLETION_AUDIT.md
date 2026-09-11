# ArchiMan Implementation Completion Audit

- Audit updated: 11 September 2026
- Product: ArchiMan — Architectural Consultancy Management App
- Current database target: Room schema 22
- Source baseline: current ArchiMan UX, company portability, optional Supabase configuration and secure LAN-workspace implementation

## Release identity

| Control | Current state | Result |
|---|---|---|
| User-facing brand | ArchiMan and full Architectural Consultancy Management App descriptor across app, portal, exports and diagnostics | Pass |
| Android upgrade identity | Package `com.aistudio.sitemeasure.qvtrpl` retained | Pass; intentional data-preservation decision |
| Database identity | `site_measurement.db` retained | Pass; intentional migration compatibility |
| Database version | Explicit migrations through schema 22, with no destructive fallback | Pass locally |

## Implemented product areas

| Area | Evidence in current implementation | Status |
|---|---|---|
| Portfolio and navigation | Projects, Directory, Work List and More; selected project uses Project, Work, Record and M-Book | Implemented |
| Profiles | Expanded company/practice and client profiles, company logo/JSON portability and project profile | Implemented through schema 22 |
| Project brief, scope and site data | Consultancy profile, phase/stage/status, brief narratives, structured scope rows and collapsible site-data capture | Implemented foundation |
| Project planning | Tasks, schedule and specification/selection list with quantity-only purchase-order export | Implemented foundation |
| Site reporting | Structured meeting minutes and site inspections with optional photo | Implemented foundation |
| Drawing control | Drawing register, immutable revision records, transmittals and markup metadata | Implemented foundation; native DWG rendering is future work |
| Contractor and catalogue | Existing-contractor selection, standard contractor types, work hierarchy, PWD SR starter catalogue, qualification import and duplicate controls | Implemented foundation |
| Commercial workflow removal | Rate-book screens, assignments, pricing, displays and exports removed; schema 22 also removes obsolete commercial tables and columns through a preserving measurement-table migration | Implemented and verified |
| Measurement Book | Contractor-first selection, formula-dependent row columns, per-row descriptions/photos, duplication, drafts and metric/imperial conversion; the dormant alternate project-tab editor has been removed | Implemented |
| Review and audit | Draft/Submitted/Checked/Approved/Returned workflow, review events and approved-row database locks | Implemented |
| Local web access | Explicit, named-user HTTPS workspace on the current Wi-Fi network; Admin/Editor/Viewer roles, controlled quick entry, 30-minute login sessions, one-hour host timeout and immutable audit events | Implemented |
| Legacy export cleanup | Obsolete portfolio Export Measurement Sheet destination removed; M-Book remains the measurement export owner | Implemented |
| Material 3 migration | Direct Material 3 dynamic theme and core components; parallel Carbon/Sleek tokens and custom field/dialog/navigation shells removed | Implemented and verified |

## Product boundary

| Requirement | Result |
|---|---|
| No rates, valuation, bills, invoices, receipts, payment collection, retention or accounting | Pass |
| New measurements and exports are quantity-only | Pass |
| Offline-first local Room database; no silent cloud upload | Pass |
| Local workspace is user-started, authenticated, role-controlled, CSRF-protected and auditable | Pass |
| Approval/compliance rules and jurisdiction profiles excluded | Pass |
| Launcher, Pomodoro and calculator excluded from ArchiMan | Pass |

## Verification evidence

- `testDebugUnitTest` completed 63 tests with zero failures; lint and debug assembly passed for the schema-22 quantity-only baseline.
- Migration coverage includes a 21→22 preservation test proving every quantity-bearing measurement field survives while obsolete commercial tables and columns are removed.
- A clean-install seeding policy test proves only reference catalogue masters are added; clients, contractors, projects, project structure and measurements start empty.
- The stricter pure Measurement Book product-boundary verification script passed.
- The quantity-only Material 3 APK installed as an in-place upgrade and launched successfully on Samsung SM-A107F with no app-process startup error.
- Debug APK output: `app/build/outputs/apk/debug/app-debug.apk`.
- USB upgrade installation succeeded on the Samsung SM-A107F without clearing application data; `MainActivity` resumed and no app-process Room/SQLite/fatal error was reported. Full hands-on interaction remains open.

## Remaining release acceptance

- [x] Install the current schema-22 APK as an in-place upgrade without clearing application data, verify `user_version=22`, no foreign-key violations, no commercial tables/columns and a clean launch on Samsung SM-A107F.
- [ ] Confirm `PRAGMA user_version = 21` and zero foreign-key violations on a consistent populated DB/WAL/SHM snapshot.
- [ ] Verify expanded company/client profiles, project site data, Brief & Scope and current navigation on the target phone.
- [ ] Smoke-test contractor-first entry, conditional cells, per-row description/photo, row duplication, draft recovery and unit conversion.
- [ ] Smoke-test submit, return comment, resubmit, check, approve and approved-sheet locking.
- [ ] Verify TalkBack, large fonts, landscape and high-row-count performance.
- [ ] Produce a signed release build and managed backup/restore evidence before production rollout.

## Decision

The repository is technically accepted as a schema-22 development baseline: branding, navigation, quantity-only schema migration, build, automated tests and device upgrade checks pass. It is not yet marked production-released because full interaction/accessibility testing, browser-to-phone HTTPS acceptance and signed distribution evidence remain outstanding.
