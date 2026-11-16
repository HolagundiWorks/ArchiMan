package com.holagundi.archiman.company

import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

/** Draft and operation identifiers exist only in memory while the live editor is open. */
data class CompanyTaskEdit(
    val projectId: String,
    val base: CompanyTask? = null,
    val id: String = base?.id ?: UUID.randomUUID().toString(),
    val operationId: String = UUID.randomUUID().toString(),
    val title: String = base?.title.orEmpty(),
    val description: String = base?.description.orEmpty(),
    val dueDate: String = base?.dueAt?.take(10).orEmpty(),
    val dueAtReference: String? = base?.dueAt,
    val status: String = base?.status ?: "open",
    val priority: String = base?.priority ?: "normal",
) {
    fun dueAt(): String? {
        if (dueDate.isBlank()) return null
        if (!dueDate.matches(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}"))) throw CompanyApiException("INVALID_TASK")
        val date = runCatching { LocalDate.parse(dueDate) }.getOrElse { throw CompanyApiException("INVALID_TASK") }
        if (date.year !in 1..9999) throw CompanyApiException("INVALID_TASK")
        // Retain an existing time when only other task fields change.
        return dueAtReference?.takeIf { it.take(10) == dueDate } ?: date.atStartOfDay().toInstant(ZoneOffset.UTC).toString()
    }
    fun fields(): JSONObject {
        fun validId(value: String) = runCatching { UUID.fromString(value).toString() == value }.getOrDefault(false)
        if (!validId(projectId) || !validId(id) || !validId(operationId) || title.trim().length !in 1..240 || description.length > 10000 ||
            status !in listOf("open","in_progress","completed","cancelled") || priority !in listOf("low","normal","high","urgent") ||
            (base != null && (base.projectId != projectId || base.id != id || base.version !in 1 until Int.MAX_VALUE)))
            throw CompanyApiException("INVALID_TASK")
        return JSONObject().put("title",title.trim()).put("description",description).put("dueAt",dueAt() ?: JSONObject.NULL)
            .put("status",status).put("priority",priority)
    }
    val valid get() = runCatching { fields() }.isSuccess
}
sealed interface CompanyTaskMutationResult {
    data class Applied(val task: CompanyTask) : CompanyTaskMutationResult
    data class Conflict(val current: CompanyTask) : CompanyTaskMutationResult
}

internal fun readCompanyTask(row: JSONObject, projectId: String): CompanyTask {
    val task = CompanyTask(row.getString("id"),row.getString("projectId"),row.getString("title"),row.getString("description"),
        if (row.isNull("dueAt")) null else row.getString("dueAt"),row.getString("status"),row.getString("priority"),row.getInt("version"),readTaskAssignees(row.optJSONArray("assignees")))
    if (!task.id.matches(Regex("[0-9a-fA-F-]{36}")) || task.projectId != projectId || task.title.isBlank() || task.title.length > 240 ||
        task.description.length > 10000 || task.version < 1 || task.status !in setOf("open","in_progress","completed","cancelled") ||
        task.priority !in setOf("low","normal","high","urgent") || (task.dueAt != null && runCatching { java.time.Instant.parse(task.dueAt) }.isFailure))
        throw CompanyApiException("INVALID_RESPONSE")
    return task
}
