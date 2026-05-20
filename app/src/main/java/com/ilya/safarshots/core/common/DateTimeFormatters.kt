package com.ilya.safarshots.core.common

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object DateTimeFormatters {
    private val photoFormat = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault())
    private val shortTimeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("LLLL yyyy", Locale.getDefault())

    fun photoDateTime(millis: Long): String = photoFormat.format(Date(millis))

    fun timeOnly(millis: Long): String = shortTimeFormat.format(Date(millis))

    fun galleryGroup(millis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val date = Calendar.getInstance().apply { timeInMillis = millis }
        val yesterday = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return when {
            isSameDay(now, date) -> "Today"
            isSameDay(yesterday, date) -> "Yesterday"
            abs(now.get(Calendar.DAY_OF_YEAR) - date.get(Calendar.DAY_OF_YEAR)) < 7 &&
                now.get(Calendar.YEAR) == date.get(Calendar.YEAR) -> "This week"
            else -> monthFormat.format(Date(millis)).replaceFirstChar { it.titlecase(Locale.getDefault()) }
        }
    }

    fun coordinates(latitude: Double, longitude: Double): String =
        String.format(Locale.US, "%.5f, %.5f", latitude, longitude)

    private fun isSameDay(a: Calendar, b: Calendar): Boolean =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
}
