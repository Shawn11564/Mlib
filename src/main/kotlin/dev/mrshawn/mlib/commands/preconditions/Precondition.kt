package dev.mrshawn.mlib.commands.preconditions

import org.bukkit.command.CommandSender

interface Precondition {

	class Builder {
		private val preconditions = mutableListOf<Precondition>()

		fun hasPermission(permission: String): Builder {
			preconditions.add(PermissionPrecondition(permission))
			return this
		}

		fun isPlayer(): Builder {
			preconditions.add(PlayerPrecondition())
			return this
		}

		fun addPrecondition(precondition: Precondition): Builder {
			preconditions.add(precondition)
			return this
		}

		fun build(): List<Precondition> {
			return preconditions
		}
	}

	fun check(commandSender: CommandSender): Boolean

	fun failMessage(): String

}