package com.example.sbb.widget

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class TactileSimpleTextView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
                                                     ) : AppCompatTextView(context, attrs, defStyleAttr), ITactileItem {
    override var editing: Boolean = false

    override val label: String?
        get() = text.toString()

    private val hitRect = Rect()

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        getGlobalVisibleRect(hitRect)
    }

    override fun dimAlpha() = animate().alpha(TactileLayout.ALPHA_DIM).start()

    override fun restoreAlpha() = animate().alpha(1f).start()

    override fun containsTouch(x: Float, y: Float) = hitRect.contains(x.toInt(), y.toInt())
}