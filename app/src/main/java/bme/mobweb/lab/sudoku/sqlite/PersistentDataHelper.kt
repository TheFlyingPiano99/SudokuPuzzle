package hu.bme.mobweb.lab.sudoku.sqlite

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import bme.mobweb.lab.sudoku.model.Puzzle
import java.text.SimpleDateFormat


/*
    Provides data persistence
 */
class PersistentDataHelper(context: Context) {
    private var database: SQLiteDatabase? = null
    private val dbHelper: DbHelper = DbHelper(context)

    private var timeCreatedFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    private var timeSpentFormatter = SimpleDateFormat("HH:mm:ss")

    private val tableColumns = arrayOf(
        DbConstants.Puzzles.Columns.ID.name,
        DbConstants.Puzzles.Columns.timeCreated.name,
        DbConstants.Puzzles.Columns.timeSpentSolving.name,
        DbConstants.Puzzles.Columns.gridString.name
    )

    @Throws(SQLiteException::class)
    fun open() {
        database = dbHelper.writableDatabase
    }

    fun close() {
        dbHelper.close()
    }

    fun persistTables(puzzles: List<Puzzle>) {
        clearTables()
        for (puzzle in puzzles) {
            val values = ContentValues()
            values.put(DbConstants.Puzzles.Columns.ID.name, puzzle.ID)
            values.put(DbConstants.Puzzles.Columns.timeCreated.name, timeCreatedFormatter.format(puzzle.timeCreated))
            values.put(DbConstants.Puzzles.Columns.timeSpentSolving.name, timeSpentFormatter.format(puzzle.timeSpentSolving))
            values.put(DbConstants.Puzzles.Columns.gridString.name, puzzle.toString())
            database!!.insert(DbConstants.Puzzles.DATABASE_TABLE, null, values)
        }
    }

    fun restoreTables(): MutableList<Puzzle> {
        val puzzles: MutableList<Puzzle> = ArrayList()
        val cursor: Cursor =
            database!!.query(DbConstants.Puzzles.DATABASE_TABLE, tableColumns, null, null, null, null, null)
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            val puzzle: Puzzle = cursorToPuzzle(cursor)
            puzzles.add(puzzle)
            cursor.moveToNext()
        }
        cursor.close()
        return puzzles
    }

    fun clearTables() {
        database!!.delete(DbConstants.Puzzles.DATABASE_TABLE, null, null)
    }

    private fun cursorToPuzzle(cursor: Cursor): Puzzle {
        val puzzleID = cursor.getInt(DbConstants.Puzzles.Columns.ID.ordinal)
        val timeCreated = timeCreatedFormatter.parse(cursor.getString(DbConstants.Puzzles.Columns.timeCreated.ordinal))
        val timeSpentSolving = timeSpentFormatter.parse(cursor.getString(DbConstants.Puzzles.Columns.timeSpentSolving.ordinal))
        val gridString = cursor.getString(DbConstants.Puzzles.Columns.gridString.ordinal)
        return Puzzle(puzzleID, timeCreated!!, timeSpentSolving!!, gridString)
    }

}
