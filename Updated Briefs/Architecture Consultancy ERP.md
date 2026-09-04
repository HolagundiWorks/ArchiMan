# Architecture Consultancy ERP
## WordPress + Supabase Frontend Specification

**Version:** 1.0  
**Architecture:** WordPress Frontend + Supabase Backend  
**Custom WordPress Plugins:** **NONE**  
**Primary ERP Database:** Supabase PostgreSQL  
**Frontend Platform:** WordPress  
**Authentication:** Supabase Auth  
**File Storage:** Supabase Storage  
**Security:** Supabase Row Level Security (RLS)

---

# 1. Core Requirement

Build the complete Architecture Consultancy ERP as a **WordPress-based web application**, using WordPress only as the frontend/application hosting environment and Supabase as the backend.

The ERP must **not require development of a custom WordPress plugin**.

All ERP functionality must be implemented using standard WordPress capabilities:

- WordPress Pages
- WordPress Templates
- WordPress Theme
- Child Theme where required
- Gutenberg / Block Editor
- Custom HTML blocks
- CSS
- JavaScript
- WordPress REST API where required
- WordPress AJAX where required
- Shortcodes where supported by the selected theme/system
- Supabase JavaScript client/API
- Supabase Auth
- Supabase PostgreSQL
- Supabase Storage

The ERP data must **not be stored in WordPress posts, pages, post meta, or the WordPress database**.

Supabase is the ERP database.

---

# 2. System Architecture

```text
                         USER
                           │
                           ▼
                    WORDPRESS WEBSITE
                           │
              ┌────────────┴────────────┐
              │                         │
        WordPress Theme            JavaScript UI
              │                         │
              └────────────┬────────────┘
                           │
                    Supabase Client
                           │
                           ▼
                    SUPABASE BACKEND
                           │
          ┌────────────────┼────────────────┐
          │                │                │
      PostgreSQL          Auth           Storage
          │                │                │
       ERP DATA         LOGIN          FILES/PDFS
          │
          ▼
         RLS
   ACCESS CONTROL
```

---

# 3. WordPress Role

WordPress is the **presentation and application shell**.

WordPress provides:

- Website hosting
- Page routing
- ERP screens
- Navigation
- HTML structure
- CSS
- JavaScript
- Responsive UI
- Client portal
- Staff portal
- Dashboard
- Forms
- Reports
- Document interface

WordPress does **not** become the ERP database.

---

# 4. Supabase Role

Supabase provides:

## Database

PostgreSQL containing:

- Companies
- Users
- Roles
- Permissions
- Clients
- Projects
- Project types
- Consultancy types
- Consultancy templates
- Onboarding templates
- Project briefs
- Site data
- Requirements
- Drawings
- Approvals
- Backlogs
- Tasks
- Agreements
- Fees
- Invoices
- Payments
- Documents
- Audit logs
- Sync information

## Authentication

Supabase Auth handles:

- Login
- Logout
- Session
- Password authentication
- Password reset
- Email verification where required

## Storage

Supabase Storage handles:

- Drawings
- PDFs
- Agreements
- Site photographs
- Documents
- Reference images
- Signed documents
- Project files

## Security

Supabase RLS controls access to:

- Company data
- User data
- Project data
- Client data
- Financial data
- Documents
- Client portal data

---

# 5. No Custom Plugin Requirement

Do NOT create:

```text
architecture-erp.php
```

Do NOT create a custom ERP plugin.

Do NOT implement the ERP database inside:

```text
wp_posts
wp_postmeta
wp_options
wp_users
```

The ERP must be implemented through the WordPress frontend/theme and Supabase.

Recommended structure:

```text
WordPress
│
├── Theme
│
├── Child Theme
│
├── Pages
│
├── Templates
│
├── Gutenberg Blocks
│
├── HTML
│
├── CSS
│
└── JavaScript
       │
       ▼
    Supabase
```

---

# 6. WordPress Page Structure

Create the following WordPress pages.

