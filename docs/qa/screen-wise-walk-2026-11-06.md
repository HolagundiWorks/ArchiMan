# Screen-wise UI walk — 6 November 2026

Automated UiAutomator walk against the installed debug APK on **SM-M115F**.

```sh
adb shell am instrument -w -e suite screenWalk com.holagundi.archiman.test/com.holagundi.archiman.BackupDeviceChecks
```

Result: **31/31 PASS** (~107 s). Demo company (Studio Holagundi / Skyline Villa Residence).

| Screen id | Marker seen | Status |
|---|---|---|
| shell.session | Practice shell | PASS |
| practice.dashboard | Projects | PASS |
| practice.projects | Skyline Villa Residence | PASS |
| practice.menu | Profile | PASS |
| practice.profile | Studio Holagundi | PASS |
| practice.clients | Clients | PASS |
| practice.invoices | Invoices | PASS |
| practice.finance | Taxes & banking | PASS |
| practice.backup | Backup and restore | PASS |
| practice.portal | Wi-Fi portal | PASS |
| practice.demo | Built-in company file | PASS |
| project.OVERVIEW | Continue your work | PASS |
| project.BRIEF | Brief | PASS |
| project.TASKS | Tasks | PASS |
| project.DOCUMENTS | Documents | PASS |
| project.LETTERS | Letters | PASS |
| project.DRAWINGS | Drawings | PASS |
| project.VISITS | Site visits | PASS |
| project.SNAGS | Snags and NCRs | PASS |
| project.COORDINATION | Coordination | PASS |
| project.SITEMAN | Daily log | PASS |
| project.MEASUREMENTS | Measurements | PASS |
| project.SETUP | Measurement and billing setup | PASS |
| project.LEVELS | sheets | PASS |
| project.BOOK | Measurement Book | PASS |
| project.MEASURE | Select contractor | PASS |
| project.PROPOSAL | Fee proposals | PASS |
| project.CONTRACT | Agreements | PASS |
| project.INVOICE | Client invoice | PASS |
| project.BILL | Contractor running bills | PASS |
| project.exit | Back to practice shell | PASS |

App lock is skipped while `ActivityManager.isRunningInTestHarness()` is true so the walk is not blocked by the device credential prompt.
