# ArchiMan Application Structure and Hierarchy

The Android app is the offline field client and authoritative local server. The authenticated Carbon-based web portal is the primary office ERP surface for portfolio administration, planning, registers, coordination, review and high-volume entry. Both surfaces use the same phone database; there is no browser-side replica.

> Current baseline: ArchiMan — Architectural Consultancy Management App, Room schema 24, reviewed 13 September 2026.

## User-facing hierarchy

```text
Portfolio
├── Projects
│   └── Selected Project
│       ├── Project Overview
│       │   └── Project Profile
│       ├── Work Catalogue
│       ├── Record Measurement
│       ├── Measurement Book
│       └── More
│           ├── Brief / Scope
│           ├── Planning
│       │   │   ├── Tasks
│       │   │   ├── Schedule
│       │   │   └── Specification / Selection List
│           ├── Onboarding / Approvals / Backlog
│       │       ├── Drawings
│       │       │   ├── DWG Viewer / Markup / Measurement
│       │       │   ├── Drawing Register / Revisions
│       │       │   └── Transmittals
│       │       ├── Site Reports
│       │       │   ├── Meeting Minutes
│       │       │   └── Site Inspections
│       │       ├── Team / Contractors
│       │       ├── Coordination
│       │       │   ├── RFIs / Submittals / Site Instructions
│       │       │   └── Consultants / Responsibility Matrix
│       │       ├── Field Control
│       │       │   ├── Daily Reports
│       │       │   └── Snags / NCRs
│       │       └── Handover / As-built Package
├── Contacts
│   ├── Clients
│   └── Contractors
│       └── Contractor → Type → Qualified Work Items
├── Library
│   └── PWD SR → Work Type → Work Item → UOM / Formula
└── Practice
    ├── Company / Practice Profile, Logo, Profile Transfer, Encrypted Company Database and Supabase Connection
    └── Local Wi-Fi Workspace / User Access / Support Diagnostics
```

The bottom navigation represents stable hierarchy levels, not a second copy of project sections:

- At portfolio level: Projects, Contacts, Library and Practice.
- Inside a project: Project, Work, Record, M-Book and More. Portfolio exit uses the project header back action.
- There is no competing project tab row. Brief, planning and specialist registers are grouped in one labelled More hierarchy.
- Company profile, logo/profile transfer, encrypted whole-company database portability, Supabase configuration and the LAN workspace are grouped under Practice; measurement exports remain inside M-Book. Database import/export is intentionally unavailable through the browser workspace.
- M-Book and Record remain persistent project actions and are not repeated inside the Overview menu.
- Launcher, Pomodoro and calculator functionality belongs to the separate Archi Launcher application and is not part of ArchiMan.

## Project creation contract

Project creation establishes identity only: project name, optional project code, linked client, project type and site address. The app creates one Ground Floor baseline so measurement entry has a valid level, without asking users to define the whole building during onboarding. Additional floors belong to measurement setup; contractors belong to Project Team and are selected contractor-first when recording. Dates, areas, status, description and architect-in-charge remain editable in Project Profile.

## Data ownership

- PWD SR owns the canonical item identity, specification, unit and formula.
- A measurement sheet owns member rows.
- Each saved measurement snapshots dimensions, quantity, formula, UOM, work item, contractor, floor and date.
- Schema 22 contains no commercial tables or measurement columns; older databases are upgraded through preserving migrations.

## Code organization target

```text
com.example
├── data
│   ├── local
│   │   ├── entity        one entity file per business area
│   │   ├── dao           one DAO per business area
│   │   └── migration     versioned, tested migrations
│   ├── repository        repositories split by feature
│   └── attachments
├── domain
│   ├── catalog
│   ├── measurement
│   └── project
└── ui
    ├── navigation        destinations and hierarchy rules
    ├── project
    ├── measurement
    ├── catalog
    ├── contractor
    └── shared
```

## UI composition contract

- The app shell owns portfolio and project-level navigation; feature screens do not create competing navigation systems.
- Portfolio destinations are Projects, Contacts, Library and Practice. Project destinations are Project, Work, Record, M-Book and More.
- A feature screen uses Material 3 primitives directly: `Scaffold`, app bars, navigation bars, `ListItem`, cards, buttons, fields, chips, dialogs and bottom sheets.
- Shared composables may package repeated app behavior, accessibility or navigation, but must not recreate Material components or introduce custom color, typography, shape or elevation systems.
- Screen-specific sections remain close to their feature until a genuinely reused component emerges.
- Adaptive behavior is driven by available width and Material guidance; the 360 dp phone remains the minimum acceptance viewport.

## Refactoring rules

1. A screen renders state and sends user actions; it does not directly query the database.
2. A feature ViewModel coordinates one business area instead of accumulating every app action.
3. Cross-feature rules live in domain services and remain unit-testable.
4. Database entities are persistence models; UI-specific state uses separate models.
5. Navigation destinations live in the navigation package, never inside a ViewModel.
6. New screens must have exactly one parent in the user-facing hierarchy.
7. Existing database migrations remain additive and backward-compatible during reorganization.

## Incremental refactoring sequence

1. Establish navigation hierarchy and remove duplicate project destinations. Completed and consolidated into one navigation model in the schema-24 baseline.
2. Split the monolithic `SiteViewModel` into project, measurement and catalogue coordinators.
3. Split `ProjectWorkspaceScreen` into overview, planning and team feature files.
4. Split `Entities.kt`, `SiteDaos.kt` and `SiteRepository.kt` by business area without changing the schema.
5. Replace broad global streams with project-scoped queries and immutable UI state.
6. Add navigation, repository and migration tests for every hierarchy boundary.

This sequence deliberately preserves the working Room database and installed user data while improving maintainability in reviewable stages.
