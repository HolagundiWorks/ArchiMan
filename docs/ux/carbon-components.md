# Carbon components and patterns — 1.1.0

The Android app now uses native Compose implementations of Carbon patterns. The HTTPS portal bundles official Carbon Styles, Carbon Web Components and Carbon Charts. Carbon does not supply an official Android Compose component library; the native adapters preserve Android input, accessibility and navigation behavior while applying Carbon geometry, typography, icons and semantic colors.

## Delivered components

| Area | Android | HTTPS portal |
|---|---|---|
| Typography | Bundled IBM Plex Sans Regular, Medium and SemiBold; productive type scale | Same local font files; no font CDN |
| Icons | 54 mappings generated from official Carbon SVGs, including capture save, backspace and dropdown arrows | Official Carbon SVGs in shell, navigation, controls and table tools |
| Tiles | Flat square tiles, consistent borders, spacing and interactive states | Carbon tile/layer styling and responsive grid |
| Buttons | Square primary, secondary, ghost and destructive variants with 48 dp minimum touch height | Official Carbon button styles, sizing tokens, hover/focus/disabled states |
| Forms | Persistent labels, filled underline fields, square controls and scrollable project modal | Carbon input/select/checkbox styles while retaining browser validation and existing form serialization |
| Navigation | Flat tab bar/rail with blue edge indicator, selected semantics and labeled icons | Product shell, official Carbon breadcrumb elements and current-section navigation |
| Feedback | Typed inline success/error/warning/info notifications and loading state | Official Carbon inline notifications and tags |
| Tables | Existing mobile search/filter/structured-list flows | Search, ascending/descending numeric-aware sorting, row-count controls and pagination |
| Data visualization | Zero-based project-status bars and fee distribution, with exact-value legends | Official Carbon donut and bar charts, with separate exact-value lists |

## Real data and financial meaning

Practice dashboards read project status and existing invoice/receipt totals. Fee charts separate cash, TDS withheld and outstanding balances. Project dashboards use that project's invoices and receipts. Decimal values remain exact in the data labels; floating-point ratios are used only for drawing. Archived projects are explicitly included in practice-wide project mixes and issued-fee totals.

No demo financial values are inserted by this update. Empty data produces an empty state rather than an invented percentage. Negative distributions produce a review message while retaining exact balances. Color is accompanied by labels and numeric values. Chart tooltips are disabled in the portal; exact data remains available outside SVG graphics, and user-entered project status is normalized before chart rendering.

Table search and pagination affect the visible table, not the stored records or complete exports. Row form inputs and checkbox values remain attached to their original records when sorting. Destructive actions retain their existing confirmation and backend protections.

## Local assets and maintenance

Runtime assets live under `app/src/main/assets/carbon/`. The server exposes an exact public allowlist for the static bundle and fonts; dashboard data remains behind portal authentication. The CSP permits only same-origin scripts, styles and fonts. All libraries/fonts are bundled locally and require no runtime CDN.

Source and exact package versions are in `portal-ui/package.json` and its lockfile. Build with `npm ci --prefix portal-ui --ignore-scripts` and `npm run build --prefix portal-ui`. This also regenerates native icons and retains license/provenance files under `third_party/`. Browser checks run with `npm test --prefix portal-ui`; Windows uses the installed Edge browser, while CI installs Chromium. CI runs the bundle build, production dependency audit and browser checks before Android validation.

Sources: [Carbon components](https://www.carbondesignsystem.com/building-blocks/core/components/overview/components), [Carbon icon source](https://github.com/carbon-design-system/carbon/tree/main/packages/icons), [Carbon Charts](https://charts.carbondesignsystem.com/), [IBM Plex](https://github.com/IBM/plex).

## Verification

The 1.1.0 production build passed. All 81 Android unit tests passed; lint reported zero errors and 36 existing dependency/toolchain warnings. Both Edge browser tests passed, covering real Carbon components and charts under the portal CSP, fonts, form values, search, numeric sorting, pagination and keyboard controls.

The connected-phone regression suite passed using isolated data, including backup/restore, billing, GST/TDS, schedules, measurement conversions, portal access, malformed requests, concurrent writes and 10,000-row workloads. Comparison of all 56 existing practice tables found no changes. Native phone checks verified light/dark colors, theme persistence after restart, project and fee chart labels and an empty crash buffer. Temporary test packages were removed.

The final keyboard check found and fixed a new-project dialog inset issue. The rebuilt, installed version reserves keyboard/system-bar space and scrolls its form. On-phone checks passed for four labeled inputs, disabled submission with missing required fields, Cancel reachable above the keyboard, return to the dashboard and an empty crash buffer. No project was saved during testing. Production build, 81 unit tests and lint passed again after this fix. API 26/35 CI device jobs are configured but were not run locally.
