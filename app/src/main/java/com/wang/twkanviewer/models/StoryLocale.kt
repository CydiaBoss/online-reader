package com.wang.twkanviewer.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "story_locales",
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
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "story_id") val storyId: Int,
    @ColumnInfo(name = "language") val language: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "genre") val genre: String,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "tags") val tags: List<String>
)