package com.wang.twkanviewer.models

import java.util.Date

data class Chapter(
    val title: String,
    val url: String,
    val uploadedAt: Date,
    val content: String?
)