```text
/login

/dashboard

/clients
/clients/new
/clients/view

/projects
/projects/new
/projects/view

/projects/brief
/projects/site
/projects/requirements

/projects/design
/projects/execution
/projects/drawings
/projects/approvals
/projects/backlogs
/projects/tasks
/projects/handover

/consultancy
/consultancy/templates
/consultancy/types
/consultancy/scope
/consultancy/deliverables
/consultancy/fees

/onboarding
/onboarding/templates
/onboarding/sessions

/agreements
/agreements/templates
/agreements/new
/agreements/view

/finance
/finance/fees
/finance/invoices
/finance/payments
/finance/outstanding

/documents

/reports

/settings
/settings/company
/settings/tax
/settings/users
/settings/roles
/settings/hierarchy
/settings/permissions
/settings/backup
/settings/sync

/client-portal
```

---

# 7. ERP Application Shell

After login, the user should enter an ERP shell rather than the normal WordPress website.

```text
┌───────────────────────────────────────────────┐
│ LOGO     Architecture ERP        User ▼      │
├──────────────┬────────────────────────────────┤
│              │                                │
│ Dashboard    │                                │
│ Clients      │         MAIN CONTENT           │
│ Projects     │                                │
│ Consultancy  │                                │
│ Agreements   │                                │
│ Finance      │                                │
│ Documents    │                                │
│ Reports      │                                │
│              │                                │
│ Administration                              │
│              │                                │
└──────────────┴────────────────────────────────┘
```

Desktop-first but responsive.

---

# 8. Login

Login page:

```text
Company Logo

Architecture ERP

Username / Email
Password

[ Login ]

Forgot Password
```

Authentication must use Supabase Auth.

After successful login:

```text
Supabase Auth
      ↓
User ID
      ↓
Company
      ↓
Role
      ↓
Permissions
      ↓
Project Access
      ↓
Dashboard
```

Never store passwords in WordPress.

Never store passwords in the browser.

---

# 9. User Management

Maximum active users for the initial system:

```text
5 users per company
```

Users are stored in Supabase.

Fields:

```text
id
company_id
username
email
display_name
role_id
manager_user_id
is_active
last_login_at
created_at
updated_at
```

---

# 10. Roles

Initial roles:

```text
OWNER
ADMIN
PRINCIPAL_ARCHITECT
ARCHITECT
PROJECT_MANAGER
DRAFTSMAN
ACCOUNTS
STAFF
CLIENT
VIEWER
```

Roles must be configurable.

---

# 11. User Hierarchy

Users can have a reporting relationship.

Example:

```text
OWNER
│
└── PRINCIPAL ARCHITECT
    │
    ├── ARCHITECT
    │   └── DRAFTSMAN
    │
    └── ACCOUNTS
```

Store:

```text
manager_user_id
```

The hierarchy must be independent from project assignments.

---

# 12. Permissions

Permissions:

```text
company.view
company.edit

users.view
users.create
users.edit
users.disable

clients.view
clients.create
clients.edit
clients.delete

projects.view
projects.create
projects.edit
projects.delete
projects.assign

templates.view
templates.create
templates.edit
templates.delete

onboarding.view
onboarding.edit
onboarding.approve

drawings.view
drawings.create
drawings.edit
drawings.issue

approvals.view
approvals.create
approvals.approve

agreements.view
agreements.create
agreements.approve

finance.view
finance.create
finance.edit

documents.view
documents.upload
documents.delete

reports.view

backup.create
backup.restore

sync.view
sync.start
sync.configure
```

---

# 13. Project Access

Users can also be assigned to individual projects.

```text
project_users

project_id
user_id
access_level
assigned_at
```

Access levels:

```text
OWNER
MANAGER
MEMBER
VIEWER
```

Effective access:

```text
ROLE PERMISSION
        +
PROJECT ACCESS
        =
FINAL ACCESS
```

---

# 14. Company Setup

First-run wizard:

```text
1. Company Information
2. Professional Information
3. Tax Configuration
4. Invoice Configuration
5. Initial User
6. Complete Setup
```

---

# 15. Company Profile

Fields:

```text
Company Name
Trade Name
Company Type
Address
City
State
Country
PIN Code
Email
Phone
Website
Logo
PAN
Professional Registration Number
Principal Name
Qualification
Practice Registration Details
```

---

# 16. Tax Configuration

Company-level tax configuration.

Options:

