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
    private var invalidatePuzzleViewFunction : (() -> Unit)? = null
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
        return solver.getPuzzle()!!.getField(row, column)
    }

    private fun setFieldOfCurrentPuzzle(row: Int, column: Int, value: Int) {
        try {
            solver.getPuzzle()!!.setFieldAsVariable(row, column, value)
            solver.getPuzzle()!!.checkValidityOfField(row, column)

            if (solver.getPuzzle()!!.isFinished() /*&& !solver.isFinished()*/ ) {
                buildFinishAlert()?.show()
            }
        }
        catch (e : RuntimeException) {
            Log.e(TAG, e.message.toString())
            Snackbar.make(binding.root, e.message.toString(), Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun getValidityOfFieldOfCurrentPuzzle(row: Int, column: Int): Boolean {
        return solver.getPuzzle()!!.getValidity(row, column)
    }

    override fun getEvidenceOfFieldOfCurrentPuzzle(row: Int, column: Int): Boolean {
        return solver.getPuzzle()!!.getEvidence(row, column)
    }

    override fun solveCurrentPuzzle() {
        solver.solvePuzzle(this)
    }

    override fun clearCurrentPuzzle() {
        solver.getPuzzle()!!.ereaseVariables()
    }

    override fun getNotifiedAboutSelection(row: Int, column: Int, view : View) {
        if (solver.isWorking() || solver.getPuzzle()?.getEvidence(row, column) == true) {
            return      // Perform no action
        }
        buildFieldValueInput(view, row, column)?.show()
    }

    override fun setInvalidateViewFunction(f: () -> Unit) {
        invalidatePuzzleViewFunction = f
    }

    private fun loadLatestPuzzle() {
        if (puzzles.size > 0) {
            val sorted = puzzles.sortedBy { p -> p.timeCreated }
            solver.setPuzzle(sorted.last())
            solver.checkValidity()
            solver.setFinished(false)
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
            invalidatePuzzleViewFunction?.let { it() }
            Snackbar.make(binding.root, "Solver finished the puzzle.", Snackbar.LENGTH_SHORT).show()
        }
    }


    private fun buildFieldValueInput(view : View, row : Int, column : Int): AlertDialog.Builder? {
        val builder: AlertDialog.Builder? = this.let {
            AlertDialog.Builder(it)
        }
        val editText = EditText(this)
        editText.setInputType(InputType.TYPE_CLASS_NUMBER)
        val prevVal = getFieldOfCurrentPuzzle(row, column)
        val prevStr = when (prevVal) {
            -1 -> ""
            else -> prevVal.toString()
        }
        editText.setText(prevStr)
        builder?.setView(editText)
        builder?.setPositiveButton(
            R.string.submit,
            DialogInterface.OnClickListener {
                    _, _ ->
                val str = editText.text.toString()
                val value = when (str) {
                    "" -> -1
                    else -> str.toInt()
                }
                setFieldOfCurrentPuzzle(
                    row,
                    column,
                    value)
                view.invalidate()
            })
        builder?.create()
        return builder
    }

    private fun buildFinishAlert() : AlertDialog.Builder? {
        val formatter = SimpleDateFormat("HH:mm:ss")
        val builder: AlertDialog.Builder? = this.let {
            AlertDialog.Builder(it)
        }
        builder?.setTitle(getString(R.string.puzzleSolved))
        //plus(formatter.format(Date(Date(System.currentTimeMillis()).time - (solver.getPuzzle()!!.timeCreated).time)))
        builder?.setPositiveButton(getString(R.string._continue), DialogInterface.OnClickListener { _, _ -> })
        builder?.create()
        return builder
    }

    override fun getPuzzleList(): List<Puzzle> {
        return puzzles
    }

    override fun setSelectedPuzzle(puzzle: Puzzle) {
        solver.setPuzzle(puzzle)
    }

    override fun removePuzzle(puzzle: Puzzle) {
        puzzles.remove(puzzle)
        if (solver.getPuzzle() == puzzle) {
            solver.stop()
            loadLatestPuzzle()
        }
    }

}