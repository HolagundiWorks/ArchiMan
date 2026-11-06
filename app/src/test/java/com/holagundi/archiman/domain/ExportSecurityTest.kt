package com.holagundi.archiman.domain
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
class ExportSecurityTest {
    @Test fun csvNeutralisesFormulaPrefixesAndBom() {
        val result=DocumentExports.csv(listOf(listOf("=1+1","\t+SUM(1,2)","\uFEFF=1+1","-123.00"))).toString(Charsets.UTF_8)
        assertTrue(result.contains("'=1+1"));assertTrue(result.contains("'\t+SUM"));assertTrue(result.contains("'\uFEFF=1+1"));assertFalse(result.contains("'-123.00"))
    }
    @Test fun xlsxContainsNoExecutableFormulasAndHasValidUnicodeXml() {
        val data=DocumentExports.xlsx(listOf(listOf("=HYPERLINK(\"https://evil.invalid\")","nul\u0000invalid\uFFFFemoji \uD83D\uDE00")))
        ZipInputStream(ByteArrayInputStream(data)).use { zip -> while(true) { val entry=zip.nextEntry ?: break;val bytes=zip.readBytes();DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(ByteArrayInputStream(bytes));if(entry.name.endsWith("sheet1.xml")){val xml=bytes.toString(Charsets.UTF_8);assertFalse(xml.contains("<f>"));assertTrue(xml.contains("\uD83D\uDE00"))} } }
    }
    @Test fun allCsvPathsUseTheSameCellSafety() {
        assertEquals("\"'=1+1\"",CsvCell.quote("=1+1"))
        assertEquals("\"-123.00\"",CsvCell.quote("-123.00"))
        assertEquals("\"'\uFEFF=1+1\"",CsvCell.quote("\uFEFF=1+1"))
    }
}
