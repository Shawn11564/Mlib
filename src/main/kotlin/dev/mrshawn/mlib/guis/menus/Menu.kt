package dev.mrshawn.mlib.guis.menus

import dev.mrshawn.mlib.guis.Gui
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player

abstract class Menu {

	protected abstract val gui: Gui

	fun show(player: Player) {
		gui.open(player)
	}

	fun show(player: HumanEntity) {
		gui.open(player)
	}

	fun close(player: Player) {
		gui.close(player)
	}

	fun close(player: HumanEntity) {
		gui.close(player)
	}

}