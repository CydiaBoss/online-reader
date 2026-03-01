package com.wang.twkanviewer.models

import java.util.Date

data class Story(
    val id: Int,
    val url: String,
    val title: String,
    val imgUrl: String,
    val genre: String,
    val author: String,
    val description: String,
    val completed: Boolean,
    val wordCount: Int,
    val lastUpdated: Date,
    val tags: List<String>,
    val chapters: MutableList<Chapter>
)