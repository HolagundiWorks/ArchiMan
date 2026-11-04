package com.holagundi.archiman.data

import com.holagundi.archiman.data.local.ArchiManDatabase
import com.holagundi.archiman.data.local.entity.ClientEntity
import com.holagundi.archiman.data.local.entity.CompanyProfileEntity
import com.holagundi.archiman.data.local.entity.ContractorEntity
import com.holagundi.archiman.data.local.entity.ContractorQualifiedItemEntity
import com.holagundi.archiman.data.local.entity.FloorEntity
import com.holagundi.archiman.data.local.entity.ItemMasterEntity
import com.holagundi.archiman.data.local.entity.MeasurementEntity
import com.holagundi.archiman.data.local.entity.MeasurementSheetEntity
import com.holagundi.archiman.data.local.entity.ProjectEntity
import com.holagundi.archiman.data.local.entity.RoomEntity
import com.holagundi.archiman.domain.CalculationTypes
import com.holagundi.archiman.domain.MeasurementInput
import com.holagundi.archiman.domain.QuantityCalculator
import com.holagundi.archiman.domain.SheetStatus
import androidx.room.withTransaction
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseSeeder @Inject constructor(
    private val db: ArchiManDatabase,
) {
    suspend fun seedIfEmpty() {
        if (db.practiceDao().companyCount() > 0) return
        val now = System.currentTimeMillis()
        fun ago(days: Int) = now - days * 86_400_000L

        db.withTransaction {
            db.practiceDao().upsertCompany(
                CompanyProfileEntity(
                    practiceName = "Studio Holagundi",
                    legalName = "Studio Holagundi Architecture Private Limited",
                    companyType = "Architecture practice",
                    address = "14/B, Design Enclave, Road No. 3",
                    city = "Bengaluru",
                    state = "Karnataka",
                    country = "India",
                    pinCode = "560001",
                    phone = "+91 98450 12345",
                    email = "contact@studioholagundi.com",
                    website = "https://studioholagundi.com",
                    pan = "AAACH9921D",
                    gstin = "29AAACH9921D1Z5",
                    coaRegistrationNumber = "CA/2014/61234",
                    principalName = "Ar. Vishwabhiram Holagundi",
                    principalQualification = "B.Arch, M.Des (Urban Design)",
                    practiceRegistrationDetails = "Council of Architecture registered practice",
                    updatedAt = now,
                ),
            )
            val ananya = db.practiceDao().insertClient(
                ClientEntity(
                    name = "Dr. Ananya Rao",
                    clientType = "Individual",
                    contactPerson = "Dr. Ananya Rao",
                    address = "Plot 42, Jubilee Hills",
                    correspondenceAddress = "Plot 42, Jubilee Hills, Hyderabad",
                    contactNo = "+91 98765 43210",
                    email = "ananya.rao@example.com",
                    preferredCommunication = "Email",
                    notes = "Residential villa. Natural light and exposed brick.",
                    createdAt = ago(30),
                ),
            )
            val utic = db.practiceDao().insertClient(
                ClientEntity(
                    name = "Urban Transport Infrastructure Corp",
                    clientType = "Corporate",
                    contactPerson = "K. Somasekhar",
                    address = "Metro Bhavan, MG Road",
                    correspondenceAddress = "Metro Bhavan, MG Road, Bengaluru",
                    contactNo = "+91 94444 88888",
                    email = "projects@utic.gov.in",
                    preferredCommunication = "Phone",
                    notes = "Transit plaza and concourse.",
                    createdAt = ago(60),
                ),
            )
            val villa = db.projectDao().insertProject(
                ProjectEntity(
                    name = "Skyline Villa Residence",
                    client = "Dr. Ananya Rao",
                    clientId = ananya,
                    siteLocation = "Jubilee Hills, Road No. 45, Hyderabad",
                    projectCode = "PRJ-2026-01",
                    projectType = "Residential",
                    status = "ACTIVE",
                    description = "Four-level villa with cantilevered decks, courtyards and rainwater harvesting.",
                    architectInCharge = "Ar. Vishwabhiram Holagundi",
                    startDate = ago(45),
                    targetCompletionDate = now + 240 * 86_400_000L,
                    plotArea = 480.0,
                    builtUpArea = 650.0,
                    areaUnit = "m²",
                    createdAt = ago(45),
                ),
            )
            val metro = db.projectDao().insertProject(
                ProjectEntity(
                    name = "Metro Transit Urban Pavilion",
                    client = "Urban Transport Infrastructure Corp",
                    clientId = utic,
                    siteLocation = "MG Road Metro Interchange, Bengaluru",
                    projectCode = "PRJ-2026-02",
                    projectType = "Commercial",
                    status = "ACTIVE",
                    description = "Pedestrian concourse, canopy and transit plaza amenities.",
                    architectInCharge = "Ar. Rajesh Verma",
                    startDate = ago(90),
                    targetCompletionDate = now + 180 * 86_400_000L,
                    plotArea = 2500.0,
                    builtUpArea = 3200.0,
                    areaUnit = "m²",
                    createdAt = ago(90),
                ),
            )
            val ground = db.projectDao().insertFloor(FloorEntity(projectId = villa, name = "Ground Floor", orderIndex = 0))
            val first = db.projectDao().insertFloor(FloorEntity(projectId = villa, name = "First Floor", orderIndex = 1))
            db.projectDao().insertFloor(FloorEntity(projectId = villa, name = "Terrace & Deck", orderIndex = 2))
            db.projectDao().insertFloor(FloorEntity(projectId = metro, name = "Concourse Level 0", orderIndex = 0))
            db.projectDao().insertFloor(FloorEntity(projectId = metro, name = "Mezzanine Retail", orderIndex = 1))
            listOf(
                ground to listOf("Entrance Foyer", "Double Height Living Room", "Dining & Open Kitchen"),
                first to listOf("Master Suite & Walk-in", "Bed 2 / Studio"),
            ).forEach { (floorId, rooms) ->
                rooms.forEachIndexed { index, name ->
                    db.projectDao().insertRoom(RoomEntity(projectId = villa, floorId = floorId, name = name, orderIndex = index))
                }
            }

            val apex = insertContractor(villa, "Apex Civil Infrastructure", "Civil", "+91 99001 12233", ago(40))
            val spark = insertContractor(villa, "SparkLine Electrics & Automation", "Electrical", "+91 98112 33445", ago(40))
            val aqua = insertContractor(villa, "AquaFlow Plumbing & Sanitation", "Plumbing", "+91 97223 44556", ago(40))

            qualifiedSeeds(apex, spark, aqua).forEach { item ->
                db.catalogDao().insertQualified(
                    ContractorQualifiedItemEntity(
                        contractorId = item.contractorId,
                        workType = item.workType,
                        itemName = item.name,
                        uom = item.uom,
                        calculationType = item.type,
                    ),
                )
            }

            val masters = listOf(
                ItemMasterEntity(itemCode = "EART-EXCA-01", workType = "Earthwork", name = "Earthwork Excavation", unit = "m³", calculationType = CalculationTypes.VOLUME, specification = "Excavation in ordinary soil including dressing and disposal within 50m.", sourceName = "PWD SR", sourceItemCode = "SR-01-01", isPredefined = true, isActive = true),
                ItemMasterEntity(itemCode = "CONC-PCCF-01", workType = "Concrete & Structure", name = "PCC Foundation 1:4:8", unit = "m³", calculationType = CalculationTypes.VOLUME, specification = "Plain cement concrete 1:4:8 with 40mm graded stone aggregate.", sourceName = "PWD SR", sourceItemCode = "SR-02-04", isPredefined = true, isActive = true),
                ItemMasterEntity(itemCode = "CONC-RCCM-01", workType = "Concrete & Structure", name = "RCC M25 Column & Beams", unit = "m³", calculationType = CalculationTypes.VOLUME, specification = "Design mix M25 in columns, beams and lintels.", sourceName = "PWD SR", sourceItemCode = "SR-02-12", isPredefined = true, isActive = true),
                ItemMasterEntity(itemCode = "CONC-SHUT-01", workType = "Concrete & Structure", name = "Shuttering & Formwork", unit = "m²", calculationType = CalculationTypes.AREA, specification = "Centering and shuttering with film faced plywood.", sourceName = "PWD SR", sourceItemCode = "SR-02-18", isPredefined = true, isActive = true),
                ItemMasterEntity(itemCode = "MASO-BRIC-01", workType = "Masonry", name = "BrickWork 230mm in CM 1:6", unit = "m²", calculationType = CalculationTypes.AREA, specification = "First class bricks in cement mortar 1:6.", sourceName = "PWD SR", sourceItemCode = "SR-03-02", isPredefined = true, isActive = true),
                ItemMasterEntity(itemCode = "FINI-PLAS-01", workType = "Finishes", name = "Internal Cement Plaster 12mm", unit = "m²", calculationType = CalculationTypes.WALL_PLASTER, specification = "12mm cement plaster in CM 1:4.", sourceName = "PWD SR", sourceItemCode = "SR-04-01", isPredefined = true, isActive = true),
                ItemMasterEntity(itemCode = "ELEC-COND-01", workType = "Electrical Works", name = "Concealed Conduit 25mm", unit = "m", calculationType = CalculationTypes.RUNNING_LENGTH, specification = "Medium duty rigid PVC conduit.", sourceName = "PWD SR", sourceItemCode = "SR-07-01", isPredefined = true, isActive = true),
                ItemMasterEntity(itemCode = "ELEC-LPNT-01", workType = "Electrical Works", name = "Light Point Wiring", unit = "Nos", calculationType = CalculationTypes.NOS, specification = "Point wiring with 1.5 sq.mm FRLS copper.", sourceName = "PWD SR", sourceItemCode = "SR-07-05", isPredefined = true, isActive = true),
                ItemMasterEntity(itemCode = "PLUM-CPVC-01", workType = "Plumbing Works", name = "CPVC Water Supply Pipe 25mm", unit = "m", calculationType = CalculationTypes.RUNNING_LENGTH, specification = "SDR 11 CPVC pipes concealed in chase.", sourceName = "PWD SR", sourceItemCode = "SR-08-02", isPredefined = true, isActive = true),
                ItemMasterEntity(itemCode = "JOIN-DOOR-01", workType = "Joinery & Carpentry", name = "Teakwood Door Frame", unit = "m", calculationType = CalculationTypes.RUNNING_LENGTH, specification = "CP teakwood frame 125x75mm.", sourceName = "PWD SR", sourceItemCode = "SR-05-01", isPredefined = true, isActive = true),
                ItemMasterEntity(itemCode = "FLOO-TILE-01", workType = "Flooring & Cladding", name = "Vitrified Tile Flooring 800x800", unit = "m²", calculationType = CalculationTypes.AREA, specification = "Vitrified tiles on 20mm cement mortar bed.", sourceName = "PWD SR", sourceItemCode = "SR-06-03", isPredefined = true, isActive = true),
                ItemMasterEntity(itemCode = "WATE-TERR-01", workType = "Waterproofing", name = "Brick Bat Coba Waterproofing", unit = "m²", calculationType = CalculationTypes.AREA, specification = "Integral waterproofing with brick bat coba.", sourceName = "PWD SR", sourceItemCode = "SR-10-01", isPredefined = true, isActive = true),
            )
            var rccId = 0L
            var brickId = 0L
            masters.forEach { item ->
                val id = db.catalogDao().insertItem(item)
                if (item.name.startsWith("RCC")) rccId = id
                if (item.name.startsWith("BrickWork")) brickId = id
            }

            val beamQty = QuantityCalculator.calculate(
                CalculationTypes.VOLUME,
                MeasurementInput(no = 1.0, length = 24.5, breadth = 0.3, height = 0.45),
            )
            val approved = db.measurementDao().insertSheet(
                MeasurementSheetEntity(
                    sheetCode = "MS-PRJ-2026-01-001",
                    projectId = villa,
                    floorId = ground,
                    floorNameSnapshot = "Ground Floor",
                    contractorId = apex,
                    contractorNameSnapshot = "Apex Civil Infrastructure",
                    itemId = rccId,
                    itemNameSnapshot = "RCC M25 Column & Beams",
                    uomSnapshot = "m³",
                    formulaCode = CalculationTypes.VOLUME,
                    formulaVersion = 1,
                    status = SheetStatus.APPROVED,
                    revision = 1,
                    lockedAt = ago(5),
                    createdAt = ago(15),
                    updatedAt = ago(5),
                ),
            )
            db.measurementDao().insertMeasurement(
                MeasurementEntity(
                    sheetId = approved,
                    projectId = villa,
                    floorId = ground,
                    contractorId = apex,
                    contractorName = "Apex Civil Infrastructure",
                    itemId = rccId,
                    itemName = "RCC M25 Column & Beams",
                    unit = "m³",
                    calculationType = CalculationTypes.VOLUME,
                    formulaCode = CalculationTypes.VOLUME,
                    formulaVersion = 1,
                    description = "Ground floor plinth beams PB-1 to PB-6",
                    length = 24.5,
                    width = 0.3,
                    height = 0.45,
                    nos = 1.0,
                    deduction = 0.0,
                    quantity = beamQty,
                    floor = "Ground Floor",
                    location = "Perimeter foundation grid A-D",
                    remarks = "Concreted as per structural drawing S-03",
                    date = ago(14),
                ),
            )
            val brickQty = QuantityCalculator.calculate(
                CalculationTypes.AREA,
                MeasurementInput(no = 1.0, length = 18.0, breadth = 3.0),
            )
            val submitted = db.measurementDao().insertSheet(
                MeasurementSheetEntity(
                    sheetCode = "MS-PRJ-2026-01-002",
                    projectId = villa,
                    floorId = ground,
                    floorNameSnapshot = "Ground Floor",
                    contractorId = apex,
                    contractorNameSnapshot = "Apex Civil Infrastructure",
                    itemId = brickId,
                    itemNameSnapshot = "BrickWork 230mm in CM 1:6",
                    uomSnapshot = "m²",
                    formulaCode = CalculationTypes.AREA,
                    formulaVersion = 1,
                    status = SheetStatus.SUBMITTED,
                    revision = 1,
                    createdAt = ago(7),
                    updatedAt = ago(2),
                ),
            )
            db.measurementDao().insertMeasurement(
                MeasurementEntity(
                    sheetId = submitted,
                    projectId = villa,
                    floorId = ground,
                    contractorId = apex,
                    contractorName = "Apex Civil Infrastructure",
                    itemId = brickId,
                    itemName = "BrickWork 230mm in CM 1:6",
                    unit = "m²",
                    calculationType = CalculationTypes.AREA,
                    formulaCode = CalculationTypes.AREA,
                    formulaVersion = 1,
                    description = "External 230mm wall, grid A",
                    length = 18.0,
                    width = 3.0,
                    height = 0.0,
                    nos = 1.0,
                    deduction = 0.0,
                    quantity = brickQty,
                    floor = "Ground Floor",
                    location = "Entrance Foyer",
                    remarks = "",
                    date = ago(6),
                ),
            )
        }
    }

    private data class QualifiedSeed(
        val contractorId: Long,
        val workType: String,
        val name: String,
        val uom: String,
        val type: String,
    )

    private fun qualifiedSeeds(apex: Long, spark: Long, aqua: Long) = listOf(
        QualifiedSeed(apex, "Earthwork", "Earthwork Excavation", "m³", CalculationTypes.VOLUME),
        QualifiedSeed(apex, "Concrete & Structure", "PCC Foundation 1:4:8", "m³", CalculationTypes.VOLUME),
        QualifiedSeed(apex, "Concrete & Structure", "RCC M25 Column & Beams", "m³", CalculationTypes.VOLUME),
        QualifiedSeed(apex, "Concrete & Structure", "Shuttering & Formwork", "m²", CalculationTypes.AREA),
        QualifiedSeed(apex, "Masonry", "BrickWork 230mm in CM 1:6", "m²", CalculationTypes.AREA),
        QualifiedSeed(apex, "Finishes", "Internal Cement Plaster 12mm", "m²", CalculationTypes.WALL_PLASTER),
        QualifiedSeed(spark, "Electrical Works", "Concealed Conduit 25mm", "m", CalculationTypes.RUNNING_LENGTH),
        QualifiedSeed(spark, "Electrical Works", "Light Point Wiring", "Nos", CalculationTypes.NOS),
        QualifiedSeed(spark, "Electrical Works", "Power Point 16A", "Nos", CalculationTypes.NOS),
        QualifiedSeed(aqua, "Plumbing Works", "CPVC Water Supply Pipe 25mm", "m", CalculationTypes.RUNNING_LENGTH),
        QualifiedSeed(aqua, "Plumbing Works", "SWR Drainage Pipe 110mm", "m", CalculationTypes.RUNNING_LENGTH),
        QualifiedSeed(aqua, "Plumbing Works", "Wall Hung WC Installation", "Nos", CalculationTypes.NOS),
    )

    private suspend fun insertContractor(projectId: Long, name: String, type: String, phone: String, createdAt: Long): Long =
        db.catalogDao().insertContractor(
            ContractorEntity(
                projectId = projectId,
                name = name,
                address = "",
                contactNo = phone,
                phone = phone,
                contractorType = type,
                createdAt = createdAt,
            ),
        )
}
