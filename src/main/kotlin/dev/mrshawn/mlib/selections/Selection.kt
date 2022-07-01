package dev.mrshawn.mlib.selections

import dev.mrshawn.mlib.chat.Chat
import dev.mrshawn.mlib.items.builders.ItemBuilder
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

class Selection(
	var cornerOne: Location? = null,
	var cornerTwo: Location? = null,
) {

	companion object: Listener {
		private var registered = false

		fun register(plugin: JavaPlugin) {
			if (!registered) {
				plugin.server.pluginManager.registerEvents(this, plugin)
				registered = true
			}
		}

		val SELECTION_WAND = ItemBuilder(Material.GOLDEN_AXE)
			.name("&bRegion Selection Wand")
			.addLoreLine("&7Left-click to select corner 1")
			.addLoreLine("&7Right-click to select corner 2")
			.build()

		private val selections = HashMap<UUID, Selection>()

		fun get(player: Player) = get(player.uniqueId)
		fun get(uuid: UUID): Selection {
			if (!selections.containsKey(uuid)) selections[uuid] = Selection()
			return selections[uuid]!!
		}

		@EventHandler
		fun onBlockClick(event: PlayerInteractEvent) {
			if (event.item == null) return
			if (event.item!! != SELECTION_WAND) return
			if (event.clickedBlock == null) return
			if (event.hand != EquipmentSlot.HAND) return

			val player = event.player
			if (event.action == Action.LEFT_CLICK_BLOCK) {
				Chat.tell(player, "&aCorner one set!")
				get(player).cornerOne = event.clickedBlock!!.location
				event.isCancelled = true
			}
			if (event.action == Action.RIGHT_CLICK_BLOCK) {
				Chat.tell(player, "&aCorner two set!")
				get(player).cornerTwo = event.clickedBlock!!.location
				event.isCancelled = true
			}
		}

	}

	fun bothSet() = cornerOne != null && cornerTwo != null

}