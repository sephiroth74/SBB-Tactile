package com.example.sbb.widget

import android.content.Context
import android.graphics.Point
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.sbb.R
import it.sephiroth.android.library.uigestures.*
import it.sephiroth.android.library.uigestures.UIGestureRecognizer.State.*
import timber.log.Timber


class TactileLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null,
                                              defStyleAttr: Int = 0) : ConstraintLayout(context, attrs, defStyleAttr) {

    var editing: Boolean = false
        set(value) {
            field = value
            delegate.isEnabled = !value

            grid.editing = value
            locations.editing = value

            editingActionListener?.invoke(value)
        }

    private lateinit var grid: TactileGrid
    private lateinit var locations: ITactileContainer
    private lateinit var overlay: TactileOverlay

    private val viewLocation = Point()
    private val delegate = UIGestureRecognizerDelegate()
    private val panRecognizer = UIPanGestureRecognizer(context)
    private val longRecognier = UILongPressGestureRecognizer(context)

    var actionListener: ((start: String?, end: String?) -> Unit)? = null
    var editingActionListener: ((editing: Boolean) -> Unit)? = null

    var gridDataChanged: ((list: ArrayList<GridData>) -> Unit)?
        get() {
            return grid.gridDataChanged
        }
        set(value) {
            grid.gridDataChanged = value
        }

    private var startLocation: ITactileItem? = null
        set(value) {
            field = value
            if (null != value) {
                dimChildren(value)
            } else {
                showAll()
            }
        }

    private var endLocation: ITactileItem? = null
        set(value) {
            if (field != value) {
                field?.dimAlpha()
                field = value
                value?.restoreAlpha()
            }
        }

    private var longActionListener = { recognizer: UIGestureRecognizer ->
        Timber.i("longActionListener: $recognizer")

        if (!editing) {
            if (recognizer.state == Began) {
                editing = true
            }
        }
    }

    private val panActionListener = { r: UIGestureRecognizer ->
        val recognizer = r as UIPanGestureRecognizer
        if (!editing) {
            handlePanWhileNotEditing(recognizer)
        }
    }

    init {
        delegate.addGestureRecognizer(panRecognizer)
        delegate.addGestureRecognizer(longRecognier)

        delegate.shouldReceiveTouch = { recognizer: UIGestureRecognizer ->
            var result = !editing
            Timber.i("shouldReceiveTouch: $recognizer")

            if (recognizer is UILongPressGestureRecognizer) {
                result =
                        grid.containsTouch(recognizer.currentLocationX + viewLocation.x, recognizer.currentLocationY + viewLocation.y)
            }

            result
        }

        delegate.shouldBegin = { recognizer: UIGestureRecognizer ->
            var result = !editing
            Timber.v("shouldBegin")

            if (recognizer is UIPanGestureRecognizer) {
                Timber.d("longRecognizer.state: ${longRecognier.state}")
                result = longRecognier.state != UIGestureRecognizer.State.Began && longRecognier.state !=
                        UIGestureRecognizer.State.Changed
            }

            result
        }

        setGestureDelegate(delegate)

        panRecognizer.actionListener = panActionListener
        longRecognier.actionListener = longActionListener

        setWillNotDraw(false)

    }

    fun discardChanges() {
        if (editing)
            grid.discardChanges()
    }

    fun commitChanges() {
        if (editing)
            grid.commitChanges()
    }

    private fun handlePanWhileNotEditing(recognizer: UIPanGestureRecognizer) {
        Timber.i("handlePanWhileNotEditing")

        when (recognizer.state) {
            Began -> {
                startLocation =
                        getTargetLocation(recognizer.startLocationX + viewLocation.x, recognizer.startLocationY + viewLocation.y, null)
                startLocation?.let {
                    overlay.start(recognizer.startLocationX, recognizer.startLocationY)
                }

                Timber.d("startLocation: $startLocation")

            }
            Changed -> {
                startLocation?.let {
                    endLocation =
                            getTargetLocation(recognizer.currentLocationX + viewLocation.x,
                                    recognizer.currentLocationY + viewLocation.y, startLocation)
                    overlay.moveTo(recognizer.currentLocationX, recognizer.currentLocationY)
                }
            }
            Ended -> {
                overlay.end()

                if (null != startLocation && null != endLocation) {
                    actionListener?.invoke(startLocation!!.label, endLocation!!.label)
                }

                startLocation = null
                endLocation = null
                showAll()
            }
        }
        invalidate()
    }


    private fun dimChildren(view: ITactileItem) {
        locations.dimChildrenAlpha(view)
        grid.dimChildrenAlpha(view)
    }

    private fun showAll() {
        locations.restoreChildrenAlpha()
        grid.restoreChildrenAlpha()
    }

    private fun getTargetLocation(x: Float, y: Float, ignore: ITactileItem?): ITactileItem? {
        return grid.getTargetLocation(x, y, ignore) ?: locations.getTargetLocation(x, y, ignore)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        grid = findViewById(R.id.grid)
        overlay = findViewById(R.id.overlay)

        val fixedView = findViewById<ViewGroup>(R.id.others)
        locations = fixedView as ITactileContainer
    }


    private val statusBarHeight: Int by lazy {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        result
    }

    var data: ArrayList<GridData> = arrayListOf()
        set(value) {
            grid.data = value
        }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        if (changed) {
            Timber.i("onLayout($left, $top, $right, $bottom)")
            viewLocation.x = left
            viewLocation.y = top + statusBarHeight
        }
    }

    companion object {
        const val ALPHA_DIM = 0.6f
    }
}