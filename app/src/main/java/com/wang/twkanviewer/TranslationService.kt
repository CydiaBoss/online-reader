package com.wang.twkanviewer

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface TranslationService {
    @Headers("Content-Type: application/json+protobuf")
    @POST("v1/translateHtml")
    suspend fun translateHtml(
        @Header("X-goog-api-key") apiKey: String,
        @Header("User-Agent") userAgent: String,
        @Body body: List<@JvmSuppressWildcards Any>
    ): String
}
