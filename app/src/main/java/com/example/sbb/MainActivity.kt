package com.example.sbb

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.Toolbar
import androidx.core.view.doOnNextLayout
import com.example.sbb.utils.SharedPreferencesUtils
import com.example.sbb.widget.TactileLayout
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private lateinit var tactileLayout: TactileLayout
    private lateinit var toolbar: Toolbar

    private var actionMode: ActionMode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.plant(Timber.DebugTree())
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)

        findViewById<TextView>(R.id.buttonRecentsStart).text = SharedPreferencesUtils.getInstance(this).getStartRecents("Basel")
        findViewById<TextView>(R.id.buttonRecentsEnd).text = SharedPreferencesUtils.getInstance(this).getEndRecents("Bern")

        tactileLayout.actionListener = { start, end ->
            if (null != start && null != end) {
                Toast.makeText(this, "Selected ride from $start to $end", Toast.LENGTH_LONG).show()

                if (start != getString(R.string.other_arriving_location) && start != getString(R.string.other_starting_location)) {
                    SharedPreferencesUtils.getInstance(this).saveStartRecents(start)
                }

                if (end != getString(R.string.other_arriving_location) && end != getString(R.string.other_starting_location)) {
                    SharedPreferencesUtils.getInstance(this).saveEndRecents(end)
                }

            }
        }

        tactileLayout.editingActionListener = { editing ->
            if (editing) {
                actionMode = startSupportActionMode(ActionModeCallback())
            } else {
                actionMode?.let {
                    actionMode = null
                    it.finish()
                }
            }
        }

        tactileLayout.doOnNextLayout {
            val initialData = SharedPreferencesUtils.getInstance(this).getGridData()
            tactileLayout.data = initialData
        }

        tactileLayout.gridDataChanged = { data ->
            SharedPreferencesUtils.getInstance(this).saveGridData(data)
        }
    }

    override fun onBackPressed() {
        Timber.i("onBackPressed")

        actionMode?.let {
            actionMode = null
            it.finish()
            return@let
        }
        super.onBackPressed()
    }

    override fun onContentChanged() {
        super.onContentChanged()
        tactileLayout = findViewById(R.id.tactileLayout)
        toolbar = findViewById(R.id.toolbar)
    }

    inner class ActionModeCallback : ActionMode.Callback {
        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            Timber.i("onActionItemClicked")
            when (item?.itemId) {
                R.id.cancel -> {
                    tactileLayout.discardChanges()
                    actionMode?.finish()
                }
                R.id.save -> {
                    tactileLayout.commitChanges()
                    actionMode?.finish()
                }
            }
            return false
        }

        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            menuInflater.inflate(R.menu.action_mode, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            Timber.i("onDestroyActionMode")
            tactileLayout.editing = false
        }
    }

}
