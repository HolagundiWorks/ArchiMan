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
        val notice = error?.let { "<p class=\"notice\">${PortalHttp.escape(it)}</p>" }.orEmpty()
        return shell(
            "Sign in",
            """
            <h1>${PortalHttp.escape(practiceName.ifBlank { "ArchiMan" })}</h1>
            <p>Enter the access code shown on the phone, to open the admin workspace. Use the workspace to manage company documents and measurements.</p>
            $notice
            <form method="post" action="/">
              <input type="hidden" name="csrf" value="${PortalHttp.escape(csrf)}">
              <label for="code">Access code</label>
              <input id="code" name="code" autocomplete="off" required>
              <input type="hidden" name="user" value="${users.firstOrNull { it.isActive && it.role == "Admin" }?.id ?: 0}">
              <button type="submit">Open portal</button>
            </form>
            """.trimIndent(),
        )
    }

    fun home(company: CompanyProfileEntity?, projects: List<ProjectEntity>, role: String, csrf: String): String {
        val rows = if (projects.isEmpty()) {
            "<p>No projects yet.</p>"
        } else {
            projects.joinToString("") { project ->
                "<li><a href=\"/project?id=${project.id}\">${PortalHttp.escape(project.projectCode)} · ${PortalHttp.escape(project.name)}</a><br>${PortalHttp.escape(project.client)} · ${PortalHttp.escape(project.status)}</li>"
            }.let { "<ul>$it</ul>" }
        }
        return shell(
            "Projects",
            """
            <h1>${PortalHttp.escape(company?.practiceName ?: "Practice")}</h1>
            <p>${PortalHttp.escape(role)} · Your practice stays on the phone.</p>
            <p><a href="/workspace">Open company, letterhead, item and measurement workspace</a></p>
            $rows
            ${signOut(csrf)}
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
        fun list(title: String, items: List<String>): String {
            val body = if (items.isEmpty()) "<p>None yet.</p>" else "<ul>${items.joinToString("") { "<li>$it</li>" }}</ul>"
            return "<h2>$title</h2>$body"
        }
        return shell(
            project.name,
            """
            <p><a href="/">Projects</a></p>
            <h1>${PortalHttp.escape(project.projectCode)} · ${PortalHttp.escape(project.name)}</h1>
            <p>${PortalHttp.escape(project.client)} · ${PortalHttp.escape(project.siteLocation)} · ${PortalHttp.escape(project.status)}</p>
            <p><a href="/workspace?section=measure&amp;id=${project.id}">Record / import measurements and download reports</a></p>
            <h2>Brief</h2>
            <p>${PortalHttp.escape(brief.ifBlank { "No brief yet." })}</p>
            ${list("Daily logs", logs.take(20).map { PortalHttp.escape("${it.preparedBy}: ${it.workCompleted}") })}
            ${list("Snags", issues.take(30).map { PortalHttp.escape("${it.referenceNumber} · ${it.title} · ${it.status}") })}
            ${list("Coordination", coordination.take(30).map { PortalHttp.escape("${it.type} ${it.referenceNumber} · ${it.subject} · ${it.status}") })}
            ${list("Drawings", drawings.take(40).map { PortalHttp.escape("${it.drawingNumber} · ${it.title} · ${it.packageTag}") })}
            ${signOut(csrf)}
            """.trimIndent(),
        )
    }

    fun message(title: String, body: String) = shell(title, "<h1>${PortalHttp.escape(title)}</h1><p>${PortalHttp.escape(body)}</p>")

    private fun signOut(csrf: String) = """
        <form method="post" action="/logout">
          <input type="hidden" name="csrf" value="${PortalHttp.escape(csrf)}">
          <button type="submit">Sign out</button>
        </form>
    """.trimIndent()

    private fun shell(title: String, body: String) = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1">
          <title>${PortalHttp.escape(title)} · ArchiMan</title>
          <link rel="stylesheet" href="/theme.css"><link rel="stylesheet" href="/carbon/vendor.css">
          <script src="/carbon/vendor.js" defer></script>
          <script src="/theme.js"></script>
          <style>main{max-width:720px;margin:0 auto;padding:24px}input,select{display:block;width:100%;min-height:48px;margin:4px 0 12px}form{margin:16px 0}article{background:var(--layer);padding:16px;border:1px solid var(--border-subtle)}</style>
        </head>
        <body>
          <a href="#content">Skip to content</a>
          <main id="content">$body</main>
        </body>
        </html>
    """.trimIndent()
}
