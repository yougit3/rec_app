package com.example.recapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CallDirection { INCOMING, OUTGOING, UNKNOWN }

@Entity(tableName = "recordings")
data class Recording(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val filePath: String,
    val phoneNumber: String?,
    val contactName: String?,
    val direction: CallDirection,
    val startedAt: Long,
    val durationMs: Long,
)
