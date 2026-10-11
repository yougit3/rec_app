package com.example.recapp.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.recapp.recorder.AudioSourceOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

data class Settings(
    val autoRecord: Boolean = true,
    val audioSource: AudioSourceOption = AudioSourceOption.VOICE_RECOGNITION,
    val deleteShortRecordings: Boolean = true,
)

class SettingsRepository(private val context: Context) {

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            autoRecord = prefs[AUTO_RECORD] ?: true,
            audioSource = prefs[AUDIO_SOURCE]
                ?.let { name -> AudioSourceOption.entries.find { it.name == name } }
                ?: AudioSourceOption.VOICE_RECOGNITION,
            deleteShortRecordings = prefs[DELETE_SHORT] ?: true,
        )
    }

    suspend fun current(): Settings = settings.first()

    suspend fun setAutoRecord(value: Boolean) = context.dataStore.edit { it[AUTO_RECORD] = value }

    suspend fun setAudioSource(value: AudioSourceOption) = context.dataStore.edit { it[AUDIO_SOURCE] = value.name }

    suspend fun setDeleteShortRecordings(value: Boolean) = context.dataStore.edit { it[DELETE_SHORT] = value }

    private companion object {
        val AUTO_RECORD = booleanPreferencesKey("auto_record")
        val AUDIO_SOURCE = stringPreferencesKey("audio_source")
        val DELETE_SHORT = booleanPreferencesKey("delete_short")
    }
}
