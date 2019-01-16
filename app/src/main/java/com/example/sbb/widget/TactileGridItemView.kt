package com.example.sbb.widget

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.sbb.R
import com.squareup.picasso.Picasso

class TactileGridItemView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : ConstraintLayout(context, attrs, defStyleAttr),
        ITactileItem {

    private lateinit var imageView: ImageView
    private lateinit var textView: TextView
    private lateinit var minimizeButton: View
    private lateinit var maximizeButton: AppCompatImageView
    private lateinit var deleteButton: View

    var deleteAction: ((v: TactileGridItemView) -> Unit)? = null
    var maximizeAction: ((v: TactileGridItemView) -> Unit)? = null
    var minimizeAction: ((v: TactileGridItemView) -> Unit)? = null

    private val hitRect: Rect = Rect()

    override var editing: Boolean = false
        set(value) {
            field = value
            if (!value) {
                maximizeButton.visibility = View.INVISIBLE
                deleteButton.visibility = View.GONE
            } else {
                maximizeButton.visibility = View.VISIBLE
                deleteButton.visibility = View.VISIBLE
            }

            updateEditingButtons()
        }

    val data: GridData = GridData()

    // temporary data used to perform rollbacks
    var tmpData: GridData? = null

    override fun dimAlpha() = animate().alpha(TactileLayout.ALPHA_DIM).start()

    override fun restoreAlpha() = animate().alpha(1f).start()

    override fun containsTouch(x: Float, y: Float) = hitRect.contains(x.toInt(), y.toInt())

    override val label: String?
        get() = data.label

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        getGlobalVisibleRect(hitRect)
    }

    fun updateEditingButtons() {
        if (!editing) {
            minimizeButton.alpha = 0f
        } else {
            minimizeButton.alpha = if (data.width > 1 || data.height > 1) 1f else 0.5f
        }
        minimizeButton.isEnabled = if (!editing) false else data.width > 1 || data.height > 1

    }

    private fun updateData() {
        data.img?.let {
            Picasso.get().load(it).placeholder(imageView.drawable).error(R.drawable.city_default).into(imageView)
        }

        data.label?.let {
            textView.text = it
        }

        updateEditingButtons()
    }

    override fun onDetachedFromWindow() {
        data.listener = null
        super.onDetachedFromWindow()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        data.listener = { updateData() }
        updateData()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        textView = findViewById(R.id.textView)
        imageView = findViewById(R.id.imageView)
        minimizeButton = findViewById(R.id.minimize)
        maximizeButton = findViewById(R.id.maximize)
        deleteButton = findViewById(R.id.delete)

        deleteButton.setOnClickListener { deleteAction?.invoke(this) }
        maximizeButton.setOnClickListener { maximizeAction?.invoke(this) }
        minimizeButton.setOnClickListener { minimizeAction?.invoke(this) }
    }
}