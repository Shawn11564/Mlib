package dev.mrshawn.mlib.tasks

import dev.mrshawn.mlib.tasks.schedules.MCalendar
import dev.mrshawn.mlib.tasks.schedules.Schedule
import dev.mrshawn.mlib.tasks.schedules.ScheduledTime

abstract class ScheduledMTask(
	calendar: MCalendar
): MTask() {

	class Builder {
		private var task: ScheduledMTask? = null
		private var runnable: () -> Unit = {}

		fun init(calendar: MCalendar): Builder {
			task = object: ScheduledMTask(calendar) {
				override fun run() {
					runnable()
				}
			}
			return this
		}

		fun withRunnable(runnable: () -> Unit): Builder {
			this.runnable = runnable
			return this
		}

		fun addToSchedule(day: Int, hour: Int, minute: Int): Builder {
			checkNotNull(task) { "init() must be called before addToSchedule()" }
			task?.getSchedule()?.addTime(ScheduledTime(day, hour, minute))
			return this
		}

		fun addToSchedule(day: MCalendar.Day, hour: Int, minute: Int): Builder {
			return addToSchedule(day.getCalendarDay(), hour, minute)
		}

		fun build(): ScheduledMTask {
			return task ?: throw IllegalStateException("init() must be called before build()")
		}
	}

	private val schedule = Schedule(calendar)

	fun getSchedule(): Schedule {
		return schedule
	}

}
