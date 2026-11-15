# MynaHub Mobile: what changes for the Myna platform

Status: **specification for the mobile work, 2026-11-15. Nothing in this document is implemented yet.** It tells the Android work what must change when MynaHub moves from "clients talk to the Neon Data API" to the company-owned **Myna API**. The platform plan and the API itself live in the MynaHub repository: `docs/hub-platform-plan.md` and `apps/api` (Phase 1, commit `9f83af8` there). The guidelines they implement are the owner's `DEVELOPER_GUIDELINES.md` v1.0.

The app is named **MynaHub Mobile** (SUITE.md, the launcher label). The guidelines call it "Myno Mobile"; that is a naming slip, keep MynaHub Mobile.

## 1. Where the phone is today (verified on this branch, `mynahub-mobile-sync`)

Built, tested on the JVM, **never run on a device**: Practice > Cloud sync (`ui/CloudSyncPane.kt`); `sync/` package:

| File | What it does now | Fate |
|---|---|---|
| `SyncSchema.kt`, `SqlDb.kt` | Tracking tables `sync_map`, `sync_dirty`, `sync_state` and triggers on `clients`, `projects`, `project_tasks`, created on every database open | **Keep**, extend (section 5) |
| `SyncStore.kt` | Change list, acknowledgements, applying pulled records (same rules as the desktop `sync.rs`) | **Keep the structure**, change the protocol it speaks (section 5) |
| `SyncEngine.kt` | Push then pull in rounds | **Adapt** to operations and cursors |
| `CloudClient.kt` | `CloudAuth` (Neon Auth over HTTPS), `HubDataApi` (Neon Data API with a `hub` schema header, retry on "not signed in") | **Replace** with a Myna API client |
| `CloudSessionStore.kt` | Session cookies sealed with the Android Keystore | **Keep**, extend (section 3) |
| `CloudSyncService.kt` | Glue, reads `BuildConfig.NEON_AUTH_URL` / `NEON_DATA_API_URL` | **Rewrite** around the company connection |
| `build.gradle.kts` | `NEON_AUTH_BASE_URL` and `NEON_DATA_API_URL` from `local.properties` into `BuildConfig` | **Remove** (section 2) |
| tests under `app/src/test/.../sync` | 29 tests on real SQLite via JDBC, fakes for HTTP | **Keep the approach**, update to the new protocol |

What the phone syncs now: clients, projects, project tasks, with the whole local table uploaded on join. That changes (section 4).

## 2. Company connection (replaces build-time addresses)

A published APK cannot serve many companies if the address is compiled in. Required:

- A **run-time company connection**: the company's API address (`https://api.company.example`), stored on the device (not in `BuildConfig`, not in git). Entered by pasting it or scanning a pairing text/QR shown by MynaHub (format to be fixed with the API: a single `https` URL, optionally with a company name).
- On save, check `GET {url}/health` and the API version the phone supports (the API will expose a minimum client version in `/v1/mobile/bootstrap`); refuse a plain `http` address except for a developer build.
- **Remove** `NEON_AUTH_URL`, `NEON_DATA_API_URL`, the `publicConfig` helper and the `local.properties` lines. The phone must hold **no** database address, no Neon address, no key (guidelines sections 6.5, 11.4, 15.2).
- One company per install for now; switching company = sign out, clear cloud-derived data, connect again.

## 3. Authentication (Better Auth, hosted inside the company API)

The accounts move from Neon-managed Better Auth to the API's own Better Auth. The phone must:

- Sign in with email and password against the API's Better Auth routes (base path set by the API's adapter; take it from the OpenAPI document, do not hardcode).
- Support the **invitation flow**: an employee is invited by an administrator, opens the invitation (link or token), sets a password and activates the membership. There is **no public registration** and no "create account" screen for strangers.
- Handle **MFA** when the account requires it (administrators must).
- Keep a **per-device session**: show this device's session, a "Sign out" that revokes it on the server, and handle a revoked session (the next request returns an authentication error) by clearing the session and returning to the sign-in screen.
- Check **membership status on every protected request**: a suspended account gets a distinct error code; the phone then signs out and applies the data-cleanup rule in section 6.
- Keep `CloudSessionStore` (Android Keystore AES-GCM, non-exportable key). Never store the password; never store a long-lived credential in plaintext; never log tokens or cookies. Reauthentication for sensitive operations (approvals) when the API asks for it.
- The phone's local app lock (`AppLockGate`) and the existing local sign-in are separate and keep working offline; cloud sign-in must never be required to open the app or to use local-only features.

## 4. Permissions and scope

