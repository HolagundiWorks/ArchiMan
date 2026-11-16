package com.holagundi.archiman

import android.app.Activity
import android.app.Instrumentation
import android.os.Bundle
import com.holagundi.archiman.company.*
import com.holagundi.archiman.sync.KeystoreBox
import java.io.File
import org.json.JSONObject

/** Opt-in checks use fictional users; private input is removed before any network call. */
object CompanyApiDeviceChecks {
    fun run(instrumentation: Instrumentation) {
        val result = Bundle()
        val input = File(instrumentation.targetContext.filesDir, "company-api-device-test.json")
        var stage = "input"
        fun signal(value: String) { instrumentation.sendStatus(1, Bundle().apply { putString("companyApiStage", value) }) }
        try {
            check(BuildConfig.DEBUG)
            val fixture = JSONObject(input.readText())
            check(input.delete())
            check(!fixture.has("projectCache") && !fixture.has("pendingEdit"))
            stage = "backup-routes-removed"
            verifyRetiredBackupRoutes(instrumentation)
            if(fixture.optBoolean("rendererOnly")) {
                stage="isolated-memory-renderer"
                CompanyFileDeviceChecks.verifyRenderer(instrumentation)
                result.putString("companyApi","passed: isolated image/PDF rendering through RAM descriptors, bounded raw-pixel output and no persistent file copies")
                instrumentation.finish(Activity.RESULT_OK,result)
                return
            }
            val account = CompanyAccountService(instrumentation.targetContext).account
            account.signOut()
            stage = "connection"
            account.connect(fixture.getString("apiUrl"))
            stage = "sign-in"
            account.signIn(fixture.getString("email"), fixture.getString("password"))
            if(account.pending!=null) {
                stage="sign-in-mfa"
                check(fixture.has("totpUri"))
                account.verify(totp(fixture.getString("totpUri")),false)
            }
            check(account.pending == null)
            stage = "credential-only-storage"
            verifyCredentialOnly(instrumentation)
            val restored = CompanyAccountService(instrumentation.targetContext).account
            check(restored.session() == null && restored.hasSavedSession())
            restored.refresh()
            check(restored.session() != null)
            if(fixture.optBoolean("files")) {
                stage="private-file-workflow"
                CompanyFileDeviceChecks.workflow(instrumentation,restored)
                verifyCredentialOnly(instrumentation)
                result.putString("companyApi","passed: private TXT/PNG/PDF upload/retry, verified in-memory previews, live metadata, confirmed deletion/retry and no persistent document copies")
            } else if(fixture.optBoolean("siteRecords")) {
                stage="site-record-create"
                val project=restored.liveProjects(null,null).page.projects.single {it.code=="DEMO-01"}
                val draft=CompanySiteChange(project.id,value=CompanySiteRecord(java.util.UUID.randomUUID().toString(),project.id,"snag",java.time.LocalDate.now().toString(),"Demo site workflow verification","Fictional observation","North entry","Demo contractor",null,"open",null,1))
                var active:CompanySiteRecord?=null
                try {
                    active=(restored.changeSiteRecord(draft) as CompanySiteResult.Saved).record
                    check((restored.changeSiteRecord(draft) as CompanySiteResult.Saved).record==active)
                    val before=restored.liveProjects(null,null,siteProjectId=project.id)
                    check(before.sitePage!!.records.any {it.id==draft.value.id})
                    stage="site-record-edit-conflict"
                    val edit=CompanySiteChange(project.id,active,value=active.copy(status="resolved",resolvedDate=active.recordDate,title="Demo site workflow resolved"))
                    active=(restored.changeSiteRecord(edit) as CompanySiteResult.Saved).record
                    check((restored.changeSiteRecord(edit) as CompanySiteResult.Saved).record==active)
                    val conflict=restored.changeSiteRecord(edit.copy(operationId=java.util.UUID.randomUUID().toString()))
                    check(conflict is CompanySiteResult.Conflict && conflict.current.version==2)
                    val after=restored.liveProjects(null,before.revision,siteProjectId=project.id)
                    check(after.revision!=before.revision && after.sitePage!!.records.single {it.id==draft.value.id}.status=="resolved")
                    stage="site-record-delete"
                    val remove=CompanySiteChange(project.id,active,deleting=true,confirmation="DELETE")
                    check(restored.changeSiteRecord(remove) is CompanySiteResult.Deleted)
                    check(restored.changeSiteRecord(remove) is CompanySiteResult.Deleted)
                    check(restored.liveProjects(null,null,siteProjectId=project.id).sitePage!!.records.none {it.id==draft.value.id})
                    active=null;verifyCredentialOnly(instrumentation)
                    result.putString("companyApi","passed: live site create/edit/retry/conflict, resolution dates, confirmed deletion/retry and credential-only persistence")
                } finally {
                    if(active!=null)runCatching {
                        val current=restored.liveProjects(null,null,siteProjectId=project.id).sitePage!!.records.firstOrNull {it.id==draft.value.id}
                        if(current!=null)restored.changeSiteRecord(CompanySiteChange(project.id,current,deleting=true,confirmation="DELETE"))
                    }
                }
            } else if(fixture.optBoolean("taskActions")) {
                stage="task-action-create"
                val project=restored.liveProjects(null,null).page.projects.single {it.code=="DEMO-01"}
                val draft=CompanyTaskEdit(project.id,title="Demo task action verification",description="Fictional device workflow")
                var active:CompanyTask?=null
                try {
                    active=(restored.saveTask(draft) as CompanyTaskMutationResult.Applied).task
                    stage="task-eligible-members"
                    val snapshot=restored.liveProjects(null,null,project.id,assignees=true)
                    val member=snapshot.assigneePage!!.members.first()
                    val action=CompanyTaskAction(active,membershipIds=setOf(member.membershipId))
                    stage="task-assign-retry"
                    active=(restored.taskAction(action) as CompanyTaskActionResult.Assigned).task
                    check(active.assignees.single().membershipId==member.membershipId)
                    check((restored.taskAction(action) as CompanyTaskActionResult.Assigned).task==active)
                    val staleEdit=restored.saveTask(CompanyTaskEdit(project.id,action.base,title="Stale task edit"))
                    check(staleEdit is CompanyTaskMutationResult.Conflict)
                    stage="task-action-conflict"
                    active=(restored.saveTask(CompanyTaskEdit(project.id,active,status="completed")) as CompanyTaskMutationResult.Applied).task
                    val staleDelete=restored.taskAction(CompanyTaskAction(action.base,deleting=true,confirmation="DELETE"))
                    check(staleDelete is CompanyTaskActionResult.Conflict && staleDelete.current.version==active.version)
                    stage="task-action-delete"
                    val remove=CompanyTaskAction(active,deleting=true,confirmation="DELETE")
                    check(restored.taskAction(remove) is CompanyTaskActionResult.Deleted)
                    check(restored.taskAction(remove) is CompanyTaskActionResult.Deleted)
                    check(restored.liveProjects(null,null,project.id).taskPage!!.tasks.none {it.id==draft.id})
                    active=null;verifyCredentialOnly(instrumentation)
                    result.putString("companyApi","passed: live eligible members, task assignment/retry, edit/delete conflicts, confirmed deletion/retry and credential-only persistence")
                } finally {
                    if(active!=null)runCatching {
                        val current=restored.liveProjects(null,null,project.id).taskPage!!.tasks.firstOrNull {it.id==draft.id}
                        if(current!=null)restored.taskAction(CompanyTaskAction(current,deleting=true,confirmation="DELETE"))
                    }
                }
            } else if(fixture.optBoolean("clientMutations")) {
                stage="client-create"
                val draft=CompanyClientChange(value=CompanyClient(java.util.UUID.randomUUID().toString(),"Demo client lifecycle check","individual","Demo person","lifecycle@example.test","","","","email","Fictional device verification",1))
                var created:CompanyClient?=null;var project:CompanyProject?=null
                try {
                    created=(restored.changeClient(draft) as CompanyClientChangeResult.Saved).client
                    check((restored.changeClient(draft) as CompanyClientChangeResult.Saved).client==created)
                    stage="client-edit"
                    val edit=CompanyClientChange(base=created,value=created.copy(name="Demo client lifecycle edited"))
                    created=(restored.changeClient(edit) as CompanyClientChangeResult.Saved).client
                    check((restored.changeClient(edit) as CompanyClientChangeResult.Saved).client==created)
                    val conflict=restored.changeClient(edit.copy(operationId=java.util.UUID.randomUUID().toString()))
                    check(conflict is CompanyClientChangeResult.Conflict && conflict.current.version==2)
                    stage="client-link"
                    project=restored.createProject(CompanyProjectCreate(name="Demo client link verification",code="CLIENT-"+java.util.UUID.randomUUID().toString().take(8)))
                    val link=CompanyClientLink(restored.projectClient(project.id),created.id)
                    val linked=(restored.linkClient(link) as CompanyClientLinkResult.Linked).project
                    check(linked.clientId==created.id && linked.version==2)
                    check((restored.linkClient(link) as CompanyClientLinkResult.Linked).project==linked)
                    stage="linked-client-delete-denied"
                    try {restored.changeClient(CompanyClientChange(base=created,deleting=true,confirmation="DELETE"));error("Linked client deleted")}
                    catch(error:CompanyApiException){check(error.errorCode=="CLIENT_IN_USE")}
                    val stale=restored.linkClient(link.copy(clientId=null,operationId=java.util.UUID.randomUUID().toString()))
                    check(stale is CompanyClientLinkResult.Conflict)
                    stage="client-unlink-delete"
                    val unlink=CompanyClientLink(restored.projectClient(project.id),clientId=null)
                    check((restored.linkClient(unlink) as CompanyClientLinkResult.Linked).project.clientId==null)
                    val remove=CompanyClientChange(base=created,deleting=true,confirmation="DELETE")
                    check(restored.changeClient(remove) is CompanyClientChangeResult.Deleted)
                    check(restored.changeClient(remove) is CompanyClientChangeResult.Deleted)
                    created=null
                    val current=restored.liveProjects(null,null).page.projects.single {it.id==project.id}
                    check(restored.deleteProject(CompanyProjectDelete(current,confirmation="DELETE")) is CompanyProjectDeleteResult.Deleted)
                    project=null
                    verifyCredentialOnly(instrumentation)
                    result.putString("companyApi","passed: client create/edit/retry/conflict, project link/unlink, linked deletion refusal and confirmed deletion; credential-only storage")
                } finally {
                    val ownedProject=project;val ownedClient=created
                    if(ownedProject!=null) runCatching {
                        val link=restored.projectClient(ownedProject.id)
                        if(link.clientId==draft.value.id)restored.linkClient(CompanyClientLink(link,clientId=null))
                        val current=restored.liveProjects(null,null).page.projects.single {it.id==ownedProject.id}
                        restored.deleteProject(CompanyProjectDelete(current,confirmation="DELETE"))
                    }
                    if(ownedClient!=null) runCatching {
                        val current=restored.client(draft.value.id)
                        restored.changeClient(CompanyClientChange(base=current,deleting=true,confirmation="DELETE"))
                    }
                }
            } else if(fixture.optBoolean("clientContacts")) {
                stage="live-client-contacts"
                check(restored.session()?.identity?.roles?.contains("Staff") == true)
                check(restored.liveProjects(null,null).clientPage == null)
                val contacts = restored.liveProjects(null,null,clients=true).clientPage!!
                check(contacts.clients.size == 1 && contacts.clients.single().name == "Demo residential client")
                check(contacts.clients.single().email == "residential@example.test" && contacts.nextCursor == null)
                verifyCredentialOnly(instrumentation)
                restored.disconnect()
                check(restored.session() == null)
                check(CompanyAccountService(instrumentation.targetContext).account.session() == null)
                result.putString("companyApi","passed: assigned client contacts, opt-in live reads, credential-only storage and disconnect")
            } else if(fixture.optBoolean("projectLifecycle")) {
                stage="project-create"
                val draft=CompanyProjectCreate(name="Demo lifecycle verification",code="DEMO-LIFE-"+java.util.UUID.randomUUID().toString().take(8))
                val created=restored.createProject(draft)
                var removed=false
                try {
                    check(restored.createProject(draft)==created)
                    check(restored.liveProjects(null,null).page.projects.any {it.id==created.id})
                    stage="project-child-task"
                    restored.saveTask(CompanyTaskEdit(created.id,title="Retained lifecycle test task"))
                    stage="project-stale-deletion"
                    val edited=(restored.editProject(CompanyProjectEdit(created,name=created.name+" changed")) as CompanyProjectEditResult.Applied).project
                    val stale=CompanyProjectDelete(created,confirmation="DELETE")
                    check(restored.deleteProject(stale) is CompanyProjectDeleteResult.Conflict)
                    stage="project-delete-retry"
                    val action=CompanyProjectDelete(edited,confirmation="DELETE")
                    val deleted=restored.deleteProject(action) as CompanyProjectDeleteResult.Deleted
                    removed=true
                    check(restored.deleteProject(action)==deleted)
                    stage="project-hidden"
                    val snapshot=restored.liveProjects(null,null,created.id)
                    check(snapshot.page.projects.none {it.id==created.id} && snapshot.taskPage==null)
                    verifyCredentialOnly(instrumentation)
                    result.putString("companyApi","passed: project create/retry, stale deletion conflict, confirmed removal/retry, live project/task hiding and credential-only persistence")
                }finally{
                    if(!removed) {
                        val current=restored.projects().projects.find {it.id==created.id}
                        if(current!=null)check(restored.deleteProject(CompanyProjectDelete(current,confirmation="DELETE")) is CompanyProjectDeleteResult.Deleted)
                    }
                }
            } else if (fixture.optBoolean("editTasks")) {
                stage = "task-create"
                val project = restored.liveProjects(null,null).page.projects.single { it.code == "DEMO-01" }
                val draft = CompanyTaskEdit(project.id,id="a0100000-0000-4000-8000-000000000003",
                    operationId="a0200000-0000-4000-8000-000000000001",title="Demo online task",
                    description="Fictional task for online save and conflict checks.")
                val created = restored.saveTask(draft) as CompanyTaskMutationResult.Applied
                check(restored.saveTask(draft) == created)
                fun current() = restored.liveProjects(null,null,project.id).taskPage!!.tasks.single { it.id == draft.id }
                val original = current()
                try {
                    stage = "task-edit-retry"
                    val edit = CompanyTaskEdit(project.id,original,title=original.title+" [device verification]",
                        dueDate="2026-11-24",status="in_progress",priority="high")
                    val saved = restored.saveTask(edit) as CompanyTaskMutationResult.Applied
                    check(saved.task.version == original.version+1 && saved.task.status == "in_progress")
                    check(restored.saveTask(edit) == saved)
                    stage = "task-conflict"
                    check(restored.saveTask(CompanyTaskEdit(project.id,original,title="Competing task draft")) is CompanyTaskMutationResult.Conflict)
                } finally {
                    stage = "task-restore"
                    check(restored.saveTask(CompanyTaskEdit(project.id,current(),title=draft.title,description=draft.description,
                        dueDate="",status="open",priority="normal")) is CompanyTaskMutationResult.Applied)
                }
                verifyCredentialOnly(instrumentation)
                result.putString("companyApi","passed: task create, stable retries, edits, conflict and original details restored; credential-only persistence")
            } else if (fixture.optBoolean("signInOnly")) {
                val projects = restored.liveProjects(null, null).page.projects
                check(projects.isNotEmpty())
                val selected = projects.first { it.code == "DEMO-01" }
                stage = "live-task-page"
                val tasks = restored.liveProjects(null, null, selected.id).taskPage
                check(tasks?.projectId == selected.id && tasks.tasks.size >= 2)
                check(tasks.tasks.all { it.projectId == selected.id })
                verifyCredentialOnly(instrumentation)
                result.putString("companyApi", "passed: fresh live company session, selected-project tasks; credential-only persistence")
            } else if (fixture.optBoolean("liveProjects")) {
                stage = "live-initial"
                check(restored.session()?.identity?.roles?.contains("Staff") == true)
                var snapshot = restored.liveProjects(null, null)
                val original = snapshot.page.projects.single()
                check(original.code == "DEMO-01")
                signal("waiting-change")
                stage = "live-change"
                snapshot = restored.liveProjects(null, snapshot.revision)
                check(snapshot.page.projects.single().name == original.name + " [live verification]")
                signal("waiting-restore")
                stage = "live-restore"
                snapshot = restored.liveProjects(null, snapshot.revision)
                check(snapshot.page.projects.single().name == original.name)
                signal("waiting-assignment-removal")
                stage = "live-assignment-removal"
                snapshot = restored.liveProjects(null, snapshot.revision)
                check(snapshot.page.projects.isEmpty())
                signal("waiting-assignment-restore")
                stage = "live-assignment-restore"
                snapshot = restored.liveProjects(null, snapshot.revision)
                check(snapshot.page.projects.single().id == original.id)
                verifyCredentialOnly(instrumentation)
                signal("waiting-suspension")
                stage = "live-suspension-response"
                var refused: CompanyApiException? = null
                for (attempt in 0 until 3) {
                    try { snapshot = restored.liveProjects(null, snapshot.revision); check(snapshot.page.projects.isEmpty()) }
                    catch (error: CompanyApiException) { refused = error; break }
                }
                check(refused?.endsSession == true)
                stage = "live-suspension-storage"
                check(restored.session() == null && !restored.hasSavedSession())
                check(!CompanyAccountService(instrumentation.targetContext).account.hasSavedSession())
                result.putString("companyApi", "passed: live changes, restoration, assignment removal/restoration, session revocation, credential-only storage and retired backup routes")
            } else if (fixture.optBoolean("editProject")) {
                stage = "edit-project"
                val original = restored.projects().projects.single()
                try {
                    val draft = CompanyProjectEdit(original, name = original.name + " [device verification]")
                    val saved = restored.editProject(draft) as CompanyProjectEditResult.Applied
                    check(restored.editProject(draft) == saved)
                    check(restored.editProject(CompanyProjectEdit(original, name = "Competing draft")) is CompanyProjectEditResult.Conflict)
                } finally {
                    val current = restored.projects().projects.single { it.id == original.id }
                    if (current.name != original.name || current.code != original.code || current.status != original.status)
                        check(restored.editProject(CompanyProjectEdit(current, original.name, original.code, original.status)) is CompanyProjectEditResult.Applied)
                }
                verifyCredentialOnly(instrumentation)
                result.putString("companyApi", "passed: online save, stable retry, conflict, original details restored; no project data persisted")
            } else {
                stage = "assigned-projects"
                check(restored.session()?.identity?.roles?.contains("Staff") == true)
                check(restored.projects().projects.single().code == "DEMO-01")
                stage = "sessions"
                restored.revoke(restored.devices().first { it.current })
                check(!restored.hasSavedSession())
                fixture.optJSONObject("mfa")?.let { admin ->
                    stage = "MFA"
                    restored.signIn(admin.getString("email"), admin.getString("password"))
                    check(restored.pending != null && restored.session() == null)
                    restored.verify(totp(admin.getString("uri")), false)
                    check(restored.session()?.identity?.mfaEnabled == true)
                    restored.signOut()
                }
                restored.signIn(fixture.getString("email"), fixture.getString("password"))
                verifyCredentialOnly(instrumentation)
                result.putString("companyApi", "passed: live API connection, credential restore with fresh identity, assigned projects, session revocation and optional MFA")
            }
            instrumentation.finish(Activity.RESULT_OK, result)
        } catch (_: Exception) {
            result.putString("companyApi", "failed at $stage; private details withheld")
            instrumentation.finish(Activity.RESULT_CANCELED, result)
        } finally { input.delete() }
    }

