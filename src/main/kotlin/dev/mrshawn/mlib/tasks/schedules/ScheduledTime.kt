package dev.mrshawn.mlib.tasks.schedules

import org.bukkit.configuration.serialization.ConfigurationSerializable
import java.util.*

class ScheduledTime(
	private val day: Int,
	private val hour: Int,
	private val minute: Int
): ConfigurationSerializable {

	fun isCurrentTime(calendar: Calendar): Boolean {
		return (
				day == 8
						|| day == calendar.get(Calendar.DAY_OF_WEEK))
				&& hour == calendar.get(Calendar.HOUR_OF_DAY)
				&& minute == calendar.get(
			Calendar.MINUTE
		)
	}

	override fun serialize(): MutableMap<String, Any> {
		return mutableMapOf(
			"day" to day,
			"hour" to hour,
			"minute" to minute
		)
	}

	override fun toString(): String {
		return "${MCalendar.Day.getDay(day).getSimpleName()} at $hour:$minute"
	}

}