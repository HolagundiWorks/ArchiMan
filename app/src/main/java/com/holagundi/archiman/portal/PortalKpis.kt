package com.holagundi.archiman.portal

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Shared Carbon KPI strips, tables, and India GST/TDS filing reminders for the portal. */
object PortalKpis {
    data class Kpi(val label: String, val value: String, val icon: String = "dashboard", val alert: String = "ok")
    data class FilingDue(val label: String, val dueOn: LocalDate, val hint: String)

    private val dayMonth = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

    fun strip(items: List<Kpi>): String {
        if (items.isEmpty()) return ""
        return """<div class="carbon-kpis carbon-tile-grid carbon-tile-grid--small" role="group" aria-label="Key figures">${items.joinToString("") { kpi ->
            val tone = when (kpi.alert) {
                "critical" -> "carbon-kpi--critical"
                "warning" -> "carbon-kpi--warning"
                "none" -> ""
                else -> "carbon-kpi--ok"
            }
            """<div class="carbon-kpi carbon-tile--small $tone">${CarbonPortalIcons.svg(kpi.icon, "carbon-kpi-icon")}<strong>${PortalHttp.escape(kpi.value)}</strong><span>${PortalHttp.escape(kpi.label)}</span></div>"""
        }}</div>"""
    }

    fun filingStrip(dues: List<FilingDue> = upcomingFilings()): String {
        if (dues.isEmpty()) return ""
        val today = LocalDate.now()
        val cards = dues.joinToString("") { due ->
            val days = java.time.temporal.ChronoUnit.DAYS.between(today, due.dueOn)
            val urgency = when {
                days < 0 -> "portal-filing--overdue"
                days <= 7 -> "portal-filing--soon"
                else -> "portal-filing--ok"
            }
            val whenLabel = when {
                days < 0 -> "${-days} days overdue"
                days == 0L -> "Due today"
                days == 1L -> "Due tomorrow"
                else -> "In $days days"
            }
            """
            <article class="portal-filing $urgency">
              <h3>${PortalHttp.escape(due.label)}</h3>
              <p class="portal-filing-date">${PortalHttp.escape(due.dueOn.format(dayMonth))}</p>
              <p class="portal-filing-meta">${PortalHttp.escape(whenLabel)} · ${PortalHttp.escape(due.hint)}</p>
            </article>
            """.trimIndent()
        }
        return """
        <section class="portal-filing-board" aria-label="GST and TDS filing dates">
          <h2 class="portal-section-title">${CarbonPortalIcons.svg("receipt")}<span>GST &amp; TDS filing dates</span></h2>
          <p class="portal-lead">Indicative statutory dates for a monthly GST filer in India. Confirm the exact due date for your registration and jurisdiction.</p>
          <div class="portal-filing-grid">$cards</div>
        </section>
        """.trimIndent()
    }

    private fun sheetNumeric(header: String) =
        header.matches(Regex("(?i)^(rate|amount|qty|quantity|nos|count|open tasks|value|total|measurements?)$"))

    /** Compact spreadsheet-style data grid used for directories and catalogues. */
    fun dataTable(caption: String, headers: List<String>, rows: List<List<String>>, empty: String = "No rows yet."): String {
        fun wrap(table: String) =
            """<div class="tablewrap portal-sheet-wrap"><div class="portal-sheet-scroll">$table</div></div>"""
        if (rows.isEmpty()) {
            return wrap(
                """<table class="cds--data-table portal-sheet"><caption class="carbon-table-caption">${PortalHttp.escape(caption)}</caption><tbody><tr><td class="portal-sheet-empty">${PortalHttp.escape(empty)}</td></tr></tbody></table>""",
            )
        }
        val numeric = headers.mapIndexed { index, header -> index to sheetNumeric(header) }.toMap()
        val head = headers.mapIndexed { index, header ->
            val cls = if (numeric[index] == true) """ class="portal-sheet-num"""" else ""
            """<th scope="col"$cls>${PortalHttp.escape(header)}</th>"""
        }.joinToString("")
        val body = rows.joinToString("") { row ->
            "<tr>" + row.mapIndexed { index, cell ->
                val cls = if (numeric[index] == true) """ class="portal-sheet-num"""" else ""
                "<td$cls>$cell</td>"
            }.joinToString("") + "</tr>"
        }
        val captionHtml = if (caption.isBlank()) "" else """<caption class="carbon-table-caption">${PortalHttp.escape(caption)}</caption>"""
        return wrap(
            """<table class="cds--data-table portal-sheet">$captionHtml<thead><tr>$head</tr></thead><tbody>$body</tbody></table>""",
        )
    }

    /** Next standard GSTR-1, GSTR-3B, TDS deposit and quarterly TDS return dates. */
    fun upcomingFilings(today: LocalDate = LocalDate.now()): List<FilingDue> {
        val gstr1 = nextDayOfMonth(today, 11).let {
            FilingDue("GSTR-1", it, "Outward supplies for the previous tax period")
        }
        val gstr3b = nextDayOfMonth(today, 20).let {
            FilingDue("GSTR-3B", it, "Monthly summary return and tax payment")
        }
        val tdsDeposit = nextDayOfMonth(today, 7).let {
            FilingDue("TDS deposit", it, "Challan deposit for tax deducted in the previous month")
        }
        val tdsReturn = nextQuarterlyTdsReturn(today).let {
            FilingDue("TDS return (26Q/24Q)", it, "Quarterly statement of tax deducted at source")
        }
        return listOf(gstr1, gstr3b, tdsDeposit, tdsReturn).sortedBy { it.dueOn }
    }

    private fun nextDayOfMonth(today: LocalDate, day: Int): LocalDate {
        val thisMonth = runCatching { today.withDayOfMonth(day) }.getOrElse {
            today.withDayOfMonth(today.lengthOfMonth())
        }
        return if (!thisMonth.isBefore(today)) thisMonth else thisMonth.plusMonths(1).withDayOfMonth(
            day.coerceAtMost(thisMonth.plusMonths(1).lengthOfMonth()),
        )
    }

    private fun nextQuarterlyTdsReturn(today: LocalDate): LocalDate {
        // Due by end of month following quarter: 31 Jul, 31 Oct, 31 Jan, 31 May.
        val candidates = listOf(
            LocalDate.of(today.year, 1, 31),
            LocalDate.of(today.year, 5, 31),
            LocalDate.of(today.year, 7, 31),
            LocalDate.of(today.year, 10, 31),
            LocalDate.of(today.year + 1, 1, 31),
            LocalDate.of(today.year + 1, 5, 31),
        )
        return candidates.first { !it.isBefore(today) }
    }
}
