# ArchiMan

An open-source, local-only Android app for architectural consultancy practices: projects, contractors, coordination (RFIs/submittals/site instructions), snag/NCR closure, decisions, site reports and a quantity-only Measurement Book.

ArchiMan keeps every practice's data on the phone that installs it. There is no cloud account, no server, and no background sync — the phone is the database. A built-in, password-protected LAN web portal lets other people on the same trusted Wi-Fi network view and enter records through a browser, but nothing ever leaves the local network.

See [`docs/README.md`](docs/README.md) for the full documentation index — product scope, architecture, navigation hierarchy and the delivery roadmap.

## Build

```sh
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Requires an Android SDK (see `local.properties`) and JDK 17+.

## Data portability

- **Company database (`.archimandb`)** — a password-encrypted export of the entire practice database (projects, measurements, portal users, logo, managed photos) for backup or migrating to a new phone. Practice → Company profile & connections.
- **Company profile (`.archimandb`)** — a lighter export of just the practice identity and logo, for sharing or re-seeding a fresh install without carrying projects over.

## License

MIT — see [LICENSE](LICENSE).