```text
NO_GST
COMPOSITION
REGULAR_GST
```

Initial configurable values:

```text
NO_GST        = 0%
COMPOSITION   = 6%
REGULAR_GST   = 8%
```

These rates must remain configurable and must be verified against applicable law before production use.

Tax profile:

```text
id
company_id
tax_regime
tax_rate
tax_registration_number
effective_from
effective_to
is_active
created_at
updated_at
```

---

# 17. Clients

Client management:

```text
Clients
│
├── All Clients
├── New Client
├── Client Profile
├── Projects
├── Agreements
├── Invoices
├── Payments
└── Documents
```

Client fields:

```text
Client Name
Client Type
Contact Person
Phone
Email
Address
Billing Address
Preferred Communication
Notes
```

Client types:

```text
Individual
Joint Owners
Company
Partnership
Trust
Institution
Government / Authority
Other
```

---

# 18. Project Creation

Project creation workflow:

```text
New Project
     ↓
Select Client
     ↓
Select Project Type
     ↓
Select Consultancy Type
     ↓
Load Onboarding Template
     ↓
Client Onboarding
     ↓
Project Brief
     ↓
Site Data
     ↓
Requirements
     ↓
Consultancy Proposal
     ↓
Agreement
     ↓
Project Execution
```

---

# 19. Project Master

Fields:

```text
Project Name
Project Code
Client
Project Type
Consultancy Type
Project Area
Area Unit
Project Cost
Location
City
State
Country
Latitude
Longitude
Description
Expected Start Date
Expected Completion Date
Status
```

Statuses:

```text
YET_TO_START
IN_PROGRESS
ON_HOLD
COMPLETE
```

---

# 20. Project Dashboard

Every project gets a dedicated dashboard.

```text
PROJECT
│
├── Overview
├── Client
├── Project Brief
├── Site Data
├── Requirements
├── Consultancy
├── Agreement
├── Fees
├── Design
├── Execution
├── Drawings
├── Client Approvals
├── Technical Approvals
├── Backlogs
├── Tasks
├── Documents
└── Handover
```

Dashboard should show:

```text
Current Phase
Current Level
Overall Progress
Pending Drawings
Pending Client Approvals
Pending Technical Approvals
Open Backlogs
Outstanding Fees
Recent Activity
```

---

# 21. Consultancy Types

```text
ARCHITECTURAL
LANDSCAPING
PART_CONSULTANCY
MISCELLANEOUS
```

---

# 22. Consultancy Templates

Templates include:

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

Full Landscape Consultancy
Landscape Design Consultancy
Landscape Design + Working Drawings
Landscape Design + Site Supervision
Landscape Execution Consultancy

Concept Design Only
Approval Drawings Only
Working Drawings Only
Site Supervision Only
Architectural Coordination Only
Drawing Review Consultancy

Custom Consultancy
```

Templates must be versioned.

```text
Template
   ↓
Version
   ↓
Scope
   ↓
Deliverables
   ↓
Fee
   ↓
Payment Milestones
   ↓
Agreement
```

---

# 23. Project-Type Templates

Onboarding templates:

```text
Residential
Commercial
Healthcare
Hospitality
Education
Other
```

Template engine:

```text
ONBOARDING_TEMPLATE
        ↓
ONBOARDING_SECTION
        ↓
ONBOARDING_QUESTION
        ↓
