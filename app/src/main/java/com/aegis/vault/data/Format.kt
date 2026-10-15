package com.aegis.vault.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Small formatting helpers shared by the UI. */
object Fmt {

    fun relativeTime(millis: Long, now: Long = System.currentTimeMillis()): String {
        val diff = (now - millis).coerceAtLeast(0L)
        val minute = 60_000L
        val hour = 60 * minute
        val day = 24 * hour
        val month = 30 * day
        val year = 365 * day
        return when {
            diff < minute -> "just now"
            diff < hour -> ago(diff / minute, "min")
            diff < day -> ago(diff / hour, "hour")
            diff < month -> ago(diff / day, "day")
            diff < year -> ago(diff / month, "month")
            else -> ago(diff / year, "year")
        }
    }

    private fun ago(count: Long, unit: String): String =
        if (count == 1L) "1 $unit ago" else "$count ${unit}s ago"

    fun date(millis: Long): String =
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(millis))

    fun formatBytes(bytes: Long): String = when {
        bytes < 1024L -> "$bytes B"
        bytes < 1024L * 1024L -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
        else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    }

    fun autoLockLabel(minutes: Int): String = when (minutes) {
        -1 -> "Never"
        1 -> "1 minute"
        2 -> "2 minutes"
        5 -> "5 minutes"
        15 -> "15 minutes"
        30 -> "30 minutes"
        60 -> "1 hour"
        else -> "$minutes minutes"
    }
}
