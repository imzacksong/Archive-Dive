package com.example.archivetok.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [BookmarkEntity::class, ExhibitEntity::class, ExhibitItemEntity::class], version = 5, exportSchema = false)
abstract class ArchiveDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun exhibitDao(): ExhibitDao

    companion object {
        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Add mediaType column to exhibits table with default value 'video'
                database.execSQL("ALTER TABLE exhibits ADD COLUMN mediaType TEXT NOT NULL DEFAULT 'video'")
            }
        }
    }
}
