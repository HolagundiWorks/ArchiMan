# ArchiMan: repository review and comparable-product case study

Date: 4 November 2026.

## Recommendation

Build an offline practice companion for solo architects and small studios: understand today's priorities, capture site evidence, produce professional documents, and track fees. Finish these connected journeys before adding more modules.

This is a source review and desk study of official competitor material, not a device usability test or a claim of measured customer demand. No build or runtime verification was performed for this report. Recommendations and sequencing are judgments to validate with architects.

## What the repository contains today

The product is a Kotlin Android app using Compose, Room schema 28, Hilt and coroutines. Local persistence, measurement formulas, metric/imperial conversion, Archi Calc, measurement review states and quantity CSV export form a useful foundation.

Current source differs materially from the existing current-state audit:

- `MainActivity.kt` gates navigation through `LoginScreen` and `SessionViewModel`; `SessionStore.kt` persists session state in DataStore.
- `PortfolioScreen.kt` uses `PracticeScaffold`: Home, Projects, My Work, Site and Practice. My Work and Site currently render `EmptyDestination`.
- `PracticeScaffold.kt` implements a bottom navigation bar on narrow screens and a rail at 600 dp and above. Keep this investment.
- `ProjectWorkspaceScreen.kt` reaches drawings, snags, coordination and Siteman alongside brief, tasks, documents, fees and measurements. These are no longer merely dormant tables.
- `PortalServer.kt` serves a local browser interface backed by the phone database. This is a LAN companion, not cloud synchronization. It already has access codes, sessions, CSRF checks and expiration; it uses HTTP.
- `SitePane` explicitly defers photos. Snag evidence entry is text-based. Evidence references do not establish a managed attachment workflow.
- Proposals and client invoices are basic title/amount forms. Fee invoice payment buttons record the full amount. `SuiteRepository.recordReceipt` marks an invoice PAID after inserting any receipt, without deriving its balance. These two writes are not wrapped in a transaction in that method.
- `billStage` generates invoice numbers from a truncated timestamp and marks a stage BILLED when its invoice is still DRAFT. Both deserve explicit lifecycle rules.
- Monetary amounts use `Double`; financial calculations need a defined decimal/minor-unit representation and rounding policy.
- The README and older audit reference a React `src/` prototype, but that tree and its package manifest are absent from this checkout. Do not plan a web rebuild on the assumption they are available.

Primary files reviewed include the navigation scaffolds, portfolio and project screens, project forms, session store, repositories, measurement workflow, portal server, schema and test inventory. Existing documentation is historical context rather than proof of current behavior.

## Comparable products

### Monograph: connect architectural work to fees

