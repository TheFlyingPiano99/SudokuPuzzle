package bme.mobweb.lab.sudoku.model

import android.util.Log
import java.lang.RuntimeException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.timer
import kotlin.concurrent.withLock
import kotlin.random.Random


class Solver(handler : FinishHandler) {
    private var puzzle : Puzzle? = null
    private val puzzleLock = ReentrantLock()
    private val finishedLock = ReentrantLock()
    private val workingLock = ReentrantLock()
    private val killLock = ReentrantLock()
    private var finished = false
    private var working = false
    private val maxAllowedIteration = 1000
    private var timerStart = -1L

    private var kill = false
        get () {
            return killLock.withLock {
                return@withLock field
            }
        }
        set(value) {
            killLock.withLock {
                field = value
            }
        }
    private var handler = handler
    private var workThread : Thread? = null     // Only one workThread is acceptable at the same time!


    fun initNewPuzzle() {
        if (isWorking()) {
            kill = true
            workThread?.join()
        }
        workThread = Thread {
            generatingAlgorithm()
        }
        workThread?.start()
    }

    fun setSelectedPuzzle(puzzle : Puzzle) {
        if (isWorking()) {
            kill = true
            workThread?.join()
        }
        if (puzzle == this.puzzle) {
            return
        }
        setPuzzle(puzzle)
        checkValidity()
        setFinished(puzzle.isFinished())
    }


    fun solvePuzzle(handler : FinishHandler) {
        if (puzzle == null || isWorking() || puzzle?.isFinished() == true) {
            return
        }
        setWorking(true)
        setFinished(false)
        kill = false
        workThread?.join()  // For safety
        workThread = Thread {
            val solution = solvingAlgorithm(getPuzzle()!!,true)
            if (solution != null) {
                setPuzzle(solution)
                handler.onFinishedSolving()
            }
        }
        workThread?.start()
    }

    private fun solvingAlgorithm(toSolve : Puzzle, tellUpdates : Boolean) : Puzzle? {
        toSolve.ereaseVariables()
        var r = 0
        var c = 0
        var path = ArrayList<Puzzle>()

        val minValue = 1
        val maxValue = 9

        var iteration = 0
        var localFinished = false
        while (!localFinished && iteration < maxAllowedIteration) {
            if (c == 8 && r == 8) {
                Log.d("Solver", "End of line.")
            }
            if (kill) {
                setWorking(false)
                kill = false
                break
            }
            var validCandidate = true
            if (!toSolve.getEvidence(r, c)) {
                toSolve.setFieldAsVariable(r, c, -1)
                iteration++
                validCandidate = false
                var candidate = minValue
                while (candidate <= maxValue && !validCandidate) {
                    // try to insert number [1..9]
                    toSolve.setFieldAsVariable(r, c, candidate)
                    validCandidate = toSolve.checkValidityOfField(r, c, true)
                    if (validCandidate) {    // set value
                        for (p in path) {    // Check if has been
                            if (toSolve.hasEqualState(p)) {
                                validCandidate = false
                                break
                            }
                        }
                        if (validCandidate) {
                            path.add(Puzzle(toSolve))
                            if (tellUpdates) {
                                setPuzzle(toSolve)
                                handler.onStateChange()
                            }
                            break
                        }
                    }
                    candidate++
                }
            }
            if (validCandidate) {    // proceed to next grid element
                var isEvidence = true
                while (isEvidence) {
                    if (c < toSolve.getNumberOgFieldsInColumn() - 1) {
                        c++
                    } else if (c == toSolve.getNumberOgFieldsInColumn() - 1) {    // in last row
                        if (r == toSolve.getNumberOgFieldsInRow() - 1) {    // Finished
                            setFinished(true)
                            setWorking(false)
                            return toSolve
                        }
                        c = 0
                        r++
                    }
                    isEvidence = toSolve.getEvidence(r, c)
                }
            } else {                // backtrack
                toSolve.setFieldAsVariable(r, c, -1)
                var isEvidence = true
                while (isEvidence) {
                    if (c == 0) {
                        if (r == 0) {
                            setWorking(false)
                            return null // no solution!!!
                        }
                        c = toSolve.getNumberOgFieldsInColumn() - 1
                        r--
                    } else {
                        c--
                    }
                    isEvidence = toSolve.getEvidence(r, c)
                }
            }
        }
        setWorking(false)
        return null
    }

