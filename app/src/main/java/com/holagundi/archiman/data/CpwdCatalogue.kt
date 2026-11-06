package com.holagundi.archiman.data
import com.holagundi.archiman.data.local.ArchiManDatabase
import com.holagundi.archiman.data.local.entity.ItemMasterEntity
import com.holagundi.archiman.domain.*
import androidx.room.withTransaction
object CpwdCatalogue {
    const val SOURCE = "CPWD DSR 2023 — adapted short descriptions; verify full specification and correction slips"
    val starters = listOf(
        listOf("4.1.3","Plain concrete 1:2:4 to plinth","m³","VOLUME","Concrete"),
        listOf("5.1.2","Reinforced concrete 1:1.5:3 to plinth; reinforcement and formwork separate","m³","VOLUME","Concrete"),
        listOf("6.1.2","Non-modular brick foundation masonry, class 7.5, mortar 1:6","m³","VOLUME","Masonry"),
        listOf("6.13.2","Half-brick superstructure, class 7.5, mortar 1:4","m²","WALL_PLASTER","Masonry"),
        listOf("11.3.1","Concrete flooring 1:2:4, 40 mm, 20 mm aggregate","m²","AREA","Flooring"),
        listOf("13.16.1","Cement plaster 6 mm, mortar 1:3","m²","WALL_PLASTER","Finishes"),
        listOf("13.41.1","New acrylic distemper over cement primer","m²","WALL_PLASTER","Finishes"),
        listOf("13.44.1","New cement-paint wall finish","m²","WALL_PLASTER","Finishes")
    )
    suspend fun seed(db:ArchiManDatabase)=db.withTransaction {
        starters.forEach { row ->
            val code="CPWD23-"+row[0]
            val exists=db.openHelper.writableDatabase.query("SELECT COUNT(*) FROM item_master WHERE itemCode=?",arrayOf(code)).use { it.moveToFirst();it.getInt(0)>0 }
            if(!exists) db.catalogDao().insertItem(ItemMasterEntity(itemCode=code,workType=row[4],name=row[1],unit=row[2],calculationType=row[3],specification=row[1]+". Adapted identification only; refer to the full CPWD item, specifications and current correction slips before adopting contract wording.",sourceName=SOURCE,sourceItemCode=row[0],isPredefined=true,isActive=true))
        }
    }
    fun parse(csv:String):List<ItemMasterEntity> {
        require(csv.length<=1000000); val rows=BankCsv.cells(csv);require(rows.size in 2..2001)
        val header=rows.first().map{it.removePrefix("\uFEFF").lowercase().replace(" ","")};require(listOf("code","name","description","unit","formula","edition","sourceitemcode").all{it in header}) { "CSV requires Code, Name, Description, Unit, Formula, Edition, SourceItemCode; optional Trade." }
        return rows.drop(1).mapIndexed{index,row->
            fun get(name:String)=row.getOrNull(header.indexOf(name)).orEmpty().trim()
            try { val unit=when(get("unit").lowercase()){ "cum"->"m³";"sqm"->"m²";"metre"->"m";"each"->"Nos";else->get("unit") };WorkItemLibrary.validate(get("formula"),unit);require(get("code").matches(Regex("[A-Za-z0-9._-]{1,40}"))&&get("name").isNotBlank()&&get("description").isNotBlank()&&get("edition").isNotBlank());ItemMasterEntity(itemCode=get("code"),name=get("name"),specification=get("description"),unit=unit,calculationType=get("formula"),workType=get("trade"),sourceName=get("edition"),sourceItemCode=get("sourceitemcode"),isPredefined=false,isActive=true) } catch(e:Exception){throw IllegalArgumentException("Catalogue row ${index+2}: ${e.message}")}
        }.also{require(it.map{r->r.itemCode}.distinct().size==it.size){"Duplicate item codes in CSV."}}
    }
    suspend fun import(db:ArchiManDatabase,csv:String):Int=db.withTransaction {
        var count=0;parse(csv).forEach{row->val exists=db.openHelper.writableDatabase.query("SELECT COUNT(*) FROM item_master WHERE itemCode=?",arrayOf(row.itemCode)).use{it.moveToFirst();it.getInt(0)>0};if(!exists){db.catalogDao().insertItem(row);count++}};count
    }
}
