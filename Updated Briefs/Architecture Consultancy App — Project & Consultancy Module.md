# Architecture Consultancy App — Project & Consultancy Module

## 1. Purpose

Create a structured **Project & Consultancy Management Module** for an architecture consultancy application.

The module must allow the consultancy to:

- Create and manage projects.
- Define the type and scope of consultancy.
- Define project type and project information.
- Configure professional fee structures.
- Track project phases.
- Track execution by construction level.
- Track drawings and approvals.
- Manage project backlogs.
- Track overall project status and progress.
- Provide a dashboard showing the current state of each project.

The design should support future expansion into:

- Tasks
- Team assignment
- Drawing/document management
- Drawing revisions
- Site visits
- Client communication
- Billing
- Payments
- Project timelines
- Reports

---

# 2. Core Data Hierarchy

The project module should follow this hierarchy:

```text
PROJECT
│
├── Project Information
│
├── Consultancy
│   ├── Consultancy Type
│   ├── Scope
│   └── Fee Structure
│
├── Project Phases
│   ├── Design
│   ├── Execution
│   │   ├── Foundation
│   │   ├── Level 0
│   │   ├── Level 1
│   │   └── Level N
│   └── Handover
│
├── Drawings
│
├── Client Approvals
│
├── Technical Approvals
│
├── Backlogs
│
├── Tasks
│
├── Documents
│
└── Finance
    ├── Fees
    ├── Invoices
    ├── Receipts
    └── Outstanding
```

Do **not** implement all of these as fields in a single project table.

Use related entities/tables so the system can scale.

---

# 3. Project Entity

## Project Fields

| Field | Type | Required | Description |
|---|---|---:|---|
| `id` | UUID | Yes | Unique project ID |
| `project_name` | Text | Yes | Project name |
| `project_code` | Text | Yes | Unique project reference |
| `client_id` | UUID | Yes | Reference to client |
| `consultancy_type_id` | UUID | Yes | Type of consultancy |
| `project_type_id` | UUID | Yes | Type of project |
| `project_area` | Decimal | No | Project area |
| `area_unit` | Enum | No | Default: Sq.ft |
| `project_cost` | Decimal | No | Estimated/project cost |
| `location` | Text | No | Project location |
| `description` | Text | No | Project description |
| `status` | Enum | Yes | Overall project status |
| `created_at` | Timestamp | Yes | Creation date |
| `updated_at` | Timestamp | Yes | Last modification |

---

# 4. Consultancy Type

## Primary Consultancy Types

Provide the following predefined options:

```text
Architectural
Landscaping
Part Consultancy
Miscellaneous
```

These should be stored as configurable master data rather than hard-coded UI strings.

## Consultancy Scope

For `Part Consultancy`, allow the user to define/select the specific services included.

Suggested scope options:

```text
Concept Design
Preliminary Design
Architectural Design
Design Development
Approval Drawings
Working Drawings
Detailed Drawings
Interior Design
Landscape Design
Structural Coordination
MEP Coordination
Site Supervision
Project Management
Other
```

The system should allow multiple scope items to be selected for one project.

Example:

```text
Consultancy Type:
Part Consultancy

Scope:
✓ Working Drawings
✓ Approval Drawings
✓ Site Supervision
```

---

# 5. Project Type

Provide the following project types:

```text
Residential
Commercial
Healthcare
Hospitality
Education
Others
```

`Others` should allow a custom project-type description.

Example:

```text
Project Type: Others
Custom Type: Religious Building
```

---

# 6. Project Area

Project area should be numeric.

```text
Project Area: 25,000
Unit: Sq.ft
```

Initial supported unit:

```text
Sq.ft
```

The database should allow additional units in the future.

---

# 7. Project Cost

Store the project cost as a numeric currency value.

Example:

```text
Project Cost:
₹ 2,50,00,000
```

Do not store formatted currency strings in the database.

Store:

```text
25000000
```

and format it at the UI layer.

---

# 8. Location

Location should support:

```text
Address
City
State
Country
```

Minimum implementation:

