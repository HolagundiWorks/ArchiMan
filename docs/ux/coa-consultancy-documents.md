# CoA-informed consultancy proposals and agreements

Source review and template version: 5 November 2026; `COA-ADAPTED-2026-11-05-v1`.

## Official sources

- [Comprehensive architectural services: engagement conditions](https://coa.gov.in/index1.php?lang=1&level=2&lid=81&sublinkid=294).
- [Interior architecture: engagement conditions](https://coa.gov.in/index1.php?lang=1&level=2&lid=82&sublinkid=295).
- [Landscape architecture: engagement conditions](https://coa.gov.in/index1.php?lang=1&level=2&lid=83&sublinkid=296).
- [CoA scale of charges](https://coa.gov.in/index1.php?lang=1&level=2&lid=86&sublinkid=299).

The portal distinguishes assigned services, stage payments, client inputs and site supervision. Its stage percentages are cumulative. Architecture separates approval submission and approval milestones; the interior and landscape schedules combine that stage. The scale varies by service/project category and also addresses retainers and ancillary charges.

ArchiMan supplies adapted drafting aids, not an official CoA form or a finding that an entered fee complies with the published scale. No market rate, tax rate, retainer or square-foot tariff is silently prescribed. Older tax terminology on the reference pages is not copied into a fixed tax calculation. Project-specific commercial and legal particulars must be reviewed before signing.

## In the app

Open a project → Fees → Fee proposals or Agreements → New document.

1. Select Architectural, Interior, Landscape or Miscellaneous work.
2. Check the populated parties, project address, architect and CoA registration.
3. Choose lump sum, INR per square foot, or percentage of assigned works cost. Enter the agreed amount/rate and quantity. Square-foot fees require an explicit chargeable-area definition.
4. Adapt scope, exclusions, terms and cumulative milestones. Resolve every bracketed entry before issue. Miscellaneous work starts with blank scope/terms and uses the user's own wording.
5. Preview and save a draft. Share a paginated PDF from document detail.
6. Review and issue for acceptance. Issue does not record signatures or client acceptance. Issued text is locked; create a draft revision for amendments.

Use the proposal's Document actions menu → Create agreement from proposal to carry the same scope, exclusions, fee basis, terms and milestone schedule into a new draft agreement. Changing a service template requires confirmation before replacing its clauses. Changing fee basis clears numerical inputs after confirmation, preventing a percentage or lump-sum amount being reused as a square-foot rate.

## Stored record and calculations

Each new document saves its full structured input, source/version metadata, party snapshot and rendered text. PDF export uses the saved text. Subsequent edits to a practice profile or template do not rewrite an issued document. Existing contractor contracts and older proposals remain accessible as legacy records; they are not automatically reclassified as client–architect agreements.

Fee arithmetic uses decimal strings and rounds the base fee to two decimals. Cumulative milestone targets are displayed with the increment from the previous target; they are not added together as separate percentages. A retainer is identified as an advance, and actual invoices must account for prior payments. The feature does not automatically issue invoices or charge tax/reimbursements.

Drafts may retain unresolved wording, but issue requires completed parties, scope and terms, increasing milestones ending at 100%, resolved placeholders and an explicit review acknowledgement. Save/issue checks reject stale document versions. Signature spaces remain blank until the parties execute the document outside this workflow.

## Data and validation

Room 28→29 adds default-empty structured input/rendered text columns to proposals and contracts without replacing existing rows. Schema-28 demo packages remain importable. Known schema-28 backups are upgraded during restore after integrity/schema checks, with an upgrade notice in the preview. New backups include complete consultancy documents.

JVM checks cover the three fee bases, decimal calculations, service-specific defaults, manual miscellaneous terms, placeholders, schedules and codec round-trip. Isolated device checks cover copy-to-agreement, stale saves, issue acknowledgement, immutable issued documents, PDF pagination, backup round-trip, legacy-backup upgrade and a real SQLite/Room 28→29→30 preserving migration.
