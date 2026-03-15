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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllLocale(locales: List<ChapterLocale>)

    @Query("SELECT * FROM chapters WHERE story_id = :storyId ORDER BY `order` ASC")
    suspend fun getChaptersForStory(storyId: Int): List<Chapter>

    @Query("SELECT * FROM chapters WHERE id = :id")
    suspend fun getById(id: Int): Chapter?

    @Query("SELECT * FROM chapter_locales WHERE chapter_id = :id AND language = :language")
    suspend fun getLocaleByChapterId(id: Int, language: String): ChapterLocale?

    @Query("SELECT cl.* FROM chapter_locales cl JOIN chapters c ON cl.chapter_id = c.id WHERE c.story_id = :storyId AND cl.language = :language")
    suspend fun getLocalesForStory(storyId: Int, language: String): List<ChapterLocale>
}