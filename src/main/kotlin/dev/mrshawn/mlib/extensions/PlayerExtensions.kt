package dev.mrshawn.mlib.extensions

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

fun Player.giveOrDropItems(vararg items: ItemStack): Boolean {
	var dropped = false
	items.forEach {
		val leftOver = inventory.addItem(it)
		if (leftOver.isNotEmpty()) {
			dropped = true
			leftOver.values.forEach { item ->
				world.dropItem(location, item)
			}
		}
	}
	return dropped
}