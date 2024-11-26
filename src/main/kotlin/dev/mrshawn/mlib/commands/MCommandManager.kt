package dev.mrshawn.mlib.commands

import dev.mrshawn.mlib.chat.Chat
import dev.mrshawn.mlib.commands.annotations.CommandCompletion
import dev.mrshawn.mlib.commands.annotations.Optional
import dev.mrshawn.mlib.commands.enhancements.ExecutionContext
import dev.mrshawn.mlib.commands.exceptions.ContextResolverFailedException
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.SimpleCommandMap
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import java.lang.reflect.Field
import java.lang.reflect.Method

class MCommandManager: TabExecutor {

	private val commands = HashMap<List<String>, MCommand>()
	private val commandCompletions: HashMap<String, (CommandSender) -> Collection<String>> = HashMap()
	private val commandContexts: HashMap<Class<*>, (CommandSender, Array<String>) -> Any> = HashMap()
	private val senderContexts: HashMap<Class<*>, (CommandSender) -> Any> = HashMap()

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

		registerSenderContext(CommandSender::class.java) { it }
		registerSenderContext(Player::class.java) { it as Player }
	}

	companion object {
		private val commandSenderTypes: ArrayList<Class<*>> = arrayListOf(CommandSender::class.java, Player::class.java)

		fun addCommandSenderType(type: Class<*>) {
			commandSenderTypes.add(type)
		}

		fun isCommandSender(type: Class<*>?): Boolean {
			return commandSenderTypes.contains(type)
		}
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

	fun <T: Any> registerSenderContext(clazz: Class<T>, resolver: (CommandSender) -> Any) {
		senderContexts[clazz] = resolver
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

		val context = when (objType.name) {
			"java.lang.Boolean" -> commandContexts[Boolean::class.java]?.invoke(sender, args)
			"java.lang.Integer" -> commandContexts[Int::class.java]?.invoke(sender, args)
			else -> commandContexts[objType]?.invoke(sender, args)
		}

		if (context == null && allowNull) {
			return null
		} else if (context == null) {
			throw ContextResolverFailedException("Failed to resolve context for type: ${objType.simpleName} with arguments: ${args.joinToString()}")
		}

		return context
	}

	private fun <T> parseSenderContext(objType: Class<T>, sender: CommandSender): Any {
		return senderContexts[objType]?.invoke(sender) ?: throw ContextResolverFailedException("Failed to resolve sender context for type: ${objType.simpleName}")
	}

	override fun onCommand(sender: CommandSender, cmd: Command, label: String, args: Array<String>): Boolean {
		var currentCommand: MCommand? = getCommand(label)
		var i = 0
		var methodToExecute: Method? = null

		while (i < args.size && currentCommand != null) {
			val subCommand = currentCommand.getSubCommand(args[i])
			if (subCommand != null) {
				currentCommand = subCommand
				i++
			} else {
				// Check for subcommand method
				methodToExecute = currentCommand.getSubCommandMethod(args[i])
				if (methodToExecute != null) {
					i++ // Move past the subcommand to the arguments for the method
					break
				} else {
					break // No more subcommands, remaining args are for the command method
				}
			}
		}

		// Check command PreConditions
		currentCommand?.getPreconditions()?.forEach { precondition ->
			if (!precondition.check(sender)) {
				Chat.tell(sender, "&c${precondition.failMessage()}")
				return false
			}
		}

		// Determine which method to execute
		val executeMethod = methodToExecute ?: currentCommand?.getExecuteMethod()
		val executeMethodParams = executeMethod?.parameters

		// Parse method parameters and handle CommandSender or Player specifically
		val parsedContext: MutableList<Any?> = mutableListOf()

		executeMethodParams?.forEachIndexed { index, param ->
			when {
				index == 0 && (isCommandSender(param.type)) -> {
					// For the first parameter, if it's CommandSender or Player, use the sender directly
					parsedContext.add(parseSenderContext(param.type, sender))
				}
				else -> {
					if (i < args.size) {
						// Argument provided, parse based on expected type
						try {
							val context = parseContext(param.type, sender, args.copyOfRange(i, args.size), false)
							parsedContext.add(context)
							i++
						} catch (e: ContextResolverFailedException) {
							Chat.tell(sender, "&c${e.message}")
							return false
						}
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
			executeMethod?.invoke(currentCommand, *parsedContext.toTypedArray())
		} else {
			executeMethod?.invoke(currentCommand)
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

		// Parse command PreConditions
		currentCommand?.getPreconditions()?.forEach { precondition ->
			if (!precondition.check(sender)) {
				return mutableListOf()
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
		val skipFirstParameter = isCommandSender(parameters?.firstOrNull()?.type)

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
				currentCommand?.getSubCommands()?.flatMap { it.getAliases().filter { alias -> alias.startsWith(args.last()) } } ?: listOf()
			)
		}

		// Include both subcommands and subcommand methods in completions
		if (completions.isEmpty() && args.size <= 1) {
			completions.addAll(
				currentCommand?.getAllSubcommandAliases()?.filter { it.startsWith(args.last()) } ?: listOf()
			)
		}


		return completions.distinct().toMutableList()
	}

}