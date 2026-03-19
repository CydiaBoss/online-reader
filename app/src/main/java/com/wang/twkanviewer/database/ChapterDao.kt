package com.wang.twkanviewer.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.wang.twkanviewer.models.Chapter
import com.wang.twkanviewer.models.ChapterLocale

@Dao
interface ChapterDao {
    @Upsert
    suspend fun upsert(chapter: Chapter)

    @Upsert
    suspend fun upsertAll(chapters: List<Chapter>)

    @Upsert
    suspend fun upsertLocale(locale: ChapterLocale)

    @Upsert
    suspend fun upsertAllLocale(locales: List<ChapterLocale>)

    @Query("SELECT * FROM chapters WHERE story_id = :storyId ORDER BY `order` ASC")
    suspend fun getChaptersForStory(storyId: Int): List<Chapter>

    @Query("SELECT * FROM chapters WHERE id = :id")
    suspend fun getById(id: Int): Chapter?

    @Query("SELECT * FROM chapter_locales WHERE chapter_id = :id AND language = :language")
    suspend fun getLocaleByChapterId(id: Int, language: String): ChapterLocale?

    @Query("SELECT cl.* FROM chapter_locales cl JOIN chapters c ON cl.chapter_id = c.id WHERE c.story_id = :storyId AND cl.language = :language")
    suspend fun getLocalesForStory(storyId: Int, language: String): List<ChapterLocale>
}