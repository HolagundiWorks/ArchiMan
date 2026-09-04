# ArchiMan — Architectural Consultancy Management App

## Product, UX, Architecture, and Delivery Specification

Status: Superseded in part by the approved contractor rate-book extension
Product boundary: Construction measurement book with contractor rate books and project-specific rate application
Explicitly out of scope: invoices, bills, retention, payments, and accounting
## 1. Product purpose

ArchiMan is an offline-first architectural consultancy and field application for managing projects and recording, reviewing, approving, and exporting construction measurements. Its Measurement Book replaces handwritten sheets while retaining the familiar project, contractor, work type, work item, floor, member description, dimensions, quantity, and unit structure.

The application is the authoritative record of measured quantities. It is not a billing or cost-management product.

## 2. Product principles

1. Measurement entry must be faster than paper.
2. A recorded quantity must always be reproducible from its inputs and formula version.
3. Historical records must never change when a work item or formula is edited later.
4. The application must remain fully usable without internet access.
5. Destructive actions must be reversible or protected by an explicit confirmation and backup.
6. Each business concept must have one canonical source of truth.
7. Field screens show only information required for the current task.
8. Rates are managed only through versioned contractor rate books. Billing, invoicing, retention, payment, and accounting remain out of scope.

### Standards catalog provenance

The bundled standards library uses Karnataka PWD Schedule of Rates for Buildings 2023-24, Volume 2 as the canonical work/specification catalog. Contractors import selected work items into separate versioned rate books and provide their own rates. Projects explicitly select the applicable contractor rate book. New measurements snapshot the selected rate and derived amount so later book changes do not rewrite history.

## 3. Users and roles

### Site engineer

- Selects project, contractor, work type, work item, and floor.
- Records member-level measurement rows.
- Adds optional supporting photographs.
- Saves drafts and submits completed sheets.

### Checker / quantity surveyor

- Reviews measurement sheets and formula results.
- Returns rows with comments or verifies them.
- Exports approved measurement books.

### Project administrator

- Creates projects, floors, contractors, work types, work items, and formula definitions.
- Imports standard work libraries.
- Merges duplicate master data.
- Manages backups, users, and permissions in a future synchronized edition.

## 4. Canonical workflow

### Unit-system entry

- The row editor provides a Metric/Imperial toggle after contractor, work item, and floor selection.
- Switching systems converts every active length, breadth, height, and formula-dependent deduction before recalculating quantities.
- Linear, area, and volume UOM snapshots are stored as `m`/`ft`, `m²`/`ft²`, and `m³`/`ft³`; count items remain unchanged.
- Draft recovery retains the selected unit system. Existing saved sheets are immutable snapshots and are never rewritten by an editor toggle.
- Imported linked measurements are converted from their stored UOM into the currently selected entry system.

```text
Projects
  -> Project overview
  -> Record measurement
     -> Contractor
     -> Work type
     -> Work item and formula
     -> Floor / location
     -> Measurement sheet
        -> Member description per row
        -> Required dimensions based on formula
        -> Optional photo per row
        -> Duplicate row / selected rows
     -> Review totals
     -> Save draft or submit
  -> Measurement Book
     -> Filter / review / edit / export
```

There will be one canonical measurement editor. Quick Entry, component entry, and project entry routes must preselect context and open the same editor rather than implement separate forms.

## 5. Navigation model

Recommended project bottom navigation:

```text
Overview | Measure | M-Book | Work List | More
```

- Overview: project status, recent sheets, floors, and shortcuts.
- Measure: resumes the active draft or starts the canonical selection flow.
- M-Book: searchable measurement sheets and rows.
- Work List: contractor type -> work type -> work item -> formula hierarchy.
- More: project setup, contractors, exports, backup, and settings.

Project switching belongs in the top bar, not in the bottom navigation.

## 6. Measurement sheet UX

### Persistent header

- Project
- Contractor
- Work type
- Work item
- Formula and UOM
- Floor / location
- Date and draft status

### Row structure

The visible columns are derived from the selected formula:

- Count: `Description | No | Quantity`
- Running length: `Description | No | Length | Quantity`
- Area: `Description | No | Length | Breadth | Quantity`
- Wall area: `Description | No | Length | Height | Deduction | Quantity`
- Volume: `Description | No | Length | Breadth | Height | Quantity`

Each row supports optional photo, duplicate, select, and delete actions. A photo is never required by default.

### Interaction rules

- System keyboard/numpad only.
- Keyboard Next advances to the next required cell.
- Invalid or incomplete values are marked inline.
- Draft rows are auto-saved locally.
- Removing a row offers Undo.
- Quantity is read-only and calculated by the central formula engine.
- A running sheet total is always visible.
- Save failure must not discard typed input.

## 7. Work catalog

Canonical hierarchy:

```text
ContractorType
  -> WorkType
     -> WorkItem
        -> FormulaDefinition
```

Example:

