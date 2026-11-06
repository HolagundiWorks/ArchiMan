# Google Play submission report — ArchiMan 1.2.0

**Prepared:** 6 November 2026  
**Package:** `com.holagundi.archiman`  
**Version name:** 1.2.0  
**Version code:** 8  
**Target API:** 35 · **Minimum API:** 26 (Android 8.0)  
**Release build:** `gradlew.bat assembleProduction` (owner signing required)  
**License:** MIT (Viswabhiram Holagundi, 2026)

This document is the copy-and-checklist pack for Google Play Console. Paste listing text into the store listing forms; use the Data safety and permissions sections when completing those questionnaires.

---

## 1. Store listing (English — India / primary)

### App name (30 characters max)

```
ArchiMan
```

### Short description (80 characters max)

```
Offline practice hub for architects: projects, measurements, site work and fees.
```

### Full description (4000 characters max)

```
ArchiMan keeps your architecture practice on your phone.

Projects, clients, drawings, site issues, measurements, invoices and encrypted backups stay on the device. There is no cloud account and no practice data uploaded to ArchiMan servers.

WHAT YOU CAN DO

• Practice dashboard — open tasks, sheets to review, site issues and fee balances at a glance
• Projects — portfolio, levels and rooms, brief, tasks and documents
• Measurement Book — quantity sheets with draft → submit → check → approve; CSV export
• Record measurements — contractor-first capture with live formulas (count, length, area, plaster, volume)
• Site work — daily logs, snags/NCRs, RFIs, submittals, drawings and transmittals
• Fees — proposals, agreements, client invoices, receipts, GST and TDS-aware balances
• Backup — password-encrypted practice backups you keep offline; forgotten passwords cannot be recovered
• Optional Wi‑Fi portal — open an HTTPS workspace on a trusted local network for letterhead, catalogues, estimates and exports (match the certificate fingerprint shown on the phone)

DESIGNED FOR FREELANCE PRACTICE

ArchiMan follows IBM Carbon colours and productive typography on Android. Use System, Light or Dark appearance. A home-screen widget shows practice KPIs without exposing project titles.

PRIVACY FIRST

• Practice data stays on the phone
• Android cloud backup is disabled for app data
• Optional LAN portal uses HTTPS only; stop the portal when finished
• Protect exported PDF/Excel/CSV as password-locked files when sharing

REQUIREMENTS

Android 8.0 or later. A device screen lock (PIN, pattern or biometric) is recommended before storing client and fee records.
```

### App category

**Suggested primary:** Business  
**Suggested tags / audience:** Architecture, construction admin, freelancers, professional tools

### Contact details (fill in Play Console)

| Field | Suggested value |
|---|---|
| Email | *(owner support email — required)* |
| Website | *(optional practice / product site)* |
| Privacy policy URL | **Required before publishing** — host a short policy (see §5 draft) |

---

## 2. What’s new (1.2.0)

Paste into “Release notes” for this version:

```
• Faster practice and project screens (lighter database subscriptions, smoother lists)
• Stronger Wi‑Fi portal: Carbon layout, practice backup/restore from a computer before opening the company, clearer HTTPS host guidance
• Measurement capture refinements and production hardening (app lock when backgrounded, safer recovery storage, schema indexes)
• Widget shows aggregates only — no project or task titles
```

---

## 3. Technical release identity

| Item | Value |
|---|---|
| Application ID | `com.holagundi.archiman` |
| versionCode | `8` |
| versionName | `1.2.0` |
| compileSdk / targetSdk | 35 |
| minSdk | 26 |
| Room schema | 32 |
| Signing | Upload keystore via `.signing/release.properties` or `ARCHIMAN_*` env vars — see `scripts/New-ReleaseSigning.ps1` |
| Shrink / minify | Enabled on `release` |
| Cleartext traffic | Disabled |
| `allowBackup` | `false` |
| Data extraction | Cloud backup and device transfer excluded (`data_extraction_rules.xml`) |