```text
location_text
```

Future implementation may add:

```text
latitude
longitude
```

for map integration.

---

# 9. Fee Structure

The system must support three fee calculation methods.

```text
Sq.ft Basis
Percentage Basis
Lump Sum
```

## 9.1 Sq.ft Basis

Fields:

```text
fee_basis = SQFT

project_area = 25000
rate_per_sqft = 25

total_fee = project_area × rate_per_sqft
```

Example:

```text
25,000 Sq.ft × ₹25
= ₹6,25,000
```

---

## 9.2 Percentage Basis

Fields:

```text
fee_basis = PERCENTAGE

project_cost = ₹2,50,00,000
fee_percentage = 2%

total_fee = project_cost × fee_percentage / 100
```

Example:

```text
₹2,50,00,000 × 2%
= ₹5,00,000
```

---

## 9.3 Lump Sum

Fields:

```text
fee_basis = LUMP_SUM

agreed_fee = ₹8,00,000
```

No automatic calculation is required.

---

# 10. Fee Entity

Recommended structure:

```text
PROJECT
   │
   └── PROJECT_FEE
```

## Project Fee Fields

| Field | Type |
|---|---|
| `id` | UUID |
| `project_id` | UUID |
| `fee_basis` | Enum |
| `project_area` | Decimal |
| `rate_per_sqft` | Decimal |
| `project_cost` | Decimal |
| `fee_percentage` | Decimal |
| `agreed_fee` | Decimal |
| `calculated_fee` | Decimal |
| `final_fee` | Decimal |
| `created_at` | Timestamp |
| `updated_at` | Timestamp |

Only relevant fields should be displayed based on the selected fee basis.

---

# 11. Fee Summary

Every project should display:

```text
Total Consultancy Fee
Amount Received
Amount Outstanding
```

Calculation:

```text
Outstanding =
Total Consultancy Fee - Amount Received
```

Example:

```text
Total Fee        ₹8,00,000
Received         ₹3,00,000
Outstanding      ₹5,00,000
```

Finance should eventually be separated into invoices and payment records.

---

# 12. Project Phases

The project must support three primary phases:

```text
Design
Execution
Handover
```

These are hierarchical phases.

---

# 13. Design Phase

Recommended default design stages:

```text
Concept
Preliminary Design
Design Development
Approval Drawings
Working Drawings
Detailed Drawings
Issued for Construction
```

Each stage must be independently trackable.

## Design Stage Entity

```text
DESIGN_STAGE
```

Fields:

| Field | Type |
|---|---|
| `id` | UUID |
| `project_id` | UUID |
| `name` | Text |
| `sequence` | Integer |
| `status` | Enum |
| `start_date` | Date |
| `due_date` | Date |
| `completion_date` | Date |
| `remarks` | Text |

---

# 14. Execution Phase

Execution is different from Design.

Execution must be tracked by **construction level**.

Default structure:

```text
Foundation
Level 0
Level 1
Level 2
Level 3
...
Level N
```

Do not hard-code the number of levels.

Each project should be able to define its own execution levels.

Example:

```text
Project A

Foundation
Ground Floor
First Floor
Second Floor
Terrace
```

Another project:

```text
Project B

Foundation
Basement
Level 0
Level 1
Level 2
Level 3
Level 4
Roof
```

---

# 15. Execution Level Entity

```text
EXECUTION_LEVEL
```

Fields:

| Field | Type |
|---|---|
| `id` | UUID |
| `project_id` | UUID |
| `name` | Text |
| `level_number` | Integer |
| `sequence` | Integer |
| `status` | Enum |
| `start_date` | Date |
| `target_date` | Date |
| `completion_date` | Date |
| `remarks` | Text |

Example:

```text
Foundation       Complete
Level 0          Complete
Level 1          In Progress
Level 2          Yet to Start
Level 3          Yet to Start
```

---

# 16. Handover Phase

Handover should initially be a project phase that can contain tasks/checklists.

Suggested default checklist:

