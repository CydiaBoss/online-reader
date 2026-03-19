package com.wang.twkanviewer

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    companion object {
        val SHOW_TRANSLATE = booleanPreferencesKey("show_translate")
        val CHAPTER_FONT_SIZE = floatPreferencesKey("chapter_font_size")
        val CHAPTER_FONT_FAMILY = stringPreferencesKey("chapter_font_family")
        val USE_EXTERNAL_TRANSLATOR = booleanPreferencesKey("use_external_translator")
        val TRANSLATOR_API_KEY = stringPreferencesKey("translator_api_key")
        val USER_AGENT = stringPreferencesKey("user_agent")
    }

    val showTranslate: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[SHOW_TRANSLATE] ?: false
        }

    val chapterFontSize: Flow<Float> = context.dataStore.data
        .map { preferences ->
            preferences[CHAPTER_FONT_SIZE] ?: 16f
        }

    val chapterFontFamily: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[CHAPTER_FONT_FAMILY] ?: "Default"
        }

    val useExternalTranslator: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[USE_EXTERNAL_TRANSLATOR] ?: false
        }

    val translatorApiKey: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[TRANSLATOR_API_KEY] ?: BuildConfig.TRANSLATOR_API_KEY
        }

    val userAgent: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[USER_AGENT] ?: "Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.198 Mobile Safari/537.36"
        }

    suspend fun setShowTranslate(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_TRANSLATE] = enabled
        }
    }

    suspend fun setChapterFontSize(size: Float) {
        context.dataStore.edit { preferences ->
            preferences[CHAPTER_FONT_SIZE] = size
        }
    }

    suspend fun setChapterFontFamily(fontFamily: String) {
        context.dataStore.edit { preferences ->
            preferences[CHAPTER_FONT_FAMILY] = fontFamily
        }
    }

    suspend fun setUseExternalTranslator(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[USE_EXTERNAL_TRANSLATOR] = enabled
        }
    }

    suspend fun setTranslatorApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[TRANSLATOR_API_KEY] = apiKey
        }
    }

    suspend fun setUserAgent(userAgent: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_AGENT] = userAgent
        }
    }
}
