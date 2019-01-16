package com.example.sbb.widget

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.core.view.children
import com.example.sbb.R
import com.example.sbb.utils.MockData
import it.sephiroth.android.library.uigestures.*
import org.json.JSONObject
import timber.log.Timber
import java.lang.ref.WeakReference
import kotlin.math.roundToInt


class TactileGrid @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null,
        defStyleAttr: Int = 0) : RelativeLayout(context, attrs, defStyleAttr), ITactileContainer {

    private val delegate = UIGestureRecognizerDelegate()
    private val tapRecognizer = UITapGestureRecognizer(context)
    private val panRecognizer = UIPanGestureRecognizer(context)

    // action listener for dataset changed
    var gridDataChanged: ((list: ArrayList<GridData>) -> Unit)? = null

    override var editing: Boolean = false
        set(value) {
            if (field != value) {
                Timber.i("editing: $value")
                field = value

                children.forEach { (it as TactileGridItemView).editing = value }

                if (value) {
                    tempMatrixData = finalMatrixData.clone()
                    finalMatrixData.ibernate()

                    setGestureDelegate(delegate)
                } else {
                    tempMatrixData?.let {
                        datasetChanged(it.getArrayData())
                        gridDataChanged?.invoke(it.getArrayData())
                    } ?: run {
                        finalMatrixData.print()
                        datasetChanged(finalMatrixData.getArrayData())
                    }

                    tempMatrixData = null
                    setGestureDelegate(null)
                }
                postInvalidate()
            }
        }

    private var columns: Int
    private var rows: Int
    private var gap: Int
    private val globalRect = Rect()
    private val hitRect = Rect()
    private var gridPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var gridPadding: Int
    private var gridItemAnimationDuration: Int
    private var itemWidth: Int = 0
    private var itemHeight: Int = 0
    private var draggingToleranceWidth = 0f
    private var draggingToleranceHeight = 0f
    private var hasSize: Boolean = false

    // stable data
    private lateinit var finalMatrixData: MatrixData

    // data used while editing
    private var tempMatrixData: MatrixData? = null

    private fun getMatrixData(): MatrixData = tempMatrixData?.let { return it } ?: run { return finalMatrixData }

    var data: MutableList<GridData>
        get() = getMatrixData().getArrayData()
        set(value) {
            datasetChanged(value)
        }


    // single tap (add item to the grid)
    private val tapActionListener = { recognizer: UIGestureRecognizer ->
        val x = recognizer.currentLocationX + globalRect.left
        val y = recognizer.currentLocationY + globalRect.top

        val location = getRowColForLocation(recognizer.currentLocationX, recognizer.currentLocationY)
        if (getMatrixData().isLocationEmpty(location[0], location[1])) {
            performClick()
            val mock = MockData.getNext(location[0], location[1], 1, 1)
            data.add(mock)
            addGridItem(getMatrixData(), mock)
        }
    }

    // dragging data
    private var draggingItem: TactileGridItemView? = null
    private var draggingItemOffset = PointF()
    private var draggingCompleted: Boolean = false
    private var draggingData: GridData? = null
    private var draggingRect = Rect()

    private var panActionListener = { r: UIGestureRecognizer ->
        val recognizer = r as UIPanGestureRecognizer
        Timber.i("recognier: $recognizer")

        when (recognizer.state) {
            UIGestureRecognizer.State.Began -> {
                Timber.i("DRAG AND DROP BEGIN")

                draggingCompleted = false
                draggingData = null

                for (data in tempMatrixData!!.getArrayData()) {
                    val rect = getItemRect(data.row, data.column, data.width, data.height)
                    if (rect.contains(recognizer.startLocationX.toInt(), recognizer.startLocationY.toInt())) {
                        draggingItem = tempMatrixData?.getViewForData(data)
                        draggingItem?.let { ditem ->
                            draggingItemOffset.set(recognizer.startLocationX - ditem.translationX, recognizer.startLocationY - ditem.translationY)
                            dimChildrenAlpha(ditem)

                            Timber.d("draggingItem: ${ditem.data}")
                            tempMatrixData?.removeItem(ditem)

                            draggingData = GridData(ditem.data)
                            ditem.tmpData = GridData(ditem.data)
                        }
                        break
                    }
                }
            }

            UIGestureRecognizer.State.Changed -> {
                draggingItem?.let {
                    it.translationX = recognizer.currentLocationX - draggingItemOffset.x
                    it.translationY = recognizer.currentLocationY - draggingItemOffset.y


                    // clamping
                    // maybe refactoring this?
                    it.getHitRect(draggingRect)
                    if (draggingRect.right > hitRect.right + draggingToleranceWidth) it.translationX =
                            (hitRect.right - draggingRect.width() + draggingToleranceWidth)
                    if (draggingRect.bottom > hitRect.bottom + draggingToleranceHeight) it.translationY =
                            (hitRect.bottom - draggingRect.height() + draggingToleranceHeight)
                    if (draggingRect.left < -draggingToleranceWidth) it.translationX = (-draggingToleranceWidth)
                    if (draggingRect.top < -draggingToleranceHeight) it.translationY = (-draggingToleranceHeight)

                    it.getHitRect(draggingRect)
                    val position = getPosition(draggingRect)

                    draggingCompleted = true

                    if (!tempMatrixData?.isLocationAvailable(position.y, position.x, it)!!) {
                        val dropData = GridData(it.data, position.y, position.x)
                        val dropItems = tempMatrixData!!.getDropItems(position.y, position.x, it)

                        for (dropItem in dropItems) {
                            Timber.v("dropItem: ${dropItem.data}")

                            val newPoint = tempMatrixData!!.findNewPosition(dropItem, dropData, it)

                            if (dropItem.tmpData == null)
                                dropItem.tmpData = GridData(dropItem.data)

                            newPoint?.let { pt ->
                                val dropItemNewData = GridData(dropItem.data, pt.y, pt.x)
                                tempMatrixData!!.removeItem(dropItem)
                                updateGridItemDataAndPosition(dropItem, dropItemNewData, true)
                                tempMatrixData!!.addItem(dropItem)
                            } ?: run {
                                Timber.e("cannot find new position for ${dropItem.data}")
                                draggingCompleted = false
                            }
                        }
                    }

                    draggingData = if (draggingCompleted) GridData(it.data, position.y, position.x) else null
                    Timber.v("draggingData: $draggingData")
                }
            }

            UIGestureRecognizer.State.Ended -> {

                restoreChildrenAlpha()

                if (!draggingCompleted) {
                    tempMatrixData!!.reset()

                    for (child in children) {
                        if (child is TactileGridItemView) {
                            Timber.v("restore child: ${child.data}, ${child.id}")
                            child.tmpData?.let {
                                updateGridItemDataAndPosition(child, it, true)
                            } ?: run {
                                updateGridItemDataAndPosition(child, child.data, true)
                            }
                            child.tmpData = null

                            if (!tempMatrixData!!.contains(child.data)) {
                                tempMatrixData!!.addItem(child)
                            }
                        }
                    }


                } else {

                    draggingItem?.let {

                        val newData = draggingData?.let { it } ?: run { it.data }
                        draggingData = null

                        updateGridItemDataAndPosition(it, newData, true)
                        tempMatrixData?.addItem(it)
                        it.tmpData = null

                        // Timber.i("tempMatrixData")
                        // tempMatrixData?.print()
                        // Timber.i("finalMatrixData")
                        // finalMatrixData.print()

                    }
                }
                draggingItem = null
                children.forEach { (it as TactileGridItemView).tmpData = null }
            }
            else -> {
            }
        }

    }

    init {
        val array = resources.obtainAttributes(attrs, R.styleable.TactileGrid)
        columns = array.getInteger(R.styleable.TactileGrid_sbb_columns, 3)
        rows = array.getInteger(R.styleable.TactileGrid_sbb_rows, 3)
        gap = array.getDimensionPixelSize(R.styleable.TactileGrid_sbb_gap, 2)
        gridPaint.color = array.getColor(R.styleable.TactileGrid_sbb_itemGrid_strokeColor, Color.BLACK)
        gridPaint.strokeWidth = array.getDimension(R.styleable.TactileGrid_sbb_itemGrid_strokeWidth, 6f)
        gridPaint.style = Paint.Style.STROKE
        gridPadding = array.getDimensionPixelSize(R.styleable.TactileGrid_sbb_itemGrid_padding, 2)
        gridItemAnimationDuration = array.getInteger(R.styleable.TactileGrid_sbb_itemGrid_animationDuration, 100)

        array.recycle()

        finalMatrixData = MatrixData(rows, columns)

        delegate.addGestureRecognizer(tapRecognizer)
        delegate.addGestureRecognizer(panRecognizer)

        tapRecognizer.actionListener = tapActionListener
        panRecognizer.actionListener = panActionListener

        setWillNotDraw(false)

    }

    fun discardChanges() {
        Timber.i("discardChanges")
        if (editing) {
            tempMatrixData = null
        }
    }

    fun commitChanges() {
        Timber.i("commitChanges")
    }

    private fun datasetChanged(data: MutableList<GridData>) {
        Timber.i("datasetChanged")

        removeAllViews()
        finalMatrixData.reset()

        for (item in data) {
            addGridItem(finalMatrixData, item)
        }
        finalMatrixData.print()
    }

    private val onItemDeleteClick: ((v: TactileGridItemView) -> Unit)? = { view ->
        deleteGridItem(view)
    }

    private val onItemMaximizeClick: ((v: TactileGridItemView) -> Unit)? = { view ->
        if (editing) {
            val newData = getMatrixData().canExpand(view)
            newData?.let {
                getMatrixData().removeItem(view)
                updateGridItemDataAndPosition(view, newData)
                getMatrixData().addItem(view)
            }
        }
    }

    private val onItemMinimizeClick: ((v: TactileGridItemView) -> Unit)? = { view ->
        val data = view.data

        if (editing && (data.width > 1 || data.height > 1)) {
            getMatrixData().removeItem(view)

            val newData: GridData = if (data.width > data.height) {
                GridData(data.row, data.column, data.width - 1, data.height, data.label, data.img)
            } else {
                GridData(data.row, data.column, data.width, data.height - 1, data.label, data.img)
            }

            updateGridItemDataAndPosition(view, newData)
            getMatrixData().addItem(view)

        }
    }

    private fun deleteGridItem(view: TactileGridItemView) {
        getMatrixData().removeItem(view)
        removeView(view)
        getMatrixData().print()
    }

    private fun updateGridItemDataAndPosition(view: TactileGridItemView,
                                              newData: GridData, animate: Boolean = false) {
        val r = getItemRect(newData.row, newData.column, newData.width, newData.height)
        val params = view.layoutParams
        params.width = r.width()
        params.height = r.height()

        if (animate) {
            view.animate()
                .translationX(r.left.toFloat())
                .translationY(r.top.toFloat())
                .setDuration(gridItemAnimationDuration.toLong()).start()
        } else {
            view.translationX = r.left.toFloat()
            view.translationY = r.top.toFloat()
        }

        view.layoutParams = params
        view.data.set(newData)
    }

    private fun addGridItem(data: MatrixData, item: GridData) {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.grid_item_view, this, false) as TactileGridItemView
        view.data.set(item)
        data.addItem(view)

        val r = getItemRect(item.row, item.column, item.width, item.height)
        val params = ViewGroup.LayoutParams(r.width(), r.height())
        addView(view, params)

        view.translationX = r.left.toFloat()
        view.translationY = r.top.toFloat()
        view.deleteAction = onItemDeleteClick
        view.maximizeAction = onItemMaximizeClick
        view.minimizeAction = onItemMinimizeClick
        view.editing = editing
    }

    private fun getRowColForLocation(x: Float, y: Float): Array<Int> {
        val col = (x / globalRect.width()) * columns
        val row = (y / globalRect.height()) * rows
        return arrayOf(row.toInt(), col.toInt())
    }

    private fun getItemRect(r: Int, c: Int, spanH: Int = 1, spanV: Int = 1): Rect {
        val insideGapH = (c) * gap
        val insideGapV = (r) * gap
        val w = itemWidth * spanH
        val h = itemHeight * spanV

        return Rect(
                gap + (c * itemWidth) + insideGapH,
                gap + (r * itemHeight) + insideGapV,
                gap + (c * itemWidth) + w + insideGapH,
                gap + (r * itemHeight) + h + insideGapV)
    }

    private fun getPosition(rect: Rect): Point {
        val insideGapH = (columns) * gap
        val insideGapV = (rows) * gap
        val maxLeft = gap + (columns * itemWidth) + insideGapH
        val maxBottom = gap + (rows * itemHeight) + insideGapV
        val c = ((rect.left.toFloat() / maxLeft.toFloat()) * columns).roundToInt()
        val r = ((rect.top.toFloat() / maxBottom.toFloat()) * rows).roundToInt()

        return Point(c.coerceIn(0, columns - 1), r.coerceIn(0, rows - 1))
    }

    private fun drawGrid(canvas: Canvas) {
        for (r in 0 until rows) {
            for (c in 0 until columns) {
                drawGridItem(canvas, getItemRect(r, c))
            }
        }
    }

    private fun drawGridItem(canvas: Canvas, rect: Rect) {
        rect.inset(gridPadding, gridPadding)
        val lw = rect.width() / 12

        canvas.drawLine((rect.centerX() - lw).toFloat(), rect.centerY().toFloat(), (rect.centerX() + lw).toFloat(), rect.centerY().toFloat(), gridPaint)
        canvas.drawLine(rect.centerX().toFloat(), (rect.centerY() - lw).toFloat(), rect.centerX().toFloat(), (rect.centerY() + lw).toFloat(), gridPaint)
        canvas.drawRect(rect, gridPaint)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        if (editing && null != canvas && hasSize) {
            drawGrid(canvas)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        itemWidth = ((w - (gap * 2)) - (gap * (columns - 1))) / columns
        itemHeight = ((h - (gap * 2)) - (gap * (rows - 1))) / rows
        draggingToleranceHeight = itemHeight.toFloat() / 2.5f
        draggingToleranceWidth = itemWidth.toFloat() / 2.5f
        hasSize = true
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        getGlobalVisibleRect(globalRect)
        getHitRect(hitRect)
    }

    override fun containsTouch(x: Float, y: Float) = globalRect.contains(x.toInt(), y.toInt())

    override fun getTargetLocation(x: Float, y: Float, ignore: ITactileItem?): ITactileItem? {
        for (child in children) {
            if (null != ignore && ignore == child) continue
            if ((child as TactileGridItemView).containsTouch(x, y)) return child
        }
        return null
    }

    override fun dimChildrenAlpha(view: ITactileItem) {
        for (child: View in children) {
            if (child != view) {
                (child as TactileGridItemView).dimAlpha()
            }
        }
    }

    override fun restoreChildrenAlpha() {
        for (child: View in children) {
            (child as TactileGridItemView).restoreAlpha()
        }
    }
}

