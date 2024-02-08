package dev.mrshawn.mlib.guis

import dev.mrshawn.mlib.chat.Chat
import dev.mrshawn.mlib.guis.items.GuiItem
import dev.mrshawn.mlib.guis.items.impl.CycleItem
import dev.mrshawn.mlib.guis.panes.Pane
import dev.mrshawn.mlib.items.ItemUtils
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.util.function.Consumer
import java.util.*

abstract class Gui(
	private val plugin: JavaPlugin,
	private var previousGui: Gui? = null,
	private var nextGui: Gui? = null,
	val uuid: UUID = UUID.randomUUID(),
	private val destroyOnClose: Boolean = true
): Listener {

	protected abstract val inventory: Inventory
	private val items = HashMap<Int, GuiItem>()
	private val panes = TreeSet(Pane.PaneComparator())
	private var onItemPlace: Consumer<InventoryClickEvent> = Consumer { it.isCancelled = true }

	init { create() }

	fun addItem(item: GuiItem, x: Int, y: Int, update: Boolean = false) {
		removeItem(x, y)
		val index = x + y * 9
		items[index] = item
		if (update)	inventory.setItem(index, item.getDisplayedItem())
	}

	fun fillWith(item: GuiItem, update: Boolean = false) {
		for (i in 0 until inventory.size) {
			addItem(item, i % 9, i / 9, update)
		}
	}

	fun fillArea(item: GuiItem, x: Int, y: Int, width: Int, height: Int, update: Boolean = false) {
		for (i in 0 until width) {
			for (j in 0 until height) {
				addItem(item, x + i, y + j, update)
			}
		}
	}

	fun removeItem(x: Int, y: Int) {
		inventory.clear(x + y * 9)
		items.remove(x + y * 9)
	}

	fun getInventoryItem(x: Int, y: Int): ItemStack? {
		return inventory.getItem(x + y * 9)
	}

	fun addPane(pane: Pane) {
		panes.add(pane)
	}

	fun removePane(pane: Pane) {
		panes.remove(pane)
	}

	fun clear() {
		items.clear()
		panes.clear()
		update()
	}

	fun update(clear: Boolean = true) {
		if (clear) inventory.clear()

		for ((index, item) in items) {
			inventory.setItem(index, item.getDisplayedItem())
		}

		/* Pane is sorted with highest-priority panes first
		 * Reverse the order to get lowest-priority panes first
		 * This way we can overwrite lower-priority panes with higher-priority ones
		 */
		for (pane in panes.reversed()) {
			pane.render(inventory)
		}
	}

	@EventHandler
	fun onClickEvent(event: InventoryClickEvent) {
		if (event.clickedInventory == null) return
		if (event.click == ClickType.DROP && event.clickedInventory == this.inventory) {
			onItemPlace.accept(event)
		}
		if (event.currentItem == null || event.currentItem?.type == Material.AIR) return
		val item = event.currentItem ?: return
		val id = ItemUtils.getItemID(item) ?: return

		val menuItem = getGuiItem(id) ?: return

		menuItem.getOnClick().accept(event)
		if (menuItem.updateOnClick) {
			inventory.setItem(event.rawSlot, menuItem.getDisplayedItem())
		}
	}

	@EventHandler
	fun onInventoryClose(event: InventoryCloseEvent) {
		if (event.inventory == inventory) {
			onClose(event.player as Player)

			if (destroyOnClose && canBeDestroyed()) destroy()
		}
	}

	private fun getGuiItem(id: String): GuiItem? {
		panes.find { it.getItem(id) != null }?.let { return it.getItem(id) }
		return items.values.find { it.id == id }
	}

	private fun create() {
		register()
		onCreate()
	}

	protected open fun onCreate() {}

	var doNotDestroy = false
	fun canBeDestroyed(): Boolean {
		return !doNotDestroy
				&& inventory.viewers.isEmpty()
				&& previousGui?.canBeDestroyed() ?: true
				&& nextGui?.canBeDestroyed() ?: true
	}

	private fun destroy() {
		onDestroy()
		unregister()
	}

	protected open fun onDestroy() {}

	protected open fun onClose(player: Player) {}

	private fun register() {
		Bukkit.getPluginManager().registerEvents(this, plugin)
	}

	private fun unregister() {
		HandlerList.unregisterAll(this)
	}

	fun next(player: Player, update: Boolean = true) {
		nextGui?.open(player)
		if (update) nextGui?.update()
	}

	fun next(player: HumanEntity, update: Boolean = true) {
		nextGui?.open(player)
		if (update) nextGui?.update()
	}

	fun back(player: Player, update: Boolean = true) {
		previousGui?.open(player)
		if (update) previousGui?.update()
	}

	fun back(player: HumanEntity, update: Boolean = true) {
		previousGui?.open(player)
		if (update) previousGui?.update()
	}

	fun open(player: Player) {
		open(player as HumanEntity)
	}

	fun open(player: HumanEntity) {
		player.openInventory(inventory)
	}

	fun close(player: Player) {
		close(player as HumanEntity)
	}

	fun close(player: HumanEntity) {
		player.closeInventory()
	}

	fun closeAll() {
		Bukkit.getOnlinePlayers()
			.filter { it.openInventory.topInventory == inventory }
			.forEach { it.closeInventory() }
	}

}