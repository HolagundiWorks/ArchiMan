# ArchiMan UX audit against the 12 core principles

Date: 6 November 2026. Baseline: current working tree, Android 1.1.0 and its HTTPS portal, after the Carbon update and project-dialog keyboard fix.

Remediation: the subsequent [1.1.1 implementation](ux-remediation-2026-11-06.md) addresses the findings below. This audit retains its original baseline; see the remediation document for current behavior and verification limits.

## Scope and interpretation

This is a source-based heuristic audit, supported by the existing phone and browser verification from the Carbon implementation. It is not a new usability study, full screen-reader assessment or accessibility certification. No practice data was changed for this audit. Scope is the native Kotlin Compose app and the optional HTTPS portal; there is no React product tree in this repository.

Evidence below uses repository-relative paths; native UI paths are under `app/src/main/java/com/holagundi/archiman/ui/`, portal paths under `app/src/main/java/com/holagundi/archiman/portal/`. “Confirmed” means directly present in the implementation; a source observation does not establish how often real users encounter the problem. Items explicitly marked for device testing remain risks, not reproduced failures.

Assessment: the visual foundation is substantially improved, but workflow usability remains uneven. Carbon colors and components do not by themselves resolve save behavior, information architecture or recovery. There is no defensible overall numerical usability score without agreed criteria and user testing.

Priority: P0 blocks safe completion or exposes established major loss; P1 affects daily task completion, trust or recovery; P2 improves consistency, discoverability or efficiency. No new P0 defect was established by this audit.

## All 12 principles

| Principle | Current strengths | Gap and user impact | Assessment / priority |
|---|---|---|---|
| 1. Clarity over cleverness | Labeled navigation and CTAs; record menus expose Edit details, Archive and Delete; units/formulas are visible. | Measurement filters may still expose raw uppercase workflow codes in places; users cannot always predict every state label. Running bill now uses `RunningBillsPane` (approved quantities + portal handoff), not the old rate-book pane. | Needs work — P1, F09 |
| 2. Consistency builds confidence | Shared Carbon palette, Plex, icons, square cards/buttons and navigation; shared record management. | Measurement dimensions use separate outlined controls; generic editors make every field multiline. Finance and project notices bypass typed notifications. Validation and save behavior differ across modules. | Partial — P1/P2, F03/F07/F09 |
| 3. Visibility of system status | Record actions show Loading actions and Saving; backup has busy/error state; dashboards show loading/empty data; portal pagination reports visible counts. | Measurement capture has no saving state or disabled-while-saving action. Shared project mutations surface errors but no general busy/success state. A disappearing dialog or changed list is often the only confirmation. | Needs work — P1, F03 |
| 4. Recognition over recall | Project groups and nested hubs; searchable projects, measurement sheets and portal tables; action restrictions explain why an operation is disabled. | Assigned-item/contractor dropdowns are unsearchable. Mobile setup tells the user to use the portal without a direct contextual setup route. Finding an exact attention item opens the module, not the record. | Partial — P1/P2, F04/F08 |
| 5. Efficiency for frequent tasks | Project defaults include Ground Floor; native capture preserves values with saveable state; unit changes convert dimensions; portal tables sort/filter/page. | Successful measurement save takes the user to the book. No explicit Save and add another; selection menus enumerate all options. No batch measurement-review control in the audited book. | Needs work — P1, F04/F05 |
| 6. User control and freedom | Cancel, edit, archive/reopen, destructive confirmation, receipt reversal, restore preview and encrypted recovery exist. | Several create handlers clear fields immediately after launching an asynchronous save. Save failure can leave the form blank. Generic record editor fields use `remember`; there is no consistent durable draft/dirty-exit policy. No general undo affordance. | Needs work — P1, F01/F06 |
| 7. Progressive disclosure | Four project groups, nested Documents/Measurements, optional measurement details and record overflow menus. | Contractor appears both in the main measurement form and expanded details. Portal setup mixes configuration/editors/registers and export controls. Long generic edit dialogs lack field-specific grouping. | Partial — P2, F07/F08 |
| 8. Error prevention before correction | Backend restrictions, required project fields, finite/nonnegative measurement validation, formula-dependent inputs, active-revision filtering, import preview and duplicate protection. | Capture silently chooses the first contractor/floor/item. Changing assigned line automatically changes item/formula but not the description; stale descriptions can remain. Native capture has no in-flight guard. Generic portal fields lack a shared field-specific required/constraint model; backend errors remain necessary. | Needs work — P1, F03/F04/F10 |
| 9. Strong visual hierarchy | Neutral square tiles, clear primary blue, exact-value chart legends and distinct support colors. | The practice hero, four KPI tiles and two charts precede Needs attention. Daily work can sit several screens down on a phone. Some edit/list pages have several peer save/actions without a single clear task. | Partial — P1/P2, F05/F08 |
| 10. Accessibility and usability | Labeled tab semantics, minimum button touch sizing, light/dark contrast tests, notification live regions, browser skip link, sortable keyboard buttons and chart text alternatives. | Measurement dimension labels are separate 10 sp text, with no explicit input label semantics in their outlined fields. Four dimensions share one row; navigation labels are one line on fixed-width rails. Large text, TalkBack, switch access, zoom/reflow and all-dialog IME behavior need a dedicated pass. | Partial; verification incomplete — P1, F07/F11 |
| 11. Workflow-oriented design | Practice → project → Work/Site/Fees; contractor-specific active revision drives measurement; quantities are reviewed before bill valuation. | Native Running bill is a rate book, while actual contractor billing/revision setup is in the portal. No integrated checklist takes a new project through contractor assignment → active revision → entry → approval → bill. | Needs work — P1, F02/F08 |
| 12. Immediate meaningful feedback | Bank import reports a count; portal measurement import reports imported entries and duplicate skipping; invalid operations explain restrictions. | Shared saves often return no result-specific receipt. Record edits close silently. Finance messages can display errors in primary blue; portal replaces ordinary status notices with generic informational Update notices. | Needs work — P1, F03/F09 |

