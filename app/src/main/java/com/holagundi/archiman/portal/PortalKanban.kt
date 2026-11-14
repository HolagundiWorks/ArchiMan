package com.holagundi.archiman.portal

import com.holagundi.archiman.data.local.ArchiManDatabase
import com.holagundi.archiman.data.local.entity.ProjectTaskEntity
import com.holagundi.archiman.domain.WorkPriority
import kotlinx.coroutines.flow.first

/** Project tasks Kanban for the Wi‑Fi portal (To do / Doing / Complete). */
class PortalKanban(private val db: ArchiManDatabase) {
    private fun e(value: Any?) = PortalHttp.escape(value?.toString().orEmpty())
    private fun hidden(name: String, value: String) =
        """<input type="hidden" name="${e(name)}" value="${e(value)}">"""

    private val columns = listOf(
        "TO_DO" to "To do",
        "DOING" to "Doing",
        "DONE" to "Complete",
    )

    private fun columnOf(status: String) = when (status.uppercase()) {
        "DONE", "COMPLETED" -> "DONE"
        "DOING", "IN_PROGRESS" -> "DOING"
        else -> "TO_DO"
    }

    private fun moveForm(
        csrf: String,
        projectId: Long,
        taskId: Long,
        status: String,
        label: String,
        icon: String,
        back: Boolean = false,
    ): String =
        """
        <form method="post" action="/workspace" class="portal-kanban-move">
          ${hidden("csrf", csrf)}
          ${hidden("action", "taskMove")}
          ${hidden("requestId", java.util.UUID.randomUUID().toString())}
          ${hidden("project", projectId.toString())}
          ${hidden("id", taskId.toString())}
          ${hidden("status", status)}
          <button class="cds--btn cds--btn--ghost portal-kanban-move__btn${if (back) " is-back" else ""}" type="submit" aria-label="${e(label)}" title="${e(label)}">${CarbonPortalIcons.svg(icon)}</button>
        </form>
        """.trimIndent()

    private fun card(task: ProjectTaskEntity, csrf: String, projectId: Long): String {
        val column = columnOf(task.status)
        val priority = WorkPriority.normalize(task.priority)
        val alert = when (priority) {
            WorkPriority.CRITICAL -> "critical"
            WorkPriority.ATTENTION -> "warning"
            else -> "ok"
        }
        val back = when (column) {
            "DOING" -> "TO_DO" to "Move to To do"
            "DONE" -> "DOING" to "Move to Doing"
            else -> null
        }
        val forward = when (column) {
            "TO_DO" -> "DOING" to ("Move to Doing" to "arrow")
            "DOING" -> "DONE" to ("Mark complete" to "task")
            else -> "TO_DO" to ("Reopen to To do" to "folder")
        }
        val backBtn = back?.let { (status, label) ->
            moveForm(csrf, projectId, task.id, status, label, "arrow", back = true)
        }.orEmpty()
        val forwardBtn = moveForm(csrf, projectId, task.id, forward.first, forward.second.first, forward.second.second)
        return """
        <article class="portal-kanban-card portal-kanban-card--$alert" draggable="true" data-task-id="${task.id}" data-project-id="$projectId">
          <span class="cds--tag cds--tag--$alert">${e(WorkPriority.label(priority))}</span>
          <p class="portal-kanban-card__title">${e(task.title)}</p>
          <div class="portal-kanban-card__actions">
            $backBtn
            $forwardBtn
          </div>
        </article>
        """.trimIndent()
    }

