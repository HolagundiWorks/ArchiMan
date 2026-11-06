package com.holagundi.archiman.ui.shell

import com.holagundi.archiman.ui.theme.CarbonIcons
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.holagundi.archiman.R

enum class PracticeDestination(
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
) {
    HOME(R.string.nav_home, CarbonIcons.Home, CarbonIcons.Home),
    PROJECTS(R.string.nav_projects, CarbonIcons.Apartment, CarbonIcons.Apartment),
    PRACTICE(R.string.nav_practice, CarbonIcons.Business, CarbonIcons.Business),
}

enum class ProjectGroup(@StringRes val labelRes: Int, val icon: ImageVector) {
    OVERVIEW(R.string.nav_overview, CarbonIcons.Dashboard),
    WORK(R.string.nav_work, CarbonIcons.Assignment),
    SITE(R.string.nav_group_site, CarbonIcons.Engineering),
    FEES(R.string.nav_group_fees, CarbonIcons.Receipt),
}

enum class WorkspacePage(
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val group: ProjectGroup,
    val parent: WorkspacePage? = null,
) {
    OVERVIEW(R.string.nav_overview, CarbonIcons.Dashboard, ProjectGroup.OVERVIEW),
    BRIEF(R.string.nav_brief, CarbonIcons.Description, ProjectGroup.WORK),
    TASKS(R.string.nav_tasks, CarbonIcons.ViewKanban, ProjectGroup.WORK),
    DOCUMENTS(R.string.nav_documents, CarbonIcons.FolderOpen, ProjectGroup.WORK),
    LETTERS(R.string.nav_letters, CarbonIcons.Email, ProjectGroup.WORK, DOCUMENTS),
    DRAWINGS(R.string.nav_drawings, CarbonIcons.Architecture, ProjectGroup.WORK, DOCUMENTS),
    PROPOSAL(R.string.nav_proposal, CarbonIcons.RequestQuote, ProjectGroup.FEES),
    CONTRACT(R.string.nav_contract, CarbonIcons.Handshake, ProjectGroup.FEES),
    VISITS(R.string.nav_visits, CarbonIcons.Place, ProjectGroup.SITE),
    SNAGS(R.string.nav_snags, CarbonIcons.Report, ProjectGroup.SITE),
    COORDINATION(R.string.nav_coordination, CarbonIcons.Forum, ProjectGroup.SITE),
    SITEMAN(R.string.nav_siteman, CarbonIcons.Engineering, ProjectGroup.SITE),
    MEASUREMENTS(R.string.nav_measurements, CarbonIcons.Straighten, ProjectGroup.SITE),
    SETUP(R.string.nav_measurement_setup, CarbonIcons.Assignment, ProjectGroup.SITE, MEASUREMENTS),
    LEVELS(R.string.nav_levels, CarbonIcons.Layers, ProjectGroup.SITE, MEASUREMENTS),
    MEASURE(R.string.nav_measure, CarbonIcons.Straighten, ProjectGroup.SITE, MEASUREMENTS),
    BOOK(R.string.nav_book, CarbonIcons.MenuBook, ProjectGroup.SITE, MEASUREMENTS),
    INVOICE(R.string.nav_invoice, CarbonIcons.Receipt, ProjectGroup.FEES),
    BILL(R.string.nav_bill, CarbonIcons.Calculate, ProjectGroup.FEES),
}