- After sign-in fetch the person's **effective permissions** (`GET /v1/me/permissions`) and cache them; refresh on app start and after any `403`. Permission identifiers are `resource.action` strings shared with Hub and the API (`projects.read`, `tasks.update`, `tasks.approve`, `documents.upload`, ...). **Hiding a button is not security**; the server enforces everything. Never trust or send a role or an organisation id from the client as authority.
- **Cloud scope of the phone (per the guidelines section 11.1):** assigned projects (read, selected updates), assigned tasks (read, update), site updates (create, update), photographs (capture, upload), authorised documents (view), notifications, selected approvals. **Out of cloud scope, stays local-only and is never synced:** finance, ledger, invoices, receipts, fee proposals, measurements and the Measurement Book, RCC members, rate book, schedules, the Wi-Fi portal, Siteman, company administration. The phone must not duplicate Hub.
- The phone downloads **only what the person is assigned to**, not "all company records". The current "upload everything, merge by name" join (docs/cloud-sync.md) goes away: joining a company means signing in and receiving the person's assigned work. Local records that already exist on the phone stay local unless the person (with `projects.create`) explicitly **publishes** a project to the company; design that screen with a preview.
- Existing local entities map to the cloud like this (to be confirmed against the API contract): `ProjectEntity` to projects, `ProjectTaskEntity` to tasks, `SiteInspectionEntity` / `DailySiteReportEntity` / `SiteIssueEntity` to site updates, `SiteDocumentEntity` and drawings to documents (metadata plus signed-URL file transfer). Clients are not a phone-synced entity in the new scope.

## 5. Synchronisation protocol changes

The change tracking (triggers, `sync_dirty` with a growing `v`, UUID mapping) stays. What changes:

| Today | Required |
|---|---|
| Push a record with `base_rev`; the server answers created/updated/merged/rejected_older | Push **operations**: `{operationId (uuid), type (for example task.update), resourceId, expectedVersion, changes}`; the server answers a **receipt** or a **structured conflict**. Retries must be **idempotent**: reuse the same `operationId`; persist it before sending (new table `sync_operation`) |
| Pull by a numeric `seq` | An **opaque cursor string** from the server; store it, never interpret it |
| Last writer wins, loser kept only on the server | On conflict the server returns both versions; the phone stores it (new table `sync_conflict`) and offers resolution: simple fields (take theirs, keep mine, edit); approvals and anything financial are not resolved on the phone |
| Everything the person may see | Filtered by organisation and assignment; when a record becomes unassigned or deleted the server sends a **deletion/removal event**; the phone deletes its copy of cloud-derived rows only |
| `cloud_rev` | the record's `version` (integer) |

Also required: unique operation ids, record versions, deletion events, receipts remembered, permission changes handled (a revoked permission turns a queued operation into a `403` receipt the person can see and discard).

**Release 1 is online-first** (guidelines section 12.6): read from the local cache, send changes when online, no background or periodic sync yet, the offline queue limited to the scope in section 4. Release 2 widens offline.

New Room-level pieces are additive (`sync_operation`, `sync_conflict`, a permissions cache or DataStore entry, per-row `origin`/`cloud` flag so cleanup knows which rows came from the cloud). Keep them out of the Room entity schema the way the current tracking tables are, or add real entities with a numbered migration; either way include a migration test.

## 6. Device security and data cleanup

- No database credentials, no hardcoded company secrets, no developer-controlled services (guidelines section 11.4, 15.2).
- On sign-out, session revocation or suspension: delete the session, the permissions cache, the cursor, `sync_operation` and `sync_conflict`, **and the cloud-derived rows** (those flagged as coming from the company), but never the person's own local-only records. Provide an explicit "Remove company data from this phone" action.
- Local database protection: the app already has encrypted backups; check that a backup or restore cannot carry a session or a cloud cursor to another device (exclude them or re-authenticate on restore).
- Sensitive cache cleanup when the app is locked is the existing `AppLockGate` behaviour; extend it to the cloud cache.

## 7. API the phone will need (proposal, **not built**; the OpenAPI document `/openapi.json` of the API is the contract once it exists)

