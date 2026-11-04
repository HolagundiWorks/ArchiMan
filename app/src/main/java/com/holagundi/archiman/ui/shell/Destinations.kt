package com.holagundi.archiman.ui.shell

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Architecture
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material.icons.outlined.RequestQuote
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.ViewKanban
import androidx.compose.ui.graphics.vector.ImageVector
import com.holagundi.archiman.R

enum class PracticeDestination(
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
) {
    HOME(R.string.nav_home, Icons.Outlined.Home, Icons.Filled.Home),
    PROJECTS(R.string.nav_projects, Icons.Outlined.Apartment, Icons.Filled.Apartment),
    MY_WORK(R.string.nav_my_work, Icons.Outlined.Inbox, Icons.Filled.Inbox),
    SITE(R.string.nav_site, Icons.Outlined.Place, Icons.Filled.Place),
    PRACTICE(R.string.nav_practice, Icons.Outlined.Business, Icons.Filled.Business),
}

enum class ProjectGroup(@StringRes val labelRes: Int) {
    PROJECT(R.string.nav_group_project),
    SITE(R.string.nav_group_site),
    FEES(R.string.nav_group_fees),
}

enum class WorkspacePage(
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val group: ProjectGroup,
) {
    BRIEF(R.string.nav_brief, Icons.Outlined.Description, ProjectGroup.PROJECT),
    TASKS(R.string.nav_tasks, Icons.Outlined.ViewKanban, ProjectGroup.PROJECT),
    LETTERS(R.string.nav_letters, Icons.Outlined.Email, ProjectGroup.PROJECT),
    PROPOSAL(R.string.nav_proposal, Icons.Outlined.RequestQuote, ProjectGroup.PROJECT),
    CONTRACT(R.string.nav_contract, Icons.Outlined.Handshake, ProjectGroup.PROJECT),
    VISITS(R.string.nav_visits, Icons.Outlined.Place, ProjectGroup.SITE),
    SNAGS(R.string.nav_snags, Icons.Outlined.Report, ProjectGroup.SITE),
    COORDINATION(R.string.nav_coordination, Icons.Outlined.Forum, ProjectGroup.SITE),
    DRAWINGS(R.string.nav_drawings, Icons.Outlined.Architecture, ProjectGroup.SITE),
    SITEMAN(R.string.nav_siteman, Icons.Outlined.Engineering, ProjectGroup.SITE),
    LEVELS(R.string.nav_levels, Icons.Outlined.Layers, ProjectGroup.SITE),
    MEASURE(R.string.nav_measure, Icons.Outlined.Straighten, ProjectGroup.SITE),
    BOOK(R.string.nav_book, Icons.AutoMirrored.Outlined.MenuBook, ProjectGroup.SITE),
    INVOICE(R.string.nav_invoice, Icons.Outlined.Receipt, ProjectGroup.FEES),
    BILL(R.string.nav_bill, Icons.Outlined.Calculate, ProjectGroup.FEES),
}
