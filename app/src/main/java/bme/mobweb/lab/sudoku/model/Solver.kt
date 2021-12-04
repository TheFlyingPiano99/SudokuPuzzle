package bme.mobweb.lab.sudoku.model

import android.util.Log
import java.lang.RuntimeException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.random.Random


class Solver(private var handler: FinishHandler) {
    @Volatile private var puzzle : Puzzle? = null
    private val puzzleLock = ReentrantLock()
    private val finishedLock = ReentrantLock()
    private val workingLock = ReentrantLock()
    private val killLock = ReentrantLock()
    private val maxAllowedIteration = 1000
    private var timerStart = -1L

    var finished : Boolean = false
        get() {
            return finishedLock.withLock {
                return@withLock field
            }
        }
        private set(value) {
            finishedLock.withLock {
                field = value
            }
        }

    var working : Boolean = false
        get() {
            return workingLock.withLock {
                return@withLock field
            }
        }
        private set(value) {
            workingLock.withLock {
                field = value
            }
        }

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
    private var workThread : Thread? = null     // Only one workThread is acceptable at the same time!


    fun initNewPuzzle() {
        if (working) {
            kill = true
            workThread?.join()
        }
        workThread = Thread {
            puzzleLock.withLock {
                generatingAlgorithm()
            }
        }
        workThread?.start()
    }

    fun setSelectedPuzzle(puzzle : Puzzle) {
        if (working) {
            kill = true
            workThread?.join()
        }
        puzzleLock.withLock {
            if (puzzle == this.puzzle) {
                return
            }
            setPuzzle(puzzle)
            checkValidity()
            finished = puzzle.isFinished()
        }
    }


    fun solvePuzzle(handler : FinishHandler) {
        if (puzzle == null || working || puzzle?.isFinished() == true) {
            return
        }
        working = true
        finished = false
        kill = true
        workThread?.join()  // For safety
        kill = false
        workThread = Thread {
            puzzleLock.withLock {
            val solution = solvingAlgorithm(getPuzzle()!!,true)
            if (solution != null) {
                    if (solution != null) {
                        setPuzzle(solution)
                        handler.onFinishedSolving()
                    }
                }
            }
        }
        workThread?.start()
    }

    private fun solvingAlgorithm(toSolve : Puzzle, tellUpdates : Boolean) : Puzzle? {
        toSolve.ereaseVariables()
        var r = 0
        var c = 0
        val path = ArrayList<Puzzle>()

        val minValue = 1
        val maxValue = 9

        var iteration = 0
        val localFinished = false
        while (!localFinished && iteration < maxAllowedIteration) {
            var localKill = false
            if (kill) {
                workingLock.withLock {
                    if (kill) {
                        working = false
                        kill = false
                        localKill = true
                    }
                }
            }
            if (localKill) {
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
                                toSolve.checkValidityOfAll()
                                puzzleLock.withLock {
                                    setPuzzle(toSolve)
                                    handler.onStateChange()
                                }
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
                            finished = true
                            working = false
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
                            working = false
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
        working = false
        return null
    }

    private fun generatingAlgorithm() {
        var initVal : Puzzle? = null
        val noOfEvidences = 25
        var noOfPreEvidence = 3
        var generationAttempt = 0
        val maxGenerationAttempts = 5
        finished = false
        while (!finished) {
            initVal = Puzzle()
            if (generationAttempt > maxGenerationAttempts) {
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
        initVal?.checkValidityOfAll()
        finished = false
        puzzleLock.withLock {
            setPuzzle(initVal)
            handler.onFinishedGenerating(initVal!!)
        }
    }

    fun getPuzzle() : Puzzle? {
        return when (puzzle) {
            null -> null
            else -> Puzzle(puzzle!!)
        }
    }

    private fun setPuzzle(p : Puzzle?) {
        puzzleLock.withLock {
            puzzle = when (p) {
                null -> null
                else -> Puzzle(p)
            }
        }
    }


    private fun checkValidity() : Boolean? {
        puzzleLock.withLock {
            return getPuzzle()?.checkValidityOfAll()
        }
    }

    fun stop() {
        kill = true
        workThread?.join()
    }

    fun unload() {
        if (working) {
            kill = true
            workThread?.join()
        }
        puzzleLock.withLock {
            setPuzzle(null)
        }
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
        return if (timerStart > 0) {
            System.currentTimeMillis() - timerStart
        } else {
            0L
        }
    }

    // Called from UI
    // Usage not implemented
    fun tryToEraseVariables() {
        if (working) {
            return
        }
        workThread?.join()
        puzzleLock.withLock {
            val p = getPuzzle()
            p?.ereaseVariables()
            setPuzzle(p)
        }
    }

    fun setFieldAsVariable(row : Int, column : Int, value : Int) : Boolean? {
        var isValid : Boolean? = false
        puzzleLock.withLock {
            val p = getPuzzle()
            p?.setFieldAsVariable(row, column, value)
            isValid = p?.checkValidityOfField(row, column, true)
            setPuzzle(p)
        }
        return isValid
    }

    fun addToSolvingTime(delta : Long) {
        puzzleLock.withLock {
            val p = getPuzzle()
            p?.addToSolvingTime(delta)
            setPuzzle(p)
        }
    }

    fun giveHint() {
        if (!working) {
            puzzleLock.withLock {
                if (!working) {
                    working = true
                    workThread = Thread  {
                        val toHint = getPuzzle()
                        if (toHint?.isFinished() != false) {
                            working = false
                            return@Thread
                        }
                        val solved = hintAlgorithm(toHint)
                        setPuzzle(solved)
                        handler.onFinishedGeneratingHint(solved)
                        working = false
                    }
                    workThread?.start()
                }
            }
        }
        else {
            Log.d("Solver", "Too busy generating hints.")
        }
    }

    private fun hintAlgorithm(toHint : Puzzle) : Puzzle {
        kill = false
        val solved = solvingAlgorithm(Puzzle(toHint), false)
            ?: throw RuntimeException("Failed to solve puzzle, while trying to generate hint.")
        var r: Int
        var c: Int
        while (true) {
            r = Random.nextInt(0, 9)
            c = Random.nextInt(0, 9)
            if (toHint.getValue(r, c) == -1 && !toHint.getEvidence(r, c)) {
                toHint.setFieldAsVariable(r, c, solved.getValue(r, c))
                break
            }
        }
        return toHint
    }

    interface FinishHandler {
        fun onStateChange()
        fun onFinishedSolving()
        fun onFinishedGenerating(puzzle : Puzzle)
        fun onFinishedGeneratingHint(puzzle : Puzzle)
    }

}