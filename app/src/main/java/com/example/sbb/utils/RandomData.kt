package com.example.sbb.utils

import com.example.sbb.widget.GridData


object MockData {

    val CITIES = arrayListOf(
            GridData("Zurich", "file:///android_asset/cities/zurich.jpg"),
            GridData("Geneva", "file:///android_asset/cities/geneva.jpg"),
            GridData("Basel", "file:///android_asset/cities/basel.jpg"),
            GridData("Bern", "file:///android_asset/cities/bern.jpg"),
            GridData("Lausanne", "file:///android_asset/cities/lausanne.jpg"),
            GridData("Lucerne", "file:///android_asset/cities/lucerne.jpg"),
            GridData("Lugano", "file:///android_asset/cities/lugano.jpg"),
            GridData("St Gallen", "file:///android_asset/cities/st_gallen.jpg"),
            GridData("Interlaken", "file:///android_asset/cities/interlaken.jpg"),
            GridData("Zermatt", "file:///android_asset/cities/zermatt.jpg"),
            GridData("Zug", "file:///android_asset/cities/zug.jpg"),
            GridData("Locarno", "file:///android_asset/cities/locarno.jpg"),
            GridData("Fribourg", "file:///android_asset/cities/fribourg.jpg"),
            GridData("Sion", "file:///android_asset/cities/sion.jpg"),
            GridData("Baden", "file:///android_asset/cities/baden.jpg"),
            GridData("Bellinzona", "file:///android_asset/cities/bellinzona.jpg"),
            GridData("Thun", "file:///android_asset/cities/thun.jpg"),
            GridData("Neuchâtel", "file:///android_asset/cities/neuchatel.jpg")
                            )

    fun getNext(r: Int = 0, c: Int = 0, w: Int = 0, h: Int = 0): GridData {
        val item =
                GridData(CITIES[(Math.random() * CITIES.size).toInt()])
        return GridData(r, c, w, h, item.label, item.img)
    }

}