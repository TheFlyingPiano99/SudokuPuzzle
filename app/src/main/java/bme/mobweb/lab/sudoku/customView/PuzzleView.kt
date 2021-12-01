package bme.mobweb.lab.sudoku.customView

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import bme.mobweb.lab.sudoku.R
import bme.mobweb.lab.sudoku.model.Puzzle
import kotlin.math.min


class PuzzleView(context : Context, attributeSet : AttributeSet) : View(context, attributeSet) {
    var dataProvider : PuzzleDataProvider? = null

    private val thickLinePaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.BLACK
        strokeWidth = 10F
    }
    private val thinLinePaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.BLACK
        strokeWidth = 6F
    }

    private val fieldTextPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
        textSize = resources.getDimensionPixelSize(R.dimen.puzzleFieldFontSize).toFloat()
    }

    private val fieldNegativeTextPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = resources.getDimensionPixelSize(R.dimen.puzzleFieldFontSize).toFloat()
    }

    private val selectedFillPaint = Paint().apply {
        style = Paint.Style.FILL
        color = (0xFFD58936).toInt()
    }

    private val invalidFillPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.RED
    }

    private val invalidSelectedFillPaint = Paint().apply {
        style = Paint.Style.FILL
        color = (Color.RED + 0x00008888).toInt()
    }

    private val evidenceFillPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.GRAY
    }

    private val invalidEvidenceFillPaint = Paint().apply {
        style = Paint.Style.FILL
        color = (Color.GRAY - 0x00008888).toInt()
    }

    private val fieldsInRow = 9
    private val fieldsInRowInSquare = 3
    private var fieldPixelSize = 0F

    private var selectedRow = -1
    private var selectedColumn = -1


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val sizePixels = min(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(sizePixels, sizePixels)
    }
    override fun onDraw(canvas : Canvas) {
        fieldPixelSize = (width / fieldsInRow).toFloat()
        super.onDraw(canvas)
        val puzzle = dataProvider?.getPuzzle()
        drawFieldBackgrounds(canvas, puzzle)
        drawLines(canvas)
        drawValues(canvas, puzzle)
    }

    private fun drawFieldBackgrounds(canvas : Canvas, puzzle : Puzzle?) {
        for (r in 0 until fieldsInRow) {
            for (c in 0 until fieldsInRow) {
                val selected = (selectedRow == r && selectedColumn == c)
                val evidence = puzzle?.getEvidence(r, c)
                val paint = when (puzzle?.getValidity(r, c)) {
                    true -> when (selected) {
                        true -> when (evidence) {
                            true -> evidenceFillPaint
                            else -> selectedFillPaint
                        }
                        false -> when (evidence) {
                            true -> evidenceFillPaint
                            else -> null
                        }
                    }
                    false -> when (selected) {
                        true -> when(evidence) {
                            true -> invalidEvidenceFillPaint
                            else -> invalidSelectedFillPaint
                        }
                        false -> when (evidence) {
                            true -> invalidEvidenceFillPaint
                            else -> invalidFillPaint
                        }
                        else -> null
                    }
                    else -> null
                }
                if (paint != null) {
                    canvas.drawRect(
                        c * fieldPixelSize,
                        r * fieldPixelSize,
                        (c + 1) * fieldPixelSize,
                        (r + 1) * fieldPixelSize,
                        paint)
                }
            }
        }
    }

    private fun getNightMode() : Boolean {
        val nightMode = context.resources.configuration.uiMode and
        Configuration.UI_MODE_NIGHT_MASK
        return when(nightMode) {
            Configuration.UI_MODE_NIGHT_YES -> true
            Configuration.UI_MODE_NIGHT_NO -> false
            else -> false
        }
    }

    private fun drawValues(canvas : Canvas, puzzle : Puzzle?) {
        val isNightMode = getNightMode()
        for (r in 0..8) {
            for (c in 0..8) {
                val value = puzzle?.getValue(r, c)
                val stringValue = when (value) {
                    null -> ""
                    -1 -> ""
                    else -> value.toString()
                }
                val paint = when (puzzle?.getEvidence(r, c)) {
                    true -> fieldTextPaint
                    else -> when(isNightMode) {
                        true -> fieldNegativeTextPaint
                        else -> fieldTextPaint
                    }
                }
                canvas.drawText(stringValue,
                    c * fieldPixelSize + fieldPixelSize * 0.5F,
                    r * fieldPixelSize + fieldPixelSize * 0.8F,
                    paint
                )
            }
        }
    }

    private fun drawLines(canvas : Canvas) {
        canvas.drawRect(0F, 0F, width.toFloat(), height.toFloat(), thickLinePaint)
        for (i in 1 until fieldsInRow) {
            var paint : Paint
            paint = when (i % fieldsInRowInSquare) {
                0 -> thickLinePaint
                else -> thinLinePaint
            }

            canvas.drawLine(
                i * fieldPixelSize,
                0F,
                i * fieldPixelSize,
                height.toFloat(),
                paint)
            canvas.drawLine(
                0F,
                i * fieldPixelSize,
                width.toFloat(),
                i * fieldPixelSize,
                paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                handleTouchEvent(event.x, event.y)
                true
            }
            else -> false
        }
    }

    private fun handleTouchEvent(x : Float, y : Float) {
        selectedColumn = ( x / fieldPixelSize ).toInt()
        selectedRow = ( y / fieldPixelSize ).toInt()
        dataProvider?.onSelection(selectedRow, selectedColumn)
        invalidate()
    }

    interface PuzzleDataProvider {
        fun getPuzzle() : Puzzle?
        fun onSelection(row : Int, column : Int)
    }
}