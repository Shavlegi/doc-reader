package com.shavlegi.docreader.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.shavlegi.docreader.model.RecentFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "doc_reader")

/** Persists the "recent files" list locally (no account, no cloud). */
class RecentFilesStore(private val context: Context) {

    private val key = stringPreferencesKey("recent_files")
    private val json = Json { ignoreUnknownKeys = true }

    val recents: Flow<List<RecentFile>> = context.dataStore.data.map { prefs ->
        val raw = prefs[key] ?: return@map emptyList()
        runCatching { json.decodeFromString<List<RecentFile>>(raw) }.getOrDefault(emptyList())
    }

    suspend fun add(file: RecentFile) {
        context.dataStore.edit { prefs ->
            val current = read(prefs)
            val updated = (listOf(file) + current.filterNot { it.uri == file.uri })
                .take(MAX_FILES)
            prefs[key] = json.encodeToString(updated)
        }
    }

    suspend fun remove(uri: String) {
        context.dataStore.edit { prefs ->
            val updated = read(prefs).filterNot { it.uri == uri }
            prefs[key] = json.encodeToString(updated)
        }
    }

    suspend fun clear() {
        context.dataStore.edit { prefs ->
            prefs.remove(key)
        }
    }

    private fun read(prefs: Preferences): List<RecentFile> {
        val raw = prefs[key] ?: return emptyList()
        return runCatching { json.decodeFromString<List<RecentFile>>(raw) }
            .getOrDefault(emptyList())
    }

    companion object {
        const val MAX_FILES = 20
    }
}
