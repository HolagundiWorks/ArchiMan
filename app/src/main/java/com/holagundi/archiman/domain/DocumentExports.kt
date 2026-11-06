package com.holagundi.archiman.domain

import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.util.Base64
import com.holagundi.archiman.data.local.entity.CompanyProfileEntity
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.json.JSONObject

object DocumentExports {
    private fun bounded(rows: List<List<String>>): List<List<String>> {
        require(rows.size<=20000 && rows.sumOf { it.sumOf(String::length).toLong() }<=8_000_000L) { "Export is too large. Export a smaller project or set of records." }
        return rows
    }
    fun csv(rows:List<List<String>>):ByteArray = ("\uFEFF"+bounded(rows).joinToString("\r\n") { row -> row.joinToString(",",transform=CsvCell::quote) }).toByteArray(Charsets.UTF_8)
    private fun xml(text:String):String {
        val clean=buildString { var offset=0;while(offset<text.length) { val code=Character.codePointAt(text,offset);if(code==9||code==10||code==13||code in 0x20..0xD7FF||code in 0xE000..0xFFFD||code in 0x10000..0x10FFFF) appendCodePoint(code);offset+=Character.charCount(code) } }
        return clean.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;")
    }
    fun xlsx(rows: List<List<String>>): ByteArray {
        bounded(rows)
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            fun entry(name: String, body: String) { zip.putNextEntry(ZipEntry(name)); zip.write(body.toByteArray(Charsets.UTF_8)); zip.closeEntry() }
            entry("[Content_Types].xml", """<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>""")
            entry("_rels/.rels", """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>""")
            entry("xl/workbook.xml", """<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="ArchiMan export" sheetId="1" r:id="rId1"/></sheets></workbook>""")
            entry("xl/_rels/workbook.xml.rels", """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/></Relationships>""")
            entry("xl/worksheets/sheet1.xml", """<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><cols><col min="1" max="20" width="23" customWidth="1"/></cols><sheetData>""" + rows.mapIndexed { index, row -> "<row r=\"${index + 1}\">" + row.joinToString("") { value ->
                if (value.matches(Regex("-?[0-9]+(\\.[0-9]+)?")) && value.length < 15) "<c><v>$value</v></c>" else "<c t=\"inlineStr\"><is><t xml:space=\"preserve\">${xml(value)}</t></is></c>"
            } + "</row>" }.joinToString("") + "</sheetData></worksheet>")
        }
        return output.toByteArray()
    }
    fun identity(company: CompanyProfileEntity): List<String> = listOf(company.legalName.ifBlank { company.practiceName }, listOf(company.address, company.city, company.state, company.pinCode, company.country).filter(String::isNotBlank).joinToString(", "), listOf(company.phone, company.email, company.website).filter(String::isNotBlank).joinToString(" | "), listOf("PAN: ${company.pan}", "GSTIN: ${company.gstin}", "CoA: ${company.coaRegistrationNumber}").joinToString(" | "), listOf(company.principalName, company.principalQualification, company.practiceRegistrationDetails).filter(String::isNotBlank).joinToString(" | "))
    fun pdf(company: CompanyProfileEntity, style: JSONObject, title: String, text: String): ByteArray {
        require(text.length<=1_000_000 && title.length<=2000) { "PDF is too large. Use CSV/Excel or a smaller selection." }
        val document = PdfDocument(); val output = ByteArrayOutputStream()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f; color = Color.rgb(24,24,27) }
        val accent = runCatching { Color.parseColor(style.optString("accent", "#0F62FE")) }.getOrDefault(Color.rgb(15,98,254))
        val logo=style.optString("logo").takeIf { it.startsWith("data:image/") && it.length<=710000 }?.let { value ->
            runCatching {
                val bytes=Base64.decode(value.substringAfter(','),Base64.DEFAULT)
                require(bytes.size<=524288)
                val options=BitmapFactory.Options().apply { inJustDecodeBounds=true }
                BitmapFactory.decodeByteArray(bytes,0,bytes.size,options)
                require(options.outWidth in 1..4000 && options.outHeight in 1..4000 && options.outWidth.toLong()*options.outHeight<=4_000_000)
                options.inJustDecodeBounds=false; options.inSampleSize=1
                while(maxOf(options.outWidth,options.outHeight)/options.inSampleSize>256) options.inSampleSize*=2
                BitmapFactory.decodeByteArray(bytes,0,bytes.size,options)
            }.getOrNull()
        }
        var page: PdfDocument.Page? = null; var number = 0; var y = 0f
        fun finish() { page?.let { paint.textSize=8f; it.canvas.drawText(style.optString("footer").take(95),40f,805f,paint); it.canvas.drawText("Page $number",500f,821f,paint); document.finishPage(it) }; page=null }
        fun line(text: String, x: Float, width: Float, size: Float) { paint.textSize=size; var rest=text; while(rest.isNotEmpty()) { val count=paint.breakText(rest,true,width,null).coerceAtLeast(1); page!!.canvas.drawText(rest.take(count),x,y,paint); rest=rest.drop(count); y += size+4 } }
        fun next() {
            finish(); number++; require(number<=200) { "PDF exceeds 200 pages. Use CSV/Excel or a smaller selection." }; page=document.startPage(PdfDocument.PageInfo.Builder(595,842,number).create()); y=38f
            val left = if (style.optString("layout") == "LOGO_RIGHT") 40f else if(logo != null) 115f else 40f
            val width = if(logo != null) 440f else 515f
            logo?.let { val x=if(style.optString("layout")=="LOGO_RIGHT") 495f else 40f; val scale=minOf(60f/it.width,60f/it.height); page!!.canvas.drawBitmap(it,null,RectF(x,25f,x+it.width*scale,25f+it.height*scale),paint) }
            identity(company).forEachIndexed { i, value -> line(value,left,if(logo!=null) 425f else width,if(i==0) 15f else 8f) }
            if(style.optString("header").isNotBlank()) line(style.optString("header").take(180),40f,515f,9f)
            y += 8; paint.color=accent; page!!.canvas.drawLine(40f,y,555f,y,paint); y+=22; line(title,40f,515f,13f); paint.color=Color.rgb(24,24,27); y+=8
        }
        try {
            next()
            text.lines().forEach { paragraph ->
                var remaining=paragraph
                if(remaining.isEmpty()) y+=9
                while(remaining.isNotEmpty()) { if(y>780) next(); paint.textSize=10f; var count=paint.breakText(remaining,true,515f,null).coerceAtLeast(1); if(count<remaining.length) { val split=remaining.lastIndexOf(' ',count-1); if(split>0) count=split }; page!!.canvas.drawText(remaining.take(count),40f,y,paint); remaining=remaining.drop(count).trimStart(); y+=15 }
            }
            finish(); document.writeTo(output)
        } finally { document.close(); logo?.recycle() }
        return output.toByteArray()
    }
}
