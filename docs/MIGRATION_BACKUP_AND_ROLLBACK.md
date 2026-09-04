# ArchiMan Database Migration Backup and Rollback Runbook

## Purpose

This runbook applies to managed ArchiMan deployments. A database backup is required before installing an APK that raises the Room schema version. Measurement rows and managed photo attachments must be treated as one backup set.

## Pre-deployment gate

1. Confirm the current APK opens without a Room or SQLite error.
2. Record the installed APK version, database `PRAGMA user_version`, measurement count, and attachment count.
3. Stop ArchiMan before copying files so the database and WAL are consistent.
4. Back up the complete application data through the approved device-management or application backup channel. A database-only copy is insufficient because row photos live under private `files/measurement_attachments`.
5. Calculate and retain SHA-256 checksums for the backup archive and APK.
6. Keep the prior signed APK together with the backup. Installing an older APK over a newer schema is not a rollback.

## Debug-device verification procedure

For a USB-debuggable internal build, stop the package and copy all present SQLite files using `run-as`:

- `databases/site_measurement.db`
- `databases/site_measurement.db-wal`
- `databases/site_measurement.db-shm`
- `files/measurement_attachments/`
- `shared_prefs/measurement_drafts.xml`

Never inspect only the main `.db` file while the app is running; committed changes may still be in the WAL.

## Post-migration acceptance

After first launch, verify:

- the expected schema version;
- the measurement count equals the pre-deployment count;
- every measurement row resolves to its sheet, project, contractor, and work item;
- `PRAGMA foreign_key_check` returns no rows;
- formula codes and versions are populated;
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
