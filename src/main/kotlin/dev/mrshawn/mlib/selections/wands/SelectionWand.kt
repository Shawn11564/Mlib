package dev.mrshawn.mlib.selections.wands

import dev.mrshawn.mlib.chat.Chat
import dev.mrshawn.mlib.selections.Selection
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import java.util.function.Consumer

abstract class SelectionWand(
	private val wandItem: ItemStack,
	private val onClick: Consumer<PlayerInteractEvent>
		= Consumer<PlayerInteractEvent> {
		if (it.action == Action.LEFT_CLICK_BLOCK) {
			Chat.tell(it.player, "&aCorner one set!")
			Selection.get(it.player).cornerOne = it.clickedBlock!!.location
			it.isCancelled = true
		}
		if (it.action == Action.RIGHT_CLICK_BLOCK) {
			Chat.tell(it.player, "&aCorner two set!")
			Selection.get(it.player).cornerTwo = it.clickedBlock!!.location
			it.isCancelled = true
		}
	}
) {

	fun getWandItem(): ItemStack {
		return wandItem
	}

	fun handleClick(event: PlayerInteractEvent) {
		onClick.accept(event)
	}

	fun removeFromInventory(player: Player) {
		for (i in 0 until player.inventory.size) {
			if (player.inventory.getItem(i)?.isSimilar(wandItem) == true) {
				player.inventory.setItem(i, null)
			}
		}
	}

}