package dev.mrshawn.mlib.guis.items.impl

import dev.mrshawn.mlib.guis.items.GuiItem
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Consumer

open class BasicItem(
	item: ItemStack,
	private val onClick: Consumer<InventoryClickEvent> = DEFAULT_ON_CLICK
): GuiItem(item) {

	override fun getOnClick(): Consumer<InventoryClickEvent> {
		return onClick
	}

}