> Historical palette: superseded by [the Carbon colour system](carbon-colour-system.md).

# Dashboard and orange palette

Implemented 5 November 2026. This palette supersedes the earlier teal recommendation in the redesign plan.

## Palette

| Role | Color | Use |
| --- | --- | --- |
| Primary | Deep orange `#C2410C` | Primary buttons, dashboard hero, links and selected accents |
| White | Warm white `#FAFAF9` | Page backgrounds; pure `#FFFFFF` for light-theme cards and on-orange text |
| Black | Charcoal `#18181B` | Primary text and the fee-summary card |

Orange/white contrast is approximately 5.18:1; charcoal/warm-white is approximately 16.96:1, calculated using sRGB relative luminance. The first pair exceeds the 4.5:1 normal-text threshold in [WCAG contrast guidance](https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html). These pair checks are not a claim that every app screen has completed an accessibility audit.

Supporting surfaces use neutral grays and pale orange tints. Semantic warning/error colors remain distinct. Dark mode uses light orange `#FFB38A` against charcoal surfaces, with dark text on filled orange controls.

## Shared cards

`ArchiCard` provides 20 dp corners, a 1 dp neutral border, white/charcoal theme-aware surfaces and restrained elevation. Project cards, navigation/attention rows, dashboard statistics and the existing `GlassSurface` wrapper share this treatment. Orange icon containers distinguish navigation and workflow cards without coloring every card background.

## Practice dashboard

The main Dashboard replaces the old Home project-count list. It includes:

- Practice header and New project action.
- Current projects, open tasks, sheets awaiting review and open site issue counts.
- Outstanding and received fees, with a link to the invoice register.
- Pending measurement reviews, the next open tasks, and prioritized site issues, each labelled with its project.
- Recent current projects and an empty-project entry point.

Task/review/issue queues exclude archived projects. Tasks marked DONE/COMPLETED and closed issues are excluded. Measurement reviews include non-archived SUBMITTED/CHECKED sheets. Queue previews are bounded; summary counts are calculated separately and are not the preview length.

Fees include issued/paid invoices across all projects, including archived projects. Draft/void invoices are excluded. Balances use actual receipts, including reversals, rather than trusting a PAID label alone. Amounts have no invented currency symbol because the current data model does not specify currency.

Dashboard links open the relevant project section via the optional page argument on the project route. Existing project links still default to Overview. View invoices opens the practice invoice register; tapping Practice again returns to its menu.

## Verification

JVM checks cover draft/void exclusion, partial payment and reversal arithmetic, exact decimal settlement, and device-zone due-date labels. Isolated device checks verify dashboard totals/queues, completed-task exclusion, due-task ordering and archived-project exclusion. Existing backup, billing and record-operation checks remain in the same isolated test runner. Visual checks on the connected phone cover Dashboard cards, primary actions, scrolling and section links.
