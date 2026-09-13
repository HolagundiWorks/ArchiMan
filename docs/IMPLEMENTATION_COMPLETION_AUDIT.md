# ArchiMan Implementation Completion Audit

- Audit updated: 13 September 2026
- Product: ArchiMan — Architectural Consultancy Management App
- Current database target: Room schema 24
- Source baseline: current ArchiMan UX, company portability, optional Supabase configuration and secure LAN-workspace implementation

## Release identity

| Control | Current state | Result |
|---|---|---|
| User-facing brand | ArchiMan and full Architectural Consultancy Management App descriptor across app, portal, exports and diagnostics | Pass |
| Android upgrade identity | Package `com.aistudio.sitemeasure.qvtrpl` retained | Pass; intentional data-preservation decision |
| Database identity | `site_measurement.db` retained | Pass; intentional migration compatibility |
| Database version | Explicit migrations through schema 24, with no destructive fallback | Pass locally |

## Implemented product areas

| Area | Evidence in current implementation | Status |
|---|---|---|
| Portfolio and navigation | Projects, Directory, Work List and More; selected project uses Project, Work, Record and M-Book | Implemented |
| Profiles and portability | Expanded company/practice and client profiles, company logo/JSON transfer, plus password-encrypted whole-company `.archimandb` portability with a consistent SQLite snapshot, managed files, checksums, preview and staged rollback-safe restore | Implemented through schema 24; production restore drill remains open |
| Project brief, scope and site data | Consultancy profile, phase/stage/status, brief narratives, structured scope rows and collapsible site-data capture | Implemented foundation |
| Project planning | Tasks, schedule, formal decision register and specification/selection list with quantity-only purchase-order export | Implemented foundation |
| Site reporting | Daily site reports, structured meeting minutes, inspections, and snag/NCR correction and verified closure with optional evidence | Implemented in schema 24 |
| Drawing control | Drawing register, immutable revision records, transmittals and markup metadata | Implemented foundation; native DWG rendering is future work |
| Coordination | Consultant responsibility matrix plus RFI, submittal and site-instruction registers with due dates, ball-in-court, exact drawing-revision links, controlled transitions, archive and an in-app immutable activity timeline | Implemented foundation in schema 23 |
| Contractor and catalogue | Existing-contractor selection, standard contractor types, work hierarchy, PWD SR starter catalogue, qualification import and duplicate controls | Implemented foundation |
| Commercial workflow removal | Rate-book screens, assignments, pricing, displays and exports removed; schema 22 also removes obsolete commercial tables and columns through a preserving measurement-table migration | Implemented and verified |
| Measurement Book | Contractor-first selection, formula-dependent row columns, per-row descriptions/photos, duplication, drafts and metric/imperial conversion; the dormant alternate project-tab editor has been removed | Implemented |
| Review and audit | Draft/Submitted/Checked/Approved/Returned workflow, review events and approved-row database locks | Implemented |
| Local web access | Phone-hosted, named-user HTTPS administration on the current Wi-Fi network; resilient Wi-Fi-address discovery; bounded clients and request sizes; Host/Origin and content-type validation; capped idle/absolute sessions; hardened browser headers; cached project snapshots; responsive data entry across profile, contacts, contractors, planning, coordination, reporting and measurements; role-appropriate navigation and immutable audit visibility | Hardened administration foundation; field-by-field editing of every existing record, project-scoped authorization and independent security acceptance remain open |
| Legacy export cleanup | Obsolete portfolio Export Measurement Sheet destination removed; M-Book remains the measurement export owner | Implemented |
| Material 3 migration | Direct Material 3 dynamic theme and core components; parallel Carbon/Sleek tokens and custom field/dialog/navigation shells removed | Implemented and verified |
| Release footprint | R8 code optimization and resource shrinking enabled for release artifacts; line metadata and mapping support retained for diagnosable production crashes | Implemented; signed release artifact remains an acceptance gate |

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

- The schema-24 baseline adds preservation and immutable-event coverage for decisions, daily reports and snag/NCR closure; the current test count is recorded after the full verification run below.
- Migration coverage includes a 21→22 preservation test proving every quantity-bearing measurement field survives while obsolete commercial tables and columns are removed.
- Migration 22→23 adds coordination tables without rewriting existing projects or measurements; immutable-event triggers and workflow state machines are tested.
- A clean-install seeding policy test proves only reference catalogue masters are added; clients, contractors, projects, project structure and measurements start empty.
- The stricter pure Measurement Book product-boundary verification script passed.
- The quantity-only Material 3 APK installed as an in-place upgrade and launched successfully on Samsung SM-A107F with no app-process startup error.
- Debug APK output: `app/build/outputs/apk/debug/app-debug.apk`.
- USB upgrade installation succeeded on the Samsung SM-A107F without clearing application data; `MainActivity` resumed and no app-process Room/SQLite/fatal error was reported.
- The schema-23 development APK was also installed in place and visibly launched on a Samsung SM-M115F running Android 12 at 720×1411; the phone contained no projects or measurements, so populated-data migration and coordination interaction remain open on that device.
- The phone-hosted portal now uses a Carbon-style ERP information architecture with separate dashboard, project, planning, site, coordination, measurement, directory and administration views; automated portal tests enforce module-specific action visibility.
- Portal security tests cover unauthenticated data isolation, Host and Origin rejection, hardened response headers, CSRF, role denial and removal of unavailable Viewer controls.

## Remaining release acceptance

- [x] Install the current schema-22 APK as an in-place upgrade without clearing application data, verify `user_version=22`, no foreign-key violations, no commercial tables/columns and a clean launch on Samsung SM-A107F.
- [x] Install schema 23 as an in-place upgrade on Samsung SM-A107F and verify `user_version=23`, the three coordination tables, both immutable-event triggers, zero foreign-key violations and a clean launch.
- [ ] Confirm `PRAGMA user_version = 21` and zero foreign-key violations on a consistent populated DB/WAL/SHM snapshot.
- [ ] Verify expanded company/client profiles, project site data, Brief & Scope and current navigation on the target phone.
- [ ] Smoke-test contractor-first entry, conditional cells, per-row description/photo, row duplication, draft recovery and unit conversion.
- [ ] Smoke-test submit, return comment, resubmit, check, approve and approved-sheet locking.
- [ ] Complete an operator-observed `.archimandb` export/import/rollback drill using populated field data and retain the evidence.
- [ ] Complete browser-to-phone HTTPS acceptance across Admin, Editor and Viewer roles; add project-scoped authorization and optimistic concurrency before concurrent production use.
- [ ] Verify TalkBack, large fonts, landscape and high-row-count performance.
- [ ] Produce a signed release build and managed backup/restore evidence before production rollout.

## Decision

The repository is a schema-24 development baseline: the quantity-only Measurement Book, coordination, formal decisions, daily reporting, verified snag/NCR closure, encrypted company portability and broad local-browser entry foundation are implemented. It is not yet marked production-released because populated restore drills, full interaction/accessibility testing, browser concurrency/security acceptance and signed distribution evidence remain outstanding.
