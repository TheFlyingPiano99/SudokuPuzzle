package bme.mobweb.lab.sudoku.model

import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class ConcurrentPuzzle(from : Puzzle, puzzleLock : Lock) {
    private val puzzle = from
    private val lock = puzzleLock
    val ID = puzzle.ID

    fun getField(row : Int, column : Int) : Int {
        return lock.withLock {
            return@withLock puzzle.getField(row, column)
        }
    }

    fun getEvidence(row : Int, column : Int) : Boolean {
        return lock.withLock {
            return@withLock puzzle.getEvidence(row, column)
        }
    }

    fun getValidity(row : Int, column : Int) : Boolean {
        return lock.withLock {
            return@withLock puzzle.getValidity(row, column)
        }
    }

    fun setFieldAsVariable(row : Int, column : Int, value : Int) {
        lock.withLock {
            puzzle.setFieldAsVariable(row, column, value)
        }
    }

    fun checkValidityOfField(row : Int, column : Int) : Boolean {
        return lock.withLock {
            return@withLock puzzle.checkValidityOfField(row, column)
        }
    }

    fun isFinished() : Boolean {
        return lock.withLock {
            return@withLock puzzle.isFinished()
        }
    }

    fun ereaseVariables() {
        lock.withLock {
            puzzle.ereaseVariables()
        }
    }

    fun getCopyOfPuzzle() : Puzzle {
        return lock.withLock {
            return@withLock Puzzle(puzzle)
        }
    }

}
