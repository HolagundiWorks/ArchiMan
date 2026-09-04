# Architecture Consultancy App
## Local-First Multi-User + Optional Supabase Sync Architecture

---

# 1. Core Architecture

The application is **local-first**.

```text
                    LOCAL NETWORK
                 Wi-Fi / LAN only
                        │
        ┌───────────────┼───────────────┐
        │               │               │
      USER 1          USER 2          USER 3
        │               │               │
        └───────────────┼───────────────┘
                        ↓
              LOCAL APPLICATION SERVER
                        │
                        ↓
               LOCAL DATABASE
                        │
                  ┌─────┴─────┐
                  │           │
              Local Files   Backups
                  │
                  ↓
             OPTIONAL SYNC
                  │
                  ↓
               SUPABASE
```

The application must continue working when there is **no Internet connection**.

Supabase is an optional:

- Backup
- Synchronization
- Disaster recovery
- Remote access layer
- Cross-location synchronization layer

It must not be required for normal local operation.

---

# 2. Maximum Users

Initial system limit:

```text
Maximum users = 5
```

This should be an application-level configurable limit.

Example:

```text
Company
 ├── User 1
 ├── User 2
 ├── User 3
 ├── User 4
 └── User 5
```

The system should prevent creation of a sixth active user unless the application license/configuration is changed.

---

# 3. Local Login

Users authenticate against the **local database**.

```text
Username / Email
Password
```

The login process:

```text
USER
 ↓
LOCAL LOGIN
 ↓
VERIFY PASSWORD
 ↓
LOAD USER
 ↓
LOAD ROLE
 ↓
LOAD PERMISSIONS
 ↓
OPEN APPLICATION
```

Internet connectivity must not be required for login.

---

# 4. User Hierarchy

Users should support organizational hierarchy.

Example:

```text
OWNER
  │
  ├── ADMIN
  │
  ├── PRINCIPAL ARCHITECT
  │      │
  │      ├── ARCHITECT
  │      └── PROJECT MANAGER
  │
  └── ACCOUNTS
```

Another company could use:

```text
OWNER
 ├── ARCHITECT 1
 ├── ARCHITECT 2
 ├── DRAFTSMAN
 └── ACCOUNTS
```

Therefore hierarchy must be configurable.

---

# 5. USERS TABLE

```sql
CREATE TABLE users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

  company_id UUID NOT NULL
    REFERENCES companies(id)
    ON DELETE CASCADE,

  username TEXT NOT NULL,

  email TEXT,

  password_hash TEXT NOT NULL,

  display_name TEXT NOT NULL,

  role_id UUID,

  manager_user_id UUID
    REFERENCES users(id)
    ON DELETE SET NULL,

  is_active BOOLEAN NOT NULL DEFAULT true,

  last_login_at TIMESTAMPTZ,

  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

  UNIQUE(company_id, username)
);
```

Important:

**Never store plain-text passwords.**

Use a secure password hashing algorithm such as Argon2id or bcrypt.

---

# 6. USER ROLES

Roles should be configurable.

```sql
CREATE TABLE roles (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

  company_id UUID
    REFERENCES companies(id)
    ON DELETE CASCADE,

  name TEXT NOT NULL,

  description TEXT,

  is_system_role BOOLEAN NOT NULL DEFAULT false,

  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

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

The company can create additional roles.

---

# 7. ROLE HIERARCHY

Do not rely only on the role name.

Store hierarchy explicitly.

```text
USER
 ├── role_id
 └── manager_user_id
```

For example:

```text
Vishal — OWNER
   │
   ├── Architect A — ARCHITECT
   │      │
   │      └── Drafter A — DRAFTSMAN
   │
   └── Accounts — ACCOUNTS
