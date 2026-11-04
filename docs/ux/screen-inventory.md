# ArchiMan screen inventory

Audit date: 4 November 2026. This lists screens that exist, screens the 2.0 specification asks for, and whether anyone can open them.

Status values:

| Status | Meaning |
| --- | --- |
| Reachable | Linked from the live navigation |
| Unlinked | Composable or view exists and is not in the live navigation |
| Missing | Named by the 2.0 specification or by older docs, and not built |
| Stored | Room or the React model can hold the record; the screen does not expose it |

React has no URL routes. Android routes are only `portfolio` and `project/{projectId}`. Everything else is local tab state.

## Practice workspace

The 2.0 specification separates practice navigation (Home, Projects, My Work, Site, Search, Practice) from project sections.

| 2.0 screen | React today | Android today |
| --- | --- | --- |
| Home / dashboard | Missing. Portfolio opens on the project list | Missing. Portfolio opens on the project list |
| Notifications | Missing | Missing |
| Project list | Reachable. `PortfolioScreen` Projects tab | Reachable. `ProjectList` |
| Create project | Reachable. Modal. Writes Ground Floor | Reachable. Dialog. Writes Ground Floor |
| Clients | Reachable. Contacts → Clients | Reachable. Clients tab. Name, phone, email |
| Contractors | Reachable. Contacts → Contractors, with qualified items | Stored. Seeded and used as a picker inside Measure. No directory screen |
| Work library | Reachable. Library tab | Stored. Item master is seeded. No library screen |
| Practice profile | Reachable. Editable form | Reachable. Read-only letterhead |
| Backup / restore | Reachable. JSON download and immediate replace. Not encrypted | Missing in code. Described in `docs/MIGRATION_BACKUP_AND_ROLLBACK.md` |
| Diagnostics | Copy on the Practice page | Missing |
| Sign-in / roles | Types only (`LocalUserEntity`) | `SignInPane` is unlinked. Users are seeded |
| Search across projects | Missing | Missing |
| My Work inbox | Missing as a practice destination. Tasks are inside a project | Missing as a practice destination. Tasks are inside a project |
| Site hub across projects | Missing. Siteman is inside one project | Missing |

## Project workspace

| 2.0 screen | React today | Android today |
| --- | --- | --- |
| Overview (health, milestones, actions) | Reachable as the Project tab: profile, counts, levels. Not a health dashboard | Levels page shows profile, floors, and rooms. No milestone or issue counts on that page |
| Project details editor | Partial, inside overview | Partial. Create sets identity. Areas and dates are not an edit form |
| Levels and rooms | Reachable from overview | Reachable. Levels page |
| Project team | Contractors can be added for the project | Stored. No team screen. Contractors appear in the measure picker |
| Brief / scope | Inside Planning, not its own More tab | Reachable. Brief page |
| Tasks | Planning tab. Toggle open/completed | Reachable. Kanban |
| Decisions | Planning tab | Stored. No screen |
| Approvals inbox | Missing as a review queue | `ApprovalsPane` is unlinked |
| Letters on letterhead | Missing | Reachable. Letters page |
| Fee proposal | Missing | Reachable. Proposal page |
| Contract | Missing | Reachable. Contract page |
| Fee invoice | Missing | Reachable. Invoice page, plus a practice-wide invoice list |
| Running bill | Missing. M-Book is quantity-only | Reachable. Running bill page. This is outside the 2.0 M-Book rule |

## Coordination, quality, drawings, site

