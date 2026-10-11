package com.example.recapp.service

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.telephony.TelephonyManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import com.example.recapp.R
import com.example.recapp.RecApp
import com.example.recapp.data.CallLogResolver
import com.example.recapp.data.RecordingNaming
import com.example.recapp.recorder.AudioSourceOption
import com.example.recapp.recorder.CallRecorder
import com.example.recapp.ui.MainActivity
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Android 10 以降、有効なアクセシビリティサービスは通話中でもマイク入力をキャプチャできる。
 * このサービス内で通話状態を監視し、通話中のみ録音する。
 */
class CallRecorderAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // 保存処理はサービス破棄後も完了させる必要があるため、scope とは別にキャンセルしない
    private val saveScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val app get() = application as RecApp
    private lateinit var recorder: CallRecorder
    private lateinit var monitor: CallStateMonitor
    private lateinit var callLog: CallLogResolver

    private var inCall = false
    private var callSeq = 0
    private var callLogBaselineId = 0L
    private var currentFile: File? = null
    private var startedAt = 0L
    private var startedElapsed = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        recorder = CallRecorder(this)
        monitor = CallStateMonitor(this, ::onCallStateChanged)
        callLog = CallLogResolver(this)
        ensureMonitoring()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // サービス有効化後に電話の権限が付与された場合に備え、監視の開始を再試行する
        ensureMonitoring()
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (::monitor.isInitialized) monitor.stop()
        if (::recorder.isInitialized && recorder.isRecording) stopRecording()
        scope.cancel()
        super.onDestroy()
    }

    private fun ensureMonitoring() {
        if (!::monitor.isInitialized || monitor.isStarted) return
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            monitor.start()
            Log.i(TAG, "call state monitoring started")
        }
    }

    private fun onCallStateChanged(state: Int) {
        when (state) {
            TelephonyManager.CALL_STATE_OFFHOOK -> if (!inCall) {
                inCall = true
                val seq = ++callSeq
                scope.launch {
                    val settings = app.settingsRepository.current()
                    if (!settings.autoRecord) return@launch
                    val baselineId = callLog.latestId()
                    startRecording(seq, settings.audioSource, baselineId)
                }
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                inCall = false
                if (recorder.isRecording) stopRecording()
            }
        }
    }

    /** 中断を挟まずに呼ぶこと。待機中に通話が終了・再開した場合は seq の不一致で破棄する。 */
    private fun startRecording(seq: Int, audioSource: AudioSourceOption, baselineId: Long) {
        if (!inCall || seq != callSeq || recorder.isRecording) return
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "RECORD_AUDIO not granted")
            return
        }
        startedAt = System.currentTimeMillis()
        startedElapsed = SystemClock.elapsedRealtime()
        val file = app.recordingRepository.newTempFile(startedAt)
        if (recorder.start(file, audioSource)) {
            currentFile = file
            callLogBaselineId = baselineId
            showRecordingNotification()
        }
    }

    private fun stopRecording() {
        val file = currentFile ?: return
        currentFile = null
        val ok = recorder.stop()
        cancelRecordingNotification()
        val durationMs = SystemClock.elapsedRealtime() - startedElapsed
        val callStartedAt = startedAt
        val baselineId = callLogBaselineId
        saveScope.launch {
            withContext(NonCancellable) {
                val settings = app.settingsRepository.current()
                if (!ok || (settings.deleteShortRecordings && RecordingNaming.isTooShort(durationMs))) {
                    file.delete()
                    return@withContext
                }
                val info = callLog.resolve(baselineId)
                app.recordingRepository.save(
                    tempFile = file,
                    phoneNumber = info.phoneNumber,
                    contactName = info.contactName,
                    direction = info.direction,
                    startedAt = callStartedAt,
                    durationMs = durationMs,
                )
            }
        }
    }

    private fun showRecordingNotification() {
        // POST_NOTIFICATIONS は API 33 以降の実行時権限。それ未満では常に表示できる
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val contentIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, RecApp.CHANNEL_RECORDING)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notification_recording))
            .setOngoing(true)
            .setUsesChronometer(true)
            .setContentIntent(contentIntent)
            .build()
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }

    private fun cancelRecordingNotification() {
        getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
    }

    private companion object {
        const val TAG = "CallRecorderService"
        const val NOTIFICATION_ID = 1
    }
}
