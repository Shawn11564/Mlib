package dev.mrshawn.mlib.guis.items.impl

import dev.mrshawn.mlib.items.ItemEditor
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Consumer

class GlowingItem(
	item: ItemStack,
	onClick: Consumer<InventoryClickEvent> = DEFAULT_ON_CLICK,
	private val glowIf: () -> Boolean = { true }
): BasicItem(item, onClick) {

	override fun getDisplayedItem(): ItemStack {
		return ItemEditor.glowIf(super.getDisplayedItem(), glowIf.invoke())
	}

}