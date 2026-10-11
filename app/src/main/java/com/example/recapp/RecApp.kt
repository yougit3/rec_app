package com.example.recapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.example.recapp.data.AppDatabase
import com.example.recapp.data.RecordingRepository
import com.example.recapp.data.SettingsRepository

class RecApp : Application() {

    val recordingRepository: RecordingRepository by lazy {
        RecordingRepository(AppDatabase.get(this).recordingDao(), filesDir)
    }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(
            CHANNEL_RECORDING,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_RECORDING = "recording"
    }
}
