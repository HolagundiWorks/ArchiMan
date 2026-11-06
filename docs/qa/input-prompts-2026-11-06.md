# Input prompt automation — 6 November 2026

Validates that every create/edit prompt is formulated (clear required labels) and working (rejects blank/invalid, accepts valid, trims whitespace).

## How to run

```sh
gradlew.bat :app:testDebugUnitTest --tests com.holagundi.archiman.domain.InputPromptFormulationTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -e suite inputPrompts com.holagundi.archiman.test/com.holagundi.archiman.BackupDeviceChecks
```

## Coverage

### Repository / domain prompts (`InputPromptDeviceChecks`)
- RecordKind field labels (unique columns, non-blank labels, required flags)
- Client, project, floor, room, company create
- Enquiry, letter, rate, meeting, daily log, snag, coordination
- Drawing, revision, inspection, transmittal, permit, time
- Proposal, task, brief, invoice, approval, contract, activity, order
- Finance settings / invoice / amount parsing
- Receipt TDS reference
- Record editor required fields
- Consultancy title/fee/placeholder rules
- Work-item formula + password envelope
- Snag close verification note

### UI labels (`InputPromptUiChecks`)
- New project required labels + blank submit guard
- Clients, Brief, Tasks, Letters, Visits, Snags, Coordination, Siteman, Invoices, Fee proposals

### Production fixes from this pass
- `ProjectRepository.createProject` now requires project name and client name (matches UI)
- `addFloor` / `addRoom` reject blank names

## Status (SM-M115F)
- JVM formulation tests: **PASS**
- Device repository/domain prompts: **107 PASS**
- Device UI prompt labels: **14 PASS**
- Overall: **PASSED** (~2 min)
