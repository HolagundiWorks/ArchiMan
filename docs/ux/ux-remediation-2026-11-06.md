# UX remediation — 1.1.1

Follow-up in 1.1.2: entering a new recording session asks for the contractor in a popup. The entry form shows only a single-line contractor name with an edit affordance; the floor remains a dropdown. Changing contractor retains the dimension-clear confirmation, and cancelling the initial picker returns to the measurement hub without saving.

Phone checks passed for the new-session popup, initial cancellation, compact single-line name, changing/cancelling the contractor picker and the retained floor dropdown. The 1.1.2 production/audit builds, all 84 unit tests and lint passed.

Follow-up in 1.1.3: removed the duplicate project/Measurement Book breadcrumb beneath Record entry. Count and formula-relevant dimensions now share a single row. Narrow screens or enlarged text retain that row with horizontal scrolling instead of wrapping dimensions onto another row.

The 1.1.3 production build and lint passed. The installed-phone check confirmed removal of the duplicate breadcrumb and matching vertical bounds for all four input fields. No measurement was saved during this check; the crash buffer remained empty.

Implements the current [12-principle audit](ux-principles-audit-2026-11-06.md). The Android app remains a native Compose application; the HTTPS portal uses bundled Carbon components. Contractor measurements and running bills remain tax-exclusive.

## Changes

| Audit finding | Implementation |
|---|---|
| F01: premature form clearing | Shared project and practice operations return success-aware actions. Create forms clear submitted values only after a committed save and only if the user has not entered a newer draft. This includes tasks, visits, drawings/revisions, site logs, coordination, issues, clients, letters and payment entry. Project creation closes only on success. Blank required inputs now produce validation errors instead of silent repository no-ops. |
| F02: incorrect Running bill destination | The native destination now shows contractor-approved-entry context and a project-specific handoff to actual portal billing. It explicitly distinguishes approved totals from eligible unbilled quantities. It no longer presents the shared rate book as a contractor bill. |
| F03: operation state and repeat taps | Shared operations acquire a key before launching, expose pending work, reject repeated submissions and release guards on success, failure or cancellation. Measurement capture and sheet review have their own in-flight guards. Results use typed, announced feedback. |
| F04: measurement selection | Explicit contractor/item selection, searchable long option lists, context-change confirmation before clearing dimensions, preserved custom descriptions and no duplicate contractor control. Unit switching continues to convert dimensions. The portal also protects item changes from reusing dimensions for another formula. |
| F05: repeated capture and attention | Save and add another retains project/contractor/floor/revision context; Save and view sheet remains available. Newly edited values are not cleared by an older save completion. Needs attention appears before charts. KPI counts open filtered, progressively loaded inboxes; attention items route to their exact task, issue or sheet. |
| F06: recovery and control | Briefs, structured proposal/agreement editors and generic record editors retain device-bound encrypted local drafts. Resume is explicit; local drafts never silently replace a saved record. Record editors offer keep/discard/continue choices. Edits and archive/reopen have an Undo action while their controls remain mounted; undo checks the committed snapshot and rejects intervening changes. Financial void/reversal history remains protected. |
| F07: form consistency | Measurement dimensions use the shared labeled fields in a two-column flow, appropriate decimal keyboards and visible units. Generic editors distinguish numeric, phone, email, single-line and long-text fields; required labels and invalid-number feedback are explicit. Project creation remains keyboard-safe and blocks editing while saving. |
| F08: setup/navigation | A native measurement/billing checklist links prerequisites, active revision, recording, review and billing. Portal navigation groups practice setup and shows project identity/revision with an ordered estimate → assigned works → measurement → bill path. Portal pairing retains its existing certificate check and never puts access codes in links. |
| F09: readable labels and outcomes | Sheet filters/transitions use domain wording rather than raw codes. Shared project, finance and review results distinguish success/error/info. Portal notices accept explicit kinds. Import counts and retained financial histories remain visible. |
| F10: aligned tables | Measurement headers and rows share horizontal scroll state and matching widths. Entry accessibility text identifies the count, dimensions, description, location and quantity/unit. |
| F11: accessibility | Navigation labels can wrap, measurement fields have accessible names/units, portal tables have captions and domain search labels, form errors reference their feedback, and narrow layouts retain accessible controls. Browser forms warn before leaving typed work and guard repeat submits. Downloads are explicitly separate from saves and remain repeatable. |

## Draft storage and undo limits

Local editor drafts use AES-GCM through the existing device-bound Android Keystore cipher. Files are atomically written under app-private `noBackupFilesDir`, named with hashed keys and bounded to 2 MB each. Ordered, coalesced writes avoid retaining every keystroke as a queued full-document copy. The UI reports whether local persistence succeeded; a failed local write must not be mistaken for a saved record. These local drafts are not portable practice backups and are removed when the app is uninstalled.

Draft keys include the project/practice identity or the source record snapshot. Generic edit recovery and undo continue to respect repository concurrency and issued/approved-record protections. Undo does not erase receipts, issued documents or audit history. The Undo affordance is temporary; archive/reopen and financial reversal remain available through their normal workflows.

## Validation

- 84 unit tests passed, including delayed-save/repeat-tap protection, failure retention, unrelated operation independence and cancellation cleanup.
- Four Edge browser tests passed: Carbon assets/components and charts under the portal CSP; form values; table sorting/search/pagination; typed feedback; named tables; 320 px controls; required-field feedback and repeat-submit guards.
- Production/debug/test APK builds passed. Android lint: zero errors, 36 existing dependency/toolchain warnings.
- The connected-phone regression suite passed, including encrypted draft round-trip, size bounds/tamper rejection, invalid-form rejection, undo and stale-snapshot protection, and the existing backup, billing, GST/TDS, schedule, import, security, concurrency and 10,000-row checks. All 56 current practice tables compared unchanged before/after isolated testing.
- A separate optimized audit app verified exact task/sheet navigation, the setup checklist, corrected contractor-billing destination, one committed sample measurement followed by repeat capture, and encrypted brief-draft recovery after a cold restart plus discard. No sample records were written to the real practice.
- Native phone checks confirmed two-column dimensions at normal text size, single-column reflow at 200% text and reachable save actions. The original phone font scale was restored. A final loading-state safeguard distinguishes unavailable lookup data from a genuinely empty contractor/revision setup.

The code changes do not establish full accessibility conformance. A supervised TalkBack/switch-access assessment and representative architect usability study remain necessary; API 26/35 emulator CI jobs have not been executed locally. Existing phone checks, browser checks and contrast tests should not be presented as substitutes for that coverage.
