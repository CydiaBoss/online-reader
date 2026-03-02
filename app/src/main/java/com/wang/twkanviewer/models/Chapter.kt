package com.wang.twkanviewer.models

import java.util.Date

data class Chapter(
    val id: Int,
    val title: String,
    val url: String,
    var uploadedAt: Date?,
    var content: String?
)