```

`manager_user_id` establishes the reporting relationship.

---

# 8. PERMISSIONS

Use granular permissions rather than hard-coding permissions into roles.

```sql
CREATE TABLE permissions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

  permission_key TEXT NOT NULL UNIQUE,

  name TEXT NOT NULL,

  description TEXT
);
```

Examples:

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

finance.view
finance.create
finance.edit

agreements.view
agreements.create
agreements.approve

backup.create
backup.restore

sync.view
sync.start
sync.configure
```

---

# 9. ROLE PERMISSIONS

```sql
CREATE TABLE role_permissions (
  role_id UUID NOT NULL
    REFERENCES roles(id)
    ON DELETE CASCADE,

  permission_id UUID NOT NULL
    REFERENCES permissions(id)
    ON DELETE CASCADE,

  PRIMARY KEY(role_id, permission_id)
);
```

Example:

```text
OWNER
 → Everything

ADMIN
 → Everything except ownership/system controls

ARCHITECT
 → Clients
 → Projects
 → Drawings
 → Approvals
 → Onboarding

DRAFTSMAN
 → Assigned Projects
 → Drawings
 → Documents

ACCOUNTS
 → Clients
 → Fees
 → Invoices
 → Payments

VIEWER
 → Read only
```

---

# 10. PROJECT-LEVEL ACCESS

Role permissions alone are insufficient.

A user may have access only to selected projects.

Create:

```sql
CREATE TABLE project_users (
  project_id UUID NOT NULL
    REFERENCES projects(id)
    ON DELETE CASCADE,

  user_id UUID NOT NULL
    REFERENCES users(id)
    ON DELETE CASCADE,

  access_level TEXT NOT NULL DEFAULT 'MEMBER',

  assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),

  PRIMARY KEY(project_id, user_id)
);
```

Access levels:

```text
OWNER
MANAGER
MEMBER
VIEWER
```

Example:

```text
Project: Villa A

Principal Architect → OWNER
Architect A          → MANAGER
Drafter A            → MEMBER
Accounts             → VIEWER
```

---

# 11. LOCAL DATABASE

Recommended local database:

```text
PostgreSQL
```

For a single-machine/small deployment, SQLite can be considered, but for:

- 5 concurrent users
- LAN access
- relational data
- transactions
- future synchronization
- complex permissions

PostgreSQL is the preferred architecture.

---

# 12. Local Server

One machine acts as the local server.

Example:

```text
Office Server PC
IP: 192.168.1.10
Port: 3000
```

Users connect through:

```text
http://192.168.1.10:3000
```

or a local hostname:

```text
http://architecture.local
```

Recommended deployment:

```text
                    OFFICE Wi-Fi
                         │
              ┌──────────┴──────────┐
              │                     │
          Server PC             User PCs
              │                     │
              ↓                     ↓
        App + Database          Browser/App
```

---

# 13. LAN-Only Security

The application should bind to the local network interface.

Do not expose the application directly to the public Internet.

Recommended firewall:

```text
ALLOW
192.168.x.x / local subnet
       ↓
Application Port

DENY
Internet / WAN
```

The database itself should **not** be directly exposed to client computers.

Instead:

```text
User Browser
     ↓
Application Server
     ↓
PostgreSQL
```

Never:

```text
User Browser
     ↓
PostgreSQL
```

---

# 14. Local Network Discovery

Future convenience feature:

```text
Architecture ERP Server
       ↓
LAN Discovery
       ↓
User opens application
```

The server can advertise itself using:

```text
mDNS / Bonjour
```

Example:

```text
http://architecture-erp.local
```

This avoids users needing to remember the IP address.

---

# 15. LOCAL FILE STORAGE

Large project files should remain local.

Examples:

```text
PDF
DWG
DXF
Images
Videos
DOCX
XLSX
Agreements
Invoices
Site Surveys
```

Recommended structure:

```text
/ERP_DATA
   /COMPANY
      /CLIENTS
         /CLIENT_ID
            /PROJECTS
               /PROJECT_ID
                  /DRAWINGS
                  /DOCUMENTS
                  /SITE
                  /AGREEMENTS
                  /INVOICES
                  /PHOTOS
                  /VIDEOS
```

