# Mobile UI/UX Audit — 360 dp Baseline

Date: 2026-08-23  
Reference device: Samsung A10s, 720 × 1520 px (approximately 360 dp wide)

## Scope

The audit covered the complete primary workflow: Home, Clients, Contractors, Project Hub, Work List and Formulas, Measurement Book, and the measurement row editor. The product remains a pure measurement book: rates, billing, BOQ valuation, and other commercial concepts are outside scope.

## Resolved findings

| Area | Finding | Resolution |
|---|---|---|
| Global navigation | `Project Hub` wrapped in the bottom bar and increased its height | Shortened the destination label to `Hub`; the full context remains visible inside the screen |
| Home | Long application title clipped on a 360 dp viewport | Changed the masthead to `Measurement Book` and constrained title/subtitle with ellipsis |
| Work List | Oversized masthead and a clipped duplicate Add Item control | Constrained header text and removed the redundant top action; the FAB is the single creation action |
| Project Hub | Long tab names wrapped and consumed vertical space | Replaced them with concise `Overview`, `Contractors`, `M-Book`, and `Record` labels |
| Record launcher | Primary action floated inside excessive empty space | Top-aligned and compacted the launcher, icon, copy, and button |
| Contractors | FAB displayed two plus signs | Retained the icon and changed the text to `Add Contractor` |
| Clients/Contractors | Duplicate top creation buttons competed with FABs | Removed redundant top actions and shortened search prompts |
| Measurement Book | Review workflow was always expanded ahead of measurement content | Made the workflow a compact, collapsible panel, collapsed by default |
| Unit display | Imperial records could render dimensions with a hard-coded `m` suffix | Dimension labels now derive from the record UOM and show `ft` for Imperial records |
| Measurement editor | Dense columns could collide at phone width | Retained horizontal table scrolling and introduced a responsive two-line editor header |

## Responsive UI rules

- 360 dp is the minimum supported design viewport; no primary title or bottom-navigation label may require wrapping.
- Header text takes the remaining row width and uses one-line ellipsis when paired with actions.
- Creation uses one dominant control per screen. A FAB is not duplicated by a masthead button.
- Secondary workflows are collapsed by default when they would push the user's main content below the fold.
- The measurement grid deliberately scrolls horizontally. Compressing Item, No., Length, Breadth, Height, description, photo, and row actions into 360 dp would make data entry and touch targets unsafe.
- System numeric/text keyboards are used. No custom keypad obscures the table.
- Length, breadth, and height remain conditional on the selected work item's UOM/formula rather than appearing active for every item.
- Metric/Imperial conversion updates dimensions and compatible UOMs while preserving calculated measurement meaning.

## Remaining acceptance checks

- Conduct field testing with a gloved user and representative high-row-count sheets.
- Verify TalkBack traversal order and labels on every row action.
- Verify landscape/tablet behavior and font scaling at 1.3× and 1.5×.
- Validate photo capture, permission denial, offline restart, and draft recovery on production-class devices.