data class MatrixData(val rows: Int, val columns: Int) {
    private var data: Array<Array<WeakReference<TactileGridItemView?>>> =
            Array(rows) { Array(size = columns, init = { WeakReference<TactileGridItemView?>(null) }) }

    private var arrayData: ArrayList<GridData> = arrayListOf()

    fun getArrayData(): ArrayList<GridData> {
        arrayListOf<GridData>().apply { addAll(arrayData) }.run { return this }
    }

    init {
        reset()
    }

    private val width = data[0].size
    private val height = data.size

    fun clone(): MatrixData {
        val newData = MatrixData(rows, columns)

        newData.data.forEachIndexed { index, arrayOfWeakReferences ->
            arrayOfWeakReferences.forEachIndexed { index2, _ ->
                newData.data[index][index2] = WeakReference(data[index][index2].get())
            }
        }

        newData.arrayData = arrayData.map { it.copy() } as ArrayList<GridData>
        return newData
    }


    fun ibernate() {
        arrayData = arrayData.map { it.copy() } as ArrayList<GridData>
    }


    fun reset() {
        arrayData.clear()
        data.forEach { it.fill(WeakReference<TactileGridItemView?>(null), 0, it.size) }
    }

    fun removeItem(view: TactileGridItemView) {
        val item = view.data

        if (arrayData.remove(item)) {
            Timber.v("removeItem $item")

            for (y in item.row until item.row + item.height) {
                for (x in item.column until item.column + item.width) {
                    if (data[y][x].get() != view) {
                        throw RuntimeException("data corrupted! Expecting ${view.data} at $y,$x but found ${data[y][x].get()?.data}")
                    }
                    data[y][x] = WeakReference(null)
                }
            }
        } else {
            Timber.e("cannot remove $item")
        }
    }