## Confirmed findings and remedies

### F01 — Preserve entered work until save succeeds (P1)

Evidence: `ProjectPages.kt` task, visit, meeting and other create handlers call a ViewModel mutation and immediately clear fields. `ViewModels.kt` `SuiteProjectViewModel.launch` starts a coroutine and catches errors later. This is a confirmed failure-path risk: the form clears regardless of the result; no new production save failure was induced.

Remedy: return an operation result, retain the complete draft on failure and clear only the successfully saved draft. Show Saving, block repeat submission and put validation beside the affected field. Do not clear a newer draft when an earlier request completes.

Acceptance: simulate delayed success, validation rejection and storage failure. Success clears the submitted draft once; failures retain every input and selection; retry produces one record. Cover tasks and visits first, then reuse the same pattern across forms.

### F02 — Make Running bill lead to actual billing (P1) — addressed

Evidence (current): `ProjectWorkspaceScreen.kt` maps BILL to `RunningBillsPane` (`ProjectPortalHandoff.kt`): approved quantities, unbilled context and a project-specific portal billing handoff. The old rate-book `EstimatePane` was removed. Portal contractor bills remain in `PortalContractorBills.kt`.

Acceptance (met by remediation): Running bill no longer opens an unrelated rate editor; the user sees contractor/quantity context and how to continue billing. Contractor measurements/bills remain tax-exclusive; architect fee GST/TDS remains separate.

### F03 — Standardize operation state and prevent repeat submission (P1)

Evidence: `RecordMeasurementScreen.kt` Save entry enablement checks inputs but not an in-flight state. `RecordMeasurementViewModel` exposes error only and launches a save for every call. `SuiteProjectViewModel.launch` has no shared busy/result state. `RecordActions.kt` and `BackupPane.kt` already provide stronger patterns. This establishes absent UI guards, not a reproduced duplicate database record.

Remedy: operation-scoped idle/saving/success/error states; synchronous in-flight guards; retain backend transaction/idempotency protections. Announce the record, result and useful next action: “Entry saved to draft sheet MB-… · 4.05 m³. Add another / View sheet.” Avoid one global busy flag blocking unrelated work.

Acceptance: repeated taps during a delayed save cause one mutation; failure preserves the draft, reenables Retry and is announced; success includes record context. Cancellation behavior is explicit while work is in flight.

### F04 — Make measurement selection safe and fast (P1)

Evidence: `RecordMeasurementScreen.kt` selects first contractor/floor and first assigned line in `LaunchedEffect`. Automatic line replacement updates item/formula but does not update description; explicit item selection does. `MeasurementSelect` is an unsearchable dropdown. Contractor is repeated in expanded details.

Remedy: distinguish deliberate defaults from silently assigned choices. Retain last-used selection within the project/contractor/revision; show a compact context summary. Search items by code/description. Update an untouched generated description with the item; preserve user-edited text and flag the context change. Remove duplicate contractor controls. Do not silently reinterpret dimensions when a new formula/unit is chosen; confirm/reset or convert explicitly.

