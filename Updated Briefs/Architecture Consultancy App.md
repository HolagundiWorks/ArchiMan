# Architecture Consultancy App
## Consultancy Templates, Scope of Work & Agreement Module

---

# 1. Purpose

Add a **Consultancy Template System** to the Architecture Consultancy App.

The purpose is to allow the practice to create predefined consultancy templates containing:

- Consultancy type
- Scope of work
- Deliverables
- Project phases
- Fee structure
- Payment schedule
- Exclusions
- Client responsibilities
- Architect/consultant responsibilities
- Agreement clauses
- Agreement document template

When creating a new project, the user should be able to select a template and automatically populate the project with the appropriate consultancy scope, phases and fee structure.

---

# 2. Overall Architecture

The system should follow:

```text
CONSULTANCY TEMPLATE
│
├── Consultancy Type
├── Project Type
├── Scope of Work
├── Deliverables
├── Project Phases
├── Fee Structure
├── Payment Schedule
├── Exclusions
├── Client Responsibilities
├── Consultant Responsibilities
├── Terms & Conditions
└── Agreement Template
          │
          ↓
       NEW PROJECT
          │
          ├── Project Information
          ├── Consultancy
          ├── Scope
          ├── Fee
          ├── Phases
          └── Agreement
```

---

# 3. Template Types

The system should support templates based on the type of professional service.

Initial templates:

```text
Architectural Consultancy
Landscape Consultancy
Part Consultancy
Miscellaneous Consultancy
```

---

# 4. Architectural Consultancy Templates

Create configurable templates for different levels of architectural service.

Suggested default templates:

```text
Full Architectural Consultancy

Architectural Design Consultancy

Design + Working Drawings

Design + Site Supervision

Approval + Working Drawings

Working Drawing Consultancy

Site Supervision Consultancy

Project Management Consultancy

Part Architectural Consultancy
```

These are templates and should be editable by the practice.

Do not permanently hard-code fee percentages or clauses into the application.

---

# 5. Landscape Consultancy Templates

Suggested templates:

```text
Full Landscape Consultancy

Landscape Design Consultancy

Landscape Design + Working Drawings

Landscape Design + Site Supervision

Landscape Execution Consultancy
```

---

# 6. Part Consultancy Templates

Part consultancy should be highly configurable.

Example:

```text
Part Consultancy — Working Drawings

Scope:
✓ Architectural Working Drawings
✓ Detailed Drawings
✓ Door/Window Schedule

Excluded:
✗ Concept Design
✗ Site Supervision
✗ Structural Design
✗ MEP Design
```

Another:

```text
Part Consultancy — Site Supervision

Scope:
✓ Periodic Site Visits
✓ Workmanship Review
✓ Drawing Clarifications
✓ Site Instructions

Excluded:
✗ Contractor Management
✗ Material Procurement
✗ Project Cost Management
```

---

# 7. Template Entity

Create:

```text
CONSULTANCY_TEMPLATE
```

Fields:

| Field | Type |
|---|---|
| `id` | UUID |
| `template_name` | Text |
| `template_code` | Text |
| `consultancy_type` | Enum |
| `description` | Text |
| `project_types` | Relation |
| `version` | Integer |
| `status` | Draft / Active / Archived |
| `created_at` | Timestamp |
| `updated_at` | Timestamp |

---

# 8. Template Versioning

Templates must support versions.

Example:

```text
Full Architectural Consultancy

Version 1
Version 2
Version 3
```

Once a template has been used to create an agreement, do not silently change the historical project agreement if the master template is later edited.

When a project is created:

```text
Template:
Full Architectural Consultancy

Template Version:
3
```

The project stores the version used.

This preserves historical agreements.

---

# 9. Template Scope of Work

Each template should contain structured scope items.

Entity:

```text
TEMPLATE_SCOPE
```

Fields:

