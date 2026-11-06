package com.holagundi.archiman

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.json.JSONArray
import org.json.JSONObject

/**
 * Entry point kept for clarity; invoke via
 * `am instrument -w -e suite screenWalk …/BackupDeviceChecks`.
 */
class ScreenWalkDeviceChecks : Instrumentation() {
    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        start()
    }

    override fun onStart() = ScreenWalkRunner.run(this)
}

/** Walks every practice shell destination and every project workspace page on device. */
object ScreenWalkRunner {
    private const val PACKAGE = "com.holagundi.archiman"

    fun run(instrumentation: Instrumentation) {
        val results = Bundle()
        val report = JSONArray()
        val failures = mutableListOf<String>()
        val device = UiDevice.getInstance(instrumentation)
        val target = instrumentation.targetContext
        try {
            // Do not force-stop the target: instrumentation runs in the app process.
            val walker = Walker(device, target, report, failures)
            walker.launchApp()
            walker.ensureSignedIn()
            walker.walkPracticeShell()
            walker.walkProjectWorkspace()
            results.putString("stream", "\nScreen-wise walk ${if (failures.isEmpty()) "PASSED" else "FAILED"} (${report.length()} screens)\n$report\n")
            results.putString("screenReport", report.toString(2))
            if (failures.isNotEmpty()) {
                results.putString("stream", results.getString("stream") + "Failures:\n" + failures.joinToString("\n") + "\n")
                instrumentation.finish(Activity.RESULT_CANCELED, results)
            } else {
                instrumentation.finish(Activity.RESULT_OK, results)
            }
        } catch (error: Throwable) {
            results.putString("stream", "\nScreen walk aborted: ${error.stackTraceToString()}\n$report\n")
            instrumentation.finish(Activity.RESULT_CANCELED, results)
        }
    }

    private class Walker(
        private val device: UiDevice,
        private val targetContext: android.content.Context,
        private val report: JSONArray,
        private val failures: MutableList<String>,
    ) {

    fun launchApp() {
        val intent = targetContext.packageManager.getLaunchIntentForPackage(PACKAGE)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        } ?: error("Launch intent missing for $PACKAGE")
        targetContext.startActivity(intent)
        device.wait(Until.hasObject(By.pkg(PACKAGE).depth(0)), 15_000)
            || error("ArchiMan did not open")
        device.waitForIdle(1_500)
    }

    fun ensureSignedIn() {
        if (awaitAny(listOf("Dashboard", "Projects", "Practice", "Skyline Villa Residence"), 4_000)) {
            pass("shell.session", "Signed-in practice shell visible")
            return
        }
        if (hasText("Unlock ArchiMan")) {
            fail("shell.lock", "App lock gate is blocking automation; disable device lock for this run or unlock once")
            error("App lock active")
        }
        if (hasText("Open a company file") || hasText("Open built-in company") || hasText("Built-in demo")) {
            clickFirst(listOf("Open built-in company", "Built-in demo", "Open demo company"))
            awaitAny(listOf("Open the built-in company?", "Sign in", "Dashboard"), 8_000)
            if (hasText("Open the built-in company?")) {
                clickFirst(listOf("Open demo company", "Open built-in company"))
            }
            awaitAny(listOf("Sign in", "Dashboard", "Projects", "Admin"), 12_000)
            if (hasText("Sign in") || hasText("Admin") || hasText("admin")) {
                clickFirst(listOf("Admin", "admin", "Principal", "Sign in"))
            }
            awaitAny(listOf("Dashboard", "Projects", "Practice"), 12_000)
                || error("Could not reach practice shell after demo sign-in")
            pass("shell.login", "Opened demo company and signed in")
            return
        }
        error("Unrecognized start screen; neither login nor practice shell was visible")
    }

