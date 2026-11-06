package com.holagundi.archiman.portal

import com.holagundi.archiman.data.local.entity.CompanyProfileEntity
import com.holagundi.archiman.data.local.entity.CoordinationItemEntity
import com.holagundi.archiman.data.local.entity.DailySiteReportEntity
import com.holagundi.archiman.data.local.entity.LocalUserEntity
import com.holagundi.archiman.data.local.entity.ProjectDrawingEntity
import com.holagundi.archiman.data.local.entity.ProjectEntity
import com.holagundi.archiman.data.local.entity.SiteIssueEntity

object PortalPages {
    fun login(practiceName: String, users: List<LocalUserEntity>, csrf: String, error: String?): String {
        val admin = users.firstOrNull { it.isActive && it.role == "Admin" }
        val notice = error?.let { """<p class="notice" role="alert">${PortalHttp.escape(it)}</p>""" }.orEmpty()
        val title: String
        val lead: String
        val hiddenUser: String
        val action: String
        val icon: String
        if (admin == null) {
            title = "Practice transfer"
            lead = "Enter the access code shown on the phone to save or restore a practice backup before opening a company."
            hiddenUser = ""
            action = "Open practice transfer"
            icon = "backup"
        } else {
            title = practiceName.ifBlank { "ArchiMan" }
            lead = "Enter the access code shown on the phone. The practice stays on the phone; this computer only connects while the portal is on."
            hiddenUser = """<input type="hidden" name="user" value="${admin.id}">"""
            action = "Open portal"
            icon = "wifi"
        }
        return CarbonPortal.page(
            title = "Sign in",
            audience = CarbonPortal.Audience.SIGNED_OUT,
            body = """
                <section class="cds--tile portal-sign-in">
                  <div class="portal-sign-in-brand">${CarbonPortalIcons.svg(icon, "portal-sign-in-glyph")}</div>
                  <p class="portal-kicker">ArchiMan Wi‑Fi portal</p>
                  <h1>${PortalHttp.escape(title)}</h1>
                  <p class="portal-lead">$lead</p>
                  $notice
                  <form method="post" action="/">
                    <input type="hidden" name="csrf" value="${PortalHttp.escape(csrf)}">
                    $hiddenUser
                    ${CarbonPortal.textField("code", "code", "Access code", attributes = "autocomplete=\"off\" required autofocus")}
                    ${CarbonPortal.button(action, icon = if (admin == null) "backup" else "wifi")}
                  </form>
                </section>
            """.trimIndent(),
        )
    }

    fun transfer(practice: String, csrf: String, error: String?, role: String = "Admin") =
        CarbonPortal.page(
            title = "Practice transfer",
            audience = if (role == PortalTransfer.ROLE) CarbonPortal.Audience.TRANSFER else CarbonPortal.Audience.ADMIN,
            current = "transfer",
            crumb = "Practice transfer",
            csrf = csrf,
            body = PortalTransfer.page(practice, csrf, error),
        )

    fun home(company: CompanyProfileEntity?, projects: List<ProjectEntity>, role: String, csrf: String): String {
        val tiles = if (projects.isEmpty()) {
            """<p class="portal-lead">No projects yet.</p>"""
        } else {
            projects.joinToString("") { project ->
                """
                <a class="cds--tile cds--tile--clickable portal-tile" href="/project?id=${project.id}">
                  <span class="portal-tile-icon">${CarbonPortalIcons.svg("building", "portal-tile-glyph")}</span>
                  <span class="portal-tile-copy">
                    <h2>${PortalHttp.escape(project.name)}</h2>
                    <p class="portal-tile-meta">${PortalHttp.escape(project.projectCode)} · ${PortalHttp.escape(project.client)}</p>
                    <span class="${statusClass(project.status)}">${PortalHttp.escape(project.status)}</span>
                  </span>
                  ${CarbonPortalIcons.svg("arrow", "portal-tile-arrow")}
                </a>
                """.trimIndent()
            }.let { """<div class="portal-tiles">$it</div>""" }
        }
        return CarbonPortal.page(
            title = "Projects",
            audience = CarbonPortal.Audience.ADMIN,
            current = "projects",
            crumb = "Projects",
            csrf = csrf,
            body = """
                ${CarbonPortal.masthead(company?.practiceName ?: "Practice", "$role · Your practice stays on the phone.", "building")}
                <div class="portal-tiles">
                  ${CarbonPortal.tile("/workspace?section=dashboard", "Overview", "Projects, fees and work that needs attention.", "dashboard")}
                  ${CarbonPortal.tile("/transfer", "Practice transfer", "Download or restore the practice on this computer.", "backup")}
                </div>
                <h2 class="portal-section-title">${CarbonPortalIcons.svg("folder")}<span>Projects</span></h2>
                $tiles
            """.trimIndent(),
        )
    }

