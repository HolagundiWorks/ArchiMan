# Updated briefs: scope reconciliation

Date reviewed: 4 September 2026

## How the source files were treated

The files in `Updated Briefs` are product proposals and reference material. Their implementation directives do not override the current ArchiMan architecture, the existing product boundary, or prior user decisions.

`Architecture Consultancy App (3).md` and `Architecture Consultancy App (4).md` are byte-for-byte duplicates. They are one proposal, not two requirements.

## Accepted additions

1. Project and consultancy setup
   - Project types: Residential, Commercial, Healthcare, Hospitality, Education and Other.
   - Multi-select consultancy types: Architectural, Landscaping, Part consultancy and Miscellaneous.
   - Consistent project phases: Design, Execution and Handover.
   - Design stages from Concept through Issued for Construction.
   - Project-specific execution levels; no hard-coded floor count.

2. Client brief and requirements
   - A project brief with objectives, space/functional requirements, design preferences, known constraints and clarifications.
   - Review states: Not started, In progress, Under review, Clarification required and Approved.
   - Later expansion to project-type onboarding templates, repeatable spaces and conditional questions.
   - Regulatory/site information is recorded as supplied or known information; ArchiMan does not certify compliance automatically.

3. Consultancy scope control
   - Structured scope, deliverables, exclusions and responsibility items.
   - Future versioned reusable templates.
   - A copied project snapshot so later template edits cannot silently alter an agreed project scope.

4. Project execution control
   - Client decisions and technical decisions as manually recorded workflow evidence.
   - Backlogs linked to drawings, decisions and project phases.
   - Project dashboard signals for phase, stage, open actions and deliverables.

5. Enterprise foundations
   - Configurable roles and project access, immutable audit records, backups and explicit sync conflict handling.
   - These remain local-first and must be introduced behind repository/service boundaries.

## Excluded or deferred proposals

- Consultancy fees, GST/tax engines, invoices, receipts, payments, outstanding amounts and accounts are excluded. ArchiMan has contractor rate books for measurement valuation, but it is not a billing or accounting product.
- Automated statutory approval/compliance rules, jurisdiction profiles and claims of legal confirmation remain excluded.
- WordPress as the application shell and Supabase as the mandatory database are rejected for the current product. ArchiMan remains an Android, Room/SQLite, offline-first application.
- A PostgreSQL office-server migration, cloud synchronization, client cloud portal and Supabase storage are separate architecture decisions requiring an explicit future approval and migration plan.
- Agreement generation and commercial variations are deferred. Non-commercial scope snapshots can later support document generation without adding finance.
- A hard maximum of five users is not a domain rule and will not be embedded in the data model.

## Consolidated delivery order

### A. Brief and scope foundation — started

- Project-level consultancy profile.
- Phase, design stage and brief review status.
- Brief narrative fields.
- Scope/deliverable/exclusion/responsibility register.

### B. Dynamic onboarding templates

- Versioned project-type templates, sections and questions.
- Text, number, date, yes/no, single-select, multi-select and repeatable-group responses.
- Conditional visibility, validation, clarifications and brief generation.
- Seeded Residential, Commercial, Healthcare, Hospitality and Education templates.

### C. Consultancy template library

- Versioned non-commercial templates.
- Scope, deliverables, exclusions, responsibilities and phase definitions.
- Preview and explicit copy into a project snapshot.

### D. Design/execution/handover workflow

- Client decisions, technical decisions and linked evidence.
- Backlogs, priorities, due dates and relationships to drawings/tasks.
- Dynamic execution levels and phase/stage progress.
- Handover checklist and as-built package.

### E. Enterprise access and recovery

- Users, configurable roles, permissions and project assignments.
- Cross-feature immutable audit events.
- Tested backup/restore.
- Only then evaluate office LAN editing and optional synchronization.

## Current implementation note

Schema 18 introduces the Brief & Scope foundation without changing measurement, drawing, contractor-rate-book or local-portal behavior. Existing project data is preserved by an explicit migration.