    fun addItem(view: TactileGridItemView) {
        Timber.v("addItem ${view.data}")
        val startRow = view.data.row
        val endRow = startRow + (view.data.height - 1)
        val startColumn = view.data.column
        val endColumn = startColumn + (view.data.width - 1)

        assert(startRow >= 0 && endRow < data.size && view.data.height > 0)
        assert(startColumn >= 0 && endColumn < data[0].size && view.data.width > 0)

        for (y in startRow..endRow) {
            for (x in startColumn..endColumn) {
                if (data[y][x].get() != null) {
                    Timber.e("there's already ${data[y][x].get()!!.data} here")
                    throw RuntimeException("Matrix is not empty at $y, $x")
                }
                data[y][x] = WeakReference(view)
            }
        }

        arrayData.add(view.data)
    }

    fun isLocationEmpty(row: Int, col: Int) = data[row][col].get() == null

    fun canExpand(view: TactileGridItemView): GridData? {
        val item = view.data

        Timber.i("canExpand: $item")
        Timber.v("this size: $width x $height")

        if (item.column + item.width >= width && item.row + item.height >= height) return null

        val h1 = canExpandHorizontallyRight(item)
        val h2 = canExpandHorizontallyLeft(item)
        val v1 = canExpandVerticallyDown(item)
        val v2 = canExpandVerticallyUp(item)

        if (item.width <= item.height && (h1 || h2)) {
            return if (h1) {
                GridData(item.row, item.column, item.width + 1, item.height, item.label, item.img)
            } else {
                GridData(item.row, item.column - 1, item.width + 1, item.height, item.label, item.img)
            }
        } else if (v1 || v2) {
            return if (v1)
                GridData(item.row, item.column, item.width, item.height + 1, item.label, item.img)
            else
                GridData(item.row - 1, item.column, item.width, item.height + 1, item.label, item.img)
        }

        if (item.width <= item.height) {
            val newData = GridData(item, item.row, item.column, item.width + 1, item.height)
            findNewPosition(view, newData)?.let {
                return GridData(newData, it.y, it.x)
            }
        } else {
            val newData = GridData(item, item.row, item.column, item.width, item.height + 1)
            findNewPosition(view, newData)?.let {
                return GridData(newData, it.y, it.x)
            }
        }

        return null
    }

