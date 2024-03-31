package dev.mrshawn.mlib.guis.items

import org.bukkit.NamespacedKey
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import java.util.function.Consumer

abstract class GuiItem(
	protected val item: ItemStack,
	val updateOnClick: Boolean = true,
	var id: String = UUID.randomUUID().toString()
) {

	companion object {
		@JvmStatic
		protected val DEFAULT_ON_CLICK = Consumer<InventoryClickEvent> { click -> click.isCancelled = true }
		val KEY_UUID = NamespacedKey(JavaPlugin.getProvidingPlugin(GuiItem::class.java), "MM-uuid")
	}

	open fun getDisplayedItem(): ItemStack {
		return addItemData(item, id)
	}

	abstract fun getOnClick(): Consumer<InventoryClickEvent>

	protected fun addItemData(item: ItemStack, data: String): ItemStack {
		val meta = item.itemMeta ?: throw NullPointerException("ItemMeta is null")
		val dataContainer = meta.persistentDataContainer
		dataContainer.set(KEY_UUID, PersistentDataType.STRING, data)
		item.itemMeta = meta
		return item
	}

}