    private fun generatingAlgorithm() {
        var initVal : Puzzle? = null
        val noOfEvidences = 25
        var noOfPreEvidence = 3
        var generationAttempt = 0
        val maxGenerationAttempts = 2
        setFinished(false)
        while (!isFinished()) {
            initVal = Puzzle()
            if (generationAttempt > 5) {
                noOfPreEvidence = 1
                Log.d("Solver", "Too many generation attempts.")
            }
            var e = 0
            while (e < noOfPreEvidence) {
                val r = Random.nextInt(0, 9)
                val c = Random.nextInt(0, 9)
                val v = Random.nextInt(1, 10)
                try {
                    initVal.setFieldAsVariable(r, c, v)
                    if (initVal.checkValidityOfField(r, c, true)) {
                        initVal.setEvidence(r, c, true)
                        e++
                    }
                    else {
                        initVal.setFieldAsVariable(r, c, -1)
                        initVal.checkValidityOfField(r, c, true)
                    }
                }
                catch (e : RuntimeException) {
                    Log.d("Solver", e.message.toString())
                }
            }
            initVal = solvingAlgorithm(initVal, false)
            generationAttempt++
        }

        var e = noOfPreEvidence
        while (e < noOfEvidences) {
            val r = Random.nextInt(0, 9)
            val c = Random.nextInt(0, 9)
            if (initVal?.getEvidence(r, c) == false) {
                initVal.setEvidence(r, c, true)
                e++
            }
        }
        initVal?.ereaseVariables()
        setFinished(false)
        setPuzzle(initVal)
        handler.onFinishedGenerating(initVal!!)
    }

    fun getPuzzle() : Puzzle? {
        return when (puzzle) {
            null -> null
            else -> Puzzle(puzzle!!)
        }
    }

    fun setPuzzle(p : Puzzle?) {
        puzzle = when (p) {
            null -> null
            else -> Puzzle(p)
        }
    }

    fun checkValidity() : Boolean? {
        return getPuzzle()?.checkValidityOfAll()
    }

    fun isFinished() : Boolean {
        return finished
    }

    fun setFinished(b : Boolean) {
        finished = b
    }

    fun isWorking() : Boolean {
        return working
    }

    private fun setWorking(b : Boolean){
        working = b
    }

    fun stop() {
        kill = true
        workThread?.join()
    }

    fun unload() {
        if (isWorking()) {
            kill = true
            workThread?.join()
        }
        setPuzzle(null)
    }

    fun resetTimer() {
        timerStart = -1L
    }

    fun startTimer() {
        if (puzzle != null && puzzle?.isFinished() == false) {
            timerStart = System.currentTimeMillis()
        }
    }

    fun stopTimer() : Long {
        if (timerStart > 0) {
            return System.currentTimeMillis() - timerStart
        }
        else {
            return 0L
        }
    }

    fun tryToEraseVariables() {
        if (isWorking()) {
            return
        }
        workThread?.join()
        val p = getPuzzle()
        p?.ereaseVariables()
        setPuzzle(p)
    }

    fun setFieldAsVariable(row : Int, column : Int, value : Int) {
        val p = getPuzzle()
        p?.setFieldAsVariable(row, column, value)
        p?.checkValidityOfField(row, column, true)
        setPuzzle(p)
    }

    interface FinishHandler {
        fun onStateChange()
        fun onFinishedSolving()
        fun onFinishedGenerating(puzzle : Puzzle)
    }

}