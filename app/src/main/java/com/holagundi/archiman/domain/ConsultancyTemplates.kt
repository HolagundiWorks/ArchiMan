package com.holagundi.archiman.domain

import java.math.BigDecimal
import java.math.RoundingMode

enum class ConsultancyService(val label: String, val sourceUrl: String) {
    ARCHITECTURE("Architectural consultancy", "https://coa.gov.in/index1.php?lang=1&level=2&lid=81&sublinkid=294"),
    INTERIORS("Interior consultancy", "https://coa.gov.in/index1.php?lang=1&level=2&lid=82&sublinkid=295"),
    LANDSCAPE("Landscape consultancy", "https://coa.gov.in/index1.php?lang=1&level=2&lid=83&sublinkid=296"),
    MISCELLANEOUS("Miscellaneous work", ""),
}

enum class ConsultancyFeeBasis(val label: String) {
    LUMP_SUM("Lump sum"), PER_SQ_FT("Per square foot"), PERCENT_OF_COST("Percentage of assigned works cost")
}

data class ConsultancyMilestone(val label: String, val cumulativePercent: String)

data class ConsultancyInput(
    val title: String,
    val service: ConsultancyService,
    val feeBasis: ConsultancyFeeBasis = ConsultancyFeeBasis.LUMP_SUM,
    val rate: String = "",
    val quantity: String = "",
    val areaDefinition: String = "",
    val gstMode: GstMode = GstMode.NONE,
    val gstRate: String = "18",
    val gstin: String = "",
    val supplierState: String = "",
    val placeState: String = "",

    val parties: String = "",
    val scope: String = "",
    val exclusions: String = "",
    val terms: String = "",
    val milestones: List<ConsultancyMilestone> = emptyList(),
    val templateVersion: String = ConsultancyTemplates.VERSION,
    val sourceUrl: String = service.sourceUrl,
    val proposalReference: String = "",
)

object ConsultancyTemplates {
    const val VERSION = "COA-ADAPTED-2026-11-05-v1"
    const val SCALE_URL = "https://coa.gov.in/index1.php?lang=1&level=2&lid=86&sublinkid=299"
    const val REVIEW_NOTE = "Adapted ArchiMan draft; not a CoA-issued or certified form. Review project scope, commercial choices and legal particulars before signing."

    private val commonTerms = """
        1. Client inputs: supply the brief, ownership records, surveys and investigations; approve stages promptly and fund authority charges.
        2. Services: perform the agreed professional scope with reasonable care. Periodic visits do not replace client-appointed day-to-day site supervision or contractor responsibility for execution and safety.
        3. Changes: agree additional scope, revisions, fees and time consequences in writing before proceeding.
        4. Payments: cumulative milestones are fee targets, not additional percentage charges. Account for earlier payments and the agreed advance when invoicing.
        5. Retainer, tax/withholding, documentation charges and reimbursable expenses: [enter amounts, inclusions and adjustment method].
        6. Programme and site visits: [enter deliverable dates, client response periods and visit frequency].
        7. Design ownership remains with the architect; project-use permission and any reuse require agreed licence terms, subject to law.
        8. Substantial default not caused by the terminating party may lead to termination on at least 30 days' written notice; settle earned fees and agree handover.
        9. Disputes: seek written resolution, then use the CoA arbitration mechanism subject to applicable Indian law. Seat/jurisdiction: [enter agreed location and reviewed dispute terms].
    """.trimIndent()

