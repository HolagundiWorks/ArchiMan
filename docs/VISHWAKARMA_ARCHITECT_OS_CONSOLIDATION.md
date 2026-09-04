# Vishwakarma Architect OS Consolidation

## Product decision

AMB is the authoritative construction-data application. Archi Launcher is a separate application and repository boundary. The older Vishwakarma standalone app suite remains a reference and migration source, not a second production database.

## Capability ownership

| Capability | Previous owner | Combined owner | Decision |
| --- | --- | --- | --- |
| Projects | AMB and Vishwakarma Projects | AMB Room database | Retire duplicate project store after migration tooling exists |
| Measurement Book | AMB and Vishwakarma Measurement | AMB | Retire the simplified standalone measurement APK |
| PWD catalogue, formulas, rate books | AMB | AMB | Preserve |
| Tasks | AMB and Architect OS TaskStore | AMB project tasks | AMB owns construction tasks; a future launcher may consume a deliberate read-only contract |
| Schedule | Architect OS EventStore | AMB project schedule | Imported into Room and shown in Project Planning |
| Material selection / PO | AMB | AMB Planning | Preserve existing quantity-only PO export |
| Meeting minutes | Vishwakarma shared bundle | AMB Reports | Imported as a structured project record |
| Site inspection reports | Missing | AMB Reports | Added with severity, corrective action, status and optional photo |
| Pomodoro | Fixed Launcher and Architect OS | Separate Archi Launcher app | Excluded from AMB |
| Architect calculator | Architect OS Tools page | Separate Archi Launcher app | Excluded from AMB |
| Billing, invoices, payments | Architect Books | None | Deliberately excluded |

## Separate launcher boundary

```text
Archi Launcher application (separate project)
├── Desk
│   ├── Clock and active project
│   ├── Pomodoro widget
│   ├── Project task widget
│   └── Project schedule widget
├── Architect Calculator
└── Explicit links into AMB
```

Opening a project enters the construction workspace:

```text
Project Hub
├── Overview
├── Planning
│   ├── Tasks
│   ├── Schedule
│   └── Material Selections / Purchase Order
├── Reports
│   ├── Meeting Minutes
│   └── Site Inspections
├── Team
└── Rate Books
```

## Data migration policy

1. The AMB Room database remains authoritative.
2. Existing `/sdcard/Vishwakarma/os-bundle.json` data must be imported through an explicit preview-and-confirm migration, never read as a competing live database.
3. Project matching must use a user-confirmed mapping; names alone are insufficient.
4. Imported records retain their original timestamps and receive a migration source marker in the future audit schema.
5. Standalone APKs are removed only after record counts and sample content are verified in AMB and a backup is produced.

## Delivered foundation

- Schema 15 project schedules, meeting minutes and site inspections.
- Planning and Reports hierarchy.
- Optional inspection photographs.
- Existing AMB selection-list purchase-order export retained.
- Launcher, Pomodoro and calculator code removed from AMB.

## Next consolidation stages

1. Add previewed import from the Vishwakarma JSON bundle for projects, tasks, schedules and minutes.
2. Add export/print templates for meeting minutes and site inspection reports.
3. Add notifications for scheduled activities and overdue corrective actions.
4. Define a narrow deep-link/read-only contract for the separate launcher without sharing AMB's Room database.
5. Verify migrated data, create a backup, then retire only the duplicate Vishwakarma Projects and Measurement APKs.
