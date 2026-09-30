package com.vanu.videocut.util

import java.util.Locale
import kotlin.math.roundToLong

object TimeFormat {
    fun format(ms: Long, showHours: Boolean = false): String {
        val total = ms.coerceAtLeast(0)
        val hours = total / 3_600_000
        val minutes = (total % 3_600_000) / 60_000
        val seconds = (total % 60_000) / 1000
        val millis = (total % 1000)
        return if (showHours || hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d.%03d", hours, minutes, seconds, millis)
        } else {
            String.format(Locale.US, "%02d:%02d.%03d", minutes, seconds, millis)
        }
    }

    fun formatShort(ms: Long): String {
        val total = ms.coerceAtLeast(0)
        val hours = total / 3_600_000
        val minutes = (total % 3_600_000) / 60_000
        val seconds = (total % 60_000) / 1000
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    /**
     * Parses mm:ss, mm:ss.ms, hh:mm:ss, hh:mm:ss.ms
     */
    fun parse(input: String): Long? {
        val cleaned = input.trim().replace(',', '.')
        if (cleaned.isEmpty()) return null
        return try {
            val parts = cleaned.split(':')
            when (parts.size) {
                1 -> {
                    val sec = parts[0].toDouble()
                    (sec * 1000).roundToLong()
                }
                2 -> {
                    val minutes = parts[0].toLong()
                    val sec = parts[1].toDouble()
                    minutes * 60_000 + (sec * 1000).roundToLong()
                }
                3 -> {
                    val hours = parts[0].toLong()
                    val minutes = parts[1].toLong()
                    val sec = parts[2].toDouble()
                    hours * 3_600_000 + minutes * 60_000 + (sec * 1000).roundToLong()
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }
}
