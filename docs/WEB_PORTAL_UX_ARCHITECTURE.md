# ArchiMan Web Portal UX Architecture

## Purpose

The web portal is a full-access client for the phone-hosted ArchiMan database. It does not create a second database and does not depend on an internet connection or a CDN. Authentication, role checks, validation, audit events and persistence remain on the Android device.

## Design system

The portal uses an offline implementation of Carbon Design System foundations and interaction patterns:

- IBM Plex-compatible typography stack with system fallbacks;
- Carbon gray and blue tokens, square controls, 48 px shell and control rhythm;
- persistent global header and left product navigation on desktop;
- horizontally scrollable product navigation on narrow screens;
- 16/32 px responsive grid and consistent key lines;
- data-table toolbar, status tags, structured empty states and inline notifications;
- progressive-disclosure forms with visible labels and keyboard focus states;
- semantic landmarks, heading hierarchy and a skip-to-content link.

No Carbon JavaScript or remote assets are loaded. This preserves the local-only security boundary and keeps the portal usable when the phone has no internet connection.

## Information architecture

| Area | Operator goal | Primary records and actions |
| --- | --- | --- |
| Dashboard | Understand current workload | KPIs, project register, schedule, site attention, coordination |
| Projects | Establish project context | Project profile, client, contractor assignment, floors, scope |
| Planning | Control delivery | Tasks, approvals, backlog, programme, selections |
| Site management | Record field activity | Inspections, drawing references, meeting minutes |
| Coordination | Close communication loops | RFI, submittal, site instruction, RACI responsibilities |
| Measurements | Maintain the measurement book | Rows, books, work items, floors, review transitions |
| Directory and library | Maintain reusable master data | Clients, contractors, contractor qualification, work library |
| Administration | Govern the practice | Company profile, users, roles, project lifecycle, audit history |

## Mobile and web responsibility split

ArchiMan is not two independent products. The Android app owns storage, device security and field capture; the browser is the larger-screen operational client connected to that same repository.

### Mobile-first work

- start, stop and secure the local workspace;
- quick project lookup and essential contact access;
- site measurements, member descriptions and optional row photographs;
- inspection observations, snag/NCR evidence and site notes;
- field checks where offline operation, camera access or one-handed entry matters;
- company database import/export and device recovery controls.

### Web-first work

- company, client, consultant and contractor administration;
- project onboarding, profile, scope and responsibility setup;
- programme, task, dependency and progress control;
- drawing register, revisions, transmittals, RFI and submittal registers;
- meeting minutes, selection schedules and purchase-order preparation;
- measurement-book review, structured high-volume entry and approval transitions;
- portfolio dashboards, audit review and user administration.

Mobile can retain read access to office-managed records so site users have context. Office-heavy workflows should not be duplicated as dense mobile forms; they should open as concise summaries or field actions on the phone and as complete registers on the web.

The selected project is carried in the URL while moving between delivery areas. Unbounded project records are kept out of the navigation and shown through project drill-downs. Viewers see records without edit actions; administrator-only controls remain protected by server-side role checks.

## Security and performance controls

- The listener binds only to the phone's active Wi-Fi IPv4 address and accepts at most 12 active clients; excess sockets are closed immediately so a local connection burst cannot create unbounded coroutines.
- Request lines, header count, header length and form bodies are explicitly bounded. POST endpoints accept only URL-encoded forms, reject foreign `Host`/`Origin` values, require CSRF tokens and re-check authorization on the server.
- Sessions use high-entropy cookies with `HttpOnly`, `SameSite=Strict`, `Secure` under HTTPS, a 15-minute idle timeout and a 30-minute absolute timeout. Expired sessions and throttling records are actively purged and both collections are capped.
- Browser responses use no-store caching, a restrictive Content Security Policy, clickjacking and MIME protections, feature restrictions, and same-origin opener/resource isolation.
- Read snapshots are cached for two seconds per project context, avoiding repeated Room aggregation while keeping LAN edits responsive. Every successful mutation invalidates the cache before redirecting.
- Viewer accounts do not receive edit actions or Administration navigation. Editor and administrator controls remain backed by mutation-level authorization and immutable audit events.

These controls harden the current trusted-LAN deployment; they do not replace project-scoped authorization, penetration testing, signed release management, restore drills or an external security review required for production enterprise acceptance.

## ERP interaction rules

1. Start at a scan-friendly dashboard, not an entry form.
2. Establish company or project context before showing project-specific actions.
3. Keep one bounded module per navigation area and show only actions relevant to that area.
4. Use tables for registers and lists or tiles for compact operational summaries.
5. Keep destructive lifecycle actions in Administration and require an audit note.
6. Preserve server-side authorization regardless of whether a control is visible in the interface.
7. Explain empty states with the next valid action instead of showing blank containers.
8. Maintain usable layouts at desktop, tablet and phone browser widths.

## Current implementation boundary

The shell, module routing, responsive layout, role-aware edit rendering, bounded request processing, short-lived snapshot cache and existing create/workflow actions are implemented. Rich table filtering, pagination, bulk selection, saved views and field-by-field editing of every existing entity remain future enhancements; they must be added without introducing a second browser-side persistence layer.
