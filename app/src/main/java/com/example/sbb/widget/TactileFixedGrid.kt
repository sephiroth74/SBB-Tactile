package com.example.sbb.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children

class TactileFixedGrid @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
                                                ) : ConstraintLayout(context, attrs, defStyleAttr), ITactileContainer {
    override var editing: Boolean
        get() = visibility == View.VISIBLE
        set(value) {
            visibility = if (value) {
                View.INVISIBLE
            } else {
                View.VISIBLE
            }

            for (child: View in children) {
                if (child is ITactileItem) {
                    (child as ITactileItem).editing = value
                }
            }
        }

    override fun dimChildrenAlpha(view: ITactileItem) {
        for (child: View in children) {
            if (child != view && child is ITactileItem) {
                (child as ITactileItem).dimAlpha()
            }
        }
    }

    override fun restoreChildrenAlpha() {
        for (child: View in children) {
            if (child is ITactileItem) {
                (child as ITactileItem).restoreAlpha()
            }
        }
    }

    override fun getTargetLocation(x: Float, y: Float, ignore: ITactileItem?): ITactileItem? {
        for (child: View in children) {
            if (null != ignore && ignore == child || child !is ITactileItem) {
                continue
            }

            if ((child as ITactileItem).containsTouch(x, y)) {
                return child
            }
        }
        return null
    }

    override fun containsTouch(x: Float, y: Float): Boolean = false

}