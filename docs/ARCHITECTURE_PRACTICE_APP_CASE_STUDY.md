# Architecture Practice Application Case Study

> Reviewed 11 September 2026. Sources are current vendor documentation and product pages. Commercial capabilities were evaluated but are outside ArchiMan's quantity-only product boundary.

## Products reviewed

| Product | Strong pattern | Relevance to ArchiMan |
|---|---|---|
| [Monograph Project Planner](https://monograph.com/feature/project-planner) | Project phases, staff assignment and a single project plan | Keep phase and responsibility ownership visible; exclude budgets, fees and time billing. |
| [Deltek Vantagepoint](https://www.deltek.com/products/erp/vantagepoint/) | Project dashboards, resource planning, skills and workload visibility | Future practice-level resource planning can use people, roles, availability and phases without financial data. |
| [BQE CORE Project Management](https://www.bqe.com/features/project-management) | Schedules, milestones, tasks, deliverables, CRM and resource planning | ArchiMan already has projects, contacts, tasks and schedules; it needs stronger deliverable ownership and reminders. |
| [Newforma RFI workflow](https://projectcenter.help.newforma.com/overviews/rfis_overview/) | Log, forward, answer, close, discipline, due-date attention and linked correspondence | Use explicit status transitions, discipline, assignee or ball-in-court, due date and immutable history. |
| [Autodesk Build RFI workflow](https://help.autodesk.com/cloudhelp/ENU/Build-Rfis/files/Work_RFIs.html) | Draft, submit, review, answer and close with simple or coordinated review paths | Use a small offline state machine now; retain a path for future coordinator/reviewer roles. |
| [Autodesk RFI reports](https://help.autodesk.com/cloudhelp/ENU/Docs-Reports/files/report-types/RFI_Reports.html) | Summary/detail reports with status, due date, priority, discipline, location and activity log | Persist these core fields even before PDF/XLSX coordination exports are added. |
| [ArchiSnapper field reports and punch lists](https://docs.archisnapper.com/reference/sending-a-field-a-report-or-punch-list) | Mobile-first capture, assignment-specific distribution and report output | Existing inspections need later assignee-specific issue lists and distribution-ready exports. |

## Gap assessment

| Capability | Before schema 23 | Decision |
|---|---|---|
| Project identity, brief and site profile | Implemented | Retain. |
| Tasks, schedule, selections and meetings | Implemented foundation | Retain; add dependencies and reminders later. |
| Measurement Book and controlled review | Implemented | Retain as the core field workflow. |
| Drawing register, revisions and transmittals | Implemented foundation | Retain; licensed DWG rendering remains separate. |
| Consultant responsibility matrix | Missing | Implement in schema 23 with discipline, phase, responsibility and RACI role. |
| RFI register | Missing | Implement in schema 23 with reference, question, discipline, location, priority, due date, ball-in-court and controlled status. |
| Submittal review | Missing | Implement in schema 23 with received, review, approval and revise/resubmit states. |
| Site instructions | Missing | Implement in schema 23 with draft, issue, acknowledgement, compliance and closure states. |
| Coordination audit trail | Missing | Append an immutable event for creation, every status transition and archive action. |
| Snag/NCR verified closure | Missing | Next field-control milestone; requires before/after evidence and independent verification. |
| Daily reports | Missing | Next field-control milestone; should reuse project contacts, locations and attachments. |
| Programme dependencies and baselines | Missing | Later planning milestone; do not overload the simple schedule list. |
| Handover package manifest | Missing | Later controlled-document milestone. |
| Practice staffing and workload | Missing | Later firm-management milestone, intentionally without rates, fees or payroll. |

## Implemented response

Schema 23 adds a Coordination destination under Project tools. It contains four compact registers:

1. RFIs: `DRAFT → OPEN → ANSWERED → CLOSED`, with an allowed reopen to `OPEN`.
2. Submittals: `RECEIVED → UNDER REVIEW → APPROVED / APPROVED AS NOTED / REVISE & RESUBMIT`.
3. Site instructions: `DRAFT → ISSUED → ACKNOWLEDGED → COMPLIED → CLOSED`.
4. Responsibility: consultant, organisation, discipline, phase, deliverable/responsibility, contact details and RACI role.

Records are archived instead of deleted. Every creation, transition and archive produces an immutable coordination event. Reference numbers are unique within project and record type. This provides a reliable offline foundation without pretending to deliver email distribution, cloud collaboration or financial ERP functions.

## UX principles adopted

- One Coordination parent instead of separate top-level destinations for every register.
- Type tabs preserve context and reduce menu depth on 360 dp phones.
- The list shows reference, subject, current state, discipline/location, ball-in-court and due date before detail text.
- Only valid next statuses are offered.
- RACI is captured as structured responsibility, not buried in project notes.
- Destructive deletion is replaced with archive, while transition history remains immutable.

## Next recommended implementation order

1. Managed coordination attachments with controlled retention and export inclusion.
2. PDF/CSV summary and individual-record exports for RFIs, submittals and instructions.
3. Daily reports plus snag/NCR verified closure using managed before/after photos.
4. Dependency-aware programme with immutable baseline snapshots.
5. Versioned handover/as-built manifest.
6. Non-commercial practice resource planner for staff roles, skills, availability and phase assignments.

The coordination detail view, immutable event timeline and exact drawing-revision linking are now implemented in the schema-23 application layer.
