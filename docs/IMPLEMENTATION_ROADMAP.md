# ArchiMan Implementation Roadmap

> ArchiMan is an Architectural Consultancy Management App with a field-first Measurement Book. The approved scope and delivery order are defined in `ARCHITECT_PRACTICE_SCOPE.md`. A statutory approval/compliance matrix and jurisdiction profiles are not part of the product.

Current implementation baseline: Room schema 20, branded and audited 4 September 2026.

## Phase 0 - Scope lock and safety

Status: Completed.

- Adopt the pure Measurement Book product specification.
- Inventory every rate, amount, billing, and retention dependency.
- Preserve existing user measurement data while commercial fields are retired.
- Establish build and USB-device smoke-test baseline.

Exit criteria: product boundary documented and current app builds.

## Phase 1 - Remove billing and uncontrolled commercial functionality

Status: Completed.

- Remove ad hoc/default rate and amount entry from measurement screens; later approved contractor rate books remain a controlled exception.
- Remove rate from contractor qualification creation and display.
- Remove amount totals from project, register, reports, and work-item screens.
- Remove Bills routes and entry points.
- Remove billing and rate endpoints from the LAN portal.
- Stop writing rate, amount, and bill references for new measurements.
- Rename reporting copy to measurement-only terminology.

Compatibility rule: legacy database columns may remain temporarily but must be ignored and written as zero/null until the migration phase.

Exit criteria: no billing, invoice, payment, retention or accounting workflow is visible; rates originate only from an explicitly assigned contractor rate book.

## Phase 2 - Canonical measurement workflow

Status: Completed and verified on device.

- [x] Consolidate Quick Entry, Work Item Measure, project-tab entry, and dedicated entry into one editor.
- [x] Implement contractor -> work type -> work item -> floor selection.
- [x] Add session-scoped persistent row drafts with automatic recovery after process recreation.
- [x] Implement draft auto-save, formula-aware validation, system-keyboard traversal, one-step undo, and row status.
- [x] Copy optional row photos into private managed attachment storage.

Exit criteria: every measurement route opens the same editor and produces identical records.

## Phase 3 - Work catalog normalization and duplicate merge

Status: Completed; unresolved near-duplicate candidates remain visible for explicit human review.

- [x] Persist contractor type, work type, work item formula, and contractor qualification hierarchy.
- [x] Repoint references and merge exact case/spacing duplicates transactionally.
- [x] Add uniqueness indexes preventing exact duplicate masters and qualifications.
- [x] Render Work List as expandable Work Type -> Work Item -> Formula groups.
- [x] Add stable work-item codes and alias storage.
- [x] Build a conservative near-duplicate candidate report with side-by-side review details.
- [x] Add transactional merge preview and explicit canonical-item selection.
- [x] Preserve measurement rows and snapshot descriptions while repointing catalog references.
- [x] Replace hard-coded contractor work lists with a versioned bundled JSON catalog and validated user import.
- [x] Archive work items instead of deleting referenced masters.
- [x] Add preserving foreign-key protection for measurement project, floor, contractor, work-item, and optional structural references.

Exit criteria: one canonical work catalog with no unresolved duplicate candidates.

## Phase 4 - Measurement sheet domain and safe schema migration

Status: Completed through schema version 20 locally. WAL-consistent device preservation was verified through schema 15; schema-20 device acceptance remains open.

- [x] Split measurement sheet headers from linked measurement rows while retaining compatibility snapshots.
- [x] Introduce immutable formula code, formula version, UOM, item, contractor, floor, and date snapshots.
- [x] Remove contractor rate and bill tables.
- [x] Remove rate, amount, bill ID, and default-rate columns through explicit migration 4 -> 5.
- [x] Remove destructive migration fallback.
- [x] Add automated migration preservation verification.
- [x] Split measurement sheet headers from rows through preserving migration 8 -> 9.
- [x] Add migration backup and rollback guidance for managed deployments.

Exit criteria: measurement schema without billing tables, controlled rate-book snapshots, and passing supported migration tests.

## Phase 5 - Review, approval, and audit

Status: Implemented through schema version 20. Trigger and workflow tests pass locally; hands-on review-transition testing remains part of field acceptance.

