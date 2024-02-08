package dev.mrshawn.mlib.commands.preconditions

import org.bukkit.command.CommandSender

interface Precondition {

	fun check(commandSender: CommandSender): Boolean

}