Monograph connects practice workflows and emphasizes phases, budgets, time, invoicing and financial visibility. Its project setup supports templates and budgets. Sources: [Monograph method](https://monograph.com/monograph-method), [product overview](https://monograph.com/), [project setup](https://support.monograph.com/en/articles/11390695-creating-a-project).

**Lesson for ArchiMan:** make each project stage carry a deliverable, target date, agreed fee, completion state and billing state. Show agreed fees, issued invoices, receipts and outstanding balance together. Make stage completion and invoice issuance separate actions. Start with reusable residential and interiors templates, reviewed by working architects.

Example: completing schematic design offers a draft stage invoice with the agreed fee already filled in. A later receipt updates the outstanding balance without changing design progress.

Avoid importing enterprise staffing complexity before small practices demonstrate a need for it. Monograph is a practice-management benchmark, not an equivalent offline measurement app.

### Fieldwire: give field records a physical context

Fieldwire's mobile workflow includes plan viewing, markup, tasks and reports; tasks can be pinned to plans. Its mobile apps support offline editing followed by synchronization. Sources: [mobile guide](https://www.fieldwire.com/support/fieldwire-on-mobile/), [sync guide](https://help.fieldwire.com/hc/en-us/articles/360043052832-Field-Guide-to-Syncing).

**Lesson for ArchiMan:** start with reliable camera/gallery attachments, floor/room context, assignee, due date and before/after evidence. Generate a visit report from those records. Plan pinning becomes worthwhile after actual drawing attachments and revision handling work reliably.

Example: select a project and room, photograph a defect, assign a correction, attach the rectification photo and record the architect's verification. Preserve the existing mandatory verification-note rule.

Do not label a locally saved record as synchronized. If remote sync is later added, show pending, failed and completed transfer states explicitly.

### Joist: complete the commercial journey

Joist connects estimates, invoices and payments; its feature set includes reusable templates, documents, signatures and payment reminders. Sources: [features](https://www.joist.com/features/), [estimate-to-payment workflow](https://www.joist.com/estimate-to-payment/).

**Lesson for ArchiMan:** connect proposal, accepted scope, fee schedule, invoice and receipt. Carry project/client information forward. Produce previewable PDFs and share them through Android's share sheet. Begin with manual receipt recording; payment-provider integration can follow demonstrated demand and local availability.

Example: accept a proposal, create the fee schedule, issue an invoice and record a partial payment without retyping the client or fee. Preserve an issued document snapshot when practice details later change.

Joist is contractor-oriented; borrow its flow while keeping architectural fees distinct from contractor running bills.

## Prioritized improvements

| Priority | Improvement | Why it matters | Acceptance evidence |
| --- | --- | --- | --- |
| P0 | Full backup and safe restore | An offline app must have a credible phone-loss recovery path | Restore projects, relationships, documents and attachments to a clean install; reject corrupt packages; preview replacement and retain a recovery copy |
| P0 | Correct invoice/receipt lifecycle | Current receipt handling can misstate payment status | A 40 payment on a 100 invoice leaves 60 outstanding; repeated submission cannot duplicate receipts; writes are atomic |
| P0 | Reconcile docs with source | Current docs can drive incorrect design and implementation decisions | README and audit accurately describe reachable features, absent prototype, portal and backup limitations |
| P1 | Complete My Work and Site | Primary navigation currently leads to placeholders | My Work aggregates due tasks and reviews; Site provides project selection and quick visit, snag and measurement actions |
| P1 | Camera and managed attachments | Site work needs retrievable evidence | Capture offline, reopen after process restart, retain through backup/restore and include in reports |
| P1 | Professional document output | Saved text alone does not complete client communication | Preview, export and share proposal, invoice, receipt, letter and visit-report PDFs with editable practice identity |
| P1 | Project summary and next action | A project currently opens on Brief regardless of urgency | Show stage, next deadline, outstanding fees and open critical issues with direct actions |
| P2 | Faster measurement entry | This is an existing distinctive capability | Repeat previous row, retain contractor/item context, show units and deductions clearly, and flag invalid input rather than silently treating it as zero |
| P2 | Linked approvals and scope changes | Changes should remain traceable across work and fees | An accepted scope revision links to its affected deliverable and fee adjustment with history |
| P3 | Remote collaboration and plan markup | Valuable but dependent on identity, attachments and conflict rules | Pilot scoped access and concurrent-edit recovery before broad rollout |

For backup, use a versioned package, attachment manifest and integrity checks. If password encryption is offered, design and test recovery expectations explicitly. Demo package import and measurement CSV export are not substitutes for full backup.

For billing, use stable unique invoice numbers, explicit allowed transitions, transaction boundaries and remaining-balance calculations. Separate quantity-only M-Book records, contractor valuation and the architect's client fee invoices. Running bills should state whether quantities are draft, checked or approved.

## Suggested information architecture

Keep the existing adaptive navigation. Make Home answer what needs attention; My Work answer what I must do; Site answer what I can capture now; Practice hold reusable identity, people, templates and data controls.

Within a project, offer an overview first, then the existing Project, Site and Fees groups. Link related records directly: visit to snag, drawing revision to coordination item, measurement sheet to valuation, proposal to invoice. Reduce repeated data entry before adding destinations. Preserve system Back behavior and selected context when users return from a detail screen.

## Implementation sequence and validation

1. **Trust foundation:** billing correctness, complete backup/restore, documentation reconciliation and attachment storage design. Test partial payments, duplicate submissions, interrupted writes, corrupt backups and schema upgrades.
2. **Daily usefulness:** finish My Work/Site, add project summaries, camera evidence and shareable visit/invoice PDFs. Verify airplane-mode capture and process recreation on a device.
3. **Connected practice:** stage templates, proposal-to-invoice links, scope changes and measurement shortcuts. Validate with real project examples.
4. **Selective collaboration:** improve the LAN workflow or add remote sync according to observed use. The HTTP portal's code/session controls do not encrypt traffic; review transport and access design before expanding sensitive access.

These are dependency-ordered work packages, not calendar estimates. Backup, attachments and sync require design before effort can be estimated credibly.

Run a small usability pilot with about five architects using three tasks: create a project and proposal; record a visit and defect offline; issue an invoice and record a partial payment. Observe completion, repeated typing, errors and help needed. Track time to first useful document, visit-report completion, backup recovery success and weekly return use. Establish baselines before promising percentage improvements.

Defer AI estimation, BIM integrations, procurement expansion and a second full client until the core journeys are dependable. The strongest differentiation hypothesis is local ownership of practice data combined with field measurement and professional client output in one phone app.
