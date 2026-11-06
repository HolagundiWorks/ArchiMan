# Standard record operations audit

Reviewed and implemented: 5 November 2026. Scope is the Kotlin Android product, with an emphasis on reachable record screens. The LAN portal remains a read-only companion.

The original gap was systematic: most lists could create and display records but offered no correction or removal controls. A shared record editor now supplies validated edits, Save/Cancel, confirmed deletion, stale-record checks and local operation logging. Lifecycle transitions remain separate from free-text editing. No database schema change was needed.

| Surface | Added operations | Protection / existing behavior |
| --- | --- | --- |
| Projects, Home and project workspace | Edit details, select another existing client, archive/reopen, permanently delete | Project deletion requires typing DELETE and names the linked data it removes. Shared clients and item library remain. Project list adds search and Active/Archived/All filters. |
| Clients | Edit contact/profile details, delete unused clients | Linked clients cannot be deleted. Renaming updates project client labels. |
| Practice profile | Edit practice identity and letterhead fields | Profile deletion is intentionally unavailable. |
| Tasks | Edit title/description, confirmed delete | Existing To do/Doing/Done and Reopen actions remain. |
| Letters, fee proposals, contracts | Edit/delete drafts | Issued/accepted records retain history. |
| Fee invoices, both project and practice lists | Edit/delete drafts, void issued invoices with a reason | Receipts must be reversed before voiding. Posted documents are not silently rewritten. |
| Receipts in project invoices | Reverse with a reason | Inserts a compensating receipt, keeps the original and recalculates invoice status. Duplicate reversals are rejected. |
| Site visits | Edit inspection details, confirmed delete | Does not claim photo capture is implemented. |
| Levels and rooms | Rename, confirmed delete | Deletion blocked while dependent rooms, components or measurements exist. |
| Contractors shown under Contracts | Edit directory details, delete unused entries | Referenced contractors cannot be deleted. Historical measurement snapshots stay unchanged. |
| Measurement sheets | Confirmed delete of draft/returned sheets | Deletes entries and review comments with the sheet. Submitted/checked/approved sheets are protected. |
| Measurement entries | Edit dimensions/text and recalculate quantity; confirmed delete | Only draft/returned entries can change. Removed the old immediate-delete button. |
| Running bill | Edit/delete rate-book entries | Bill totals remain derived from measurements and rates. Deleting all rates no longer causes them to reappear at startup. |
| Daily site logs | Edit details, confirmed delete | Updates timestamp. |
| Material orders and lines | Edit/delete before receipt | Received orders and their lines retain history. |
| Snags | Edit open details; delete unprocessed open issues | Closed verification is protected. Workflow events prevent erasing processed issues. Existing status transitions remain. |
| Coordination | Edit early-stage details; delete unprocessed drafts/received items | Reviewed records and workflow history are protected. |
| Drawings | Edit register metadata; delete drawings without revisions | A drawing with revision history cannot be deleted individually. |
| Drawing revisions | Edit/delete drafts | Issued revisions are corrected by adding a new revision. |
| Transmittals | Explicit retained-history explanation | Issued transmittals are corrected through a new transmittal. |

Shared controls were also added to the existing meetings, approvals and schedule panes. Those panes are not currently linked from project navigation; this change does not claim they became reachable. The time/enquiry model definitions have editing rules in the shared allowlist but their legacy lists are not part of the current product navigation.

## Integrity rules

- All mutations re-read the record inside a Room transaction. Saving a stale editor is rejected instead of overwriting a newer change.
- Only explicitly listed form fields can be updated. Status, review signoff, quantity totals and most relationships cannot be freely typed. Client reassignment uses existing client choices and validates the chosen ID again when saving.
- Required text, finite/non-negative numeric values, positive fees and duplicate project codes/level names/room names/drawing numbers are checked.
- Project deletion explicitly handles older project-owned tables without foreign keys, including letters and contractors. Child-only tables follow their foreign-key cascades. A project-owned contractor used by another project is retained as a shared contractor.
- Unused clients/library items and other projects are retained. Local operation-log entries are retained after project deletion. Attachment files are not physically purged because other records or recovery packages may still refer to them.
- Permanent deletion has no Trash/Undo feature. Export a backup before removing important records. Archive is the reversible option for projects.

## Validation

`RecordDeviceChecks` runs against the existing isolated device-test database, never the user's practice. It checks every editor's field names against the actual schema, project edits and archive/reopen, stale editors, client linkage, task edits/deletion, level dependencies, draft invoice protections, receipt reversal and voiding, measurement review locks, populated-project cleanup, preservation of other projects/shared data, and rate-deletion persistence.

The existing archive-integrity, backup/restore, recovery, rollback and billing checks remain in the device runner. Local JVM tests and debug/test APK assembly are also required before delivery. UI checks on the connected phone use edit/delete dialogs without confirming removal of existing user records.
