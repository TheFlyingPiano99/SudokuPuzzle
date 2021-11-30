package bme.mobweb.lab.sudoku.customView

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.text.InputType
import android.view.View
import android.widget.EditText
import bme.mobweb.lab.sudoku.R
import java.text.SimpleDateFormat

class DialogBuilder(context : Context) {
    private val context = context

    fun buildFieldValueInput(view : View, row : Int, column : Int,
                                     getFieldOfCurrentPuzzle : (row : Int, column : Int)->Int,
                                     setFieldOfCurrentPuzzle : (row : Int, column : Int, value : Int)->Unit): AlertDialog.Builder? {
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

    fun buildFinishAlert() : AlertDialog.Builder? {
        val formatter = SimpleDateFormat("HH:mm:ss")
        val builder: AlertDialog.Builder? = context.let {
            AlertDialog.Builder(it)
        }
        builder?.setTitle(context.getString(R.string.puzzleSolved))
        //plus(formatter.format(Date(Date(System.currentTimeMillis()).time - (solver.getPuzzle()!!.timeCreated).time)))
        builder?.setPositiveButton(context.getString(R.string._continue), DialogInterface.OnClickListener { _, _ -> })
        builder?.create()
        return builder
    }
}