| 2.0 screen | React today | Android today |
| --- | --- | --- |
| RFI list / detail / form | One Coordination list with type `RFI` and status buttons. Not a separate detail route | Stored. Workflow object unused by UI |
| Submittals | Same Coordination view, type `SUBMITTAL` | Stored |
| Site instructions | Same Coordination view, type `SITE_INSTRUCTION` | Stored |
| Consultant RACI | Coordination → Consultants | Stored |
| Issue list (snag / NCR) | Site Reports → Snags | Stored |
| Issue detail | Expanded card in that list, not its own route | Missing |
| Capture with discovery evidence | Form fields, including a URI. No camera | Missing |
| Rectification evidence | `afterEvidenceUri` on the model, edited as text/URI | Column exists. No screen |
| Architect verification | Reachable. Close stays disabled until the note is filled. Workflow rejects an empty note | Workflow object only |
| Drawing register | Reachable | Stored |
| Drawing detail | Card on the register | Missing |
| Revision | Form on the register. Genealogy is the revision list | Stored |
| HFC | `packageTag = HFC` toggle. Not a file upload | Column exists. No screen |
| Transmittal | Drawings → Transmittals | Repository insert exists. No reachable screen |
| As-built | Flag on the revision | Column exists. No screen |
| Site hub | Siteman, inside More | Missing |
| Daily log | Siteman → Logs | Stored. `MeetingsPane` / log code is unlinked |
| Quantity abstract | Siteman → Abstract, from measurement rows | Missing. CSV exists per sheet on M-Book |
| Purchase order | Siteman → Orders. Quantity and specification fields | `StoresPane` is unlinked. `material_orders` is stored |
| Site inspection | Site Reports → Inspections | Reachable as Visits. Text only |
| Meeting minutes | Site Reports → Meetings | Stored. `MeetingsPane` is unlinked |
| Archi Calc | Siteman and Record Measurement | Measure page |
| Quantity entry | Record tab. Live formula result | Measure page. Live formula result |
| M-Book list | M-Book tab | M-Book page |
| Sheet detail | Expanded sheet | Expanded sheet |
| Sheet review | Status buttons plus return comment | Status buttons plus return comment. Audit row is written |

## Android composables that compile and are not linked

These are not 2.0 screens. They are leftovers from the suite pass. Leaving them compiled is not the same as shipping them.

| Composable | File | Would have covered |
| --- | --- | --- |
| `SignInPane` | `PortfolioScreen.kt` | Role picker |
| `PulsePane` | `PortfolioScreen.kt` | Stage billing and enquiries |
| `DocumentsPane` | `PortfolioScreen.kt` | Letters, transmittals, work library |
| `PathPane` | `ProjectPages.kt` | Stage path |
| `MeetingsPane` | `ProjectPages.kt` | Meetings |
| `TimePane` | `ProjectPages.kt` | Time entries |
| `ApprovalsPane` | `ProjectPages.kt` | Approvals |
| `QuantityEntryPane` | `ProjectPages.kt` | RCC / civil quantity shortcut |
| `SchedulePane` | `ProjectPages.kt` | Programme |
| `StoresPane` | `ProjectPages.kt` | Material orders |

`ui/shell/Destinations.kt` names practice and project destinations with icons. Nothing navigates with it.

## Spec inventory count

The specification lists 32 principal screen types and asks for shared templates. Mapped onto this repo:

| Group | Spec screens | Already reachable somewhere | Notes |
| --- | --- | --- | --- |
| Home | Dashboard, notifications | 0 | |
| Portfolio | List, create, overview | List and create on both. Overview on React only, and it is a profile, not a health view | |
| Setup | Details, levels/rooms, team | Levels/rooms on both. Team on React. Details are partial | |
| My Work | Inbox, task detail, approval review | Project tasks only. No cross-project inbox | |
| Coordination | RFI, submittals, site instructions | React lists them in one view | |
| Quality | List, detail, capture, verification | React combines them. Android has none of the four on screen | |
| Drawings | Register, detail, revision, transmittal | React covers them in one view | |
| Measurements | List, sheet, entry, review | Both | |
| Site | Hub, daily log, purchase order | React | |
| Tools | Archi Calc, quantity abstract | Calc on both. Abstract on React | |
| Practice | Registries, profile, backup | React profile and JSON backup. Android read-only profile | |

A shared record shell (identity, context, status, attachments, history, next action) does not exist on either client. React repeats card layouts per feature. Android repeats `GlassSurface` cards, which are Material `Card`s.

## Navigation the 2.0 specification wants, and what to reuse

| Destination | Desktop (spec) | Android (spec) | Reuse |
| --- | --- | --- | --- |
| Home, Projects, My Work, Site, Practice | Sidebar | Bottom navigation, five items | New shell. Do not add RFIs, snags, drawings, or measurements as global items |
| Search | Top bar | Search action | New. No query API today |
| Project sections | Secondary navigation | Tabs or a section list | React More row is the closest pattern, and it is too wide for a phone |
| Record detail | Page or side panel | Full screen | Neither client has a detail route |
| Create | Dialog or page | Sheet or full-screen form | Create project is already a dialog on both |
