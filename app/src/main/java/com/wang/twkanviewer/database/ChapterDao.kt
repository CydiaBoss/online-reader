package com.wang.twkanviewer.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.wang.twkanviewer.models.Chapter
import com.wang.twkanviewer.models.ChapterLocale

@Dao
interface ChapterDao {
    @Upsert
    suspend fun upsert(chapter: Chapter)

    @Upsert
    suspend fun upsertAll(chapters: List<Chapter>)

    @Query("UPDATE chapters SET title = :title, `order` = :order, url = :url WHERE id = :id")
    suspend fun updateMetadata(id: Int, title: String, order: Int, url: String)

    @Upsert
    suspend fun upsertLocale(locale: ChapterLocale)

    @Upsert
    suspend fun upsertAllLocale(locales: List<ChapterLocale>)

    @Query("UPDATE chapter_locales SET title = :title WHERE chapter_id = :chapterId AND language = :language")
    suspend fun updateLocaleTitle(chapterId: Int, language: String, title: String)

    @Query("DELETE FROM chapter_locales WHERE chapter_id = :chapterId")
    suspend fun deleteLocalesForChapter(chapterId: Int)

    @Query("UPDATE chapters SET content = '', uploaded_at = NULL WHERE id = :chapterId")
    suspend fun clearChapterContent(chapterId: Int)

    @Query("SELECT * FROM chapters WHERE story_id = :storyId ORDER BY `order` ASC")
    suspend fun getChaptersForStory(storyId: Int): List<Chapter>

    @Query("SELECT * FROM chapters WHERE id = :id")
    suspend fun getById(id: Int): Chapter?

    @Query("SELECT * FROM chapter_locales WHERE chapter_id = :id AND language = :language")
    suspend fun getLocaleByChapterId(id: Int, language: String): ChapterLocale?

    @Query("SELECT cl.* FROM chapter_locales cl JOIN chapters c ON cl.chapter_id = c.id WHERE c.story_id = :storyId AND cl.language = :language")
    suspend fun getLocalesForStory(storyId: Int, language: String): List<ChapterLocale>

    @Transaction
    suspend fun safeUpsertAll(chapters: List<Chapter>) {
        for (chapter in chapters) {
            val existing = getById(chapter.id)
            if (existing == null) {
                upsert(chapter)
            } else {
                updateMetadata(chapter.id, chapter.title, chapter.order, chapter.url)
            }
        }
    }

    @Transaction
    suspend fun safeUpsertAllLocales(locales: List<ChapterLocale>) {
        for (locale in locales) {
            // Ensure the parent chapter exists before inserting the locale to avoid FOREIGN KEY constraint failure
            if (getById(locale.chapterId) == null) continue

            val existing = getLocaleByChapterId(locale.chapterId, locale.language)
            if (existing == null) {
                upsertLocale(locale)
            } else {
                updateLocaleTitle(locale.chapterId, locale.language, locale.title)
            }
        }
    }
}