    fun create(service: ConsultancyService, parties: String): ConsultancyInput {
        val scope = when (service) {
            ConsultancyService.ARCHITECTURE -> "Brief and site appraisal; concept and developed design; applicable approval drawings; construction documents and specifications; tender advice; periodic construction review; completion assistance and record drawings. Engineering services and appointed consultants: [define assigned responsibilities]."
            ConsultancyService.INTERIORS -> "Space planning, interior concepts, finishes, furniture and lighting; coordination of assigned engineering services; approval and working drawings; specifications, tender advice, samples/shop-drawing review, periodic site/workshop visits and completion records. Specialist appointments: [define responsibilities]."
            ConsultancyService.LANDSCAPE -> "Site appraisal, grading, drainage, irrigation, hardscape and planting; landscape structures, furniture and lighting; external-service coordination; approval/working drawings, specifications, tender advice, periodic inspection and completion/planting records. Specialist appointments: [define responsibilities]."
            ConsultancyService.MISCELLANEOUS -> ""
        }
        val stages = mutableListOf(
            ConsultancyMilestone("Concept", "10"), ConsultancyMilestone("Developed scheme", "20"),
        )
        if (service == ConsultancyService.ARCHITECTURE) stages.add(ConsultancyMilestone("Approval submission", "30"))
        stages.addAll(listOf(
            ConsultancyMilestone("Approval stage", "35"), ConsultancyMilestone("Detailed documents", "45"),
            ConsultancyMilestone("Tender advice", "55"), ConsultancyMilestone("Construction-start documents", "65"),
            ConsultancyMilestone("Work 20% complete", "70"), ConsultancyMilestone("Work 40% complete", "75"),
            ConsultancyMilestone("Work 60% complete", "80"), ConsultancyMilestone("Work 80% complete", "85"),
            ConsultancyMilestone("Virtual completion", "90"), ConsultancyMilestone("Completion records", "100"),
        ))
        return ConsultancyInput(
            title = "${service.label} fee proposal", service = service, parties = parties, scope = scope,
            exclusions = if (service == ConsultancyService.MISCELLANEOUS) "" else "Only expressly assigned services are included. Resident supervision, specialist appointments, procurement/execution and work outside the stated scope require separate agreement.",
            terms = if (service == ConsultancyService.MISCELLANEOUS) "" else commonTerms,
            milestones = if (service == ConsultancyService.MISCELLANEOUS) listOf(ConsultancyMilestone("Completion", "100")) else stages,
            templateVersion = if (service == ConsultancyService.MISCELLANEOUS) "MANUAL-v1" else VERSION,
        )
    }

    private fun positive(value: String, label: String): BigDecimal {
        val number = value.trim().toBigDecimalOrNull()
        require(number != null && number.signum() > 0 && number.precision() <= 18 && number.scale() in -6..6) { "$label must be a positive decimal with at most six decimal places." }
        return number
    }

    fun amount(input: ConsultancyInput): BigDecimal {
        val rate = positive(input.rate, if (input.feeBasis == ConsultancyFeeBasis.LUMP_SUM) "Fee" else "Rate")
        val raw = when (input.feeBasis) {
            ConsultancyFeeBasis.LUMP_SUM -> rate
            ConsultancyFeeBasis.PER_SQ_FT -> rate.multiply(positive(input.quantity, "Chargeable area"))
            ConsultancyFeeBasis.PERCENT_OF_COST -> {
                require(rate <= BigDecimal("100")) { "Percentage must not exceed 100." }
                rate.multiply(positive(input.quantity, "Assigned works cost")).divide(BigDecimal("100"))
            }
        }
        val amount = raw.setScale(2, RoundingMode.HALF_UP)
        require(amount.signum() > 0 && amount <= BigDecimal("999999999999.99")) { "Fee is outside the supported range." }
        return amount
    }

    fun validate(input: ConsultancyInput, forIssue: Boolean = false) {
        require(input.title.isNotBlank()) { "A document title is required." }
        amount(input)
        if (input.gstMode != GstMode.NONE) require(input.gstin.matches(Regex("[0-9]{2}[A-Z0-9]{13}"))) { "Enter supplier GSTIN." }
        if (input.gstMode == GstMode.REGULAR) {
            require(input.gstin.take(2) == input.supplierState) { "Supplier state must match GSTIN." }
            FinanceMath.tax(amount(input).toPlainString(), input.gstMode, input.gstRate, input.supplierState, input.placeState)
        }
        if (input.feeBasis == ConsultancyFeeBasis.PER_SQ_FT) require(input.areaDefinition.isNotBlank()) { "Define which area is chargeable and how it is measured." }
        require(input.milestones.isNotEmpty()) { "Add at least one payment milestone." }
        var previous = BigDecimal.ZERO
        input.milestones.forEach {
            require(it.label.isNotBlank()) { "Name every milestone." }
            val percent = positive(it.cumulativePercent, "Cumulative percentage")
            require(percent > previous && percent <= BigDecimal("100")) { "Milestone percentages must increase and not exceed 100." }
            previous = percent
        }
        require(previous.compareTo(BigDecimal("100")) == 0) { "The last milestone must be 100%." }
        if (forIssue) {
            val required = listOf(input.parties, input.scope, input.terms)
            require(required.all { it.isNotBlank() }) { "Complete parties, scope and contract terms before issue." }
            require((required + input.exclusions + input.areaDefinition + input.milestones.map { it.label }).none { Regex("\\[[^]]*]|\\b(TODO|TBD)\\b", RegexOption.IGNORE_CASE).containsMatchIn(it) }) { "Resolve every bracketed placeholder before issue." }
        }
    }

