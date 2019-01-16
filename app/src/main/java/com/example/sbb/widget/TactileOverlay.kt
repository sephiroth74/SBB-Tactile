package com.example.sbb.widget

import android.R
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.annotation.AttrRes


class TactileOverlay @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
                                              ) : View(context, attrs, defStyleAttr) {

    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pointStart = PointF()
    private val pointEnd = PointF()
    private var state: Int = 0

    init {
        arrowPaint.color = getThemeColor(context, R.attr.colorPrimary)
        arrowPaint.style = Paint.Style.FILL_AND_STROKE
        arrowPaint.strokeWidth = STROKE_WIDTH
        setWillNotDraw(false)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        if (state == (STATE_STARTED or STATE_MOVED) && null != canvas) {
            canvas.drawCircle(pointStart.x, pointStart.y, CIRCLE_SIZE, arrowPaint)
            canvas.drawLine(pointStart.x, pointStart.y, pointEnd.x, pointEnd.y, arrowPaint)
        }
    }

    fun start(x: Float, y: Float) {
        pointStart.x = x
        pointStart.y = y
        state = state or STATE_STARTED
    }

    fun moveTo(x: Float, y: Float) {
        pointEnd.x = x
        pointEnd.y = y
        state = state or STATE_MOVED
        invalidate()
    }

    fun end() {
        state = STATE_NONE
        invalidate()
    }

    companion object {
        private const val STATE_NONE = 0
        private const val STATE_STARTED = 1 shl 1
        private const val STATE_MOVED = 1 shl 2
        private const val CIRCLE_SIZE = 14f // TODO: move to attrs
        private const val STROKE_WIDTH = 16f // TODO: move to attrs

        private fun getThemeColor(context: Context, @AttrRes id: Int): Int {
            val typedValue = TypedValue()
            val a = context.obtainStyledAttributes(typedValue.data, intArrayOf(id))
            val color = a.getColor(0, 0)
            a.recycle()
            return color
        }
    }
}