**Do not upload:** debug APK, `.audit` variant (`com.holagundi.archiman.audit`), signing material, practice backups, or `app/build/audit/` artifacts.

**Build command for Play:**

```sh
gradlew.bat assembleProduction
```

Upload the signed AAB/APK from the release output after `assembleProduction` succeeds. Prefer **Android App Bundle (AAB)** for Play if you add `bundleProduction` / `bundleRelease` to your release habit; today the documented production task builds the release APK via `assembleRelease`.

---

## 4. Permissions — Play Console justification

Declare only what the manifest requests:

| Permission | Why ArchiMan needs it | User-visible behaviour |
|---|---|---|
| `INTERNET` | Local HTTPS Wi‑Fi portal sockets; no ArchiMan cloud API | Portal works only while started on the same LAN |
| `ACCESS_NETWORK_STATE` | Detect Wi‑Fi / choose bind address for the portal | “Connect to Wi‑Fi” guidance when offline cellular-only |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_DATA_SYNC` | Keep the portal service alive with a notification while sharing the practice on LAN | Persistent “Wi‑Fi portal” notification with Stop |
| `POST_NOTIFICATIONS` | Android 13+ notification for the portal foreground service | Asked when the user starts the portal |

**Not requested:** camera, contacts, location, SMS, storage “all files”, microphone, nearby devices, exact alarm, etc.

**File sharing:** `FileProvider` for user-initiated exports and restored attachments only (not exported).

---

## 5. Data safety form (draft answers)

Play Data safety must match the product. Suggested answers for **this build**:

### Data collection / sharing

| Question | Answer | Notes |
|---|---|---|
| Does the app collect or share user data with third parties / your servers? | **No collection to ArchiMan servers** | Practice stays on-device |
| Is data encrypted in transit? | **Yes, where network is used** | Portal is HTTPS/TLS 1.2+ only |
| Can users request deletion? | **Yes (on-device)** | User deletes the app / restores a blank practice / clears app data |

### Data types that may exist **on the device** (not sent to ArchiMan)

Mark as **collected** only if Play’s definition includes on-device processing that you also transmit. For a strict offline app with no analytics SDK and no backend:

- Prefer: **“No data collected”** / **“No data shared”** if you do not transmit practice data off-device via ArchiMan infrastructure.
- Disclose in the privacy policy that **the user** may export backups and documents; those files leave the device only when the user chooses (files, email, LAN portal download).

If Play reviewers ask about the portal: it is a **local HTTPS server on the phone**; the desktop browser talks to the phone on the LAN. ArchiMan does not operate a cloud endpoint for that traffic.

### Security practices to affirm

- [x] Data is encrypted in transit (portal TLS)
- [x] Users can delete data (uninstall / clear storage / restore)
- [x] Committed to Google Play Families Policy if you ever target children — **this app is not for children**
- [ ] Independent security review — optional; not claimed

### Sensitive categories

| Topic | Guidance |
|---|---|
| Financial info | Invoices/receipts/GST may be stored **on device**. Do not claim “we collect financial info” unless you also transmit it. |
| Personal info | Client names, phones, emails on device. Same rule. |
| Location | Not collected. |
| Photos / files | User may attach evidence / logos; stored in app-private files. |
| Device IDs / advertising ID | Not used. Do not declare Ads. |

---

## 6. Privacy policy — short draft to host

Host this (or an equivalent) at a public HTTPS URL and paste the URL into Play Console. Replace bracketed fields.

```
ArchiMan Privacy Policy

Effective date: 6 November 2026
Developer: [Legal name / Viswabhiram Holagundi]
Contact: [support email]

ArchiMan is an offline-first Android application for architecture practices. Practice data (projects, clients, measurements, fees, drawings and related records) is stored on your device.

We do not operate an ArchiMan cloud account for your practice data. We do not sell your data.

