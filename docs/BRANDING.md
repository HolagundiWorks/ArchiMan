# MynaHub and MynaHub Mobile brand guide

Updated 2026-11-16. This guide records the shared identity and platform differences.
MynaHub's suite artwork is maintained in the sibling MynaUI repository. Historical
suite naming and app boundaries are documented in MynaHub's docs/SUITE.md.

## Names and identity

| Surface | Name |
|---|---|
| Windows office/company application | MynaHub |
| Android companion | MynaHub Mobile |
| Isolated Android debug build | MynaHub Mobile API Dev |
| Company-owned backend | Myna API |
| Product family | Myna |

Use these spellings in UI and current documentation. ArchiMan, O-Studio,
OSAS, Adms and com.holagundi.archiman remain compatibility identifiers where
renaming would affect installed apps, namespaces, file formats or signing.
Do not imply a package-ID migration. Release Android ID is
com.holagundi.archiman; debug adds .apidev.

Both Hub products use **Ember #B83E16**. Midnight #102C80 belongs to MynaCAD,
Dusk #5630A0 to MynaOps and Grove #166040 to Myna3D. Other suite products
are references here, not a request to restyle their implementations.

## Marks

Use the existing white three-circle Myna mark on the Ember app tile. Keep its
geometry and clear space (one quarter of tile width); do not add a second logo,
gradient, shadow or a new mascot. MynaUI/brand/studio is the artwork source.
Existing launcher artwork is historical. The four-colour orb belongs to the AI
agent in CAD/Ops; neither Hub product gets an AI assistant or launcher from this work.

## Platform implementation

| Platform | Implemented source and treatment |
|---|---|
| Desktop | native/Adms.Desktop/App.xaml loads O.Ui's SuiteAccent.Orange; artwork in native/Adms.Desktop/Assets |
| Android | ui/theme/ArchiTheme.kt uses Ember light primary #B83E16 and dark primary #E0703A |
| Android type | Bundled IBM Plex Sans through CarbonTypography.kt; Carbon controls and spacing |
| Android neutral surfaces | Light #F4F4F4 with #161616 text; dark #161616 with #F4F4F4 text; white/gray tile levels |

Desktop suite typography policy uses Apfel Grotezk for names, Mazius Display
for display text and the Windows body font through O.Ui. Do not describe that
policy as the Android implementation. Mobile uses its bundled Carbon typography.
Desktop pure white/black policy is distinct from Mobile's implemented Carbon
gray surfaces; this documentation update does not redesign either platform.

Use Ember for actions, focus and selection. Reserve semantic support colours
for success, warning, error and information; they do not change the app identity.
Honor System/Light/Dark preferences, maintain readable contrast and preserve
existing accessible control behavior. A full accessibility audit was not rerun
for this documentation update.

## Product language

The current Mobile destination is **Practice > Live company sync**. Use
**Company connection**, **View company tasks** and **Edit company project**.
Use **New company project** for creation and **Delete company project** for
removal. The confirmation names the project and requires typing DELETE; describe
the removal from live views and retention of records/audit history accurately.
Project/task writes require an active connection and permissions. Read-only accounts see task records without write controls.
Do not promise offline company access, backup/restore, saved drafts or automatic
offline replay. Existing local practice modules are explicitly separate.

Development builds and demo companies must remain clearly labeled. No production
deployment, complete desktop integration or release-store rollout is implied.

The canonical Hub guide is in HolagundiWorks/MynaHub, docs/BRANDING.md. Mobile source paths above are relative to app/src/main/java/com/holagundi/archiman.
