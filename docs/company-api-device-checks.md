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
email/password/uri to test TOTP. signInOnly leaves a fresh online session for UI
inspection. editProject checks online save/retry/conflict and restores original
fictional project details before finishing.

liveProjects uses Staff. The runner emits stages waiting-change, waiting-restore,
waiting-assignment-removal, waiting-assignment-restore and waiting-suspension.
An authorized second session changes/restores DEMO-01, removes/restores Staff's
assignment, then suspends Staff. The device verifies each live update and final
credential removal. Reactivate Staff and restore all fixture details/assignments
in cleanup, including on failure.

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
