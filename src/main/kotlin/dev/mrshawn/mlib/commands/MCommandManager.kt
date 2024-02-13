package dev.mrshawn.mlib.commands

import dev.mrshawn.mlib.chat.Chat
import dev.mrshawn.mlib.commands.annotations.CommandCompletion
import dev.mrshawn.mlib.commands.annotations.Optional
import dev.mrshawn.mlib.commands.enhancements.ExecutionContext
import dev.mrshawn.mlib.commands.exceptions.ContextResolverFailedException
import dev.mrshawn.mlib.extensions.isCommandSender
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.SimpleCommandMap
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import java.lang.reflect.Field

class MCommandManager: TabExecutor {

	private val commands = HashMap<List<String>, MCommand>()
	private val commandCompletions: HashMap<String, (CommandSender) -> Collection<String>> = HashMap()
	private val commandContexts: HashMap<Class<*>, (CommandSender, Array<String>) -> Any> = HashMap()

	init {
		// default completions
		registerCompletion("@nothing") { listOf(" ") }
		registerCompletion("@players") { Bukkit.getOnlinePlayers().map { it.name }.ifEmpty { listOf(" ") } }
		registerCompletion("@boolean") { listOf("true", "false") }

		// default contexts
		registerContext(ExecutionContext::class.java) { sender, args -> ExecutionContext(sender, args) }
		registerContext(Player::class.java) { _, args ->
			Bukkit.getPlayer(args[0]) ?: throw ContextResolverFailedException("Player not found: ${args[0]}!")
		}
		registerContext(String::class.java) { _, args -> args[0] }
		registerContext(Array<String>::class.java) { _, args -> args }
		registerContext(Int::class.java) { _, args -> args[0].toIntOrNull() ?: throw ContextResolverFailedException("The value you entered is not a number!") }
		registerContext(Double::class.java) { _, args -> args[0].toDoubleOrNull() ?: throw ContextResolverFailedException("The value you entered is not a number!") }
		registerContext(Boolean::class.java) { _, args -> args[0].lowercase().toBooleanStrictOrNull() ?: throw ContextResolverFailedException("The value you entered is not a boolean!") }
	}

	fun registerCommand(command: MCommand) {
		if (command.getAliases().isEmpty()) {
			Chat.error("Command ${command.javaClass.simpleName} does not have a CommandAliases annotation or has no aliases specified!")
			return
		}

		commands[command.getAliases()] = command
		registerToCommandMap(command)
	}

