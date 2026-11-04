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
        val options = users.joinToString("") { user ->
            "<option value=\"${user.id}\">${PortalHttp.escape(user.displayName)} · ${PortalHttp.escape(user.role)}</option>"
        }
        val notice = error?.let { "<p class=\"notice\">${PortalHttp.escape(it)}</p>" }.orEmpty()
        return shell(
            "Sign in",
            """
            <h1>${PortalHttp.escape(practiceName.ifBlank { "ArchiMan" })}</h1>
            <p>Enter the access code shown on the phone, then choose a role. This portal is read only.</p>
            $notice
            <form method="post" action="/">
              <input type="hidden" name="csrf" value="${PortalHttp.escape(csrf)}">
              <label for="code">Access code</label>
              <input id="code" name="code" autocomplete="off" required>
              <label for="role">Role</label>
              <select id="role" name="user">${options}</select>
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
            <p>${PortalHttp.escape(role)} · read only. Rates and invoices stay on the phone.</p>
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
          <style>
            body{margin:0;background:#F5F5F1;color:#1A2424;font-family:Roboto,sans-serif;line-height:1.45}
            main{max-width:720px;margin:0 auto;padding:24px}
            a{color:#173B40} button,input,select{font:inherit;min-height:48px;box-sizing:border-box}
            input,select{display:block;width:100%;margin:4px 0 12px;padding:8px 12px;border:1px solid #DCE5E3;border-radius:12px;background:#fff;color:inherit}
            button{background:#173B40;color:#F5F5F1;border:0;border-radius:12px;padding:0 16px}
            .notice{color:#6B4710}
            @media (prefers-color-scheme: dark){
              body{background:#101918;color:#F5F5F1}
              input,select{background:#101918;color:#F5F5F1;border-color:#5E6E6B}
              button{background:#9FDBD6;color:#101918}
              a{color:#9FDBD6}
            }
          </style>
        </head>
        <body>
          <a href="#content">Skip to content</a>
          <main id="content">$body</main>
        </body>
        </html>
    """.trimIndent()
}
