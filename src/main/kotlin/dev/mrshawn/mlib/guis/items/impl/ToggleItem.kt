package dev.mrshawn.mlib.guis.items.impl

import dev.mrshawn.mlib.guis.items.GuiItem
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Consumer

class ToggleItem(
	private val toggledOnItem: GuiItem,
	private val toggledOffItem: GuiItem
): GuiItem(toggledOnItem.getDisplayedItem()) {

	private var isToggledOn = true

	fun toggle() {
		isToggledOn = !isToggledOn
	}

	fun isToggled(): Boolean {
		return isToggledOn
	}

	override fun getOnClick(): Consumer<InventoryClickEvent> {
		return if (isToggledOn) {
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