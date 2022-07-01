package dev.mrshawn.mlib.guis.items.impl

import dev.mrshawn.mlib.guis.items.GuiItem
import dev.mrshawn.mlib.items.ItemEditor
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Consumer

class TwoStageItem(
	item: ItemStack,
	private val onSecondClick: Consumer<InventoryClickEvent>,
	private val glowOnFirstClick: Boolean = true
): GuiItem(item) {

	private var isSecondStage = false

	override fun getDisplayedItem(): ItemStack {
		return if (isSecondStage && glowOnFirstClick) {
			ItemEditor.glow(super.getDisplayedItem())
		} else {
			super.getDisplayedItem()
		}
	}

	override fun getOnClick(): Consumer<InventoryClickEvent> {
		return if (isSecondStage) {
			onSecondClick
		} else {
			Consumer<InventoryClickEvent> {
				it.isCancelled = true
				isSecondStage = true
			}
		}
	}

}