- [x] Draft, submitted, checked, approved, and returned states.
- [x] Review comments and correction revision increments on resubmission.
- [x] Archive measurement sheets instead of destructively deleting rows.
- [x] Immutable audit events enforced by database triggers.
- [x] Measurement-row update and deletion lock after approval.
- [x] M-Book workflow controls and audit-history viewer.

Exit criteria: a complete traceable field-to-approved-M-Book process.

## Phase 6 - Enterprise platform

Status: Started; offline security and encrypted platform backup controls are implemented. Multi-user platform capabilities remain future scope.

- Authentication, organizations, roles, and permissions.
- [x] Encrypted Android backup/device-transfer allowlist for the database, attachments, and drafts.
- [x] User-started, PIN-authenticated, read-only portal bound to the phone's current local Wi-Fi address.
- Optional secure multi-device synchronization and remote review remain future scope.
- Conflict resolution and device management.
- [x] Privacy-safe, user-initiated offline support diagnostics export and operations runbook.
- [x] CI build, unit/migration test, product-boundary, merged-manifest, report, and APK artifact gates.
- Managed distribution and signed release promotion.
- [x] Remove unused network SDKs; allow only the platform permissions required by the approved local Wi-Fi portal.

Exit criteria: multi-user, recoverable, supportable deployment with operational controls.

## Phase 5A - Project onboarding and controls

Status: Core registers implemented in schema 20; template administration and formal sign-off documents remain open.

- [x] Project-type onboarding questionnaire with stable question codes and template version snapshots.
- [x] Persisted answers, clarifications and completion progress.
- [x] Client and technical approval register with lifecycle timestamps.
- [x] Categorised project backlog with phase, priority and close/reopen workflow.
- [ ] Reusable practice-managed questionnaire/template editor.
- [ ] Approval attachments, named approver identity and signed approval export.
- [ ] Programme dependency links and progress baseline comparison.

## Current implementation checklist

- [x] Product boundary documented.
- [x] Target UX and architecture documented.
- [x] Complete the 360 dp mobile UI audit and resolve header, navigation, duplicate-action, unit-label, and above-the-fold workflow defects.
- [x] Add project Tasks and a persisted Specification / Selection List.
- [x] Export a project selection list as a quantity-only purchase order without rates or billing fields.
- [x] Add the first curated Karnataka PWD SR Buildings 2023-24 work/specification catalogue slice with source provenance; catalogue masters contain no rates.
- [x] Approve the rate-book scope extension: contractor-owned versioned books, PWD item import, project assignment, and immutable measurement rate snapshots.
- [x] Deliver the first project Rates workspace, including contractor selection, multiple named/versioned books, PWD item import, rate entry, and project assignment.
- [x] Show rate and calculated amount snapshots in the M-Book and CSV export without introducing bills or payment workflows.
- [x] Define the canonical portfolio → project → planning/setup → measurement hierarchy and remove duplicate Record/M-Book destinations from the project Overview.
- [x] Move application destinations out of the all-purpose ViewModel into a dedicated navigation package.
- [x] Keep Archi Launcher, Pomodoro and calculator outside ArchiMan as a separate application boundary.
- [x] Add project schedules, structured meeting minutes and site inspection reports with optional photos in schema 15.
- [x] Retain material selection and quantity-only purchase-order export under Project Planning.
- [ ] Build a previewed, user-confirmed import from `/sdcard/Vishwakarma/os-bundle.json`; do not maintain two live project databases.
- [ ] Add PDF/CSV exports for meeting minutes and inspection reports before retiring standalone Vishwakarma apps.
- [ ] Split the all-purpose ViewModel, repository, entities and DAOs into measurement, catalogue, project and rate-book feature boundaries.
- [x] Phased migration plan documented.
- [x] Remove commercial values from contractor qualification UI.
- [x] Remove commercial values from the canonical measurement selection and row-entry UI.
- [x] Remove commercial values from legacy entry and register UI.
- [x] Remove commercial navigation and APIs.
- [x] Stop commercial writes from the canonical row editor (legacy columns receive zero during compatibility period).
- [x] Centralize quantity formulas and add formula tests.
- [x] Metric/Imperial field-entry toggle with atomic row, deduction, quantity, UOM, import, Undo, and draft conversion.
- [x] Route all active measurement entry points through the canonical editor.
- [x] Preserve the selected work-item ID through canonical editor sessions and reject orphan measurement saves.
- [x] Build and migration-test the schema-18 milestone locally.
- [x] Add schema 19 with preserving practice/client profile and project site-data expansion.
- [x] Migrate the shared design foundation to Material 3 color, typography, shape, text-field, divider, dropdown and auto-mirrored icon APIs.
- [ ] Replace remaining legacy source-level color aliases with semantic Material 3 roles and add verified dark/adaptive layouts.
- [x] Install schema 15 on a USB device and verify preserved data, PWD catalogue rows, rate-book and project-operations tables, and clean startup.
- [x] Install schema 19 as an in-place USB upgrade and confirm the 18→19 migration and clean app-process startup.
- [ ] Complete field-data reconciliation and hands-on verification of expanded profiles, Brief & Scope, site data and navigation.
- [ ] Complete hands-on field interaction smoke testing for entry, photos, duplication, drafts, and review transitions.
- [ ] Expand the curated PWD SR starter catalogue into a complete, edition-controlled official dataset after source-by-source validation.
- [ ] Add rate-book clone/revision, publish/retire lifecycle, effective-date controls, and an assignment history screen.
- [ ] Add an impact preview before changing a project's applicable rate book; historical measurement snapshots must remain unchanged.
- [x] Add schema-16 drawing register, immutable revision, transmittal and markup-metadata foundation.
- [ ] Complete controlled drawing-file intake, managed storage, revision issue workflow and transmittal export.
- [ ] Integrate a native DWG viewer behind a replaceable engine boundary; add non-destructive annotation and calibrated measurement overlays.
- [ ] Add revision-linked RFIs, submittals and numbered site instructions.
- [ ] Add daily reports, snagging and NCR evidence/verification closure.
- [ ] Add consultant coordination and a responsibility matrix.
- [ ] Replace the simple schedule with programme dependencies, baselines and progress updates.
- [ ] Add a versioned handover and as-built document package.
- [ ] Add roles, offline synchronisation, immutable cross-feature audit events and tested backups.
- [ ] Add external integrations through explicit adapters.
- [x] Exclude approval/compliance matrices and jurisdiction profiles from ArchiMan.