```text
Civil
  -> Masonry
     -> Brickwork 230 mm
        -> Area: No x Length x Height
```

Each work item requires:

- Stable code
- Display name
- Contractor type
- Work type
- UOM
- Formula definition and version
- Aliases for duplicate detection/import
- Active/archive status

No rate or price is stored against a contractor or work item.

## 8. Formula engine

Formula calculation must exist in one domain service and be reused by Android screens, imports, exports, and any future API.

```kotlin
interface QuantityFormula {
    val code: String
    val version: Int
    fun requiredFields(): Set<Dimension>
    fun calculate(input: MeasurementInput): QuantityResult
}
```

Initial formulas:

- `COUNT`: No
- `RUNNING_LENGTH`: No x Length
- `AREA`: No x Length x Breadth
- `WALL_AREA`: (No x Length x Height) - Deduction
- `VOLUME`: No x Length x Breadth x Height

The saved row records formula code, formula version, inputs, rounded quantity, and UOM. Quantity uses controlled decimal precision. Financial decimal logic is not required.

## 9. Target data model

### Master data

- `projects`
- `floors`
- `locations`
- `contractors`
- `contractor_types`
- `work_types`
- `work_items`
- `formula_definitions`
- `contractor_qualifications`
- `project_contractors`

### Transaction data

- `measurement_sheets`
- `measurement_rows`
- `attachments`
- `review_events`
- `audit_events`

### Required integrity rules

- Unique project-contractor assignment.
- Unique contractor-work-item qualification.
- Unique normalized work item code.
- Foreign keys for every relationship.
- Master records referenced by history are archived, not deleted.
- Measurement rows retain item/formula snapshots for historical reproducibility.

## 10. Duplicate-master-data policy

Duplicate detection uses normalized name, UOM, formula, work type, and aliases. A merge operation must:

1. Create a backup.
2. Select one canonical work item.
3. Repoint contractor qualifications and component references.
4. Preserve the previous names as aliases.
5. Keep historical measurement snapshots unchanged.
6. Archive redundant masters.
7. Record an audit event.

Direct deletion is forbidden when a work item is referenced.

## 11. Photo and attachment policy

- Copy selected images into application-managed storage.
- Persist metadata separately from the measurement row.
- Generate a compressed display version while retaining configurable original quality.
- Store checksum, MIME type, size, capture time, and row ID.
- Support offline access and later synchronization.
- Never depend solely on a temporary external content URI.

## 12. Offline, backup, and synchronization

### First release

- Room is the local source of truth.
- Exportable encrypted backup package.
- Tested, explicit schema migrations.
- No destructive migration fallback.

### Enterprise synchronization

- Append-only local operation queue.
- Server-generated organization-scoped identifiers or UUIDs.
- Retry using WorkManager.
- Conflict detection based on record version.
- Submitted/approved sheets are immutable; corrections create revisions.
- Remote wipe and device-session revocation.

## 13. Security and governance

- LAN access disabled by default.
- Authenticated, time-limited pairing if LAN access remains.
- Read-only LAN mode by default.
- No plain unauthenticated mutation endpoints.
- Role-based access for create, submit, check, approve, export, and administration.
- Audit user, device, timestamp, old value, and new value.
- Configurable backup and retention policy.
- Rate and amount data is exposed only from explicit project rate-book assignments and immutable measurement snapshots; billing and payment data remains excluded.

## 14. Target Android architecture

```text
app
core:model
core:database
core:formula
core:designsystem
core:backup
core:sync
feature:projects
feature:contractors
feature:workcatalog
feature:measurement
feature:mbook
feature:export
feature:settings
```

Use typed Navigation Compose, feature-specific ViewModels, repository interfaces, domain use cases, Room transactions, dependency injection, and immutable UI state. The current all-purpose ViewModel will be decomposed incrementally.

## 15. Quality gates

A release is not production-ready unless it has:

- Unit tests for every formula and rounding boundary.
- Room migration tests for every supported schema version.
- Repository transaction and duplicate-merge tests.
- End-to-end contractor-to-measurement workflow tests.
- Process-death and draft-restoration tests.
- Screenshot tests for small phones, standard phones, tablets, and large font sizes.
- Export reconciliation: exported totals equal database totals.
- Backup and restore verification.
- Accessibility checks and touch targets.
- Static analysis and release build verification in CI.

## 16. Definition of done for measurement and rate-book scope

- No bill, invoice, retention, payment, or accounting UI.
- No billing routes. The only LAN endpoints are the authenticated read-only portfolio portal and health check, enabled by explicit user action.
- New measurements snapshot the explicitly assigned contractor rate-book rate and derived amount.
- Measurement exports reconcile quantities, rates, and snapshot amounts.
- Contractor setup supports multiple named and versioned rate books sourced from the PWD master list.
- Work-item setup captures UOM and formula, not default rate.
- Existing databases migrate without losing measurement rows.
- Automated tests cover formulas and migrations.