    fun walkPracticeShell() {
        openTab("Dashboard")
        assertScreen("practice.dashboard", listOf("Dashboard", "Projects", "Practice"), listOf("Open tasks", "Reviews", "Issues", "Projects", "Search practice", "Create"))

        openTab("Projects")
        assertScreen("practice.projects", listOf("Projects", "New project"), listOf("Skyline Villa Residence", "Metro Transit Urban Pavilion", "No projects yet"))

        openTab("Practice")
        assertScreen("practice.menu", listOf("Your practice", "Finance", "Data and access"), listOf("Profile", "Clients", "Backup and restore"))

        val registries = listOf(
            Triple("practice.profile", "Profile", listOf("Studio Holagundi", "CoA", "Opening the practice")),
            Triple("practice.clients", "Clients", listOf("Clients", "Save client")),
            Triple("practice.invoices", "Invoices", listOf("Invoices", "No invoices yet", "Fee invoices")),
            Triple("practice.finance", "Taxes & banking", listOf("Taxes & banking")),
            Triple("practice.backup", "Backup and restore", listOf("Backup and restore", "Save backup")),
            Triple("practice.portal", "Portal", listOf("Wi-Fi portal", "Start portal")),
            Triple("practice.demo", "Demo", listOf("Built-in company file", "Studio Holagundi")),
        )
        registries.forEach { (id, openLabel, markers) ->
            openTab("Practice")
            if (!hasText("Your practice") && hasText("Practice menu")) clickText("Practice menu")
            scrollClick(openLabel)
            assertScreen(id, markers, markers)
            if (hasText("Practice menu")) clickText("Practice menu") else device.pressBack()
            device.waitForIdle(600)
        }
    }

    fun walkProjectWorkspace() {
        openTab("Projects")
        scrollClick("Skyline Villa Residence")
        awaitAny(listOf("Project overview", "Opening project…", "Overview"), 10_000)
            || error("Project workspace did not open")
        awaitGone("Opening project…", 8_000)
        assertScreen("project.OVERVIEW", listOf("Project overview"), listOf("Continue your work", "Tasks", "Measurement Book"))

        data class Step(val id: String, val group: String, val open: String?, val markers: List<String>, val nestedUnder: String? = null)
        val steps = listOf(
            Step("project.BRIEF", "Work", "Brief", listOf("Brief", "Save brief")),
            Step("project.TASKS", "Work", "Tasks", listOf("Tasks", "To do", "Add to To do")),
            Step("project.DOCUMENTS", "Work", "Documents", listOf("Documents", "Letters", "Drawings")),
            Step("project.LETTERS", "Work", "Letters", listOf("Letters", "Save letter"), nestedUnder = "Documents"),
            Step("project.DRAWINGS", "Work", "Drawings", listOf("Drawings", "Register"), nestedUnder = "Documents"),
            Step("project.VISITS", "Site", "Visits", listOf("Site visits", "Save visit")),
            Step("project.SNAGS", "Site", "Snags", listOf("Snags and NCRs", "New snag")),
            Step("project.COORDINATION", "Site", "Coordination", listOf("Coordination", "New item")),
            Step("project.SITEMAN", "Site", "Daily logs & orders", listOf("Siteman", "Daily log")),
            Step("project.MEASUREMENTS", "Site", "Measurements", listOf("Measurements", "Measurement setup", "Record measurement")),
            Step("project.SETUP", "Site", "Measurement setup", listOf("Measurement and billing setup"), nestedUnder = "Measurements"),
            Step("project.LEVELS", "Site", "Levels & rooms", listOf("Add level", "sheets"), nestedUnder = "Measurements"),
            Step("project.BOOK", "Site", "Measurement Book", listOf("Measurement Book"), nestedUnder = "Measurements"),
            Step("project.MEASURE", "Site", "Record measurement", listOf("Record entry", "Select contractor", "Loading contractor"), nestedUnder = "Measurements"),
            Step("project.PROPOSAL", "Fees", "Fee proposals", listOf("Fee proposals")),
            Step("project.CONTRACT", "Fees", "Agreements", listOf("Client–architect agreements", "Agreements")),
            Step("project.INVOICE", "Fees", "Client invoices", listOf("Client invoice")),
            Step("project.BILL", "Fees", "Running bill", listOf("Contractor running bills")),
        )

        steps.forEach { step ->
            openGroup(step.group)
            if (step.nestedUnder != null) {
                // Re-enter parent hub when needed.
                if (!hasAny(listOf(step.open!!, step.nestedUnder) + step.markers)) {
                    openGroup(step.group)
                }
                if (!hasText(step.open!!) && hasText(step.nestedUnder)) {
                    scrollClick(step.nestedUnder)
                    device.waitForIdle(700)
                }
            }
            step.open?.let { label ->
                if (!hasAny(step.markers)) scrollClick(label)
            }
            // Measure opens a dedicated capture chrome; dismiss contractor picker noise if it blocks.
            if (step.id == "project.MEASURE" && hasText("Select contractor")) {
                assertScreen(step.id, listOf("Select contractor", "Record entry"), step.markers)
                device.pressBack()
                device.waitForIdle(500)
                if (hasText("Leave without saving?")) clickText("Discard and leave")
                return@forEach
            }
            assertScreen(step.id, step.markers, step.markers)
            // Prefer in-app back to leave child pages without exiting the project.
            device.pressBack()
            device.waitForIdle(700)
            if (hasText("Leave without saving?")) {
                clickText("Discard and leave")
                device.waitForIdle(500)
            }
            // If we left the project accidentally, reopen.
            if (!hasAny(listOf("Overview", "Work", "Site", "Fees", "Project overview", "Back to practice", "Back to section"))) {
                openTab("Projects")
                scrollClick("Skyline Villa Residence")
                awaitAny(listOf("Project overview", "Overview"), 8_000)
            }
        }

        // Return to practice shell.
        while (!hasText("Dashboard") && !hasText("Projects") && !hasText("Practice")) {
            device.pressBack()
            device.waitForIdle(400)
            if (hasText("Leave without saving?")) clickText("Discard and leave")
        }
        pass("project.exit", "Returned to practice shell")
    }

