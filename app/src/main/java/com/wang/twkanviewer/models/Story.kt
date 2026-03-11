package com.wang.twkanviewer.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "stories")
data class Story(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "img_url") val imgUrl: String,
    @ColumnInfo(name = "genre") val genre: String,
    @ColumnInfo(name = "author") val author: String,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "completed") val completed: Boolean,
    @ColumnInfo(name = "word_count") val wordCount: Int,
    @ColumnInfo(name = "last_updated") val lastUpdated: Date,
    @ColumnInfo(name = "tags") val tags: List<String>,
    @ColumnInfo(name = "bookmarked_chapter_id") var bookmarkedChapterId: Int? = null
)