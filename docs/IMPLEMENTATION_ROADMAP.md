# ArchiMan Implementation Roadmap

> ArchiMan is a field-first, quantity-only Measurement Book application. Rates, valuation, billing and accounting are outside the product boundary. The approved scope and delivery order are defined in `ARCHITECT_PRACTICE_SCOPE.md`.

Current implementation baseline: Room schema 22, pure Material 3 UI, audited 11 September 2026.

## Phase 0 - Scope lock and safety

Status: Completed.

- Adopt the pure Measurement Book product specification.
- Inventory every rate, amount, billing, and retention dependency.
- Preserve existing user measurement data while commercial fields are retired.
- Establish build and USB-device smoke-test baseline.

Exit criteria: product boundary documented and current app builds.

## Phase 1 - Remove all commercial functionality

Status: Completed.

- Remove every rate-book, rate and amount entry or display path.
- Remove rate from contractor qualification creation and display.
- Remove amount totals from project, register, reports, and work-item screens.
- Remove Bills routes and entry points.
- Remove commercial endpoints from the LAN portal.
- Stop writing rate, amount, and bill references for new measurements.
- Rename reporting copy to measurement-only terminology.

Compatibility rule: older databases are accepted only through explicit, preserving migrations. Schema 22 contains no commercial tables or measurement columns.

Exit criteria: no rate, valuation, billing, invoice, payment, retention or accounting workflow is visible or writable.

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

Status: Completed through schema version 22 locally and installed as an in-place device upgrade. The schema-22 migration removes the obsolete commercial schema while preserving every quantity and measurement-context field; a production backup/restore drill remains open.

- [x] Split measurement sheet headers from linked measurement rows while retaining compatibility snapshots.
- [x] Introduce immutable formula code, formula version, UOM, item, contractor, floor, and date snapshots.
- [x] Remove contractor rate and bill tables, including the schema-14 rate-book structures through preserving migration 21 -> 22.
- [x] Remove rate, amount, bill ID, and default-rate columns through explicit migration 4 -> 5.
- [x] Remove destructive migration fallback.
- [x] Add automated migration preservation verification.
- [x] Split measurement sheet headers from rows through preserving migration 8 -> 9.
- [x] Add migration backup and rollback guidance for managed deployments.

Compatibility note: migrations 3 -> 22 can still read older databases. Migration 21 -> 22 explicitly copies all measurement identifiers, dimensions, deductions, quantities, snapshots, remarks, evidence links and dates before removing the three rate-book tables and three commercial measurement columns.

Exit criteria: quantity-only measurement behavior, preserved legacy data and passing supported migration tests.

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
- [x] Add optional Supabase project configuration using a client-safe publishable key and explicit connection test; no data sync is enabled by configuration alone.
- [x] Add portable company-profile/logo export and confirmation-based import.
- [x] Encrypted Android backup/device-transfer allowlist for the database, attachments, and drafts.
- [x] User-started, named-user HTTPS workspace bound to the phone's current local Wi-Fi address.
- [x] Admin, Editor and Viewer roles with slow salted password hashing, login throttling, session expiry and CSRF protection.
- [x] Controlled task, approval and backlog quick entry with an append-only audit log.
- [ ] Project-specific account assignments and optimistic conflict handling before exposing measurement or document editing in the browser.
- Optional secure multi-device synchronization and remote review remain future scope.
- Conflict resolution and device management.
- [x] Privacy-safe, user-initiated offline support diagnostics export and operations runbook.
- [x] CI build, unit/migration test, product-boundary, merged-manifest, report, and APK artifact gates.
- Managed distribution and signed release promotion.
- [x] Remove unused network SDKs; allow only the platform permissions required by the approved local Wi-Fi workspace.

Exit criteria: multi-user, recoverable, supportable deployment with operational controls.

## Phase 5A - Project onboarding and controls

