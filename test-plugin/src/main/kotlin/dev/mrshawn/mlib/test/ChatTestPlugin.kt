package dev.mrshawn.mlib.test

import dev.mrshawn.mlib.chat.Chat
import dev.mrshawn.mlib.chat.TextMessage
import dev.mrshawn.mlib.chat.TextReplacement
import dev.mrshawn.mlib.chat.platform.PaperNativePlatform
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

/**
 * Manual test harness for Mlib's [Chat] system.
 *
 * Run with `./gradlew runServer`, then in-game / console use `/chattest <sub>` to
 * exercise each feature. On enable it logs which [dev.mrshawn.mlib.chat.platform.ChatPlatform]
 * was selected — on Paper this should report `PaperNativePlatform`.
 */
class ChatTestPlugin : JavaPlugin() {

	override fun onEnable() {
		// Mirrors what MPlugin does automatically. AUTO -> Paper-native when available.
		Chat.init(this)

		logger.info("Paper-native Adventure available: ${PaperNativePlatform.isAvailable()}")
		logger.info("Active chat platform: ${Chat.activePlatform()}")
		// Exercises the logging path (level + plugin-name labelling) and hex colorize.
		Chat.log("&aChatTest enabled with &#55FF55hex &alogging support")
		// End-to-end send through the active platform, to the console audience.
		Chat.tellMini(server.consoleSender, "<green>Platform send OK via ${Chat.activePlatform()}</green>")
	}

	override fun onDisable() {
		Chat.shutdown()
	}

	override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
		when (args.getOrNull(0)?.lowercase() ?: "help") {
			"legacy" ->
				Chat.tell(sender, "&aLegacy &bcolors&r, &cand &#FF8800hex &#33CCFFcodes&r work together")

			"mini" ->
				Chat.tellMini(sender, "<rainbow>MiniMessage</rainbow> with a <bold><gradient:#FF0000:#0000FF>gradient</gradient></bold>")

			"actionbar" ->
				Chat.tellActionbar(sender, "&eAction bar &#FF00FFtest &7(legacy + hex)")

			"title" ->
				Chat.sendTitle(sender, "&6&lMlib", "&7chat test &#55FF55subtitle", fadeIn = 10, stay = 60, fadeOut = 10)

			"broadcast" ->
				Chat.broadcast("&b[Broadcast] &fHello, everyone!")

			"clear" ->
				Chat.clearChat(sender, 30)

			"template" ->
				TextMessage()
					.addMessages("&aHello %player%!", "&7Active platform: &e%platform%")
					.addReplacements(
						TextReplacement.of(
							"%player%" to sender.name,
							"%platform%" to Chat.activePlatform()
						)
					)
					.send(sender)

			"papi" ->
				if (sender is Player) Chat.tell(sender, "&7Name via PlaceholderAPI: &f%player_name%")
				else Chat.tell(sender, "&cRun this one as a player.")

			"platform" ->
				Chat.tell(sender, "&7Active platform: &e${Chat.activePlatform()} &7(native available: &e${PaperNativePlatform.isAvailable()}&7)")

			else ->
				Chat.tell(
					sender,
					listOf(
						"&6&lMlib ChatTest",
						"&e/chattest legacy &7- & codes + &#RRGGBB hex",
						"&e/chattest mini &7- MiniMessage tags",
						"&e/chattest actionbar &7- action bar",
						"&e/chattest title &7- title + subtitle",
						"&e/chattest broadcast &7- broadcast to all",
						"&e/chattest clear &7- clear your chat",
						"&e/chattest template &7- TextMessage + TextReplacement",
						"&e/chattest papi &7- PlaceholderAPI (needs the plugin)",
						"&e/chattest platform &7- show the active platform"
					)
				)
		}
		return true
	}

}
