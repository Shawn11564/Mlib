package dev.mrshawn.mlib.commands.preconditions

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class PlayerPrecondition: Precondition {
	override fun check(commandSender: CommandSender): Boolean {
		return commandSender is Player
	}

	override fun failMessage(): String {
		return "You must be a player to execute this command."
	}
}

class PermissionPrecondition(private val permission: String): Precondition {
	override fun check(commandSender: CommandSender): Boolean {
		return commandSender.hasPermission(permission)
	}
	override fun failMessage(): String {
		return "You do not have permission to execute this command."
	}
}