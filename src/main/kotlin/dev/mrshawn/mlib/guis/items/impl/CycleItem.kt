package dev.mrshawn.mlib.guis.items.impl

import dev.mrshawn.mlib.chat.Chat
import dev.mrshawn.mlib.guis.items.GuiItem
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import java.util.function.Consumer

class CycleItem(
	private vararg val items: GuiItem,
	private val cycleOnClick: Boolean = true
): GuiItem(items[0].getDisplayedItem()) {

	init {
		items.forEach { it.id = this.id } // Set the id of all items to the id of this item
	}

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