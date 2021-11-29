package bme.mobweb.lab.sudoku.model

import android.util.Log
import java.util.concurrent.ThreadFactory
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock

class WorkThreadFactory : ThreadFactory {

    override fun newThread(r: Runnable?): Thread {
        return Thread {
            r?.run()
        }
    }

}


class Solver : Runnable {
    private var puzzle : Puzzle? = null
    private val puzzleLock = ReentrantLock()
    private val finishedLock = ReentrantLock()
    private val workingLock = ReentrantLock()
    private val killLock = ReentrantLock()
    private var finished = false
    private var working = false
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
    private var handler : FinishHandler? = null
    private val workThreadFactory = WorkThreadFactory()
    private var workThread : Thread? = null

    fun initNewPuzzle() : Puzzle {
        if (isWorking()) {
            kill = true
            workThread?.join()
        }
        val retVal = Puzzle()
        //TODO
        //Generate puzzle.

        setPuzzle(retVal)
        setFinished(false)
        return retVal
    }

    fun setPuzzle(p : Puzzle?) {
        puzzleLock.withLock {
            puzzle = p
        }
    }

    fun solvePuzzle(handler : FinishHandler) {
        if (isFinished() || isWorking()) {
            return
        }
        setWorking(true)
        setFinished(false)
        kill = false
        this.handler = handler
        workThread = workThreadFactory.newThread(this)
        workThread?.start()
    }

    private fun solvingAlgorithm() {
        if (getPuzzle() == null) {
            return
        }
        var toSolve = Puzzle(getPuzzle()!!)

        //srand(0)
        /*
    for (int r = 0; r < toSolve.getNumberOgFieldsInRow(); r++) {
        for (int c = 0; c < toSolve.getNumberOgFieldsInColumn(); c++) {
            grid[r][c] = rand() % 9 + 1;
        }
    }
    */
        /*
    for (int r = 0; r < toSolve.getNumberOgFieldsInRow(); r++) {
        for (int c = 0; c < toSolve.getNumberOgFieldsInColumn(); c++) {
            grid[r][c] = rand() % 9 + 1;
        }
    }
    */

        var r = 0
        var c = 0
        var path = ArrayList<Puzzle>()

        val minValue = 1
        val maxValue = 9

        var iteration = 0
        var localFinished = false
        while (!localFinished) {
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
                /*
                var depthPushCounter = 0
                if (depthPushCounter == DEPTH_RESOLUTION) {
                    depth.push_back(r * toSolve.getNumberOgFieldsInColumn() + c)
                    depthPushCounter = 0
                } else {
                    depthPushCounter++
                }
                * */
                iteration++
                validCandidate = false
                var candidate = minValue
                while (candidate <= maxValue && !validCandidate) {
                    // try to insert number [1..9]
                    toSolve.setFieldAsVariable(r, c, candidate)
                    validCandidate = toSolve.checkValidityOfField(r, c)
                    if (validCandidate) {    // set value
                        for (p in path) {    // Check if has been
                            if (toSolve.hasEqualState(p)) {
                                validCandidate = false
                                break
                            }
                        }
                        if (validCandidate) {
                            path.add(Puzzle(toSolve))
                            setPuzzle(Puzzle(toSolve))
                            handler?.getNotifiedAboutStateChange()
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
                            setPuzzle(Puzzle(toSolve))
                            setFinished(true)
                            setWorking(false)
                            handler?.getNotifiedAboutFinish()
                            return
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
                            return  // no solution!!!
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
    }

    fun getPuzzle() : Puzzle? {
        puzzleLock.withLock {
            return puzzle
        }
    }

    fun checkValidity() : Boolean? {
        return getPuzzle()?.checkValidityOfAll()
    }

    fun isFinished() : Boolean {
        finishedLock.withLock {
            return finished
        }
    }

    fun setFinished(b : Boolean) {
        finishedLock.withLock {
            finished = b
        }
    }

    fun isWorking() : Boolean {
        workingLock.withLock {
            return working
        }
    }

    fun setWorking(b : Boolean){
        workingLock.withLock {
            working = b
        }
    }

    interface FinishHandler {
        fun getNotifiedAboutStateChange()
        fun getNotifiedAboutFinish()
    }

    fun stop() {
        kill = true
        workThread?.join()
    }

    override fun run() {
        solvingAlgorithm()
    }

}