# MynaHub Mobile 1.2.0: release preparation

Updated 2026-11-17. **Draft, not a release approval or completed Play submission.**
Do not reuse earlier offline/backup listing claims. The text below reflects the
current company API implementation and remaining release checks.

## Current identity and description

Name: MynaHub Mobile. Release ID: com.holagundi.archiman.
Version: 1.2.0, code 8. API Dev uses the separate .apidev package.
Target/compile SDK 35, minimum SDK 26. Use [branding](../BRANDING.md).

Suggested short description:

Architecture practice tools with live company projects and tasks.

The company connection supports sign-in, invitations, MFA, session management,
live authorized projects, tasks, client contacts, site records and private files;
online project/task/client/site changes, task assignments and file uploads within granted permissions.
Company business data requires connectivity and stays in memory. Only the
encrypted login credential and public connection settings are retained.
Existing local practice modules remain separate. Local backup/restore is removed.
The optional local HTTPS portal still supports existing practice workflows.
Authorized project removal hides projects/tasks from live company views;
database records and audit history are retained. Private uploaded objects are also
retained after removal from live views; production retention and orphan reconciliation
remain release checks. File selection and previews use bounded memory; binary previews
run in an isolated Android process. Privacy and retention text
must reflect this behavior.

## Required before release

- Review the exact release variant, signing configuration and store assets.
- Build and verify the signed release; current device verification used API Dev.
- Verify public HTTPS API hosting and the intended company deployment.
- Publish an accurate privacy policy covering company authentication, credentials,
  client contact details, site records, user-selected uploaded documents/images,
  company API/private object-storage processing and separate local practice records.
- Reassess Play Data safety for this actual backend and build. Do not reuse the
  older blanket no-cloud/no-account/no-data-upload declarations.
- Review permissions, exports and file handling against the installed release.
- Keep package/signing continuity; do not rename compatibility IDs.

Do not publish the historic backup claims or imply every local practice module
is synchronized. Native desktop API integration remains pending. Android OS cloud
backup is disabled. The interactive file-picker check still awaits device-owner unlock.

See [current implementation](../current-implementation.md),
[live company data](../live-company-sync.md) and
[device checks](../company-api-device-checks.md).
