# Live company sync

Owner decision, 2026-11-16: Mobile uses live company API data, with no local
backup/restore, offline project snapshots or persisted edit drafts.

## Current behavior

Practice > Live company sync accepts the company's HTTPS API origin. Sign-in,
invitations, MFA and session controls use the company API. Project pages load
and update automatically while the screen is open; Next/Previous switches
between authorized pages of at most 25 rows.

View company tasks opens a live task page for a project on the current
authorized project page. Tasks update through the same live request and are
bounded to 25 rows, with Next/Previous controls. Titles, descriptions, status,
priority, due time and revision stay in memory only. Changing project pages,
losing access, disconnecting or leaving clears the task page. Local practice
tasks are neither uploaded nor merged with this company dataset.

Accounts with tasks.write can create and edit company tasks online, including
title, description, due date, status and priority. Each save uses a stable task
and operation UUID plus the expected revision for edits. Conflicts require
explicit review and a fresh operation. The editor is memory-only and closes on
connection loss, backgrounding, leaving or loss of current project/write access.
Read-only accounts see task records without write controls.

Organization-scoped creators can open New company project and save it online.
Authorized deletion requires a fresh session, the expected revision and typing
DELETE in the selected project's confirmation form. A conflict requires review
and confirmation again. Deleted projects and their tasks disappear from all live
views, while database rows and audit history remain. Creation and deletion use
stable operation UUIDs; their forms and confirmation text are memory-only.

The API holds each live request for up to 20 seconds, checking current session,
membership, permissions and project scope approximately every second. It returns
the changed page or a heartbeat snapshot. The client reconnects automatically
after a connection failure. This is authenticated API long polling; it does not
enable a separate Neon Realtime service or expose database credentials.

Project edits save online and use expected versions and operation UUIDs.
Conflicts show both versions for explicit review. Drafts exist only in memory.
Leaving the screen or detecting a lost connection clears company records and
drafts. An interrupted write may already have committed: reconnect to read the
current server state. There is no offline write queue or automatic write replay.

## Local storage and backup removal

Only the API address and Keystore-encrypted login credential are retained.
Identity, permissions, project summaries and draft values are not persisted.
Existing offline snapshot/draft fields are removed when the account store opens,
including abandoned data from a previously selected API origin. Reopening must
fetch identity and projects online. HTTP caching is disabled.

Suspension or session revocation clears the credential too. A network failure
retains the credential for reconnection, while clearing visible company data.
Live reads use a separate cancellable connection so closing the screen does not
cancel an online save or erase a newly rotated session.

Backup/restore is removed from the welcome screen and Practice menu. Portal
backup/download/restore paths return 410 Gone, and automatic legacy recovery
protection no longer runs at startup. Old navigation state reaches a retirement
notice rather than a backup screen. Existing practice records and previously
created files are preserved.

The retired manual-sync UI/service and compiled Neon endpoint fields are removed.
The legacy backup repository is now androidTest-only; the unused portal transfer
helper and its obsolete test are removed. Shared archive validation remains where protected-file features use it.
Database migration history and pure legacy sync helpers are retained for compatibility.

Older local practice modules remain separate and are not silently uploaded.
Current live API coverage is company projects, online task creation/editing and role/permission changes.
Task deletion/assignees, site records, files and desktop API integration still need their
own implementation; this does not claim all Mobile records are synchronized.

## Verification and limits

148 API tests and 185 Android JVM tests passed, with API lint/type checking/build
and Android debug/test APK builds. Tests cover live page changes, empty scope,
permission removal/restoration, reauthentication during held requests, credential
migration, cancellation, stale-token isolation and backup navigation removal.

The physical device received edits made by another authenticated demo session,
project restoration, assignment removal/restoration and session-revocation
refusal. It verified credential-only persisted fields and 410 responses from
retired portal routes. The Practice menu and live project screen were visually
checked, including an automatic update without a refresh action.

The physical device also loaded both DEMO-01 company tasks and verified
credential-only persistence after loading them. Changing a demo task's title
and status in Neon appeared on the open screen automatically. Original details
were restored; its revision advanced. The original task-read layout was visually checked.

A real demo API outage was also checked: the company name, identity, project
rows and editor disappeared and the screen reported reconnection. Restarting
the API and restoring its link reloaded live server data automatically. No
offline copy was displayed.

The isolated API Dev package is com.holagundi.archiman.apidev. The installed
normal practice app was not replaced. Public HTTPS hosting, release installation,
production rollout and large-scale live-connection load have not been verified.

## Live client contacts

Show company clients opens a 25-row contact page through the same live connection. Company-wide readers see company contacts; assigned readers see contacts linked to their assigned readable projects. Next/Previous changes the contact cursor. Contacts clear with connection/access loss, backgrounding or leaving. No contacts are saved locally. Organization-scoped client creation/editing and confirmed deletion are available online; project linking also checks current project write scope.

New/Edit/Delete company client and Manage project client use online operations with revision conflicts and stable retries. Deletion requires DELETE confirmation and refuses contacts still linked to active projects. A conflict must be reviewed; forms close when disconnected or backgrounded.

Task assignees are managed online from a live eligible-member page (25 per page, up to 20 selected). Confirmed task deletion requires typing DELETE and a fresh login. Assignment, edit and deletion share revisions and require conflict review. Task action forms and member lists remain in memory and clear on connection/access loss, backgrounding or leaving. Deleted tasks retain database/audit history and disappear from live views.

View company site records opens live instructions, inspections and snags on the current project. Authorized accounts can create/edit them online; managers can delete with typed confirmation and a fresh login. Calendar/resolution dates and versions are validated. Rows, drafts and confirmation remain memory-only. Daily reports, issue histories and local practice inspections remain separate.

Private company attachments now have live 25-row pages, explicit uploads and confirmed removal. PDF/PNG/JPEG/WebP/TXT/CSV are supported up to 5 MiB. File data is hash-verified and stays in RAM; binary previews use an isolated service and no saved copy. Choose a local file, then open its project and confirm upload. Picker results are not restored after process recreation. Backgrounding/leaving/disconnect/scope loss clears selections, drafts and previews. Removal retains private content and audit history on the company backend.