Status: Core registers implemented through schema 22; template administration and formal sign-off documents remain open.

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
- [x] Retire the former contractor rate-book scope extension and remove its project workspace.
- [x] Remove automatic rate assignment and calculated amount snapshots from all new measurement writes.
- [x] Remove rate and amount display/export, then remove their obsolete schema fields through the preserving 21→22 measurement migration.
- [x] Remove fictional demo clients, contractors, projects and measurement rows from clean-install seeding; retain only reference work-catalogue masters.
- [x] Define the canonical portfolio → project → planning/setup → measurement hierarchy and remove duplicate Record/M-Book destinations from the project Overview.
- [x] Move application destinations out of the all-purpose ViewModel into a dedicated navigation package.
- [x] Keep Archi Launcher, Pomodoro and calculator outside ArchiMan as a separate application boundary.
- [x] Add project schedules, structured meeting minutes and site inspection reports with optional photos in schema 15.
- [x] Retain material selection and quantity-only purchase-order export under Project Planning.
- [ ] Build a previewed, user-confirmed import from `/sdcard/Vishwakarma/os-bundle.json`; do not maintain two live project databases.
- [ ] Add PDF/CSV exports for meeting minutes and inspection reports before retiring standalone Vishwakarma apps.
- [ ] Split the all-purpose ViewModel, repository, entities and DAOs into measurement, catalogue and project feature boundaries.
- [x] Phased migration plan documented.
- [x] Remove commercial values from contractor qualification UI.
- [x] Remove commercial values from the canonical measurement selection and row-entry UI.
- [x] Remove commercial values from legacy entry and register UI.
- [x] Remove commercial navigation and APIs.
- [x] Stop commercial writes from the canonical row editor and remove the obsolete commercial columns in schema 22.
- [x] Centralize quantity formulas and add formula tests.
- [x] Metric/Imperial field-entry toggle with atomic row, deduction, quantity, UOM, import, Undo, and draft conversion.
- [x] Route all active measurement entry points through the canonical editor.
- [x] Delete the dormant legacy project-tab measurement form and its duplicate ViewModel save workflow so only the canonical editor remains in source.
- [x] Preserve the selected work-item ID through canonical editor sessions and reject orphan measurement saves.
- [x] Build and migration-test the schema-18 milestone locally.
- [x] Add schema 19 with preserving practice/client profile and project site-data expansion.
- [x] Migrate the shared design foundation to Material 3 color, typography, shape, text-field, divider, dropdown and auto-mirrored icon APIs.
- [x] Replace the parallel custom design system with direct Material 3 theme roles and components.
- [ ] Add verified dark/adaptive screenshot coverage.
- [x] Install schema 15 on a USB device and verify preserved data, PWD catalogue rows, project-operations tables, and clean startup.
- [x] Install schema 19 as an in-place USB upgrade and confirm the 18→19 migration and clean app-process startup.
- [ ] Complete field-data reconciliation and hands-on verification of expanded profiles, Brief & Scope, site data and navigation.
- [ ] Complete hands-on field interaction smoke testing for entry, photos, duplication, drafts, and review transitions.
- [ ] Expand the curated PWD SR starter catalogue into a complete, edition-controlled official dataset after source-by-source validation.
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

The six distinct September 2026 source briefs remain under `Updated Briefs` as requirements evidence; the exact duplicate was removed on 9 September. Accepted work is represented in this roadmap and the product scope. WordPress deployment instructions and all consultancy billing, tax, invoice and payment features are outside the current app; Supabase remains an optional, user-configured connection only.

- [x] Add schema-18 project consultancy profile and structured scope-register foundation.
- [x] Add a mobile Brief & Scope project workspace for consultancy, phase, design stage, review status and core brief capture.
- [x] Consolidate navigation into four portfolio destinations, four project actions and four primary project sections; group secondary tools under labelled More menus.
- [x] Benchmark Autodesk, Procore, PlanRadar and Fieldwire; combine Clients/Contractors into Directory and promote the shared Work List to portfolio navigation.
- [ ] Add versioned, project-type onboarding templates with conditional and repeatable questions.
- [ ] Generate a reviewed project brief snapshot from onboarding responses.
- [ ] Add reusable, versioned non-commercial consultancy templates and explicit project import.
- [ ] Add client/technical decision records and linked project backlogs.
- [ ] Add design/execution/handover progress and project-specific execution-level status.
- [x] Add local users, Admin/Editor/Viewer roles and immutable audit events for controlled LAN quick entry.
- [ ] Add project-specific assignments and conflict handling before measurement or document editing is exposed over LAN.

## Architecture Consultancy ERP v3 proposal

The ERP proposal is retained under `Updated Briefs` as reference material. Its finance, tax, agreement-fee and automatic-cloud instructions do not override ArchiMan's approved boundary or security gates.

- [x] Remove the obsolete portfolio Export Measurement Sheet screen; keep measurement exports in M-Book.
- [x] Expand company/practice identity with company type, country, PAN, principal and registration details.
- [x] Expand client records with type, contact person, email, correspondence address, preferred communication and notes.
- [x] Add structured project site data under Brief & Scope.
- [ ] Add installation/server identity and local system-health reporting.
- [ ] Design encrypted whole-company package export/import with preview, checksum and safety backup.
- [x] Add local users, roles and immutable audit events for controlled writable LAN access.
- [ ] Add project assignments and cross-feature conflict handling before expanding LAN writes.
- [ ] Add authenticated responsive LAN administration only after concurrency, threat-model and recovery tests.
- [ ] Evaluate optional Supabase backup separately from synchronization; neither is part of the current release.
