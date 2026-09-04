# Architecture Consultancy App
## Client Onboarding & Project Brief Template Module

---

# 1. Purpose

Create a structured **Client Onboarding / Project Brief** system to capture all information required before starting architectural consultancy.

The onboarding system must collect:

- Client information
- Project information
- Site information
- Client requirements
- Project-specific requirements
- Budget
- Time expectations
- Regulatory/approval information
- Existing documentation
- Consultant requirements
- Design preferences
- Special requirements
- Initial scope expectations

The onboarding form must be **dynamic based on Project Type**.

---

# 2. Core Principle

Do not use one giant generic questionnaire.

Instead:

```text
CLIENT
   ↓
PROJECT
   ↓
SELECT PROJECT TYPE
   ↓
LOAD PROJECT-TYPE TEMPLATE
   ↓
CLIENT ONBOARDING
   ↓
PROJECT BRIEF
   ↓
SITE DATA
   ↓
REQUIREMENTS
   ↓
DESIGN BRIEF
   ↓
CONSULTANCY PROPOSAL
   ↓
AGREEMENT
```

---

# 3. Project-Type Templates

Create separate onboarding templates for:

```text
Residential
Commercial
Healthcare
Hospitality
Education
Others
```

Each template should contain:

```text
Common Questions
+
Project-Type-Specific Questions
```

---

# 4. Common Client Onboarding

All projects should first collect common information.

## 4.1 Client Information

```text
Client Name
Client Type
Contact Person
Phone
Email
Address
Billing Address
Preferred Communication Method
```

Client type:

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

# 5. Project Information

```text
Project Name
Project Type
Project Location
Project Address
Site Area
Expected Built-up Area
Approximate Project Cost
Expected Start Date
Expected Completion Date
```

---

# 6. Client Objectives

Use structured questions.

### Primary Objective

```text
What is the primary objective of the project?

○ Own Use
○ Investment
○ Rental
○ Sale
○ Institutional Use
○ Commercial Use
○ Other
```

### Project Vision

```text
Describe what you want to achieve with the project.
```

Large text field.

---

# 7. Budget

Capture budget separately from project cost estimate.

```text
Land Cost
Construction Budget
Interior Budget
Landscape Budget
Equipment Budget
Overall Project Budget
```

Allow:

```text
Exact
Approximate
Not Decided
```

---

# 8. Timeline

Capture:

```text
Desired Design Start
Desired Construction Start
Desired Completion
Preferred Handover Date
```

Also ask:

```text
Is the timeline flexible?

○ Yes
○ No
○ Somewhat
```

---

# 9. Site Information

Create a dedicated **Site Data** section.

```text
SITE DATA
│
├── Location
├── Site Dimensions
├── Orientation
├── Access
├── Existing Conditions
├── Surroundings
├── Topography
├── Utilities
├── Existing Structures
├── Vegetation
├── Legal/Planning Information
└── Site Documents
```

---

# 10. Site Location

Fields:

```text
Site Address
Village / Locality
City
District
State
PIN
Latitude
Longitude
```

Allow map location selection in future.

---

# 11. Site Dimensions

Capture:

```text
Site Area
Site Length
Site Width
Frontage
Depth
```

Allow irregular sites.

Future:

```text
Upload Site Survey
Upload CAD/DWG
Upload PDF Survey
```

---

# 12. Site Orientation

Capture:

```text
North Direction
Road Direction
Main Entry Direction
Site Orientation
```

Allow graphical/site-plan input later.

---

# 13. Site Access

Questions:

```text
Number of Road Frontages
Road Width
Primary Access
Secondary Access
Vehicle Access
Pedestrian Access
Service Access
```

Example:

```text
Road Frontage:
2

Primary Road:
12m

Secondary Road:
6m
```

---

# 14. Existing Site Conditions

Capture:

```text
Existing Building
Existing Structure
Existing Compound Wall
Existing Trees
Existing Utilities
Existing Borewell
Existing Water Connection
Existing Drainage
Existing Electrical Connection
Existing Road
Existing Landscape
```

Each can use:

```text
○ Yes
○ No
○ Unknown
```

If Yes:

```text
Details
Photo
Document
Location
```

---

# 15. Topography

Capture:

```text
Flat
Sloping
Steep Slope
Uneven
Unknown
```

Additional:

```text
Approximate level difference
Existing contour survey available?
```

---

# 16. Site Documents

Allow uploading:

```text
Sale Deed
Survey Sketch
Site Plan
Survey Drawing
Khata / Property Documents
Tax Documents
Previous Approval
Existing Building Drawings
Structural Drawings
Utility Documents
Photographs
Videos
Other
```