The database stores metadata.

Example:

```text
documents.storage_path
```

rather than storing large binary files inside PostgreSQL.

---

# 16. LOCAL BACKUP

The application should support automatic local backups.

Example:

```text
/ERP_BACKUPS
    /DAILY
    /WEEKLY
    /MONTHLY
```

Backup should contain:

```text
Database
+
File Metadata
+
Application Configuration
```

Project files should also be included in a complete backup.

---

# 17. BACKUP SETTINGS

Company administrator can configure:

```text
Backup Enabled
Backup Location
Backup Frequency
Retention
Maximum Backup Copies
```

Example:

```text
Daily
Keep last 14
Weekly
Keep last 8
Monthly
Keep last 12
```

---

# 18. OPTIONAL SUPABASE CONNECTION

Supabase is an **optional external synchronization target**.

```text
LOCAL DATABASE
      │
      ├── Normal operation
      │
      └── Sync Engine
             ↓
          SUPABASE
```

If Supabase is unavailable:

```text
Application continues normally.
```

---

# 19. SUPABASE CONFIGURATION

Create:

```sql
CREATE TABLE sync_config (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

  company_id UUID NOT NULL
    REFERENCES companies(id)
    ON DELETE CASCADE,

  enabled BOOLEAN NOT NULL DEFAULT false,

  supabase_url TEXT,

  encrypted_credentials TEXT,

  last_sync_at TIMESTAMPTZ,

  sync_status TEXT DEFAULT 'NOT_CONFIGURED',

  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

Never store Supabase credentials as plain text.

---

# 20. SYNC ENGINE

The system should use a change queue rather than repeatedly uploading the entire database.

```text
LOCAL CHANGE
     ↓
CHANGE LOG
     ↓
SYNC QUEUE
     ↓
SUPABASE
```

---

# 21. CHANGE LOG

```sql
CREATE TABLE sync_changes (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

  company_id UUID NOT NULL
    REFERENCES companies(id)
    ON DELETE CASCADE,

  table_name TEXT NOT NULL,

  record_id UUID NOT NULL,

  operation TEXT NOT NULL,

  changed_at TIMESTAMPTZ NOT NULL DEFAULT now(),

  user_id UUID
    REFERENCES users(id),

  sync_status TEXT NOT NULL DEFAULT 'PENDING',

  synced_at TIMESTAMPTZ,

  error_message TEXT
);
```

Operations:

```text
INSERT
UPDATE
DELETE
```

---

# 22. SYNC STATES

```text
LOCAL_ONLY
PENDING_SYNC
SYNCING
SYNCED
SYNC_ERROR
CONFLICT
```

Example:

```text
Project edited
      ↓
LOCAL_ONLY
      ↓
PENDING_SYNC
      ↓
Internet available
      ↓
SYNCING
      ↓
SUPABASE
      ↓
SYNCED
```

---

# 23. OFFLINE-FIRST BEHAVIOUR

If Internet disappears:

```text
Internet
   X
   │
   ↓
Local Application
   ↓
Local Database
   ↓
Everything continues
```

Users can:

- Create clients
- Create projects
- Edit projects
- Create drawings
- Upload documents
- Create invoices
- Record payments
- Complete onboarding
- Generate agreements

without Internet.

---

# 24. RECONNECT BEHAVIOUR

When Internet becomes available:

```text
Internet detected
       ↓
Sync service starts
       ↓
Pending changes identified
       ↓
Changes uploaded
       ↓
Remote changes downloaded
       ↓
Conflict detection
       ↓
Database synchronized
```

---

# 25. CONFLICT HANDLING

Do not blindly overwrite data.

Example:

```text
User A:
Project Area = 10,000

User B:
Project Area = 12,000

