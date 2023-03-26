package dev.mrshawn.mlib.tasks.schedules

import dev.mrshawn.mlib.tasks.MTask
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitRunnable
import java.util.*

abstract class MCalendar(
	private val plugin: Plugin,
	private val timeZone: String,
	private val checkInterval: Int
) {

	private val tasks = HashMap<Int, MTask>()

	fun addTask(task: MTask) {
		tasks[task.taskID] = task
	}

	fun removeTask(taskID: Int) {
		tasks.remove(taskID)
	}

	fun startScheduler() {
		object: BukkitRunnable() {
			override fun run() {
				tasks.values
					.filter { it.getSchedule().shouldRun() }
					.forEach { it.run() }
			}
		}.runTaskTimer(plugin, checkInterval * 20L, checkInterval * 20L)
	}

	fun getCalendar(): Calendar {
		return Calendar.getInstance(TimeZone.getTimeZone(timeZone))
	}

	enum class Day(private val simpleName: String, private val calendarDay: Int) {

		SUNDAY("Sunday", Calendar.SUNDAY),
		MONDAY("Monday", Calendar.MONDAY),
		TUESDAY("Tuesday", Calendar.TUESDAY),
		WEDNESDAY("Wednesday", Calendar.WEDNESDAY),
		THURSDAY("Thursday", Calendar.THURSDAY),
		FRIDAY("Friday", Calendar.FRIDAY),
		SATURDAY("Saturday", Calendar.SATURDAY),
		DAILY("Daily", 8);

		fun getSimpleName(): String {
			return simpleName
		}

		fun getCalendarDay(): Int {
			return calendarDay
		}

		companion object {
			fun getDay(calendarDay: Int): Day {
				return values().first { it.calendarDay == calendarDay }
			}

			fun getDay(simpleName: String): Day {
				return values().first { it.simpleName == simpleName }
			}

			fun isDay(simpleName: String): Boolean {
				return values().any { it.simpleName == simpleName }
			}

			fun isDay(calendarDay: Int): Boolean {
				return values().any { it.calendarDay == calendarDay }
			}
		}

	}

}