    private fun verifyCredentialOnly(instrumentation: Instrumentation) {
        val sealed = instrumentation.targetContext.getSharedPreferences("company_api_account", 0).getString("session", null)
        if (sealed != null) {
            val bytes = java.util.Base64.getDecoder().decode(sealed)
            val value = JSONObject(String(KeystoreBox("mynahub_company_api_session").open(bytes), Charsets.UTF_8))
            check(value.keys().asSequence().toSet() == setOf("url", "token"))
        }
    }

    private fun verifyRetiredBackupRoutes(instrumentation: Instrumentation) {
        val db = androidx.room.Room.inMemoryDatabaseBuilder(instrumentation.targetContext,
            com.holagundi.archiman.data.local.ArchiManDatabase::class.java).build()
        val server = com.holagundi.archiman.portal.PortalServer(db, instrumentation.targetContext)
        try {
            val route = server.javaClass.getDeclaredMethod("route", java.net.Socket::class.java,
                com.holagundi.archiman.portal.PortalRequest::class.java, String::class.java, String::class.java)
            route.isAccessible = true
            for (path in listOf("/transfer", "/transfer/backup", "/transfer/restore")) {
                val output = java.io.ByteArrayOutputStream()
                val socket = object : java.net.Socket() { override fun getOutputStream(): java.io.OutputStream = output }
                val request = com.holagundi.archiman.portal.PortalRequest("POST", path, emptyMap(), emptyMap(), "")
                route.invoke(server, socket, request, "127.0.0.1", "127.0.0.1")
                check(output.toString("UTF-8").startsWith("HTTP/1.1 410 Gone"))
            }
        } finally { server.stop(false); db.close() }
    }

    private fun totp(uri: String): String {
        val secret = android.net.Uri.parse(uri).getQueryParameter("secret") ?: error("Missing test authenticator")
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val bits = secret.replace("=", "").map { alphabet.indexOf(it).toString(2).padStart(5, '0') }.joinToString("")
        val bytes = (0 until bits.length / 8).map { bits.substring(it * 8, it * 8 + 8).toInt(2).toByte() }.toByteArray()
        val counter = java.nio.ByteBuffer.allocate(8).putLong(System.currentTimeMillis() / 30000).array()
        val mac = javax.crypto.Mac.getInstance("HmacSHA1").apply { init(javax.crypto.spec.SecretKeySpec(bytes, "HmacSHA1")) }.doFinal(counter)
        val offset = mac.last().toInt() and 15
        val number = java.nio.ByteBuffer.wrap(mac, offset, 4).int and 0x7fffffff
        return (number % 1000000).toString().padStart(6, '0')
    }
}