| Field | Type |
|---|---|
| `id` | UUID |
| `template_id` | UUID |
| `category` | Text |
| `title` | Text |
| `description` | Rich Text |
| `sequence` | Integer |
| `included` | Boolean |
| `phase` | Relation |
| `deliverable` | Boolean |

---

# 10. Scope Categories

Recommended categories:

```text
Pre-Design
Concept Design
Schematic Design
Design Development
Approval
Working Drawings
Detailed Drawings
Coordination
Tender
Execution
Site Supervision
Handover
Post-Handover
```

---

# 11. Scope Example

Example template:

```text
FULL ARCHITECTURAL CONSULTANCY
```

### Pre-Design

```text
✓ Understanding client requirements
✓ Site information review
✓ Preliminary project analysis
```

### Concept Design

```text
✓ Concept development
✓ Preliminary plans
✓ Preliminary elevations
✓ Preliminary sections
✓ Design presentation
```

### Design Development

```text
✓ Developed plans
✓ Developed elevations
✓ Sections
✓ Material/finish concepts
✓ Design coordination
```

### Approval

```text
✓ Preparation of architectural approval drawings
✓ Coordination of required information
✓ Submission support
```

### Working Drawings

```text
✓ Working plans
✓ Sections
✓ Elevations
✓ Door/window details
✓ Staircase details
✓ Toilet details
✓ Typical construction details
✓ Schedules
```

### Execution

```text
✓ Construction drawing clarification
✓ Periodic site visits
✓ Design-related site instructions
✓ Review of architectural execution
```

### Handover

```text
✓ Final architectural inspection
✓ As-built documentation, if included
✓ Handover documentation
```

---

# 12. Included / Excluded Scope

Every template should support explicit inclusion and exclusion.

Example:

```text
INCLUDED

✓ Architectural design
✓ Working drawings
✓ Approval drawings
✓ Periodic site visits
```

```text
EXCLUDED

✗ Structural design
✗ Electrical design
✗ Plumbing design
✗ HVAC design
✗ Contractor appointment
✗ Material procurement
✗ Project cost management
```

This is important because the agreement should clearly distinguish:

> **What the consultant is responsible for**

from:

> **What is outside the consultant's scope.**

---

# 13. Deliverables

Each scope item can optionally generate a deliverable.

Example:

```text
Scope:
Working Drawings

Deliverables:
- Floor Plans
- Sections
- Elevations
- Door Schedule
- Window Schedule
- Staircase Details
- Toilet Details
- Construction Details
```

Entity:

```text
TEMPLATE_DELIVERABLE
```

Fields:

```text
id
template_id
scope_id
name
description
sequence
```

---

# 14. Fee Template

Each consultancy template should have a default fee structure.

Supported:

```text
Sq.ft Basis
Percentage Basis
Lump Sum
```

Entity:

```text
TEMPLATE_FEE
```

Fields:

| Field | Type |
|---|---|
| `id` | UUID |
| `template_id` | UUID |
| `fee_basis` | Enum |
| `default_rate` | Decimal |
| `default_percentage` | Decimal |
| `default_lump_sum` | Decimal |
| `currency` | Text |
| `notes` | Text |

---

# 15. Fee Structure in Template

Example:

```text
Template:
Full Architectural Consultancy

Fee Basis:
Percentage

Default Fee:
X %

Calculation Base:
Project Cost
```

Another:

```text
Template:
Working Drawing Consultancy

Fee Basis:
Sq.ft

Default Rate:
₹ XX / Sq.ft
```

Another:

```text
Template:
Landscape Consultancy

Fee Basis:
Lump Sum

Default Fee:
₹ XXXXX
```

The actual project fee must be editable after selecting the template.

---

# 16. Fee Breakdown by Phase

Templates should optionally allow fee allocation by phase.

Example:

```text
TOTAL PROFESSIONAL FEE
100%

Concept                10%
Design Development     20%
Approval               10%
Working Drawings       30%
Execution              25%
Handover                5%
```

