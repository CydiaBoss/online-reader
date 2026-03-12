package com.wang.twkanviewer

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    companion object {
        val SHOW_TRANSLATE = booleanPreferencesKey("show_translate")
        val CHAPTER_FONT_SIZE = floatPreferencesKey("chapter_font_size")
    }

    val showTranslate: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[SHOW_TRANSLATE] ?: false
        }

    val chapterFontSize: Flow<Float> = context.dataStore.data
        .map { preferences ->
            preferences[CHAPTER_FONT_SIZE] ?: 16f
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
}
