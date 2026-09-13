# ArchiMan Database Migration Backup and Rollback Runbook

> Current database target: Room schema 24. Package ID and database filename remain unchanged to preserve upgrades and field data.

## Purpose

This runbook applies to managed ArchiMan deployments. A database backup is required before installing an APK that raises the Room schema version. Measurement rows and managed photo attachments must be treated as one backup set.

## User-operated whole-company package

Company Profile → Full company database creates a password-encrypted `.archimandb` package. It contains a consistent SQLite snapshot, the company logo directory and managed measurement-photo directory. The manifest records schema version, exact file list, sizes and SHA-256 checksums. Supabase connection preferences, temporary drafts and arbitrary device files are deliberately excluded.

Import decrypts into private staging, rejects unsafe or unexpected paths, verifies every checksum, runs SQLite integrity and foreign-key checks, and shows the company name/count preview before confirmation. A confirmed import is applied only on the next app start, before Room opens. The prior database and managed files are retained in private recovery storage; the newest three recovery copies are kept.

Do not send backup/export/import operations through the LAN workspace. Keep the package password separate from the package, use at least 12 characters, and test restore on a non-production device before relying on it operationally.

## Pre-deployment gate

1. Confirm the current APK opens without a Room or SQLite error.
2. Record the installed APK version, database `PRAGMA user_version`, measurement count, and attachment count.
3. Stop ArchiMan before copying files so the database and WAL are consistent.
4. Export and verify a whole-company `.archimandb` package, or back up the complete application data through the approved device-management channel. A database-only copy is insufficient because row photos and the company logo live under private managed directories.
5. Calculate and retain SHA-256 checksums for the backup archive and APK.
6. Keep the prior signed APK together with the backup. Installing an older APK over a newer schema is not a rollback.

## Debug-device verification procedure

For a USB-debuggable internal build, stop the package and copy all present SQLite files using `run-as`:

- `databases/site_measurement.db`
- `databases/site_measurement.db-wal`
- `databases/site_measurement.db-shm`
- `files/measurement_attachments/`
- `shared_prefs/measurement_drafts.xml`

The current package remains `com.aistudio.sitemeasure.qvtrpl` and the database remains `site_measurement.db`; the ArchiMan rebrand intentionally does not rename either technical identifier.

Never inspect only the main `.db` file while the app is running; committed changes may still be in the WAL.

## Post-migration acceptance

After first launch, verify:

- the expected schema version;
- the measurement count equals the pre-deployment count;
- every measurement row resolves to its sheet, project, contractor, and work item;
- `PRAGMA foreign_key_check` returns no rows;
- formula codes and versions are populated;
- expanded practice/client profile and project consultancy/site-data fields are available from schema 19, with LAN users and immutable web audit events added in schema 21;
- schema 22 removes the obsolete commercial tables and columns only after copying every quantity-bearing measurement field into the replacement table;
- schema 23 adds project consultants, coordination items and immutable coordination events without rewriting existing project or measurement data;
- schema 24 adds formal decisions, daily site reports, snag/NCR records and immutable issue events without rewriting existing project or measurement data;
- managed photo paths referenced by measurements exist;
- the application opens the M-Book and canonical editor without a fatal error.

Do not approve deployment based only on a successful APK installation.

## Rollback

If acceptance fails:

1. Stop ArchiMan and preserve the failed post-migration data separately for diagnosis.
2. Remove the failed application data only through the managed recovery workflow.
3. Restore the entire pre-migration backup set, including attachments and preferences.
4. Reinstall the exact prior signed APK.
5. Launch and repeat the pre-deployment integrity checks.

Room migrations are forward-only. Do not attempt to downgrade the database in place, manually decrement `user_version`, or copy individual old tables over a new database.

## Retention and audit

Retain the backup, APK checksums, migration test report, operator, device identifier, timestamps, and acceptance results according to the organization’s measurement-record retention policy. Backups must be encrypted at rest and accessible only to authorized project administrators.