    fun project(
        project: ProjectEntity,
        brief: String,
        logs: List<DailySiteReportEntity>,
        issues: List<SiteIssueEntity>,
        coordination: List<CoordinationItemEntity>,
        drawings: List<ProjectDrawingEntity>,
        csrf: String,
    ): String {
        fun tile(title: String, items: List<String>, icon: String): String {
            val body = if (items.isEmpty()) {
                """<p class="portal-tile-meta">None yet.</p>"""
            } else {
                "<ul>${items.joinToString("") { "<li>$it</li>" }}</ul>"
            }
            return """<section class="cds--tile portal-panel"><h2>${CarbonPortalIcons.svg(icon)}<span>$title</span></h2>$body</section>"""
        }
        return CarbonPortal.page(
            title = project.name,
            audience = CarbonPortal.Audience.ADMIN,
            current = "projects",
            crumb = project.name,
            csrf = csrf,
            body = """
                ${CarbonPortal.masthead("${project.projectCode} · ${project.name}", "${project.client} · ${project.siteLocation} · ${project.status}", "building")}
                <div class="portal-tiles">
                  ${CarbonPortal.tile("/workspace?section=measure&id=${project.id}", "Measurement book", "Record, import and download measurements.", "notebook")}
                  ${CarbonPortal.tile("/workspace?section=schedule&id=${project.id}", "Assigned works", "Item revision, contractors and rates.", "task")}
                </div>
                <div class="portal-stack">
                  <section class="cds--tile portal-panel"><h2>${CarbonPortalIcons.svg("document")}<span>Brief</span></h2><p>${PortalHttp.escape(brief.ifBlank { "No brief yet." })}</p></section>
                  ${tile("Daily logs", logs.take(20).map { PortalHttp.escape("${it.preparedBy}: ${it.workCompleted}") }, "document")}
                  ${tile("Snags", issues.take(30).map { PortalHttp.escape("${it.referenceNumber} · ${it.title} · ${it.status}") }, "task")}
                  ${tile("Coordination", coordination.take(30).map { PortalHttp.escape("${it.type} ${it.referenceNumber} · ${it.subject} · ${it.status}") }, "partnership")}
                  ${tile("Drawings", drawings.take(40).map { PortalHttp.escape("${it.drawingNumber} · ${it.title} · ${it.packageTag}") }, "folder")}
                </div>
            """.trimIndent(),
        )
    }

    fun message(title: String, body: String) = CarbonPortal.page(
        title = title,
        audience = CarbonPortal.Audience.SIGNED_OUT,
        body = """<section class="cds--tile portal-sign-in"><div class="portal-sign-in-brand">${CarbonPortalIcons.svg("wifi", "portal-sign-in-glyph")}</div><h1>${PortalHttp.escape(title)}</h1><p class="portal-lead">${PortalHttp.escape(body)}</p></section>""",
    )

    private fun statusClass(status: String) = when (status.uppercase()) {
        "APPROVED", "ISSUED", "ACTIVE", "COMPLETED" -> "status status-approved"
        "RETURNED", "VOID", "ARCHIVED" -> "status status-returned"
        "CHECKED", "ON_HOLD" -> "status status-checked"
        "SUBMITTED" -> "status status-submitted"
        else -> "status"
    }
}
