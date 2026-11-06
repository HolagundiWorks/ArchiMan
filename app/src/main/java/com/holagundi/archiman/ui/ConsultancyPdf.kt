package com.holagundi.archiman.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.OutputStream
import java.util.UUID

object ConsultancyPdf {
    /** A4 text export with measured wrapping and a footer on every page. */
    fun write(output: OutputStream, title: String, text: String) {
        val document = PdfDocument()
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f; color = Color.rgb(22, 22, 22) }
        val heading = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 14f; color = Color.rgb(15, 98, 254); isFakeBoldText = true }
        val footer = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9f; color = Color.DKGRAY }
        var pageNumber = 0
        var page: PdfDocument.Page? = null
        var y = 0f
        fun finishPage() {
            page?.let {
                it.canvas.drawText("ArchiMan · Page $pageNumber", 40f, 815f, footer)
                document.finishPage(it)
            }
            page = null
        }
        fun nextPage() {
            finishPage()
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
            val count = heading.breakText(title, true, 515f, null)
            page!!.canvas.drawText(title.take(count), 40f, 40f, heading)
            y = 70f
        }
        try {
            nextPage()
            text.lines().forEach { paragraph ->
                var remaining = paragraph
                if (remaining.isEmpty()) {
                    if (y > 785f) nextPage()
                    y += 10f
                }
                while (remaining.isNotEmpty()) {
                    if (y > 785f) nextPage()
                    var count = body.breakText(remaining, true, 515f, null).coerceAtLeast(1)
                    if (count < remaining.length) {
                        val space = remaining.lastIndexOf(' ', count - 1)
                        if (space > 0) count = space
                    }
                    page!!.canvas.drawText(remaining.take(count), 40f, y, body)
                    remaining = remaining.drop(count).trimStart()
                    y += 16f
                }
            }
            finishPage()
            document.writeTo(output)
        } finally { document.close() }
    }
}

fun shareConsultancyPdf(context: Context, title: String, text: String, company: com.holagundi.archiman.data.local.entity.CompanyProfileEntity? = null, style: org.json.JSONObject = org.json.JSONObject()) {
    try {
        val directory = File(context.cacheDir, "exports").apply { check(mkdirs() || isDirectory) }
        val file = File(directory, "${title.take(60).replace(Regex("[^A-Za-z0-9._-]"), "_")}-${UUID.randomUUID()}.pdf")
        file.outputStream().use { if (company == null) ConsultancyPdf.write(it, title, text) else it.write(com.holagundi.archiman.domain.DocumentExports.pdf(company, style, title, text)) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"; putExtra(Intent.EXTRA_STREAM, uri); putExtra(Intent.EXTRA_SUBJECT, title)
            clipData = ClipData.newRawUri("Consultancy document", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share consultancy PDF"))
    } catch (_: Exception) { Toast.makeText(context, "Could not export the document. Please try again.", Toast.LENGTH_LONG).show() }
}
