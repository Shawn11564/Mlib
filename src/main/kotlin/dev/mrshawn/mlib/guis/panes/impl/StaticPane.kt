package dev.mrshawn.mlib.guis.panes.impl

import dev.mrshawn.mlib.guis.items.GuiItem
import dev.mrshawn.mlib.guis.panes.Pane
import org.bukkit.inventory.Inventory

class StaticPane(
	x: Int,
	y: Int,
	width: Int,
	height: Int,
	clearOld: Boolean = true,
	priority: Priority = Priority.NORMAL
): Pane(x, y, width, height, clearOld, priority) {

	private val items = HashMap<Int, GuiItem>()

	fun addItem(item: GuiItem, x: Int, y: Int) {
		items[x + y * 9] = item
	}

	fun removeItem(x: Int, y: Int) {
		items.remove(x + y * 9)
	}

	override fun fillWith(items: Collection<GuiItem>) {
		var i = 0
		for (y in this.y until height + 1) {
			for (x in this.x until width + 1) {
				if (i < items.size) {
					addItem(items.elementAt(i), x, y)
					i++
				}
			}
		}
	}

	override fun render(inventory: Inventory) {
		if (clearOld) {
			for (x in this.x until width + 1) {
				for (y in this.y until height + 1) {
					inventory.clear(x + y * 9)
				}
			}
		}
		for ((index, item) in items) {
			inventory.setItem(index, item.getDisplayedItem())
		}
	}

	override fun getItem(id: String): GuiItem? {
		return items.values.find { it.getId() == id }
	}

}