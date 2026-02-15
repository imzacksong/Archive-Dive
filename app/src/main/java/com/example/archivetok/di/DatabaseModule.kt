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
        ).build()
    }

    @Provides
    fun provideBookmarkDao(database: ArchiveDatabase): BookmarkDao {
        return database.bookmarkDao()
    }
}
