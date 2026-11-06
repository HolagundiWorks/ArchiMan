package com.holagundi.archiman.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.security.MessageDigest

enum class GstMode { NONE, COMPOSITION, REGULAR }
data class TaxBreakdown(val base: BigDecimal, val cgst: BigDecimal, val sgst: BigDecimal, val igst: BigDecimal) {
    val total get() = base + cgst + sgst + igst
}
object FinanceMath {
    private val moneyPattern=Regex("[0-9]{1,12}(\\.[0-9]{1,6})?")
    fun decimal(text: String): BigDecimal {
        val clean=text.trim()
        require(clean.matches(moneyPattern)) { "Enter a plain decimal amount, up to 12 whole-number digits and six decimal places." }
        return clean.toBigDecimal().setScale(2,RoundingMode.HALF_UP).also { require(it < BigDecimal("100000000000")) { "Invalid amount." } }
    }
    fun tax(base: String, mode: GstMode, rate: String, supplier: String, place: String): TaxBreakdown {
        val amount = decimal(base); val zero = BigDecimal("0.00")
        if (mode != GstMode.REGULAR) return TaxBreakdown(amount, zero, zero, zero)
        require(supplier.matches(Regex("[0-9]{2}")) && place.matches(Regex("[0-9]{2}"))) { "Enter two-digit supplier and place-of-supply state codes." }
        val percent = decimal(rate); require(percent <= BigDecimal("100")) { "Invalid GST rate." }
        val tax = (amount * percent / BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)
        val half = (tax / BigDecimal("2")).setScale(2, RoundingMode.HALF_UP)
        return if (supplier == place) TaxBreakdown(amount, half, tax - half, zero) else TaxBreakdown(amount, zero, zero, tax)
    }
}
data class BankRow(val date: String, val description: String, val reference: String, val amount: BigDecimal, val key: String)
object BankCsv {
    private val numberPattern=Regex("-?[0-9]{1,12}(\\.[0-9]{1,6})?")
    fun cells(text: String): List<List<String>> {
        require(text.length <= 5_000_000) { "Statement exceeds 5 MB." }
        val rows = mutableListOf<List<String>>(); val row = mutableListOf<String>(); val field = StringBuilder()
        val delimiter = if (text.lineSequence().take(10).sumOf { it.count { c -> c == '\t' } } > 2) '\t' else ','
        var quoted = false; var i = 0
        while (i < text.length) {
            val c = text[i++]
            when {
                c == '"' && quoted && i < text.length && text[i] == '"' -> { field.append('"'); i++ }
                c == '"' -> quoted = !quoted
                !quoted && c == delimiter -> { row.add(field.toString().trim()); field.clear() }
                !quoted && c == '\n' -> { row.add(field.toString().trim()); field.clear(); rows.add(row.toList()); row.clear() }
                c != '\r' -> field.append(c)
            }
        }
        require(!quoted) { "Unclosed quoted CSV field." }
        if (field.isNotEmpty() || row.isNotEmpty()) { row.add(field.toString().trim()); rows.add(row) }
        require(rows.size <= 10020) { "Maximum 10,000 transactions." }
        return rows.filter { r -> r.any { it.isNotBlank() } }
    }
    fun parse(text: String, account: String, monthFirst: Boolean = false): List<BankRow> {
        require(account.isNotBlank() && account.length<=120) { "Enter a stable account alias for duplicate detection." }
        fun norm(s: String) = s.removePrefix("\uFEFF").lowercase().replace(Regex("[^a-z0-9]"), "")
        val rows = cells(text)
        val dateNames = setOf("date", "transactiondate", "txndate", "valuedate", "postingdate")
        val header = rows.indexOfFirst { row -> row.any { norm(it) in dateNames } && row.any { norm(it) in setOf("amount", "credit", "creditamount", "depositamt", "depositamount", "debit", "withdrawalamt", "withdrawalamount") } }
        require(header >= 0) { "CSV needs Date and Amount, or Credit/Debit columns." }
        val names = rows[header].map(::norm)
        fun col(vararg aliases: String) = names.indexOfFirst { it in aliases }
        val dateCol = names.indexOfFirst { it in dateNames }; val amountCol = col("amount", "transactionamount")
        val creditCol = col("credit", "creditamount", "depositamt", "depositamount"); val debitCol = col("debit", "debitamount", "withdrawalamt", "withdrawalamount")
        val descCol = col("description", "narration", "particulars", "transactionremarks", "remarks")
        val refCol = col("reference", "referenceno", "refno", "utr", "chequerefno", "chqrefnumber")
        val digester=MessageDigest.getInstance("SHA-256")
        val typeCol = col("drcr", "type", "transactiontype"); val occurrences = mutableMapOf<String, Int>()
        fun number(s: String): BigDecimal {
            if (s.isBlank() || s == "-") return BigDecimal.ZERO
            val clean=s.uppercase().replace("INR", "").replace("\u20B9", "").replace(",", "").replace("CR", "").replace("DR", "").trim().replace("(", "-").replace(")", "")
            require(clean.matches(numberPattern)) { "Invalid bank amount: ${s.take(80)}" }
            return clean.toBigDecimal().setScale(2,RoundingMode.HALF_UP)
        }
        fun formatter(pattern:String)=java.time.format.DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(pattern).toFormatter(java.util.Locale.ENGLISH).withResolverStyle(ResolverStyle.STRICT)
        val isoDate=formatter("uuuu/M/d")
        val numericDate=formatter(if(monthFirst) "M/d/uuuu" else "d/M/uuuu")
        val namedMonthDate=formatter("d/MMM/uuuu")
        return rows.drop(header + 1).mapIndexed { index, row ->
            try {
                fun at(i: Int) = row.getOrNull(i).orEmpty()
                val raw = at(dateCol).substringBefore(' ').replace('.', '/').replace('-', '/')
                val datePattern=when { raw.substringBefore('/').length==4 -> isoDate; raw.any { it.isLetter() } -> namedMonthDate; else -> numericDate }
                val date=runCatching { LocalDate.parse(raw,datePattern) }.getOrNull() ?: error("Invalid date: ${raw.take(80)}")
                var amount = if (amountCol >= 0) number(at(amountCol)) else number(at(creditCol)).abs() - number(at(debitCol)).abs()
                if (amountCol >= 0 && (at(typeCol).uppercase() in listOf("DR", "DEBIT") || at(amountCol).uppercase().endsWith("DR"))) amount = -amount.abs()
                require(amount.signum() != 0) { "Zero transaction amount." }
                val description = at(descCol); val reference = at(refCol)
                val signature = listOf(account.trim().lowercase(), date.toString(), amount.toPlainString(), reference.ifBlank { description }.trim().lowercase()).joinToString("|")
                val occurrence = occurrences.getOrDefault(signature, 0); occurrences[signature] = occurrence + 1
                val key = Hex.encode(digester.digest("$signature|$occurrence".toByteArray()))
                BankRow(date.toString(), description, reference, amount, key)
            } catch (error: Exception) { throw IllegalArgumentException("Statement row ${header + index + 2}: ${error.message?.take(240)}") }
        }
    }
}
