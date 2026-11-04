# ArchiMan

**ArchiMan** is an offline-first app for a freelance architect. The phone holds projects, clients, briefs, proposals, contracts, a task board, letterhead letters, fee invoices, site-visit reports, measurements and a running bill.

The Kotlin app lives in `app/`. The React files under `src/` are the earlier prototype and are not the product build.

## Stack

- Kotlin, Jetpack Compose, Material 3 glass surfaces
- Room schema 28, Hilt, Navigation Compose, Coroutines
- Archi Calc and the measurement-sheet workflow ported from the prototype domain

## Run

Install JDK 17 and Android SDK 35, then:

```sh
gradlew.bat :app:assembleDebug
```

Open the `app` module in Android Studio and run it on a device or emulator. minSdk is 26. Backdrop blur is used on Android 12+; earlier devices get a solid translucent surface.

## Product surface

Projects, Clients, Invoices and Practice. An open project has Brief, Tasks, Letters, Proposal, Contract, Visits, Levels, Measure, M-Book, Invoice and Running bill. Measure includes Archi Calc, with metric and imperial lengths. Letters use the practice profile as the letterhead.

---

## Key Features & Architecture

The table below is older practice scope. The phone now keeps the freelance set only: projects, clients, briefs, proposals, contracts, tasks, letters, invoices, visits, measurement and running bills.

| Module | Features & Capabilities |
|---|---|
| **Projects & Portfolio** | Multi-project portfolio hub, client associations, built-up & plot areas, building level & room space hierarchy. Project creation contract automatically seeds a Ground Floor baseline. |
| **Measurement Book (M-Book)** | Quantity-only sheets grouped by contractor, level, and work item. Formal review lifecycle: `DRAFT` → `SUBMITTED` → `CHECKED` → `APPROVED` / `RETURNED` with audit comments. CSV export. Strictly quantity-only (no rates, amounts, or commercial billing). |
| **Record Measurement** | Contractor-first entry flow with real-time formula computation across `NOS`, `RUNNING_LENGTH`, `AREA`, `WALL_PLASTER` (with opening deductions), and `VOLUME`. Integrates directly with Archi Calc. |
| **Siteman Hub** | Site-manager command surface: Daily Site Logs (weather, manpower, work completed, materials received), HFC Drawing Package tagging & site documents, Aggregated Quantity Abstract with CSV export, Material Purchase Orders (quantity & technical spec only), and Archi Calc. |
| **Archi Calc** | Embedded AcharyaOS/Paperdesk calculation engine with expression evaluation, trigonometric functions (`sin`, `cos`, `tan`), powers, square roots, reciprocal, and a real-time 6-way length/area/volume conversion grid (`m`, `m²`, `m³`, `ft`, `ft²`, `ft³`). |
| **Site Reports & Quality** | Snag & NCR defect tracker with discovery (before) evidence, rectification (after) evidence, severity levels (`NORMAL`, `ATTENTION`, `CRITICAL`), and mandatory architect verification note signoff. |
| **Coordination & Matrix** | Multi-discipline Requests for Information (RFIs), technical Submittals, Site Instructions (SIs), and Consultants RACI responsibility matrix (Responsible, Accountable, Consulted, Informed). |
| **Drawings Register** | Master drawing register with disciplines, HFC tagging, revision genealogy (`REV-A`, `REV-B`), revision sources (client request, site condition, architect review), as-built flags, and transmittal generation. |
| **Practice & Data Portability** | Practice profile (Council of Architecture CoA registration, principal credentials, PAN, GSTIN), full database backup/export (`.archimandb.json`), restore from file, and diagnostics. |

---

## Earlier web prototype

The files under `src/` are a React + Vite prototype of the same workflows. They are not the Android product. Do not treat `npm run dev` as the app.

```sh
npm install
npm run dev
```

---

## License
MIT — See `LICENSE` and `NOTICE`.
