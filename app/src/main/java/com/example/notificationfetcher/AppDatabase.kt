package com.example.notificationfetcher

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room database singleton for the app.
 *
 * - version: Increment this whenever you change the schema (add/remove columns).
 *   Always provide a Migration object when doing so, or use fallbackToDestructiveMigration()
 *   during development (which wipes and recreates the DB).
 * - exportSchema: Set to true in production to keep a schema history for migration tracking.
 */
@Database(
    entities = [NotificationEntity::class],
    version = 1,
    exportSchema = false // TODO: set to true and add schema export dir in production
)
abstract class AppDatabase : RoomDatabase() {

    /** Provides access to all notification CRUD operations */
    abstract fun notificationDao(): NotificationDao

    companion object {

        /**
         * @Volatile ensures that INSTANCE is always read from main memory,
         * not from a CPU cache — critical for thread safety.
         */
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Returns the singleton database instance.
         * Uses double-checked locking to avoid race conditions
         * when multiple threads call this simultaneously.
         *
         * @param context Application context (use applicationContext to avoid memory leaks)
         */
        fun getDatabase(context: Context): AppDatabase {
            // Return existing instance immediately if available (fast path)
            return INSTANCE ?: synchronized(this) {
                // Second check inside lock in case another thread already created it
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notification_database" // SQLite file name on disk
                )
                    // Uncomment during development to allow schema changes without migration:
                    // .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}