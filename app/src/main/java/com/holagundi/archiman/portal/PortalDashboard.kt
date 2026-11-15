package com.holagundi.archiman.portal

import androidx.room.withTransaction
import com.holagundi.archiman.data.FeeDashboardSummary
import com.holagundi.archiman.data.local.ArchiManDatabase
import org.json.JSONArray
import org.json.JSONObject

class PortalDashboard(private val db: ArchiManDatabase) {
    private fun e(value: Any?) = PortalHttp.escape(value?.toString().orEmpty())

    private data class AttentionRow(
        val title: String,
        val detail: String,
        val href: String,
        val icon: String,
        val severity: String = "normal",
    )

    private data class TaskRow(val title: String, val detail: String, val href: String, val overdue: Boolean)

    suspend fun page(): String = db.withTransaction {
        val projects = db.projectDao().projects()
        fun count(sql: String) = db.openHelper.writableDatabase.query(sql).use {
            it.moveToFirst()
            it.getInt(0)
        }
        val active = projects.count { it.status.equals("ACTIVE", true) }
        val onHold = projects.count { it.status.equals("ON_HOLD", true) }
        val currentCount = projects.count { !it.status.equals("ARCHIVED", true) }
        val tasks = count("SELECT COUNT(*) FROM project_tasks t JOIN projects p ON p.id=t.projectId WHERE p.status!='ARCHIVED' AND t.status NOT IN ('DONE','COMPLETED')")
        val tasksDone = count("SELECT COUNT(*) FROM project_tasks t JOIN projects p ON p.id=t.projectId WHERE p.status!='ARCHIVED' AND t.status IN ('DONE','COMPLETED')")
        val tasksAll = tasks + tasksDone
        val reviews = count("SELECT COUNT(*) FROM measurement_sheets s JOIN projects p ON p.id=s.projectId WHERE p.status!='ARCHIVED' AND s.archivedAt IS NULL AND s.status IN ('SUBMITTED','CHECKED')")
        val drafts = count("SELECT COUNT(*) FROM measurement_sheets s JOIN projects p ON p.id=s.projectId WHERE p.status!='ARCHIVED' AND s.archivedAt IS NULL AND s.status IN ('DRAFT','RETURNED')")
        val recorded = count("SELECT COUNT(*) FROM measurements m JOIN projects p ON p.id=m.projectId WHERE p.status!='ARCHIVED'")
        val issues = count("SELECT COUNT(*) FROM site_issues i JOIN projects p ON p.id=i.projectId WHERE p.status!='ARCHIVED' AND i.status!='CLOSED'")
        val catalogue = count("SELECT COUNT(*) FROM item_master WHERE isActive = 1")
        val fees = FeeDashboardSummary.calculate(db.suiteDao().allInvoices(), db.suiteDao().allReceipts())
        fun status(value: String) = when (value.uppercase()) {
            "ACTIVE" -> "Active"
            "ON_HOLD" -> "On hold"
            "COMPLETED" -> "Completed"
            "ARCHIVED" -> "Archived"
            else -> value.replaceFirstChar(Char::uppercase)
        }
        val balances = listOf(
            "Cash received" to fees.received.toPlainString(),
            "TDS withheld" to fees.tdsWithheld.toPlainString(),
            "Outstanding" to fees.outstanding.toPlainString(),
        )
        fun chart(title: String, description: String, type: String, rows: List<Pair<String, String>>): String {
            val data = JSONArray().also { array ->
                rows.forEach { (label, value) ->
                    array.put(JSONObject().put("group", label).put("value", value))
                }
            }
            return "<section class='carbon-insight'><h2>${e(title)}</h2><p>${e(description)}</p><div class='carbon-chart' data-carbon-chart='$type' data-rows='${e(data)}' aria-label='${e(title)}'></div><ul class='carbon-data-list' aria-label='${e(title)} exact values'>" +
                rows.mapIndexed { i, (label, value) -> "<li><span><span class='swatch series-$i' aria-hidden='true'></span>${e(label)}</span><strong>${e(value)}</strong></li>" }.joinToString("") +
                "</ul></section>"
        }
        val projectRows = projects.filter { !it.status.equals("ARCHIVED", true) }.map { p ->
            listOf(
                "<a href='/project?id=${p.id}'>${e(p.name)}</a>",
                e(p.projectCode),
                e(p.client),
                """<span class="status">${e(status(p.status))}</span>""",
                "<a href='/workspace?section=measure&id=${p.id}'>Measurement book</a>",
            )
        }
        val attention = attentionRows()
        val attentionCount = attention.size
        val openTasks = taskRows()
        val brand = PortalBrand.load(db)

        val summaryTone = when {
            issues > 0 -> "portal-tile--critical"
            reviews > 0 || onHold > 0 || fees.unpaidInvoices > 0 -> "portal-tile--warning"
            else -> "portal-tile--ok"
        }
        val findingsTone = when {
            attentionCount == 0 -> "portal-tile--ok"
            issues > 0 -> "portal-tile--critical"
            else -> "portal-tile--warning"
        }
        val tasksTone = if (tasks == 0) "portal-tile--ok" else "portal-tile--warning"

        val overviewPanel = """
            <div class="carbon-tile-grid carbon-tile-grid--small" role="group" aria-label="At a glance">
              ${compactKpi(currentCount.toString(), "Projects", if (currentCount == 0) "warning" else "ok")}
              ${compactKpi(active.toString(), "Active", "ok")}
              ${compactKpi(onHold.toString(), "On hold", if (onHold > 0) "warning" else "ok")}
              ${compactKpi(tasks.toString(), "Open tasks", if (tasks > 0) "warning" else "ok")}
              ${compactKpi(issues.toString(), "Issues", if (issues > 0) "critical" else "ok")}
              ${compactKpi(reviews.toString(), "Reviews", if (reviews > 0) "warning" else "ok")}
              ${compactKpi(drafts.toString(), "Drafts", if (drafts > 0) "warning" else "ok")}
              ${compactKpi(recorded.toString(), "Measurements", "ok")}
              ${compactKpi(catalogue.toString(), "Catalogue", if (catalogue == 0) "warning" else "ok")}
              ${compactKpi(fees.unpaidInvoices.toString(), "Unpaid", if (fees.unpaidInvoices > 0) "warning" else "ok")}
            </div>
            <div class="carbon-tile-grid carbon-tile-grid--medium">
              <details class="carbon-tile--expandable carbon-tile--medium $summaryTone" open>
                <summary><span>Practice summary</span><span class="portal-product-panel__meta">Fees · filings</span></summary>
                <div class="carbon-tile-expand__body">
                  ${PortalKpis.filingStrip()}
                  <div class="carbon-insights portal-product-chart">${chart("Fee balances", "INR · cash, TDS and outstanding client fees.", "bar", balances)}</div>
                </div>
              </details>
              <details class="carbon-tile--expandable carbon-tile--medium $findingsTone" open>
                <summary><span>Needs attention</span><span class="portal-product-panel__meta">${if (attentionCount == 0) "Up to date" else "$attentionCount open"}</span></summary>
                <div class="carbon-tile-expand__body">${findingsList(attention)}</div>
              </details>
              <details class="carbon-tile--expandable carbon-tile--medium $tasksTone" open>
                <summary><span>Tasks</span><span class="portal-product-panel__meta">$tasksDone/$tasksAll complete · $tasks open</span></summary>
                <div class="carbon-tile-expand__body">
                  ${taskList(openTasks)}
                  <p class="portal-product-footer"><a href="/workspace?section=tasks">View all ($tasks)</a></p>
                </div>
              </details>
            </div>
        """.trimIndent()

        val attentionPanel = """
            <section class="portal-panel portal-product-panel">
              <header class="portal-product-panel__head">
                <h2>Needs attention</h2>
                <span class="portal-product-panel__meta">${if (attentionCount == 0) "Up to date" else "$attentionCount open"}</span>
              </header>
              ${findingsList(attention)}
            </section>
        """.trimIndent()

        val projectsPanel = PortalKpis.dataTable(
            "Active project directory",
            listOf("Project", "Code", "Client", "Status", "Measurements"),
            projectRows,
            "No active projects yet.",
        )

        """
        <div class="portal-product" data-portal-product>
          <header class="portal-product-header">
            <div class="portal-product-header__copy">
              <p class="portal-product-crumb"><a href="/">${e(brand.displayName)}</a><span aria-hidden="true"> / </span><span>Overview</span></p>
              <h1>${e(brand.displayName)} practice</h1>
            </div>
            <details class="portal-product-actions">
              <summary class="cds--btn cds--btn--primary">Actions ${CarbonPortalIcons.svg("chevron")}</summary>
              <div class="portal-product-actions__menu" role="menu">
                <a role="menuitem" href="/">${CarbonPortalIcons.svg("building")}<span>Open projects</span></a>
                <a role="menuitem" href="/workspace?section=tasks">${CarbonPortalIcons.svg("task")}<span>Task boards</span></a>
                <a role="menuitem" href="/workspace?section=items">${CarbonPortalIcons.svg("ruler")}<span>Item library</span></a>
              </div>
            </details>
          </header>
          <div class="portal-product-tabs" role="tablist" aria-label="Dashboard">
            <button type="button" class="portal-product-tab" role="tab" aria-selected="true" data-panel="overview">Overview</button>
            <button type="button" class="portal-product-tab" role="tab" aria-selected="false" data-panel="attention">Attention${if (attentionCount > 0) """ <span class="portal-product-tab__count">$attentionCount</span>""" else ""}</button>
            <button type="button" class="portal-product-tab" role="tab" aria-selected="false" data-panel="projects">Projects</button>
          </div>
          <section class="portal-product-panel-host is-active" data-panel="overview" role="tabpanel">$overviewPanel</section>
          <section class="portal-product-panel-host" data-panel="attention" role="tabpanel" hidden>$attentionPanel</section>
          <section class="portal-product-panel-host" data-panel="projects" role="tabpanel" hidden>$projectsPanel</section>
        </div>
        """.trimIndent()
    }

