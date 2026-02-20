package com.example.archivetok.di

import android.content.Context
import androidx.room.Room
import com.example.archivetok.data.local.ArchiveDatabase
import com.example.archivetok.data.local.BookmarkDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ArchiveDatabase {
        return Room.databaseBuilder(
            context,
            ArchiveDatabase::class.java,
            "archive_db"
        )
        .addMigrations(ArchiveDatabase.MIGRATION_4_5)
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideBookmarkDao(database: ArchiveDatabase): BookmarkDao {
        return database.bookmarkDao()
    }

    @Provides
    fun provideExhibitDao(database: ArchiveDatabase): com.example.archivetok.data.local.ExhibitDao {
        return database.exhibitDao()
    }
}