```text
Final Drawings
As-Built Drawings
Completion Documents
Client Documentation
Maintenance Documentation
Final Inspection
Final Handover
```

The checklist should be configurable.

---

# 17. Status System

Use a common status enum across projects, phases, stages, levels, tasks, drawings and approvals where applicable.

```text
YET_TO_START
IN_PROGRESS
ON_HOLD
COMPLETE
```

UI labels:

```text
Yet to Start
In Progress
On Hold
Complete
```

Do not use multiple variations such as:

```text
Hold
On Hold
Paused
Pending Hold
```

Use one standard value:

```text
ON_HOLD
```

---

# 18. Drawings

Drawings should be a separate entity.

```text
PROJECT
   │
   └── DRAWINGS
```

Each drawing should support:

| Field | Description |
|---|---|
| `drawing_number` | Drawing reference |
| `drawing_name` | Drawing title |
| `drawing_type` | Plan / Section / Elevation / Detail / etc. |
| `phase_id` | Associated project phase |
| `level_id` | Associated execution level |
| `revision` | Drawing revision |
| `status` | Drawing status |
| `assigned_to` | Responsible person |
| `due_date` | Target date |
| `issued_date` | Issue date |
| `remarks` | Notes |

Possible drawing statuses:

```text
Yet to Start
In Progress
For Review
Issued
Revision Required
Complete
```

---

# 19. Client Approvals

Client approvals must be tracked separately.

```text
PROJECT
   │
   └── CLIENT_APPROVAL
```

Examples:

```text
Elevation
Floor Plan
Material Selection
Colour Scheme
Landscape Proposal
Interior Layout
Furniture Selection
```

Fields:

| Field | Type |
|---|---|
| `id` | UUID |
| `project_id` | UUID |
| `description` | Text |
| `phase_id` | UUID |
| `level_id` | UUID / Nullable |
| `status` | Enum |
| `submitted_date` | Date |
| `approved_date` | Date |
| `remarks` | Text |

Suggested statuses:

```text
Pending
Submitted
Approved
Rejected
Revision Required
```

---

# 20. Technical Approvals

Technical approvals should be tracked independently from client approvals.

Examples:

```text
Structural Drawings
Electrical Drawings
Plumbing Drawings
HVAC Drawings
Fire Safety
MEP Coordination
Structural Coordination
Authority/Technical Approval
```

Entity:

```text
TECHNICAL_APPROVAL
```

Fields should follow the same general structure as client approvals.

---

# 21. Backlogs

Backlogs are items preventing or delaying project progress.

Backlogs should **not** simply be another project status.

Create a dedicated entity:

```text
BACKLOG
```

## Backlog Categories

Initial categories:

```text
Drawings
Client Approvals
Technical Approvals
```

Future categories may include:

```text
Site
Client Information
Consultant Coordination
Material Selection
Contractor
Payment
Authority
Other
```

---

# 22. Backlog Fields

| Field | Type |
|---|---|
| `id` | UUID |
| `project_id` | UUID |
| `category` | Enum |
| `title` | Text |
| `description` | Text |
| `phase_id` | UUID |
| `level_id` | UUID / Nullable |
| `assigned_to` | UUID / Nullable |
| `priority` | Enum |
| `due_date` | Date |
| `status` | Enum |
| `created_at` | Timestamp |
| `completed_at` | Timestamp |
| `remarks` | Text |

---

# 23. Backlog Priority

Use:

```text
Low
Medium
High
Critical
```

---

# 24. Backlog Status

Use:

```text
Open
In Progress
Complete
Cancelled
```

---

# 25. Example Backlog

```text
Category:
Drawings

Title:
Level 1 Toilet Detail

Phase:
Design

Level:
Level 1

Priority:
High

Due Date:
15 September 2026

Assigned To:
Architect A

Status:
Open
```

---

# 26. Project Dashboard

Each project should have a dashboard.

Example:

```text
┌──────────────────────────────────────────────┐
│ PROJECT NAME                         ● ACTIVE │
│ Client Name                                 │
│ Residential • 25,000 Sq.ft                  │
│ Bengaluru                                   │
├──────────────────────────────────────────────┤
│                                              │
│ DESIGN        EXECUTION       HANDOVER       │
│   75%             42%             0%         │
│                                              │
├──────────┬──────────┬──────────┬─────────────┤
│ DRAWINGS │ CLIENT   │ TECHNICAL│ BACKLOGS    │
│   18     │    4     │    2     │     7       │
├──────────┴──────────┴──────────┴─────────────┤
│                                              │
│ CURRENT EXECUTION                            │
│                                              │
│ Foundation             ✓ Complete            │
│ Level 0                ✓ Complete            │
│ Level 1                ● In Progress         │
│ Level 2                ○ Yet to Start        │
│ Level 3                ○ Yet to Start        │
│                                              │
└──────────────────────────────────────────────┘
```

---

# 27. Project Progress Calculation

The system should calculate progress automatically.

## Phase Progress

Example:

```text
7 design stages

5 Complete
1 In Progress
1 Yet to Start
```

Progress:

```text
5 / 7 × 100
= 71.4%
```

The exact weighting system should remain configurable for future implementation.

---

# 28. Execution Progress

For execution levels:

```text
5 levels

Foundation      Complete
Level 0         Complete
Level 1         Complete
Level 2         In Progress
Level 3         Yet to Start
```

Basic progress:

```text
3 / 5 = 60%
```

Future implementation may use weighted progress because Foundation, structural floors, finishing and handover may not represent equal percentages.

---

# 29. Project Status

Project status should be automatically suggested from phase/level status but should allow authorized users to override it if required.

Primary statuses:

```text
Yet to Start
In Progress
On Hold
Complete
```

Example rules:

```text
If all project work = Yet to Start
→ Project = Yet to Start

If any active work exists
→ Project = In Progress

If project is deliberately paused
→ Project = On Hold

If all required phases are complete
→ Project = Complete
```

---

# 30. Project Creation Workflow

The Create Project screen should follow this sequence:

```text
1. Basic Information
        ↓
2. Consultancy
        ↓
3. Fee Structure
        ↓
4. Project Type & Area
        ↓
5. Project Phases
        ↓
6. Execution Levels
        ↓
7. Initial Tasks / Backlogs
        ↓
8. Create Project
```

---

# 31. Create Project — Basic Form

```text
Project Name
[____________________________]

Project Code
[____________________________]

Client
[ Select Client ▼ ]

Consultancy Type
[ Architectural ▼ ]

Project Type
[ Residential ▼ ]

Project Area
[____________] Sq.ft

Project Cost
₹ [________________]

Location
[____________________________]

Description
[____________________________]
[____________________________]
```

---

# 32. Consultancy Form

```text
Consultancy Type

○ Architectural
○ Landscaping
○ Part Consultancy
○ Miscellaneous
```

If:

```text
Part Consultancy
```

is selected:

```text
Select Scope

☐ Concept Design
☐ Architectural Design
☐ Working Drawings
☐ Approval Drawings
☐ Site Supervision
☐ Project Management
☐ Landscape
☐ Interior
☐ MEP Coordination
☐ Other
```

---

# 33. Fee Form

```text
Fee Structure

○ Sq.ft Basis
○ Percentage Basis
○ Lump Sum
```

### Sq.ft

```text
Project Area     25,000 Sq.ft

Rate / Sq.ft     ₹25

Total Fee        ₹6,25,000
```

### Percentage

```text
Project Cost     ₹2,50,00,000

Fee %            2%

Total Fee        ₹5,00,000
```

### Lump Sum

```text
Agreed Fee       ₹8,00,000
```

---

# 34. Execution Level Setup

During project creation:

```text
Execution Levels

☑ Foundation

☑ Level 0
☑ Level 1
☑ Level 2
☑ Level 3

[ + Add Level ]
```

Allow custom names.

Example:

```text
[ + Add Level ]

Level Name:
[ Basement 1 ]

[Save]
```

---

# 35. Navigation

Recommended project navigation:

