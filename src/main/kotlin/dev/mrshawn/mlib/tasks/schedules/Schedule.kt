package dev.mrshawn.mlib.tasks.schedules

import org.bukkit.configuration.serialization.ConfigurationSerializable

class Schedule(
	private val calendar: MCalendar
): ConfigurationSerializable {

	companion object {
		fun deserialize(calendar: MCalendar, map: Map<String, Any>): Schedule {
			val schedule = Schedule(calendar)
			(map["times"] as List<Map<String, Any>>).forEach {
				schedule.addTime(ScheduledTime.deserialize(it))
			}
			return schedule
		}
	}

	private val times = ArrayList<ScheduledTime>()
	var onCooldown = false

	fun addTime(time: ScheduledTime) {
		times.add(time)
	}

	fun removeTime(time: ScheduledTime) {
		times.remove(time)
	}

	fun getTimes(): List<ScheduledTime> {
		return times
	}

	fun clear() {
		times.clear()
	}

	fun shouldRun(): Boolean {
		return !onCooldown && times.any { it.isCurrentTime(calendar.getCalendar()) }
	}

	override fun serialize(): MutableMap<String, Any> {
		return mutableMapOf(
			"times" to times.map { it.serialize() },
		)
	}

}