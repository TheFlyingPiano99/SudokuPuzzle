package bme.mobweb.lab.sudoku.customView

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.text.InputType
import android.view.View
import android.widget.EditText
import bme.mobweb.lab.sudoku.R
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.*

class DialogBuilder(context : Context) {
    private val context = context

    fun buildFieldValueInput(view : View, row : Int, column : Int,
                                        getFieldOfCurrentPuzzle : (row : Int, column : Int)->Int,
                                        setFieldOfCurrentPuzzle : (row : Int, column : Int, value : Int)->Unit,
                                        onInvalideEntered : () -> Unit
                            ): AlertDialog.Builder? {
        val builder: AlertDialog.Builder? = context.let {
            AlertDialog.Builder(it)
        }
        val editText = EditText(context)
        editText.setInputType(InputType.TYPE_CLASS_NUMBER)
        val prevStr = when (val prevVal = getFieldOfCurrentPuzzle(row, column)) {
            -1 -> ""
            else -> prevVal.toString()
        }
        editText.setText(prevStr)
        builder?.setView(editText)
        builder?.setPositiveButton(
            R.string.submit
        ) { _, _ ->
            val str = editText.text.toString()
            val value = when (str) {
                "" -> -1
                else -> str.toInt()
            }
            setFieldOfCurrentPuzzle(
                row,
                column,
                value
            )
            view.invalidate()
        }
        return builder
    }

    fun buildFinishAlert(timeSpentSolving : Date) : AlertDialog.Builder? {
        val formatter = SimpleDateFormat("HH:mm:ss")
        val stringBuilder = StringBuilder("Time spent solving: ").append(formatter.format(timeSpentSolving))
        val builder: AlertDialog.Builder? = context.let {
            AlertDialog.Builder(it)
        }
        builder?.setTitle(context.getString(R.string.puzzleSolved))
        builder?.setMessage(stringBuilder.toString())
        //plus(formatter.format(Date(Date(System.currentTimeMillis()).time - (solver.getPuzzle()!!.timeCreated).time)))
        builder?.setPositiveButton(context.getString(R.string._continue), DialogInterface.OnClickListener { _, _ -> })
        return builder
    }
}