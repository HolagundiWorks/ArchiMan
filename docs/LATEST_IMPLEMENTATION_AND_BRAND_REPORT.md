# ArchiMan Latest Implementation and Brand Report

- Report date: 4 September 2026
- Product: **ArchiMan — Architectural Consultancy Management App**
- Code baseline: `321fa7d` plus the current schema-20 completion tranche
- Database baseline: Room schema 20

## Executive summary

ArchiMan has moved from a measurement-only utility to an offline-first architectural consultancy management application with a field-first Measurement Book. The current baseline combines project and practice profiles, project briefs and scope, planning, site records, drawing-control foundations, PWD work catalogues, contractor rate books and controlled measurement review in one project hierarchy.

The product is not an ERP accounting package. Contractor rates are supported through named/versioned rate books and immutable measurement snapshots, but bills, invoices, receipts, retention, payment processing, GST accounting and consultancy fee accounting remain excluded.

## Brand implementation

| Surface | Current identity |
|---|---|
| Short name | ArchiMan |
| Full descriptor | ArchiMan — Architectural Consultancy Management App |
| Android application label | ArchiMan |
| Gradle root project | ArchiMan |
| In-app mastheads | ArchiMan / context-specific screen title |
| Local workspace | ArchiMan-branded HTTPS workspace with named accounts, role controls and audited quick entry |
| Exports and diagnostics | ArchiMan-branded filenames and headings |
| Documentation | ArchiMan terminology and schema-18 baseline |

The Android package ID `com.aistudio.sitemeasure.qvtrpl` and Room filename `site_measurement.db` are intentionally retained. Renaming them during a visual rebrand would create upgrade and data-migration risk without user value.

## Current information architecture

```text
Portfolio
├── Projects
├── Contacts
│   ├── Clients
│   └── Contractors
├── Library
└── Practice
    ├── Company Profile, Backup & Connections
    └── Local Wi-Fi Portal / Support Diagnostics

Selected Project
├── Project
├── Work
├── Record
├── M-Book
└── More
    ├── Brief & Scope
    ├── Planning
    ├── Onboarding & Controls
│       ├── Drawings
│       ├── Site Reports
│       ├── Onboarding & Controls
│       ├── Project Team
│       └── Rate Books
```

This hierarchy follows the comparative UX study: project context is selected before project tools, frequent field actions remain persistent, and long-tail tools move under labelled More menus. The former project tab row has been removed so the bottom bar is the only permanent project navigation surface.

## Latest implemented capabilities

### Practice and project setup

- Expanded company/practice profile with company type, country, PAN, principal, qualification and registration fields.
- Company logo selection with a durable app-managed copy for reports and future branded outputs.
- Portable JSON company-profile backup and confirmation-based restore, including an optional embedded logo; project and measurement records are intentionally excluded.
- Expanded client directory with type, contact person, email, correspondence address, preferred communication and notes.
- Project profile with project code/type/status, client/location, architect, programme dates and area data.
- Project-specific execution levels rather than a fixed floor count.
- Collapsible project site-data capture for dimensions, orientation, access, conditions, surroundings, topography, utilities, structures, vegetation and supplied legal/planning information.

### Brief and consultancy scope

- Multi-select Architectural, Landscaping, Part Consultancy and Miscellaneous consultancy types.
- Design, Execution and Handover phases.
- Design stage and brief review status.
- Objectives, requirements, preferences, constraints and clarifications.
- Structured Scope, Deliverable, Exclusion and Responsibility rows.
- Versioned project-type onboarding questionnaires for residential, commercial, healthcare, hospitality, education and other projects.
- Per-question responses and clarification notes with completion progress.

### Project controls

- Client and technical approval register with Pending, Submitted and Approved transitions.
- Project backlog grouped by drawings, client approvals, technical approvals, coordination and general actions.
- Phase and priority classification with reversible close/reopen handling.
- All control records are project-scoped, offline-first and included in preserving schema migration 19 → 20.

### Planning and site administration

- Project tasks and schedules.
- Specification/material selection register.
- Quantity-only purchase-order export.
- Structured meeting minutes.
- Site inspections with optional photo evidence.

### Drawing control

- Drawing register metadata.
- Immutable drawing revision records.
- Transmittal records linked to exact revisions.
- Non-destructive markup metadata and a replaceable viewer-engine boundary.

Native DWG rendering, calibrated graphical measurement and full annotation tooling are not yet implemented.

### Contractor, PWD and rate-book workflow

- Standard contractor types and contractor-specific qualified work items.
- Importing items from an existing contractor during contractor setup.
- Work Type → Work Item → Formula hierarchy.
- Curated Karnataka PWD SR Buildings 2023–24 starter catalogue with provenance.
- Multiple named/versioned contractor rate books.
- Import of canonical PWD items into a rate book.
- Project-specific rate-book assignment.
- Immutable rate and calculated-amount snapshots on measurements.

### Measurement Book

- Contractor-first selection followed by filtered work item and floor/location.
- Spreadsheet-style rows using the Android system keyboard.
- Per-row member description and optional photo.
- Formula/UOM-dependent No, Length, Breadth and Height columns.
- Duplicate one row or a selected set of rows, with Undo.
- Metric/Imperial entry toggle with compatible value/UOM conversion and recalculation.
- Draft recovery and managed attachment storage.
- Draft, Submitted, Checked, Approved and Returned states.
- Review comments, immutable events and database-enforced approved-row locking.

