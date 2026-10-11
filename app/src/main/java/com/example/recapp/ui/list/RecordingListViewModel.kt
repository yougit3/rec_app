package com.example.recapp.ui.list

import android.app.Application
import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.recapp.RecApp
import com.example.recapp.data.Recording
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecordingListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as RecApp).recordingRepository

    val recordings: StateFlow<List<Recording>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _playingId = MutableStateFlow<Long?>(null)
    val playingId: StateFlow<Long?> = _playingId.asStateFlow()

    private var player: MediaPlayer? = null

    fun togglePlay(recording: Recording) {
        val wasPlaying = _playingId.value == recording.id
        stopPlayback()
        if (wasPlaying) return
        try {
            player = MediaPlayer().apply {
                setDataSource(recording.filePath)
                setOnCompletionListener { stopPlayback() }
                prepare()
                start()
            }
            _playingId.value = recording.id
        } catch (e: Exception) {
            Log.w(TAG, "playback failed", e)
            stopPlayback()
        }
    }

    fun delete(recording: Recording) {
        if (_playingId.value == recording.id) stopPlayback()
        viewModelScope.launch { repository.delete(recording) }
    }

    private fun stopPlayback() {
        player?.release()
        player = null
        _playingId.value = null
    }

    override fun onCleared() {
        stopPlayback()
    }

    private companion object {
        const val TAG = "RecordingList"
    }
}