CLIENT_RESPONSE
```

---

# 24. Dynamic Forms

Supported field types:

```text
Text
Long Text
Number
Currency
Date
Yes / No
Single Select
Multi Select
Checkbox
File Upload
Image Upload
Location
Repeating Group
```

Future:

```text
Drawing
Signature
Map
Room Planner
Diagram
```

Questions must support:

```text
Required
Sequence
Options
Conditional Logic
Validation Rules
Help Text
```

---

# 25. Residential Onboarding

Sections:

```text
Client Information
Family Profile
Lifestyle
Rooms
Spaces
Kitchen
Bedrooms
Bathrooms
Living
Dining
Services
Parking
Outdoor
Accessibility
Security
Vastu / Cultural Preferences
Design Preferences
Future Expansion
```

Repeatable room requirement:

```text
Room Type
Quantity
Preferred Area
Required
Floor Preference
Special Requirements
```

---

# 26. Commercial Onboarding

Sections:

```text
Business Information
Occupancy
Operations
Customer Flow
Staff Flow
Service Flow
Departments
Workspaces
Public Areas
Parking
Loading
Storage
Security
Branding
MEP Requirements
Future Expansion
```

---

# 27. Healthcare Onboarding

Sections:

```text
Facility Type
Clinical Services
Departments
Patient Types
Patient Flow
Staff Flow
Emergency Flow
Public Areas
Clinical Areas
Support Areas
Medical Equipment
Infection Control
Accessibility
MEP
Medical Gas
Waste Management
Regulatory Requirements
```

---

# 28. Hospitality Onboarding

Sections:

```text
Property Type
Brand
Category
Keys / Rooms
Guest Profile
Public Areas
Guest Flow
Service Flow
Food & Beverage
Events
Back of House
Staff Facilities
Parking
Landscape
Recreation
MEP / Services
```

---

# 29. Education Onboarding

Sections:

```text
Institution Type
Student Profile
Age Groups
Student Capacity
Staff
Academic Program
Classrooms
Laboratories
Administration
Library
Sports
Assembly
Dining
Transport
Safety
Accessibility
Future Expansion
```

---

# 30. Site Data

Site data must be a separate project module.

Sections:

```text
Location
Site Dimensions
Orientation
Access
Existing Conditions
Surroundings
Topography
Utilities
Existing Structures
Vegetation
Legal / Planning Information
Site Documents
```

---

# 31. Project Brief

Generated from onboarding information.

```text
PROJECT BRIEF
│
├── Client
├── Project
├── Site
├── Requirements
├── Space Program
├── Budget
├── Timeline
├── Design Preferences
├── Special Requirements
├── Documents
└── Clarifications
```

Architect reviews and approves the brief.

---

# 32. Project Phases

```text
DESIGN
EXECUTION
HANDOVER
```

Design stages:

```text
Concept
Preliminary Design
Design Development
Approval Drawings
Working Drawings
Detailed Drawings
Issued for Construction
```

---

# 33. Execution Levels

Execution must support arbitrary levels.

Examples:

```text
Basement
Ground Floor
First Floor
Second Floor
Roof
Terrace
```

or:

```text
Foundation
Level 0
Level 1
Level 2
Level 3
```

Each project can define its own level structure.

---

# 34. Drawings

Drawing entity:

```text
Drawing Number
Drawing Name
Drawing Type
Revision
Phase
Execution Level
Status
Assigned User
Due Date
Issued Date
File
Remarks
```

Statuses:

```text
DRAFT
IN_REVIEW
REVISION_REQUIRED
APPROVED
ISSUED
SUPERSEDED
```

---

# 35. Client Approvals

Track:

```text
Title
Description
Project Phase
Level
Submitted Date
Approved Date
Status
Remarks
```

---

# 36. Technical Approvals

Track:

```text
Title
Approval Type
Description
Phase
Level
Submitted Date
Approved Date
Status
Remarks
```

---

# 37. Backlogs

Categories:

```text
DRAWINGS
CLIENT_APPROVALS
TECHNICAL_APPROVALS
```

Priority:

```text
LOW
MEDIUM
HIGH
CRITICAL
```

Status:

```text
OPEN
IN_PROGRESS
COMPLETE
CANCELLED
```

---

# 38. Tasks

Tasks can belong to:

```text
Project
Phase
Execution Level
```

Fields:

```text
Title
Description
Status
Priority
Assigned User
Start Date
Due Date
Completed Date
```

---

# 39. Fees

Supported fee bases:

```text
SQFT
PERCENTAGE
LUMP_SUM
```

Calculations:

```text
SQFT:

Project Area × Rate per Sq.ft


PERCENTAGE:

Project Cost × Fee Percentage / 100


LUMP SUM:

Agreed Fee
```

---

# 40. Tax Calculation

Centralized calculation:

```text
tax = taxable_amount × tax_rate / 100
```

Project fee display:

```text
Professional Fee
Tax Regime
Tax Rate
Tax Amount
Total Payable
```

When an agreement or invoice is finalized, store a tax snapshot.

Historical documents must not change when the company's tax profile changes.

---

# 41. Agreements

Agreement workflow:

```text
Draft
 ↓
