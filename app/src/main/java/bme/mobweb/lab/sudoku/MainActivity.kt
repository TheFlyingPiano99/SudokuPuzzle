package bme.mobweb.lab.sudoku

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.InputType
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import bme.mobweb.lab.sudoku.adapter.PuzzleAdapter
import bme.mobweb.lab.sudoku.customView.DialogBuilder
import bme.mobweb.lab.sudoku.databinding.ActivityMainBinding
import bme.mobweb.lab.sudoku.model.Solver
import bme.mobweb.lab.sudoku.model.Puzzle
import com.google.android.material.snackbar.Snackbar
import hu.bme.mobweb.lab.sudoku.sqlite.PersistentDataHelper
import java.lang.RuntimeException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity(), PuzzleFragment.PuzzleHolder, Solver.FinishHandler, SelectFragment.PuzzleListHolder {
    private companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private lateinit var persistentDataHelper : PersistentDataHelper
    private var puzzles : MutableList<Puzzle> = ArrayList()
    private var solver = Solver()
    private lateinit var invalidatePuzzleViewFunction : (() -> Unit)
    private val dialogBuilder = DialogBuilder(this)
    private var prevTime : Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        persistentDataHelper = PersistentDataHelper(this)
        persistentDataHelper.open()
        puzzles = persistentDataHelper.restoreTables()
        loadLatestPuzzle()

        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    override fun onPause() {
        super.onPause()
        solver.stop()
        persistentDataHelper.persistTables(puzzles)
    }

    override fun onDestroy() {
        super.onDestroy()
        persistentDataHelper.close()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }



    override fun initNewPuzzle() {
        puzzles.add(solver.initNewPuzzle())
    }

    override fun getFieldOfCurrentPuzzle(row: Int, column: Int): Int {
        return solver.getPuzzleAsConcurrent().getField(row, column)
    }

    private fun setFieldOfCurrentPuzzle(row: Int, column: Int, value: Int) {
        try {
            solver.getPuzzleAsConcurrent().setFieldAsVariable(row, column, value)
            solver.getPuzzleAsConcurrent().checkValidityOfField(row, column)

            if (solver.getPuzzleAsConcurrent().isFinished() && !solver.isFinished()) {
                dialogBuilder.buildFinishAlert()?.show()
            }
        }
        catch (e : RuntimeException) {
            Log.e(TAG, e.message.toString())
            Snackbar.make(binding.root, e.message.toString(), Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun getValidityOfFieldOfCurrentPuzzle(row: Int, column: Int): Boolean {
        return solver.getPuzzleAsConcurrent().getValidity(row, column)
    }

    override fun getEvidenceOfFieldOfCurrentPuzzle(row: Int, column: Int): Boolean {
        return solver.getPuzzleAsConcurrent().getEvidence(row, column)
    }

    override fun solveCurrentPuzzle() {
        solver.solvePuzzle(this)
    }

    override fun clearCurrentPuzzle() {
        solver.getPuzzleAsConcurrent().ereaseVariables()
    }

    override fun getNotifiedAboutSelection(row: Int, column: Int, view : View) {
        if (solver.isWorking() || solver.getPuzzleAsConcurrent().getEvidence(row, column)) {
            return      // Perform no action
        }
        dialogBuilder.buildFieldValueInput(view, row, column,
            {r, c -> getFieldOfCurrentPuzzle(r, c)},
            {r, c, v -> setFieldOfCurrentPuzzle(r, c, v)}
        )?.show()
    }

    override fun setInvalidateViewFunction(f: () -> Unit) {
        invalidatePuzzleViewFunction = f
    }

    private fun loadLatestPuzzle() {
        solver.stop()
        if (puzzles.size > 0) {
            val sorted = puzzles.sortedBy { p -> p.timeCreated }
            setSelectedPuzzle(sorted.last())
        }
        else {
            initNewPuzzle()
        }
    }

    override fun getNotifiedAboutStateChange() {
        val currentTime = System.currentTimeMillis()
        if ((currentTime - prevTime) > 500L) {
            prevTime = currentTime
            runOnUiThread {
                invalidatePuzzleViewFunction?.let { it() }
            }
        }
    }

    override fun getNotifiedAboutFinish() {
        runOnUiThread {
            val toReplace = puzzles.find { p -> p.ID == solver.getPuzzleAsConcurrent().ID }
            if (toReplace != null) {
                puzzles.remove(toReplace)
                puzzles.add(solver.getPuzzleAsConcurrent().getCopyOfPuzzle())
            }
            puzzles.sortBy { p -> p.timeCreated }
            Snackbar.make(binding.root, "Solver finished the puzzle.", Snackbar.LENGTH_SHORT).show()
            invalidatePuzzleViewFunction?.let { it() }
        }
    }




    override fun getPuzzleList(): List<Puzzle> {
        return puzzles
    }

    override fun setSelectedPuzzle(puzzle: Puzzle) {
        solver.setSelectedPuzzle(puzzle)
    }

    override fun removePuzzle(puzzle: Puzzle) {
        puzzles.remove(puzzle)
        if (solver.getPuzzleID() == puzzle.ID) {
            loadLatestPuzzle()
        }
    }

}