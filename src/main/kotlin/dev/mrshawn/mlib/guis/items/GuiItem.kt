package dev.mrshawn.mlib.guis.items

import org.bukkit.NamespacedKey
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.Consumer
import java.util.*

abstract class GuiItem(
	protected val item: ItemStack,
	val updateOnClick: Boolean = true
) {

	companion object {
		@JvmStatic
		protected val DEFAULT_ON_CLICK = Consumer<InventoryClickEvent> { click -> click.isCancelled = true }
		val KEY_UUID = NamespacedKey(JavaPlugin.getProvidingPlugin(GuiItem::class.java), "MM-uuid")
	}

	private val id = UUID.randomUUID().toString()

	fun getId() = id

	open fun getDisplayedItem(): ItemStack {
		return addItemData(item, id)
	}

	abstract fun getOnClick(): Consumer<InventoryClickEvent>

	private fun addItemData(item: ItemStack, data: String): ItemStack {
		val meta = item.itemMeta ?: throw NullPointerException("ItemMeta is null")
		val dataContainer = meta.persistentDataContainer
		dataContainer.set(KEY_UUID, PersistentDataType.STRING, data)
		item.itemMeta = meta
		return item
	}

}