package com.example.recapp.util

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import com.example.recapp.service.CallRecorderAccessibilityService

object AccessibilityUtil {
    fun isServiceEnabled(context: Context): Boolean {
        val expected = ComponentName(context, CallRecorderAccessibilityService::class.java)
        val enabled = Settings.Secure.getString(
            context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == expected }
    }
}
