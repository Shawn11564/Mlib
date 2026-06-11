package dev.mrshawn.mlib.chat.platform

import net.kyori.adventure.platform.bukkit.BukkitAudiences
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import java.time.Duration

/**
 * Delivers components through adventure-platform-bukkit's [BukkitAudiences]. Works on
 * any server (Spigot included); on Paper it transparently bridges to native Adventure.
 */
class BukkitAudiencesPlatform(plugin: JavaPlugin) : ChatPlatform {

	private val audiences: BukkitAudiences = BukkitAudiences.create(plugin)

	override fun sendMessage(sender: CommandSender, component: Component) {
		audiences.sender(sender).sendMessage(component)
	}

	override fun sendActionBar(sender: CommandSender, component: Component) {
		audiences.sender(sender).sendActionBar(component)
	}

	override fun showTitle(
		sender: CommandSender,
		title: Component,
		subtitle: Component,
		fadeIn: Int,
		stay: Int,
		fadeOut: Int
	) {
		val times = Title.Times.times(ticks(fadeIn), ticks(stay), ticks(fadeOut))
		audiences.sender(sender).showTitle(Title.title(title, subtitle, times))
	}

	override fun close() {
		audiences.close()
	}

	private fun ticks(ticks: Int): Duration = Duration.ofMillis(ticks * 50L)

}