Acceptance: contractor/revision changes cannot retain an invalid item key; generated descriptions follow the item; custom descriptions are preserved; a 200-item list is searchable; zero eligible items offers the exact setup action. Verify unit/formula switches without changing physical quantity unexpectedly.

### F05 — Put repeat capture and actionable work first (P1)

Evidence: `ProjectWorkspaceScreen.kt` routes measurement success to BOOK. `PracticeDashboard.kt` places Needs attention after the hero, KPIs, fee chart and project mix. Dashboard task/review rows navigate to a page without the selected record ID; only the Current projects metric has a direct drill-down among the four KPI tiles.

Remedy: Save and add another retains contractor/floor/revision and clears only entry dimensions/description as appropriate; View sheet remains available. Move a compact attention summary above charts. Make metrics open filtered lists and attention rows open the exact task/sheet. Keep charts as secondary summaries.

Acceptance: enter ten different measurements without repeatedly navigating back; every save reports its sheet. A pending review is reachable from the first dashboard viewport and opens the matching sheet, not an unfiltered register.

### F06 — Define draft, cancel and recovery behavior (P1)

Evidence: project pages use `SaveableStateProvider`, many forms use `rememberSaveable`, but `RecordActions.kt` uses ordinary remembered field maps. Neither is a general persisted draft store or a universal dirty-exit policy. Existing archive/reopen, receipt reversal and backup recovery must be retained.

Remedy: durable local drafts for long agreements/briefs/editors; visible draft state; safe exit choices Save draft / Discard / Continue editing. For reversible nonfinancial operations, offer Undo where practical. Keep issued invoices/receipts auditable through void/reversal rather than history-erasing undo. Prefer archive over project deletion.

Acceptance: rotation, app recreation and navigation do not lose a long unsaved draft; restoring a draft never overwrites a newer committed record silently. Destructive exits explain what will be lost. Export/download does not imply saved/issued state.

### F07 — Complete form consistency and accessible measurement layout (P1/P2)

Evidence: `ArchiTheme.kt` supplies labeled Carbon-style fields, but measurement dimensions use separate `OutlinedTextField`s with external 10 sp labels. `RecordActions.kt` sets all generic edit fields to multiline. Project creation now explicitly applies IME/system-bar insets; this does not prove every dialog is safe.

Remedy: a field schema defining label, type, unit, required state, keyboard, error and helper text. Use numeric keyboards for dimensions/money and appropriate phone/email keyboards. Give each input an accessible name including its unit. Reflow four dimension fields into two columns or a vertical layout as space/text scale requires. Apply a consistent keyboard-safe editor container to long forms.

Acceptance: complete forms with TalkBack and an external keyboard; 200% font size does not hide values/actions; labels/errors are announced with inputs; required fields are explicit; all Cancel/Save actions remain reachable with IME open. Verify compact phone, landscape, split-screen and tablet.

### F08 — Guide project setup and simplify portal information architecture (P1/P2)

Evidence: `PortalWorkspace.kt` exposes a flat workspace nav and mixes register/editor/export content. Native measurement setup directs the user to activate a revision in the web portal. Project section grouping already improves native navigation, but setup and bill creation remain split across surfaces.

Remedy: a project checklist: client/site → contractors → reference items/estimate → contractor assignments/rates → active revision → record → review → running bill. Offer direct actions only for missing prerequisites. Group portal navigation into Projects, Measurements & contractor bills, Practice setup and Documents; keep active project and revision visible. Explain the phone/portal division in the handoff.

Acceptance: a first-time freelancer can reach the first valid measurement without guessing which catalogue, schedule or revision screen to use. One project context persists through setup, review, billing and export; inaccessible prerequisites have a reason and a next action.

### F09 — Use domain labels and typed outcome feedback everywhere (P1/P2)

Evidence: `MeasurementBookScreen.kt` filter labels use raw statuses and `statusLabel` returns the stored string. `FinancePane.kt` defaults to Saved and sometimes displays notice text in primary blue, including failures. `ProjectWorkspaceScreen.kt` renders shared notices in primary color. `portal-ui/src/index.js` maps ordinary status notices to informational Update notifications.

Remedy: separate machine values from readable labels: Draft, Ready to check, Awaiting approval, Approved, Returned with comments. Carry typed outcome metadata from the operation to its display; do not infer success/error from message text. Provide counts and identifiers: imported, skipped, rejected, and where to inspect them.

