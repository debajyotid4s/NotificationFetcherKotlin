package com.example.notificationfetcher

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var btnToggle: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        btnToggle = findViewById(R.id.btnToggleService)

        btnToggle.setOnClickListener {
            // Open the specific settings page to grant permission
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            startActivity(intent)
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
}