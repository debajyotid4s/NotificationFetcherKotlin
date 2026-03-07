package com.example.notificationfetcher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * Main UI — status dashboard + data export controls.
 * The service runs independently of this Activity.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var btnToggle: Button
    private lateinit var btnExportJson: Button
    private lateinit var btnExportCsv: Button
    private lateinit var btnExportDb: Button
    private lateinit var exportStatus: TextView

    // Database and DAO references for export operations
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_SECURE,
            android.view.WindowManager.LayoutParams.FLAG_SECURE
        )
        setContentView(R.layout.activity_main)

        // Ensure notification channel exists
        NotificationChannelHelper.createChannel(this)

        // Initialize DB
        database = AppDatabase.getDatabase(this)

        // Bind views
        statusText   = findViewById(R.id.statusText)
        btnToggle    = findViewById(R.id.btnToggleService)
        btnExportJson = findViewById(R.id.btnExportJson)
        btnExportCsv  = findViewById(R.id.btnExportCsv)
        btnExportDb   = findViewById(R.id.btnExportDb)
        exportStatus  = findViewById(R.id.exportStatus)

        // Open system Notification Access settings
        btnToggle.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        // ── Export Buttons ──

        // Export as JSON → Downloads/NotificationExport/
        btnExportJson.setOnClickListener {
            exportData("json")
        }

        // Export as CSV → Downloads/NotificationExport/
        btnExportCsv.setOnClickListener {
            exportData("csv")
        }

        // Export raw SQLite DB → Downloads/NotificationExport/
        btnExportDb.setOnClickListener {
            exportData("db")
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    /**
     * Triggers export in a coroutine, shows progress and result to the user.
     * @param format "json", "csv", or "db"
     */
    private fun exportData(format: String) {
        exportStatus.text = getString(R.string.export_in_progress)
        setExportButtonsEnabled(false)

        // Run export on IO thread via lifecycleScope
        lifecycleScope.launch {
            val dao  = database.notificationDao()
            val file = when (format) {
                "json" -> ExportManager.exportToJson(this@MainActivity, dao)
                "csv"  -> ExportManager.exportToCsv(this@MainActivity, dao)
                "db"   -> ExportManager.exportDatabase(this@MainActivity, database)
                else   -> null
            }

            setExportButtonsEnabled(true)

            if (file != null) {
                // Show success with file path
                exportStatus.text = getString(R.string.export_success, file.name)
                exportStatus.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.success_green))

                // Offer to share the exported file immediately
                ExportManager.shareFile(this@MainActivity, file)
            } else {
                // Show failure
                exportStatus.text = getString(R.string.export_failed)
                exportStatus.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.error_red))
                Toast.makeText(this@MainActivity, getString(R.string.export_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** Enable or disable all export buttons (prevents double-tap during export) */
    private fun setExportButtonsEnabled(enabled: Boolean) {
        btnExportJson.isEnabled = enabled
        btnExportCsv.isEnabled  = enabled
        btnExportDb.isEnabled   = enabled
    }

    /** Updates status text and toggle button based on permission state */
    private fun updateUI() {
        val isEnabled = isNotificationServiceEnabled(this)
        if (isEnabled) {
            statusText.text = getString(R.string.status_running)
            statusText.setTextColor(ContextCompat.getColor(this, R.color.success_green))
            btnToggle.text      = getString(R.string.btn_permission_granted)
            btnToggle.isEnabled = false
        } else {
            statusText.text = getString(R.string.status_stopped)
            statusText.setTextColor(ContextCompat.getColor(this, R.color.error_red))
            btnToggle.text      = getString(R.string.btn_enable)
            btnToggle.isEnabled = true
        }
    }

    private fun isNotificationServiceEnabled(context: Context): Boolean {
        val flat = Settings.Secure.getString(
            context.contentResolver, "enabled_notification_listeners"
        )
        return flat != null && flat.contains(context.packageName)
    }
}