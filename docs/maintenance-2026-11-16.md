# Hub and Mobile maintenance — 2026-11-16

## Source and documentation cleanup

Current READMEs, implementation maps and branding now describe the company API,
Ember identity, actual Android Carbon styling and desktop integration limits.
Old direct-Neon, offline-cache and backup proposal copies were removed at the
owner's request. Current setup and release-preparation text no longer claim
offline Mobile backups, compiled Neon URLs or a completed production rollout.
Obsolete standalone offline/draft pages and one unused empty evidence CSV were
removed. Intentional .gitkeep directory placeholders remain.

Removed the unreachable Android legacy sync screen/service and compiled Neon
endpoints, unused portal-transfer implementation/test and 13 unused backup
strings. The backup repository is device-test-only. Protected-file archive
validation and legacy database migration history remain where still used.
The Appearance card says Ember; portal help no longer advertises restore.

## Disk cleanup scope

Only verified generated build outputs and owned temporary test files were
removed. The first cleanup pass removed 3,062,528,915 bytes from D: and
156,342,242 bytes from C: (about 3.22 GB total). The final rebuild's regenerated
intermediates were also cleared after verification, with latest APKs and reports retained.

Preserved the latest Hub 1.9.0 installer, current API Dev app/test APKs, reports,
API runtime output, dependency installations, source, credentials, signing
material, existing company records and user-created release APK/screenshots.
CAD/Ops checkouts and installers were not changed. No aged top-level .tmp files
were found in the inspected standard temporary folders. Downloads, Recycle Bin,
system installations and unrelated projects were not cleaned.

## Verification

175 Android unit tests and both debug app/test APK builds passed after final
source and resource cleanup. The compiled portal help text was checked.
The cleaned build passed the physical company API check (projects/tasks,
credential-only persistence and retired backup routes). The Ember label was
visually checked. APK inspection found no BackupRepository, PortalTransfer,
CloudSyncService or CloudSyncViewModel production classes. Current-document
local links and Git whitespace checks passed. Backend source did not change.

Public HTTPS hosting, signed release installation, store publishing and a fresh
Windows desktop build were not performed for this cleanup.