    private fun canExpandHorizontallyRight(item: GridData): Boolean {
        if (item.column + item.width < width) {
            for (y in item.row until item.row + item.height) {
                if (data[y][item.column + item.width].get() != null) {
                    return false
                }
            }
            return true
        }
        return false
    }

    private fun canExpandHorizontallyLeft(item: GridData): Boolean {
        if (item.column > 0) {
            for (y in item.row until item.row + item.height) {
                if (data[y][item.column - 1].get() != null) {
                    return false
                }
            }
            return true
        }
        return false
    }

    private fun canExpandVerticallyDown(item: GridData): Boolean {
        if (item.row + item.height < height) {
            for (x in item.column until item.column + item.width) {
                if (data[item.row + item.height][x].get() != null) {
                    return false
                }
            }
            return true
        }
        return false
    }

    private fun canExpandVerticallyUp(item: GridData): Boolean {
        if (item.row > 0) {
            for (x in item.column until item.column + item.width) {
                if (data[item.row - 1][x].get() != null) {
                    return false
                }
            }
            return true
        }
        return false
    }

    fun print() {
        for (item in arrayData) {
            Timber.v("arrayData: $item")
        }
//
//        val s = StringBuilder()
//        Timber.v("---------- matrixData -----------")
//        for (y in 0 until data.size) {
//            s.append("$y = [")
//            for (x in 0 until data[y].size) {
//                val value = if (data[y][x].get() == null) 0 else 1
//                s.append(" $value")
//            }
//            s.append(" ]")
//            Timber.v(s.toString())
//            s.clear()
//        }
    }