Both edit while offline.
```

Sync engine detects:

```text
CONFLICT
```

Administrator can select:

```text
Keep Local
Keep Remote
Merge
Review
```

For critical commercial data such as:

```text
Agreement
Fee
Invoice
Payment
Tax
```

prefer explicit conflict resolution.

---

# 26. RECORD VERSIONING

Add version tracking to important synchronized tables.

Example:

```sql
ALTER TABLE projects
ADD COLUMN record_version BIGINT NOT NULL DEFAULT 1;
```

Every update increments:

```text
record_version
```

Also maintain:

```text
updated_at
updated_by
```

This makes synchronization safer.

---

# 27. AUDIT LOG

Because multiple users can modify the same project, maintain an audit log.

```sql
CREATE TABLE audit_logs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

  company_id UUID NOT NULL
    REFERENCES companies(id)
    ON DELETE CASCADE,

  user_id UUID
    REFERENCES users(id),

  table_name TEXT NOT NULL,

  record_id UUID NOT NULL,

  action TEXT NOT NULL,

  old_data JSONB,

  new_data JSONB,

  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

Example:

```text
ARCHITECT A
changed

Project Fee
₹5,00,000 → ₹5,50,000

04 Sep 2026
10:32 AM
```

---

# 28. USER SESSION

Local login sessions should be stored securely.

```sql
CREATE TABLE user_sessions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

  user_id UUID NOT NULL
    REFERENCES users(id)
    ON DELETE CASCADE,

  session_token_hash TEXT NOT NULL,

  ip_address INET,

  user_agent TEXT,

  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

  expires_at TIMESTAMPTZ NOT NULL,

  last_activity_at TIMESTAMPTZ
);
```

Sessions should expire and be revocable.

---

# 29. DEVICE REGISTRATION

Optional but recommended.

```sql
CREATE TABLE devices (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

  company_id UUID NOT NULL
    REFERENCES companies(id)
    ON DELETE CASCADE,

  device_name TEXT NOT NULL,

  device_identifier TEXT,

  ip_address INET,

  last_seen_at TIMESTAMPTZ,

  is_active BOOLEAN NOT NULL DEFAULT true,

  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

This allows the administrator to see:

```text
Office Server
Architect PC
Drafting PC
Accounts PC
Laptop
```

---

# 30. User Hierarchy + Project Hierarchy

There are two different hierarchies.

## Organizational hierarchy

```text
OWNER
 ↓
PRINCIPAL ARCHITECT
 ↓
ARCHITECT
 ↓
DRAFTSMAN
```

## Project hierarchy

```text
PROJECT
 ↓
PHASE
 ↓
LEVEL
 ↓
TASK / DRAWING / APPROVAL
```

Do not mix these two hierarchies.

---

# 31. Recommended Database Addition

The original database should therefore add these tables:

```text
users
roles
permissions
role_permissions
project_users
user_sessions
devices
audit_logs
sync_config
sync_changes
```

The existing tables remain:

```text
companies
company_tax_profiles
clients
projects
project_types
consultancy_types
consultancy_templates
template_scopes
template_deliverables
template_phases
template_fees
template_payment_milestones
agreement_templates
project_scopes
project_deliverables
project_fees
project_payment_milestones
project_phases
execution_levels
drawings
client_approvals
technical_approvals
backlogs
tasks
documents
onboarding_templates
onboarding_sections
onboarding_questions
onboarding_sessions
onboarding_responses
onboarding_clarifications
project_briefs
project_agreements
agreement_snapshots
scope_variations
invoices
invoice_items
payments
```

---

# 32. Deployment Model

Recommended first deployment:

```text
                 OFFICE ROUTER
                     │
               Local Wi-Fi
                     │
          ┌──────────┴──────────┐
          │                     │
      SERVER PC              USER DEVICES
          │                     │
          │              ┌──────┼──────┐
          │              │      │      │
          │             PC     PC    Laptop
          │
          ├── Application Server
          ├── PostgreSQL
          ├── Local File Storage
          └── Backup Storage
