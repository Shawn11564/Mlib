package dev.mrshawn.mlib.utilities.players

import org.bukkit.Bukkit

object PlayerUtils {

	fun isPlayer(name: String) = Bukkit.getPlayer(name) != null

	fun getPlayer(name: String) = Bukkit.getPlayer(name)

}