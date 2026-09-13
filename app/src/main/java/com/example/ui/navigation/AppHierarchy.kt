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
    REGISTER("Measurements Register"),
    PROJECTS("Projects"),
    CLIENTS("Clients"),
    CONTRACTORS("Contractors"),
    MASTER_DATA("PWD SR Work Catalogue")
}

/** Top-level portfolio directories. Project-specific features live below a selected project. */
enum class HomeTab(val label: String) {
    PROJECTS("Projects"),
    DIRECTORY("Directory"),
    WORK_LIBRARY("Work Library"),
    PRACTICE("Practice")
}

/** Pages within the currently selected project. Kept outside composables so bottom navigation and back handling agree. */
enum class ProjectSection {
    OVERVIEW,
    BRIEF_SCOPE,
    PLANNING,
    MORE,
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
