package com.example.sbb.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.sbb.widget.GridData
import org.json.JSONArray
import timber.log.Timber

class SharedPreferencesUtils private constructor(val context: Context) {

    private val preferences: SharedPreferences = context.getSharedPreferences(context.packageName, 0)

    fun getGridData(): ArrayList<GridData> {
        if (!preferences.contains("gridData")) {
            val array = arrayListOf(
                    MockData.getNext(0, 1, 2, 2),
                    MockData.getNext(0, 3, 1, 1),
                    MockData.getNext(1, 0, 1, 1),
                    MockData.getNext(2, 0, 2, 1))

            val json = JSONArray()
            for (item in array) {
                json.put(item.toJson())
            }

            preferences.edit {
                this.putString("gridData", json.toString())
            }

            return array
        } else {
            val result = arrayListOf<GridData>()
            val json = JSONArray(preferences.getString("gridData", ""))
            for (i in 0 until json.length()) {
                val obj = json.getJSONObject(i)
                val data = GridData.fromJson(obj.toString())
                result.add(data)
            }

            return result
        }
    }

    fun saveGridData(data: ArrayList<GridData>) {
        Timber.i("saveGridData: $data")
        val json = JSONArray()
        for (item in data) {
            json.put(item.toJson())
        }
        preferences.edit {
            this.putString("gridData", json.toString())
        }
    }

    fun saveStartRecents(start: String) = preferences.edit { this.putString("recentsStart", start) }
    fun saveEndRecents(start: String) = preferences.edit { this.putString("recentsEnd", start) }

    fun getStartRecents(def: String) = preferences.getString("recentsStart", def)
    fun getEndRecents(def: String) = preferences.getString("recentsEnd", def)

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: SharedPreferencesUtils? = null

        fun getInstance(context: Context): SharedPreferencesUtils {
            val i = instance
            if (i != null) {
                return i
            }

            return synchronized(this) {
                val i2 = instance
                if (i2 != null) {
                    i2
                } else {
                    instance = SharedPreferencesUtils(context)
                    instance!!
                }


            }

        }
    }
}