Documents should be linked to the project.

---

# 17. Regulatory / Planning Data

Capture known information:

```text
Planning Authority
Local Authority
Site Zoning
Land Use
Applicable Development Regulations
Setback Requirements
FAR / FSI
Ground Coverage
Height Restrictions
Parking Requirements
Known Restrictions
```

Important:

These fields should capture **known/project-provided information**.

The application should not automatically represent unverified information as legally confirmed.

---

# 18. Residential Onboarding Template

Residential projects require a detailed **Family / Lifestyle / Space Requirement Brief**.

Template sections:

```text
Residential
│
├── Family Profile
├── Lifestyle
├── Rooms
├── Spaces
├── Kitchen
├── Bedrooms
├── Bathrooms
├── Living
├── Dining
├── Services
├── Parking
├── Outdoor
├── Accessibility
├── Security
├── Vastu / Cultural Preferences
├── Design Preferences
└── Future Expansion
```

---

# 19. Residential — Family Profile

Capture:

```text
Number of Family Members
Adults
Children
Senior Citizens
Pets
Live-in Staff
Visitors
```

Optional:

```text
Family Structure
Lifestyle Description
Special Needs
```

---

# 20. Residential — Room Requirements

Use a repeatable room requirement table.

```text
Room Type
Quantity
Preferred Area
Required?
Floor Preference
Special Requirements
```

Example:

```text
Master Bedroom       1
Bedroom              3
Guest Bedroom        1
Living Room          1
Family Room          1
Dining               1
Kitchen              1
Puja Room            1
Study                1
Home Office          1
Utility              1
Store                2
```

Do not hard-code the number of rooms.

Allow:

```text
[ + Add Space ]
```

---

# 21. Residential — Space Preferences

For each space:

```text
Required
Preferred Size
Location Preference
Privacy Level
Natural Light
Natural Ventilation
Furniture Requirements
Special Equipment
Notes
```

---

# 22. Residential — Kitchen

Capture:

```text
Kitchen Type

○ Open
○ Semi Open
○ Closed
○ No Preference
```

Additional:

```text
Dry Kitchen
Wet Kitchen
Utility
Pantry
Island
Breakfast Counter
Storage Requirements
Appliance Requirements
```

---

# 23. Residential — Parking

Capture:

```text
Cars
Two-Wheelers
EV Charging
Visitor Parking
Driver Room
Garage
Covered Parking
```

---

# 24. Residential — Lifestyle

Capture preferences:

```text
Entertainment
Home Theatre
Gym
Library
Office
Gaming
Garden
Pool
Terrace
Balcony
Bar
Prayer/Puja
Hobby Room
Children's Play Area
```

---

# 25. Residential — Design Preferences

Allow:

```text
Modern
Contemporary
Traditional
Minimal
Industrial
Classic
Regional
Other
```

Also allow:

```text
Upload Reference Images
Upload Pinterest/Reference Links
Upload Existing Inspiration
```

---

# 26. Residential — Vastu / Cultural Requirements

Make this an optional section.

```text
Do you have specific planning/cultural requirements?

○ Yes
○ No
```

If Yes:

```text
Describe requirements
```

Do not make assumptions about requirements.

---

# 27. Commercial Onboarding Template

Commercial projects should capture **business and operational requirements**.

Structure:

```text
Commercial
│
├── Business Information
├── Occupancy
├── Operations
├── Customer Flow
├── Staff Flow
├── Service Flow
├── Departments
├── Workspaces
├── Public Areas
├── Parking
├── Loading
├── Storage
├── Security
├── Branding
├── MEP Requirements
└── Future Expansion
```

---

# 28. Commercial — Business Information

```text
Business Type
Business Name
Primary Activity
Operating Hours
Expected Opening Date
Expected Daily Users
Expected Staff
```

---

# 29. Commercial — Occupancy

Capture:

```text
Normal Occupancy
Peak Occupancy
Staff Count
Visitor Count
Customer Count
```

---

# 30. Commercial — Operational Requirements

Questions:

```text
How does the business operate?
What is the customer journey?
What is the staff workflow?
What deliveries are required?
What storage is required?
What service areas are required?
```

Use structured workflow fields where possible.

---

# 31. Commercial — Space Program

Repeatable:

```text
Space
Quantity
Area
Occupancy
Public / Private
Floor Preference
Special Requirements
```

---

# 32. Healthcare Onboarding Template

Healthcare projects require a specialized workflow.

Structure:

