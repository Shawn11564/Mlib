package dev.mrshawn.mlib.guis.items.impl

import dev.mrshawn.mlib.guis.items.GuiItem
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import java.util.function.Consumer
import java.util.*

class ToggleItem(
	private val toggledOnItem: GuiItem,
	private val toggledOffItem: GuiItem,
	initiallyToggledOn: Boolean = true,
	private val toggleOnClick: Boolean = true
): GuiItem(toggledOnItem.getDisplayedItem()) {

	init {
		val itemID = UUID.randomUUID().toString()
		id = itemID
		toggledOnItem.id = itemID
		toggledOffItem.id = itemID
	}

	private var isToggledOn = initiallyToggledOn

	fun toggle() {
		isToggledOn = !isToggledOn
	}

	fun isToggled(): Boolean {
		return isToggledOn
	}

	override fun getOnClick(): Consumer<InventoryClickEvent> {
		val state = isToggledOn
		if (toggleOnClick) toggle()
		return if (state) {
			toggledOnItem.getOnClick()
		} else {
			toggledOffItem.getOnClick()
		}
	}

	override fun getDisplayedItem(): ItemStack {
		return if (isToggledOn) {
			toggledOnItem.getDisplayedItem()
		} else {
			toggledOffItem.getDisplayedItem()
		}
	}

}