# MynaHub Mobile: implementation and migration status

Updated 2026-11-16. Company data uses the live Myna API. Mobile has no local
backup/restore, offline company snapshot, saved draft or persistent write queue.
Only the encrypted login credential and public connection settings are retained.

Implemented: runtime API discovery/readiness checks, sign-in and invitations,
MFA, session list/revoke, live scoped project pages, online project edits with
conflict review, live task pages, permission-scoped task writes and confirmed project creation/deletion. Company identity,
permissions, rows and drafts clear on disconnect, backgrounding or leaving.

Existing practice records remain in Room and are separate from company API data.
They are not uploaded or deleted. Task deletion/assignees, clients, site records, files,
native desktop API integration and production HTTPS deployment remain pending.
The unreachable legacy manual-sync screen/service and compiled Neon endpoints
are removed. Legacy archive fixtures are excluded from the product APK.

See [current implementation](current-implementation.md),
[live company sync](live-company-sync.md), [branding](BRANDING.md) and
[device checks](company-api-device-checks.md).
