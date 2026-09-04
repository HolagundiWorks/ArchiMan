# ArchiMan Mobile UI/UX Audit — 360 dp Baseline

- Date updated: 4 September 2026
- Reference device: Samsung A10s, 720 × 1520 px (approximately 360 dp wide)
- Implementation baseline: ArchiMan schema 19

## Scope

The audit covers portfolio navigation, Directory, Work List, project Overview/Brief/Planning/More, Measurement Book and the measurement row editor. ArchiMan is now an Architectural Consultancy Management App with a field-first Measurement Book and contractor rate books; bills, invoices, payments and accounting remain outside scope.

## Resolved findings

| Area | Previous problem | Current resolution |
|---|---|---|
| Brand | Product surfaces used the older measurement-only identity | App label, mastheads, portal, exports and build identity now use ArchiMan and the full product descriptor |
| Portfolio navigation | Projects, clients, contractors and tools competed at the same level | Four stable destinations: Projects, Directory, Work List and More |
| Directory | Existing clients and contractors were difficult to discover | Clients and Contractors are explicit sections within one Directory |
| Project navigation | Project sections and field actions were duplicated | Four persistent actions: Project, Work, Record and M-Book |
| Project workspace | Too many permanent tabs and repeated shortcuts | Overview, Brief, Planning and More; secondary tools are grouped in labelled menus |
| Project overview | Duplicate Record/M-Book actions obscured project setup | Overview focuses on identity, brief status, planning counts and next project actions |
| Record launcher | Primary action floated inside excessive empty space | Compact, top-aligned contractor-first workflow |
| Measurement entry | Dense controls and a custom keypad reduced usable space | Horizontally scrollable spreadsheet-style rows with the system keyboard |
| Formula columns | Length, breadth and height appeared when not required | Columns are derived from the selected work-item formula/UOM |
| Row evidence | Description or evidence applied too broadly | Member description and optional photo belong to each row |
| Repeated work | Row recreation was slow | Duplicate-one-row and duplicate-selected-rows actions with Undo |
| Units | Metric/Imperial display and conversion were inconsistent | Atomic entry-system conversion with compatible UOM labels and recalculation |
| Review | Workflow controls displaced the measurement table | Compact, collapsible review panel; approved records remain protected |
| Design system | Carbon-style sharp shapes, custom basic fields and deprecated Compose APIs were inconsistent with Material UI | Central Material 3 theme, typography, shape scale, outlined fields, search, current dividers/dropdowns and auto-mirrored icons |

## Current navigation contract

```text
Portfolio: Projects | Directory | Work List | More
Project:   Project  | Work      | Record    | M-Book
Sections:  Overview | Brief     | Planning  | More
```

The project More screen contains Drawings, Site Reports, Project Team and Rate Books. Portfolio More contains Company Profile and Local Wi-Fi Portal. The obsolete portfolio Export Measurement Sheet screen is removed; M-Book owns measurement exports.

## Responsive rules

- 360 dp remains the minimum supported design viewport; primary labels must not wrap.
- Each screen has one dominant creation action and one clear back path.
- Long titles use one line with ellipsis when paired with actions.
- Secondary workflows stay collapsed or under More until selected.
- The measurement grid scrolls horizontally; unsafe compression is not used.
- System numeric/text keyboards are used; keyboard Next/Done follows required cells.
- Length, breadth and height remain conditional on the formula/UOM.
- Metric/Imperial conversion updates compatible fields and preserves measurement meaning.
- Row photo controls are optional and do not block saving a valid row.
- Touch targets must remain at least 48 dp and retain accessible labels.
- New UI must use semantic Material 3 theme roles instead of adding feature-specific colors or shapes.

## Remaining acceptance checks

- Complete hands-on field testing for high-row-count entry, duplication, photo capture, draft recovery and review transitions.
- Verify TalkBack traversal and row-action labels.
- Verify landscape/tablet layouts and font scaling at 1.3× and 1.5×.
- Validate permission denial, offline restart and attachment recovery on production-class devices.
- Add automated navigation and screenshot tests for the schema-18 hierarchy.
