# Live company sync

Owner decision, 2026-11-16: Mobile uses live company API data, with no local
backup/restore, offline project snapshots or persisted edit drafts.

## Current behavior

Practice > Live company sync accepts the company's HTTPS API origin. Sign-in,
invitations, MFA and session controls use the company API. Project pages load
and update automatically while the screen is open; Next/Previous switches
between authorized pages of at most 25 rows.

View company tasks opens a read-only task page for a project on the current
authorized project page. Tasks update through the same live request and are
bounded to 25 rows, with Next/Previous controls. Titles, descriptions, status,
priority, due time and revision stay in memory only. Changing project pages,
losing access, disconnecting or leaving clears the task page. Local practice
tasks are neither uploaded nor merged with this company dataset.

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

Older local practice modules remain separate and are not silently uploaded.
Current live API coverage is company projects, read-only project tasks and role/permission changes.
Task mutations, clients, site records, files and desktop API integration still need their
own implementation; this does not claim all Mobile records are synchronized.

## Verification and limits

132 API tests and 176 Android JVM tests passed, with API lint/type checking/build
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
were restored; its revision advanced. The read-only task layout was visually checked.

A real demo API outage was also checked: the company name, identity, project
rows and editor disappeared and the screen reported reconnection. Restarting
the API and restoring its link reloaded live server data automatically. No
offline copy was displayed.

The isolated API Dev package is com.holagundi.archiman.apidev. The installed
normal practice app was not replaced. Public HTTPS hosting, release installation,
production rollout and large-scale live-connection load have not been verified.
