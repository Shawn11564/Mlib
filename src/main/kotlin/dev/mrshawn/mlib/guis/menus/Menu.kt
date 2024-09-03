package dev.mrshawn.mlib.guis.menus

import dev.mrshawn.mlib.guis.Gui
import dev.mrshawn.mlib.guis.items.impl.BasicItem
import dev.mrshawn.mlib.guis.types.ChestGui
import dev.mrshawn.mlib.items.builders.ItemBuilder
import org.bukkit.Material
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player

abstract class Menu(
	protected val previousMenu: Menu? = null,
	protected val nextMenu: Menu? = null
) {

	companion object {
		fun rebuild(menuClass: Class<out Menu>) {
			val instances = getInstances(menuClass)
			instances.forEach { menuInstance ->
				val viewers = menuInstance.gui.getViewers()
				viewers.forEach { _ ->
					menuInstance.gui.clear()
					menuInstance.createGui()
					menuInstance.gui.update()
				}
			}
		}

		private fun getInstances(menuClass: Class<out Menu>): List<Menu> {
			val instances = mutableListOf<Menu>()
			val fields = menuClass.declaredFields
			for (field in fields) {
				if (Menu::class.java.isAssignableFrom(field.type)) {
					field.isAccessible = true
					val instance = field.get(null) as? Menu
					if (instance != null) {
						instances.add(instance)
					}
				}
			}
			return instances
		}
	}

	protected abstract val gui: Gui

	protected abstract fun createGui()

	fun back(player: Player, update: Boolean = true) {
		back(player as HumanEntity, update)
	}

	fun back(player: HumanEntity, update: Boolean = true) {
		previousMenu?.show(player)
		if (update) previousMenu?.gui?.update()
	}

	fun next(player: Player, update: Boolean = true) {
		next(player as HumanEntity, update)
	}

	fun next(player: HumanEntity, update: Boolean = true) {
		nextMenu?.show(player)
		if (update) nextMenu?.gui?.update()
	}

	fun show(player: Player) {
		show(player as HumanEntity)
	}

	fun show(player: HumanEntity, reCreateOnShow: Boolean = true, clearOnShow: Boolean = true) {
		if (clearOnShow) gui.clear()
		if (reCreateOnShow) createGui()
		createGui()
		setNextAndBackButtons()
		gui.open(player)
	}

	fun close(player: Player, doNotDestroy: Boolean = false) {
		close(player as HumanEntity, doNotDestroy)
	}

	fun close(player: HumanEntity, doNotDestroy: Boolean = false) {
		gui.doNotDestroy = doNotDestroy
		gui.close(player)
	}

	private fun setBackButton() {
		if (gui is ChestGui && previousMenu != null) {
			gui.addItem(BasicItem(
				ItemBuilder(Material.RED_WOOL)
					.name("&cPrevious Menu")
					.build(),
				onClick = {
					it.isCancelled = true
					back(it.whoClicked)
				}
			), 0, 0)
			gui.update()
		}
	}

	private fun setNextButton() {
		if (gui is ChestGui && nextMenu != null) {
			gui.addItem(BasicItem(
				ItemBuilder(Material.LIME_WOOL)
					.name("&aNext Menu")
					.build(),
				onClick = {
					it.isCancelled = true
					next(it.whoClicked)
				}
			), 8, 0)
			gui.update()
		}
	}

	private fun setNextAndBackButtons() {
		setBackButton()
		setNextButton()
	}

}