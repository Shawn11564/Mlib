package dev.mrshawn.mlib.utilities.numbers

import java.util.*


object NumberUtils {

	private val suffixes = arrayOf("th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th")
	private val numeralMap = TreeMap(mapOf(
		Pair(1000, "M"),
		Pair(900, "CM"),
		Pair(500, "D"),
		Pair(400, "CD"),
		Pair(100, "C"),
		Pair(90, "XC"),
		Pair(50, "L"),
		Pair(40, "XL"),
		Pair(10, "X"),
		Pair(9, "IX"),
		Pair(5, "V"),
		Pair(4, "IV"),
		Pair(1, "I")
	))

	fun toOrdinal(i: Int): String {
		return when (i % 100) {
			11, 12, 13 -> i.toString() + "th"
			else -> i.toString() + suffixes[i % 10]
		}
	}

	fun toRomanNumeral(number: Int): String? {
		val l = numeralMap.floorKey(number)
		return if (number == l) {
			numeralMap[number]
		} else numeralMap[l] + toRomanNumeral(number - l)
	}

	fun linearConvert(currentValue: Int, originalMin: Int, originalMax: Int, newMin: Int, newMax: Int): Int {
		return (currentValue - originalMin) * (newMax - newMin) / (originalMax - originalMin) + newMin
	}

	fun linearConvert(currentValue: Int, originalMin: Int, originalMax: Int): Int {
		return linearConvert(currentValue, originalMin, originalMax, 0, 100)
	}

	fun toTime(originalMillis: Long): Time {
		val millis: Long = originalMillis % 1000
		val second: Long = originalMillis / 1000 % 60
		val minute: Long = originalMillis / (1000 * 60) % 60
		val hour: Long = originalMillis / (1000 * 60 * 60) % 24

		return Time(hour, minute, second, millis)
	}

	data class Time(val hour: Long, val minute: Long, val second: Long, val millis: Long) {
		override fun toString(): String {
			return if (millis == 0L) {
				String.format("%02d:%02d:%02d", hour, minute, second)
			} else {
				String.format("%02d:%02d:%02d.%02d", hour, minute, second, millis)
			}

		}
	}

}