	private fun registerToCommandMap(command: MCommand) {
		try {
			val bukkitCommandMap: Field = Bukkit.getServer().javaClass.getDeclaredField("commandMap")
			bukkitCommandMap.isAccessible = true
			val commandMap = bukkitCommandMap.get(Bukkit.getServer()) as SimpleCommandMap

			command.getAliases().forEach { alias ->
				val bukkitCommand = object : Command(alias) {
					override fun execute(sender: CommandSender, commandLabel: String, args: Array<String>): Boolean {
						return onCommand(sender, this, commandLabel, args)
					}

					override fun tabComplete(sender: CommandSender, alias: String, args: Array<String>): MutableList<String> {
						return onTabComplete(sender, this, alias, args)
					}
				}
				commandMap.register(alias, bukkitCommand)
			}
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	fun registerCompletion(id: String, completion: (CommandSender) -> Collection<String>) {
		commandCompletions[id] = completion
	}

	fun <T: Any> registerContext(clazz: Class<T>, resolver: (CommandSender, Array<String>) -> T) {
		commandContexts[clazz] = resolver
	}

	private fun getCommand(cmdString: String): MCommand? {
		val lowercase = cmdString.lowercase()
		return commands.values.stream()
			.filter { cmd: MCommand -> cmd.getAliases().contains(lowercase) }
			.findFirst()
			.orElse(null)
	}

	private fun parseCompletion(sender: CommandSender, completionID: String): Collection<String> {
		val completion = commandCompletions[completionID]
		return completion?.invoke(sender) ?: listOf()
	}

	private fun <T> parseContext(objType: Class<T>, sender: CommandSender, args: Array<String>, allowNull: Boolean = false): Any? {
		if (args.isEmpty() && allowNull) {
			return null
		} else if (args.isEmpty()) {
			throw ContextResolverFailedException("Expected argument after command but none was found.")
		}

		val context = if (objType.name.equals("java.lang.Boolean")) {
			commandContexts[Boolean::class.java]?.invoke(sender, args)
		} else {
			commandContexts[objType]?.invoke(sender, args)
		}

		if (context == null && allowNull) {
			return null
		} else if (context == null) {
			throw ContextResolverFailedException("Failed to resolve context for type: ${objType.simpleName} with arguments: ${args.joinToString()}")
		}

		return context
	}

	override fun onCommand(sender: CommandSender, cmd: Command, label: String, args: Array<String>): Boolean {
		var currentCommand: MCommand? = getCommand(label)
		var i = 0
		while (i < args.size && currentCommand != null) {
			val subCommand = currentCommand.getSubCommand(args[i])
			if (subCommand != null) {
				currentCommand = subCommand
				i++ // Move past the subcommand to the arguments for the command method
			} else {
				break // No more subcommands, remaining args are for the command method
			}
		}


		// Parse method parameters and handle CommandSender or Player specifically
		val executeMethodParams = currentCommand?.getExecuteMethodParams()
		val parsedContext: MutableList<Any?> = mutableListOf()

		executeMethodParams?.forEachIndexed { index, param ->
			when {
				index == 0 && (param.type.isCommandSender()) -> {
					// For the first parameter, if it's CommandSender or Player, use the sender directly
					when (param.type) {
						CommandSender::class.java -> parsedContext.add(sender)
						Player::class.java -> parsedContext.add(sender as? Player)
					}
				}
				else -> {
					if (index < args.size) {
						// Argument provided, parse based on expected type
						val context = parseContext(param.type, sender, args.copyOfRange(index, args.size), false)
						parsedContext.add(context)
					} else if (param.isAnnotationPresent(Optional::class.java)) {
						// Parameter is optional and not provided, add null
						parsedContext.add(null)
					} else {
						// Handle mandatory parameters not provided (e.g., by showing usage message)
						Chat.tell(sender, "&cInvalid command syntax.")
						return false
					}
				}
			}
		}

		// Execute the command method with parsed parameters
		if (parsedContext.isNotEmpty()) {
			currentCommand?.getExecuteMethod()?.invoke(currentCommand, *parsedContext.toTypedArray())
		} else {
			currentCommand?.getExecuteMethod()?.invoke(currentCommand)
		}
		return true
	}

	override fun onTabComplete(sender: CommandSender, cmd: Command, label: String, args: Array<String>): MutableList<String> {
		var currentCommand: MCommand? = getCommand(label)
		var i = 0
		while (i < args.size - 1) {
			val subCommand = currentCommand?.getSubCommand(args[i])
			if (subCommand != null) {
				currentCommand = subCommand
				i++
			} else {
				// If no more subcommands, break the loop to start suggesting parameters
				break
			}
		}

		// Prepare the list for completions
		val completions = mutableListOf<String>()

		// If we are exactly at the subcommand level, suggest subcommands
		if (i == args.size - 1 && args.last().isEmpty()) {
			completions.addAll(
				currentCommand?.getSubCommands()?.flatMap { it.getAliases() } ?: listOf()
			)
		}

		// Check if we're at the parameter entry stage for the subcommand
		val executeMethod = currentCommand?.getExecuteMethod()
		val parameters = executeMethod?.parameters
		val skipFirstParameter = parameters?.firstOrNull()?.type?.isCommandSender() ?: false

		// If the first parameter is CommandSender or Player and there are parameters to complete
		if (skipFirstParameter && (parameters?.size ?: 0) > 1 && args.isNotEmpty()) {
			// Adjust the completion suggestions based on CommandCompletion annotation
			val commandCompletionAnnotation = executeMethod?.getAnnotation(CommandCompletion::class.java)
			if (commandCompletionAnnotation != null) {
				val completionList = commandCompletionAnnotation.completions.split(" ")
				// Determine the adjusted index for completion, considering skipped CommandSender/Player
				val adjustedIndexForCompletion = if (skipFirstParameter) args.size - 2 else args.size - 1
				if (adjustedIndexForCompletion < completionList.size) {
					val completionOptions = parseCompletion(sender, completionList[adjustedIndexForCompletion])
					completions.addAll(completionOptions.filter { it.startsWith(args.last()) })
				}
			}
		} else if (!skipFirstParameter && args.size > 1) {
			// Handle when the first parameter is not skipped and it's time to suggest parameters
			val commandCompletionAnnotation = executeMethod?.getAnnotation(CommandCompletion::class.java)
			if (commandCompletionAnnotation != null && args.size - 1 <= (parameters?.size ?: 0)) {
				val completionList = commandCompletionAnnotation.completions.split(" ")
				val completionOptions = parseCompletion(sender, completionList[args.size - 2])
				completions.addAll(completionOptions.filter { it.startsWith(args.last()) })
			}
		}

		// Still include subcommands if we're not yet into parameter completion
		if (completions.isEmpty() && args.size <= 1) {
			completions.addAll(
				currentCommand?.getSubCommands()?.flatMap { it.getAliases() } ?: listOf()
			)
		}

		return completions.distinct().toMutableList()
	}

}