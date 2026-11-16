# MynaHub Mobile: current implementation

Updated 2026-11-16. Product name: MynaHub Mobile; repository/package compatibility
name: ArchiMan. The release ID remains com.holagundi.archiman and API Dev installs
as com.holagundi.archiman.apidev.

## Implemented company API flow

- Runtime HTTPS API connection and capability/readiness discovery.
- Invitation acceptance, sign-in, MFA enrollment/challenge and session management.
- Authorized live project/task pages, plus permission-scoped task creation, editing and status changes.
- Explicit online project/task writes with stable retries and version-conflict review.
- Permission-scoped project creation and confirmed deletion with retained audit history.
- Keystore-encrypted credential persistence; identity, permissions, business rows
  and drafts only in memory, cleared on disconnect/background/leaving.
- No local backup/restore, company offline cache or persistent write queue.

Local practice modules still use Room. Their data remains separate and is not
automatically uploaded, deleted or merged. Backup archive fixtures now live
only in androidTest; shared archive validation used by protected-file features
remains in main. The obsolete portal transfer implementation and its retired-behavior
test are removed. The old manual-sync screen/service and compiled Neon URLs are
removed. Pure legacy sync helpers/tests and database migration history remain
for compatibility until a separately verified data migration exists.

## Remaining work

Desktop API integration. Public HTTPS hosting, production-scale load, release
installation and store rollout are not verified.

## Documentation

[Branding](BRANDING.md), [live data](live-company-sync.md),
[device checks](company-api-device-checks.md), [migration status](platform-migration.md).
Superseded offline and backup proposals have been removed.

## Build and cleanup

Run gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest.
Keep the latest verified app/test APKs and test reports. Generated build
intermediates, old screenshots and temporary logs can be removed after validation.
Never clean the installed app's records, signing material, local.properties or
SDK/dependency installations as part of build-output cleanup.

See [maintenance and cleanup record](maintenance-2026-11-16.md) for the cleanup scope and validation.

Live client contact reads are implemented with opt-in pagination and assigned-project visibility. Client creation/editing, confirmed deletion and project/client link management are implemented online. Company contact data stays in memory only.

Task assignees and confirmed deletion are implemented with project-scoped permissions, revision conflicts, immutable retries and retained audit history.

Shared company site records (instructions, inspections and snags) have live reads, online create/edit and confirmed deletion, with scoped permissions, revision conflicts and immutable retries. Other local fieldwork modules remain separate.

Private project attachments have live metadata, authenticated uploads/downloads, confirmed removal and memory-only Mobile previews. PDF/image decoding is isolated; no offline file cache is added. Storage retention/reconciliation remains part of production rollout.
