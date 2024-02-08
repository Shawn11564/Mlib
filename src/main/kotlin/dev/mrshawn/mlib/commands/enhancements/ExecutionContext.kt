package dev.mrshawn.mlib.commands.enhancements

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class ExecutionContext(
	private val commandSender: CommandSender,
	private val args: Array<String>
) {

	fun getPlayer(): Player {
		return commandSender as Player
	}

	fun getName(): String {
		return getSender().name
	}

	fun getSender(): CommandSender {
		return commandSender
	}

	fun getArgs(): Array<String> {
		return args
	}

}