    private fun compactKpi(value: String, label: String, alert: String = "ok") =
        """<div class="carbon-kpi carbon-kpi--$alert carbon-tile--small"><strong>${e(value)}</strong><span>${e(label)}</span></div>"""

    private fun findingsList(rows: List<AttentionRow>): String {
        if (rows.isEmpty()) {
            return """<p class="portal-overview-empty">You're up to date. No open tasks, reviews or site issues.</p>"""
        }
        return """<ul class="portal-findings">${rows.joinToString("") { row ->
            val sev = when (row.severity) {
                "critical" -> "portal-findings__item--critical"
                "attention" -> "portal-findings__item--attention"
                else -> ""
            }
            """<li class="portal-findings__item $sev"><a href="${e(row.href)}">${CarbonPortalIcons.svg(row.icon)}<span class="portal-findings__copy"><strong>${e(row.title)}</strong><span class="portal-findings__detail">${e(row.detail)}</span></span></a></li>"""
        }}</ul>"""
    }

    private fun taskList(rows: List<TaskRow>): String {
        if (rows.isEmpty()) {
            return """<p class="portal-overview-empty">No open tasks.</p>"""
        }
        return """<ul class="portal-findings portal-findings--tasks">${rows.joinToString("") { row ->
            val cls = if (row.overdue) " portal-findings__item--critical" else ""
            val status = if (row.overdue) """<span class="portal-findings__status">Overdue</span>""" else ""
            """<li class="portal-findings__item$cls"><a href="${e(row.href)}">${CarbonPortalIcons.svg("task")}<span class="portal-findings__copy"><strong>${e(row.title)}</strong><span class="portal-findings__detail">${e(row.detail)}$status</span></span></a></li>"""
        }}</ul>"""
    }

