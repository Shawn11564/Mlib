package dev.mrshawn.mlib.chat.platform

import net.kyori.adventure.text.Component
import org.bukkit.command.CommandSender

/**
 * Strategy for delivering Adventure [Component]s to a recipient.
 *
 * Two implementations exist:
 *  - [BukkitAudiencesPlatform] — works everywhere (Spigot + Paper) via the
 *    adventure-platform-bukkit bridge.
 *  - [PaperNativePlatform] — uses the server's own native Adventure on Paper/forks,
 *    skipping the bridge entirely.
 *
 * The component passed in is *this library's* relocated Adventure type; platform
 * implementations are responsible for delivering it appropriately.
 */
interface ChatPlatform {

	fun sendMessage(sender: CommandSender, component: Component)

	fun sendActionBar(sender: CommandSender, component: Component)

	fun showTitle(
		sender: CommandSender,
		title: Component,
		subtitle: Component,
		fadeIn: Int,
		stay: Int,
		fadeOut: Int
	)

	/** Releases any resources held by the platform. */
	fun close()

}
