package bme.mobweb.lab.sudoku

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.os.Looper
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
import bme.mobweb.lab.sudoku.databinding.ActivityMainBinding
import bme.mobweb.lab.sudoku.model.Solver
import bme.mobweb.lab.sudoku.model.Puzzle
import com.google.android.material.snackbar.Snackbar
import hu.bme.mobweb.lab.sudoku.sqlite.PersistentDataHelper
import java.lang.RuntimeException

class MainActivity : AppCompatActivity(), PuzzleFragment.PuzzleHolder, Solver.FinishHandler {
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

    override fun setFieldOfCurrentPuzzle(row: Int, column: Int, value: Int) {
        try {
            solver.getPuzzle()!!.setFieldAsVariable(row, column, value)
            solver.getPuzzle()!!.checkValidityOfField(row, column)
        }
        catch (e : RuntimeException) {
            Log.e(TAG, e.message.toString())
            Snackbar.make(binding.root, e.message.toString(), Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun getValidityOfFieldOfCurrentPuzzle(row: Int, column: Int): Boolean {
        return solver.getPuzzle()!!.getValidity(row, column)
    }

    override fun solveCurrentPuzzle() {
        solver.solvePuzzle(this)
    }

    override fun clearCurrentPuzzle() {
        solver.getPuzzle()!!.clearGrid()
    }

    override fun getNotifiedAboutSelection(row: Int, column: Int, view : View) {
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
                    dialog, id ->
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
        builder?.show()

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
        }
    }


}