    private fun openTab(label: String) {
        if (!clickText(label, 2_000)) {
            // Bottom nav text can be truncated; try partial.
            val node = device.findObject(By.textContains(label.take(4)))
                ?: error("Could not open tab $label")
            node.click()
        }
        device.waitForIdle(800)
    }

    private fun openGroup(label: String) {
        // Tapping the selected group again opens its hub.
        clickText(label, 2_000) || clickText(label, 2_000)
        device.waitForIdle(700)
        clickText(label, 1_500)
        device.waitForIdle(700)
    }

    private fun assertScreen(id: String, requiredAny: List<String>, markers: List<String>) {
        assertAlive()
        val hit = markers.firstOrNull { hasText(it) } ?: requiredAny.firstOrNull { hasText(it) }
        if (hit == null) {
            // One fling in case content is below the fold.
            device.findObject(By.scrollable(true))?.scroll(Direction.DOWN, 0.6f)
            device.waitForIdle(400)
        }
        val found = markers.firstOrNull { hasText(it) } ?: requiredAny.firstOrNull { hasText(it) }
        if (found == null) {
            fail(id, "Missing markers $markers; visible=${visibleSample()}")
        } else {
            pass(id, "Visible marker: $found")
        }
    }

    private fun pass(id: String, detail: String) {
        report.put(JSONObject().put("id", id).put("status", "PASS").put("detail", detail))
    }

    private fun fail(id: String, detail: String) {
        failures += "$id: $detail"
        report.put(JSONObject().put("id", id).put("status", "FAIL").put("detail", detail))
    }

    private fun assertAlive() {
        val foreground = device.currentPackageName
        check(foreground == PACKAGE) { "Left ArchiMan (now $foreground) — likely a crash" }
    }

    private fun hasText(text: String): Boolean =
        device.findObject(By.text(text)) != null || device.findObject(By.textContains(text)) != null

    private fun hasAny(texts: List<String>): Boolean = texts.any(::hasText)

    private fun awaitAny(texts: List<String>, timeoutMs: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (hasAny(texts)) return true
            device.waitForIdle(200)
        }
        return hasAny(texts)
    }

    private fun awaitGone(text: String, timeoutMs: Long) {
        device.wait(Until.gone(By.text(text)), timeoutMs)
    }

    private fun clickText(text: String, timeoutMs: Long = 3_000): Boolean {
        val found = device.wait(Until.findObject(By.text(text)), timeoutMs)
            ?: device.wait(Until.findObject(By.textContains(text)), timeoutMs)
            ?: return false
        found.click()
        device.waitForIdle(400)
        return true
    }

    private fun clickFirst(labels: List<String>) {
        labels.firstOrNull { clickText(it, 2_500) } ?: error("None of $labels were clickable")
    }

    private fun scrollClick(text: String) {
        if (clickText(text, 1_500)) return
        repeat(6) {
            val scrollable = device.findObject(By.scrollable(true)) ?: return@repeat
            scrollable.scroll(Direction.DOWN, 0.8f)
            device.waitForIdle(300)
            if (clickText(text, 800)) return
        }
        // Try from top.
        repeat(4) {
            device.findObject(By.scrollable(true))?.scroll(Direction.UP, 0.9f)
            device.waitForIdle(250)
            if (clickText(text, 800)) return
        }
        error("Could not scroll to and click '$text'")
    }

    private fun visibleSample(): String = buildString {
        device.findObjects(By.clickable(true)).take(12).forEach { node ->
            val t = node.text ?: node.contentDescription
            if (!t.isNullOrBlank()) append(t).append(" | ")
        }
    }
    }
}
