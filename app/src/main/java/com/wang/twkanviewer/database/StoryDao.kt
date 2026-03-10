package com.wang.twkanviewer.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wang.twkanviewer.models.Story
import com.wang.twkanviewer.models.StoryLocale

@Dao
interface StoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(story: Story)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocale(locale: StoryLocale)

    @Query("SELECT * FROM stories")
    suspend fun getAll(): List<Story>

    @Query("SELECT * FROM stories WHERE id = :id")
    suspend fun getById(id: Int): Story?

    @Query("SELECT * FROM story_locales WHERE story_id = :id AND language = :language")
    suspend fun getLocaleByStoryId(id: Int, language: String): StoryLocale?
}