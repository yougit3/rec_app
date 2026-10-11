package com.example.recapp.service

import android.content.Context
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission

/** 通話状態 (TelephonyManager.CALL_STATE_*) の変化を通知する。 */
class CallStateMonitor(
    context: Context,
    private val onStateChanged: (Int) -> Unit,
) {
    private val telephony = context.getSystemService(TelephonyManager::class.java)
    private val executor = context.mainExecutor
    private var callback: Any? = null

    val isStarted: Boolean get() = callback != null

    @RequiresPermission(android.Manifest.permission.READ_PHONE_STATE)
    fun start() {
        if (callback != null) return
        callback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Api31Callback(onStateChanged).also { telephony.registerTelephonyCallback(executor, it) }
        } else {
            @Suppress("DEPRECATION")
            val listener = object : PhoneStateListener(executor) {
                @Deprecated("Deprecated in Java")
                override fun onCallStateChanged(state: Int, phoneNumber: String?) = onStateChanged(state)
            }
            @Suppress("DEPRECATION")
            telephony.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
            listener
        }
    }

    fun stop() {
        when (val cb = callback) {
            null -> return
            is PhoneStateListener -> @Suppress("DEPRECATION") telephony.listen(cb, PhoneStateListener.LISTEN_NONE)
            else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                telephony.unregisterTelephonyCallback(cb as TelephonyCallback)
            }
        }
        callback = null
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private class Api31Callback(private val onStateChanged: (Int) -> Unit) :
        TelephonyCallback(), TelephonyCallback.CallStateListener {
        override fun onCallStateChanged(state: Int) = onStateChanged(state)
    }
}
