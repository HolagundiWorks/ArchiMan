# Cloud login and sync (MynaHub Mobile)

Status: Stage 3 of the MynaHub plan, built on branch `mynahub-mobile-sync` (2026-11-15). The design is `docs/hub-sync-design.md` in the MynaHub repository; this page says only what the phone does.

**What it does.** Practice > Cloud sync: sign in with a cloud account, join a company the account belongs to, then Sync now. Clients, projects and project tasks are pushed and pulled through Neon (Neon Auth for the sign-in, the Neon Data API with row level security for the data). The phone keeps working offline; nothing is sent until you press Sync now.

**What it does not do yet.** No background or automatic sync. Project stages are not synced (the phone models them as fee items, the office as percentages). Tasks without a project cannot be held on the phone and are skipped. A task's priority and position, client tax ids and private notes stay on the phone. Invoices, receipts, the ledger and files are not synced. Clients archived in the office stay visible here.

**Joining.** Before the first sync the phone shows how many clients, projects and tasks it will upload. Records with the same name as ones already in the company are merged with them, not duplicated.

**Where the code is.** `app/src/main/java/com/holagundi/archiman/sync/` (`SyncStore` change tracking and applying pulled records, `SyncEngine` push then pull, `CloudClient` HTTPS to Neon, `CloudSessionStore` the sign-in sealed with the Android Keystore, `CloudSyncService`), the screen `ui/CloudSyncPane.kt`. The tracking tables (`sync_map`, `sync_dirty`, `sync_state`) and their triggers are created on every database open; they are not Room entities and the Room schema version is unchanged.

**Configuration.** `NEON_AUTH_BASE_URL` and `NEON_DATA_API_URL` are public addresses, read from `local.properties` (not committed) or the build environment into `BuildConfig`. They are not credentials; no database password or key is ever in the app. Without them the screen says sync is off. The cloud's trusted-origin list must allow the sign-in origin the app sends (`http://localhost:3000` on the dev branch, where localhost is allowed).

**Tests.** `gradlew :app:testDebugUnitTest` runs the sync tests on the JVM against real SQLite (the triggers and queries are the real ones; table definitions come from the exported Room schema). The Keystore sealing, the HTTPS transport and the screen are not covered by unit tests.