    fun render(input: ConsultancyInput, agreement: Boolean, status: String = "DRAFT"): String {
        validate(input)
        val fee = amount(input)
        return buildString {
            appendLine(if (agreement) "CLIENT–ARCHITECT CONSULTANCY AGREEMENT" else "FEE PROPOSAL")
            appendLine(input.title); appendLine("Status: $status · ${input.service.label}")
            appendLine(); appendLine(input.parties)
            if (input.proposalReference.isNotBlank()) appendLine("Proposal reference: ${input.proposalReference}")
            appendLine(); appendLine("1. ASSIGNED SCOPE"); appendLine(input.scope)
            appendLine(); appendLine("2. EXCLUSIONS"); appendLine(input.exclusions.ifBlank { "None stated; confirm before signing." })
            appendLine(); appendLine("3. PROFESSIONAL FEE")
            appendLine("Basis: ${input.feeBasis.label}")
            when (input.feeBasis) {
                ConsultancyFeeBasis.LUMP_SUM -> appendLine("Agreed lump sum: $fee")
                ConsultancyFeeBasis.PER_SQ_FT -> { appendLine("${input.quantity} sq ft × ${input.rate} per sq ft = $fee"); appendLine("Area definition: ${input.areaDefinition}") }
                ConsultancyFeeBasis.PERCENT_OF_COST -> appendLine("${input.rate}% × assigned works cost ${input.quantity} = $fee; reconcile changes to the agreed cost basis in writing.")
            }
            appendLine("Base professional fee; currency INR. Reimbursements and additional services require agreement.")
            when (input.gstMode) {
                GstMode.NONE -> appendLine("No GST charged: supplier has no GST registration.")
                GstMode.COMPOSITION -> appendLine("Composition taxable person, not eligible to collect tax on supplies. GSTIN: ${input.gstin}. No GST is added to the fee.")
                GstMode.REGULAR -> {
                    val tax = FinanceMath.tax(fee.toPlainString(), input.gstMode, input.gstRate, input.supplierState, input.placeState)
                    appendLine("GSTIN: ${input.gstin}; supplier state ${input.supplierState}; place of supply ${input.placeState}.")
                    appendLine("GST ${input.gstRate}%: CGST ${tax.cgst}; SGST ${tax.sgst}; IGST ${tax.igst}; total including GST ${tax.total}.")
                }
            }
            appendLine("Milestone percentages below apply to professional fees; invoice applicable GST separately. Actual client TDS is reconciled against payment and tax records, not treated as cash received.")
            appendLine(); appendLine("4. PAYMENT MILESTONES — CUMULATIVE TARGETS")
            var previousAmount = BigDecimal("0.00")
            input.milestones.forEach { stage ->
                val target = fee.multiply(stage.cumulativePercent.toBigDecimal()).divide(BigDecimal("100"), 2, RoundingMode.HALF_UP)
                appendLine("${stage.label}: ${stage.cumulativePercent}% to date = $target; increase from prior target ${target.subtract(previousAmount)}.")
                previousAmount = target
            }
            appendLine("Deduct payments already accounted for; do not add cumulative targets together. Any retainer is an advance, not an extra fee.")
            appendLine(); appendLine("5. AGREED TERMS"); appendLine(input.terms)
            appendLine(); appendLine("6. ACCEPTANCE")
            appendLine("Client name/signature: __________________  Date: __________")
            appendLine("Architect name/signature: _______________  Date: __________")
            appendLine("Issue is not evidence of either party's signature or acceptance.")
            appendLine(); appendLine("Template: ${input.templateVersion}")
            if (input.sourceUrl.isNotBlank()) {
                appendLine("CoA reference, checked 5 November 2026: ${input.sourceUrl}")
                appendLine("Scale of charges: $SCALE_URL")
                appendLine("Entered rates and altered terms are project-specific; the app does not certify compliance with the CoA scale.")
            }
            appendLine(REVIEW_NOTE)
        }
    }
}
