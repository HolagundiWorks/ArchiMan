# ArchiMan (React Web Application)

**ArchiMan** is an architectural consultancy management system rewritten as a modern React application.

It manages practice projects, client and contractor registries, coordination workflows (RFIs / submittals / site instructions), snag/NCR tracking with evidence notes, project decisions, site reports, a quantity-only Measurement Book, and **Siteman** — the site-manager command hub for daily logs, Held-for-Construction (HFC) drawing registers, quantity abstracts, material purchase orders, and the embedded Archi Calc engine.

---

## Key Features & Architecture

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

## Development & Build

This project is built with:
- **React 18** + **TypeScript**
- **Vite** bundler
- **Tailwind CSS v4** with a **Material Design 3** token layer (`src/theme/tokens.css`) and shared UI primitives (`src/components/ui`)
- **Lucide React** iconography

### Running Locally
```sh
npm install
npm run dev
```
The application runs on `http://localhost:3000`.

### Production Build
```sh
npm run build
```

---

## License
MIT — See `LICENSE` and `NOTICE`.