This allows future payment milestones to be generated automatically.

---

# 17. Payment Schedule

Create:

```text
TEMPLATE_PAYMENT_MILESTONE
```

Example:

```text
At appointment                 10%
On concept approval            15%
On design development          20%
On approval submission         10%
On working drawings            30%
During execution               10%
At handover                     5%
```

The percentages must be configurable.

The system should validate:

```text
Total payment milestone %
= 100%
```

before the template is activated.

---

# 18. Agreement Template

Every consultancy template may have an associated agreement template.

Structure:

```text
CONSULTANCY TEMPLATE
        │
        └── AGREEMENT TEMPLATE
                │
                ├── Introduction
                ├── Appointment
                ├── Scope of Work
                ├── Deliverables
                ├── Fee
                ├── Payment Schedule
                ├── Exclusions
                ├── Client Responsibilities
                ├── Consultant Responsibilities
                ├── Variations
                ├── Additional Services
                ├── Site Visits
                ├── Approvals
                ├── Intellectual Property
                ├── Termination
                ├── Liability
                ├── Dispute Resolution
                └── Signatures
```

The actual legal wording should be configurable/reviewed by the practice's legal adviser.

---

# 19. Agreement Entity

Create:

```text
PROJECT_AGREEMENT
```

Fields:

| Field | Type |
|---|---|
| `id` | UUID |
| `project_id` | UUID |
| `template_id` | UUID |
| `template_version` | Integer |
| `agreement_number` | Text |
| `agreement_date` | Date |
| `status` | Draft / Sent / Signed / Active / Terminated |
| `content` | Rich Text |
| `signed_document` | File |
| `created_at` | Timestamp |
| `updated_at` | Timestamp |

---

# 20. Agreement Generation

When a project is created from a consultancy template:

```text
Select Template
       ↓
Load Scope
       ↓
Load Deliverables
       ↓
Load Fee Structure
       ↓
Load Payment Schedule
       ↓
Load Terms
       ↓
Populate Project Information
       ↓
Generate Agreement Draft
```

The user must be able to review and edit the agreement before finalization.

---

# 21. Dynamic Agreement Variables

Agreement templates should support variables.

Example:

```text
{{PROJECT_NAME}}
{{PROJECT_CODE}}
{{CLIENT_NAME}}
{{CLIENT_ADDRESS}}
{{CONSULTANT_NAME}}
{{PROJECT_TYPE}}
{{PROJECT_AREA}}
{{PROJECT_LOCATION}}
{{PROJECT_COST}}
{{CONSULTANCY_TYPE}}
{{TOTAL_FEE}}
{{FEE_BASIS}}
{{PAYMENT_SCHEDULE}}
{{AGREEMENT_DATE}}
{{AGREEMENT_NUMBER}}
```

When the agreement is generated, these variables are replaced with project information.

---

# 22. Scope Variables

Scope should also be generated dynamically.

Example:

```text
## Scope of Work

The Consultant shall provide the following services:

{{SCOPE_OF_WORK}}
```

The system inserts:

```text
1. Concept Design
2. Design Development
3. Approval Drawings
4. Working Drawings
5. Periodic Site Supervision
```

---

# 23. Fee Variables

Example agreement section:

```text
## Professional Fee

Consultancy Type:
{{CONSULTANCY_TYPE}}

Fee Basis:
{{FEE_BASIS}}

Project Area:
{{PROJECT_AREA}}

Project Cost:
{{PROJECT_COST}}

Professional Fee:
{{TOTAL_FEE}}
```

---

# 24. Agreement Scope Snapshot

Important:

When an agreement is finalized, the system must save a **snapshot** of:

- Scope
- Deliverables
- Fee
- Payment schedule
- Exclusions
- Terms
- Template version

Do not dynamically regenerate an old signed agreement from the current template.

Historical agreements must remain unchanged.

---

# 25. Scope Variation

The project may require additional work after agreement.