```text
Healthcare
│
├── Facility Type
├── Clinical Services
├── Departments
├── Patient Types
├── Patient Flow
├── Staff Flow
├── Emergency Flow
├── Public Areas
├── Clinical Areas
├── Support Areas
├── Medical Equipment
├── Infection Control
├── Accessibility
├── MEP
├── Medical Gas
├── Waste Management
└── Regulatory Requirements
```

---

# 33. Healthcare — Facility Type

Examples:

```text
Clinic
Diagnostic Centre
Day Care Centre
Nursing Home
Hospital
Specialty Hospital
Rehabilitation Centre
Other
```

---

# 34. Healthcare — Clinical Program

Repeatable department structure:

```text
Department
Number of Rooms
Area Requirement
Patient Capacity
Equipment
Special Requirements
```

Example:

```text
OPD
Emergency
Radiology
Laboratory
Pharmacy
OT
ICU
Ward
Consultation
Administration
```

The list must remain configurable.

---

# 35. Healthcare — Patient Flow

Capture separately:

```text
Patient Entry
Registration
Waiting
Consultation
Diagnostics
Treatment
Pharmacy
Discharge
```

Also:

```text
Emergency Access
Ambulance Access
Staff Access
Service Access
Waste Flow
```

This should eventually support a graphical workflow diagram.

---

# 36. Hospitality Onboarding Template

Structure:

```text
Hospitality
│
├── Property Type
├── Brand
├── Star / Category
├── Keys / Rooms
├── Guest Profile
├── Public Areas
├── Guest Flow
├── Service Flow
├── Food & Beverage
├── Events
├── Back of House
├── Staff Facilities
├── Parking
├── Landscape
├── Recreation
└── MEP / Services
```

---

# 37. Hospitality — Property Type

Examples:

```text
Hotel
Resort
Boutique Hotel
Guest House
Serviced Apartment
Homestay
Restaurant
Banquet Facility
Club
Other
```

---

# 38. Hospitality — Accommodation

Repeatable:

```text
Room Type
Quantity
Area
Occupancy
Bed Type
Bathroom
Balcony
Special Requirements
```

---

# 39. Hospitality — Public Areas

Examples:

```text
Reception
Lobby
Restaurant
Cafe
Bar
Banquet
Conference
Pool
Gym
Spa
Lounge
Children's Area
Outdoor Areas
```

Allow custom additions.

---

# 40. Education Onboarding Template

Structure:

```text
Education
│
├── Institution Type
├── Student Profile
├── Age Groups
├── Student Capacity
├── Staff
├── Academic Program
├── Classrooms
├── Laboratories
├── Administration
├── Library
├── Sports
├── Assembly
├── Dining
├── Transport
├── Safety
├── Accessibility
└── Future Expansion
```

---

# 41. Education — Institution Type

Examples:

```text
School
Pre-School
College
University
Training Centre
Coaching Centre
Vocational Institute
Other
```

---

# 42. Education — Capacity

Capture:

```text
Total Students
Students per Class
Number of Classes
Teaching Staff
Administrative Staff
Support Staff
Expected Growth
```

---

# 43. Education — Space Program

Repeatable:

```text
Space
Quantity
Area
Capacity
Floor Preference
Special Equipment
Special Requirements
```

Examples:

```text
Classrooms
Laboratories
Library
Computer Lab
Staff Room
Principal Office
Administration
Toilets
Activity Rooms
Auditorium
Sports
Canteen
```

---

# 44. Other Project Types

If:

```text
Project Type = Others
```

show:

```text
Project Type Name
```

Then load the **Generic Project Brief Template**.

The generic template should contain:

```text
Project Purpose
Users
Occupancy
Activities
Space Requirements
Operational Requirements
Site Requirements
Budget
Timeline
Special Requirements
```

The administrator can later create a dedicated template for that project type.

---

# 45. Dynamic Template Engine

The onboarding system should not be hard-coded separately into the frontend.

Use:

```text
ONBOARDING_TEMPLATE
        ↓
ONBOARDING_SECTION
        ↓
ONBOARDING_QUESTION
        ↓
CLIENT_RESPONSE
```

This allows administrators to modify questionnaires without changing application code.

---

# 46. Onboarding Template Entity

```text id="q1x2sm"
ONBOARDING_TEMPLATE
```

Fields:

```text
id
name
project_type
consultancy_type
description
version
status
created_at
updated_at
```

---

# 47. Onboarding Section Entity

```text id="0hsfjj"
ONBOARDING_SECTION
```

Fields:

```text
id
template_id
name
description
sequence
is_required
```

Example:

```text
Residential
   ↓
Family Profile
   ↓
Space Requirements
   ↓
Lifestyle
   ↓
Site
   ↓
Budget
```

---

# 48. Onboarding Question Entity