    private fun attentionRows(): List<AttentionRow> {
        val rows = mutableListOf<AttentionRow>()
        db.openHelper.writableDatabase.query(
            """
            SELECT s.id, s.projectId, s.sheetCode, p.name AS projectName, s.status
            FROM measurement_sheets s JOIN projects p ON p.id = s.projectId
            WHERE p.status != 'ARCHIVED' AND s.archivedAt IS NULL AND s.status IN ('SUBMITTED', 'CHECKED')
            ORDER BY s.updatedAt LIMIT 6
            """.trimIndent(),
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val status = cursor.getString(4)
                rows += AttentionRow(
                    title = cursor.getString(2),
                    detail = "${cursor.getString(3)} · ${if (status == "SUBMITTED") "Ready to check" else "Awaiting approval"}",
                    href = "/workspace?section=book&id=${cursor.getLong(1)}&sheet=${cursor.getLong(0)}",
                    icon = "notebook",
                    severity = "attention",
                )
            }
        }
        db.openHelper.writableDatabase.query(
            """
            SELECT t.id, t.projectId, t.title, p.name AS projectName
            FROM project_tasks t JOIN projects p ON p.id = t.projectId
            WHERE p.status != 'ARCHIVED' AND t.status NOT IN ('DONE', 'COMPLETED')
            ORDER BY t.dueDate IS NULL, t.dueDate, t.createdAt DESC LIMIT 6
            """.trimIndent(),
        ).use { cursor ->
            while (cursor.moveToNext()) {
                rows += AttentionRow(
                    title = cursor.getString(2),
                    detail = "${cursor.getString(3)} · Open task",
                    href = "/workspace?section=tasks&id=${cursor.getLong(1)}",
                    icon = "task",
                )
            }
        }
        db.openHelper.writableDatabase.query(
            """
            SELECT i.id, i.projectId, i.title, p.name AS projectName, i.severity
            FROM site_issues i JOIN projects p ON p.id = i.projectId
            WHERE p.status != 'ARCHIVED' AND i.status != 'CLOSED'
            ORDER BY CASE i.severity WHEN 'CRITICAL' THEN 0 WHEN 'ATTENTION' THEN 1 ELSE 2 END, i.updatedAt DESC LIMIT 6
            """.trimIndent(),
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val severity = cursor.getString(4)
                rows += AttentionRow(
                    title = cursor.getString(2),
                    detail = "${cursor.getString(3)} · ${severity.lowercase().replaceFirstChar(Char::uppercase)}",
                    href = "/workspace?section=snags&id=${cursor.getLong(1)}",
                    icon = "construction",
                    severity = when (severity.uppercase()) {
                        "CRITICAL" -> "critical"
                        "ATTENTION" -> "attention"
                        else -> "normal"
                    },
                )
            }
        }
        return rows
    }

    private fun taskRows(): List<TaskRow> {
        val rows = mutableListOf<TaskRow>()
        val now = System.currentTimeMillis()
        db.openHelper.writableDatabase.query(
            """
            SELECT t.id, t.projectId, t.title, p.name AS projectName, t.dueDate
            FROM project_tasks t JOIN projects p ON p.id = t.projectId
            WHERE p.status != 'ARCHIVED' AND t.status NOT IN ('DONE', 'COMPLETED')
            ORDER BY t.dueDate IS NULL, t.dueDate, t.createdAt DESC LIMIT 7
            """.trimIndent(),
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val due = if (cursor.isNull(4)) null else cursor.getLong(4)
                rows += TaskRow(
                    title = cursor.getString(2),
                    detail = cursor.getString(3),
                    href = "/workspace?section=tasks&id=${cursor.getLong(1)}",
                    overdue = due != null && due < now,
                )
            }
        }
        return rows
    }
}
