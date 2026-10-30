package com.example.ui.navigation

/** Stable destinations in the application hierarchy. */
enum class AppScreen(val title: String) {
    HOME("Portfolio"),
    COMPANY_PROFILE("Company Profile"),
    LOCAL_PORTAL("Local Wi-Fi Workspace"),
    PROJECT_WORKSPACE("Project Hub"),
    DEDICATED_MEASUREMENT("Record Measurement"),
    ROOM_WORKSPACE("Room Components"),
    MEASUREMENT_BOOK("Measurement Book"),
    CLIENTS("Clients"),
    CONTRACTORS("Contractors"),
    MASTER_DATA("PWD SR Work Catalogue"),
    COMPANY_DASHBOARD("Company Dashboard")
}

/**
 * Top-level portfolio destinations.
 * [label] matches the bottom-navigation copy users see (Contacts / Library / Practice).
 */
enum class HomeTab(val label: String) {
    PROJECTS("Projects"),
    DIRECTORY("Contacts"),
    WORK_LIBRARY("Library"),
    PRACTICE("Practice")
}

/** Pages within the currently selected project. Kept outside composables so bottom navigation and back handling agree. */
enum class ProjectSection {
    OVERVIEW,
    BRIEF_SCOPE,
    PLANNING,
    MORE,
    /** Site-manager surface: logs, HFC docs, quantity abstracts, material orders, Archi Calc. */
    SITEMAN,
    DRAWINGS,
    REPORTS,
    DECISIONS,
    COORDINATION,
    CONTROLS,
    CONTRACTORS
}

enum class DirectorySection(val label: String) {
    CLIENTS("Clients"),
    CONTRACTORS("Contractors")
}
