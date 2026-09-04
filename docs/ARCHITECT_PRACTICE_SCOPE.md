# ArchiMan Architectural Practice Scope

> Current baseline: ArchiMan — Architectural Consultancy Management App, Room schema 21, reviewed 4 September 2026.

## Product boundary

ArchiMan is an offline-first Architectural Consultancy Management App with project administration and measurement-book capabilities. It is not a statutory approval portal, CAD/BIM authoring package, accounting system, billing system, or launcher.

ArchiMan may expose a write-enabled web workspace on the phone's current local Wi-Fi network only after an explicit user action. Access uses named local accounts with Admin, Editor or Viewer roles. Passwords are salted and deliberately slow-hashed; data is disclosed only after authentication; state-changing requests require CSRF tokens and create append-only audit events. The workspace uses phone-generated HTTPS, binds to the Wi-Fi address, performs no cloud upload, and stops on user request, after one hour, or on application teardown.

Company/practice identity is maintained once at portfolio level. Each project has a separate profile containing its code, type, status, client, location, architect, programme dates and area summary. Controlled exports consume snapshots of these profiles so later edits do not rewrite historical issued documents.

The approval/compliance matrix and jurisdiction-profile proposal is explicitly excluded. Approval portals and local rules vary by authority and change independently of ArchiMan.

## Canonical project workspaces

The schema-21 release implements expanded practice/client profiles, project site data, Brief & Scope, planning, measurement, contractor rate books, drawing-register foundations, site reports and controlled LAN quick entry for tasks, approvals and backlog actions. Items below that require full DWG rendering, coordination workflows, organisation identity or synchronisation are approved roadmap scope, not claims about the current APK.

1. Drawings
   - DWG viewing, layers, pan, zoom and extents.
   - Non-destructive annotations and calibrated distance, area and angle measurements.
   - Drawing register, immutable revisions, issue status and as-built classification.
   - Transmittals with recipients, purpose, included revisions and acknowledgement.
2. Coordination
   - RFI register and responses.
   - Submittals and shop-drawing review.
   - Numbered site instructions and compliance evidence.
   - Consultant directory, disciplines and responsibility matrix.
3. Field
   - Daily progress reports.
   - Snags, defects and NCRs with assignment, before/after evidence and verified closure.
   - Existing inspections, meeting minutes and Measurement Book.
4. Programme
   - Activities, milestones, dependencies, baseline and current dates.
   - Progress updates, look-ahead view and delay reasons.
5. Handover
   - As-built drawing set.
   - O&M manuals, warranties, test certificates, asset/key schedules and completion checklist.
   - Versioned exportable handover package with a manifest.
6. Platform
   - Organisation/project roles and least-privilege permissions.
   - Offline-first change queue, explicit conflict resolution and device registration.
   - Append-only audit events, encrypted backups and tested restore.
   - External storage, email/calendar, signature and CAD-engine adapters.

## DWG architecture decision

DWG files remain the source artefacts. ArchiMan stores a controlled local copy, checksum, metadata and revision identity. Rendering is supplied through a `DrawingViewerEngine` boundary so the application can use a licensed native Android DWG engine without coupling project records to its API.

Annotations and measurements are stored separately from the DWG using drawing-space coordinates. They never rewrite the source drawing. Each markup is tied to one drawing revision and records its author, creation time, calibration and unit.

If a renderer is unavailable, ArchiMan must clearly report that the drawing is registered but cannot be rendered. It must never display a raster/PDF derivative as though it were the authoritative DWG.

## Delivery sequence

### Phase A - Controlled documents

- Drawing register and revision intake.
- File checksum, managed storage and duplicate detection.
- Transmittals and revision-aware issue history.
- Viewer-engine contract and capability reporting.

### Phase B - DWG review

- Native DWG rendering integration.
- Layer visibility, pan, zoom, fit/extents and sheet/model-space selection.
- Calibrated distance, polyline length, area and angle measurements.
- Pins, clouds, arrows, freehand, text and colour-coded annotation layers.
- Export a flattened review PDF while retaining editable markups.

### Phase C - Construction administration

- RFI, submittal and site-instruction workflows linked to drawing revisions and locations.
- Daily reports, snagging and NCR verified closure.
- Consultant directory and responsibility matrix.

### Phase D - Delivery control

- Dependency-aware programme and progress reporting.
- Handover register and as-built package.

### Phase E - Enterprise platform

- Roles and permissions.
- Offline synchronisation and explicit conflict handling.
- Immutable organisation-wide audit trail, encrypted backup and restore drills.
- External integrations through replaceable adapters.

## Non-negotiable record rules

- A drawing revision is immutable after it is issued.
- A transmittal references exact revisions, never only a drawing number.
- An RFI or instruction retains the drawing revision that was current when it was raised.
- Closing a snag or NCR requires rectification evidence and independent verification.
- Historical programme baselines and progress updates are append-only.
- An as-built package is a versioned manifest, not an untracked folder export.
- Synchronisation never silently resolves competing edits to controlled records.