Optional Wi‑Fi portal: when you start the portal, the app serves an HTTPS page on your local network so a computer you choose can view or edit practice data while the portal is running. Traffic stays on your network between your phone and that computer. Stop the portal when finished. Compare the certificate fingerprint shown in the app before trusting a browser warning.

Backups and exports: password-protected backups and document files are created only when you ask. Keep passwords separately; forgotten backup passwords cannot be recovered. Files you share leave the device under your control.

Permissions: Internet and network state are used for the local portal. Notifications are used to show that the portal is running. The app disables Android cloud backup of its private data.

Children: ArchiMan is intended for professional adult users, not for children under 13.

Changes: we may update this policy; the effective date above will change. Contact us with questions at the email above.
```

---

## 7. Content rating (IARC questionnaire — expected)

Expect a **Everyone** / low maturity rating if answers are:

- No violence, sexual content, or gambling
- No user-generated public social feed
- Not primarily for children
- May include **mild business/financial** record-keeping (invoices) — answer questionnaire truthfully
- No unrestricted web browser; portal is LAN-only and started by the user

Complete the official questionnaire in Play Console; do not guess a rating code.

---

## 8. Target audience & news apps

| Setting | Recommended |
|---|---|
| Target age | 18+ / professionals (not Designed for Families) |
| News app | No |
| COVID / health | No |
| Government apps | No |

---

## 9. Store assets checklist (still required)

Play will reject incomplete graphics. Produce these outside the repo (or add under `docs/store/assets/` later):

| Asset | Spec | Status |
|---|---|---|
| App icon | 512 × 512 PNG (32-bit) | **Missing** — current launcher is vector-only in-app |
| Feature graphic | 1024 × 500 PNG/JPG | **Missing** |
| Phone screenshots | ≥ 2, up to 8; 16:9 or 9:16 | **Missing** |
| 7" / 10" tablet screenshots | Optional | Optional |
| Promo video | Optional YouTube | Optional |
| Privacy policy URL | Public HTTPS | **Missing** — host §5 |

**Suggested screenshot set (phone):**

1. Practice dashboard (Studio / KPIs / needs attention)  
2. Project overview  
3. Measurement Book sheet  
4. Record measurement  
5. Practice → Backup / Wi‑Fi portal pairing  
6. Fee invoice or site issue (one of)

Blur or use demo company data only — never ship real client PII in store screenshots.

---

## 10. Pre-upload verification checklist

From production readiness and this release:

- [ ] Device PIN/pattern/biometric enabled on test devices  
- [ ] `gradlew.bat testDebugUnitTest lintDebug` green  
- [ ] `gradlew.bat assembleProduction` with owner signing  
- [ ] Install production build; confirm app lock after backgrounding  
- [ ] Backup encrypt → restore preview → restore on a throwaway install  
- [ ] Portal: start → HTTPS open → fingerprint match → stop  
- [ ] Widget: aggregates only (no titles)  
- [ ] Privacy policy URL live  
- [ ] Listing + screenshots + 512 icon + feature graphic uploaded  
- [ ] Data safety form saved to match §5  
- [ ] Content rating questionnaire completed  
- [ ] Release notes (§2) attached to the release track  

---

## 11. Known product boundaries (do not overclaim in the listing)

1. No separate ArchiMan account password — OS screen lock is the second gate.  
2. Room database is not SQLCipher; protection relies on app-private storage and device encryption.  
3. Wi‑Fi portal is for a **trusted LAN**; always verify the TLS fingerprint.  
4. Forgotten backup passwords cannot be recovered.  
5. The phone remains the system of record; the portal does not replace offline custody of backups.

---

## 12. Support / review notes for Play reviewers

If reviewers ask how networking is used:

> ArchiMan does not sync practice data to a developer cloud. The optional Wi‑Fi portal binds an HTTPS server to the phone’s Wi‑Fi address (port 8765) so a browser on the same LAN can access practice tools while the user keeps the portal running. The foreground notification controls that service.

Contact for closed testing / review account: provide a demo company path (**Practice → Demo**) and note that no login server is required after local company setup.
