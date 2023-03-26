package dev.mrshawn.mlib.regions

import org.bukkit.Location
import kotlin.math.max
import kotlin.math.min

open class Region(
	private var cornerOne: Location,
	private var cornerTwo: Location
) {

	protected var minX: Double = 0.0
	protected var minY: Double = 0.0
	protected var minZ: Double = 0.0
	protected var maxX: Double = 0.0
	protected var maxY: Double = 0.0
	protected var maxZ: Double = 0.0

	init { calculateBounds() }

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

}