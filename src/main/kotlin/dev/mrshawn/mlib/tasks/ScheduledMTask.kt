package dev.mrshawn.mlib.tasks

import dev.mrshawn.mlib.tasks.schedules.MCalendar
import dev.mrshawn.mlib.tasks.schedules.Schedule

abstract class ScheduledMTask(
	calendar: MCalendar
): MTask() {

	private val schedule = Schedule(calendar)

	fun getSchedule(): Schedule {
		return schedule
	}

}