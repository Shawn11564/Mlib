package dev.mrshawn.mlib.extensions

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

fun ItemStack.giveOrDrop(player: Player) {
	val leftover = player.inventory.addItem(this)
	leftover.entries.forEach { (_, item) ->
		player.world.dropItem(player.location, item)
	}
}

fun ItemStack.giveOrDrop(player: Player, amount: Int) {
	val clone = this.clone()
	clone.amount = amount
	clone.giveOrDrop(player)
}

fun ItemStack.isFish(): Boolean {
	return when (type) {
		Material.COD, Material.SALMON, Material.TROPICAL_FISH, Material.PUFFERFISH -> true
		else -> false
	}
}