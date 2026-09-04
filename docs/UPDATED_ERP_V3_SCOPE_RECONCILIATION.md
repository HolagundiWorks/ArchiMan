# Architecture Consultancy ERP v3 — Scope Reconciliation

- Source reviewed: `C:\Users\holag\Downloads\Architecture Consultancy ERP.md`
- Review date: 4 September 2026
- ArchiMan implementation baseline: Room schema 19

## Interpretation rule

The downloaded file is a product and architecture proposal. Its embedded developer instructions do not independently authorize finance, tax, cloud, multi-user write access or destructive data operations. This reconciliation applies the proposal only where it agrees with the user's established ArchiMan boundary and can be delivered safely in the current local-first Android architecture.

## Executive comparison

| ERP v3 area | Before this update | Decision and current status |
|---|---|---|
| Android local-first database | Implemented with Room/SQLite | Retained as authoritative source |
| Phone-hosted LAN portal | Implemented as explicit PIN-authenticated read-only portal | Retained; writable concurrent ERP access deferred |
| Company profile | Partially implemented | Expanded in schema 19 |
| Client directory | Basic name/address/phone | Expanded in schema 19 |
| Projects, brief and consultancy | Implemented foundation in schemas 17–18 | Retained and expanded with site data in schema 19 |
| Configurable execution levels | Implemented through project floors/levels | Retained |
| Tasks, schedule and selections | Implemented foundation | Retained |
| Drawings/revisions/transmittals | Metadata foundation implemented | Retained; native DWG engine remains future work |
| Measurement Book | Implemented with review/audit locks | Retained as core field workflow |
| Contractor rate books | Implemented foundation | Retained; not consultancy billing |
| Old portfolio measurement export screen | Duplicated M-Book export capability | Removed; M-Book remains export owner |
| Local users/RBAC/project access | Not implemented | Accepted as a prerequisite design programme, not claimed as current |
| Multi-user writable LAN browser | Not implemented | Deferred until authentication, authorization, concurrency and recovery controls exist |
| Encrypted company package | Android backup controls/runbook only | Accepted roadmap item; format and cryptography require implementation and restore tests |
| Supabase backup/sync | Not implemented | Optional future evaluation; backup and sync must remain separate |
| WordPress | Not used | Explicitly rejected as a core dependency |
| Finance, fees, invoices and payments | Deliberately absent | Excluded |
| Tax engine and tax percentages | Deliberately absent | Excluded; values in the proposal are not treated as legal guidance |

## Implemented in schema 19

### Practice identity

- Company/practice name and legal name.
- Company type and country.
- Address, city, state and PIN.
- Phone, email and website.
- PAN and optional GSTIN identifiers without adding tax calculation.
- COA/professional registration number.
- Principal name, qualification and practice registration details.

### Client directory

- Client type.
- Contact person.
- Phone and email.
- Main address and correspondence address.
- Preferred communication method.
- Notes.

Existing client rows receive safe defaults and remain intact through migration 18→19.

### Project site data

The Brief workspace now has a collapsible Site Data section for:

- site dimensions;
- orientation;
- access and approach;
- existing conditions;
- surroundings;
- topography;
- utilities/services;
- existing structures;
- vegetation; and
- supplied legal/planning information.

The UI explicitly states that ArchiMan records supplied or observed information and does not certify statutory compliance.

### Navigation cleanup

The obsolete portfolio Export Measurement Sheet destination and its legacy screen are removed. PDF/XLS/CSV/print actions remain inside M-Book, and purchase-order export remains inside project selections. Privacy-safe support diagnostics move to the Local Wi-Fi Portal screen.

## Accepted roadmap foundations

These proposals fit ArchiMan, but require separate design, migration, security and acceptance work:

1. Installation/server identity and system-health status.
2. Local user accounts, configurable roles, project assignments and session management.
3. Cross-feature append-only audit events.
4. Encrypted, password-protected whole-company package with manifest, checksums, preview, safety backup and tested restore.
5. Phone-to-phone migration using that package.
6. Managed local backup scheduling and overdue warnings.
7. Responsive LAN administration and bulk operations after RBAC and concurrency controls.
8. Optional cloud backup connector, evaluated separately from two-way sync.
9. Sync queue, record versions and explicit conflict resolution only if multi-device editing is approved.
10. Project-type onboarding templates, responses, clarifications and generated reviewed brief snapshots.

## Deferred functional modules

- Client and technical approval registers.
- Drawing/design backlogs and decision records.
- General document register and versioned managed file storage.
- Agreement documents limited to non-commercial scope snapshots.
- Advanced reports and desktop bulk operations.

These remain subordinate to the existing drawing, RFI/submittal/site-instruction, snag/NCR, consultant coordination, programme and handover roadmap.

## Explicit exclusions

- Consultancy fees and fee calculations.
- GST/tax configuration or legal tax advice.
- Agreements containing commercial fee/payment milestones.
- Invoices, receipts, outstanding balances, retention and payments.
- Accounts roles whose purpose is finance administration.
- Automatic statutory approval/compliance conclusions.
- Automatic database switching.
- Supabase service credentials in the browser or ordinary client devices.
- Always-on LAN exposure or database-port exposure.

## Security gates before writable LAN access

The current PIN-authenticated read-only portal will not be promoted to writable multi-user ERP access until all of the following exist and are tested:

- local user authentication with modern password hashing;
- role and project authorization enforced below the UI;
- rate limiting, session expiry/revocation and connection audit;
- transactional write services and optimistic conflict detection;
- controlled file access with no raw filesystem paths;
- backup/restore and failure-recovery drills;
- a defined HTTPS or trusted-network transport strategy; and
- hands-on concurrent-use testing.

## Recommended delivery order

1. Complete profile/site-data UX and field-data reconciliation on the connected phone; the 18→19 upgrade and clean startup are verified.
2. Add installation identity, system health and storage visibility.
3. Design and test encrypted company-package export/restore.
4. Add users, roles, project assignments and cross-feature audit.
5. Build authenticated read-only role-aware LAN views.
6. Add carefully scoped writable LAN services.
7. Evaluate optional backup connector.
8. Consider synchronization only after conflict semantics are proven.