Internal Review
 ↓
Client Review
 ↓
Revision Required
 ↓
Final
 ↓
Sent
 ↓
Signed
 ↓
Active
```

Agreement must store:

```text
Template Version
Scope Snapshot
Deliverables Snapshot
Fee Snapshot
Tax Snapshot
Payment Schedule Snapshot
Exclusions Snapshot
Responsibilities Snapshot
Terms Snapshot
```

Changing a template must never modify an existing signed agreement.

---

# 42. Agreement Variables

Support:

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
{{SCOPE_OF_WORK}}
```

---

# 43. Scope Variations

Additional services must be separated from original scope.

Fields:

```text
Project
Title
Description
Original Scope
Additional Service
Additional Fee
Tax
Total Amount
Status
Approved Date
```

Statuses:

```text
PROPOSED
APPROVED
REJECTED
COMPLETED
```

---

# 44. Finance

Modules:

```text
Fees
Invoices
Payments
Outstanding
```

Invoice:

```text
Invoice Number
Invoice Date
Client
Project
Subtotal
Tax Regime
Tax Rate
Tax Amount
Total
Amount Paid
Amount Due
Status
```

Invoice items:

```text
Description
Quantity
Rate
Amount
```

Payments:

```text
Payment Date
Amount
Payment Method
Reference Number
Notes
```

---

# 45. Documents

Documents must be stored in Supabase Storage.

Document metadata in PostgreSQL:

```text
id
company_id
project_id
client_id
name
document_type
storage_path
mime_type
file_size
version
uploaded_by
created_at
```

WordPress should retrieve documents through authenticated Supabase access.

---

# 46. Client Portal

Client portal must be a WordPress page using the same Supabase backend.

Client sees only permitted information:

```text
My Projects
│
├── Project Overview
├── Project Brief
├── Requirements
├── Documents
├── Drawings
├── Approvals
├── Agreements
├── Invoices
└── Payments
```

Client must never see:

```text
Other Clients
Other Projects
Internal Notes
Internal Backlogs
Staff Information
Internal Financial Data
Administrative Settings
```

RLS must enforce this at the Supabase level.

---

# 47. WordPress Frontend Components

Build reusable frontend components.

```text
ERP Layout
ERP Sidebar
Top Bar
Breadcrumbs
Page Header
Cards
Tables
Forms
Tabs
Modal
Drawer
Dropdown
Search
Filter
Pagination
Status Badge
Priority Badge
Progress Bar
Timeline
File Upload
Document Viewer
Activity Feed
Confirmation Dialog
Toast Notification
```

---

# 48. JavaScript Architecture

Use modular JavaScript.

```text
/assets/js/

app.js
auth.js
supabase.js
dashboard.js
clients.js
projects.js
onboarding.js
consultancy.js
agreements.js
drawings.js
approvals.js
finance.js
documents.js
users.js
permissions.js
reports.js
```

The Supabase client must be initialized centrally.

```text
supabase.js
      ↓
all modules
```

Do not duplicate authentication or database connection logic in every page.

---

# 49. Supabase Client

Use the Supabase JavaScript client from the WordPress frontend.

Only expose the:

```text
Supabase Project URL
Anon / Publishable Key
```

Never expose:

```text
Service Role Key
Database Password
Private API Keys
```

---

# 50. RLS

Every ERP table must have Row Level Security enabled.

Basic hierarchy:

```text
User
 ↓
Company
 ↓
Role
 ↓
Project Assignment
 ↓
Record Access
```

Example:

```text
company_id = authenticated user's company_id
```

Project-level data additionally checks:

```text
user belongs to project
```

Client data additionally checks:

```text
client_id belongs to authenticated client
```

---

# 51. WordPress Security

WordPress must provide:

```text
HTTPS
Secure cookies
CSRF protection
Input validation
Output escaping
Content Security Policy where practical
Restricted admin access
Automatic updates
```

Supabase remains the authoritative data security layer.

---

# 52. Dashboard

Main dashboard:

```text
┌──────────────────────────────────────────┐
│ PROJECTS                                 │
│ 12 Active     4 Pending     2 On Hold    │
├──────────────────────────────────────────┤
│ FEES                                     │
│ Outstanding: ₹ XXXXX                     │
├──────────────────────────────────────────┤
│ DRAWINGS                                 │
│ Pending: 14                              │
├──────────────────────────────────────────┤
│ APPROVALS                                │
│ Pending: 7                               │
├──────────────────────────────────────────┤
│ BACKLOG                                  │
│ Critical: 2   High: 5                    │
├──────────────────────────────────────────┤
│ RECENT ACTIVITY                          │
└──────────────────────────────────────────┘
```

Dashboard contents must depend on user permissions.

---

# 53. Search

Global search:

```text
Clients
Projects
Drawings
Agreements
Invoices
Documents
Tasks
```

Search results must respect Supabase RLS.

A user must never receive search results for records they cannot access.

---

# 54. Notifications

Frontend notifications:

```text
New Assignment
Drawing Due
Approval Pending
Client Response
Agreement Revision
Payment Due
Payment Received
Backlog Assigned
Task Due
```

Realtime notifications may use Supabase Realtime.

---

# 55. Audit Log

Record:

```text
User
Action
Table
Record
Old Data
New Data
Timestamp
```

Actions:

```text
CREATE
UPDATE
DELETE
APPROVE
REJECT
ISSUE
LOGIN
LOGOUT
```

Audit records must be protected from normal users.

---

# 56. Database

Use the previously defined Supabase PostgreSQL schema.

Core entities:

```text
companies
company_tax_profiles

users
roles
permissions
role_permissions
project_users
user_sessions
devices
audit_logs

clients

project_types
consultancy_types

projects
project_consultancies
project_phases
execution_levels
tasks

consultancy_templates
consultancy_template_project_types
template_scopes
template_deliverables
template_phases
template_fees
template_payment_milestones
template_exclusions
template_responsibilities

onboarding_templates
onboarding_sections
onboarding_questions
onboarding_sessions
onboarding_responses
onboarding_clarifications

project_briefs

project_scopes
project_deliverables
project_fees
project_payment_milestones

drawings
client_approvals
technical_approvals
backlogs

agreement_templates
project_agreements
agreement_snapshots
scope_variations

invoices
invoice_items
payments

documents

sync_config
sync_changes
```

---

# 57. WordPress Database

WordPress database should contain only WordPress infrastructure.

Examples:

```text
wp_posts
wp_options
wp_users
wp_terms
wp_comments
```

These should not contain ERP business records.

The ERP's source of truth is:

```text
Supabase PostgreSQL
```

---

# 58. WordPress Theme

Create a dedicated ERP theme or child theme.

Recommended:

```text
architecture-erp-theme/
│
├── style.css
├── functions.php
├── header.php
├── footer.php
├── sidebar.php
│
├── page-login.php
├── page-dashboard.php
├── page-clients.php
├── page-projects.php
├── page-onboarding.php
├── page-agreements.php
├── page-finance.php
├── page-documents.php
├── page-settings.php
│
├── templates/
│
├── assets/
│   ├── css/
│   ├── js/
│   └── icons/
│
└── components/
```

No ERP plugin.

---

# 59. WordPress Admin

The normal WordPress administrator interface should not be used as the primary ERP interface.

Users should enter:

```text
/erp
```

or:

```text
/dashboard
```

and use the custom ERP frontend.

---

# 60. Responsive Design

Support:

```text
Desktop
Laptop
Tablet
Mobile
```

Desktop is the primary environment.

Mobile should support:

```text
Dashboard
Client lookup
Project lookup
Tasks
Approvals
Backlogs
Documents
Basic updates
```

---

# 61. Offline / Local-First Compatibility

The architecture should be designed so that a future local WordPress installation can connect to a local backend.

Cloud deployment:

```text
WordPress
   ↓
Supabase
```

Future local deployment:

```text
Local WordPress
      ↓
Local API / Database
      ↓
Optional Supabase Sync
```

Do not tightly couple frontend components to a particular database implementation.

Create a data-access abstraction:

```text
UI
 ↓
Data Service
 ↓
Supabase
```

rather than:

