package dev.mrshawn.mlib.extensions

import org.bukkit.Bukkit
import java.util.*

fun UUID.getPlayerName(): String? {
	return Bukkit.getOfflinePlayer(this).name
}