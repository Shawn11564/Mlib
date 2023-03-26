package dev.mrshawn.mlib.selections

import dev.mrshawn.mlib.selections.wands.SelectionWand
import dev.mrshawn.mlib.selections.wands.impl.BasicSelectionWand
import dev.mrshawn.mlib.utilities.versions.Version
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

class Selection(
	var cornerOne: Location? = null,
	var cornerTwo: Location? = null,
) {

	companion object: Listener {
		private var registered = false
		private val wands = ArrayList<SelectionWand>()

		fun register(plugin: JavaPlugin) {
			if (!registered) {
				plugin.server.pluginManager.registerEvents(this, plugin)
				registered = true
			}
		}

		fun registerWand(wand: SelectionWand) {
			wands.add(wand)
		}

		val BASIC_SELECTION_WAND = BasicSelectionWand

		private val selections = HashMap<UUID, Selection>()

		fun get(player: Player) = get(player.uniqueId)
		fun get(uuid: UUID): Selection {
			if (!selections.containsKey(uuid)) selections[uuid] = Selection()
			return selections[uuid]!!
		}

		fun isWand(item: ItemStack) = wands.any { it.getWandItem().isSimilar(item) }

		fun getWand(item: ItemStack) = wands.firstOrNull { it.getWandItem().isSimilar(item) }

		@EventHandler
		fun onBlockClick(event: PlayerInteractEvent) {
			if (event.item == null) return
			if (Version.isAtLeast(Version.V1_9) && event.hand != EquipmentSlot.HAND) return
			val wand = getWand(event.item!!) ?: return
			if (event.clickedBlock == null) return

			wand.handleClick(event)
		}

	}

	fun bothSet() = cornerOne != null && cornerTwo != null

}