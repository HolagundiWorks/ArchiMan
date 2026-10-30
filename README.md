# ArchiMan

**ArchiMan** is an open-source, local-only Android app for architectural consultancy practices.

It covers projects, clients and contractors, coordination (RFIs / submittals / site instructions), snag/NCR closure, decisions, site reports, a quantity-only Measurement Book, and **Siteman** — the site-manager workspace for daily logs, HFC drawings & docs, quantity abstracts, material purchase orders, and Archi Calc.

ArchiMan keeps every practice's data on the phone that installs it. There is no cloud account, no server, and no background sync — the phone is the database. A built-in, password-protected LAN web portal lets other people on the same trusted Wi-Fi network view and enter records through a browser, but nothing ever leaves the local network.

## License

**MIT** — see [`LICENSE`](LICENSE) and [`NOTICE`](NOTICE).

Third-party attribution (Carbon Icons, Archi Calc / AcharyaOS engines, AndroidX, Coil, Moshi, test tooling, etc.) lives in [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).

Dependencies are Apache-2.0 / MIT (or test-only EPL/MIT) and are compatible with distributing ArchiMan under MIT.

## Features (current baseline: Room schema 27)

| Area | What you get |
|---|---|
| Projects & practice | Portfolio, clients, contractors, company profile, encrypted `.archimandb` company backup |
| Measurement Book | Quantity-only sheets, record flow, CSV / XLS / PDF export — **no rates, amounts, or invoices** |
| Site reports | Daily logs, snag/NCR with before/after evidence, inspections, meeting minutes, CSV/PDF |
| **Siteman** | Site-manager hub under Project → More: logs, HFC package tags + docs, quantity abstract, qty/spec POs, Archi Calc |
| Coordination | RFIs, submittals, site instructions, consultant matrix |
| Drawings | Register, revisions, transmittals (native DWG viewing remains roadmap) |
| LAN portal | Same phone DB over trusted Wi-Fi; Siteman HFC / POs / Archi Calc are **phone-only** today |

Full documentation index: [`docs/README.md`](docs/README.md).

### Try Siteman

1. Open a project → **More** → **Siteman**.
2. **Site logs** — FAB to add a daily log (photos open from the card).
3. **HFC drawings & docs** — tag drawings as HFC; attach supporting documents.
4. **Quantity abstract** — totals from the Measurement Book; export or open M-Book / Record.
5. **Purchase orders** — create qty/spec lines, **Issue**, export CSV. Drafts are create / issue / export / delete (line edit after create is not available yet).
6. **Archi Calc** — expression pad plus m/ft length, area and volume grid.

## Build

```sh
# JDK 21+ recommended (required for Robolectric tests against compileSdk 36)
gradle testDebugUnitTest assembleDebug
# or, if you use the wrapper when present:
./gradlew testDebugUnitTest assembleDebug
```

Requires an Android SDK (see `local.properties`) and **JDK 21+** for the full unit-test suite. Product-boundary gate:

```sh
pwsh ./scripts/verify-product-boundary.ps1
```

CI runs the same gate + `testDebugUnitTest` + `assembleDebug` (see `.github/workflows/android-quality-gate.yml`).

### Package IDs

| | Value |
|---|---|
| Application id (Play / install) | `com.aistudio.sitemeasure.qvtrpl` |
| Kotlin / Room namespace | `com.example` |

The install id is stable so field upgrades keep their database. The `com.example` source namespace is historical; a rename is a separate migration, not required to build or contribute.

## Data portability

- **Company database (`.archimandb`)** — password-encrypted export of the entire practice database (projects, measurements, portal users, logo, managed photos). Practice → Company profile & connections.
- **Company profile** — lighter practice-identity export for seeding a fresh install without projects.

A fictional company package for acceptance testing lives under [`demo`](demo/README.md).

## Contributing

- Match existing Material 3 / Carbon-icon patterns and the quantity-only product boundary (the CI script rejects commercial rate/amount/bill/payment fields in Kotlin sources).
- Prefer additive Room migrations; never drop field measurement data.
- Keep Siteman and site-field features honest in docs: phone-only where the portal has not caught up; MVP call-outs (e.g. PO line edit) belong in strings/docs, not silent stubs.

## Docs

See [`docs/README.md`](docs/README.md) for architecture, hierarchy, roadmap, migration/backup and support runbooks.
