# AORMS Portal Case Study and ArchiMan Response

- Review date: 13 September 2026
- Reference system: local AORMS portal source under `D:\Work Development\Repos\aorms`
- ArchiMan boundary: offline-first architectural consultancy management and quantity-only Measurement Books

## Evidence reviewed

- `web/components/aorms/AppShell.tsx` for practice-level information architecture.
- `web/components/aorms/ProjectTabs.tsx` for project-workspace hierarchy.
- `web/app/(app)/layout.tsx` for authenticated shell behaviour.
- `docs/esti/AORMS-OFFICE-SYSTEM.md` and `docs/esti/ARCHITECTURE.md` for module ownership and workflow boundaries.

This was a source-level case study, not an instruction import. ArchiMan adopts only patterns that fit its documented product boundary.

## Structural findings

| AORMS pattern | Why it works | ArchiMan response |
|---|---|---|
| High-frequency practice registers at the top level | Users can manage cross-project work without entering a project first | Retain Projects, Directory and Work List as portfolio destinations; expose browser dashboard and practice administration separately |
| A project is a durable workspace with its own tabs | Context, records and ownership remain attached to the project | Retain Project, Work, Record and M-Book as primary project sections; place secondary registers in labelled More destinations |
| Decisions are formal records, not text buried in minutes | A decision has an owner, impact, due state and final outcome | Added a project decision register with reference, context, required decision, impact, requested party, owner and decided state |
| Delivery registers have explicit status and responsibility | Open work is visible and closure can be governed | Added snag/NCR records with assignee, severity, corrective action and a controlled verification lifecycle |
| Office-wide navigation and project navigation are different layers | The shell remains predictable as the product grows | Kept the responsive Carbon-style web shell and project selector; mobile remains field-first and uses Material 3 project sections |
| Audit events are append-only | Operational history cannot silently be rewritten | Added immutable snag/NCR status events enforced by SQLite triggers |

## Adopted in schema 24

### Formal decisions

The Decisions project destination records the question requiring a decision separately from the final decision. This avoids mixing discussion, meeting minutes and approvals. The phone supports creation and final-decision capture; the LAN portal supports creation from Planning.

### Daily site reports

The Site Reports workspace now begins with a daily report register. Each project/date entry captures work completed, weather, manpower/trades, materials received, delays or constraints, safety observations, next-day plan, preparer and an optional photo on mobile. The LAN portal supports full structured creation.

### Snag and NCR closure

Snags and NCRs share one searchable operational register but retain their record type. Their allowed lifecycle is:

`OPEN -> IN_PROGRESS -> READY_FOR_VERIFICATION -> CLOSED`

An item may be returned from correction or verification, but closure requires a verification note. A closed item is terminal. Every transition is appended to an immutable event table; mobile exposes the complete transition path and the LAN portal exposes structured creation.

## Adapted, not copied

- AORMS's broad departmental menu is reduced to ArchiMan's smaller practice/project hierarchy.
- Carbon patterns are used only in the desktop web portal; Android remains pure Material 3.
- AORMS programme, staff and administration ideas enter the roadmap only where they support project delivery.
- Existing ArchiMan coordination records remain the source for RFI, submittal and site-instruction workflows instead of duplicating AORMS modules.

## Explicitly excluded

- All AI assistants, generation, inference, embeddings and AI-dependent search.
- Rates, estimates, BOQ pricing, RA bills, invoices, taxation, payments, payroll and accounting.
- AORMS-specific business logic that would duplicate ArchiMan's project, M-Book, drawing or coordination records.
- Automatic cloud synchronization or silent transfer of company data.

## Ordered remaining gaps

1. Programme dependencies, baselines and progress updates linked to project tasks.
2. Project-specific user assignments and responsibility-aware authorization.
3. Field-by-field browser editing, including snag/NCR transition and verification actions.
4. Lessons-learned and close-out registers feeding versioned practice templates.
5. Controlled drawing/document intake, transmittal export and as-built handover packaging.
6. Optimistic concurrency and independent security acceptance for simultaneous LAN users.

These are roadmap items, not claims of current implementation.
