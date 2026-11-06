# Company documents, item schedules and measurement workflow

## Portal

Open Practice > Portal on the phone, start it, then use the displayed Wi-Fi address and access code in a browser. The authenticated Admin workspace now edits company identity, letterhead, client/contractor profiles, the general item catalogue, estimates, project contractor schedules, measurement imports and running bills. All data remains on the phone. Forms enforce the existing access code/session, host/origin and CSRF checks. PNG/JPEG logos are decoded and size limited; user-entered content is escaped.

## Two-level item structure

1. General catalogue: source edition, source item number, item code, descriptive name, full specification, trade, calculation formula and unit. It supplies planned estimates. CPWD DSR 2023 starters are adapted short identification descriptions, not a complete or certified CPWD catalogue. The portal also previews/imports full descriptions from CSV; up to 2,000 rows, existing codes skipped. Review specifications and current correction slips against the official source before contracting.
2. Project contractor schedule: one active revision number for the entire project, containing each contractor's assignments, detailed contract description, rate unit, negotiated tax-exclusive rate, and labour-only / labour-with-material basis. Draft revisions copy the active revision and can add/remove lines. Activation supersedes the previous revision. Active and superseded schedules are immutable.

Measurements show only the chosen contractor's active assigned items. Each new sheet and entry stores its project revision, assigned-line key, unit rate and contract basis. A stale entry form is rejected after revision activation. Old entries keep their original rates, descriptions and revisions. Legacy data is preserved without invented rates; it cannot silently become a priced running bill.

## Recording and imports

The mobile screen follows the supplied compact reference: contractor/floor/item selection, description, a dimension row with numeric keyboard, a live quantity card and Save entry. Metric/imperial switching converts entered lengths and wall-opening deductions. Stored quantities always use the assigned rate unit, avoiding metre/foot rate mismatches. Nos × L × B × H gives volume; Nos × L × H minus deduction gives wall area; Nos × L × B gives floor area; running length and count are also available.

The portal provides the same formulas and CSV/TSV measurement import with input unit selection, preview and confirmation. Saved units follow the contractor assignment. Imports accept ItemCode, Description, Nos, Length, Breadth, Height, Deduction, Location, Remarks; use LineKey to disambiguate multiple assignments with the same code. One floor and contractor per import. Limit: 1 MB / 1,000 rows. Invalid rows reject the whole import; repeat imports are fingerprinted and skipped. Genuine identical repeated work needs manual review.

Sheets are separated by unit, formula, project revision and assigned line. Submitted/checked/approved sheets do not receive new entries. Draft entries follow the existing review/approval workflow.

## Bills and taxes

Contractor running bills select approved, not-already-issued measurements and use stored quantities/rates. Drafts need review before issue; issue locks a snapshot and checks for duplicate billing. Drafts can be deleted; issued bills can be voided with a reason, retaining history and releasing quantities for correction. Issued bills never calculate or add taxes. Client consultancy invoices retain their separate GST/TDS workflow.

## Profiles and letterhead

Company: legal/display name, entity type, full address, contacts, website, PAN/GSTIN, CoA/practice registrations, principal/qualification, logo, accent, logo-left/right layout, tagline and footer. The footer can carry payment/bank information. Preview and PDF download use saved settings. Existing company GST mode remains controlled by Taxes & banking; registered GSTIN changes must match the supplier state.

Client: individual/company type, legal name, contact person, legal/billing and correspondence addresses, phone/email, billing contact, authorised signatory, optional PAN/GSTIN/TAN, entity registration, payment terms, communication preference and TDS certificate arrangements.

Contractor: project, trade, legal/business name, authorised contact, primary/alternate phone, email/address, optional PAN/GSTIN/registration/MSME references, bank beneficiary/account/IFSC/branch, payment/retention terms and insurance/labour-compliance references. The contractor profile shows assigned items from the current project revision. Tax identifiers are compliance records, not contractor-bill tax calculations.

## Exports

Portal downloads PDF, genuine Excel XLSX and CSV for consultancy proposals, agreements, invoices, letters, measurement books, draft estimates and contractor bills. PDF uses company letterhead/logo on each page. XLSX/CSV retain complete text or structured columns, metadata, units, revision/rate snapshots and status. CSV neutralizes spreadsheet formulas; XLSX uses typed values/inline strings rather than formulas. Native consultancy PDF sharing also uses the saved company design.

## Sources and preservation

Official CPWD civil DSR 2023 volumes were downloaded and checked on 5 November 2026:
- https://cpwd.gov.in/WriteReadData/Publication/DSR_Vol_1_Civil.pdf
- https://cpwd.gov.in/WriteReadData/Publication/DSR_Vol_2_Civil.pdf

The volume I preface states that the effect of works-contract GST is incorporated in published item rates. The app therefore does not copy these as tax-exclusive contractor rates; rates must be explicitly negotiated/entered on the required basis.

Room 30→31 adds optional profile details and measurement schedule/rate snapshots. Schema 28→31 migrations preserve records. Known schema-28, 29 and 30 backups upgrade on restore. Schedules, estimates, letterhead/logo and issued bill snapshots are included in normal backups. Projects/contractors with schedule or bill history retain that history through archive instead of deletion.

## Demo contractors

The built-in fictional company includes eight contractors across the villa and pavilion projects, with fourteen assigned civil, finishing, flooring and electrical works. Each project starts with an active R001 schedule. Rates are illustrative, tax-exclusive examples, not quotations. Existing matching demo projects receive an idempotent additive upgrade at startup; other practices and projects are not seeded. Existing active schedule lines are retained when adding missing demo works; unfinished draft revisions are left for review.