| Need | Proposed route (versioned `/v1`) |
|---|---|
| Health, version | `GET /health`, `GET /v1/mobile/bootstrap` (min client version, organisation name, assigned projects and tasks, cursor) |
| Who am I, what may I do | `GET /v1/me`, `GET /v1/me/permissions` |
| Sync | `POST /v1/mobile/sync` (operations in, receipts and conflicts out), `GET /v1/mobile/sync?cursor=...` (changes) |
| Projects, tasks, site updates | `/v1/projects`, `/v1/tasks`, `/v1/site-updates` |
| Files | Request an upload authorisation, upload to a short-lived signed URL, record metadata; downloads the same way (`/v1/documents`) |
| Notifications | `GET /v1/notifications` (polling first; push needs Firebase and is a separate decision) |
| Sessions | Better Auth routes plus a list/revoke of this person's sessions |
| Errors | `{ "error": { "code", "message", "requestId" } }` always; stable codes such as `PERMISSION_DENIED`, `MEMBERSHIP_SUSPENDED`, `VERSION_CONFLICT`, `VALIDATION_FAILED` |

Generate or hand-write a typed Kotlin client from the OpenAPI document (decide which when Phase 4 of the API lands) and add **contract tests** so a breaking API change fails a build here. Do not duplicate contract types by hand across the repositories.

### What the API has built so far (2026-11-15, MynaHub repository `apps/api`, tested on a WebAssembly PostgreSQL, not on a deployed server)

Better Auth routes under `/api/auth/*` (email and password sign-in, sign-out, `list-sessions`, `revoke-session`, `revoke-other-sessions`, two-factor `enable`, `verify-totp`, `verify-backup-code`); `POST /v1/invitations/accept` (token, name, password); `GET /v1/me`; `GET /v1/me/permissions`. Mobile sign-in is `Authorization: Bearer <token>` where the token is the `set-auth-token` response header. **Two things the phone must handle:** (1) after a successful two-factor verification the server **replaces the session**: the old token stops working and the new one is in the `set-auth-token` header of the verify response, so store that one; (2) a sign-in that still needs a second factor answers `twoFactorRedirect: true` and may also carry a `set-auth-token` header, which is **not** a session and must not be stored. Error bodies follow `{ "error": { "code", "message", "requestId" } }`; codes seen so far: `UNAUTHENTICATED`, `NOT_A_MEMBER`, `MEMBERSHIP_SUSPENDED`, `MFA_REQUIRED`, `REAUTH_REQUIRED`, `PERMISSION_DENIED`, `INVITATION_INVALID`, `VALIDATION_FAILED`. Sync, projects, tasks, site updates and files are not built yet.

## 8. Order of work (mobile follows the API)

| Mobile step | Needs from the API first |
|---|---|
| M0 This document reviewed; open questions answered | Nothing |
| M1 Company connection screen, remove Neon `BuildConfig`, health and version check | API Phase 1 (done) |
| M2 Sign-in, invitation acceptance, MFA, session list and revoke, permissions fetch | API Phases 2 and 3 |
| M3 Sync rewrite (operations, receipts, conflicts, cursor, scoped pull, cleanup) for projects and tasks | API Phase 4 sync endpoints |
| M4 Site updates, photographs and documents with signed URLs; notifications by polling | API files and site-update endpoints |
| M5 Release hardening: **device run**, revocation and suspension, backup/restore interplay, upgrade from the current build | API Phase 7 |

Until the API cutover the current Neon Data API path keeps working on the dev branch; do not delete it before M3 passes. The Neon dev project is disposable; nothing there is production.

## 9. Tests that must exist (guidelines section 16.4)

Company connection (valid, wrong address, `http`, API too old); login, logout, session expiry, revoked session; suspended account; offline cache behaviour; pending operation retry (same `operationId`, no duplicate effect); synchronisation conflict (both versions shown, nothing silently lost); permission change while operations are queued; local data cleanup (cloud rows removed, local-only rows kept); upgrade from the build that stores nothing cloud-related. JVM tests against real SQLite as today for the store; **device tests** for the Keystore, the HTTPS transport and the screens (none exist yet).

## 10. Definition of done for the mobile side (guidelines section 20)

Follows the company-owned architecture (no Neon addresses or database credentials in the app); authentication integrated; the server, not the app, is the authority for permissions; organisation and assignment boundaries respected; local schema changes have migrations and tests; API contract used, not re-declared; relevant tests pass **and a real device run is recorded**; sensitive data protected; existing local features unchanged; documentation (`docs/cloud-sync.md`, this file) updated in the same change.

## 11. Open questions for the owner (shared with the platform plan)

1. **Publishing existing local projects** to a company: allowed at all, and for which role? (Section 4.)
2. **Pairing format** for the company connection: paste only, or a QR shown in MynaHub? (Section 2.)
3. **Notifications:** polling only for release 1, or Firebase push? (Section 7.)
4. **Photographs:** size limits, formats and whether they are uploaded on mobile data or Wi-Fi only.
5. **Client generation:** generated Kotlin client from OpenAPI, or a small hand-written one with contract tests?
