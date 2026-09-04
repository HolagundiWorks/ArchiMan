# AMB Application Structure and Hierarchy

## User-facing hierarchy

```text
Portfolio
├── Company / Practice Profile
├── Projects
│   └── Selected Project
│       ├── Project Hub
│       │   ├── Overview
│       │   │   └── Project Profile
│       │   ├── Drawings
│       │   │   ├── DWG Viewer / Markup / Measurement
│       │   │   ├── Drawing Register / Revisions
│       │   │   └── Transmittals
│       │   ├── Planning
│       │   │   ├── Tasks
│       │   │   ├── Schedule
│       │   │   └── Specification / Selection List
│       │   ├── Reports
│       │   │   ├── Meeting Minutes
│       │   │   └── Site Inspections
│       │   ├── Coordination
│       │   │   ├── RFIs / Submittals / Site Instructions
│       │   │   └── Consultants / Responsibility Matrix
│       │   ├── Field Control
│       │   │   ├── Daily Reports
│       │   │   └── Snags / NCRs
│       │   ├── Handover / As-built Package
│       │   └── Setup
│       │       ├── Team / Contractors
│       │       └── Contractor Rate Books
│       ├── PWD SR Work Catalogue
│       │   └── Work Type → Work Item → Formula
│       ├── Measurement Book
│       │   └── Sheet → Member Row → Dimensions / Quantity → Rate Snapshot
│       └── Record Measurement
│           └── Contractor → Work Item → Floor → Member Rows
├── Clients
└── Contractors
    └── Contractor → Type → Qualified Items → Versioned Rate Books
```

The bottom navigation represents stable hierarchy levels, not a second copy of Project Hub sections:

- At portfolio level: Projects, Clients, Contractors.
- Inside a project: Portfolio, Project Hub, PWD SR Catalogue, M-Book, Record.
- Project Hub contains planning and setup only. M-Book and Record are not repeated inside it.
- Launcher, Pomodoro and calculator functionality belongs to the separate Archi Launcher application and is not part of AMB.

## Data ownership

- PWD SR owns the canonical item identity, specification, unit and formula.
- A contractor owns zero or more named and versioned rate books.
- A rate-book row references a canonical PWD item and snapshots its description and unit.
- A project selects one applicable rate book per contractor.
- A measurement sheet owns member rows.
- Each saved measurement snapshots the applied rate-book ID, rate and calculated amount.
- Changing a rate book never rewrites historical measurements.

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
│   ├── ratebook
│   └── project
└── ui
    ├── navigation        destinations and hierarchy rules
    ├── project
    ├── measurement
    ├── catalog
    ├── contractor
    └── shared
```

## Refactoring rules

1. A screen renders state and sends user actions; it does not directly query the database.
2. A feature ViewModel coordinates one business area instead of accumulating every app action.
3. Cross-feature rules live in domain services and remain unit-testable.
4. Database entities are persistence models; UI-specific state uses separate models.
5. Navigation destinations live in the navigation package, never inside a ViewModel.
6. New screens must have exactly one parent in the user-facing hierarchy.
7. Existing database migrations remain additive and backward-compatible during reorganization.

## Incremental refactoring sequence

1. Establish navigation hierarchy and remove duplicate Project Hub destinations. Completed.
2. Split the monolithic `SiteViewModel` into project, measurement, catalogue and rate-book coordinators.
3. Split `ProjectWorkspaceScreen` into overview, planning, team and rates feature files.
4. Split `Entities.kt`, `SiteDaos.kt` and `SiteRepository.kt` by business area without changing the schema.
5. Replace broad global streams with project-scoped queries and immutable UI state.
6. Add navigation, repository and migration tests for every hierarchy boundary.

This sequence deliberately preserves the working Room database and installed user data while improving maintainability in reviewable stages.