```text
UI
 ↓
direct SQL assumptions
```

---

# 62. Supabase Backup

Supabase can be used as:

```text
Cloud Backup
```

for the local deployment.

Future synchronization:

```text
LOCAL DATA
    ↕
SYNC ENGINE
    ↕
SUPABASE
```

Backup and synchronization must be treated as separate functions.

---

# 63. Development Phases

## Phase 1 — WordPress Shell

Build:

```text
ERP Theme
Login
Dashboard
Sidebar
Header
Responsive Layout
Supabase Connection
Session Handling
```

---

## Phase 2 — Authentication

Implement:

```text
Supabase Auth
Login
Logout
Session
Password Reset
User Profile
```

---

## Phase 3 — Company

Implement:

```text
Company Profile
Professional Information
Tax Configuration
Invoice Configuration
```

---

## Phase 4 — Users

Implement:

```text
Users
Roles
Permissions
Hierarchy
Project Assignment
Maximum 5 Active Users
```

---

## Phase 5 — Clients

Implement:

```text
Client CRUD
Client Profile
Client Projects
Client Documents
```

---

## Phase 6 — Projects

Implement:

```text
Project Creation
Project Dashboard
Project Phases
Execution Levels
Tasks
```

---

## Phase 7 — Onboarding

Implement:

```text
Templates
Sections
Questions
Conditional Logic
Responses
Clarifications
Project Brief
```

---

## Phase 8 — Consultancy

Implement:

```text
Consultancy Types
Consultancy Templates
Scope
Deliverables
Fees
Payment Milestones
Exclusions
Responsibilities
```

---

## Phase 9 — Agreements

Implement:

```text
Agreement Templates
Variable Replacement
Agreement Generation
Versioning
Snapshots
Variations
```

---

## Phase 10 — Project Execution

Implement:

```text
Drawings
Client Approvals
Technical Approvals
Backlogs
Tasks
Project Progress
```

---

## Phase 11 — Finance

Implement:

```text
Fees
Tax
Invoices
Payments
Outstanding
```

---

## Phase 12 — Documents

Implement:

```text
Supabase Storage
Upload
Download
Preview
Versioning
Project Documents
Client Documents
```

---

## Phase 13 — Client Portal

Implement:

```text
Client Login
Client Dashboard
Project Information
Requirements
Documents
Drawings
Approvals
Agreements
Invoices
Payments
```

---

## Phase 14 — Reports

Implement:

```text
Project Reports
Fee Reports
Outstanding Reports
Drawing Reports
Approval Reports
Backlog Reports
User Workload
```

---

# 64. Important Development Rule

Do not duplicate business data in WordPress.

Correct:

```text
WordPress
    │
    │ UI
    ▼
Supabase
    │
    ├── PostgreSQL
    ├── Auth
    ├── Storage
    └── RLS
```

Incorrect:

```text
WordPress
 ├── wp_posts
 ├── wp_postmeta
 └── Supabase

     ↓

Duplicate ERP data
```

---

# 65. Final Architecture

The completed system should behave like a standalone ERP application while being hosted on WordPress.

```text
                 ARCHITECTURE ERP
                        │
                        ▼
                WORDPRESS WEBSITE
                        │
              ┌─────────┴─────────┐
              │                   │
         ERP THEME           JAVASCRIPT
              │                   │
              └─────────┬─────────┘
                        │
                 SUPABASE CLIENT
                        │
                        ▼
                 ┌──────────────┐
                 │   SUPABASE   │
                 ├──────────────┤
                 │ PostgreSQL   │
                 │ Auth         │
                 │ Storage      │
                 │ RLS          │
                 │ Realtime     │
                 └──────────────┘
```

## Fundamental principle

> **WordPress is the application interface. Supabase is the ERP backend. No custom WordPress plugin is required.**

The system must be designed so that the WordPress frontend is a **thin, reusable application layer**, while all important business data, permissions, authentication, documents, and relationships remain in Supabase.

This allows the same Supabase ERP backend to eventually support:

```text
WordPress Web ERP
        +
Local Office ERP
        +
Client Portal
        +
Mobile Interface
        +
Future Desktop Interface
```

without redesigning the underlying ERP database.