```text id="y3y82p"
ONBOARDING_QUESTION
```

Fields:

| Field | Type |
|---|---|
| `id` | UUID |
| `section_id` | UUID |
| `question` | Text |
| `help_text` | Text |
| `field_type` | Enum |
| `required` | Boolean |
| `sequence` | Integer |
| `options` | JSON |
| `conditional_logic` | JSON |
| `validation_rules` | JSON |

---

# 49. Supported Question Types

Initial types:

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

---

# 50. Conditional Questions

Questions should be able to appear based on previous answers.

Example:

```text
Do you require parking?

○ Yes
○ No
```

If Yes:

```text
Number of Cars?
Number of Two-Wheelers?
EV Charging?
Covered Parking?
```

If No:

```text
Hide parking questions.
```

Another example:

```text
Does the project include a swimming pool?

○ Yes
○ No
```

If Yes:

```text
Pool Type
Approximate Size
Indoor / Outdoor
Pool Equipment
Changing Rooms
```

---

# 51. Client Onboarding Status

Create:

```text
ONBOARDING_SESSION
```

Statuses:

```text
Not Started
In Progress
Submitted
Under Review
Clarification Required
Approved
Converted to Project Brief
```

---

# 52. Onboarding Workflow

```text
CLIENT
  ↓
INVITATION / NEW ONBOARDING
  ↓
CLIENT INFORMATION
  ↓
PROJECT INFORMATION
  ↓
SITE DATA
  ↓
PROJECT-TYPE QUESTIONNAIRE
  ↓
DOCUMENT UPLOAD
  ↓
SUBMIT
  ↓
ARCHITECT REVIEW
  ↓
CLARIFICATIONS
  ↓
FINAL PROJECT BRIEF
```

---

# 53. Architect Review

After the client submits the onboarding form, the architect should see:

```text
CLIENT SUBMISSION

✓ Client Information
✓ Project Information
✓ Site Information
✓ Requirements
✓ Budget
✓ Timeline
✓ Documents

⚠ 4 Clarifications Required
```

The architect can request clarification on individual questions.

---

# 54. Clarification Workflow

Example:

```text
Question:
Expected Built-up Area?

Client Answer:
20,000 Sq.ft

Architect:
Please confirm whether this includes parking and service areas.

Status:
Clarification Required
```

Client responds:

```text
Confirmed: 20,000 Sq.ft excludes parking.
```

The clarification becomes part of the project record.

---

# 55. Project Brief Generation

Once onboarding is approved:

```text
CLIENT ONBOARDING
       ↓
PROJECT BRIEF
```

Generate a structured project brief containing:

```text
Client
Project
Site
Project Type
Requirements
Space Program
Budget
Timeline
Design Preferences
Special Requirements
Documents
Clarifications
```

---

# 56. Project Brief → Consultancy Proposal

The approved project brief should feed into:

```text
PROJECT BRIEF
      ↓
CONSULTANCY TEMPLATE
      ↓
SCOPE OF WORK
      ↓
FEE
      ↓
AGREEMENT
```

This avoids asking the client for the same information multiple times.

---

# 57. Client Portal

The onboarding template should eventually be accessible through a client portal.

Client sees:

```text
My Project

1. Client Information       ✓
2. Project Information      ✓
3. Site Information         ✓
4. Requirements             ●
5. Documents                ✓
6. Review & Submit          ○
```

The client should be able to save and continue later.

---

# 58. Onboarding Dashboard

Internal dashboard:

```text
┌──────────────────────────────────────────────┐
│ CLIENT ONBOARDING                            │
├──────────────────────────────────────────────┤
│                                              │
│ New Submissions              4               │
│ In Progress                  7               │
│ Clarification Required       3               │
│ Under Review                 2               │
│ Completed                    12              │
│                                              │
└──────────────────────────────────────────────┘
```

---

# 59. Data Reuse

Information captured during onboarding should automatically populate:

```text
Client Record
Project Record
Project Brief
Consultancy Proposal
Agreement
Fee Calculation
Project Dashboard
```

Example:

```text
Client Name
     ↓
Client Master

Project Area
     ↓
Project Master
     ↓
Fee Calculation

Project Type
     ↓
Onboarding Template
     ↓
Project Brief

Approved Scope
     ↓
Agreement
```

---

# 60. Important Developer Principle

Do not create separate hard-coded forms such as:

```text
ResidentialForm.jsx
CommercialForm.jsx
HospitalForm.jsx
SchoolForm.jsx
```

Instead implement:

```text
Template Engine
      +
Project Type
      +
Question Configuration
```

The administrator should be able to create/edit questions and sections.

