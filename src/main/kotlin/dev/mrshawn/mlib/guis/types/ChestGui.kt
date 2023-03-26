package dev.mrshawn.mlib.guis.types

import dev.mrshawn.mlib.chat.Chat
import dev.mrshawn.mlib.guis.Gui
import org.bukkit.Bukkit
import org.bukkit.inventory.Inventory
import org.bukkit.plugin.java.JavaPlugin

class ChestGui(
	plugin: JavaPlugin,
	name: String,
	rows: Int = 6,
	previousGui: Gui? = null,
	nextGui: Gui? = null,
): Gui(plugin, previousGui = previousGui, nextGui = nextGui) {

	override val inventory: Inventory = Bukkit.createInventory(null, rows * 9, Chat.colorize(name))

}