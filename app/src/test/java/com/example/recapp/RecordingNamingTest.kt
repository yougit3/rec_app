package com.example.recapp

import com.example.recapp.data.RecordingNaming
import com.example.recapp.recorder.AudioSourceOption
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingNamingTest {

    private val startedAt = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 11, 9, 5, 3) }.timeInMillis

    @Test
    fun fileNameContainsTimestampAndNormalizedNumber() {
        assertEquals("20261011_090503_+819012345678.m4a", RecordingNaming.fileName(startedAt, "+81 90-1234-5678"))
    }

    @Test
    fun fileNameUsesUnknownWhenNumberMissing() {
        assertEquals("20261011_090503_unknown.m4a", RecordingNaming.fileName(startedAt, null))
        assertEquals("20261011_090503_unknown.m4a", RecordingNaming.fileName(startedAt, "非通知"))
    }

    @Test
    fun shortRecordingThreshold() {
        assertTrue(RecordingNaming.isTooShort(1_999))
        assertFalse(RecordingNaming.isTooShort(2_000))
    }

    @Test
    fun fallbackOrderStartsWithPreferredAndCoversAll() {
        val order = AudioSourceOption.fallbackOrder(AudioSourceOption.MIC)
        assertEquals(AudioSourceOption.MIC, order.first())
        assertEquals(AudioSourceOption.entries.toSet(), order.toSet())
        assertEquals(AudioSourceOption.entries.size, order.size)
    }
}
