package com.wang.twkanviewer.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "story_locales",
    indices = [Index(value = ["story_id", "language"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = Story::class,
            parentColumns = ["id"],
            childColumns = ["story_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class StoryLocale(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "story_id") val storyId: Int,
    @ColumnInfo(name = "language") val language: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "genre") val genre: String,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "tags") val tags: String
)