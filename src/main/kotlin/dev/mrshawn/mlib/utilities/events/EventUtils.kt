package dev.mrshawn.mlib.utilities.events

import dev.mrshawn.mlib.chat.Chat
import dev.mrshawn.mlib.selections.Selection
import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
import org.bukkit.event.Event
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.plugin.java.JavaPlugin

object EventUtils {

	fun isDeadly(event: Event): Boolean {
		if (event !is EntityDamageEvent) return false
		if (event.entity !is LivingEntity) return false
		return (event.entity as LivingEntity).health - event.finalDamage <= 0
	}

	fun registerEvents(plugin: JavaPlugin, vararg listeners: Listener) {
		val pluginManager = Bukkit.getPluginManager()
		listeners.forEach { listener ->
			try {
				if (listener is Selection.Companion) {
					listener.register(plugin)
				} else {
					pluginManager.registerEvents(listener, plugin)
				}
			} catch (e: Exception) {
				Chat.log("&cFailed to register listener: ${listener.javaClass.simpleName}")
				e.printStackTrace()
			}
		}
	}

	fun registerEventIf(plugin: JavaPlugin, listener: Listener, condition: () -> Boolean) {
		if (condition()) {
			registerEvents(plugin, listener)
		}
	}

}