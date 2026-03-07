package com.example.notificationfetcher

import android.content.Context
import android.os.Environment
import android.util.Log
import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExportManager(private val context: Context) {

    private val TAG = "ExportManager"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
    private val exportDir: File
        get() {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "NotificationExport"
            )
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    /** Export all notifications to a JSON file and return the file path, or null on error. */
    suspend fun exportJson(notifications: List<NotificationEntity>): String? {
        return try {
            val fileName = "notifications_${dateFormat.format(Date())}.json"
            val file = File(exportDir, fileName)
            val gson = GsonBuilder().setPrettyPrinting().create()
            file.writeText(gson.toJson(notifications))
            Log.d(TAG, "JSON exported: ${file.absolutePath}")
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "JSON export failed", e)
            null
        }
    }

    /** Export all notifications to a CSV file and return the file path, or null on error. */
    suspend fun exportCsv(notifications: List<NotificationEntity>): String? {
        return try {
            val fileName = "notifications_${dateFormat.format(Date())}.csv"
            val file = File(exportDir, fileName)
            val sb = StringBuilder()
            // Header row
            sb.appendLine(
                "id,postedAt,packageName,appName,title,text,bigText,subText," +
                "category,channelId,notificationKey,isOngoing,isGroupSummary,importanceHint,label"
            )
            // Data rows
            for (n in notifications) {
                sb.appendLine(
                    "${n.id},${n.postedAt},${n.packageName.csv()},${n.appName.csv()}," +
                    "${n.title.csv()},${n.text.csv()},${n.bigText.csv()},${n.subText.csv()}," +
                    "${n.category.csv()},${n.channelId.csv()},${n.notificationKey.csv()}," +
                    "${n.isOngoing},${n.isGroupSummary},${n.importanceHint ?: ""},${n.label ?: ""}"
                )
            }
            file.writeText(sb.toString())
            Log.d(TAG, "CSV exported: ${file.absolutePath}")
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "CSV export failed", e)
            null
        }
    }

    /** Copy the Room SQLite database file to Downloads and return the destination path, or null on error. */
    suspend fun exportDb(): String? {
        return try {
            val dbFile = context.getDatabasePath("notification_database")
            if (!dbFile.exists()) {
                Log.e(TAG, "Database file not found")
                return null
            }
            val fileName = "notifications_${dateFormat.format(Date())}.db"
            val dest = File(exportDir, fileName)
            FileInputStream(dbFile).use { input ->
                FileOutputStream(dest).use { output ->
                    input.copyTo(output)
                }
            }
            Log.d(TAG, "DB exported: ${dest.absolutePath}")
            dest.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "DB export failed", e)
            null
        }
    }

    /** Wrap a nullable string value in CSV-safe double quotes, escaping inner quotes. */
    private fun String?.csv(): String {
        if (this == null) return ""
        val escaped = this.replace("\"", "\"\"")
        return "\"$escaped\""
    }
}