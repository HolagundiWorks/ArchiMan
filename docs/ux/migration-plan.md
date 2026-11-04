# ArchiMan 2.0 migration plan

Audit date: 4 November 2026. This plan follows the specification's staged order. It does not move files, add dependencies, or change screens.

The audit documents are:

- `docs/ux/current-state-audit.md` — what the code does, against the README and the specification
- `docs/ux/screen-inventory.md` — reachable, unlinked, and missing screens
- this file — the order to change them

## Decision the later phases depend on

The 2.0 specification makes React the practice workspace and Android the field client, with one vocabulary. The current README says the opposite: Android is the freelance product, and `src/` is a prototype.

Do not start the navigation shell until that choice is confirmed. If 2.0 is the direction, the freelance Android cut stays in Room and comes back only as project sections (letters, proposals, contracts, fee invoices, visits), not as a replacement for snags, drawings, and the M-Book. If the freelance cut remains the product, this plan stops after the audit.

Assumption used below: 2.0 is the direction the specification just set. Fee invoices stay a project record. Rates stay off the M-Book. The running-bill screen is not deleted in the first shell; it moves under project fees only after the measurement phase says where money lives.

## What must not happen in one pass

- Do not restructure the repo into `apps/` and `packages/` before a workflow has been migrated. The specification calls that a target, not the current layout.
- Do not share React components with Compose.
- Do not add a router, DataStore, or a design-token generator until the shell phase needs it. React has no router today. Android already uses Navigation Compose.
- Do not replace Room schema 28 with a destructive migration. The installed phone database is schema 28. New tables or columns need an explicit migration.
- Do not treat `docs/` claims about an encrypted `.archimandb` package as code that can be called.
- Do not put RFIs, snags, drawings, measurements, or purchase orders on the global navigation.

## Phase 0 — Baseline (this task)

Done when these three files exist and application code is unchanged.

Still open before Phase 1, and not part of this task:

- Golden fixtures for the five quantity formulas, the sheet transitions, and the snag close rule. Android already has `MeasurementDomainTest`. There is no shared fixture file.
- A sample `.archimandb.json` checked in from the React exporter, plus a note that it is JSON version 27 and not the encrypted package in the older docs.
- A written map of schema 28 tables that the freelance UI stopped showing. The audit's data-model table is that map.

## Phase 1 — Design tokens, no new screens

Add a small token file both clients can read, without a new build plugin:

- `packages/design-tokens/tokens.json` only when a second client is ready to consume it. Until then, one JSON file under `docs/ux/` is enough, so the Android app does not gain a Gradle source it does not use.
- Colors from the specification, checked for WCAG AA contrast before they replace Material defaults: `#173B40` on `#F5F5F1`, `#247C80`, warning `#D69B40`, and a dark-theme pair that is not just an inverted hex.
- Status labels shared as words: measurement `DRAFT`, `SUBMITTED`, `CHECKED`, `APPROVED`, `RETURNED`; snag `OPEN`, `IN_PROGRESS`, `READY_FOR_VERIFICATION`, `CLOSED`; severity `NORMAL`, `ATTENTION`, `CRITICAL`; coordination statuses already coded in `CoordinationWorkflow`.
- Spacing 4, 8, 12, 16, 24, 32. Corners 8, 12, 16. Icon sizes 18, 20, 24.

Android keeps Material 3. Map tokens onto `lightColorScheme` / `darkColorScheme`, and keep dynamic color only if the product still wants wallpaper color. The specification's fixed primary and the current dynamic theme cannot both be the brand.

React replaces indigo/slate utilities at the shell first, not inside every feature in the same change.

Components to introduce once, with enabled, disabled, and error states:

- status chip (text plus icon, not color alone)
- record card
- empty and error blocks
- form field group

Android already has `ChoiceChip`, `ArchiField`, `ScreenTitle`, and `announce()`. Extend those. Do not add a second design-system module beside them in the same phase.

## Phase 2 — Navigation shell

### Practice destinations

| Id | Desktop | Phone |
| --- | --- | --- |
| Home | Sidebar | Bottom navigation |
| Projects | Sidebar | Bottom navigation |
| My Work | Sidebar | Bottom navigation |
| Site | Sidebar | Bottom navigation |
| Practice | Sidebar | Bottom navigation, label More if five items do not fit |
| Search | Top bar field | Top bar action |

Home can be a static empty dashboard in this phase. It should list projects and counts that already exist (open sheets, open snags) and must not invent notifications.

### Project destinations

Shown only after a project is open. Secondary navigation on React. On Android, a section list or scrollable tabs inside the project, not five more bottom items.

