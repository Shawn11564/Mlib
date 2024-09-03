package dev.mrshawn.mlib.commands

import dev.mrshawn.mlib.commands.annotations.CommandAlias
import dev.mrshawn.mlib.commands.annotations.CommandExecutor
import dev.mrshawn.mlib.commands.preconditions.Precondition
import java.lang.reflect.Parameter

abstract class MCommand(
	private val preconditions: List<Precondition> = emptyList(),
	private var parentCommand: MCommand? = null
) {

	private val subcommands = ArrayList<MCommand>()
	private val aliases = javaClass.getAnnotation(CommandAlias::class.java).aliases.split("|").map { it.lowercase() }.toList()

	fun getExecuteMethod() = javaClass.methods.find { it.isAnnotationPresent(CommandExecutor::class.java) }

	fun getExecuteMethodParams(): Array<Parameter>? = getExecuteMethod()?.parameters

	fun getAliases(): List<String> {
		return aliases
	}

	fun addSubcommands(vararg subcommands: MCommand) {
		for (subcommand in subcommands) {
			this.subcommands.add(subcommand)
			subcommand.parentCommand = this
		}
	}

	fun getSubCommand(alias: String): MCommand? {
		return subcommands.find { it.getAliases().contains(alias.lowercase()) }
	}

	fun getSubCommands(): List<MCommand> {
		return subcommands
	}

	fun getPreconditions(): List<Precondition> {
		return preconditions
	}

	open fun getUsageMessage(): String {
		val usageMessage = StringBuilder().append("&cUsage: /")
		var currentCommand: MCommand? = this

		while (currentCommand != null) {
			usageMessage.insert(0, "${currentCommand.getAliases()[0]} ")
			currentCommand = currentCommand.parentCommand
		}

		getExecuteMethodParams()?.forEachIndexed { index, param ->
			if (!(index == 0 && MCommandManager.isCommandSender(param.type))) {
				usageMessage.append(" <${param.type.simpleName.lowercase()}>")
			}
		}

		return usageMessage.toString()
	}

}