```text
Project
│
├── Overview
├── Consultancy
├── Design
├── Execution
├── Handover
├── Drawings
├── Client Approvals
├── Technical Approvals
├── Backlogs
├── Tasks
├── Documents
└── Finance
```

The Overview page should provide the high-level dashboard.

---

# 36. Database Relationship

Recommended relational model:

```text
CLIENT
  │
  └── PROJECT
       │
       ├── PROJECT_CONSULTANCY
       │
       ├── PROJECT_FEE
       │
       ├── PROJECT_PHASE
       │      │
       │      ├── DESIGN_STAGE
       │      │
       │      ├── EXECUTION_LEVEL
       │      │
       │      └── HANDOVER_STAGE
       │
       ├── DRAWING
       │
       ├── CLIENT_APPROVAL
       │
       ├── TECHNICAL_APPROVAL
       │
       ├── BACKLOG
       │
       ├── TASK
       │
       ├── DOCUMENT
       │
       └── FINANCE
```

---

# 37. Important Implementation Rules

## Rule 1 — Do Not Hard-Code Project Levels

Do not create:

```text
level_0
level_1
level_2
level_3
```

as columns.

Use:

```text
execution_levels
```

with one record per level.

---

## Rule 2 — Do Not Store Multiple Backlogs in One Text Field

Avoid:

```text
backlogs =
"Drawing pending, approval pending, structural drawing pending"
```

Instead:

```text
BACKLOG
BACKLOG
BACKLOG
```

Each item must be independently trackable.

---

## Rule 3 — Do Not Store Fee Calculations as Formatted Text

Avoid:

```text
"₹6,25,000"
```

Store:

```text
625000
```

and format it in the interface.

---

## Rule 4 — Use IDs for Relationships

Use:

```text
project_id
client_id
phase_id
level_id
assigned_to
```

instead of storing names repeatedly.

---

## Rule 5 — Master Data Must Be Configurable

The following should eventually be manageable through an admin/master-data interface:

```text
Consultancy Types
Project Types
Design Stages
Execution Level Types
Drawing Types
Approval Types
Backlog Categories
Statuses
Priorities
```

---

# 38. Future Expansion

The data model should be designed so the following can be added without restructuring the Project entity:

```text
Team Management
Task Management
Time Tracking
Site Visits
Meeting Records
Drawing Revision Management
Document Management
Client Portal
Contractor Management
Consultant Management
Invoices
Payment Tracking
Expense Tracking
Purchase Orders
Reports
Project Analytics
Notifications
Calendar
Email Integration
WhatsApp Integration
```

---

# 39. Minimum Viable Implementation

The first version must implement:

### Project

- Project Name
- Project Code
- Client
- Consultancy Type
- Project Type
- Project Area
- Project Cost
- Location
- Description
- Status

### Consultancy

- Architectural
- Landscaping
- Part Consultancy
- Miscellaneous
- Scope selection

### Fees

- Sq.ft
- Percentage
- Lump Sum
- Total Fee
- Received
- Outstanding

### Phases

- Design
- Execution
- Handover

### Execution

- Foundation
- Custom Level 0 → N

### Status

- Yet to Start
- In Progress
- On Hold
- Complete

### Backlogs

- Drawings
- Client Approvals
- Technical Approvals
- Priority
- Assignment
- Due Date
- Status

### Dashboard

- Phase progress
- Execution progress
- Drawing count
- Client approval count
- Technical approval count
- Backlog count
- Current project status

---

# 40. UX Principle

The module should feel like an **architecture practice management system**, not a generic project-management application.

The primary question on opening a project should immediately be:

> **Where is this project now, what is pending, and what needs attention?**

Therefore the project overview should prioritize:

```text
CURRENT PHASE
CURRENT LEVEL
PROGRESS
PENDING DRAWINGS
PENDING CLIENT APPROVALS
PENDING TECHNICAL APPROVALS
BACKLOGS
FEE STATUS
```

Detailed records should be one level deeper.

The system should avoid overwhelming the user with every available field on the initial project screen.