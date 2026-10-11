package com.example.recapp.recorder

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

enum class AudioSourceOption(val source: Int, val label: String) {
    VOICE_RECOGNITION(MediaRecorder.AudioSource.VOICE_RECOGNITION, "音声認識 (推奨)"),
    MIC(MediaRecorder.AudioSource.MIC, "マイク"),
    VOICE_COMMUNICATION(MediaRecorder.AudioSource.VOICE_COMMUNICATION, "通話用マイク");

    companion object {
        /** 選択された音源を先頭に、残りを定義順で試す。 */
        fun fallbackOrder(preferred: AudioSourceOption): List<AudioSourceOption> =
            listOf(preferred) + entries.filter { it != preferred }
    }
}

class CallRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null

    val isRecording: Boolean get() = recorder != null

    /** 録音を開始する。いずれの音源でも開始できなければ false。 */
    fun start(output: File, preferred: AudioSourceOption): Boolean {
        check(recorder == null) { "already recording" }
        for (option in AudioSourceOption.fallbackOrder(preferred)) {
            val r = newRecorder()
            try {
                r.setAudioSource(option.source)
                r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                r.setAudioSamplingRate(44_100)
                r.setAudioEncodingBitRate(128_000)
                r.setAudioChannels(1)
                r.setOutputFile(output.absolutePath)
                r.prepare()
                r.start()
                recorder = r
                Log.i(TAG, "recording started with ${option.name}")
                return true
            } catch (e: Exception) {
                Log.w(TAG, "failed to start with ${option.name}", e)
                r.release()
                output.delete()
            }
        }
        return false
    }

    /** 録音を停止する。有効な音声が書き込まれていれば true。 */
    fun stop(): Boolean {
        val r = recorder ?: return false
        recorder = null
        return try {
            r.stop()
            true
        } catch (e: RuntimeException) {
            // 開始直後に停止した場合など、データが無いと例外になる
            Log.w(TAG, "stop failed", e)
            false
        } finally {
            r.release()
        }
    }

    private fun newRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()

    private companion object {
        const val TAG = "CallRecorder"
    }
}
