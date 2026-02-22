package com.wang.twkanviewer.web

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

// Chapters
data class Chapter(val chapter: Int=0, val name: String="", val prevUrl: String="", val nextUrl: String="", var text: String="")

// Results
data class ScrapingResult(val countries: MutableList<Chapter> = mutableListOf(), var count:Int = 0)

class Scraper {
    companion object {
        fun getChapter(url: String): Chapter {
            val doc: Document = Jsoup.connect(url).get()
            val name = doc.select("h1").text()
            val text = doc.select(".novel-content").html()
            val prevUrl = doc.select(".link-prev a").attr("href")
            val nextUrl = doc.select(".link-next a").attr("href")
            return Chapter(name = name, text = text, prevUrl = prevUrl, nextUrl = nextUrl)
        }
    }
}