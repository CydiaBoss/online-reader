package com.wang.twkanviewer

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri

private const val TAG = "MainActivity"
class MainActivity : ComponentActivity() {
    private lateinit var urlField: EditText
    private lateinit var chapterView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // System Service
        val inputMethodManager : InputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

        // Custom Tabs as Reader
        var intentTab = CustomTabsIntent.Builder()
            .setStartAnimations(this, android.R.anim.slide_in_left, android.R.anim.slide_out_right)
            .setExitAnimations(this, android.R.anim.slide_in_left, android.R.anim.slide_out_right)
            .setUrlBarHidingEnabled(true)
            .setShowTitle(true)
            .build()

        // Load Views
        urlField = findViewById<EditText>(R.id.urlField)
        chapterView = findViewById<TextView>(R.id.chapterView)

        // Listener
        urlField.setOnEditorActionListener { view, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                // Hide the keyboard
                inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
                Log.d(TAG, "Closed Keyboard")

                // Load Story from URL
                chapterView.text = R.string.loading.toString()
                Log.d(TAG, "Loading " + view.text.toString())

                // Launch Web Intent
                intentTab.launchUrl(this, view.text.toString().toUri())

                true
            }
            false
        }
    }
}