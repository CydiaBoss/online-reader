package com.wang.twkanviewer.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(
            entity = Story::class,
            parentColumns = ["id"],
            childColumns = ["story_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Chapter(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "order") val order: Int,
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "uploaded_at") var uploadedAt: Date?,
    @ColumnInfo(name = "content") var content: List<String>,
    @ColumnInfo(name = "story_id") var storyId: Int = 0
)