# ArchiMan current-state audit

Audit date: 4 November 2026. Scope: the repository as it is on disk, compared with the ArchiMan 2.0 cross-platform UX specification.

This audit does not change application code. It separates four layers that the repository currently mixes:

| Layer | Meaning in this document |
| --- | --- |
| Reachable | A person can open it in the running UI |
| Persisted | A table, type, or file format exists, with or without a screen |
| Documented | README, `docs/`, or `demo/` describes it |
| Proposed | ArchiMan 2.0 specification only |

## 1. What the repository actually is

There are two clients and no shared package.

| Surface | Path | Role today |
| --- | --- | --- |
| Android | `app/` | The product named in the current README. Kotlin, Jetpack Compose, Material 3, Room schema 28, Hilt, Navigation Compose. minSdk 26, compile/target 35. |
| React prototype | `src/` | React 18, TypeScript, Vite, Tailwind 4, Lucide. No router and no state library in `package.json`. The README says this is not the product build. |
| Docs and briefs | `docs/`, `Updated Briefs/` | Older consultancy-suite and portal descriptions. Several of them still say schema 27 and describe an encrypted `.archimandb` package and a LAN portal. |

There is no `apps/`, `packages/design-tokens/`, `packages/domain-contracts/`, or `tests/shared-fixtures/` tree. Domain rules are copied, not shared: `src/domain/*.ts` and `app/src/main/java/.../domain/*.kt`.

`app/src/main/java/com/holagundi/archiman/ui/shell/Destinations.kt` and the navigation strings in `strings.xml` are unused. No screen imports them. Live navigation is still the private enums in `PortfolioScreen.kt` and `ProjectWorkspaceScreen.kt`.

## 2. Product-direction conflict

Three different products are described at once.

1. **Current README product paragraph.** A freelance architect phone app: projects, clients, briefs, proposals, contracts, kanban, letterhead letters, fee invoices, visit reports, measurement, and a running bill. React is called an earlier prototype.
2. **README feature table and `docs/`.** A quantity-only consultancy suite: M-Book, Siteman, snags/NCRs, RFIs, drawings/HFC, practice backup. `docs/APP_STRUCTURE_AND_HIERARCHY.md` still describes bottom navigation for Projects, Contacts, Library, and Practice, plus a project bar of Project, Work, Record, M-Book, and More. That hierarchy matches the React prototype, not the Android screens that ship now.
3. **ArchiMan 2.0 specification.** React becomes the practice workspace. Android becomes the field client. Both keep one vocabulary. Practice navigation and project navigation are separate. RFIs, snags, drawings, and measurements stay inside a project.

The Android UI was narrowed to the freelance set. The Room database was not. Schema 28 still contains snags, coordination, drawings, daily logs, material orders, and the older consultancy tables. Those rows are stored. Most of them have no reachable screen.

The 2.0 specification and the freelance cut disagree. The data for the wider suite is still on the phone. The screens for it are not.

## 3. Navigation that actually runs

Neither client uses URL routes for sections. Android has two Navigation Compose destinations. React switches two root components with `useState`.

### Android routes

| Route | Screen | What it shows |
| --- | --- | --- |
| `portfolio` | `PortfolioScreen` | One of four local tabs |
| `project/{projectId}` | `ProjectWorkspaceScreen` | One of eleven local pages |

`MainActivity` applies `ArchiManTheme` and edge-to-edge insets. There is no drawer, rail, or bottom bar. Tabs are `FilterChip` rows (`ChoiceChip`).

Practice tabs that a person can open:

| Tab | Pane |
| --- | --- |
| Projects | Project list and create dialog. Create writes a Ground Floor. |
| Clients | Client list and a name/phone/email form |
| Invoices | Read-only list of fee invoices |
| Practice | Company profile as letterhead. No editor, logo, backup, or restore |

Project pages that a person can open:

| Page | Pane | Notes |
| --- | --- | --- |
| Brief | `BriefPane` | Objectives, requirements, constraints |
| Tasks | `KanbanPane` | To do / Doing / Done, stacked vertically |
| Letters | `LettersPane` | Letterhead text from the company profile. No print or share |
| Proposal | `ProposalPane` | Fee proposal title and amount |
| Contract | `ContractsPane` | Work contracts |
| Visits | `SitePane` | Inspection text. The screen says photos come later |
| Levels | `OverviewPane` | Profile summary, floors, rooms |
| Measure | `RecordMeasurementScreen` | Quantity entry plus Archi Calc |
| M-Book | `MeasurementBookScreen` | Sheet list, status changes, CSV share |
| Invoice | `FeeInvoicePane` | Draft fee invoice, issue, record payment |
| Running bill | `EstimatePane` | Quantity times rate. Rate is entered on this page |