---

# 61. Recommended Initial Template Library

Create these initial templates:

```text
01 — Residential
02 — Commercial
03 — Healthcare
04 — Hospitality
05 — Education
06 — Generic / Other
```

Each template should have:

```text
Common Sections
+
Project-Specific Sections
```

---

# 62. Common Sections

Every template should initially contain:

```text
01 Client Information
02 Project Information
03 Project Objectives
04 Site Information
05 Existing Conditions
06 Budget
07 Timeline
08 Documents
09 Special Requirements
10 Declaration & Submission
```

---

# 63. Residential Sections

```text
01 Client Information
02 Project Information
03 Family Profile
04 Lifestyle
05 Space Requirements
06 Kitchen
07 Bedrooms
08 Bathrooms
09 Parking
10 Outdoor Areas
11 Design Preferences
12 Vastu / Cultural Preferences
13 Site Information
14 Budget
15 Timeline
16 Documents
17 Special Requirements
```

---

# 64. Commercial Sections

```text
01 Client Information
02 Business Information
03 Project Information
04 Users & Occupancy
05 Operational Requirements
06 Space Program
07 Customer Flow
08 Staff Flow
09 Service Flow
10 Parking & Loading
11 Security
12 Branding
13 Site Information
14 Budget
15 Timeline
16 Documents
17 Special Requirements
```

---

# 65. Healthcare Sections

```text
01 Client Information
02 Facility Information
03 Clinical Program
04 Departments
05 Patient Profile
06 Patient Flow
07 Staff Flow
08 Emergency Flow
09 Medical Equipment
10 Infection Control
11 MEP / Medical Services
12 Waste Management
13 Accessibility
14 Site Information
15 Regulatory Information
16 Budget
17 Timeline
18 Documents
```

---

# 66. Hospitality Sections

```text
01 Client Information
02 Property Information
03 Brand Information
04 Guest Profile
05 Accommodation
06 Public Areas
07 Food & Beverage
08 Guest Flow
09 Service Flow
10 Back of House
11 Staff Facilities
12 Recreation
13 Landscape
14 Parking
15 Site Information
16 Budget
17 Timeline
18 Documents
```

---

# 67. Education Sections

```text
01 Client Information
02 Institution Information
03 Academic Program
04 Student Profile
05 Capacity
06 Classrooms
07 Laboratories
08 Administration
09 Library
10 Sports
11 Assembly
12 Dining
13 Transport
14 Staff Facilities
15 Safety
16 Accessibility
17 Site Information
18 Budget
19 Timeline
20 Documents
```

---

# 68. Final Architecture

The complete application structure should now be:

```text
COMPANY
│
├── Company Profile
├── Tax Profile
│
├── Consultancy Templates
│   ├── Scope
│   ├── Deliverables
│   ├── Fees
│   ├── Payment Schedule
│   └── Agreements
│
├── Onboarding Templates
│   ├── Residential
│   ├── Commercial
│   ├── Healthcare
│   ├── Hospitality
│   ├── Education
│   └── Other
│
└── CLIENT
    │
    └── ONBOARDING
         │
         ├── Client Data
         ├── Project Data
         ├── Site Data
         ├── Requirements
         ├── Documents
         ├── Clarifications
         │
         ↓
       PROJECT BRIEF
         │
         ↓
       PROJECT
         │
         ├── Consultancy
         ├── Scope
         ├── Fee
         ├── Agreement
         ├── Design
         ├── Execution
         ├── Handover
         ├── Drawings
         ├── Approvals
         ├── Backlogs
         └── Finance
```

---

# 69. End-to-End Client Journey

The intended user journey is:

```text
CLIENT CREATED
      ↓
SELECT PROJECT TYPE
      ↓
SELECT ONBOARDING TEMPLATE
      ↓
CLIENT FILLS REQUIREMENTS
      ↓
SITE DATA + DOCUMENTS
      ↓
SUBMIT
      ↓
ARCHITECT REVIEWS
      ↓
CLARIFICATIONS
      ↓
APPROVED PROJECT BRIEF
      ↓
SELECT CONSULTANCY TEMPLATE
      ↓
DEFINE SCOPE
      ↓
CALCULATE FEE
      ↓
APPLY COMPANY TAX PROFILE
      ↓
GENERATE AGREEMENT
      ↓
CLIENT ACCEPTS / SIGNS
      ↓
PROJECT ACTIVATED
```

The **Onboarding Template** captures *what the client needs*, while the **Consultancy Template** defines *what the practice will provide*, and the **Agreement** formally connects the two commercially.

This separation is critical to keeping the architecture consultancy system clean and scalable.