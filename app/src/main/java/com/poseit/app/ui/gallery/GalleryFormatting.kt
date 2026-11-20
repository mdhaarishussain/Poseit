package com.poseit.app.ui.gallery

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.text.format.DateFormat
import com.poseit.app.data.SavedPhoto
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** A run of photos taken on the same calendar day. */
internal data class PhotoDay(val key: Long, val label: String, val photos: List<SavedPhoto>)

/** Groups [photos] (newest first) by local calendar day, keeping the order. */
internal fun groupByDay(photos: List<SavedPhoto>, now: Long = System.currentTimeMillis()): List<PhotoDay> {
    if (photos.isEmpty()) return emptyList()
    val cal = Calendar.getInstance()
    val today = startOfDay(cal, now)
    val yesterday = today - 24L * 60 * 60 * 1000
    val thisYear = cal.apply { timeInMillis = now }.get(Calendar.YEAR)
    val locale = Locale.getDefault()
    val sameYearFormat = SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, "EEEdMMMM"), locale)
    val otherYearFormat = SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, "dMMMMyyyy"), locale)

    val days = LinkedHashMap<Long, MutableList<SavedPhoto>>()
    for (photo in photos) days.getOrPut(startOfDay(cal, photo.dateTakenMillis)) { mutableListOf() } += photo
    return days.map { (day, list) ->
        val label = when {
            day == today -> "Today"
            day == yesterday -> "Yesterday"
            day <= 0L -> "Undated"
            cal.apply { timeInMillis = day }.get(Calendar.YEAR) == thisYear -> sameYearFormat.format(Date(day))
            else -> otherYearFormat.format(Date(day))
        }
        PhotoDay(day, label, list)
    }
}

private fun startOfDay(cal: Calendar, millis: Long): Long {
    if (millis <= 0L) return 0L
    cal.timeInMillis = millis
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

/** Compact date + time ("20 Nov 2026, 21:30") in the user's locale and 12/24 h preference. */
internal fun formatPhotoDateTime(millis: Long, context: Context? = null): String {
    if (millis <= 0L) return ""
    val locale = Locale.getDefault()
    val use24h = context?.let { DateFormat.is24HourFormat(it) } ?: true
    val pattern = DateFormat.getBestDateTimePattern(locale, if (use24h) "yMMMdHHmm" else "yMMMdhmma")
    return SimpleDateFormat(pattern, locale).format(Date(millis))
}

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