```

No Internet is required for local operation.

---

# 33. Optional Cloud Layer

When enabled:

```text
                    INTERNET
                       │
                    SUPABASE
                       │
                 Sync / Backup
                       │
                LOCAL ERP SERVER
                       │
                Local PostgreSQL
                       │
              ┌────────┼────────┐
              │        │        │
             User     User     User
```

Supabase should be considered the **remote copy**, not the authority for day-to-day LAN operation.

---

# 34. Sync Direction

Initially support:

```text
LOCAL → SUPABASE
```

for:

- Backup
- Disaster recovery

Then optionally support:

```text
LOCAL ↔ SUPABASE
```

for:

- Multiple offices
- Remote access
- Remote backup restoration
- Cross-location synchronization

The second mode requires stronger conflict resolution.

---

# 35. Backup vs Sync

These are separate features.

## Backup

```text
Local DB
   ↓
Supabase
```

Purpose:

```text
Disaster recovery
```

## Sync

```text
Local DB
   ↕
Supabase
   ↕
Another Local DB
```

Purpose:

```text
Synchronize multiple installations
```

Do not implement them as the same operation.

---

# 36. Recommended MVP

### Phase 1

Build:

```text
Local Server
Local PostgreSQL
Local Login
Maximum 5 Users
Roles
Permissions
User Hierarchy
Project Assignment
Local Files
Local Backup
Audit Log
```

### Phase 2

Add:

```text
Supabase Connection
Cloud Backup
Change Queue
Sync Status
```

### Phase 3

Add:

```text
Two-way Synchronization
Conflict Resolution
Multiple Offices
Remote Recovery
```

---

# 37. Important Security Rule

The system should have **two independent security boundaries**:

```text
NETWORK SECURITY
        +
APPLICATION SECURITY
```

Network security:

```text
Only LAN devices can reach server.
```

Application security:

```text
User must authenticate.
Role determines permissions.
Project assignment determines project access.
```

Being connected to the office Wi-Fi must **not** automatically grant access to company data.

---

# 38. Final Architecture

```text
                         COMPANY
                            │
          ┌─────────────────┼─────────────────┐
          │                 │                 │
        USERS             CLIENTS          TEMPLATES
          │                 │                 │
     ROLES / ACL            │           CONSULTANCY
          │                 │           ONBOARDING
          │                 │           AGREEMENT
          │                 │                 │
          └─────────────────┼─────────────────┘
                            │
                         PROJECT
                            │
             ┌──────────────┼──────────────┐
             │              │              │
           BRIEF          SCOPE           FEE
             │              │              │
             └──────────────┼──────────────┘
                            │
                        AGREEMENT
                            │
                     ┌──────┴──────┐
                     │             │
                   DESIGN       EXECUTION
                     │             │
                  DRAWINGS      LEVELS
                     │             │
                 APPROVALS      TASKS
                     │             │
                     └──────┬──────┘
                            │
                         FINANCE
                            │
                   INVOICE / PAYMENT


LOCAL-FIRST INFRASTRUCTURE
────────────────────────────────────────────

             LOCAL OFFICE Wi-Fi
                     │
              LOCAL ERP SERVER
                     │
        ┌────────────┼────────────┐
        │            │            │
     PostgreSQL   File Store   Backup
        │
     Local Login
        │
    5 Users Maximum
        │
   Roles / Hierarchy
        │
    Audit / Changes
        │
    Optional Sync
        ↓
     SUPABASE
```

---

# 39. Core Design Principle

The application should be designed as:

> **A self-contained local ERP that works completely offline, supports up to five authenticated users over the local Wi-Fi network, allows configurable organizational/user hierarchy and project-level permissions, and optionally synchronizes or backs up its local data to Supabase when an Internet connection is available.**

This means **Supabase is not a dependency of the application**. If Supabase is disconnected, the ERP continues operating normally.