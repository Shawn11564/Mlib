package dev.mrshawn.mlib.regions

import org.bukkit.Location
import org.bukkit.configuration.serialization.ConfigurationSerializable
import kotlin.math.max
import kotlin.math.min

open class Region(
	private var cornerOne: Location,
	private var cornerTwo: Location
): ConfigurationSerializable {

	var minX: Double = 0.0
	var minY: Double = 0.0
	var minZ: Double = 0.0
	var maxX: Double = 0.0
	var maxY: Double = 0.0
	var maxZ: Double = 0.0

	init { calculateBounds() }

	fun getCornerOne(): Location {
		return cornerOne
	}

	fun getCornerTwo(): Location {
		return cornerTwo
	}

	fun setCornerOne(cornerOne: Location) {
		this.cornerOne = cornerOne
		calculateBounds()
	}

	fun setCornerTwo(cornerTwo: Location) {
		this.cornerTwo = cornerTwo
		calculateBounds()
	}

	private fun calculateBounds() {
		minX = min(cornerOne.x, cornerTwo.x)
		minY = min(cornerOne.y, cornerTwo.y)
		minZ = min(cornerOne.z, cornerTwo.z)
		maxX = max(cornerOne.x, cornerTwo.x)
		maxY = max(cornerOne.y, cornerTwo.y)
		maxZ = max(cornerOne.z, cornerTwo.z)
	}

	open fun getCenter(): Location {
		return Location(
			cornerOne.world,
			(minX + maxX) / 2,
			(minY + maxY) / 2,
			(minZ + maxZ) / 2
		)
	}

	fun inSameWorld(location: Location): Boolean {
		return location.world == cornerOne.world
	}

	open fun contains(location: Location): Boolean {
		val loc = location.block.location
		return inSameWorld(location)
				&& loc.x in minX..maxX
				&& loc.y in minY..maxY
				&& loc.z in minZ..maxZ
	}

	open fun overlaps(other: Region): Boolean {
		// Check if there is overlap in the x dimension
		val overlapInX = minX <= other.maxX && maxX >= other.minX

		// Check if there is overlap in the y dimension
		val overlapInY = minY <= other.maxY && maxY >= other.minY

		// Check if there is overlap in the z dimension
		val overlapInZ = minZ <= other.maxZ && maxZ >= other.minZ

		// If there's overlap in both the x, y, z dimensions, the claims intersect
		return overlapInX && overlapInY && overlapInZ
	}

	override fun serialize(): Map<String, Any> {
		return mapOf(
			"cornerOne" to cornerOne.serialize(),
			"cornerTwo" to cornerTwo.serialize()
		)
	}

}