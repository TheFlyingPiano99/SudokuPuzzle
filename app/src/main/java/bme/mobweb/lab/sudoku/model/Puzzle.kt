package bme.mobweb.lab.sudoku.model

import java.lang.RuntimeException
import java.util.*

/*
The puzzle grid
Row major 9x9 matrix
 */



class Puzzle() {
    private companion object IDGenerator {
        private var greatestID : Int = -1
    }
    var ID : Int
    var timeCreated : Date

    private val fieldsInRow = 9
    private val fieldsInRowInSquare = 3

    private var grid = Array(fieldsInRow) { _ -> Array(fieldsInRow) { _ -> -1 } }
    private var validity = Array(fieldsInRow) { _ -> Array(fieldsInRow) { _ -> true } }
    private var evidence = Array(fieldsInRow) { _ -> Array(fieldsInRow) { _ -> false } }

    init {
        ID = ++greatestID
        timeCreated = Date(System.currentTimeMillis())
    }

    constructor(from : Puzzle) : this() {
        ID = from.ID
        timeCreated = from.timeCreated

        for (r in 0 until fieldsInRow) {
            grid[r] = from.grid[r].copyOf()
            validity[r] = from.validity[r].copyOf()
            evidence[r] = from.evidence[r].copyOf()
        }
    }

    constructor(ID: Int, timeCreated : Date, gridString : String) : this() {
        this.ID = ID
        if (greatestID < this.ID) {
            greatestID = this.ID
        }
        this.timeCreated = timeCreated
        val lines = gridString.split("\n")
        if (lines.size < 9) {
            throw RuntimeException("Too few lines in gridString")
        }
        for (r in 0 until fieldsInRow) {
            val words = lines[r].split("\t")
            for ((c, word) in words.withIndex()) {
                grid[r][c] = word.toInt()
            }
        }
        if (18 == lines.size) {
            for (r in 0 until fieldsInRow) {
                val words = lines[r + 9].split("\t")
                for ((c, word) in words.withIndex()) {
                    evidence[r][c] = word.toBoolean()
                }
            }
        }
    }

    fun getNumberOgFieldsInRow() : Int {
        return fieldsInRow
    }

    fun getNumberOgFieldsInColumn() : Int {
        return fieldsInRow
    }

    fun getField(row : Int, column : Int) : Int {
        return grid[row][column]
    }

    fun setFieldAsVariable(row : Int, column : Int, value : Int) {
        if (evidence[row][column]) {
            throw RuntimeException("Field is evidence.")
        }
        if (value > 9 || (value < 1 && value != -1)) {
            throw RuntimeException("Value out of domain.")
        }
        if (row > 8 || row < 0) {
            throw RuntimeException("Row out of bounds.")
        }
        if (column > 8 || column < 0) {
            throw RuntimeException("Column out of bounds.")
        }

        grid[row][column] = value
    }

    fun setFieldAsEvidence(row : Int, column : Int, value : Int) {
        if (value > 9 || (value < 1 && value != -1)) {
            throw RuntimeException("Value out of domain.")
        }
        if (row > 8 || row < 0) {
            throw RuntimeException("Row out of bounds.")
        }
        if (column > 8 || column < 0) {
            throw RuntimeException("Column out of bounds.")
        }
        evidence[row][column] = true
        grid[row][column] = value
    }

    fun getEvidence(row : Int, column : Int) : Boolean {
        val v = grid[row][column]
        return evidence[row][column]
    }

    fun setEvidence(row : Int, column : Int, b : Boolean)  {
        evidence[row][column] = b
    }

    fun getValidity(row : Int, column : Int) : Boolean {
        return validity[row][column]
    }

    fun ereaseVariables() {
        for (r in 0 until fieldsInRow) {
            for (c in 0 until fieldsInRow) {
                if (!getEvidence(r, c)) {
                    grid[r][c] = -1
                }
            }
        }
    }

