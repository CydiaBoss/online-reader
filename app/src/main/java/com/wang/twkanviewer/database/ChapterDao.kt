package com.wang.twkanviewer.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wang.twkanviewer.models.Chapter

@Dao
interface ChapterDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chapters: List<Chapter>)

    @Query("SELECT * FROM chapters WHERE story_id = :storyId ORDER BY order ASC")
    suspend fun getChaptersForStory(storyId: Int): List<Chapter>
}