Compiled composables that the `when` does not call: `PathPane`, `MeetingsPane`, `TimePane`, `ApprovalsPane`, `QuantityEntryPane`, `SchedulePane`, `StoresPane`, and in the portfolio file `SignInPane`, `PulsePane`, `DocumentsPane`.

### React screens

`App.tsx` shows `PortfolioScreen` or `ProjectWorkspaceScreen`. There is no React Router. Browser refresh cannot deep-link a project.

Portfolio tabs: Projects, Contacts (clients and contractors), Library, Practice.

Project tabs: Project, Work, Record, M-Book, More.

More is a second chip row, not a separate route: Siteman Hub, Drawings & Revisions, Site Reports & Snags, Coordination (RFIs), Planning & Decisions.

`MoreSection` also names `BRIEF_SCOPE`, `DECISIONS`, and `CONTRACTORS`. Those three are not buttons on the More bar. Brief, decisions, and tasks sit inside `PlanningView`. Contractors are edited from Contacts and from project overview flows.

Icons exist on the React tabs through Lucide. Android chips are text plus a check icon when selected. Android has no side menu and no bottom navigation.

## 4. Domain rules that are implemented

These rules exist in both languages and match each other.

### Quantity

`QuantityCalculator` in `src/domain/QuantityCalculator.ts` and `app/.../domain/QuantityCalculator.kt`.

| Mode | Result |
| --- | --- |
| `NOS` | number |
| `RUNNING_LENGTH` | number × length |
| `AREA` | number × length × breadth |
| `WALL_PLASTER` | number × length × height − deduction |
| `VOLUME` | number × length × breadth × height |

Negative inputs are clamped to zero. The result is rounded to two decimal places. An unknown type falls through to area.

React Record Measurement shows the live quantity. Android Record Measurement uses the same calculator. Archi Calc can insert a length into the Android measure form.

### Measurement sheet review

`MeasurementSheetWorkflow` in both clients:

| From | Allowed next |
| --- | --- |
| `DRAFT` | `SUBMITTED` |
| `RETURNED` | `SUBMITTED` |
| `SUBMITTED` | `CHECKED`, `RETURNED` |
| `CHECKED` | `APPROVED`, `RETURNED` |
| `APPROVED` | none |

`RETURNED` requires a non-blank comment.

Android `MeasurementRepository.transition` enforces this and writes `measurement_review_events`. Android also refuses to delete a row when the sheet is `CHECKED` or `APPROVED`. CSV export is quantity columns only (`MeasurementSheetCsv`).

React `MeasurementBookView` uses `allowedTransitions` and asks for a return comment, then calls `store.updateSheetStatus`. That store method writes the new status and does not store a review-event row.

### Snag / NCR

`SiteIssueWorkflow` in both clients:

`OPEN` → `IN_PROGRESS` → `READY_FOR_VERIFICATION` → `CLOSED`, with returns to the previous step. Closing requires a verification note.

React `SiteReportsView` uses this and shows severity as the words `NORMAL`, `ATTENTION`, and `CRITICAL` with color. Android has the workflow object and the `site_issues` table. No reachable Android screen calls `SiteIssueWorkflow`.

### Coordination

`CoordinationWorkflow` in both clients:

| Type | Path |
| --- | --- |
| `RFI` | `DRAFT` → `OPEN` → `ANSWERED` → `CLOSED` (or back to `OPEN`) |
| `SUBMITTAL` | `RECEIVED` → `UNDER_REVIEW` → `APPROVED` / `APPROVED_AS_NOTED` / `REVISE_RESUBMIT` → `RECEIVED` |
| `SITE_INSTRUCTION` | `DRAFT` → `ISSUED` → `ACKNOWLEDGED` → `COMPLIED` → `CLOSED` |

React `CoordinationView` uses it. Android has the object and `coordination_items` / `coordination_events` tables. No reachable Android screen calls it.

### Archi Calc

