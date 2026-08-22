package com.example.server

import android.content.Context
import android.util.Log
import com.example.data.local.entity.*
import com.example.data.repository.SiteRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class LocalLanServer(
    private val context: Context,
    private val repository: SiteRepository,
    private val port: Int = 8080
) {
    private var serverSocket: ServerSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val threadPool = Executors.newFixedThreadPool(4)
    @Volatile
    private var isRunning = false

    fun start(): Boolean {
        if (isRunning) return true
        return try {
            val sSocket = ServerSocket(port)
            serverSocket = sSocket
            isRunning = true
            Log.i("LocalLanServer", "LAN Server started on port $port")

            scope.launch {
                while (isRunning && !sSocket.isClosed) {
                    try {
                        val clientSocket = sSocket.accept()
                        threadPool.execute {
                            handleClient(clientSocket)
                        }
                    } catch (e: Exception) {
                        if (isRunning) {
                            Log.e("LocalLanServer", "Accept error", e)
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.e("LocalLanServer", "Failed to start LAN server on port $port", e)
            isRunning = false
            false
        }
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            Log.e("LocalLanServer", "Error closing server socket", e)
        }
        serverSocket = null
        Log.i("LocalLanServer", "LAN Server stopped")
    }

    fun isServerRunning(): Boolean = isRunning

    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return "127.0.0.1"
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr.address.size == 4) {
                        val host = addr.hostAddress ?: ""
                        if (host.isNotEmpty() && !host.startsWith("127.")) {
                            return host
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("LocalLanServer", "Error getting IP", e)
        }
        return "127.0.0.1"
    }

    fun getServerUrl(): String {
        return "http://${getLocalIpAddress()}:$port"
    }

    private fun handleClient(socket: Socket) {
        try {
            socket.soTimeout = 10000
            val input = socket.getInputStream()
            val reader = BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8))
            val output = socket.getOutputStream()

            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) {
                sendResponse(output, 400, "Bad Request", "text/plain", "Bad Request".toByteArray())
                return
            }

            val method = parts[0].uppercase()
            val fullPath = parts[1]
            val path = if (fullPath.contains("?")) fullPath.substringBefore("?") else fullPath
            val queryString = if (fullPath.contains("?")) fullPath.substringAfter("?") else ""

            val headers = mutableMapOf<String, String>()
            var line: String?
            var contentLength = 0
            while (reader.readLine().also { line = it } != null) {
                if (line.isNullOrBlank()) break
                val colonIdx = line!!.indexOf(":")
                if (colonIdx > 0) {
                    val key = line!!.substring(0, colonIdx).trim().lowercase()
                    val value = line!!.substring(colonIdx + 1).trim()
                    headers[key] = value
                    if (key == "content-length") {
                        contentLength = value.toIntOrNull() ?: 0
                    }
                }
            }

            var body = ""
            if (contentLength > 0) {
                val charBuf = CharArray(contentLength)
                var read = 0
                while (read < contentLength) {
                    val r = reader.read(charBuf, read, contentLength - read)
                    if (r == -1) break
                    read += r
                }
                body = String(charBuf, 0, read)
            }

            if (method == "OPTIONS") {
                sendOptionsResponse(output)
                return
            }

            when {
                path == "/" || path == "/index.html" -> {
                    val html = generateWebDashboardHtml()
                    val bytes = html.toByteArray(StandardCharsets.UTF_8)
                    sendResponse(output, 200, "OK", "text/html; charset=UTF-8", bytes)
                }
                path == "/api/data" -> {
                    runBlocking(Dispatchers.IO) {
                        handleApiData(output)
                    }
                }
                path == "/api/measurement" -> {
                    runBlocking(Dispatchers.IO) {
                        handleApiMeasurement(method, queryString, body, output)
                    }
                }
                path == "/api/rate" -> {
                    runBlocking(Dispatchers.IO) {
                        handleApiRate(method, body, output)
                    }
                }
                path == "/api/export/csv" -> {
                    runBlocking(Dispatchers.IO) {
                        handleApiExportCsv(queryString, output)
                    }
                }
                else -> {
                    sendResponse(output, 404, "Not Found", "text/plain", "404 Not Found".toByteArray())
                }
            }
        } catch (e: Exception) {
            Log.e("LocalLanServer", "Error handling request", e)
        } finally {
            try {
                socket.close()
            } catch (_: Exception) {}
        }
    }

    private suspend fun handleApiData(output: OutputStream) {
        try {
            val projects = repository.allProjects.first()
            val contractors = repository.allContractors.first()
            val items = repository.allItems.first()
            val measurements = repository.allMeasurements.first()
            val bills = repository.allBills.first()

            val root = JSONObject().apply {
                put("serverName", "Site Measurement Local LAN Server")
                put("deviceIp", getLocalIpAddress())
                put("projects", JSONArray().apply {
                    projects.forEach { p ->
                        put(JSONObject().apply {
                            put("id", p.id)
                            put("name", p.name)
                            put("client", p.client)
                            put("siteLocation", p.siteLocation)
                        })
                    }
                })
                put("contractors", JSONArray().apply {
                    contractors.forEach { c ->
                        put(JSONObject().apply {
                            put("id", c.id)
                            put("projectId", c.projectId)
                            put("name", c.name)
                            put("phone", c.phone)
                        })
                    }
                })
                put("items", JSONArray().apply {
                    items.forEach { i ->
                        put(JSONObject().apply {
                            put("id", i.id)
                            put("name", i.name)
                            put("unit", i.unit)
                            put("calcType", i.calculationType.name)
                            put("defaultRate", i.defaultRate)
                        })
                    }
                })
                put("measurements", JSONArray().apply {
                    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    measurements.forEach { m ->
                        put(JSONObject().apply {
                            put("id", m.id)
                            put("projectId", m.projectId)
                            put("contractorId", m.contractorId)
                            put("contractorName", m.contractorName)
                            put("itemId", m.itemId)
                            put("itemName", m.itemName)
                            put("unit", m.unit)
                            put("calcType", m.calculationType.name)
                            put("length", m.length)
                            put("width", m.width)
                            put("height", m.height)
                            put("nos", m.nos)
                            put("deduction", m.deduction)
                            put("quantity", m.quantity)
                            put("rate", m.rate)
                            put("amount", m.amount)
                            put("floor", m.floor)
                            put("location", m.location)
                            put("remarks", m.remarks)
                            put("date", m.date)
                            put("dateFormatted", sdf.format(Date(m.date)))
                            put("billId", m.billId ?: 0)
                        })
                    }
                })
                put("bills", JSONArray().apply {
                    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    bills.forEach { b ->
                        put(JSONObject().apply {
                            put("id", b.id)
                            put("billNumber", b.billNumber)
                            put("projectId", b.projectId)
                            put("projectName", b.projectName)
                            put("contractorId", b.contractorId)
                            put("contractorName", b.contractorName)
                            put("dateFormatted", sdf.format(Date(b.date)))
                            put("totalQuantity", b.totalQuantity)
                            put("totalAmount", b.totalAmount)
                            put("retentionPercent", b.retentionPercent)
                            put("netAmount", b.netAmount)
                            put("notes", b.notes)
                        })
                    }
                })
            }

            val jsonBytes = root.toString(2).toByteArray(StandardCharsets.UTF_8)
            sendResponse(output, 200, "OK", "application/json; charset=UTF-8", jsonBytes)
        } catch (e: Exception) {
            val err = JSONObject().put("error", e.message ?: "Server error").toString()
            sendResponse(output, 500, "Internal Server Error", "application/json", err.toByteArray())
        }
    }

    private suspend fun handleApiMeasurement(method: String, queryString: String, body: String, output: OutputStream) {
        try {
            when (method) {
                "POST" -> {
                    val json = JSONObject(body)
                    val id = if (json.has("id")) json.optLong("id", 0L) else 0L
                    val projectId = json.getLong("projectId")
                    val contractorId = json.getLong("contractorId")
                    val contractorName = json.optString("contractorName", "")
                    val itemId = json.getLong("itemId")
                    val itemName = json.optString("itemName", "")
                    val unit = json.optString("unit", "m²")
                    val calcTypeStr = json.optString("calcType", "AREA")
                    val calcType = try {
                        CalculationType.valueOf(calcTypeStr)
                    } catch (e: Exception) {
                        CalculationType.AREA
                    }

                    val length = json.optDouble("length", 0.0)
                    val width = json.optDouble("width", 0.0)
                    val height = json.optDouble("height", 0.0)
                    val nos = json.optDouble("nos", 1.0)
                    val deduction = json.optDouble("deduction", 0.0)
                    val rate = json.optDouble("rate", 0.0)
                    val floor = json.optString("floor", "")
                    val location = json.optString("location", "")
                    val remarks = json.optString("remarks", "")

                    val quantity = when (calcType) {
                        CalculationType.RUNNING_LENGTH -> length * nos
                        CalculationType.AREA -> length * width * nos
                        CalculationType.WALL_PLASTER -> (length * height * nos) - deduction
                        CalculationType.VOLUME -> length * width * height * nos
                        CalculationType.NOS -> nos
                    }
                    val amount = quantity * rate

                    val measurement = MeasurementEntity(
                        id = id,
                        projectId = projectId,
                        contractorId = contractorId,
                        contractorName = contractorName,
                        itemId = itemId,
                        itemName = itemName,
                        unit = unit,
                        calculationType = calcType,
                        length = length,
                        width = width,
                        height = height,
                        nos = nos,
                        deduction = deduction,
                        quantity = Math.round(quantity * 100.0) / 100.0,
                        rate = rate,
                        amount = Math.round(amount * 100.0) / 100.0,
                        floor = floor,
                        location = location,
                        remarks = remarks,
                        date = System.currentTimeMillis()
                    )

                    if (id > 0) {
                        repository.updateMeasurement(measurement)
                    } else {
                        repository.insertMeasurement(measurement)
                    }

                    val res = JSONObject().apply {
                        put("success", true)
                        put("message", "Measurement saved")
                    }.toString()
                    sendResponse(output, 200, "OK", "application/json; charset=UTF-8", res.toByteArray())
                }
                "DELETE" -> {
                    val id = queryString.split("&")
                        .firstOrNull { it.startsWith("id=") }
                        ?.substringAfter("id=")?.toLongOrNull()

                    if (id != null) {
                        repository.deleteMeasurementById(id)
                        val res = JSONObject().apply {
                            put("success", true)
                            put("message", "Deleted measurement $id")
                        }.toString()
                        sendResponse(output, 200, "OK", "application/json; charset=UTF-8", res.toByteArray())
                    } else {
                        val res = JSONObject().put("error", "Missing measurement id").toString()
                        sendResponse(output, 400, "Bad Request", "application/json; charset=UTF-8", res.toByteArray())
                    }
                }
                else -> {
                    val res = JSONObject().put("error", "Method not allowed").toString()
                    sendResponse(output, 405, "Method Not Allowed", "application/json; charset=UTF-8", res.toByteArray())
                }
            }
        } catch (e: Exception) {
            val err = JSONObject().put("error", e.message ?: "Server error").toString()
            sendResponse(output, 500, "Internal Server Error", "application/json", err.toByteArray())
        }
    }

    private suspend fun handleApiRate(method: String, body: String, output: OutputStream) {
        try {
            if (method == "POST") {
                val json = JSONObject(body)
                val contractorId = json.getLong("contractorId")
                val itemId = json.getLong("itemId")
                val rate = json.getDouble("rate")

                repository.saveContractorRate(contractorId, itemId, rate)
                val res = JSONObject().apply {
                    put("success", true)
                    put("message", "Rate updated")
                }.toString()
                sendResponse(output, 200, "OK", "application/json; charset=UTF-8", res.toByteArray())
            } else {
                val res = JSONObject().put("error", "Method not allowed").toString()
                sendResponse(output, 405, "Method Not Allowed", "application/json; charset=UTF-8", res.toByteArray())
            }
        } catch (e: Exception) {
            val err = JSONObject().put("error", e.message ?: "Server error").toString()
            sendResponse(output, 500, "Internal Server Error", "application/json", err.toByteArray())
        }
    }

    private suspend fun handleApiExportCsv(queryString: String, output: OutputStream) {
        try {
            val projectId = queryString.split("&")
                .firstOrNull { it.startsWith("projectId=") }
                ?.substringAfter("projectId=")?.toLongOrNull()

            val all = repository.allMeasurements.first()
            val measurements = if (projectId != null && projectId > 0) {
                all.filter { it.projectId == projectId }
            } else {
                all
            }

            val sb = java.lang.StringBuilder()
            sb.append("ID,Date,Contractor,Item,Calculation Type,Length,Width,Height,Nos,Deduction,Quantity,Unit,Rate (INR),Amount (INR),Floor,Location,Remarks\n")

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            for (m in measurements) {
                val dateStr = sdf.format(Date(m.date))
                sb.append("${m.id},\"$dateStr\",\"${m.contractorName.replace("\"", "\"\"")}\",\"${m.itemName.replace("\"", "\"\"")}\",\"${m.calculationType.displayName}\",${m.length},${m.width},${m.height},${m.nos},${m.deduction},${m.quantity},\"${m.unit}\",${m.rate},${m.amount},\"${m.floor}\",\"${m.location}\",\"${m.remarks.replace("\"", "\"\"")}\"\n")
            }

            val bytes = sb.toString().toByteArray(StandardCharsets.UTF_8)
            val extraHeaders = mapOf(
                "Content-Disposition" to "attachment; filename=\"site_measurements.csv\""
            )
            sendResponse(output, 200, "OK", "text/csv; charset=UTF-8", bytes, extraHeaders)
        } catch (e: Exception) {
            val err = JSONObject().put("error", e.message ?: "Server error").toString()
            sendResponse(output, 500, "Internal Server Error", "application/json", err.toByteArray())
        }
    }

    private fun sendOptionsResponse(output: OutputStream) {
        val header = "HTTP/1.1 204 No Content\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Access-Control-Allow-Methods: GET, POST, DELETE, OPTIONS\r\n" +
                "Access-Control-Allow-Headers: Content-Type, Authorization\r\n" +
                "Connection: close\r\n\r\n"
        output.write(header.toByteArray(StandardCharsets.UTF_8))
        output.flush()
    }

    private fun sendResponse(
        output: OutputStream,
        statusCode: Int,
        statusText: String,
        contentType: String,
        body: ByteArray,
        extraHeaders: Map<String, String> = emptyMap()
    ) {
        val sb = java.lang.StringBuilder()
        sb.append("HTTP/1.1 $statusCode $statusText\r\n")
        sb.append("Content-Type: $contentType\r\n")
        sb.append("Content-Length: ${body.size}\r\n")
        sb.append("Access-Control-Allow-Origin: *\r\n")
        sb.append("Connection: close\r\n")
        for ((k, v) in extraHeaders) {
            sb.append("$k: $v\r\n")
        }
        sb.append("\r\n")

        output.write(sb.toString().toByteArray(StandardCharsets.UTF_8))
        output.write(body)
        output.flush()
    }

    private fun generateWebDashboardHtml(): String {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Site Measurement & Billing - Local LAN Portal</title>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700&display=swap" rel="stylesheet">
    <style>
        :root {
            --primary: #005FAC;
            --primary-dark: #004580;
            --primary-light: #EBF1FF;
            --bg-body: #F8F9FF;
            --bg-card: #FFFFFF;
            --text-main: #0F172A;
            --text-muted: #64748B;
            --border: #DCE3F0;
            --success: #16A34A;
            --danger: #DC2626;
            --radius: 12px;
        }
        * { box-sizing: border-box; margin: 0; padding: 0; font-family: 'Plus Jakarta Sans', -apple-system, sans-serif; }
        body { background: var(--bg-body); color: var(--text-main); line-height: 1.5; padding-bottom: 60px; }
        header { background: #0F172A; color: white; padding: 1rem 1.5rem; display: flex; justify-content: space-between; align-items: center; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); }
        .logo-area { display: flex; align-items: center; gap: 12px; }
        .logo-icon { background: var(--primary); color: white; width: 38px; height: 38px; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-weight: bold; font-size: 20px; }
        .lan-badge { background: rgba(34, 197, 94, 0.2); color: #4ade80; padding: 4px 10px; border-radius: 20px; font-size: 12px; font-weight: 600; display: inline-flex; align-items: center; gap: 6px; }
        .container { max-width: 1280px; margin: 1.5rem auto; padding: 0 1.2rem; }
        
        .stat-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 1rem; margin-bottom: 1.5rem; }
        .stat-card { background: var(--bg-card); padding: 1.2rem; border-radius: var(--radius); border: 1px solid var(--border); box-shadow: 0 1px 3px rgba(0,0,0,0.05); }
        .stat-label { font-size: 0.82rem; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.5px; font-weight: 600; }
        .stat-val { font-size: 1.6rem; font-weight: 700; color: var(--text-main); margin-top: 4px; }
        
        .nav-tabs { display: flex; gap: 8px; border-bottom: 2px solid var(--border); margin-bottom: 1.2rem; }
        .tab-btn { padding: 8px 18px; border: none; background: none; font-size: 14px; font-weight: 600; color: var(--text-muted); cursor: pointer; border-bottom: 3px solid transparent; margin-bottom: -2px; }
        .tab-btn.active { color: var(--primary); border-bottom-color: var(--primary); }
        
        .card { background: var(--bg-card); border-radius: var(--radius); border: 1px solid var(--border); padding: 1.2rem; box-shadow: 0 1px 3px rgba(0,0,0,0.05); margin-bottom: 1.5rem; }
        .toolbar { display: flex; flex-wrap: wrap; gap: 10px; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
        .filters { display: flex; flex-wrap: wrap; gap: 10px; align-items: center; }
        
        select, input, button { padding: 8px 14px; border: 1px solid var(--border); border-radius: 6px; font-size: 14px; outline: none; }
        select:focus, input:focus { border-color: var(--primary); }
        .btn-primary { background: var(--primary); color: white; border: none; font-weight: 600; cursor: pointer; transition: background 0.2s; }
        .btn-primary:hover { background: var(--primary-dark); }
        .btn-secondary { background: #f1f5f9; color: var(--text-main); cursor: pointer; }
        .btn-secondary:hover { background: #e2e8f0; }
        
        table { width: 100%; border-collapse: collapse; text-align: left; font-size: 13.5px; }
        th { background: #f8fafc; padding: 10px 12px; color: var(--text-muted); font-weight: 600; border-bottom: 2px solid var(--border); }
        td { padding: 10px 12px; border-bottom: 1px solid var(--border); }
        tr:hover td { background: #f8fafc; }
        .badge { display: inline-block; padding: 2px 8px; border-radius: 12px; font-size: 11px; font-weight: 600; background: #e2e8f0; }
        .badge-qty { background: #dbeafe; color: #1e40af; }
        .badge-amt { background: #dcfce7; color: #166534; font-weight: 700; }
        
        .modal { display: none; position: fixed; inset: 0; background: rgba(0,0,0,0.5); align-items: center; justify-content: center; z-index: 100; padding: 1rem; }
        .modal.open { display: flex; }
        .modal-content { background: white; width: 100%; max-width: 550px; border-radius: var(--radius); padding: 1.5rem; max-height: 90vh; overflow-y: auto; }
        .modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
        .form-group { margin-bottom: 12px; }
        .form-group label { display: block; font-size: 12px; font-weight: 600; color: var(--text-muted); margin-bottom: 4px; }
        .form-group input, .form-group select { width: 100%; }
        .form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
        .calc-preview { background: #fff7ed; border: 1px solid #fed7aa; padding: 10px; border-radius: 6px; margin: 12px 0; display: flex; justify-content: space-between; }
    </style>
</head>
<body>
    <header>
        <div class="logo-area">
            <div class="logo-icon">📐</div>
            <div>
                <h1 style="font-size: 18px; font-weight: 700;">Site Measurement & Billing</h1>
                <p style="font-size: 12px; color: #94a3b8;">Local Wi-Fi Network Access</p>
            </div>
        </div>
        <div>
            <span class="lan-badge">● LAN Connected</span>
        </div>
    </header>

    <div class="container">
        <div class="stat-grid">
            <div class="stat-card">
                <div class="stat-label">Total Measurements</div>
                <div class="stat-val" id="stat-count">0</div>
            </div>
            <div class="stat-card">
                <div class="stat-label">Total Work Amount</div>
                <div class="stat-val" id="stat-amount" style="color: var(--primary);">₹0</div>
            </div>
            <div class="stat-card">
                <div class="stat-label">Projects / Contractors</div>
                <div class="stat-val" id="stat-meta">0 / 0</div>
            </div>
            <div class="stat-card">
                <div class="stat-label">Generated Bills</div>
                <div class="stat-val" id="stat-bills">0</div>
            </div>
        </div>

        <div class="nav-tabs">
            <button class="tab-btn active" onclick="switchTab('measurements')">Measurements Register</button>
            <button class="tab-btn" onclick="switchTab('summary')">Item-wise Summary</button>
            <button class="tab-btn" onclick="switchTab('rates')">Contractor Rates</button>
            <button class="tab-btn" onclick="switchTab('bills')">Bills & Reports</button>
        </div>

        <!-- TAB: MEASUREMENTS -->
        <div id="tab-measurements" class="tab-content card">
            <div class="toolbar">
                <div class="filters">
                    <select id="filter-project" onchange="applyFilters()">
                        <option value="all">All Projects</option>
                    </select>
                    <select id="filter-contractor" onchange="applyFilters()">
                        <option value="all">All Contractors</option>
                    </select>
                    <input type="text" id="search-input" placeholder="Search item, location, floor..." onkeyup="applyFilters()" style="width: 200px;">
                </div>
                <div style="display: flex; gap: 8px;">
                    <button class="btn-secondary" onclick="exportCsv()">📥 Export CSV (Excel)</button>
                    <button class="btn-primary" onclick="openNewModal()">+ New Measurement</button>
                </div>
            </div>

            <div style="overflow-x: auto;">
                <table>
                    <thead>
                        <tr>
                            <th>Date</th>
                            <th>Contractor</th>
                            <th>Item</th>
                            <th>Dimensions (L×W×H / Nos)</th>
                            <th>Quantity</th>
                            <th>Rate</th>
                            <th>Amount</th>
                            <th>Location</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody id="measurements-tbody">
                        <tr><td colspan="9" style="text-align: center; color: var(--text-muted); padding: 2rem;">Loading site data...</td></tr>
                    </tbody>
                </table>
            </div>
        </div>

        <!-- TAB: SUMMARY -->
        <div id="tab-summary" class="tab-content card" style="display: none;">
            <h3 style="margin-bottom: 1rem;">Cumulative Item Summary by Contractor</h3>
            <div style="overflow-x: auto;">
                <table>
                    <thead>
                        <tr>
                            <th>Contractor</th>
                            <th>Item</th>
                            <th>Unit</th>
                            <th>Measurement Count</th>
                            <th>Total Quantity</th>
                            <th>Standard Rate</th>
                            <th>Total Cumulative Amount</th>
                        </tr>
                    </thead>
                    <tbody id="summary-tbody"></tbody>
                </table>
            </div>
        </div>

        <!-- TAB: RATES -->
        <div id="tab-rates" class="tab-content card" style="display: none;">
            <h3 style="margin-bottom: 1rem;">Contractor Master Rates</h3>
            <div style="overflow-x: auto;">
                <table>
                    <thead>
                        <tr>
                            <th>Contractor</th>
                            <th>Item</th>
                            <th>Unit</th>
                            <th>Calculation Type</th>
                            <th>Current Rate (₹)</th>
                        </tr>
                    </thead>
                    <tbody id="rates-tbody"></tbody>
                </table>
            </div>
        </div>

        <!-- TAB: BILLS -->
        <div id="tab-bills" class="tab-content card" style="display: none;">
            <div class="toolbar">
                <h3>Contractor Bills</h3>
                <button class="btn-secondary" onclick="window.print()">🖨️ Print View</button>
            </div>
            <div style="overflow-x: auto;">
                <table>
                    <thead>
                        <tr>
                            <th>Bill #</th>
                            <th>Date</th>
                            <th>Project</th>
                            <th>Contractor</th>
                            <th>Total Qty</th>
                            <th>Gross Amount</th>
                            <th>Net Payable</th>
                            <th>Notes</th>
                        </tr>
                    </thead>
                    <tbody id="bills-tbody"></tbody>
                </table>
            </div>
        </div>
    </div>

    <!-- MODAL ADD / EDIT -->
    <div id="entry-modal" class="modal">
        <div class="modal-content">
            <div class="modal-header">
                <h3 id="modal-title">New Site Measurement</h3>
                <button onclick="closeModal()" style="background: none; border: none; font-size: 20px; cursor: pointer;">&times;</button>
            </div>
            <form id="measure-form" onsubmit="saveMeasurement(event)">
                <input type="hidden" id="form-id" value="0">
                <div class="form-row">
                    <div class="form-group">
                        <label>Project *</label>
                        <select id="form-project" required></select>
                    </div>
                    <div class="form-group">
                        <label>Contractor *</label>
                        <select id="form-contractor" required onchange="onContractorOrItemChange()"></select>
                    </div>
                </div>

                <div class="form-row">
                    <div class="form-group">
                        <label>Item *</label>
                        <select id="form-item" required onchange="onItemSelectionChange()"></select>
                    </div>
                    <div class="form-group">
                        <label>Rate (₹) *</label>
                        <input type="number" id="form-rate" step="0.01" required oninput="calcPreview()">
                    </div>
                </div>

                <!-- Dynamic Fields -->
                <div class="form-row" id="dim-l-w">
                    <div class="form-group" id="group-length">
                        <label>Length (m)</label>
                        <input type="number" id="form-length" step="0.01" value="0" oninput="calcPreview()">
                    </div>
                    <div class="form-group" id="group-width">
                        <label id="label-width">Width (m)</label>
                        <input type="number" id="form-width" step="0.01" value="0" oninput="calcPreview()">
                    </div>
                </div>

                <div class="form-row" id="dim-h-nos">
                    <div class="form-group" id="group-height">
                        <label>Height (m)</label>
                        <input type="number" id="form-height" step="0.01" value="0" oninput="calcPreview()">
                    </div>
                    <div class="form-group" id="group-nos">
                        <label>Nos (Multiplier)</label>
                        <input type="number" id="form-nos" step="1" value="1" oninput="calcPreview()">
                    </div>
                </div>

                <div class="form-group" id="group-deduction" style="display: none;">
                    <label>Deduction Area (m²)</label>
                    <input type="number" id="form-deduction" step="0.01" value="0" oninput="calcPreview()">
                </div>

                <div class="calc-preview">
                    <div>
                        <div style="font-size: 11px; color: var(--text-muted); font-weight: 600;">CALCULATED QUANTITY</div>
                        <div id="prev-qty" style="font-size: 16px; font-weight: 700; color: #005FAC;">0.00</div>
                    </div>
                    <div style="text-align: right;">
                        <div style="font-size: 11px; color: var(--text-muted); font-weight: 600;">TOTAL AMOUNT</div>
                        <div id="prev-amt" style="font-size: 16px; font-weight: 700; color: var(--primary);">₹0</div>
                    </div>
                </div>

                <div class="form-row">
                    <div class="form-group">
                        <label>Floor (Optional)</label>
                        <input type="text" id="form-floor" placeholder="e.g. GF, 1st Floor">
                    </div>
                    <div class="form-group">
                        <label>Room / Location (Optional)</label>
                        <input type="text" id="form-location" placeholder="e.g. Room 1, North Wall">
                    </div>
                </div>

                <div class="form-group">
                    <label>Remarks (Optional)</label>
                    <input type="text" id="form-remarks" placeholder="Notes or specifications">
                </div>

                <div style="display: flex; justify-content: flex-end; gap: 8px; margin-top: 1rem;">
                    <button type="button" class="btn-secondary" onclick="closeModal()">Cancel</button>
                    <button type="submit" class="btn-primary">Save Measurement</button>
                </div>
            </form>
        </div>
    </div>

    <script>
        let appData = { projects: [], contractors: [], items: [], measurements: [], bills: [] };

        async function loadData() {
            try {
                const res = await fetch('/api/data');
                appData = await res.json();
                renderAll();
            } catch (e) {
                console.error(e);
            }
        }

        function renderAll() {
            renderStats();
            populateDropdowns();
            renderMeasurementsTable();
            renderSummaryTable();
            renderRatesTable();
            renderBillsTable();
        }

        function renderStats() {
            document.getElementById('stat-count').innerText = appData.measurements.length;
            const totalAmt = appData.measurements.reduce((sum, m) => sum + (m.amount || 0), 0);
            document.getElementById('stat-amount').innerText = '₹' + totalAmt.toLocaleString('en-IN', { maximumFractionDigits: 2 });
            document.getElementById('stat-meta').innerText = appData.projects.length + ' / ' + appData.contractors.length;
            document.getElementById('stat-bills').innerText = appData.bills.length;
        }

        function populateDropdowns() {
            const pSel = document.getElementById('filter-project');
            const cSel = document.getElementById('filter-contractor');
            const fpSel = document.getElementById('form-project');
            const fcSel = document.getElementById('form-contractor');
            const fiSel = document.getElementById('form-item');

            const currP = pSel.value;
            const currC = cSel.value;

            pSel.innerHTML = '<option value="all">All Projects</option>' + appData.projects.map(p => '<option value="' + p.id + '">' + p.name + '</option>').join('');
            cSel.innerHTML = '<option value="all">All Contractors</option>' + appData.contractors.map(c => '<option value="' + c.id + '">' + c.name + '</option>').join('');
            fpSel.innerHTML = appData.projects.map(p => '<option value="' + p.id + '">' + p.name + '</option>').join('');
            fcSel.innerHTML = appData.contractors.map(c => '<option value="' + c.id + '">' + c.name + '</option>').join('');
            fiSel.innerHTML = appData.items.map(i => '<option value="' + i.id + '">' + i.name + ' (' + i.unit + ')</option>').join('');

            if (currP) pSel.value = currP;
            if (currC) cSel.value = currC;
        }

        function renderMeasurementsTable() {
            const pVal = document.getElementById('filter-project').value;
            const cVal = document.getElementById('filter-contractor').value;
            const q = document.getElementById('search-input').value.toLowerCase();

            const tbody = document.getElementById('measurements-tbody');
            const filtered = appData.measurements.filter(m => {
                if (pVal !== 'all' && m.projectId != pVal) return false;
                if (cVal !== 'all' && m.contractorId != cVal) return false;
                if (q) {
                    const str = (m.itemName + ' ' + m.contractorName + ' ' + m.floor + ' ' + m.location + ' ' + m.remarks).toLowerCase();
                    if (!str.includes(q)) return false;
                }
                return true;
            });

            if (filtered.length === 0) {
                tbody.innerHTML = '<tr><td colspan="9" style="text-align: center; color: var(--text-muted); padding: 2rem;">No measurements found.</td></tr>';
                return;
            }

            tbody.innerHTML = filtered.map(m => {
                let dim = '';
                if (m.calcType === 'RUNNING_LENGTH') dim = 'L: ' + m.length + ' × ' + m.nos;
                else if (m.calcType === 'AREA') dim = 'L: ' + m.length + ' × W: ' + m.width + ' × ' + m.nos;
                else if (m.calcType === 'WALL_PLASTER') dim = 'L: ' + m.length + ' × H: ' + m.height + ' × ' + m.nos + (m.deduction > 0 ? ' (-' + m.deduction + ')' : '');
                else if (m.calcType === 'VOLUME') dim = 'L: ' + m.length + ' × W: ' + m.width + ' × H: ' + m.height + ' × ' + m.nos;
                else dim = 'Nos: ' + m.nos;

                const loc = [m.floor, m.location].filter(Boolean).join(' - ') || '-';

                return '<tr>' +
                    '<td>' + m.dateFormatted + '</td>' +
                    '<td><strong>' + m.contractorName + '</strong></td>' +
                    '<td>' + m.itemName + '</td>' +
                    '<td><code style="background:#f1f5f9; padding: 2px 6px; border-radius: 4px;">' + dim + '</code></td>' +
                    '<td><span class="badge badge-qty">' + m.quantity.toFixed(2) + ' ' + m.unit + '</span></td>' +
                    '<td>₹' + m.rate + '</td>' +
                    '<td><span class="badge badge-amt">₹' + m.amount.toLocaleString('en-IN') + '</span></td>' +
                    '<td>' + loc + '</td>' +
                    '<td>' +
                        '<button class="btn-secondary" style="padding: 4px 8px; font-size: 11px;" onclick="editMeasure(' + m.id + ')">Edit</button> ' +
                        '<button class="btn-secondary" style="padding: 4px 8px; font-size: 11px; color: var(--danger);" onclick="deleteMeasure(' + m.id + ')">Del</button>' +
                    '</td>' +
                '</tr>';
            }).join('');
        }

        function renderSummaryTable() {
            const tbody = document.getElementById('summary-tbody');
            const map = {};

            appData.measurements.forEach(m => {
                const key = m.contractorName + '||' + m.itemName + '||' + m.unit;
                if (!map[key]) {
                    map[key] = { contractor: m.contractorName, item: m.itemName, unit: m.unit, count: 0, totalQty: 0, totalAmt: 0, rate: m.rate };
                }
                map[key].count++;
                map[key].totalQty += m.quantity;
                map[key].totalAmt += m.amount;
            });

            const rows = Object.values(map);
            if (rows.length === 0) {
                tbody.innerHTML = '<tr><td colspan="7" style="text-align: center; color: var(--text-muted); padding: 1.5rem;">No summary data available.</td></tr>';
                return;
            }

            tbody.innerHTML = rows.map(r => '<tr>' +
                '<td><strong>' + r.contractor + '</strong></td>' +
                '<td>' + r.item + '</td>' +
                '<td>' + r.unit + '</td>' +
                '<td>' + r.count + '</td>' +
                '<td><strong>' + r.totalQty.toFixed(2) + ' ' + r.unit + '</strong></td>' +
                '<td>₹' + r.rate + '</td>' +
                '<td><span class="badge badge-amt">₹' + r.totalAmt.toLocaleString('en-IN', { maximumFractionDigits: 2 }) + '</span></td>' +
            '</tr>').join('');
        }

        function renderRatesTable() {
            const tbody = document.getElementById('rates-tbody');
            if (appData.contractors.length === 0 || appData.items.length === 0) {
                tbody.innerHTML = '<tr><td colspan="5" style="text-align: center; color: var(--text-muted); padding: 1.5rem;">No contractor rates defined yet.</td></tr>';
                return;
            }

            let html = '';
            appData.contractors.forEach(c => {
                appData.items.forEach(i => {
                    html += '<tr>' +
                        '<td><strong>' + c.name + '</strong></td>' +
                        '<td>' + i.name + '</td>' +
                        '<td>' + i.unit + '</td>' +
                        '<td>' + i.calcType + '</td>' +
                        '<td><strong>₹' + i.defaultRate + '</strong></td>' +
                    '</tr>';
                });
            });
            tbody.innerHTML = html;
        }

        function renderBillsTable() {
            const tbody = document.getElementById('bills-tbody');
            if (appData.bills.length === 0) {
                tbody.innerHTML = '<tr><td colspan="8" style="text-align: center; color: var(--text-muted); padding: 1.5rem;">No bills created yet.</td></tr>';
                return;
            }

            tbody.innerHTML = appData.bills.map(b => '<tr>' +
                '<td><strong>' + b.billNumber + '</strong></td>' +
                '<td>' + b.dateFormatted + '</td>' +
                '<td>' + b.projectName + '</td>' +
                '<td>' + b.contractorName + '</td>' +
                '<td>' + b.totalQuantity.toFixed(2) + '</td>' +
                '<td>₹' + b.totalAmount.toLocaleString('en-IN') + '</td>' +
                '<td><strong style="color: var(--primary);">₹' + b.netAmount.toLocaleString('en-IN') + '</strong></td>' +
                '<td>' + (b.notes || '-') + '</td>' +
            '</tr>').join('');
        }

        function switchTab(tabId) {
            document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
            document.querySelectorAll('.tab-content').forEach(c => c.style.display = 'none');
            event.target.classList.add('active');
            document.getElementById('tab-' + tabId).style.display = 'block';
        }

        function applyFilters() {
            renderMeasurementsTable();
        }

        function onItemSelectionChange() {
            const itemId = document.getElementById('form-item').value;
            const item = appData.items.find(i => i.id == itemId);
            if (!item) return;

            document.getElementById('form-rate').value = item.defaultRate || 0;

            const calcType = item.calcType;
            const gLen = document.getElementById('group-length');
            const gWid = document.getElementById('group-width');
            const gHei = document.getElementById('group-height');
            const gNos = document.getElementById('group-nos');
            const gDed = document.getElementById('group-deduction');

            gLen.style.display = (calcType === 'NOS') ? 'none' : 'block';
            gWid.style.display = (calcType === 'AREA' || calcType === 'VOLUME') ? 'block' : 'none';
            gHei.style.display = (calcType === 'WALL_PLASTER' || calcType === 'VOLUME') ? 'block' : 'none';
            gNos.style.display = 'block';
            gDed.style.display = (calcType === 'WALL_PLASTER') ? 'block' : 'none';

            calcPreview();
        }

        function onContractorOrItemChange() {
            onItemSelectionChange();
        }

        function calcPreview() {
            const itemId = document.getElementById('form-item').value;
            const item = appData.items.find(i => i.id == itemId);
            if (!item) return;

            const l = parseFloat(document.getElementById('form-length').value) || 0;
            const w = parseFloat(document.getElementById('form-width').value) || 0;
            const h = parseFloat(document.getElementById('form-height').value) || 0;
            const n = parseFloat(document.getElementById('form-nos').value) || 1;
            const d = parseFloat(document.getElementById('form-deduction').value) || 0;
            const rate = parseFloat(document.getElementById('form-rate').value) || 0;

            let qty = 0;
            if (item.calcType === 'RUNNING_LENGTH') qty = l * n;
            else if (item.calcType === 'AREA') qty = l * w * n;
            else if (item.calcType === 'WALL_PLASTER') qty = (l * h * n) - d;
            else if (item.calcType === 'VOLUME') qty = l * w * h * n;
            else qty = n;

            const amt = qty * rate;
            document.getElementById('prev-qty').innerText = qty.toFixed(2) + ' ' + item.unit;
            document.getElementById('prev-amt').innerText = '₹' + amt.toLocaleString('en-IN', { maximumFractionDigits: 2 });
        }

        function openNewModal() {
            document.getElementById('modal-title').innerText = 'New Site Measurement';
            document.getElementById('form-id').value = '0';
            document.getElementById('form-length').value = '';
            document.getElementById('form-width').value = '';
            document.getElementById('form-height').value = '';
            document.getElementById('form-nos').value = '1';
            document.getElementById('form-deduction').value = '0';
            document.getElementById('form-floor').value = '';
            document.getElementById('form-location').value = '';
            document.getElementById('form-remarks').value = '';
            onItemSelectionChange();
            document.getElementById('entry-modal').classList.add('open');
        }

        function editMeasure(id) {
            const m = appData.measurements.find(x => x.id == id);
            if (!m) return;
            document.getElementById('modal-title').innerText = 'Edit Measurement #' + m.id;
            document.getElementById('form-id').value = m.id;
            document.getElementById('form-project').value = m.projectId;
            document.getElementById('form-contractor').value = m.contractorId;
            document.getElementById('form-item').value = m.itemId;
            onItemSelectionChange();
            document.getElementById('form-rate').value = m.rate;
            document.getElementById('form-length').value = m.length;
            document.getElementById('form-width').value = m.width;
            document.getElementById('form-height').value = m.height;
            document.getElementById('form-nos').value = m.nos;
            document.getElementById('form-deduction').value = m.deduction;
            document.getElementById('form-floor').value = m.floor;
            document.getElementById('form-location').value = m.location;
            document.getElementById('form-remarks').value = m.remarks;
            calcPreview();
            document.getElementById('entry-modal').classList.add('open');
        }

        function closeModal() {
            document.getElementById('entry-modal').classList.remove('open');
        }

        async function saveMeasurement(e) {
            e.preventDefault();
            const pId = parseInt(document.getElementById('form-project').value);
            const cId = parseInt(document.getElementById('form-contractor').value);
            const iId = parseInt(document.getElementById('form-item').value);
            const proj = appData.projects.find(p => p.id == pId);
            const cont = appData.contractors.find(c => c.id == cId);
            const item = appData.items.find(i => i.id == iId);

            const payload = {
                id: parseInt(document.getElementById('form-id').value) || 0,
                projectId: pId,
                contractorId: cId,
                contractorName: cont ? cont.name : '',
                itemId: iId,
                itemName: item ? item.name : '',
                unit: item ? item.unit : 'm²',
                calcType: item ? item.calcType : 'AREA',
                length: parseFloat(document.getElementById('form-length').value) || 0,
                width: parseFloat(document.getElementById('form-width').value) || 0,
                height: parseFloat(document.getElementById('form-height').value) || 0,
                nos: parseFloat(document.getElementById('form-nos').value) || 1,
                deduction: parseFloat(document.getElementById('form-deduction').value) || 0,
                rate: parseFloat(document.getElementById('form-rate').value) || 0,
                floor: document.getElementById('form-floor').value,
                location: document.getElementById('form-location').value,
                remarks: document.getElementById('form-remarks').value
            };

            await fetch('/api/measurement', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            closeModal();
            loadData();
        }

        async function deleteMeasure(id) {
            if (!confirm('Are you sure you want to delete measurement #' + id + '?')) return;
            await fetch('/api/measurement?id=' + id, { method: 'DELETE' });
            loadData();
        }

        function exportCsv() {
            window.location.href = '/api/export/csv';
        }

        loadData();
    </script>
</body>
</html>
        """.trimIndent()
    }
}
