# Vishwakarma Architect OS Consolidation

> Current baseline: ArchiMan schema 19. Archi Launcher remains a separate application boundary.

## Product decision

ArchiMan is the authoritative construction-data application. Archi Launcher is a separate application and repository boundary. The older Vishwakarma standalone app suite remains a reference and migration source, not a second production database.

## Capability ownership

| Capability | Previous owner | Combined owner | Decision |
| --- | --- | --- | --- |
| Projects | ArchiMan and Vishwakarma Projects | ArchiMan Room database | Retire duplicate project store after migration tooling exists |
| Measurement Book | ArchiMan and Vishwakarma Measurement | ArchiMan | Retire the simplified standalone measurement APK |
| PWD catalogue, formulas, rate books | ArchiMan | ArchiMan | Preserve |
| Tasks | ArchiMan and Architect OS TaskStore | ArchiMan project tasks | ArchiMan owns construction tasks; a future launcher may consume a deliberate read-only contract |
| Schedule | Architect OS EventStore | ArchiMan project schedule | Imported into Room and shown in Project Planning |
| Material selection / PO | ArchiMan | ArchiMan Planning | Preserve existing quantity-only PO export |
| Meeting minutes | Vishwakarma shared bundle | ArchiMan Reports | Imported as a structured project record |
| Site inspection reports | Missing | ArchiMan Reports | Added with severity, corrective action, status and optional photo |
| Pomodoro | Fixed Launcher and Architect OS | Separate Archi Launcher app | Excluded from ArchiMan |
| Architect calculator | Architect OS Tools page | Separate Archi Launcher app | Excluded from ArchiMan |
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
└── Explicit links into ArchiMan
```

Opening a project enters the ArchiMan project workspace:

```text
Project
├── Overview
├── Brief & Scope
├── Planning
│   ├── Tasks
│   ├── Schedule
│   └── Material Selections / Purchase Order
└── More
    ├── Drawings
    ├── Site Reports
    │   ├── Meeting Minutes
    │   └── Site Inspections
    ├── Project Team
    └── Rate Books
```

## Data migration policy

1. The ArchiMan Room database remains authoritative.
2. Existing `/sdcard/Vishwakarma/os-bundle.json` data must be imported through an explicit preview-and-confirm migration, never read as a competing live database.
3. Project matching must use a user-confirmed mapping; names alone are insufficient.
4. Imported records retain their original timestamps and receive a migration source marker in the future audit schema.
5. Standalone APKs are removed only after record counts and sample content are verified in ArchiMan and a backup is produced.

## Delivered foundation

- Schema 15 project schedules, meeting minutes and site inspections.
- Schema 16 drawing register, revision, transmittal and markup metadata foundation.
- Schema 17 company and project profiles plus the PIN-authenticated local portal.
- Schema 18 project consultancy profile and Brief & Scope register.
- Planning and Site Reports hierarchy under the current project navigation.
- Optional inspection photographs.
- Existing ArchiMan selection-list purchase-order export retained.
- Launcher, Pomodoro and calculator code removed from ArchiMan.

## Next consolidation stages

1. Add previewed import from the Vishwakarma JSON bundle for projects, tasks, schedules and minutes.
2. Add export/print templates for meeting minutes and site inspection reports.
3. Add notifications for scheduled activities and overdue corrective actions.
4. Define a narrow deep-link/read-only contract for the separate launcher without sharing ArchiMan's Room database.
5. Verify migrated data, create a backup, then retire only the duplicate Vishwakarma Projects and Measurement APKs.
