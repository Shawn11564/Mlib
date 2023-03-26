package dev.mrshawn.mlib.tasks

import dev.mrshawn.mlib.tasks.schedules.MCalendar
import dev.mrshawn.mlib.tasks.schedules.Schedule
import kotlin.random.Random

abstract class MTask(
	calendar: MCalendar
) {

	val taskID = Random.nextInt(0, 1000000)
	private val schedule = Schedule(calendar)

	abstract fun run()

	fun getSchedule(): Schedule {
		return schedule
	}

}