    fun getViewForData(itemData: GridData): TactileGridItemView? {
        return data[itemData.row][itemData.column].get()
    }

    fun isLocationAvailable(row: Int, column: Int, view: TactileGridItemView): Boolean {
        if (row + view.data.height >= height) return false
        if (column + view.data.width >= width) return false

        for (y in row until row + view.data.height) {
            for (x in column until column + view.data.width) {
                val targetView = data[y][x].get()
                if (targetView != null && targetView != view) {
                    return false
                }
            }
        }
        return true
    }

    fun getDropItems(row: Int, column: Int, exclude: TactileGridItemView): List<TactileGridItemView> {
        val dict = hashSetOf<TactileGridItemView>()
        val itemData = exclude.data

        if (row + itemData.height > height || column + itemData.width > width) {
            return dict.toList()
        }

        for (y in row until row + itemData.height) {
            for (x in column until column + itemData.width) {
                val view = data[y][x].get()
                view?.let {
                    dict.add(it)
                }
            }
        }
        dict.remove(exclude)
        return dict.toList()
    }

    fun findNewPosition(targetView: TactileGridItemView, dropData: GridData, excludeView: TactileGridItemView?): Point? {
        val targetData = targetView.data
        val dropRect = Rect(dropData.column, dropData.row, dropData.column + dropData.width, dropData.row + dropData.height)

        // Timber.d("targetView: $targetView")
        // Timber.d("dropRect: $dropRect")
        // Timber.d("targetData: $targetData")

        for (y in 0..rows - targetData.height) {
            for (x in 0..columns - targetData.width) {
                var view = data[y][x].get()
                Timber.v("checking $y, $x")

                if (view == excludeView) view = null

                if (view == targetView) {
                    if (targetData.row == y && targetData.column == x) {
                        Timber.w("$y, $x same as original item")
                        continue
                    } else {
                        view = null
                    }
                }

                if (null == view) {
                    var p1: Point? = Point(x, y)

                    for (y1 in y until y + targetData.height) {
                        if (y1 > rows - 1) continue

                        for (x1 in x until x + targetData.width) {
                            if (x1 > columns - 1) continue

                            if (dropRect.contains(x1, y1)) {
                                Timber.w("dropRect $dropRect contains $x1, $y1")
                                p1 = null
                            }

                            view = data[y1][x1].get()
                            if (view == excludeView || view == targetView) view = null
                            if (null != view) {
                                Timber.w("view = $view")
                                p1 = null
                            }
                        }
                    }

                    p1?.let {
                        return it
                    }
                } else {
//                    Timber.w("other view is $view")
                }
            }
        }
        return null
    }

