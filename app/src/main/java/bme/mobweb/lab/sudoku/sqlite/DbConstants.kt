package hu.bme.mobweb.lab.sudoku.sqlite

import android.database.sqlite.SQLiteDatabase
import android.util.Log

/*
    Works with constant SQlite parameters
 */
object DbConstants {

    const val DATABASE_NAME = "sudoku.db"
    const val DATABASE_VERSION = 2

    object Puzzles {
        const val DATABASE_TABLE = "puzzles"

        enum class Columns {
            ID,
            timeCreated,
            gridString
        }

        private val DATABASE_CREATE = """create table if not exists $DATABASE_TABLE (
            ${Columns.ID.name} integer primary key autoincrement,
            ${Columns.timeCreated.name} text,
            ${Columns.gridString.name} text not null
            );"""


        private const val DATABASE_DROP = "drop table if exists $DATABASE_TABLE;"

        fun onCreate(database: SQLiteDatabase) {
            database.execSQL(DATABASE_CREATE)
        }

        fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            Log.w(
                Puzzles::class.java.name,
                "Upgrading from version $oldVersion to $newVersion"
            )
            database.execSQL(DATABASE_DROP)
            onCreate(database)
        }
    }
}
