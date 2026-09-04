# Comparative UX case study: field and construction applications

Reviewed: 4 September 2026
Applied baseline: ArchiMan schema 21

## Objective

Study established mobile construction products for information-architecture patterns that improve ArchiMan without turning it into a generic cloud ERP. The comparison focuses on navigation, project context, field entry and the relationship between frequently used and secondary tools.

## Products reviewed

### Autodesk Forma / Construction Cloud mobile

Autodesk opens a selected project on Project Home, keeps Home available in the project bottom bar, exposes frequent tools such as Sheets, Files, Forms and Issues, and places the remaining tools under More. Project Home also supports a small set of shortcuts for common creation workflows. This is a strong model for separating frequent field actions from the complete tool catalogue.

Source: [Autodesk Project Home](https://help.autodesk.com/cloudhelp/ENU/Build-Mobile/files/Project-Home-Mobile.html)

### Procore mobile

Procore uses a Project Overview as the tool launch point and allows project tools to be displayed as a grid or list and ordered around the user's needs. Its Project Directory brings companies and people into one project-level directory instead of making each party type a separate top-level application.

Sources: [Configure project tool order on Android](https://en-gb.support.procore.com/procore-mobile-android/user-guide/project-overview-screen-android/tutorials/configure-the-tool-order-for-a-project-android), [Project Directory](https://support.procore.com/products/online/user-guide/project-level/directory)

### PlanRadar mobile

PlanRadar uses an explicit hierarchy: account, project, then feature. A project main menu exposes tickets, reports, documents, schedule, project details and statistics. Back navigation reverses the same hierarchy. Tickets can be viewed as a list or in plan context, and the current layer acts as a location filter.

Sources: [PlanRadar mobile navigation](https://help.planradar.com/hc/en-gb/articles/16571813818781-Mobile-App-Navigation), [Web and mobile roles](https://help.planradar.com/hc/en-gb/articles/22539675406365-PlanRadar-Webapp-Mobile-App-Overview)

### Fieldwire

Fieldwire is centred on offline field work: drawings, task scheduling, punch lists, inspections, photos, RFIs and submittals. Its location hierarchy can be nested and reused to filter and report project records. This supports ArchiMan's decision to preserve Project → Level → Room/Component as shared context rather than duplicating free-text locations in every module.

Sources: [Fieldwire Android product listing](https://play.google.com/store/apps/details?id=net.fieldwire.app), [Fieldwire offline behaviour](https://help.fieldwire.com/hc/en-us/articles/202631944-Q-A-Can-I-use-Fieldwire-while-offline)

## Repeated design patterns

1. Select the project before opening project tools.
2. Give the selected project a home/overview rather than landing in a random feature.
3. Keep three or four frequent field actions permanently reachable.
4. Put the complete long-tail tool list under More or a project tool directory.
5. Combine organisations and people into a Directory.
6. Preserve location context across drawings, tasks, issues and reports.
7. Make creation a prominent action; do not give every register equal navigation weight.
8. Design mobile for field capture and review, while deferring dense administration to larger-screen experiences.

## Application to ArchiMan

### Portfolio navigation

```text
Projects | Directory | Work List | More
```

- Directory contains Clients and Contractors as two views of one shared contact directory.
- Work List is promoted because PWD SR items, UOMs and formulae are shared master data used throughout measurement entry.
- More contains company profile and the local Wi-Fi portal; exports remain within the owning project workflow.

### Selected-project navigation

```text
Project | Work | Record | M-Book
```

- Record remains the prominent field action.
- Work and M-Book remain one tap away during measurement work.
- The project header back action returns to the portfolio; a duplicate Home tab is unnecessary.

### Project home sections

```text
Overview | Brief | Planning | More
```

- Overview provides identity, current status, next steps and small operational counts.
- Brief contains consultancy type, design phase, client requirements and agreed scope.
- Planning contains tasks, schedule and material/specification selections.
- More contains drawings, site reports, team and contractor rate books.

## Deliberately not copied

- Cloud-only assumptions, subscription limits and always-online authentication.
- Billing, accounting, procurement pricing or contract-finance navigation.
- A configurable tool launcher before roles and permissions exist; premature customization makes support and training harder.
- Automatic statutory compliance conclusions.
- Separate location systems per feature. ArchiMan should reuse the project execution-level hierarchy.

## Implemented structural result

- Portfolio navigation is now Projects, Directory, Work List and More.
- Project navigation is now Project, Work, Record and M-Book.
- The project workspace is now Overview, Brief, Planning and More.
- Clients and Contractors are consolidated under Directory.
- Drawings, site reports, project team and rate books are grouped under project More.

## Next structural steps

1. Make Project → Level → Room/Component a reusable location picker for drawings, tasks, inspections and future snags/RFIs.
2. Add recent activity and four user-role-aware shortcuts to Project Overview after permissions exist.
3. Split the current monolithic ViewModel, repository and project screen by feature without changing the user-facing hierarchy.
4. Add navigation tests for portfolio, directory, project and secondary-tool back paths.
