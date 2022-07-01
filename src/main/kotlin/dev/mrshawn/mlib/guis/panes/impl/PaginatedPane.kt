package dev.mrshawn.mlib.guis.panes.impl

import dev.mrshawn.mlib.guis.items.GuiItem
import dev.mrshawn.mlib.guis.panes.Pane
import org.bukkit.inventory.Inventory
import kotlin.math.ceil
import kotlin.math.max

class PaginatedPane(
	x: Int,
	y: Int,
	width: Int,
	height: Int,
	clearOld: Boolean = true,
	priority: Priority = Priority.NORMAL,
): Pane(x, y, width, height, clearOld, priority) {

	private val pages = ArrayList<StaticPane>()
	fun pageCount() = pages.size

	var page = 0
		private set

	fun increment() {
		page++
		if (page >= pages.size) page = pages.size - 1
	}

	fun decrement() {
		page--
		if (page < 0) page = 0
	}

	private fun createPage() {
		pages.add(StaticPane(x, y, width, height, clearOld, priority))
	}

	fun addItem(item: GuiItem, x: Int, y: Int, page: Int = this.page) {
		pages[page].addItem(item, x, y)
	}

	fun removeItem(x: Int, y: Int, page: Int = this.page) {
		pages[page].removeItem(x, y)
	}

	override fun fillWith(items: Collection<GuiItem>) {
		if (items.isEmpty()) return

		val itemsPerPage = width * height
		val pagesNeeded = max(ceil(items.size / itemsPerPage.toDouble()), 1.0).toInt()

		val itemSets = ArrayList<ArrayList<GuiItem>>()
		var i = 0
		while (i < items.size) {
			val set = ArrayList<GuiItem>()
			for (j in 0 until itemsPerPage) {
				if (i >= items.size) break

				set.add(items.elementAt(i))
				i++
			}
			itemSets.add(set)
		}

		for (j in 0 until pagesNeeded) {
			createPage()
			pages[j].fillWith(itemSets[j])
		}
	}

	override fun render(inventory: Inventory) {
		if (preCheck()) return

		pages[page].render(inventory)
	}

	override fun getItem(id: String): GuiItem? {
		return pages[page].getItem(id)
	}

	private fun preCheck(): Boolean {
		return pages.isEmpty() || pages.size <= page
	}

}