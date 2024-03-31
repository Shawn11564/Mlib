package dev.mrshawn.mlib.guis.items.impl

import dev.mrshawn.mlib.guis.items.GuiItem
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import java.util.function.Consumer

class UpdatingItem(
	private val itemBuilder: () -> ItemStack,
	private val onClick: Consumer<InventoryClickEvent> = DEFAULT_ON_CLICK
): GuiItem(itemBuilder(), updateOnClick = true) {

	override fun getDisplayedItem(): ItemStack {
		return addItemData(itemBuilder(), id) // Rebuild the ItemStack each time it's requested
	}

	override fun getOnClick(): Consumer<InventoryClickEvent> {
		return onClick
	}

}