Create:

```text
SCOPE_VARIATION
```

Example:

```text
Original Scope:
Architectural Consultancy

Additional Service:
Interior Design

Additional Fee:
₹2,50,000
```

Status:

```text
Proposed
Approved
Rejected
Completed
```

Approved variations should optionally create:

```text
Additional Fee
Additional Scope
Additional Agreement / Addendum
```

---

# 26. Additional Services

The agreement should distinguish between:

```text
Included Services
```

and:

```text
Additional Services
```

Example:

```text
Included:
Architectural Working Drawings

Additional:
Interior Design
Landscape Design
Detailed Structural Design
3D Visualisation
Additional Site Visits
Project Management
```

Each additional service may have its own fee.

---

# 27. Template Management Screen

Admin navigation:

```text
Settings
│
└── Consultancy Templates
       │
       ├── Architectural
       ├── Landscaping
       ├── Part Consultancy
       └── Miscellaneous
```

Template list:

```text
┌──────────────────────────────────────────────┐
│ Consultancy Templates                        │
├──────────────────────────────────────────────┤
│ Full Architectural Consultancy     Active    │
│ Design + Working Drawings           Active    │
│ Site Supervision                    Active    │
│ Landscape Consultancy               Active    │
│ Part Consultancy                    Active    │
└──────────────────────────────────────────────┘

                     [ + New Template ]
```

---

# 28. Template Editor

Template editor should have tabs:

```text
General
Scope
Deliverables
Phases
Fees
Payment Schedule
Exclusions
Responsibilities
Terms
Agreement
Preview
Version History
```

---

# 29. New Project From Template

Project creation should offer:

```text
Create Project

○ Start from Blank Project
○ Use Consultancy Template
```

If template is selected:

```text
Select Consultancy Template

[ Full Architectural Consultancy ▼ ]

Template Version: 3

[Continue]
```

The system then pre-populates the project.

---

# 30. Template Override

The user must be able to modify the generated project scope without modifying the master template.

Example:

```text
MASTER TEMPLATE

Full Architectural Consultancy
      ↓
PROJECT

Full Architectural Consultancy
      +
Remove Site Supervision
      +
Add Landscape Coordination
```

The master template remains unchanged.

---

# 31. Agreement Workflow

```text
Draft
  ↓
Internal Review
  ↓
Client Review
  ↓
Revision
  ↓
Final
  ↓
Sent for Signature
  ↓
Signed
  ↓
Active
```

Possible status values:

```text
DRAFT
INTERNAL_REVIEW
CLIENT_REVIEW
REVISION_REQUIRED
FINAL
SENT
SIGNED
ACTIVE
TERMINATED
```

---

# 32. Agreement Dashboard

Project overview should display:

```text
AGREEMENT

Status:
✓ Signed

Agreement No:
ARC/2026/024

Date:
04 September 2026

Consultancy:
Architectural

Scope:
Full Architectural Consultancy

Fee:
₹8,00,000

Outstanding:
₹5,00,000
```

Actions:

```text
[View Agreement]
[Edit Draft]
[Download PDF]
[Upload Signed Copy]
[Create Variation]
```

---

# 33. Template → Project → Agreement Relationship

The final data relationship should be:

```text
CONSULTANCY_TEMPLATE
        │
        ├── SCOPE
        ├── DELIVERABLES
        ├── PHASES
        ├── FEE
        ├── PAYMENT MILESTONES
        ├── EXCLUSIONS
        ├── RESPONSIBILITIES
        └── AGREEMENT_TEMPLATE
                    │
                    ↓
                PROJECT
                    │
                    ├── PROJECT_SCOPE
                    ├── PROJECT_FEE
                    ├── PROJECT_PHASES
                    ├── PAYMENT_MILESTONES
                    │
                    └── PROJECT_AGREEMENT
                              │
                              └── SNAPSHOT
```

---

# 34. COA-Oriented Structure

