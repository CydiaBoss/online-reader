package com.wang.twkanviewer.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wang.twkanviewer.models.Chapter
import com.wang.twkanviewer.models.ChapterLocale

@Dao
interface ChapterDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chapters: List<Chapter>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocale(locale: ChapterLocale)

    @Query("SELECT * FROM chapters WHERE story_id = :storyId ORDER BY 'order' ASC")
    suspend fun getChaptersForStory(storyId: Int): List<Chapter>

    @Query("SELECT * FROM chapters WHERE id = :id")
    suspend fun getById(id: Int): Chapter?

    @Query("SELECT * FROM chapter_locales WHERE chapter_id = :id AND language = :language")
    suspend fun getLocaleByChapterId(id: Int, language: String): ChapterLocale?
}