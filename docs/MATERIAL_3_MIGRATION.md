# ArchiMan Material 3 Migration

- Migration date: 4 September 2026
- Application baseline: Room schema 21
- UI technology: Jetpack Compose Material 3

## Outcome

ArchiMan now uses a centralized Material 3 design foundation while preserving its existing portfolio, project, measurement and local-first workflows. The migration changes visual and interaction primitives; it does not change stored business data or navigation ownership.

## Delivered foundation

- Renamed the root theme to `ArchiManTheme`.
- Replaced the previous Carbon-shaped theme with Material 3 shape roles:
  - extra small: 4 dp;
  - small: 8 dp;
  - medium: 12 dp;
  - large: 16 dp; and
  - extra large: 28 dp.
- Introduced an ArchiMan Material 3 light color scheme with architectural blue, tonal containers, softer neutral surfaces and accessible text contrast.
- Added a complete Material 3 typography scale for headlines, titles, body text and labels.
- Rebuilt shared input and search controls on Material 3 `OutlinedTextField` rather than custom `BasicTextField` containers.
- Preserved temporary compatibility wrappers so feature screens can be renamed incrementally without duplicating UI logic.
- Replaced deprecated `Divider` calls with `HorizontalDivider`.
- Migrated directional icons to auto-mirrored Material icons for right-to-left compatibility.
- Updated exposed dropdown anchors to the current Material 3 API.
- Removed all Compose deprecation warnings produced by the current application build.

## UX contract

- Material 3 components supply state, focus, error, keyboard and accessibility behavior.
- The 360 dp minimum viewport remains supported.
- One dominant action remains visible per screen.
- Bottom navigation continues to represent hierarchy, not a generic tool launcher.
- Dense measurement rows remain horizontally scrollable rather than compressed.
- Business status colors remain semantic and are not used as decoration.
- Touch targets remain at least 48 dp.

## Compatibility decision

Legacy `Carbon*` color identifiers remain as source-level aliases because many existing screens use them. Their values now align with the ArchiMan Material 3 palette. Removing every identifier in one mechanical change would create unnecessary review risk without changing the rendered UI. New and substantially edited screens must use `MaterialTheme.colorScheme`, `MaterialTheme.typography` and `MaterialTheme.shapes` directly.

## Verification

- `testDebugUnitTest`: passed.
- `assembleDebug`: passed.
- 54 automated tests: passed with zero failures.
- Compose compilation: no Material component or icon deprecation warnings.
- Product-boundary gate remains required before release.
- Updated APK installed successfully on Samsung SM-M115F; the activity launched without an app-process fatal, Room or SQLite error.

## Remaining migration work

1. Replace remaining compatibility color references with semantic `MaterialTheme.colorScheme` roles feature by feature.
2. Add dark-theme support only after every remaining hard-coded surface is removed.
3. Add screenshot baselines for 360 dp, standard phone, landscape, tablet and large-font configurations.
4. Perform TalkBack, contrast, keyboard focus and switch-access testing.
5. Standardize feature-level empty, loading, error and confirmation states.
6. Introduce adaptive list/detail layouts for the future LAN/tablet administration experience.
