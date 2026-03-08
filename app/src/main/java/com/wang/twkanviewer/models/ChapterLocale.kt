package com.wang.twkanviewer.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "chapter_locales",
    foreignKeys = [
        ForeignKey(
            entity = Chapter::class,
            parentColumns = ["id"],
            childColumns = ["chapter_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ChapterLocale(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "chapter_id") val chapterId: Int,
    @ColumnInfo(name = "language") val language: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "content") var content: String?,
)