| Section | First release contents | Already reachable |
| --- | --- | --- |
| Overview | Profile, levels, sheet and issue counts | React profile. Android levels |
| Site & Quality | Link to existing snag list and daily log | React |
| Coordination | Link to existing RFI / submittal / SI list | React |
| Drawings | Link to existing register | React |
| Measurements | M-Book and record | Both |
| Documents | Transmittals | React drawings tab |
| Setup | Levels, rooms, team | Both, team on React |

Android practice bottom navigation replaces the four chips. The eleven project chips become the project section list. `Destinations.kt` can be the seed for those labels and icons, after its string resources match this table. It is unused today, so wiring it is a shell change, not a behavior change, until the `when` branches move with it.

Preserve data:

- Android routes stay `portfolio` and `project/{projectId}` until a section needs a back stack. Then add `project/{projectId}/{section}` without changing Room.
- React may add a router in this phase because the specification's desktop shell needs a URL. That is the first justified dependency. Keep `localStorage` keys stable.
- Opening a project still creates nothing. Ground Floor remains part of create only.

My Work and Site, at practice level, are filters over existing project records. They are not new tables.

## Phase 3 — One workflow at a time

Order inside the phase. Each item ends with the existing transition rules still passing.

1. **Snag / NCR on Android**, using `SiteIssueWorkflow` and `site_issues`. Mandatory verification note. Severity label and icon for Normal, Attention, and Critical. Photo capture is a later slice; URI fields can show first so the workflow is testable without a camera.
2. **Coordination on Android**, one record layout for RFI, submittal, and site instruction, using `CoordinationWorkflow`. Do not merge their statuses.
3. **Drawings on Android**: register, revision list, HFC as `packageTag`, as-built flag, transmittal. HFC stays a status, not an upload.
4. **React detail routes** for the same three, so desktop can use a side panel. Reuse the same status words.

Leave letters, proposals, contracts, and fee invoices where Android already shows them, under the project, until Phase 4 decides the fees section. Do not delete them to match the older quantity-only docs.

## Phase 4 — Measurement Book and Siteman

1. Keep the five formulas and the sheet graph byte-for-behavior identical. Extend `MeasurementDomainTest` rather than rewriting `QuantityCalculator`.
2. React should write a review comment row on transition, matching Android `measurement_review_events`.
3. Guided entry order: contractor, location, work item, formula, dimensions, quantity preview. Both screens already compute quantity live. The work is the field order and the preview of formula, unit, and deduction.
4. M-Book stays free of rates and currency.
5. Siteman on Android: daily log, HFC package list, quantity abstract CSV, purchase order quantity and specification, Archi Calc. `StoresPane` is not the design. Build the hub and retire the unlinked pane when the hub is reachable.
6. Quantity abstract reads approved measurement rows. It does not apply the rate book.

Running bill stays out of this phase's M-Book work. If fee billing remains a product requirement, it gets its own project section after the quantity abstract is stable.

## Phase 5 — Field parity and backup

1. Phone and a wide window: bottom navigation below 600 dp, navigation rail or permanent drawer from 600 dp up. Bottom navigation is the phone pattern in the specification, not a permanent side menu.
2. TalkBack pass on the shell, snag close, and measure form. Contrast of the chosen tokens against the Material surfaces.
3. Backup, as a separate design, not a silent sync:
   - Keep restoring the React JSON only behind an explicit confirm, and fix import so decisions, tasks, and consultants round-trip. Add inspections and meetings to the file or document that they are excluded.
   - Android backup is unspecified in code. Do not implement the encrypted package until its manifest is written down. Do not overwrite an existing file without confirmation.
   - Do not pretend the two databases sync.

## Acceptance checks that can be run now

| Check | Now | After the matching phase |
| --- | --- | --- |
| Ground Floor on create | Passes in both clients | Keep the repository method. Add a test that a second setup visit does not insert another Ground Floor for the same project |
| Sheet transitions and return comment | Passes. Android test exists | Add the same cases as shared fixtures |
| Snag close requires a note | Passes in the Kotlin object and the React screen | Android UI must call the object |
| Quantity formulas | Passes | No formula edits in the shell phases |
| Offline | Both clients are local | Stays true until a sync design exists |
| Existing React JSON restore | Runs, with the gaps in the audit | Confirm dialog and full round-trip |
| Encrypted `.archimandb` | Not implemented | Out of scope until specified |
| Every current feature reachable | False on Android for snags, RFIs, drawings, logs, orders | Phase 3 and 4 |
| Phone, tablet, desktop | React is a wide light layout. Android is a phone chip layout | Phase 2 and 5 |

## Suggested first implementation task after this audit

Phase 1 tokens plus the Android practice shell only: five destinations, Material icons, bottom navigation on a phone, and the existing Projects, Clients, Invoices, and Practice content moved under Projects and Practice. No new records. No schema change. React sidebar in the following task, pointed at the screens it already has.
