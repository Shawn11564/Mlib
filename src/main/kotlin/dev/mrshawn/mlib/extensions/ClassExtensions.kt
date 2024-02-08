package dev.mrshawn.mlib.extensions

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

fun Class<*>.isCommandSender(): Boolean {
	return when (this) {
		Player::class.java -> true
		CommandSender::class.java -> true
		else -> false
	}
}