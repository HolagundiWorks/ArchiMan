# Carbon colour system — version 1.0.2

The Android app and local HTTPS portal use IBM Carbon's Gray 10 light and Gray 100 dark colour families, adapted to the existing Compose/HTML components. This is a colour-system implementation, not a claim that Material components have been replaced by IBM Carbon components.

| Role | Light | Dark |
|---|---|---|
| Background | Gray 10 `#F4F4F4` | Gray 100 `#161616` |
| Card/layer | White `#FFFFFF` | Gray 90 `#262626` |
| Primary text | Gray 100 `#161616` | Gray 10 `#F4F4F4` |
| Secondary text | Gray 70 `#525252` | Gray 30 `#C6C6C6` |
| Highlight/link | Blue 60 `#0F62FE` | Blue 40 `#78A9FF` |
| Selection container | Blue 20 `#D0E2FF` | Blue 70 `#0043CE` |
| Error text | Red 60 `#DA1E28` | Red 40 `#FF8389` |
| Success text | Green 60 `#198038` | Green 40 `#42BE65` |
| Warning indicator | Yellow 30 `#F1C21B` | Yellow 30 `#F1C21B` |
| Information text | Blue 70 `#0043CE` | Blue 40 `#78A9FF` |

Yellow warning fills use dark text in light mode; pale yellow is not used as small text on white. Web primary buttons retain Blue 60 with white text in both modes. Compose maps its primary role to accessible Blue 40 in dark mode so text buttons, selected icons and controls retain contrast. Error, success, warning and information each have separate readable background/foreground combinations. Status labels remain visible so meaning does not depend on colour alone.

**Appearance:** choose System, Light or Dark at the top of the native Practice menu. The choice persists on that device, outside business records. The portal has the same choices in a theme selector on both sign-in and workspace pages; its preference is local to that browser. System mode follows OS appearance changes.

Focus, hover, active, selected and disabled web states use semantic CSS variables. The project/login pages and workspace share one palette. Print previews remain white with dark text regardless of application theme. New letterhead and PDF defaults use Blue 60; an explicitly saved practice letterhead accent remains its existing custom value.

Measurement states: approved = success green; checked = warning yellow; submitted = information blue; returned = error red. Destructive confirmation actions use error styling. Alerts retain text and their existing confirmation rules.

Validation includes WCAG AA text-contrast checks on light/dark semantic pairs, JavaScript syntax validation, Android compilation/lint and device checks for public HTTPS theme assets. Native theme switching and screenshots are checked on the connected phone before delivery.

Sources: [Carbon colour tokens](https://www.carbondesignsystem.com/building-blocks/foundations/color/tokens), [Carbon theme guidance](https://www.carbondesignsystem.com/building-blocks/foundations/themes/overview), [Carbon colour guidelines](https://www.carbondesignsystem.com/building-blocks/foundations/color/guidelines).

## Verification

76 local tests pass, including AA text contrast for both palettes and alert pairs. Android lint reports zero errors (36 existing dependency/toolchain notices). The isolated phone suite passed, including unauthenticated HTTPS theme-asset delivery and the existing data/document workflows. On the installed build, screenshot pixels confirm Gray 10 `(244,244,244)` in Light mode and Gray 100 `(22,22,22)` in Dark mode; Dark survives process restart. System mode was restored after testing. All 56 practice tables remain unchanged.
