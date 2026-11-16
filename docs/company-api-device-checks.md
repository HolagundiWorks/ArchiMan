# Company API device checks

Use only the fictional demo company and the isolated API Dev package
com.holagundi.archiman.apidev. Credentials come from the private app file
files/company-api-device-test.json, never command arguments or logs. The runner
deletes that input before network calls.

Run:

```sh
adb shell am instrument -w -e suite companyApi com.holagundi.archiman.apidev.test/com.holagundi.archiman.BackupDeviceChecks
```

The fixture supplies apiUrl, email and password. The default account check uses
Staff, verifies fresh online identity after credential recreation, assigned
projects and session revocation. Optional mfa supplies an Administrator's
email/password/uri to test TOTP. signInOnly uses the demo Project Manager, checks
the DEMO-01 company tasks and leaves a fresh online session for UI inspection.
editProject checks online save/retry/conflict and restores original
fictional project details before finishing.

editTasks uses the demo Project Manager. It creates/replays a fixed fictional
demo task, edits it with stable retry, verifies an explicit stale-revision
conflict, and restores its original title, description, due date, status and
priority. The created demo task remains available for manual testing; it is not
a local practice task. Each mode verifies credential-only persisted fields.

liveProjects uses Staff. The runner emits stages waiting-change, waiting-restore,
waiting-assignment-removal, waiting-assignment-restore and waiting-suspension.
An authorized second session changes/restores DEMO-01, removes/restores Staff's
assignment, then suspends Staff. The device verifies each live update and final
credential removal. Reactivate Staff and restore all fixture details/assignments
in cleanup, including on failure.

projectLifecycle uses the demo Owner and optional totpUri for MFA sign-in. It
creates a fresh fictional project and child task, checks create retry and a stale
deletion conflict, then confirms removal and verifies deletion retry and hidden
project/task views. Only this new test project is removed; rows/audit history
remain in the database. Failure cleanup also targets only that new project.

Every mode checks that retired portal backup paths return 410 Gone. Persisted
account data must contain only url/token; identity, project and draft fields are
not stored. Retired projectCache/pendingEdit fixture modes are rejected.

Success requires companyApi=passed and INSTRUMENTATION_CODE: -1. Failures print
only the stage. Open the exact API Dev activity for visual checks:

```sh
adb shell am start -n com.holagundi.archiman.apidev/com.holagundi.archiman.MainActivity
```

Live changes, restoration, assignment changes and revoked access passed on the
physical device. The backup-free Practice menu and automatic project update
were visually verified. Public HTTPS, release installation and other entity
sync remain unverified.

The clientContacts fixture mode verifies Staff sees only the fictional residential contact, contacts are opt-in, and disconnect/recreation retain no company identity or contacts. It passed on the connected physical device after the client migration. The private fixture is removed before requests and cleaned up afterwards.

The clientMutations fixture mode passed client creation/editing and stable retries, explicit stale conflicts, project linking/unlinking, linked-client deletion refusal, confirmed deletion and credential-only persistence on the phone. It creates only fictional records with fresh UUIDs and retains tombstones; the original demo projects/contacts remain intact. Private fixture removal was checked afterwards.

The taskActions fixture mode passed live eligible-member reads, task assignee changes and retries, concurrent edit/deletion conflicts, confirmed deletion/retry, removal from live views and credential-only persistence on the phone. Only a new fictional task was created and tombstoned; existing demo tasks remain unchanged.

The siteRecords fixture mode passed live site create/edit/retry/conflict, resolution-date validation, confirmed deletion/retry, disappearance from live views and credential-only persistence on the phone. It creates and tombstones only its own fictional record.

The rendererOnly fixture passed isolated PNG/PDF parsing, multiple PDF pages, bounded raw-pixel output and unchanged app-file inventory without company credentials. The files fixture passed actual private TXT/PNG/PDF upload/retry, verified in-memory previews, live metadata, confirmed deletion/retry and no persistent document copies. Original demo business records were preserved.

Interactive picker verification is paused at Samsung biometric/pattern authentication on the physical phone. Unlock must be performed by the owner; no pattern/PIN is requested or automated. Programmatic private upload/view/deletion and isolated-renderer checks passed.
