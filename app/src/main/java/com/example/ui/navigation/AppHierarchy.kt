package com.example.ui.navigation

/** Stable destinations in the application hierarchy. */
enum class AppScreen(val title: String) {
    HOME("Portfolio"),
    PROJECT_WORKSPACE("Project Hub"),
    DEDICATED_MEASUREMENT("Record Measurement"),
    ROOM_WORKSPACE("Room Components"),
    MEASUREMENT_BOOK("Measurement Book"),
    REGISTER("Measurements Register"),
    PROJECTS("Projects"),
    CLIENTS("Clients"),
    CONTRACTORS("Contractors"),
    MASTER_DATA("PWD SR Work Catalogue"),
    EXPORT("Export & Quantities")
}

/** Top-level portfolio directories. Project-specific features live below a selected project. */
enum class HomeTab(val label: String) {
    PROJECTS("Projects"),
    CLIENTS("Clients"),
    CONTRACTORS("Contractors")
}