### Offline access and operations

- Room/SQLite remains the local source of truth.
- Optional Supabase Project URL and publishable-key setup with an API-gateway connection test. Secret/service-role keys are rejected, and connecting does not enable upload or synchronisation.
- User-started, named-user HTTPS web workspace on the phone's current Wi-Fi network.
- Admin, Editor and Viewer access; audited task, approval and backlog quick entry for authorised users.
- One-hour portal timeout and no silent cloud upload.
- Privacy-safe support diagnostics.
- Support diagnostics are available from the Local Wi-Fi Portal screen; the obsolete portfolio measurement-export screen has been removed.
- Explicit backup/migration runbook and CI quality gate.

### Material 3 user interface

- Central `ArchiManTheme` with Material 3 color, typography and shape roles.
- Material 3 outlined input/search behavior across shared feature forms.
- Current divider and dropdown APIs.
- Auto-mirrored directional icons.
- Existing hierarchy and field workflows retained without data-model changes.

## Database progression

| Schema | Principal addition |
|---|---|
| 15 | Project schedules, meeting minutes and site inspections |
| 16 | Drawings, immutable revisions, transmittals and markup metadata |
| 17 | Company profile and expanded project profile |
| 18 | Project consultancy profile and structured Brief & Scope register |
| 19 | Expanded practice/client profiles and project site data |

All current migrations are explicit; destructive fallback is not enabled.

## Verification status

- Schema-21 `testDebugUnitTest` (61 tests) and `assembleDebug`: passed.
- Automated tests: 54 passed, 0 failed.
- Product-boundary regression check: passed.
- Latest debug APK: `app/build/outputs/apk/debug/app-debug.apk`.
- Schema-21 USB upgrade: installed in place on Samsung SM-M115F; clean app-process startup confirmed with no fatal or migration error.
- Material 3 design-foundation update: installed and launched successfully on the same device.
- Full field-data reconciliation and interaction/accessibility acceptance: pending.

## Open roadmap

### Next release priorities

1. Complete schema-21 field-data reconciliation, browser-to-phone HTTPS acceptance and full interaction/accessibility smoke testing on the upgraded device.
2. Versioned project-type onboarding templates and generated reviewed brief snapshots.
3. Rate-book lifecycle, clone/revision, effective dates, assignment history and change-impact preview.
4. Controlled drawing-file intake, managed storage and transmittal/report exports.
5. Meeting-minute and site-inspection PDF/CSV outputs.
6. Split the monolithic ViewModel, repository, entities and DAOs into feature boundaries.

### Later enterprise scope

- Native licensed DWG viewing, annotation and calibrated measurement.
- RFIs, submittals and numbered site instructions.
- Daily reports, snagging and NCR evidence/verification closure.
- Consultant coordination and responsibility matrix.
- Dependency-aware programme, baselines and progress reporting.
- Versioned handover and as-built package.
- Roles, project assignments, cross-feature immutable audit, tested backup/restore and optional offline synchronisation.
- External integrations behind explicit adapters.

## Explicit exclusions

- Bills, invoices, receipts, retention, payment collection and accounting.
- Consultancy fee/GST accounting.
- Automatic statutory compliance decisions, approval matrices and jurisdiction-rule engines.
- CAD/BIM authoring.
- Archi Launcher, Pomodoro and calculator widgets.
- Mandatory WordPress, Supabase or cloud operation.

## Documentation authority

The maintained `docs` directory describes the live product and roadmap. Files under `Updated Briefs` are source proposals only; their embedded instructions do not override the implemented architecture or the product boundary. `UPDATED_BRIEFS_SCOPE_RECONCILIATION.md` records which proposals were accepted, excluded or deferred.

| Document | Purpose |
|---|---|
| `LATEST_IMPLEMENTATION_AND_BRAND_REPORT.md` | Current executive implementation and branding status |
| `IMPLEMENTATION_COMPLETION_AUDIT.md` | Evidence, acceptance state and remaining release checks |
| `IMPLEMENTATION_ROADMAP.md` | Completed and future delivery work |
| `APP_STRUCTURE_AND_HIERARCHY.md` | User-facing and target code hierarchy |
| `ARCHITECT_PRACTICE_SCOPE.md` | Approved architectural-practice capability boundary |
| `PURE_MEASUREMENT_BOOK_PRODUCT_AND_ARCHITECTURE.md` | Detailed product/measurement architecture; legacy filename retained for link compatibility |
| `COMPARATIVE_UX_CASE_STUDY.md` | External UX evidence and applied navigation decisions |
| `UI_UX_AUDIT_360DP.md` | Small-screen rules and acceptance gaps |
| `MATERIAL_3_MIGRATION.md` | Material 3 foundation, compatibility policy and remaining UI work |
| `MIGRATION_BACKUP_AND_ROLLBACK.md` | Safe upgrade and recovery procedure |
| `SUPPORT_AND_RELEASE_OPERATIONS.md` | Diagnostics, quality gate and incident handling |
| `UPDATED_BRIEFS_SCOPE_RECONCILIATION.md` | Source-proposal disposition |
| `UPDATED_ERP_V3_SCOPE_RECONCILIATION.md` | ERP v3 comparison, accepted implementation and deferred security/platform work |
| `VISHWAKARMA_ARCHITECT_OS_CONSOLIDATION.md` | Ownership and migration boundary with older apps |
