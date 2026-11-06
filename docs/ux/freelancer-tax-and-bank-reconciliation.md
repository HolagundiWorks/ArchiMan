# Freelance practice: admin, GST and banking

Implemented 5 November 2026. Practice > Taxes & banking configures registration and imports statements. Project > Fees > Invoices creates tax-aware drafts and records cash/TDS separately.

## Single admin

The phone opens one local Admin workspace. Existing role rows are retained inactive for historical references; the current owner becomes Admin. No role picker or staff profiles are required. This is local workspace access, not password authentication. Wi-Fi portal access still requires the phone's access code and uses the Admin identity. Practice identity/letterhead remains necessary for documents.

## GST behavior

- No registration: no GST collected.
- Composition: bill of supply, no GST added to the client charge. Enrollment and eligibility remain the owner's responsibility; the app does not calculate/file composition levy.
- Regular: editable default 18%; same supplier/place state splits CGST/SGST, differing states use IGST. Decimal rounding preserves gross totals. Supplier GSTIN/state, service SAC and place of supply are explicit. GSTIN validation checks format only.
- Each new invoice stores its tax calculation and registration snapshot. Updating settings does not rewrite old invoices. Existing unclassified drafts must be recreated with tax details if a registered practice tries to issue them. Correct a tax snapshot by deleting the draft and preparing a replacement; issued records retain history.
- Proposals and consultancy agreements also store GST treatment, tax amounts and total. Milestone percentages apply to the base professional fee.

The first implementation covers domestic service-fee calculations; it does not file GST returns, generate IRNs/e-invoices, calculate input tax credits, support exports/SEZ/reverse charge, or certify a statutory invoice. Composition liability is distinct from GST collected from clients.

## Reconciliation

Receipts retain cash amount, separate actual TDS, deduction reference and verification status. Cash + TDS settles the invoice; dashboard cash received excludes TDS. A company client may withhold TDS, but the app never automatically assumes liability/rate based only on client type. Mark verified only after checking the client's certificate and your tax credit records. Receipt reversal negates both components, reopens the invoice and returns a bank match to review. Matching is transactional and rejects repeat posting or overpayment.

## Statement import

Use CSV/TSV, UTF-8, maximum 5 MB and 10,000 rows. Date plus signed Amount, or separate Credit/Debit, is required. Quoted commas/newlines and common header aliases are supported. Default dates DD/MM/YYYY; explicit MM/DD/YYYY option. Bank reference/narration and stable account alias drive duplicate detection. Review the preview before import, then select one issued invoice per bank credit. Enter actual TDS during matching. Credits are never posted automatically; debits can be ignored/reopened. Imported bank date becomes receipt date in Asia/Kolkata.

PDF/XLS/XLSX statements and split allocations across invoices are not supported. Export CSV from the bank. Without reliable unique references, overlapping exports containing identical transactions require manual review. Projects with bank reconciliation history must be archived to retain the ledger.

## Sources checked

- [CBIC service rates, heading 9983](https://cbic-gst.gov.in/hindi/gst-goods-services-rates.html): ordinary professional services entry lists 9% CGST + 9% SGST / 18% IGST; exact service and exceptions must be confirmed.
- [Government composition handbook, February 2026](https://tgct.gov.in/tgportal/GST/GstHandbook/Composition_GST_%28Feb%2026%29.pdf): composition suppliers cannot collect tax from recipients; service composition levy is separate.
- [CBIC sectoral FAQs](https://cbic-gst.gov.in/sectoral-faq.html): composition suppliers issue bills of supply, not tax invoices.
- [IGST Act section 12](https://taxinformation.cbic.gov.in/content-page/explore-act/1000620/1000001/hotel/ACTS): property-related architectural/interior services require property-location place-of-supply analysis.
- [Income Tax Department TDS compliance FAQs](https://www.incometax.gov.in/iec/foportal/help/all-topics/e-filing-services/%20tds%20compliance-faq): section 393 under the 2025 Act applies from April 2026 subject to transition rules. No hardcoded old section 194J or automatic threshold is used.

## Data and validation

Room 29→30 adds invoice tax snapshots, TDS/bank references on receipts and finance records. Room 28→29→30 preserves existing data. Known backups from schemas 28 and 29 are upgraded; current backups include GST settings, bank rows, deductions and document snapshots. Tests cover decimal GST, CSV parsing/date order, cash/TDS balances, duplicate rejection, atomic matching, reversal/rematching, single Admin, backup round-trip and actual Room migration using isolated data.
