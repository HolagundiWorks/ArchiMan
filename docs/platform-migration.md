# MynaHub Mobile: what changes for the Myna platform

Current policy, confirmed by the owner on **2026-11-16**: **live API sync only; no Mobile backup/restore, offline company snapshots or persisted drafts**.

The current implementation and verification are in [live company sync](live-company-sync.md).
Only the encrypted login credential is retained. Project pages update
automatically and clear when the connection is lost. Backup entry points and
portal transfers are removed. Other entities and native desktop API integration
remain separate work.

**Historical notes below:** earlier offline-cache, saved-draft, backup and
offline-queue proposals are superseded by this owner decision and are not current
requirements. The tested offline implementation has been removed.

The specification below records the target and remaining work. The platform plan
and API live in the MynaHub repository (`docs/hub-platform-plan.md`, `apps/api`).
The API must include `/v1/mobile/connection` for this client. The guidelines they
implement are the owner's `DEVELOPER_GUIDELINES.md` v1.0.

## Implemented connection slice

Practice > Cloud sync now opens **Company connection**. Enter a runtime HTTPS
API origin; the client checks `/health`, `/v1/mobile/connection` (service, API
version, minimum app version, auth path and capabilities) and `/health/ready`
before saving it. No API address or credentials are compiled into the new path.
Only debug builds allow HTTP on localhost, 127.0.0.1 or emulator 10.0.2.2; release
still requires HTTPS. Debug installs use `.apidev`, leaving the practice app separate.

Implemented: email/password sign-in, invitation acceptance, authenticator/backup
code sign-in, MFA enrollment and replacement-token handling, identity/effective
permissions, session list/revoke, sign-out and account refresh on resume. Pending
MFA cookies stay in memory; the pending header is never persisted as a session.
Keystore-encrypted preferences contain the full session/identity cache, with a
confirmed disk write before success. Membership removal/suspension and a refused
session clear that cache. Network outages preserve it; sign-out clears it locally
even if remote revocation fails, and reports that distinction. Company switching
requires sign-out. Authenticator setup blocks screenshots while secrets are shown.

New account code touches no local business tables and uploads no local records. The
company screen can load assigned project summaries from `/v1/projects`,
in pages of 25; API scope/permissions decide which rows are returned. These rows
stay separate from local projects. The first page is sealed with the account
for labeled offline viewing; online refresh replaces its scope, and sign-out
or confirmed access refusal clears it. Sign-out clears local state before attempting
remote revocation; a 403 refreshes effective permissions. Tasks, project creation/deletion,
files and synchronization still require their server endpoints/M3. Existing
offline features stay local. The legacy Neon
sync implementation remains reachable under **Legacy sync settings**, including
its old optional BuildConfig fields, until M3 cutover passes. Those fields are
not used by the new company client. Public registration was not added.

The connection-contract fixture under `app/src/test/resources/contracts/` was
exported from the built API's `MOBILE_CONNECTION` value. A small typed Kotlin
client consumes this versioned metadata; JVM tests exercise it alongside fake
transport responses. The scoped project-read capability is additive; business
sync contracts remain future work; online edits use the versioned PATCH contract.

### Offline project summaries (2026-11-16)

The latest authorized first page (up to 25 summaries) is now sealed with the
account and restored across app closure. Offline results show their saved
source and last-checked time; extra pages remain online-only. A successful
identity refresh discards the old scope before fetching a replacement page.
An unavailable API before identity refresh preserves the last snapshot;
confirmed permission/session loss clears it. No local practice tables are
changed. See [offline company projects](offline-company-projects.md).

192 Android JVM tests and debug/test APK builds passed. The physical phone
cached only Staff's assigned DEMO-01, recovered it after force-stop with the API
link disconnected, and displayed the offline label and timestamp. Suspending
the fictional Staff account then purged the snapshot/session. After another
force-stop and offline reopening, neither could be restored. Staff was
reactivated and signed in with a fresh scoped cache. No backend migration was
needed. Full offline business sync, public HTTPS and release installation
remain pending/unverified.

