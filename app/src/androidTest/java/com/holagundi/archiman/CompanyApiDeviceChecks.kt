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
            val account = CompanyAccountService(instrumentation.targetContext).account
            account.signOut()
            stage = "connection"
            account.connect(fixture.getString("apiUrl"))
            stage = "sign-in"
            account.signIn(fixture.getString("email"), fixture.getString("password"))
            check(account.pending == null)
            stage = "credential-only-storage"
            verifyCredentialOnly(instrumentation)
            val restored = CompanyAccountService(instrumentation.targetContext).account
            check(restored.session() == null && restored.hasSavedSession())
            restored.refresh()
            check(restored.session() != null)
            if (fixture.optBoolean("signInOnly")) {
                val projects = restored.liveProjects(null, null).page.projects
                check(projects.isNotEmpty())
                val selected = projects.first { it.code == "DEMO-01" }
                stage = "live-task-page"
                val tasks = restored.liveProjects(null, null, selected.id).taskPage
                check(tasks?.projectId == selected.id && tasks.tasks.size == 2)
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
