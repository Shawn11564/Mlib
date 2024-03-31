package dev.mrshawn.mlib.items.builders

import dev.mrshawn.mlib.chat.Chat
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

open class ItemBuilder(material: Material, amount: Int = 1) {

	protected var item = ItemStack(material, amount)
	protected var meta = item.itemMeta
	protected var lore = mutableListOf<String>()

	companion object {

		fun glow(item: ItemStack): ItemStack {
			val meta = item.itemMeta
			meta?.addItemFlags(ItemFlag.HIDE_ENCHANTS)
			meta?.addEnchant(Enchantment.DURABILITY, 1, true)
			item.itemMeta = meta
			return item
		}

		fun fromItemStack(originalItemStack: ItemStack): ItemBuilder {
			val item = originalItemStack.clone()
			val builder = ItemBuilder(item.type, item.amount)
			builder.item = item.clone()
			builder.meta = item.itemMeta
			builder.lore = item.itemMeta?.lore ?: mutableListOf()
			return builder
		}

	}

	fun name(name: String): ItemBuilder {
		meta?.setDisplayName(Chat.colorize(name))
		return this
	}

	fun setNoName(): ItemBuilder {
		name(" ")
		return this
	}

	fun addLoreLine(line: String): ItemBuilder {
		lore.add(Chat.colorize(line))
		return this
	}

	fun addLoreLines(vararg lines: String): ItemBuilder {
		return addLoreLines(lines.toList())
	}

	fun addLoreLines(lines: ArrayList<String>): ItemBuilder {
		return addLoreLines(lines.toList())
	}

	fun addLoreLines(lines: List<String>): ItemBuilder {
		lines.forEach { addLoreLine(it) }
		return this
	}

	fun addEnchantment(enchantment: Enchantment, level: Int): ItemBuilder {
		meta?.addEnchant(enchantment, level, true)
		return this
	}

	fun glow(): ItemBuilder {
		if (meta?.hasEnchants() == false) {
			hideAttributes()
			addEnchantment(Enchantment.DURABILITY, 1)
		}
		return this
	}

	fun glowIf(condition: () -> Boolean): ItemBuilder {
		if (condition.invoke()) glow()
		return this
	}

	fun addData(key: NamespacedKey, value: String): ItemBuilder {
		val container = meta?.persistentDataContainer
		container?.set(key, PersistentDataType.STRING, value)
		return this
	}

	fun addData(key: NamespacedKey, value: Int): ItemBuilder {
		val container = meta?.persistentDataContainer
		container?.set(key, PersistentDataType.INTEGER, value)
		return this
	}

	fun setCustomModelData(data: Int): ItemBuilder {
		meta?.setCustomModelData(data)
		return this
	}

	fun addItemFlag(flag: ItemFlag): ItemBuilder {
		meta?.addItemFlags(flag)
		return this
	}

	fun hideAttributes(): ItemBuilder = addItemFlag(ItemFlag.HIDE_ATTRIBUTES)

	open fun build(): ItemStack {
		meta?.lore = lore
		item.itemMeta = meta
		return item
	}

}