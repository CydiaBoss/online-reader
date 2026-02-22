package com.wang.twkanviewer.web

// Chapters
data class Chapter(val chapter: Int=0, val name: String="", val prevUrl: String="", val nextUrl: String="", var text: String="")

// Results
data class ScrapingResult(val countries: MutableList<Chapter> = mutableListOf(), var count:Int = 0)