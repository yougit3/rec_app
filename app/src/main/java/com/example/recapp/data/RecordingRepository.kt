package com.example.recapp.data

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class RecordingRepository(
    private val dao: RecordingDao,
    filesDir: File,
) {
    val recordingsDir: File = File(filesDir, "recordings")

    fun observeAll(): Flow<List<Recording>> = dao.observeAll()

    /** 録音開始時点では番号が未確定のため、仮の名前で作成し保存時にリネームする。 */
    fun newTempFile(startedAt: Long): File {
        recordingsDir.mkdirs()
        return File(recordingsDir, RecordingNaming.fileName(startedAt, null).replace(".m4a", ".tmp.m4a"))
    }

    suspend fun save(
        tempFile: File,
        phoneNumber: String?,
        contactName: String?,
        direction: CallDirection,
        startedAt: Long,
        durationMs: Long,
    ): Recording = withContext(Dispatchers.IO) {
        val target = File(recordingsDir, RecordingNaming.fileName(startedAt, phoneNumber))
        val file = if (tempFile.renameTo(target)) target else tempFile
        val recording = Recording(
            filePath = file.absolutePath,
            phoneNumber = phoneNumber,
            contactName = contactName,
            direction = direction,
            startedAt = startedAt,
            durationMs = durationMs,
        )
        recording.copy(id = dao.insert(recording))
    }

    suspend fun delete(recording: Recording) = withContext(Dispatchers.IO) {
        File(recording.filePath).delete()
        dao.delete(recording)
    }
}

object RecordingNaming {
    const val MIN_DURATION_MS = 2_000L

    fun fileName(startedAt: Long, phoneNumber: String?): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(startedAt))
        val number = phoneNumber?.filter { it.isDigit() || it == '+' }?.takeIf { it.isNotEmpty() } ?: "unknown"
        return "${timestamp}_$number.m4a"
    }

    fun isTooShort(durationMs: Long): Boolean = durationMs < MIN_DURATION_MS
}
