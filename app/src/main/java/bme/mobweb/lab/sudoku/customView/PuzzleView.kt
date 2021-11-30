package bme.mobweb.lab.sudoku.customView

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import bme.mobweb.lab.sudoku.R
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

    private val selectedFillPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.GREEN
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
        drawFieldBackgrounds(canvas)
        drawLines(canvas)
        drawValues(canvas)
    }

    private fun drawFieldBackgrounds(canvas : Canvas) {
        for (r in 0 until fieldsInRow) {
            for (c in 0 until fieldsInRow) {
                val selected = (selectedRow == r && selectedColumn == c)
                val evidence = dataProvider?.getEvidenceAtLocation(r, c)
                val paint = when (dataProvider?.getValidityAtLocation(r, c)) {
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

    private fun drawValues(canvas : Canvas) {
        for (r in 0..8) {
            for (c in 0..8) {
                val value = dataProvider?.getValueAtLocation(r, c)
                val stringValue = when (value) {
                    null -> ""
                    -1 -> ""
                    else -> value.toString()
                }
                canvas.drawText(stringValue,
                    c * fieldPixelSize + fieldPixelSize * 0.5F,
                    r * fieldPixelSize + fieldPixelSize * 0.8F,
                    fieldTextPaint
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
        dataProvider?.getNotifiedAboutSelection(selectedRow, selectedColumn)
        invalidate()
    }

    interface PuzzleDataProvider {
        fun getValueAtLocation(row : Int, column : Int) : Int
        fun getValidityAtLocation(row : Int, column : Int) : Boolean?
        fun getEvidenceAtLocation(row : Int, column : Int) : Boolean?
        fun getNotifiedAboutSelection(row : Int, column : Int)
    }
}