## Updated consultancy briefs programme

The seven September 2026 source briefs were reconciled in `UPDATED_BRIEFS_SCOPE_RECONCILIATION.md`; one source file is an exact duplicate. WordPress/Supabase deployment instructions and all consultancy billing, tax, invoice and payment features are not part of the current app.

- [x] Add schema-18 project consultancy profile and structured scope-register foundation.
- [x] Add a mobile Brief & Scope project workspace for consultancy, phase, design stage, review status and core brief capture.
- [x] Consolidate navigation into four portfolio destinations, four project actions and four primary project sections; group secondary tools under labelled More menus.
- [x] Benchmark Autodesk, Procore, PlanRadar and Fieldwire; combine Clients/Contractors into Directory and promote the shared Work List to portfolio navigation.
- [ ] Add versioned, project-type onboarding templates with conditional and repeatable questions.
- [ ] Generate a reviewed project brief snapshot from onboarding responses.
- [ ] Add reusable, versioned non-commercial consultancy templates and explicit project import.
- [ ] Add client/technical decision records and linked project backlogs.
- [ ] Add design/execution/handover progress and project-specific execution-level status.
- [ ] Add roles, project assignments and cross-feature immutable audit events before any multi-user editing portal.

## Architecture Consultancy ERP v3 proposal

The downloaded ERP v3 brief is reconciled in `UPDATED_ERP_V3_SCOPE_RECONCILIATION.md`. Its finance, tax, agreement-fee, automatic cloud and immediately writable multi-user LAN instructions do not override ArchiMan's approved boundary or security gates.

- [x] Remove the obsolete portfolio Export Measurement Sheet screen; keep measurement exports in M-Book.
- [x] Expand company/practice identity with company type, country, PAN, principal and registration details.
- [x] Expand client records with type, contact person, email, correspondence address, preferred communication and notes.
- [x] Add structured project site data under Brief & Scope.
- [ ] Add installation/server identity and local system-health reporting.
- [ ] Design encrypted whole-company package export/import with preview, checksum and safety backup.
- [ ] Add local users, roles, project assignments and immutable cross-feature audit before writable LAN access.
- [ ] Add authenticated responsive LAN administration only after concurrency, threat-model and recovery tests.
- [ ] Evaluate optional Supabase backup separately from synchronization; neither is part of the current release.
