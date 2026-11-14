package com.holagundi.archiman.sync

import androidx.room.RoomDatabase
import java.util.concurrent.Callable

/**
 * The little SQL surface the sync code needs. The app implements it over Room's database ([RoomSqlDb]); the unit tests implement it
 * over JDBC SQLite, so the real triggers and queries are exercised on the JVM.
 */
interface SqlDb {
    fun exec(sql: String, vararg args: Any?)

    /** Rows as column name to value (Long, Double, String, ByteArray or null). */
    fun query(sql: String, vararg args: Any?): List<Map<String, Any?>>

    fun lastInsertId(): Long

    /** One transaction; an exception rolls everything back. */
    fun <T> transaction(block: () -> T): T
}

/** [SqlDb] over Room. Writes go through Room's transaction so live queries (Flows) refresh after a sync. */
class RoomSqlDb(private val room: RoomDatabase) : SqlDb {
    private val db get() = room.openHelper.writableDatabase

    override fun exec(sql: String, vararg args: Any?) {
        if (args.isEmpty()) db.execSQL(sql) else db.execSQL(sql, arrayOf(*args))
    }

    override fun query(sql: String, vararg args: Any?): List<Map<String, Any?>> {
        val bind = args.map { it?.toString() }.toTypedArray()
        db.query(sql, bind).use { c ->
            val rows = ArrayList<Map<String, Any?>>()
            while (c.moveToNext()) {
                val row = LinkedHashMap<String, Any?>()
                for (i in 0 until c.columnCount) {
                    row[c.getColumnName(i)] = when (c.getType(i)) {
                        android.database.Cursor.FIELD_TYPE_NULL -> null
                        android.database.Cursor.FIELD_TYPE_INTEGER -> c.getLong(i)
                        android.database.Cursor.FIELD_TYPE_FLOAT -> c.getDouble(i)
                        android.database.Cursor.FIELD_TYPE_BLOB -> c.getBlob(i)
                        else -> c.getString(i)
                    }
                }
                rows += row
            }
            return rows
        }
    }

    override fun lastInsertId(): Long = (query("SELECT last_insert_rowid() AS id").first()["id"] as Long)

    override fun <T> transaction(block: () -> T): T = room.runInTransaction(Callable { block() })
}