Acceptance: failure is never styled as success; all outcomes are understandable without relying on color. The same workflow uses the same labels on phone, portal and exported documents while retaining the stored codes.

### F10 — Keep measurement tables aligned (P1)

Evidence: `MeasurementBookScreen.kt` creates independent `horizontalScroll(rememberScrollState())` containers for header and entry rows.

Remedy: one shared horizontal coordinate system for all columns; retain row identity and units while inspecting values. On small screens provide a compact entry card/detail view instead of requiring a wide spreadsheet for every task.

Acceptance: horizontal scrolling never separates values from their headings; the same dimension stays under the same header for all visible rows. Keyboard/screen-reader reading order identifies each row, dimension and unit. Pagination retains the selected sheet.

### F11 — Finish accessibility evidence rather than assume conformance (P1)

Evidence: `CarbonNavigation.kt` uses fixed-width rails and single-line labels. Current contrast tests, browser interaction tests and native keyboard smoke are valuable but do not cover a full accessibility journey. Portal table search names are generic Search table 1/2 in `portal-ui/src/index.js`, rather than domain names/captions.

Remedy: perform a dedicated TalkBack, keyboard-only, large text, touch target, contrast, zoom/reflow and focus restoration pass. Give tables meaningful captions/search labels; check modal focus entry/trapping/return and error-summary focus. Preserve chart exact-value alternatives and selected state semantics.

Acceptance: all critical journeys work without color, a mouse or precise tapping; browser content reflows at 320 CSS px and 200% zoom where applicable; chart meaning remains available in text. Document actual device/browser results and outstanding exceptions.

## Implementation order

| Stage | Deliverable | Exit criteria |
|---|---|---|
| 1 — Trust and reliable saves | F01/F03: shared operation results, no premature clearing, busy guards, contextual typed feedback | Delayed saves, rejected values, storage failures and rapid repeat taps tested on task, visit and measurement flows; same pattern adopted by remaining editors |
| 2 — Measurement and billing journey | F02/F04/F05/F10: correct billing route, safe searchable selection, repeat capture, aligned review tables | New project through assigned revision, ten measurements, review and tax-free contractor bill completed without context loss |
| 3 — Structure and recovery | F06/F08/F09: long-form drafts, setup checklist, attention-first home, domain wording and grouped portal navigation | Draft recovery and dirty exit verified; dashboard opens exact work; new-user setup needs no module-name guessing |
| 4 — Accessibility and consistency closure | F07/F11: field schema, responsive inputs and full accessibility matrix | TalkBack, keyboard, large text, narrow/wide layouts and both themes verified across critical journeys |

F07 accessibility fixes should accompany the affected components in earlier stages, not wait until stage 4. Stage 4 closes coverage across the rest of the app.

## Validation plan

Use isolated demo/test data for mutation checks. Preserve the existing practice and financial histories. Reuse current backend regression tests; their passing results establish backend behavior, not UX completion.

1. Create a project, correct missing fields, cancel and resume a draft; confirm no unintended project is created.
2. Create a task/visit with delayed save, failure and retry; confirm input retention and exactly one record.
3. Select contractor/revision/item; change unit/formula; capture ten entries; verify physical quantities and saved context.
4. Find a returned sheet, inspect comments, correct a draft, submit/check/approve eligible records and generate the corresponding contractor bill excluding taxes.
5. Issue an architect fee invoice, reconcile cash plus TDS and reverse a receipt; verify clear balance/result feedback and preserved audit history.
6. Preview a bank/measurement import with invalid and duplicate rows; compare reported imported/skipped/rejected counts against actual records. Verify full exports regardless of table filters/pagination.
7. Edit a long agreement, navigate away, rotate/recreate the app and recover the draft; verify cancellation and issued-document protections.
8. Repeat critical journeys with TalkBack/keyboard, large text, compact/landscape/wide layouts and light/dark modes. Inspect focus, labels, scrolling and keyboard reachability.

With representative freelance architects, measure completion without assistance, wrong-destination visits, taps per measurement, recovered save failures and correctly understood billing status. Record a baseline before claiming percentage improvements; validate a redesigned prototype before expanding the pattern.

## Relationship to earlier audits

This document is the current principles-based assessment. Earlier redesign notes (teal styling and pre-Carbon navigation) were removed as superseded. `carbon-components.md` and `carbon-colour-system.md` record the shipped visual/component foundation. Neither this audit nor those notes should be read as proof that every workflow satisfies all 12 principles. No implementation changes are included in this audit.
