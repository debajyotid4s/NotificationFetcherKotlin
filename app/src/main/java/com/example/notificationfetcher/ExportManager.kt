package com.example.notificationfetcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Handles all data export operations.
 * Supports JSON, CSV, and raw SQLite DB export.
 * All exports are saved to the public Downloads folder
 * so they are accessible from the Files app — no root or PC needed.
 */
object ExportManager {

    private val TAG = "ExportManager"

    // Timestamp format used in exported file names
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())

    // ─────────────────────────────────────────────
    // JSON EXPORT
    // ─────────────────────────────────────────────

    /**
     * Exports all notifications to a JSON file in the Downloads folder.
     * JSON is the most portable format — readable by Python, JavaScript,
     * Excel (via Power Query), and most data analysis tools.
     *
     * Output: Downloads/NotificationExport/notifications_YYYY-MM-DD_HH-mm-ss.json
     *
     * @return The exported File, or null if export failed
     */
    suspend fun exportToJson(context: Context, dao: NotificationDao): File? {
        return withContext(Dispatchers.IO) {
            try {
                // Fetch all records from Room database
                val notifications = dao.getAllNotificationsSync()
                Log.d(TAG, "Exporting ${notifications.size} records to JSON")

                // Use pretty-printing Gson so the JSON is human-readable
                val gson = GsonBuilder()
                    .setPrettyPrinting()
                    .serializeNulls() // Include null fields so schema is consistent
                    .create()

                val json = gson.toJson(notifications)

                // Save to Downloads/NotificationExport/
                val file = getExportFile(context, "json")
                file.writeText(json, Charsets.UTF_8)

                Log.d(TAG, "JSON export saved: ${file.absolutePath}")
                file
            } catch (e: Exception) {
                Log.e(TAG, "JSON export failed", e)
                null
            }
        }
    }

    // ─────────────────────────────────────────────
    // CSV EXPORT
    // ─────────────────────────────────────────────

    /**
     * Exports all notifications to a CSV file in the Downloads folder.
     * CSV opens directly in Excel, Google Sheets, or any spreadsheet app.
     *
     * Output: Downloads/NotificationExport/notifications_YYYY-MM-DD_HH-mm-ss.csv
     *
     * @return The exported File, or null if export failed
     */
    suspend fun exportToCsv(context: Context, dao: NotificationDao): File? {
        return withContext(Dispatchers.IO) {
            try {
                val notifications = dao.getAllNotificationsSync()
                Log.d(TAG, "Exporting ${notifications.size} records to CSV")

                val file = getExportFile(context, "csv")

                FileWriter(file, Charsets.UTF_8).use { writer ->
                    // Write CSV header row
                    writer.appendLine(
                        "id,postedAt,postedAtReadable,packageName,appName," +
                                "title,text,bigText,subText,category,channelId," +
                                "notificationKey,isOngoing,isGroupSummary,importanceHint"
                    )

                    // Write one row per notification
                    notifications.forEach { n ->
                        writer.appendLine(
                            listOf(
                                n.id,
                                n.postedAt,
                                // Human-readable timestamp alongside raw epoch ms
                                SimpleDateFormat(
                                    "yyyy-MM-dd HH:mm:ss",
                                    Locale.getDefault()
                                ).format(Date(n.postedAt)),
                                n.packageName.csvSafe(),
                                n.appName.csvSafe(),
                                n.title.csvSafe(),
                                n.text.csvSafe(),
                                n.bigText.csvSafe(),
                                n.subText.csvSafe(),
                                n.category.csvSafe(),
                                n.channelId.csvSafe(),
                                n.notificationKey.csvSafe(),
                                n.isOngoing,
                                n.isGroupSummary,
                                n.importanceHint ?: ""
                            ).joinToString(",")
                        )
                    }
                }

                Log.d(TAG, "CSV export saved: ${file.absolutePath}")
                file
            } catch (e: Exception) {
                Log.e(TAG, "CSV export failed", e)
                null
            }
        }
    }

    // ─────────────────────────────────────────────
    // RAW DATABASE EXPORT
    // ─────────────────────────────────────────────

    /**
     * Copies the raw SQLite database file to the Downloads folder.
     * This gives you the actual .db file which can be opened with
     * DB Browser for SQLite, DBeaver, or queried with Python (sqlite3 module).
     *
     * IMPORTANT: Closes any pending WAL transactions by checkpointing first.
     *
     * Output: Downloads/NotificationExport/notifications_YYYY-MM-DD_HH-mm-ss.db
     *
     * @return The exported File, or null if export failed
     */
    suspend fun exportDatabase(context: Context, db: AppDatabase): File? {
        return withContext(Dispatchers.IO) {
            try {
                // Force all pending WAL (Write-Ahead Log) writes into the main DB file
                // This ensures the exported file contains ALL data
                db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()

                val dbPath = context.getDatabasePath("notification_database")
                val destFile = getExportFile(context, "db")

                // Copy the database file byte-by-byte to Downloads
                dbPath.copyTo(destFile, overwrite = true)

                Log.d(TAG, "DB export saved: ${destFile.absolutePath}")
                destFile
            } catch (e: Exception) {
                Log.e(TAG, "DB export failed", e)
                null
            }
        }
    }

    // ─────────────────────────────────────────────
    // SHARE VIA INTENT
    // ─────────────────────────────────────────────

    /**
     * Opens Android's share sheet for any exported file.
     * Allows sharing via WhatsApp, Gmail, Drive, Telegram, etc.
     *
     * @param context Activity context (needed to start the share intent)
     * @param file The exported file to share
     */
    fun shareFile(context: Context, file: File) {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = when (file.extension) {
                "json" -> "application/json"
                "csv"  -> "text/csv"
                "db"   -> "application/octet-stream"
                else   -> "*/*"
            }
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Notification Export — ${file.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Export Via…"))
    }

    // ─────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────

    /**
     * Creates the export file in Downloads/NotificationExport/.
     * Creates the directory if it doesn't exist yet.
     */
    private fun getExportFile(context: Context, extension: String): File {
        val exportDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "NotificationExport" // Subfolder inside Downloads
        )
        if (!exportDir.exists()) exportDir.mkdirs()

        val timestamp = dateFormat.format(Date())
        return File(exportDir, "notifications_$timestamp.$extension")
    }

    /**
     * Makes a string safe for CSV by:
     *  - Replacing null with empty string
     *  - Wrapping in quotes if it contains commas, quotes, or newlines
     *  - Escaping internal double-quotes by doubling them
     */
    private fun String?.csvSafe(): String {
        if (this == null) return ""
        val escaped = this.replace("\"", "\"\"") // Escape existing quotes
        return if (this.contains(",") || this.contains("\"") || this.contains("\n")) {
            "\"$escaped\""  // Wrap in quotes if special characters present
        } else {
            escaped
        }
    }
}