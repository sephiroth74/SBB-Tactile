package com.example.sbb.widget

interface ITactileItem {
    fun dimAlpha()
    fun restoreAlpha()
    fun containsTouch(x: Float, y: Float): Boolean
    var editing: Boolean
    val label: String?
}

interface ITactileContainer {
    var editing: Boolean
    fun dimChildrenAlpha(view: ITactileItem)
    fun restoreChildrenAlpha()
    fun getTargetLocation(x: Float, y: Float, ignore: ITactileItem?): ITactileItem?
    fun containsTouch(x: Float, y: Float): Boolean
}