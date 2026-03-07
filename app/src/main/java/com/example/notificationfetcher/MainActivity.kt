package com.example.notificationfetcher

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var btnToggle: Button
    private lateinit var btnExportJson: Button
    private lateinit var btnExportCsv: Button
    private lateinit var btnExportDb: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Block screenshots and hide from Recents for privacy
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        btnToggle = findViewById(R.id.btnToggleService)
        btnExportJson = findViewById(R.id.btnExportJson)
        btnExportCsv = findViewById(R.id.btnExportCsv)
        btnExportDb = findViewById(R.id.btnExportDb)

        btnToggle.setOnClickListener {
            // Open the specific settings page to grant permission
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            startActivity(intent)
        }

        val exportManager = ExportManager(this)

        btnExportJson.setOnClickListener {
            lifecycleScope.launch {
                val notifications = withContext(Dispatchers.IO) {
                    AppDatabase.getDatabase(this@MainActivity).notificationDao().getAllNotificationsSync()
                }
                val path = withContext(Dispatchers.IO) { exportManager.exportJson(notifications) }
                showExportResult(path, "JSON")
            }
        }

        btnExportCsv.setOnClickListener {
            lifecycleScope.launch {
                val notifications = withContext(Dispatchers.IO) {
                    AppDatabase.getDatabase(this@MainActivity).notificationDao().getAllNotificationsSync()
                }
                val path = withContext(Dispatchers.IO) { exportManager.exportCsv(notifications) }
                showExportResult(path, "CSV")
            }
        }

        btnExportDb.setOnClickListener {
            lifecycleScope.launch {
                val path = withContext(Dispatchers.IO) { exportManager.exportDb() }
                showExportResult(path, "DB")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        val isEnabled = isNotificationServiceEnabled(this)
        if (isEnabled) {
            statusText.text = "Status: RUNNING\nCapturing notifications in real-time."
            statusText.setTextColor(Color.parseColor("#4CAF50")) // Green
            btnToggle.text = "Permission Granted"
            btnToggle.isEnabled = false
        } else {
            statusText.text = "Status: STOPPED\nPermission required to start."
            statusText.setTextColor(Color.parseColor("#F44336")) // Red
            btnToggle.text = "Enable Collection"
            btnToggle.isEnabled = true
        }
    }

    private fun isNotificationServiceEnabled(context: Context): Boolean {
        val pkgName = context.packageName
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(pkgName)
    }

    private fun showExportResult(path: String?, format: String) {
        val message = if (path != null) {
            "$format exported:\n$path"
        } else {
            "$format export failed."
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}