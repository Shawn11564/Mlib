package dev.mrshawn.mlib.chat

import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

object Chat {

	private var logProvider = ""

	fun setLogProvider(provider: String) { logProvider = provider }

	fun tell(toWhom: CommandSender?, message: String?) { if (toWhom != null && message != null && message != "") toWhom.sendMessage(
		colorize(message)
	) }

	fun tell(toWhom: CommandSender, messages: Array<String?>) { for (message in messages) { tell(toWhom, message) } }

	fun tell(toWhom: CommandSender, messages: ArrayList<String>) { for (message in messages) { tell(toWhom, message) } }

	fun tellActionbar(toWhom: Player, message: String?) {
		if (message != null)
			toWhom.spigot().sendMessage(ChatMessageType.ACTION_BAR, *TextComponent.fromLegacyText(colorize(message)))
	}

	fun log(message: String?) {
		if (message != null) {
			val formattedMessage = if (logProvider != "") "[$logProvider] $message" else message
			Bukkit.getConsoleSender().sendMessage(colorize(formattedMessage))
		}
	}

	fun log(collection: Collection<*>) { for (item in collection) { log(item.toString()) } }

	fun log(vararg messages: String?) { for (message in messages) { log(message) } }

	fun error(message: String?) { if (message != null) log("&4[ERROR] $message") }

	fun broadcast(message: String?) { if (message != null) Bukkit.broadcastMessage(colorize(message)) }

	fun broadcast(vararg messages: String?) { for (message in messages) { broadcast(message) } }

	fun clearChat() = run { for (i in 0 .. 100) { Bukkit.broadcastMessage(" ") } }

	fun colorize(message: String?): String =
		if (message != null) ChatColor.translateAlternateColorCodes('&', message) else ""

}