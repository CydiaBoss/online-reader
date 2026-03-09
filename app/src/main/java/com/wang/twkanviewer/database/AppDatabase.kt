package com.wang.twkanviewer.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.wang.twkanviewer.models.Chapter
import com.wang.twkanviewer.models.ChapterLocale
import com.wang.twkanviewer.models.Story
import com.wang.twkanviewer.models.StoryLocale

@Database(entities = [
    Story::class,
    StoryLocale::class,
    Chapter::class,
    ChapterLocale::class
], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun storyDao(): StoryDao
    abstract fun chapterDao(): ChapterDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}