    suspend fun page(csrf: String, projectId: Long?): String {
        if (projectId == null) {
            val projects = db.projectDao().projects().filter { !it.status.equals("ARCHIVED", true) }
            val openByProject = projects.associate { project ->
                project.id to db.suiteDao().observeTasks(project.id).first()
                    .count { columnOf(it.status) != "DONE" }
            }
            val rows = projects.map { project ->
                listOf(
                    """<a href="/workspace?section=tasks&id=${project.id}">${e(project.name)}</a>""",
                    e(project.projectCode),
                    e(project.client),
                    openByProject[project.id]?.toString() ?: "0",
                    """<a href="/workspace?section=tasks&id=${project.id}">Open board</a>""",
                )
            }
            return CarbonPortal.masthead("Project tasks", "Open a project board to move work across To do, Doing and Complete.", "task") +
                PortalKpis.strip(
                    listOf(
                        PortalKpis.Kpi("Projects", projects.size.toString(), "building"),
                        PortalKpis.Kpi("Open tasks", openByProject.values.sum().toString(), "task"),
                    ),
                ) +
                PortalKpis.dataTable(
                    "Choose a project",
                    listOf("Project", "Code", "Client", "Open tasks", ""),
                    rows,
                    "No active projects.",
                )
        }
        val project = requireNotNull(db.projectDao().getProject(projectId)) { "Choose a project." }
        val tasks = db.suiteDao().observeTasks(project.id).first()
        val grouped = columns.associate { (status, _) ->
            status to tasks.filter { columnOf(it.status) == status }
        }
        val toDo = grouped["TO_DO"].orEmpty().size
        val doing = grouped["DOING"].orEmpty().size
        val done = grouped["DONE"].orEmpty().size
        val priorityOptions = WorkPriority.all.joinToString("") {
            """<option value="${e(it)}"${if (it == WorkPriority.NORMAL) " selected" else ""}>${e(WorkPriority.label(it))}</option>"""
        }
        val add = """
            <form method="post" action="/workspace" class="portal-kanban-add">
              ${hidden("csrf", csrf)}
              ${hidden("action", "taskAdd")}
              ${hidden("requestId", java.util.UUID.randomUUID().toString())}
              ${hidden("project", project.id.toString())}
              <div class="cds--form-item portal-form-span-2">
                <label class="cds--label" for="task-title">New task</label>
                <input class="cds--text-input" id="task-title" name="title" maxlength="200" required placeholder="What needs doing?">
              </div>
              <div class="cds--form-item">
                <label class="cds--label" for="task-priority">Priority</label>
                <div class="cds--select-input__wrapper">
                  <select class="cds--select-input" id="task-priority" name="priority">$priorityOptions</select>
                </div>
              </div>
              ${CarbonPortal.button("Add to To do", "primary", "task")}
            </form>
        """.trimIndent()
        val tabs = columns.mapIndexed { index, (status, label) ->
            val count = grouped[status].orEmpty().size
            val selected = index == 0
            """<button type="button" class="portal-kanban-tab" role="tab" aria-selected="${if (selected) "true" else "false"}" data-status="$status"><span class="portal-kanban-tab__label">${e(label)}</span><span class="portal-kanban-tab__count">$count</span></button>"""
        }.joinToString("")
        val panels = columns.mapIndexed { index, (status, label) ->
            val cards = grouped[status].orEmpty().joinToString("") { card(it, csrf, project.id) }
            val empty = if (grouped[status].isNullOrEmpty()) {
                """<p class="portal-kanban-empty">Drop tasks here</p>"""
            } else ""
            """
            <section class="portal-kanban-column${if (index == 0) " is-active" else ""}" role="tabpanel" data-status="$status" data-project-id="${project.id}" aria-label="${e(label)}"${if (index != 0) " hidden" else ""}>
              <header class="portal-kanban-column__head">
                <h2>${e(label)}</h2>
                <span class="portal-kanban-column__count">${grouped[status].orEmpty().size}</span>
              </header>
              <div class="portal-kanban-column__body">$cards$empty</div>
            </section>
            """.trimIndent()
        }.joinToString("")
        return CarbonPortal.masthead(
            "${project.name} · Tasks",
            "Use tabs for To do, Doing and Complete. Move with arrows or drag onto the open tab.",
            "task",
        ) +
            PortalKpis.strip(
                listOf(
                    PortalKpis.Kpi("To do", toDo.toString(), "task"),
                    PortalKpis.Kpi("Doing", doing.toString(), "construction"),
                    PortalKpis.Kpi("Complete", done.toString(), "chart"),
                    PortalKpis.Kpi("All tasks", tasks.size.toString(), "dashboard"),
                ),
            ) +
            """<p class="portal-actions"><a class="cds--btn cds--btn--tertiary" href="/project?id=${project.id}">${CarbonPortalIcons.svg("building")}<span>Project overview</span></a>
            <a class="cds--btn cds--btn--ghost" href="/workspace?section=tasks">${CarbonPortalIcons.svg("folder")}<span>All projects</span></a></p>""" +
            CarbonPortal.formSection("Add task", add) +
            """<div class="portal-kanban" data-csrf="${e(csrf)}"><div class="portal-kanban-tabs" role="tablist">$tabs</div>$panels</div>"""
    }

    suspend fun save(fields: Map<String, String>): String {
        val projectId = requireNotNull(fields["project"]?.toLongOrNull()) { "Choose a project." }
        val project = requireNotNull(db.projectDao().getProject(projectId)) { "Project not found." }
        require(project.status != "ARCHIVED") { "Archived projects are read-only." }
        val csrf = fields["csrf"].orEmpty()
        when (fields["action"]) {
            "taskAdd" -> {
                val title = fields["title"].orEmpty().trim()
                require(title.isNotBlank()) { "Enter a title." }
                require(title.length <= 200) { "Title is limited to 200 characters." }
                db.suiteDao().insertTask(
                    ProjectTaskEntity(
                        projectId = projectId,
                        title = title,
                        description = "",
                        status = "TO_DO",
                        priority = WorkPriority.normalize(fields["priority"].orEmpty()),
                        createdAt = System.currentTimeMillis(),
                    ),
                )
            }
            "taskMove" -> {
                val id = requireNotNull(fields["id"]?.toLongOrNull()) { "Task missing." }
                val status = fields["status"].orEmpty().uppercase()
                require(status in setOf("TO_DO", "DOING", "DONE")) { "Unknown task status." }
                val task = db.suiteDao().observeTasks(projectId).first().firstOrNull { it.id == id }
                requireNotNull(task) { "Task not found on this project." }
                db.suiteDao().setTaskStatus(id, status)
            }
            else -> error("Unknown task action.")
        }
        return page(csrf, projectId)
    }
}