The system should provide a **professional-service classification framework aligned to the categories and scope adopted by the practice and applicable professional/regulatory requirements**.

Do not hard-code legal claims or regulatory fee percentages into the application.

Instead, create configurable master data:

```text
Professional Service Category
Service Description
Scope
Deliverables
Fee Basis
Fee Rate
Agreement Clause
```

This allows the practice to update templates when professional regulations, practice standards or fee policies change.

---

# 35. Recommended Initial Template Library

Create the following starter templates:

### Architectural

```text
01 — Full Architectural Consultancy
02 — Architectural Design Consultancy
03 — Design + Working Drawings
04 — Approval Drawing Consultancy
05 — Working Drawing Consultancy
06 — Design + Site Supervision
07 — Site Supervision Consultancy
08 — Project Management Consultancy
09 — Part Architectural Consultancy
```

### Landscape

```text
10 — Full Landscape Consultancy
11 — Landscape Design Consultancy
12 — Landscape Design + Working Drawings
13 — Landscape Site Supervision
```

### Part Consultancy

```text
14 — Concept Design Only
15 — Approval Drawings Only
16 — Working Drawings Only
17 — Site Supervision Only
18 — Architectural Coordination Only
19 — Drawing Review Consultancy
```

### Miscellaneous

```text
20 — Custom Consultancy
```

---

# 36. Important Developer Principle

Templates are **starting points**, not immutable project definitions.

The correct flow is:

```text
MASTER TEMPLATE
      ↓
COPY
      ↓
PROJECT-SPECIFIC SCOPE
      ↓
PROJECT-SPECIFIC FEE
      ↓
PROJECT AGREEMENT
      ↓
AGREEMENT SNAPSHOT
```

Never maintain a live dependency between a signed agreement and the master template.

If the master template changes tomorrow, an agreement signed today must remain exactly as agreed.

---

# 37. Future Integration

This module should integrate with:

```text
Projects
Clients
CRM
Tasks
Drawings
Documents
Approvals
Backlogs
Invoices
Payments
Accounting
Digital Signatures
PDF Generation
Email
Client Portal
```

The ultimate workflow should be:

```text
CLIENT
   ↓
NEW PROJECT
   ↓
SELECT CONSULTANCY TEMPLATE
   ↓
AUTO-GENERATE SCOPE
   ↓
CONFIGURE PROJECT
   ↓
CALCULATE FEE
   ↓
GENERATE AGREEMENT
   ↓
CLIENT REVIEW
   ↓
SIGN AGREEMENT
   ↓
PROJECT ACTIVATED
   ↓
DESIGN
   ↓
EXECUTION
   ↓
HANDOVER
   ↓
FINAL PAYMENT
   ↓
PROJECT CLOSED
```

# 38. MVP Priority

Implement in this order:

### Phase 1

- Consultancy Template Master
- Template categories
- Scope of Work
- Deliverables
- Fee structure
- Exclusions

### Phase 2

- Payment milestones
- Agreement templates
- Dynamic variables
- Agreement generation
- Agreement versioning

### Phase 3

- Project creation from template
- Project-specific scope editing
- Scope variations
- Additional services
- Agreement addendums

### Phase 4

- PDF generation
- Digital signature integration
- Client portal
- Invoice/payment integration

---

# 39. Core Principle

The application should treat the **Consultancy Template + Scope + Fee + Agreement** as the commercial foundation of the project.

```text
CONSULTANCY
      │
      ├── WHAT WE DO
      │      ↓
      │   SCOPE
      │
      ├── WHAT WE DELIVER
      │      ↓
      │   DELIVERABLES
      │
      ├── HOW MUCH WE CHARGE
      │      ↓
      │   FEE
      │
      └── WHAT BOTH PARTIES AGREE TO
             ↓
          AGREEMENT
```

Once the agreement is signed, the agreed scope and fee become the **baseline against which project execution, additional work, billing and variations are tracked**.