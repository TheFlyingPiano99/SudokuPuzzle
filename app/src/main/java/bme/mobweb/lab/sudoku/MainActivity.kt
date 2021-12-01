package bme.mobweb.lab.sudoku

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import bme.mobweb.lab.sudoku.customView.DialogBuilder
import bme.mobweb.lab.sudoku.databinding.ActivityMainBinding
import bme.mobweb.lab.sudoku.model.Solver
import bme.mobweb.lab.sudoku.model.Puzzle
import bme.mobweb.lab.sudoku.model.Settings
import com.google.android.material.snackbar.Snackbar
import hu.bme.mobweb.lab.sudoku.sqlite.PersistentDataHelper
import java.lang.RuntimeException
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity(), PuzzleFragment.PuzzleHolder, Solver.FinishHandler, SelectFragment.PuzzleListHolder, SettingsFragment.SettingsListener {
    private companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private lateinit var persistentDataHelper : PersistentDataHelper
    private var puzzles : MutableList<Puzzle> = ArrayList()
    private var solver = Solver(this)
    private var invalidatePuzzleViewFunction : (() -> Unit)? = null
    private var updatePuzzleAdapter : ((puzzles : List<Puzzle>) -> Unit)? = null
    private val dialogBuilder = DialogBuilder(this)
    private val settings = Settings()
    private var prevTimeViewInvalidated : Long = 0L



    // Private functions:---------------------------------------------------------------

    private fun loadLatestPuzzle() {
        solver.stop()
        if (puzzles.size > 0) {
            val sorted = puzzles.sortedBy { p -> p.timeCreated }
            setSelectedPuzzle(sorted.last())
        }
    }





    // Activity lifecycle implementation:-----------------------------------------------

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
            R.id.action_settings ->
            {
                false   // Delegate to fragments
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    override fun onPause() {
        super.onPause()
        persistentDataHelper.persistTables(puzzles)
    }




    // PuzzleHolder implementation:----------------------------------------------------

    override fun solveCurrentPuzzle() {
        solver.solvePuzzle(this)
    }

    override fun initNewPuzzle() {
        solver.initNewPuzzle()
        solver.resetTimer()
        solver.startTimer()
    }

    override fun getCurrentPuzzle(): Puzzle? {
        return solver.getPuzzle()
    }


    private fun setFieldOfCurrentPuzzle(row: Int, column: Int, value: Int) {
        try {
            solver.setFieldAsVariable(row, column, value)

            if (solver.getPuzzle()?.isFinished() == true && !solver.isFinished()) {
                dialogBuilder.buildFinishAlert()?.show()
            }
        }
        catch (e : RuntimeException) {
            Log.e(TAG, e.message.toString())
            Snackbar.make(binding.root, e.message.toString(), Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun clearCurrentPuzzle() {
        solver.tryToEraseVariables()
    }

    override fun getNotifiedAboutSelection(row: Int, column: Int, view : View) {
        if (solver.getPuzzle() == null || solver.isWorking() || solver.getPuzzle()?.getEvidence(row, column) == true) {
            return      // Perform no action
        }
        dialogBuilder.buildFieldValueInput(view, row, column,
            {r, c ->
                val p = solver.getPuzzle()
                when (p) {
                    null -> -1
                    else -> p.getValue(r, c)
            }},
            {r, c, v -> setFieldOfCurrentPuzzle(r, c, v)}
        )?.show()
    }

    override fun setInvalidateViewFunction(f: () -> Unit) {
        invalidatePuzzleViewFunction = f
    }

    override fun continueSolving() {
        solver.resetTimer()
        if (solver.getPuzzle() != null) {
            solver.startTimer()
        }
    }

    override fun breakSolving() {
        solver.stop()
        val delta = solver.stopTimer()
        val p = solver.getPuzzle()
        p?.addToSolvingTime(delta)
        solver.setPuzzle(p)
    }


    // FinishHandler implementation:----------------------------------------------------------

    override fun onStateChange() {
        val currentTime = System.currentTimeMillis()
        if ((currentTime - prevTimeViewInvalidated) > 500L) {
            prevTimeViewInvalidated = currentTime
            runOnUiThread {
                invalidatePuzzleViewFunction?.let { it() }
            }
        }
    }

    override fun onFinishedSolving() {
        val copy = solver.getPuzzle()
        copy?.addToSolvingTime(solver.stopTimer())
        solver.resetTimer()
        val toReplace = puzzles.find { p -> p.ID == copy?.ID }
        if (toReplace != null && copy != null) {
            puzzles.remove(toReplace)
            puzzles.add(copy)
        }
        puzzles.sortBy { p -> p.timeCreated }
        runOnUiThread {
            Snackbar.make(binding.root, "Solver finished the puzzle.", Snackbar.LENGTH_SHORT).show()
            invalidatePuzzleViewFunction?.let { it() }
        }
    }

    override fun onFinishedGenerating(puzzle : Puzzle) {
        puzzles.add(puzzle)
        solver.resetTimer()
        runOnUiThread {
            invalidatePuzzleViewFunction?.let { it() }
            updatePuzzleAdapter?.let { it(puzzles) }
            Snackbar.make(binding.root, "New puzzle created.", Snackbar.LENGTH_SHORT).show()
        }
    }

    // PuzzleListItemListener implementation:------------------------------------------------------
    override fun getPuzzleList(): List<Puzzle> {
        return puzzles
    }

    override fun setSelectedPuzzle(puzzle: Puzzle) {
        solver.setSelectedPuzzle(puzzle)
    }

    override fun removePuzzle(puzzle: Puzzle) {
        puzzles.remove(puzzle)
        if (solver.getPuzzle()?.ID == puzzle.ID) {
            solver.unload()
            loadLatestPuzzle()
        }
    }

    override fun setUpdateAdapterFunction(f: (puzzles: List<Puzzle>) -> Unit) {
        updatePuzzleAdapter = f
    }

    override fun getSettings(): Settings {
        return settings
    }


    // SettingsListener implementation:-------------------------------------------------------
    override fun onHintSettingChanged(newValue: Boolean) {
        settings.hints = newValue

    }

    override fun onDarkSettingChanged(newValue: Boolean) {
        settings.darkTheme = newValue
        if (settings.darkTheme) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            delegate.applyDayNight()
        }
        else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            delegate.applyDayNight()
        }
    }

    override fun onDeletePuzzles() {
        solver.stop()
        solver.resetTimer()
        solver.unload()
        puzzles.clear()
        Snackbar.make(binding.root, "All puzzles deleted.", Snackbar.LENGTH_SHORT).show()
    }

}