    fun ereaseVariablesAndEvidences() {
        for (r in 0 until fieldsInRow) {
            for (c in 0 until fieldsInRow) {
                grid[r][c] = -1
                evidence[r][c] = false
            }
        }
    }

    override fun toString(): String {
        var builder = StringBuilder("")
        for (r in 0..8) {
            for (c in 0..8) {
                builder.append(this.getField(r, c).toString())
                if (c == 8) {
                    builder.append("\n")
                }
                else if (c < 8) {
                    builder.append("\t")
                }
            }
        }

        for (r in 0..8) {
            for (c in 0..8) {
                builder.append(this.getEvidence(r, c).toString())
                if (c == 8 && r < 8) {
                    builder.append("\n")
                }
                else if (c < 8) {
                    builder.append("\t")
                }
            }
        }

        return builder.toString()
    }

    private fun checkAllRelated(row : Int, column : Int) {
        for (r in 0..8) {
            if (r != row) {
                checkValidityOfField(r, column)
            }
        }
        for (c in 0..8) {
            if (c != column) {
                checkValidityOfField(row, c)
            }
        }
        val squareInRow = (column / fieldsInRowInSquare).toInt()
        val squareInColumn = (row / fieldsInRowInSquare).toInt()
        for (r in 0 until fieldsInRowInSquare) {
            for (c in 0 until fieldsInRowInSquare) {
                val calcR = squareInColumn * fieldsInRowInSquare + r
                val calcC = squareInRow * fieldsInRowInSquare + c
                if (calcR != row && calcC != column) {
                    checkValidityOfField(calcR, calcC)
                }
            }
        }
    }


    fun checkValidityOfField(row : Int, column : Int) : Boolean {
        val wasValid = validity[row][column]
        var isValid = true
        if (grid[row][column] == -1) {
            isValid = true
            validity[row][column] = isValid
            if (!wasValid) {
                checkAllRelated(row, column)
            }
            return true                 // Not filled
        }
        for (r in 0..8) {
            if (r != row && grid[r][column] == grid[row][column]) {  // Equal value in same column
                isValid = false
                validity[r][column] = false
            }
        }
        for (c in 0..8) {
            if (c != column && grid[row][c] == grid[row][column]) { // Equal value in same row
                isValid = false
                validity[row][c] = false
            }
        }
        val squareInRow = (column / fieldsInRowInSquare).toInt()
        val squareInColumn = (row / fieldsInRowInSquare).toInt()
        for (r in 0 until fieldsInRowInSquare) {            // Equal value in square
            for (c in 0 until fieldsInRowInSquare) {
                val calcR = squareInColumn * fieldsInRowInSquare + r
                val calcC = squareInRow * fieldsInRowInSquare + c
                if ((calcR != row && calcC != column) && grid[row][column] == grid[calcR][calcC]) {
                    isValid = false
                    validity[calcR][calcC] = false
                }
            }
        }

        validity[row][column] = isValid
        if (isValid && !wasValid) {
            checkAllRelated(row, column)
        }
        return isValid             // No inconsistency found
    }

    fun checkValidityOfAll(): Boolean {
        var isValid = true
        for (r in 0 until fieldsInRow) {
            for (c in 0 until fieldsInRow) {
                if (!checkValidityOfField(r, c)) {
                    isValid = false
                }
            }
        }
        return isValid
    }

    fun isFinished(): Boolean {
        var isFinished = true
        for (r in 0 until fieldsInRow) {
            for (c in 0 until fieldsInRow) {
                if (-1 == grid[r][c]) {
                    isFinished = false
                    break
                }
            }
        }
        if (isFinished) {
            isFinished = checkValidityOfAll()
        }
        return isFinished
    }

    fun hasEqualState(other : Puzzle) : Boolean {
        var equal = true
        for (r in 0 until fieldsInRow) {
            for (c in 0 until fieldsInRow) {
                if (grid[r][c] != other.getField(r, c)) {
                    equal = false
                    break
                }
            }
        }
        return equal
    }

}