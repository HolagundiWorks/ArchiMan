package com.holagundi.archiman.portal

import com.holagundi.archiman.data.local.entity.CompanyProfileEntity
import com.holagundi.archiman.data.local.entity.CoordinationItemEntity
import com.holagundi.archiman.data.local.entity.DailySiteReportEntity
import com.holagundi.archiman.data.local.entity.LocalUserEntity
import com.holagundi.archiman.data.local.entity.ProjectDrawingEntity
import com.holagundi.archiman.data.local.entity.ProjectEntity
import com.holagundi.archiman.data.local.entity.SiteIssueEntity

object PortalPages {
    fun login(
        practiceName: String,
        users: List<LocalUserEntity>,
        csrf: String,
        error: String?,
        brand: PortalBrand = PortalBrand(practiceName = practiceName),
    ): String {
        val admin = users.firstOrNull { it.isActive && it.role == "Admin" }
        if (admin == null) return message("Company connection required", "Open Mobile and connect to the company API. Local backup and restore are not available.")
        val notice = error?.let { """<p class="notice" role="alert">${PortalHttp.escape(it)}</p>""" }.orEmpty()
        val title = brand.displayName.ifBlank { practiceName.ifBlank { "MynaHub" } }
        val lead = "Enter the access code shown on the phone. This computer connects only while the portal is on."
        val hiddenUser = """<input type="hidden" name="user" value="${admin.id}">"""
        val action = "Open portal"
        val icon = "wifi"
        val mark = if (brand.hasLogo) {
            """<img class="portal-sign-in-logo" src="${PortalHttp.escape(brand.logoDataUrl)}" alt="${PortalHttp.escape(title)}">"""
        } else {
            """<div class="portal-sign-in-brand">${CarbonPortalIcons.svg(icon, "portal-sign-in-glyph")}</div>"""
        }
        return CarbonPortal.page(
            title = "Sign in",
            audience = CarbonPortal.Audience.SIGNED_OUT,
            brand = brand,
            body = """
                <section class="cds--tile portal-sign-in">
                  $mark
                  <p class="portal-kicker">ArchiMan Wi‑Fi portal</p>
                  <h1>${PortalHttp.escape(title)}</h1>
                  <p class="portal-lead">$lead</p>
                  $notice
                  <form method="post" action="/">
                    <input type="hidden" name="csrf" value="${PortalHttp.escape(csrf)}">
                    $hiddenUser
                    ${CarbonPortal.textField("code", "code", "Access code", attributes = "autocomplete=\"off\" required autofocus")}
                    ${CarbonPortal.button(action, icon = icon)}
                  </form>
                </section>
            """.trimIndent(),
        )
    }

    fun home(
        company: CompanyProfileEntity?,
        projects: List<ProjectEntity>,
        role: String,
        csrf: String,
        brand: PortalBrand = PortalBrand.from(company),
    ): String {
        fun label(status: String) = when (status.uppercase()) {
            "ACTIVE" -> "Active"
            "ON_HOLD" -> "On hold"
            "COMPLETED" -> "Completed"
            "ARCHIVED" -> "Archived"
            else -> status.replaceFirstChar(Char::uppercase)
        }
        val active = projects.count { it.status.equals("ACTIVE", true) }
        val onHold = projects.count { it.status.equals("ON_HOLD", true) }
        val completed = projects.count { it.status.equals("COMPLETED", true) }
        val archived = projects.count { it.status.equals("ARCHIVED", true) }
        val rows = projects.map { project ->
            listOf(
                """<a href="/project?id=${project.id}">${PortalHttp.escape(project.name)}</a>""",
                PortalHttp.escape(project.projectCode),
                PortalHttp.escape(project.client),
                """<span class="${statusClass(project.status)}">${PortalHttp.escape(label(project.status))}</span>""",
                """<a href="/workspace?section=measure&id=${project.id}">Measurement book</a>""",
                """<a href="/workspace?section=schedule&id=${project.id}">Assigned works</a>""",
            )
        }
        return CarbonPortal.page(
            title = "Projects",
            audience = CarbonPortal.Audience.ADMIN,
            current = "projects",
            crumb = "Projects",
            csrf = csrf,
            brand = brand,
            body = """
                ${CarbonPortal.masthead(brand.displayName, "$role · Project directory for this phone.", "building")}
                ${PortalKpis.strip(listOf(
                    PortalKpis.Kpi("Active projects", active.toString(), "building", alert = "ok"),
                    PortalKpis.Kpi("On hold", onHold.toString(), "task", alert = if (onHold > 0) "warning" else "ok"),
                    PortalKpis.Kpi("Completed", completed.toString(), "chart", alert = "ok"),
                    PortalKpis.Kpi("Archived", archived.toString(), "folder", alert = "none"),
                    PortalKpis.Kpi("All projects", projects.size.toString(), "dashboard", alert = if (projects.isEmpty()) "warning" else "ok"),
                ))}
                <p class="portal-actions">
                  <a class="cds--btn cds--btn--primary" href="/workspace?section=dashboard">${CarbonPortalIcons.svg("dashboard")}<span>Practice dashboard</span></a>
                </p>
                ${PortalKpis.dataTable("All projects", listOf("Project", "Code", "Client", "Status", "Measurements", "Schedule"), rows, "No projects yet.")}
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
        brand: PortalBrand = PortalBrand.FALLBACK,
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
            brand = brand,
            body = """
                ${CarbonPortal.masthead("${project.projectCode} · ${project.name}", "${project.client} · ${project.siteLocation} · ${project.status}", "building")}
                <div class="portal-tiles">
                  ${CarbonPortal.tile("/workspace?section=tasks&id=${project.id}", "Tasks", "Kanban board — To do, Doing and Complete.", "task")}
                  ${CarbonPortal.tile("/workspace?section=estimate&id=${project.id}", "Estimate", "Planned quantities at tax-exclusive rates.", "ruler")}
                  ${CarbonPortal.tile("/workspace?section=bills&id=${project.id}", "Running bills", "Contractor bills from approved measurements.", "document")}
                  ${CarbonPortal.tile("/workspace?section=measure&id=${project.id}", "Measurement book", "Record, import and download measurements.", "notebook")}
                  ${CarbonPortal.tile("/workspace?section=schedule&id=${project.id}", "Assigned works", "Item revision, contractors and rates.", "ruler")}
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
