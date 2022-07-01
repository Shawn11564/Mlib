package dev.mrshawn.mlib.items

import dev.mrshawn.mlib.chat.Chat
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType

object ItemEditor {

	private fun getItemMeta(item: ItemStack): ItemMeta {
		return item.itemMeta ?: Bukkit.getItemFactory().getItemMeta(item.type)!!
	}

	fun rename(item: ItemStack, name: String): ItemStack {
		val meta = getItemMeta(item)
		meta.setDisplayName(Chat.colorize(name))
		item.itemMeta = meta
		return item
	}

	fun relore(item: ItemStack, lore: Array<String>): ItemStack {
		val meta = getItemMeta(item)
		meta.lore = lore.map { Chat.colorize(it) }.toMutableList()
		item.itemMeta = meta
		return item
	}

	fun addLoreLine(item: ItemStack, line: String): ItemStack {
		val meta = getItemMeta(item)
		val lore = meta.lore ?: ArrayList<String>()
		lore.add(Chat.colorize(line))
		meta.lore = lore
		item.itemMeta = meta
		return item
	}

	fun setLoreLine(item: ItemStack, line: String, index: Int): ItemStack {
		val meta = getItemMeta(item)
		val lore = meta.lore ?: ArrayList<String>()

		while (lore.size <= index) {
			lore.add("")
		}

		lore[index] = Chat.colorize(line)
		meta.lore = lore
		item.itemMeta = meta
		return item
	}

	fun removeLoreLine(item: ItemStack, index: Int): ItemStack {
		val meta = getItemMeta(item)
		val lore = meta.lore ?: ArrayList<String>()

		return if (lore.size <= index) {
			item
		} else {
			lore.removeAt(index)
			meta.lore = lore
			item.itemMeta = meta
			item
		}
	}

	fun amount(item: ItemStack, amount: Int): ItemStack {
		item.amount = amount
		return item
	}

	fun enchant(item: ItemStack, enchantment: Enchantment, level: Int): ItemStack {
		val meta = getItemMeta(item)
		meta.addEnchant(enchantment, level, true)
		item.itemMeta = meta
		return item
	}

	fun removeEnchant(item: ItemStack, enchantment: Enchantment): ItemStack {
		val meta = getItemMeta(item)
		meta.removeEnchant(enchantment)
		item.itemMeta = meta
		return item
	}

	fun glow(item: ItemStack): ItemStack {
		val meta = getItemMeta(item)
		meta.addEnchant(Enchantment.DURABILITY, 1, true)
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
		item.itemMeta = meta
		return item
	}

	fun glowIf(item: ItemStack, condition: Boolean): ItemStack {
		if (condition) {
			return glow(item)
		}
		return item
	}

	fun glowIf(item: ItemStack, condition: () -> Boolean): ItemStack {
		if (condition.invoke()) {
			return glow(item)
		}
		return item
	}

	fun unglow(item: ItemStack): ItemStack {
		val meta = getItemMeta(item)
		meta.removeEnchant(Enchantment.DURABILITY)
		meta.removeItemFlags(ItemFlag.HIDE_ENCHANTS)
		item.itemMeta = meta
		return item
	}

	fun setCustomModelData(item: ItemStack, data: Int): ItemStack {
		val meta = getItemMeta(item)
		meta.setCustomModelData(data)
		item.itemMeta = meta
		return item
	}

	fun getData(item: ItemStack, key: NamespacedKey): String? {
		return getItemMeta(item).persistentDataContainer
			.get(key, PersistentDataType.STRING)
	}

	fun getAllKeys(item: ItemStack) = item.itemMeta?.persistentDataContainer?.keys
}