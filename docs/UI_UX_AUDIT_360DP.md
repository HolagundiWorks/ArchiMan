# ArchiMan Mobile UI/UX Audit — 360 dp Baseline

- Date updated: 11 September 2026
- Reference device: Samsung A10s, 720 × 1520 px (approximately 360 dp wide)
- Implementation baseline: ArchiMan schema 24

## Scope

The audit covers portfolio navigation, Directory, Work List, project Overview/Brief/Planning/More, Measurement Book and the measurement row editor. ArchiMan is a quantity-only field Measurement Book; rates, valuation, bills, invoices, payments and accounting remain outside scope.

## Resolved findings

| Area | Previous problem | Current resolution |
|---|---|---|
| Brand | Product surfaces used the older measurement-only identity | App label, mastheads, portal, exports and build identity now use ArchiMan and the full product descriptor |
| Portfolio navigation | Projects, clients, contractors and tools competed at the same level | Four stable destinations: Projects, Contacts, Library and Practice |
| Directory | Existing clients and contractors were difficult to discover | Clients and Contractors are explicit sections within one Directory |
| Project navigation | Project sections and field actions were duplicated | Five persistent actions: Project, Work, Record, M-Book and More |
| Project workspace | A top tab row competed with bottom navigation | The top tabs are removed; brief, planning and specialist pages use one More hierarchy and one back path |
| Project header | Three metric blocks plus actions crowded 360 dp screens | One compact status line and one project-switch action; printing remains in M-Book |
| New project | Project identity, client onboarding, contractor assignment and floor setup were mixed into one form | Creation now captures project name, code, client, type and site only; a Ground Floor baseline is created silently, while additional levels and contractor assignments are handled inside the project |
| Project overview | Duplicate Record/M-Book actions obscured project setup | Overview focuses on identity, brief status, planning counts and next project actions |
| Record launcher | Primary action floated inside excessive empty space | Compact, top-aligned contractor-first workflow |
| Measurement entry | Dense controls and a custom keypad reduced usable space | Horizontally scrollable spreadsheet-style rows with the system keyboard |
| Formula columns | Length, breadth and height appeared when not required | Columns are derived from the selected work-item formula/UOM |
| Row evidence | Description or evidence applied too broadly | Member description and optional photo belong to each row |
| Repeated work | Row recreation was slow | Duplicate-one-row and duplicate-selected-rows actions with Undo |
| Units | Metric/Imperial display and conversion were inconsistent | Atomic entry-system conversion with compatible UOM labels and recalculation |
| Review | Workflow controls displaced the measurement table | Compact, collapsible review panel; approved records remain protected |
| Design system | Carbon-style tokens, sharp custom shapes, custom basic fields and platform dialog shells competed with Material UI | Pure Material 3 theme and components: dynamic color, standard typography and shapes, outlined fields, app bars, list items, dialogs, bottom sheets and navigation bars |

## Current navigation contract

```text
Portfolio: Projects | Contacts | Library | Practice
Project:   Project  | Work     | Record  | M-Book | More
More:      Brief → Planning → Controls → Documents/Site → Project Team
```

The project More screen contains Brief, Planning, Onboarding & Controls, Drawings, Site Reports and Project Team. Practice contains Company Profile/Connections and the Local Wi-Fi Workspace. The obsolete portfolio Export Measurement Sheet screen is removed; M-Book owns quantity-only measurement exports.

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

## Material 3 component contract

- `MaterialTheme` is the only source of colors, typography and shapes. ArchiMan does not maintain a parallel design-token layer.
- Primary screens use `Scaffold` with `TopAppBar` and the appropriate Material 3 bottom navigation.
- Search and data entry use `OutlinedTextField`; system keyboards and standard focus behavior are retained.
- Directory and tool menus use `ListItem` and `HorizontalDivider` instead of hand-built rows.
- Confirmations use `AlertDialog`; complex modal content uses `BasicAlertDialog`; contextual actions use `ModalBottomSheet`.
- Component defaults control elevation, states, indicator colors, label typography and touch targets unless a semantic status such as error requires an override.

## Remaining acceptance checks

- Complete hands-on field testing for high-row-count entry, duplication, photo capture, draft recovery and review transitions.
- Verify TalkBack traversal and row-action labels.
- Verify landscape/tablet layouts and font scaling at 1.3× and 1.5×.
- Validate permission denial, offline restart and attachment recovery on production-class devices.
- Add automated navigation and screenshot tests for the schema-24 hierarchy.
