package dev.mrshawn.mlib.guis.items.impl

import dev.mrshawn.mlib.guis.items.GuiItem
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Consumer

class CycleItem(
	private vararg val items: GuiItem,
	private val cycleOnClick: Boolean = true
): GuiItem(items[0].getDisplayedItem()) {

	private var currentItem = 0

	fun cycle() {
		currentItem++
		if (currentItem >= items.size) currentItem = 0
	}

	override fun getDisplayedItem(): ItemStack {
		return items[currentItem].getDisplayedItem()
	}

	override fun getOnClick(): Consumer<InventoryClickEvent> {
		val onClick = items[currentItem].getOnClick()
		if (cycleOnClick) cycle()
		return onClick
	}

}