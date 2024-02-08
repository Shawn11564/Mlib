package dev.mrshawn.mlib.commands.preconditions

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class PlayerPrecondition: Precondition {
	override fun check(commandSender: CommandSender): Boolean {
		return commandSender is Player
	}
}

class PermissionPrecondition(private val permission: String): Precondition {
	override fun check(commandSender: CommandSender): Boolean {
		return commandSender.hasPermission(permission)
	}
}