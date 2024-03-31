package dev.mrshawn.mlib.extensions

import org.bukkit.Bukkit
import org.bukkit.event.Event

inline fun <reified T: Event> T.callEvent(): T {
	Bukkit.getPluginManager().callEvent(this)
	return this
}