    private fun findNewPosition(targetView: TactileGridItemView, newData: GridData): Point? {
        for (y in 0..rows - newData.height) {
            for (x in 0..columns - newData.width) {
                var view = data[y][x].get()
                if (view == targetView && newData.row == y && newData.column == x) continue

                if (null == view) {
                    var p1: Point? = Point(x, y)

                    for (y1 in y until y + newData.height) {

                        if (y1 > rows - 1) continue

                        for (x1 in x until x + newData.width) {
                            if (x1 > columns - 1) continue

                            view = data[y1][x1].get()
                            if (view == targetView) view = null
                            if (null != view) p1 = null
                        }
                    }

                    p1?.let {
                        return it
                    }

                }
            }
        }
        return null
    }

    fun contains(data: GridData): Boolean = arrayData.contains(data)
}


data class GridData(var row: Int, var column: Int, var width: Int, var height: Int, var label: String?, var img: String? = null) {
    constructor(label: String, img: String) : this(0, 0, 1, 1, label, img)
    constructor(other: GridData) : this(other.row, other.column, other.width, other.height, other.label, other.img)
    constructor(other: GridData, row: Int, column: Int) : this(row, column, other.width, other.height, other.label, other.img)
    constructor(other: GridData, row: Int, column: Int, width: Int,
                height: Int) : this(row, column, width, height, other.label, other.img)

    constructor() : this(0, 0, 0, 0, null, null)

    var listener: ((data: GridData) -> Unit)? = null

    override fun equals(other: Any?): Boolean {
        if (other is GridData) {
            return other.row == row
                   && other.column == column
                   && other.width == width
                   && other.height == height
                   && other.label == label
        }
        return false
    }

    fun copy(): GridData {
        return GridData(row, column, width, height, label, img)
    }

    override fun toString(): String {
        return "GridData(row=$row, column=$column, width=$width, height=$height, label=$label)"
    }

    fun set(other: GridData) {
        row = other.row
        column = other.column
        width = other.width
        height = other.height
        label = other.label
        img = other.img
        listener?.invoke(this)
    }

    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("row", row)
        json.put("column", column)
        json.put("width", width)
        json.put("height", height)
        json.put("label", label)
        json.put("img", img)
        return json
    }

    companion object {
        fun fromJson(string: String): GridData {
            val json = JSONObject(string)
            val r = json.getInt("row")
            val c = json.getInt("column")
            val w = json.getInt("width")
            val h = json.getInt("height")
            val l = json.getString("label")
            val i = json.getString("img")
            return GridData(r, c, w, h, l, i)
        }
    }

}
