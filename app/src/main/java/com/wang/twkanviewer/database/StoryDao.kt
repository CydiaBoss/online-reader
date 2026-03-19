package com.wang.twkanviewer.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.wang.twkanviewer.models.Story
import com.wang.twkanviewer.models.StoryLocale

@Dao
interface StoryDao {
    @Upsert
    suspend fun upsert(story: Story)

    @Upsert
    suspend fun upsertLocale(locale: StoryLocale)

    @Query("SELECT * FROM stories")
    suspend fun getAll(): List<Story>

    @Query("SELECT * FROM stories WHERE id = :id")
    suspend fun getById(id: Int): Story?

    @Query("SELECT * FROM story_locales WHERE story_id = :id AND language = :language")
    suspend fun getLocaleByStoryId(id: Int, language: String): StoryLocale?

    @Delete
    suspend fun delete(story: Story)
}