### Online project edits (2026-11-15, preceding milestone)

Members with projects.write can explicitly edit a company project's name, code
and status. The API enforces role scope and assignments again. Each draft holds
an operation UUID and expected revision. Once submitted its values are frozen
for identical retries; interrupted requests never cause automatic resubmission.
A version conflict shows submitted and company values. The user can review
their draft against the current revision or use company values, then explicitly
save. Neither choice overwrites automatically. Closing hides the editor.
One saved edit is now sealed with the account and can be reviewed after reopening;
unsaved field changes remain memory-only. Sign-out or confirmed access loss
clears the saved edit. No automatic/background queue or local project
synchronization was added. See [pending edits](pending-company-edits.md).

Online-edit milestone checks: 124 API tests and 166 Android JVM tests passed; backend
lint/type checking/build/format and debug APK/test-APK builds passed. Live Neon
checks verified edits, identical retries, structured conflicts, operation-ID
reuse refusal and write/receipt isolation. The physical phone passed save,
retry, conflict and restoration through the real account service. The editor
and a deliberately stale draft's conflict screen were visually inspected; both
draft and newer company values were visible. Original demo details were
restored and reloaded; revision history remains. Public HTTPS, release
installation and full offline sync remain unverified/not implemented.

### Verification of this slice (2026-11-15)

Run: `:app:testDebugUnitTest` (160 tests, zero failures/errors), `:app:assembleDebug`
and `:app:assembleDebugAndroidTest`. Backend checks passed 113 tests plus lint,
type checking and build. On a real Android device, the isolated API Dev app
connected through an adb loopback forward to the running demo API backed by
Neon's separate `demo` branch. Device checks passed Staff sign-in, restored
Keystore-protected identity/permissions, active-session listing, current-session
revocation, Administrator MFA challenge/verification, and another Staff sign-in.
Confirmed the sealed session survived instrumentation exit and reopening the app.
Inspected the Company connection screen visually with the demo identity/role.

Fixed findings: current-session comparison uses the raw portion of Better Auth's
signed bearer header; account storage confirms its disk write before reporting
success. Private device-test input is deleted before network calls (instrumentation
finish can terminate before finally); verified absent afterward, and no private
fixture files are packaged in the APK. The normal practice app was not replaced.

Not run: a public HTTPS deployment/certificate flow on the device, a release
installation, Android-device invitation acceptance, MFA enrollment UI/backup-code
sign-in. Those are covered only by JVM/backend tests
where applicable. No business sync, background work or cloud-derived business
cache was implemented, so M3 and its data-removal/conflict tests remain pending.

### Device access-removal checks (2026-11-15)

Extended the opt-in device suite with assignment-removal and ended-access modes.
Against the live demo API/Neon, a fresh Staff session first read DEMO-01. Removing
that assignment returned no projects while retaining the session. Suspending the
fictional Staff member then refused the next project read and cleared the sealed
session, including after recreating the account service. The company address
remained saved. Logical rows in the isolated API Dev app's local databases were
unchanged across both checks; SQLite WAL bookkeeping is deliberately excluded.

The test operator restored the assignment and reactivated Staff; a fresh phone
sign-in and assigned-project read passed afterward. Private input was deleted.
These checks exercise the real Android account service and Keystore, not a UI
automation assertion that an already displayed project disappears. See
[device-check procedure](company-api-device-checks.md). Full sync cleanup still
needs its own tests once cloud business records are persisted.

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

### Scoped project verification (2026-11-15)

The API now implements project list/detail, assignments and role management;
the earlier API inventory above describes the identity-only milestone. The
current backend suite passes 113 tests. Live demo checks on Neon verified
assignment boundaries, immediate access removal, owner protections and runtime
RLS with transaction context cleared after commit.

The physical-device check passed with Staff receiving exactly DEMO-01. The
installed API Dev screen was visually checked showing Hillside Villa only.
These are read-only summaries; project writes, tasks, files and sync remain
pending. Public HTTPS and release installation remain unverified.

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
