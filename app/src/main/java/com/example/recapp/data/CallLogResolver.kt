package com.example.recapp.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

data class CallInfo(val phoneNumber: String?, val contactName: String?, val direction: CallDirection)

/** 通話終了後に通話履歴から相手番号と発着信種別を取得する。 */
class CallLogResolver(private val context: Context) {

    /** 録音開始時点で最新の通話履歴 ID。今回の通話の行はこれより大きい ID で書き込まれる。 */
    suspend fun latestId(): Long = withContext(Dispatchers.IO) {
        if (!granted(Manifest.permission.READ_CALL_LOG)) return@withContext 0L
        context.contentResolver.query(
            CallLog.Calls.CONTENT_URI, arrayOf(CallLog.Calls._ID), null, null, "${CallLog.Calls._ID} DESC",
        )?.use { c -> if (c.moveToFirst()) c.getLong(0) else 0L } ?: 0L
    }

    /** [afterId] より新しい通話履歴の行から、今回の通話の情報を取得する。 */
    suspend fun resolve(afterId: Long): CallInfo {
        if (!granted(Manifest.permission.READ_CALL_LOG)) {
            return CallInfo(null, null, CallDirection.UNKNOWN)
        }
        // 通話履歴は通話終了直後に書き込まれるため、少し待ってから再試行する
        repeat(5) {
            withContext(Dispatchers.IO) { queryNewest(afterId) }?.let { return it }
            delay(1_000)
        }
        return CallInfo(null, null, CallDirection.UNKNOWN)
    }

    private fun queryNewest(afterId: Long): CallInfo? {
        val projection = arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.CACHED_NAME)
        val selection = "${CallLog.Calls._ID} > ?"
        val args = arrayOf(afterId.toString())
        context.contentResolver.query(
            CallLog.Calls.CONTENT_URI, projection, selection, args, "${CallLog.Calls.DATE} DESC",
        )?.use { c ->
            if (!c.moveToFirst()) return null
            val number = c.getString(0)?.takeIf { it.isNotBlank() }
            val direction = when (c.getInt(1)) {
                CallLog.Calls.INCOMING_TYPE, CallLog.Calls.ANSWERED_EXTERNALLY_TYPE -> CallDirection.INCOMING
                CallLog.Calls.OUTGOING_TYPE -> CallDirection.OUTGOING
                else -> CallDirection.UNKNOWN
            }
            val name = c.getString(2)?.takeIf { it.isNotBlank() } ?: number?.let(::lookupContactName)
            return CallInfo(number, name, direction)
        }
        return null
    }

    private fun lookupContactName(number: String): String? {
        if (!granted(Manifest.permission.READ_CONTACTS)) return null
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        return context.contentResolver.query(
            uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null,
        )?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    }

    private fun granted(permission: String) =
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
}