Both engines evaluate expressions, including `sin`, `cos`, `tan`, powers, square root, and reciprocal, and keep a six-way metric/imperial grid (`m`, `m²`, `m³`, `ft`, `ft²`, `ft³`). Feet-and-inch text such as `12'-6"` is parsed on Android (`ArchiCalcEngine` / `MeasurementUnitConverter`). React mounts the pad inside Siteman and Record Measurement. Android mounts it inside Measure.

### Project creation

Both clients insert one floor named `Ground Floor` inside project creation (`PracticeStore.addProject`, `ProjectRepository.createProject`). The Android create dialog tells the user this. Neither client checks for an existing Ground Floor on a later visit, because creation runs once per new project id. A second call that inserted another project would add another Ground Floor for that new id only.

### Running bill (Android only)

`EstimateBuilder` multiplies aggregated quantity by a rate. Missing rates become 0. This is commercial. It contradicts the quantity-only M-Book rule in the README feature table and in the 2.0 measurement flow. The M-Book screen itself does not show rates. The Running bill page does.

## 5. Persistence

### Android

Room database `ArchiManDatabase`, `SchemaContract.VERSION = 28`. New installs create schema 28. An existing schema 27 database uses `MIGRATION_27_28`, which adds tables. `fallbackToDestructiveMigration` is not called.

Schema 27 identity recorded in the migration comments is `df9616b39618434be105ba6fd23e8773`. Exports live in `app/schemas/.../27.json` and `28.json`.

There is no DataStore. There is no Kotlin `.archimandb` writer. Photo columns exist (`photoUri`, evidence URIs). Visit capture does not take a photo. Attachment directories described in `docs/MIGRATION_BACKUP_AND_ROLLBACK.md` are not created by the current Kotlin UI.

Seed data in `DatabaseSeeder` loads a sample practice, projects, floors, contractors, qualified items, and measurement sheets when the database is empty. `SuiteRepository.ensureDefaults` adds local users, a rate book, and item-link rules when those counts are zero. The portfolio does not ask anyone to sign in.

### React

`localStorage` keys prefixed `archiman_v27_`. Seed data in `src/data/seedData.ts` fills an empty browser. This store is not the phone database. The two clients do not sync.

`exportDatabase()` downloads a JSON file named `.archimandb.json` with `version: 27`. It is not encrypted and it is not a SQLite snapshot. Restore calls `importDatabase` immediately. No confirmation dialog was found. Import replaces the collections it understands and leaves others untouched.

Export includes decisions, tasks, and consultants. Import does not read those three arrays back. Inspections and meeting minutes are in the store and on screen, and they are absent from both export and import.

`resetToDefault()` clears `localStorage` after a reset control in Practice. That is a separate, destructive action from restore.

### Documented backup that the code does not do

`docs/MIGRATION_BACKUP_AND_ROLLBACK.md`, `docs/IMPLEMENTATION_ROADMAP.md`, and `demo/README.md` describe a password-encrypted `.archimandb` package: SQLite snapshot, logo, `files/measurement_attachments/`, `files/site_attachments/`, and SHA-256 manifest. No Kotlin or TypeScript implementation of that package format is in this tree. `demo/README.md` points at a generator that is not in the repo. Treat the encrypted package as documented, not implemented.

## 6. Data model versus screens

Room entities are the Android source of truth. React `src/types/index.ts` mirrors the schema 27 shapes and does not include schema 28 tables (invoices, letters, proposals, contracts, stages, receipts, and the rest of `SuiteEntities.kt`).

