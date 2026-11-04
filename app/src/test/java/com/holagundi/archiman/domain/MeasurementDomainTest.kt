package com.holagundi.archiman.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MeasurementDomainTest {
    @Test
    fun schemaIs28() {
        assertEquals(28, SchemaContract.VERSION)
    }

    @Test
    fun quantityFormulasMatchTheBook() {
        assertEquals(3.0, QuantityCalculator.calculate(CalculationTypes.NOS, MeasurementInput(no = 3.0)), 0.0)
        assertEquals(12.5, QuantityCalculator.calculate(CalculationTypes.RUNNING_LENGTH, MeasurementInput(no = 2.5, length = 5.0)), 0.0)
        assertEquals(6.0, QuantityCalculator.calculate(CalculationTypes.AREA, MeasurementInput(length = 3.0, breadth = 2.0)), 0.0)
        assertEquals(23.0, QuantityCalculator.calculate(CalculationTypes.WALL_PLASTER, MeasurementInput(no = 2.0, length = 3.0, height = 4.0, deduction = 1.0)), 0.0)
        assertEquals(3.31, QuantityCalculator.calculate(CalculationTypes.VOLUME, MeasurementInput(length = 24.5, breadth = 0.3, height = 0.45)), 0.001)
    }

    @Test
    fun sheetReturnRequiresAComment() {
        assertTrue(MeasurementSheetWorkflow.canTransition(SheetStatus.SUBMITTED, SheetStatus.CHECKED))
        assertFalse(MeasurementSheetWorkflow.canTransition(SheetStatus.SUBMITTED, SheetStatus.RETURNED, ""))
        assertTrue(MeasurementSheetWorkflow.canTransition(SheetStatus.CHECKED, SheetStatus.RETURNED, "Recheck grid B"))
        assertTrue(MeasurementSheetWorkflow.allowedTransitions(SheetStatus.APPROVED).isEmpty())
    }

    @Test
    fun calcHonoursPrecedenceAndMeasureGrid() {
        assertEquals("14.00", ArchiCalcEngine.evaluate("2+3*4"))
        assertEquals("14.00", ArchiCalcEngine.evaluate("2*(3+4)"))
        val grid = ArchiCalcMeasure.formatGrid("3 x 4")
        assertEquals("3.00", grid["m"])
        assertEquals("12.00", grid["m2"])
        assertEquals("0.00", grid["m3"])
    }

    @Test
    fun workCodeIsStableAndClassified() {
        val code = WorkCatalog.codeFor("Earthwork", "Earthwork Excavation")
        assertEquals(code, WorkCatalog.codeFor("Earthwork", "Earthwork Excavation"))
        assertTrue(code.matches(Regex("^[A-Z0-9]{4}-[A-Z0-9]{3,8}-[0-9A-F]{4}$")))
        assertEquals("Masonry", WorkCatalog.classify("Civil", "Brickwork 230mm"))
    }

    @Test
    fun unitConversionAndClosureRules() {
        val feet = MeasurementUnitConverter.quantity(1.0, CalculationTypes.RUNNING_LENGTH, MeasurementUnitSystem.METRIC, MeasurementUnitSystem.IMPERIAL)
        assertEquals(3.2808, feet, 0.001)
        assertEquals("m²", MeasurementUnitConverter.unitFor(CalculationTypes.AREA, MeasurementUnitSystem.METRIC))
        val closed = SiteIssueWorkflow.canTransition(SiteIssueWorkflow.READY_FOR_VERIFICATION, SiteIssueWorkflow.CLOSED, "")
        assertFalse(closed.valid)
        val verified = SiteIssueWorkflow.canTransition(
            SiteIssueWorkflow.READY_FOR_VERIFICATION,
            SiteIssueWorkflow.CLOSED,
            "Rectification checked on site.",
        )
        assertTrue(verified.valid)
        assertEquals(SiteIssueWorkflow.SEVERITY_ATTENTION, SiteIssueWorkflow.normalizeSeverity("MEDIUM"))
        assertEquals(SiteIssueWorkflow.SEVERITY_CRITICAL, SiteIssueWorkflow.normalizeSeverity("HIGH"))
        assertEquals("OPEN", CoordinationWorkflow.allowedNextStatuses(CoordinationWorkflow.RFI, "DRAFT").single())
        assertEquals(
            "Invalid coordination transition DRAFT to CLOSED for RFI.",
            CoordinationWorkflow.transitionError(CoordinationWorkflow.RFI, "DRAFT", "CLOSED"),
        )
        val abstract = QuantityAbstract.summarize(
            listOf(
                AbstractRow("Brick", "m²", 10.0, approved = false),
                AbstractRow("RCC", "m³", 3.31, approved = true),
                AbstractRow("RCC", "m³", 1.0, approved = true),
            ),
        )
        assertEquals(1, abstract.size)
        assertEquals(4.31, abstract.single().totalQuantity, 0.001)
        val csv = QuantityAbstract.csv("Villa", "PRJ-1", abstract)
        assertTrue(csv.contains("QUANTITY ONLY"))
        assertFalse(csv.contains("rate", ignoreCase = true))
        assertTrue(csv.contains("4.31"))
    }

    @Test
    fun csvStaysQuantityOnly() {
        val csv = MeasurementSheetCsv.render(
            SheetExport(
                projectName = "Villa",
                projectCode = "PRJ-1",
                sheetCode = "MS-001",
                floorName = "Ground Floor",
                contractorName = "Apex",
                itemName = "Plaster",
                formulaCode = CalculationTypes.WALL_PLASTER,
                status = SheetStatus.DRAFT,
                unit = "m²",
                rows = listOf(
                    SheetExportRow("Wall", "Foyer", 4.0, 0.0, 3.0, 1.0, 1.2, 10.8, "", 0L),
                ),
            ),
        )
        assertTrue(csv.contains("QUANTITY ONLY"))
        assertFalse(csv.contains("rate", ignoreCase = true))
        assertTrue(csv.contains("10.80"))
    }

    @Test
    fun projectPathFollowsBillThenPayment() {
        assertEquals(ProjectPath.ASSIGN_STAGES, ProjectPath.next(false, false, false, false))
        assertEquals(ProjectPath.ISSUE_INVOICE, ProjectPath.next(true, true, true, true))
        assertEquals(ProjectPath.RECORD_PAYMENT, ProjectPath.next(true, true, false, true))
        assertEquals(ProjectPath.BILL_STAGE, ProjectPath.next(true, true, false, false))
        assertEquals(ProjectPath.WAIT, ProjectPath.next(true, false, false, false))
    }

    @Test
    fun estimateMultipliesQuantityByRate() {
        val lines = EstimateBuilder.lines(
            mapOf("Plaster" to (10.0 to "m²"), "Unknown" to (2.0 to "m")),
            mapOf("plaster" to 100.0),
        )
        assertEquals(1000.0, lines.first { it.itemName == "Plaster" }.amount, 0.0)
        assertEquals(0.0, lines.first { it.itemName == "Unknown" }.rate, 0.0)
    }
}
