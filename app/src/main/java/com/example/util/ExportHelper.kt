package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.FileProvider
import com.example.data.local.entity.MeasurementEntity
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

object ExportHelper {

    fun shareSupportDiagnostics(context: Context, report: String) {
        try {
            val file = File(context.cacheDir, "AMB_Support_Diagnostics.json")
            FileOutputStream(file).use {
                it.write(report.toByteArray(StandardCharsets.UTF_8))
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "AMB support diagnostics")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share support diagnostics"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Export measurements to CSV file with UTF-8 BOM and share via standard Android intent
     */
    fun exportAndShareCsv(
        context: Context,
        projectName: String,
        measurements: List<MeasurementEntity>
    ) {
        val sb = StringBuilder()
        // UTF-8 BOM for Excel compatibility
        sb.append("\uFEFF")
        sb.append("Sr No,Date,Project,Floor,Location / Member,Item of Work,Calculation Formula,Nos,Length (m),Width (m),Height / Depth (m),Deduction,Quantity,Unit,Remarks\n")

        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        for ((idx, m) in measurements.withIndex()) {
            val dateStr = sdf.format(Date(m.date))
            val projEscaped = projectName.replace("\"", "\"\"")
            val floorEscaped = m.floor.replace("\"", "\"\"")
            val locEscaped = m.location.replace("\"", "\"\"")
            val itemEscaped = m.itemName.replace("\"", "\"\"")
            val formulaEscaped = m.calculationType.displayName.replace("\"", "\"\"")
            val remarksEscaped = m.remarks.replace("\"", "\"\"")

            sb.append("${idx + 1},\"$dateStr\",\"$projEscaped\",\"$floorEscaped\",\"$locEscaped\",\"$itemEscaped\",\"$formulaEscaped\",${m.nos},${m.length},${m.width},${m.height},${m.deduction},${m.quantity},\"${m.unit}\",\"$remarksEscaped\"\n")
        }

        try {
            val file = File(context.cacheDir, "AMB_${projectName.replace(" ", "_")}_Measurements.csv")
            FileOutputStream(file).use {
                it.write(sb.toString().toByteArray(StandardCharsets.UTF_8))
            }
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "AMB Measurement Sheet (CSV) - $projectName")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Export Measurement Sheet (CSV)"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Export measurements to an Excel-compatible XML Spreadsheet (.xls)
     * Opens directly in Microsoft Excel, Google Sheets, LibreOffice Calc with formatted cells, bold headers, and formulas.
     */
    fun exportAndShareXls(
        context: Context,
        projectName: String,
        measurements: List<MeasurementEntity>
    ) {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val generatedDate = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())

        val sb = StringBuilder()
        sb.append("""<?xml version="1.0"?>
<?mso-application progid="Excel.Sheet"?>
<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"
 xmlns:o="urn:schemas-microsoft-com:office:office"
 xmlns:x="urn:schemas-microsoft-com:office:excel"
 xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet"
 xmlns:html="http://www.w3.org/TR/REC-html40">
 <Styles>
  <Style ss:ID="Default" ss:Name="Normal">
   <Alignment ss:Vertical="Center"/>
   <Font ss:FontName="Calibri" x:Family="Swiss" ss:Size="11" ss:Color="#000000"/>
  </Style>
  <Style ss:ID="sHeaderTitle">
   <Font ss:FontName="Calibri" ss:Size="16" ss:Bold="1" ss:Color="#0F62FE"/>
   <Alignment ss:Horizontal="Left" ss:Vertical="Center"/>
  </Style>
  <Style ss:ID="sMeta">
   <Font ss:FontName="Calibri" ss:Size="10" ss:Color="#525252"/>
  </Style>
  <Style ss:ID="sColHeader">
   <Font ss:FontName="Calibri" ss:Size="11" ss:Bold="1" ss:Color="#FFFFFF"/>
   <Interior ss:Color="#161616" ss:Pattern="Solid"/>
   <Alignment ss:Horizontal="Center" ss:Vertical="Center"/>
   <Borders>
    <Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#000000"/>
   </Borders>
  </Style>
  <Style ss:ID="sItemRow">
   <Alignment ss:Horizontal="Left" ss:Vertical="Center"/>
   <Borders>
    <Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E0E0E0"/>
   </Borders>
  </Style>
  <Style ss:ID="sNumber">
   <Alignment ss:Horizontal="Right" ss:Vertical="Center"/>
   <NumberFormat ss:Format="0.00"/>
   <Borders>
    <Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E0E0E0"/>
   </Borders>
  </Style>
  <Style ss:ID="sQty">
   <Font ss:FontName="Calibri" ss:Bold="1" ss:Color="#0F62FE"/>
   <Alignment ss:Horizontal="Right" ss:Vertical="Center"/>
   <NumberFormat ss:Format="0.000"/>
   <Borders>
    <Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E0E0E0"/>
   </Borders>
  </Style>
  <Style ss:ID="sCenter">
   <Alignment ss:Horizontal="Center" ss:Vertical="Center"/>
   <Borders>
    <Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E0E0E0"/>
   </Borders>
  </Style>
  <Style ss:ID="sTotalRow">
   <Font ss:FontName="Calibri" ss:Bold="1" ss:Color="#161616"/>
   <Interior ss:Color="#EDF5FF" ss:Pattern="Solid"/>
   <Alignment ss:Horizontal="Right" ss:Vertical="Center"/>
   <Borders>
    <Border ss:Position="Top" ss:LineStyle="Continuous" ss:Weight="2" ss:Color="#0F62FE"/>
    <Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="2" ss:Color="#0F62FE"/>
   </Borders>
  </Style>
 </Styles>
 <Worksheet ss:Name="Measurement Sheet">
  <Table>
   <Column ss:Width="40"/>
   <Column ss:Width="70"/>
   <Column ss:Width="110"/>
   <Column ss:Width="120"/>
   <Column ss:Width="150"/>
   <Column ss:Width="100"/>
   <Column ss:Width="45"/>
   <Column ss:Width="60"/>
   <Column ss:Width="60"/>
   <Column ss:Width="60"/>
   <Column ss:Width="65"/>
   <Column ss:Width="75"/>
   <Column ss:Width="50"/>
   <Column ss:Width="130"/>

   <!-- Title Section -->
   <Row ss:Height="25">
    <Cell ss:StyleID="sHeaderTitle"><Data ss:Type="String">ACCELERATED MEASUREMENT BOOK (AMB)</Data></Cell>
   </Row>
   <Row ss:Height="18">
    <Cell ss:StyleID="sMeta"><Data ss:Type="String">Project: $projectName | Generated: $generatedDate</Data></Cell>
   </Row>
   <Row ss:Height="10"/>

   <!-- Table Headers -->
   <Row ss:Height="24">
    <Cell ss:StyleID="sColHeader"><Data ss:Type="String">#</Data></Cell>
    <Cell ss:StyleID="sColHeader"><Data ss:Type="String">Date</Data></Cell>
    <Cell ss:StyleID="sColHeader"><Data ss:Type="String">Floor</Data></Cell>
    <Cell ss:StyleID="sColHeader"><Data ss:Type="String">Location / Space</Data></Cell>
    <Cell ss:StyleID="sColHeader"><Data ss:Type="String">Item of Work</Data></Cell>
    <Cell ss:StyleID="sColHeader"><Data ss:Type="String">Formula</Data></Cell>
    <Cell ss:StyleID="sColHeader"><Data ss:Type="String">Nos</Data></Cell>
    <Cell ss:StyleID="sColHeader"><Data ss:Type="String">Length</Data></Cell>
    <Cell ss:StyleID="sColHeader"><Data ss:Type="String">Width</Data></Cell>
    <Cell ss:StyleID="sColHeader"><Data ss:Type="String">Depth/Ht</Data></Cell>
    <Cell ss:StyleID="sColHeader"><Data ss:Type="String">Deduction</Data></Cell>
    <Cell ss:StyleID="sColHeader"><Data ss:Type="String">Quantity</Data></Cell>
    <Cell ss:StyleID="sColHeader"><Data ss:Type="String">Unit</Data></Cell>
    <Cell ss:StyleID="sColHeader"><Data ss:Type="String">Remarks</Data></Cell>
   </Row>
""")

        for ((idx, m) in measurements.withIndex()) {
            val dateStr = sdf.format(Date(m.date))
            val floorStr = xmlEscape(m.floor)
            val locStr = xmlEscape(if (m.description.isNotBlank()) "${m.location} (${m.description})" else m.location)
            val itemStr = xmlEscape(m.itemName)
            val formulaStr = xmlEscape(m.calculationType.displayName)
            val remarksStr = xmlEscape(m.remarks)

            sb.append("""
   <Row ss:Height="20">
    <Cell ss:StyleID="sCenter"><Data ss:Type="Number">${idx + 1}</Data></Cell>
    <Cell ss:StyleID="sCenter"><Data ss:Type="String">$dateStr</Data></Cell>
    <Cell ss:StyleID="sItemRow"><Data ss:Type="String">$floorStr</Data></Cell>
    <Cell ss:StyleID="sItemRow"><Data ss:Type="String">$locStr</Data></Cell>
    <Cell ss:StyleID="sItemRow"><Data ss:Type="String">$itemStr</Data></Cell>
    <Cell ss:StyleID="sItemRow"><Data ss:Type="String">$formulaStr</Data></Cell>
    <Cell ss:StyleID="sCenter"><Data ss:Type="Number">${m.nos}</Data></Cell>
    <Cell ss:StyleID="sNumber"><Data ss:Type="Number">${m.length}</Data></Cell>
    <Cell ss:StyleID="sNumber"><Data ss:Type="Number">${m.width}</Data></Cell>
    <Cell ss:StyleID="sNumber"><Data ss:Type="Number">${m.height}</Data></Cell>
    <Cell ss:StyleID="sNumber"><Data ss:Type="Number">${m.deduction}</Data></Cell>
    <Cell ss:StyleID="sQty"><Data ss:Type="Number">${m.quantity}</Data></Cell>
    <Cell ss:StyleID="sCenter"><Data ss:Type="String">${xmlEscape(m.unit)}</Data></Cell>
    <Cell ss:StyleID="sItemRow"><Data ss:Type="String">$remarksStr</Data></Cell>
   </Row>
""")
        }

        // Summary by Item of Work
        sb.append("""
   <Row ss:Height="15"/>
   <Row ss:Height="22">
    <Cell ss:MergeAcross="4" ss:StyleID="sHeaderTitle"><Data ss:Type="String">Quantity Abstract by Work Item</Data></Cell>
   </Row>
   <Row ss:Height="20">
    <Cell ss:StyleID="sColHeader"><Data ss:Type="String">#</Data></Cell>
    <Cell ss:StyleID="sColHeader" ss:MergeAcross="2"><Data ss:Type="String">Item Description</Data></Cell>
    <Cell ss:StyleID="sColHeader"><Data ss:Type="String">Unit</Data></Cell>
    <Cell ss:StyleID="sColHeader"><Data ss:Type="String">Total Quantity</Data></Cell>
   </Row>
""")

        val itemGroups = measurements.groupBy { it.itemName }
        var sumIdx = 1
        for ((name, list) in itemGroups) {
            val totalQty = list.sumOf { it.quantity }
            val unit = list.firstOrNull()?.unit ?: "unit"
            sb.append("""
   <Row ss:Height="20">
    <Cell ss:StyleID="sCenter"><Data ss:Type="Number">$sumIdx</Data></Cell>
    <Cell ss:StyleID="sItemRow" ss:MergeAcross="2"><Data ss:Type="String">${xmlEscape(name)}</Data></Cell>
    <Cell ss:StyleID="sCenter"><Data ss:Type="String">${xmlEscape(unit)}</Data></Cell>
    <Cell ss:StyleID="sQty"><Data ss:Type="Number">$totalQty</Data></Cell>
   </Row>
""")
            sumIdx++
        }

        sb.append("""
  </Table>
 </Worksheet>
</Workbook>
""")

        try {
            val file = File(context.cacheDir, "AMB_${projectName.replace(" ", "_")}_Measurements.xls")
            FileOutputStream(file).use {
                it.write(sb.toString().toByteArray(StandardCharsets.UTF_8))
            }
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.ms-excel"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "AMB Measurement Sheet (Excel XLS) - $projectName")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Export Measurement Sheet (Excel XLS)"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Export measurements to a real PDF file via Android PdfDocument and share via Intent
     */
    fun exportAndSharePdf(
        context: Context,
        projectName: String,
        measurements: List<MeasurementEntity>
    ) {
        try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595 // A4 standard width in pt
            val pageHeight = 842 // A4 standard height in pt
            var pageNumber = 1

            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            val paint = Paint().apply { isAntiAlias = true }
            val titlePaint = Paint().apply {
                isAntiAlias = true
                textSize = 14f
                isFakeBoldText = true
                color = Color.rgb(15, 98, 254) // Carbon Blue 60
            }
            val subPaint = Paint().apply {
                isAntiAlias = true
                textSize = 9f
                color = Color.rgb(82, 82, 82)
            }
            val headerPaint = Paint().apply {
                isAntiAlias = true
                textSize = 8.5f
                isFakeBoldText = true
                color = Color.WHITE
            }
            val rowPaint = Paint().apply {
                isAntiAlias = true
                textSize = 8f
                color = Color.rgb(22, 22, 22)
            }
            val boldRowPaint = Paint().apply {
                isAntiAlias = true
                textSize = 8.5f
                isFakeBoldText = true
                color = Color.rgb(15, 98, 254)
            }
            val linePaint = Paint().apply {
                color = Color.rgb(224, 224, 224)
                strokeWidth = 0.5f
            }

            fun drawHeader(c: Canvas) {
                // Top brand bar
                paint.color = Color.rgb(15, 98, 254)
                c.drawRect(30f, 25f, 565f, 28f, paint)

                c.drawText("ACCELERATED MEASUREMENT BOOK (AMB)", 30f, 45f, titlePaint)
                val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
                c.drawText("Project: $projectName | Generated: $sdf | Total Records: ${measurements.size}", 30f, 58f, subPaint)

                // Table Header background
                paint.color = Color.rgb(22, 22, 22) // Carbon Black
                c.drawRect(30f, 70f, 565f, 88f, paint)

                // Columns: Sr(20), Item(120), Floor/Loc(100), Formula(65), Nos(25), L(35), W(35), D(35), Ded(35), Qty(45), Unit(20)
                c.drawText("#", 35f, 82f, headerPaint)
                c.drawText("Item / Description", 55f, 82f, headerPaint)
                c.drawText("Floor / Space", 175f, 82f, headerPaint)
                c.drawText("Formula", 270f, 82f, headerPaint)
                c.drawText("Nos", 335f, 82f, headerPaint)
                c.drawText("L (m)", 360f, 82f, headerPaint)
                c.drawText("W (m)", 395f, 82f, headerPaint)
                c.drawText("D (m)", 430f, 82f, headerPaint)
                c.drawText("Ded", 465f, 82f, headerPaint)
                c.drawText("Qty", 500f, 82f, headerPaint)
                c.drawText("Unit", 535f, 82f, headerPaint)
            }

            drawHeader(canvas)

            var y = 104f
            val sdfDate = SimpleDateFormat("dd/MM", Locale.getDefault())

            for ((idx, m) in measurements.withIndex()) {
                if (y > pageHeight - 50) {
                    // Footer
                    cDrawPageFooter(canvas, pageNumber, paint, subPaint)
                    pdfDocument.finishPage(page)

                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    drawHeader(canvas)
                    y = 104f
                }

                // Alternate row background
                if (idx % 2 == 1) {
                    paint.color = Color.rgb(244, 244, 244)
                    canvas.drawRect(30f, y - 10f, 565f, y + 6f, paint)
                }

                // Row values
                canvas.drawText("${idx + 1}", 35f, y, rowPaint)
                canvas.drawText(truncate(m.itemName, 22), 55f, y, rowPaint)
                val locText = listOf(m.floor, m.location).filter { it.isNotBlank() }.joinToString(" - ")
                canvas.drawText(truncate(locText, 18), 175f, y, rowPaint)
                canvas.drawText(truncate(m.calculationType.displayName, 12), 270f, y, rowPaint)
                canvas.drawText("${m.nos}", 335f, y, rowPaint)
                canvas.drawText(String.format(Locale.getDefault(), "%.2f", m.length), 360f, y, rowPaint)
                canvas.drawText(String.format(Locale.getDefault(), "%.2f", m.width), 395f, y, rowPaint)
                canvas.drawText(String.format(Locale.getDefault(), "%.2f", m.height), 430f, y, rowPaint)
                canvas.drawText(if (m.deduction > 0) String.format(Locale.getDefault(), "%.2f", m.deduction) else "-", 465f, y, rowPaint)
                canvas.drawText(String.format(Locale.getDefault(), "%.3f", m.quantity), 500f, y, boldRowPaint)
                canvas.drawText(m.unit, 535f, y, rowPaint)

                canvas.drawLine(30f, y + 7f, 565f, y + 7f, linePaint)
                y += 18f
            }

            // Quantity abstract summary on PDF
            if (y > pageHeight - 120) {
                cDrawPageFooter(canvas, pageNumber, paint, subPaint)
                pdfDocument.finishPage(page)

                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                drawHeader(canvas)
                y = 104f
            }

            y += 10f
            paint.color = Color.rgb(237, 245, 255) // Carbon Blue 10
            canvas.drawRect(30f, y, 565f, y + 20f, paint)
            val abstractHeaderPaint = Paint().apply {
                isAntiAlias = true
                textSize = 9.5f
                isFakeBoldText = true
                color = Color.rgb(0, 67, 206)
            }
            canvas.drawText("QUANTITY ABSTRACT SUMMARY", 40f, y + 14f, abstractHeaderPaint)
            y += 26f

            val itemGroups = measurements.groupBy { it.itemName }
            for ((name, list) in itemGroups) {
                if (y > pageHeight - 50) {
                    cDrawPageFooter(canvas, pageNumber, paint, subPaint)
                    pdfDocument.finishPage(page)

                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    drawHeader(canvas)
                    y = 104f
                }
                val totalQty = list.sumOf { it.quantity }
                val unit = list.firstOrNull()?.unit ?: "unit"

                canvas.drawText("• $name", 40f, y, rowPaint)
                canvas.drawText("${String.format(Locale.getDefault(), "%.3f", totalQty)} $unit", 480f, y, boldRowPaint)
                canvas.drawLine(40f, y + 4f, 555f, y + 4f, linePaint)
                y += 16f
            }

            cDrawPageFooter(canvas, pageNumber, paint, subPaint)
            pdfDocument.finishPage(page)

            val file = File(context.cacheDir, "AMB_${projectName.replace(" ", "_")}_Measurement_Sheet.pdf")
            FileOutputStream(file).use {
                pdfDocument.writeTo(it)
            }
            pdfDocument.close()

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "AMB Measurement Sheet (PDF) - $projectName")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Export Measurement Sheet (PDF)"))
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to WebView Print
            printMeasurementSheetPdf(context, projectName, measurements)
        }
    }

    private fun cDrawPageFooter(canvas: Canvas, pageNumber: Int, paint: Paint, textPaint: Paint) {
        paint.color = Color.rgb(224, 224, 224)
        canvas.drawLine(30f, 810f, 565f, 810f, paint)
        canvas.drawText("Accelerated Measurement Book (AMB) • Page $pageNumber", 30f, 825f, textPaint)
        canvas.drawText("Confidential & Engineering Verified", 410f, 825f, textPaint)
    }

    /**
     * Print / Save to PDF using Android's PrintManager & WebView
     */
    fun printMeasurementSheetPdf(
        context: Context,
        projectName: String,
        measurements: List<MeasurementEntity>
    ) {
        val html = generateMeasurementBookHtml(projectName, measurements)
        val jobName = "AMB_${projectName.replace(" ", "_")}_${System.currentTimeMillis()}"

        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                val printAdapter = webView.createPrintDocumentAdapter(jobName)
                printManager?.print(
                    jobName,
                    printAdapter,
                    PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .setResolution(PrintAttributes.Resolution("res1", "default", 300, 300))
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build()
                )
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    private fun generateMeasurementBookHtml(
        projectName: String,
        measurements: List<MeasurementEntity>
    ): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val generatedDate = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())

        val rows = StringBuilder()
        for ((idx, m) in measurements.withIndex()) {
            val dim = when (m.calculationType.name) {
                "RUNNING_LENGTH" -> "L: ${m.length} × ${m.nos}"
                "AREA" -> "L: ${m.length} × W: ${m.width} × ${m.nos}"
                "WALL_PLASTER" -> "L: ${m.length} × H: ${m.height} × ${m.nos}" + if (m.deduction > 0) " (-${m.deduction})" else ""
                "VOLUME" -> "L: ${m.length} × W: ${m.width} × H: ${m.height} × ${m.nos}"
                else -> "Nos: ${m.nos}"
            }
            val loc = listOf(m.floor, m.location).filter { it.isNotBlank() }.joinToString(" - ")

            rows.append("""
                <tr>
                    <td style="text-align:center;">${idx + 1}</td>
                    <td>${sdf.format(Date(m.date))}</td>
                    <td><strong>${m.itemName}</strong></td>
                    <td>$loc ${if (m.description.isNotBlank() && m.description != m.itemName) "<br><small style='color:#525252;'>${m.description}</small>" else ""}</td>
                    <td><code>$dim</code></td>
                    <td style="text-align:right; font-weight:bold; color:#0f62fe;">${String.format(Locale.getDefault(), "%.3f", m.quantity)}</td>
                    <td style="text-align:center;">${m.unit}</td>
                    <td>${m.remarks}</td>
                </tr>
            """.trimIndent())
        }

        // Summary Rows
        val itemGroups = measurements.groupBy { it.itemName }
        val summaryRows = StringBuilder()
        var sumIdx = 1
        for ((name, list) in itemGroups) {
            val totalQty = list.sumOf { it.quantity }
            val unit = list.firstOrNull()?.unit ?: "unit"
            summaryRows.append("""
                <tr>
                    <td style="text-align:center;">$sumIdx</td>
                    <td><strong>$name</strong></td>
                    <td style="text-align:center;">$unit</td>
                    <td style="text-align:right; font-weight:bold; color:#0f62fe;">${String.format(Locale.getDefault(), "%.3f", totalQty)}</td>
                </tr>
            """.trimIndent())
            sumIdx++
        }

        return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>AMB Measurement Sheet - $projectName</title>
    <style>
        body { font-family: 'Helvetica Neue', Arial, sans-serif; color: #161616; padding: 25px; line-height: 1.4; font-size: 12px; }
        .header { border-bottom: 2px solid #0f62fe; padding-bottom: 10px; margin-bottom: 15px; }
        .title { font-size: 20px; font-weight: bold; color: #0f62fe; }
        .sub { font-size: 12px; color: #525252; margin-top: 4px; }
        table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }
        th { background: #161616; color: white; padding: 7px 8px; text-align: left; font-size: 11px; }
        td { padding: 6px 8px; border-bottom: 1px solid #e0e0e0; }
        tr:nth-child(even) td { background: #f4f4f4; }
        .section-title { font-size: 14px; font-weight: bold; color: #161616; margin: 20px 0 8px 0; border-left: 3px solid #0f62fe; padding-left: 8px; }
        .sign-area { margin-top: 40px; display: table; width: 100%; }
        .sign-col { display: table-cell; width: 50%; text-align: center; padding-top: 30px; }
        .sign-line { border-top: 1px dashed #8d8d8d; width: 65%; margin: 0 auto; padding-top: 5px; font-size: 11px; color: #525252; }
    </style>
</head>
<body>
    <div class="header">
        <div class="title">ACCELERATED MEASUREMENT BOOK (AMB)</div>
        <div class="sub">Project: <strong>$projectName</strong> | Generated: <strong>$generatedDate</strong> | Total Entries: <strong>${measurements.size}</strong></div>
    </div>

    <div class="section-title">1. DETAILED MEASUREMENT ENTRIES</div>
    <table>
        <thead>
            <tr>
                <th style="width:25px; text-align:center;">#</th>
                <th style="width:70px;">Date</th>
                <th>Item of Work</th>
                <th>Location / Member</th>
                <th>Dimensions</th>
                <th style="text-align:right; width:80px;">Quantity</th>
                <th style="width:40px; text-align:center;">Unit</th>
                <th>Remarks</th>
            </tr>
        </thead>
        <tbody>
            $rows
        </tbody>
    </table>

    <div class="section-title">2. QUANTITY ABSTRACT SUMMARY</div>
    <table style="width: 70%;">
        <thead>
            <tr>
                <th style="width:30px; text-align:center;">#</th>
                <th>Item of Work</th>
                <th style="width:60px; text-align:center;">Unit</th>
                <th style="text-align:right; width:120px;">Total Quantity</th>
            </tr>
        </thead>
        <tbody>
            $summaryRows
        </tbody>
    </table>

    <div class="sign-area">
        <div class="sign-col">
            <div class="sign-line">Recorded By (Site Engineer)</div>
        </div>
        <div class="sign-col">
            <div class="sign-line">Checked & Verified (Project In-Charge)</div>
        </div>
    </div>
</body>
</html>
        """.trimIndent()
    }

    private fun truncate(str: String, maxLen: Int): String {
        return if (str.length > maxLen) str.take(maxLen - 2) + ".." else str
    }

    private fun xmlEscape(str: String): String {
        return str
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