| Area | Android stored | Android reachable | React reachable |
| --- | --- | --- | --- |
| Company profile | Yes | Read-only letterhead | Edit, plus JSON backup |
| Clients | Yes | Create (name, phone, email) | Directory |
| Projects, floors, rooms | Yes | Create project, add level, add room | Same, richer overview |
| Contractors, qualified items, item master | Yes, seeded | Used inside Measure | Contacts and Library |
| Measurement sheets and rows | Yes | Measure and M-Book | Record and M-Book |
| Review events | Yes | Written on transition | Not stored |
| Tasks | Yes | Kanban `TO_DO` / `DOING` / `DONE` | Planning, `OPEN` / `COMPLETED` toggle |
| Brief / consultancy profile | Yes | Three text fields | Inside Planning |
| Letters | Yes (schema 28) | Create on letterhead | No letter entity |
| Fee proposals, contracts, invoices, receipts | Yes | Proposal, contract, invoice, running bill | No |
| Inspections | Yes | Text report, no camera | Site Reports tab |
| Daily logs | Yes | No | Siteman |
| Snags / NCR and events | Yes | No | Site Reports, workflow enforced |
| Meetings | Yes | No | Site Reports |
| Decisions, scope, onboarding, backlog | Yes | No | Decisions inside Planning |
| Coordination items and events | Yes | No | Coordination |
| Consultants / RACI | Yes | No | Coordination consultants tab |
| Drawings, revisions, transmittals | Yes | No drawing register. Transmittal insert exists in the repository and is not on a reachable page | Drawings register and HFC tag |
| Site documents, material orders | Yes | `StoresPane` is not linked | Siteman |
| Components and component work items | Yes | No | Types only, no screen found |
| Local users, portal audit | Yes, users seeded | No sign-in | Types only |
| Stages, permits, time, RCC, rate book, item links, schedule activities | Yes | Dead panes or repository methods only | Not in the React model |
| Enquiries | Yes | `PulsePane` is not linked | No |

HFC in React is a `packageTag` of `HFC` on the drawing, toggled by a button. It is not a file upload. Revision rows have `revisionCode`, `revisionSource`, and `isAsBuilt`. That matches the specification's warning that HFC must not mean "a file was uploaded." Android stores the same columns and does not show them.

## 7. Spec flows against this tree

| Spec flow | React | Android |
| --- | --- | --- |
| A. Create project, Ground Floor, then overview | Reachable. No duplicate-floor guard beyond a new id | Reachable. Overview is the Levels page, not a health dashboard |
| B. Snag / NCR with evidence and mandatory verification | Reachable. Severity is text. Photo fields are URI strings, not a camera | Workflow and table only |
| C. Guided measure, live quantity, review statuses, no money on the M-Book | Reachable | Reachable. A separate Running bill page does show money |
| D. Drawing register, revision lineage, HFC, transmittal, as-built | Reachable in one Drawings view | Tables only |
| E. Shared coordination record shell | One list/detail-style view per type, not a shared shell component | Tables and workflow object only |
| F. Siteman hub | Reachable: logs, HFC docs, quantity abstract, purchase orders, Archi Calc | Not linked. Archi Calc is under Measure |

## 8. Design system versus the specification

| Token in the 2.0 spec | What the code does |
| --- | --- |
| Primary `#173B40`, accent `#247C80`, surface `#F5F5F1` | Not used. React is Tailwind slate/indigo. Android uses Material 3 dynamic color on API 31 and the default light/dark schemes below that |
| Roboto / Inter type scale 12–32 | Android uses the stock Material type scale. React uses Tailwind defaults |
| Light, dark, system | Android follows the system theme. React is a light slate layout |
| Side menu on desktop, bottom navigation on Android | React uses top tabs. Android uses wrapping chips |
| String resources | Android navigation labels are mostly hardcoded. New unused strings exist for a drawer that is not on screen |
| 48 dp targets, headings, live regions | Partially addressed on Android chips, titles, and notices. Not a TalkBack pass |

## 9. Tests

`app/src/test/.../MeasurementDomainTest.kt` covers schema version 28, quantity formulas, the return-comment rule, Archi Calc, work codes, unit conversion, quantity-only CSV, `ProjectPath.next`, and `EstimateBuilder`. There is no Compose UI test and no shared fixture pack. React has no test script in `package.json`.

## 10. Gaps that block a straight redesign

1. The freelance UI cut and the 2.0 information architecture name different products. Schema 28 can hold the wider model. The Android navigation cannot reach it.
2. React and Android do not share identifiers at runtime. Browser `localStorage` and Room are separate databases.
3. `.archimandb.json` (React) and the documented encrypted `.archimandb` (docs) are different formats. Android implements neither.
4. React restore does not round-trip every collection it exports, and it does not ask before replacing data.
5. Task status vocabularies differ (`TO_DO`/`DOING`/`DONE` versus `OPEN`/`COMPLETED`).
6. Measurement review comments are enforced in both UIs, but only Android persists the audit row.
7. Commercial running bills exist on Android beside a quantity-only M-Book. The 2.0 measurement flow says rates stay out of the M-Book. It does not yet say where fee invoices live. The current freelance README treats invoices as in scope.
8. Docs that say the Android bottom bar and the encrypted package are already shipped are ahead of this tree.
