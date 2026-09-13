# ArchiMan complete demo company

`ArchiMan-Demo-Company.archimandb` is a fictional, password-encrypted schema-24 company package generated through ArchiMan's production export path.

Package password: `ArchiMan-Demo-2026!`

LAN portal users after import:

| Role | Username | Password |
|---|---|---|
| Admin | `admin` | `DemoAdmin#2026` |
| Editor | `architect` | `DemoEditor#2026` |
| Viewer | `siteviewer` | `DemoViewer#2026` |

The package contains four linked projects and coverage for every persisted operational table: company/client profiles, project briefs and onboarding, scope, approvals, backlog, tasks, selections, schedules, meeting minutes, inspections, daily reports, decisions, snag/NCR lifecycles, consultants, RFI/submittal/site-instruction workflows, drawings/revisions/transmittals/markup metadata, contractors and qualifications, floors/rooms/components, all five measurement formula types, every M-Book review state, immutable audit events, a company logo and managed measurement evidence.

All names, registrations, contacts and records are fictional. The package intentionally contains no rates, estimates, bills, invoices, payments, accounting, payroll, AI configuration or statutory-compliance claims.

Regenerate and validate from the repository root:

```powershell
$env:ARCHIMAN_DEMO_OUTPUT = "$PWD\demo\ArchiMan-Demo-Company.archimandb"
$env:ARCHIMAN_DEMO_PASSWORD = "ArchiMan-Demo-2026!"
.\gradlew.bat testDebugUnitTest --tests com.example.company.DemoCompanyPackageGeneratorTest
Remove-Item Env:ARCHIMAN_DEMO_OUTPUT, Env:ARCHIMAN_DEMO_PASSWORD
```
