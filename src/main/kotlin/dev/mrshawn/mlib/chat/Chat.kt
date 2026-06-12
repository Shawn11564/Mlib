package dev.mrshawn.mlib.chat

import dev.mrshawn.mlib.chat.Chat.colorize
import dev.mrshawn.mlib.chat.Chat.init
import dev.mrshawn.mlib.chat.Chat.mini
import dev.mrshawn.mlib.chat.Chat.send
import dev.mrshawn.mlib.chat.Chat.severe
import dev.mrshawn.mlib.chat.Chat.shutdown
import dev.mrshawn.mlib.chat.Chat.tell
import dev.mrshawn.mlib.chat.Chat.tellMini
import dev.mrshawn.mlib.chat.platform.BukkitAudiencesPlatform
import dev.mrshawn.mlib.chat.platform.ChatPlatform
import dev.mrshawn.mlib.chat.platform.PaperNativePlatform
import me.clip.placeholderapi.PlaceholderAPI
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Level
import java.util.logging.Logger
import java.util.regex.Pattern
import net.md_5.bungee.api.ChatColor as BungeeChatColor

/**
 * Central chat / logging helper.
 *
 * Supports three text formats:
 *  - **Legacy** `&`-codes *and* hex `&#RRGGBB` (1.16+) via [colorize] / [tell].
 *  - **MiniMessage** tags (`<red>`, `<gradient>`, …) via [mini] / [tellMini].
 *  - Raw Adventure [Component]s via [send].
 *
 * Delivery goes through a [ChatPlatform] once [init] has been called: Paper servers use
 * native Adventure ([PaperNativePlatform]); everything else uses adventure-platform-bukkit
 * ([BukkitAudiencesPlatform]). Before [init] (or if a platform fails), sending falls back
 * to the plain Bukkit API. If PlaceholderAPI is installed, placeholders are resolved
 * automatically when a [Player] recipient is known.
 *
 * Plugins extending `MPlugin` get [init]/[shutdown] wired up automatically. Standalone
 * plugins should call [init] in `onEnable` and [shutdown] in `onDisable`.
 */
object Chat {

	/** Which [ChatPlatform] to use. [AUTO] prefers Paper-native when available. */
	enum class Platform { AUTO, PAPER_NATIVE, BUKKIT_AUDIENCES }

	private val HEX_PATTERN: Pattern = Pattern.compile("&#([0-9a-fA-F]{6})")

	private val MINI: MiniMessage = MiniMessage.miniMessage()
	private val LEGACY: LegacyComponentSerializer = LegacyComponentSerializer.builder()
		.hexColors()
		.useUnusualXRepeatedCharacterHexFormat()
		.build()

	private var logProvider = ""
	private var logger: Logger = Bukkit.getLogger()
	private var platform: ChatPlatform? = null
	private var placeholderApiPresent = false
	// Latches true after the first send with no active platform, so the warning fires once, not per line.
	private var warnedNoPlatform = false

	// ---------------------------------------------------------------------------------------------
	// Lifecycle
	// ---------------------------------------------------------------------------------------------

	/** Wires Chat to a plugin: selects a [ChatPlatform], adopts the plugin logger, detects PAPI. */
	fun init(plugin: JavaPlugin, platform: Platform = Platform.AUTO) {
		logProvider = plugin.name
		logger = plugin.logger
		placeholderApiPresent = plugin.server.pluginManager.getPlugin("PlaceholderAPI") != null
		this.platform = createPlatform(plugin, platform)
		warnedNoPlatform = false
	}

	private fun createPlatform(plugin: JavaPlugin, requested: Platform): ChatPlatform = when (requested) {
		Platform.BUKKIT_AUDIENCES -> BukkitAudiencesPlatform(plugin)
		Platform.PAPER_NATIVE -> tryPaperNative(plugin)
		Platform.AUTO -> if (PaperNativePlatform.isAvailable()) tryPaperNative(plugin) else BukkitAudiencesPlatform(plugin)
	}

	private fun tryPaperNative(plugin: JavaPlugin): ChatPlatform = try {
		PaperNativePlatform()
	} catch (e: Throwable) {
		logger.log(Level.WARNING, "Paper-native chat unavailable, falling back to BukkitAudiences: ${e.message}")
		BukkitAudiencesPlatform(plugin)
	}

	/** Releases the active platform. Call from `onDisable`. */
	fun shutdown() {
		platform?.close()
		platform = null
	}

	/** Diagnostic: the simple name of the active [ChatPlatform], or a fallback marker. */
	fun activePlatform(): String = platform?.let { it::class.java.simpleName } ?: "none (Bukkit fallback)"

	fun setLogger(logger: Logger) { this.logger = logger }
	fun setLogProvider(provider: String) { logProvider = provider }

	// ---------------------------------------------------------------------------------------------
	// Formatting
	// ---------------------------------------------------------------------------------------------

	/** Translates legacy `&` codes and hex `&#RRGGBB` codes into a Bukkit-ready string. */
	fun colorize(message: String?): String {
		if (message == null) return ""
		val matcher = HEX_PATTERN.matcher(message)
		val buffer = StringBuffer()
		while (matcher.find()) {
			matcher.appendReplacement(buffer, BungeeChatColor.of("#${matcher.group(1)}").toString())
		}
		matcher.appendTail(buffer)
		return ChatColor.translateAlternateColorCodes('&', buffer.toString())
	}

	/** Builds a [Component] from a legacy (`&` / `&#hex`) string, resolving PAPI placeholders for [viewer]. */
	fun legacy(message: String?, viewer: OfflinePlayer? = null): Component =
		LEGACY.deserialize(colorize(setPlaceholders(viewer, message ?: "")))

	/** Builds a [Component] from a MiniMessage string, resolving PAPI placeholders for [viewer]. */
	fun mini(message: String?, viewer: OfflinePlayer? = null): Component =
		MINI.deserialize(setPlaceholders(viewer, message ?: ""))

	// ---------------------------------------------------------------------------------------------
	// Sending
	// ---------------------------------------------------------------------------------------------

	/** Sends a raw [Component] to a recipient (via the active platform, or a legacy string fallback). */
	fun send(toWhom: CommandSender?, component: Component) {
		if (toWhom == null) return
		val active = platform
		if (active != null) {
			active.sendMessage(toWhom, component)
		} else {
			warnNoPlatformOnce()
			toWhom.sendMessage(LEGACY.serialize(component))
		}
	}

	/**
	 * Warns (once) that Chat is delivering without an active platform, i.e. [init] never ran for this
	 * plugin. mlib is shaded into each plugin, so every plugin owns a separate [Chat] singleton and must
	 * call [init] itself — a sibling plugin's init does nothing here. In this state messages fall back to
	 * plain legacy text: MiniMessage interactivity (hover/click) is dropped and PlaceholderAPI is skipped.
	 */
	private fun warnNoPlatformOnce() {
		if (warnedNoPlatform) return
		warnedNoPlatform = true
		logger.log(
			Level.WARNING,
			"Chat is sending without an active platform — Chat.init(plugin) was not called for this plugin. " +
				"Messages fall back to plain legacy text: hover/click and other MiniMessage interactivity are " +
				"dropped and PlaceholderAPI is not resolved. Call Chat.init(this) in onEnable() (and " +
				"Chat.shutdown() in onDisable), or extend MPlugin. Note: mlib is shaded per-plugin, so each " +
				"plugin must init its own Chat — initializing it in another plugin has no effect here."
		)
	}

	fun tell(toWhom: CommandSender?, message: String?) = tellOne(toWhom, message)

	fun tell(toWhom: CommandSender?, vararg messages: String) {
		messages.forEach { tellOne(toWhom, it) }
	}

	fun tell(toWhom: CommandSender?, messages: Iterable<String>) {
		messages.forEach { tellOne(toWhom, it) }
	}

	fun tell(toWhom: CommandSender?, message: TextMessage) {
		tell(toWhom, message.getMessage())
	}

	// Single source of truth for sending one line. Every public tell(...) overload funnels here
	// rather than into each other: calling tell(toWhom, it) with a non-null String re-selected the
	// vararg overload (its non-null String element is "more specific" than this String?), so the
	// vararg / Iterable overloads recursed into themselves forever (StackOverflowError).
	private fun tellOne(toWhom: CommandSender?, message: String?) {
		if (toWhom == null || message.isNullOrEmpty()) return
		send(toWhom, legacy(message, toWhom as? Player))
	}

	/** Sends a MiniMessage-formatted message. */
	fun tellMini(toWhom: CommandSender?, message: String?) {
		if (toWhom == null || message.isNullOrEmpty()) return
		send(toWhom, mini(message, toWhom as? Player))
	}

	fun tellActionbar(toWhom: CommandSender?, message: String?) {
		if (toWhom == null || message.isNullOrEmpty()) return
		val active = platform
		if (active != null) {
			active.sendActionBar(toWhom, legacy(message, toWhom as? Player))
		} else if (toWhom is Player) {
			warnNoPlatformOnce()
			@Suppress("DEPRECATION")
			toWhom.spigot().sendMessage(
				ChatMessageType.ACTION_BAR,
				*TextComponent.fromLegacyText(colorize(setPlaceholders(toWhom, message)))
			)
		}
	}

	/**
	 * Shows a title/subtitle. [fadeIn]/[stay]/[fadeOut] are in **ticks**. Falls back to
	 * [Player.sendTitle] when no platform is active.
	 */
	fun sendTitle(
		toWhom: CommandSender?,
		title: String?,
		subtitle: String? = null,
		fadeIn: Int = 10,
		stay: Int = 70,
		fadeOut: Int = 20
	) {
		if (toWhom == null) return
		val viewer = toWhom as? Player
		val active = platform
		if (active != null) {
			active.showTitle(toWhom, legacy(title ?: "", viewer), legacy(subtitle ?: "", viewer), fadeIn, stay, fadeOut)
		} else if (viewer != null) {
			warnNoPlatformOnce()
			viewer.sendTitle(colorize(title ?: ""), colorize(subtitle ?: ""), fadeIn, stay, fadeOut)
		}
	}

	// ---------------------------------------------------------------------------------------------
	// Broadcasting
	// ---------------------------------------------------------------------------------------------

	fun broadcast(message: String?) {
		if (message.isNullOrEmpty()) return
		val component = legacy(message)
		Bukkit.getOnlinePlayers().forEach { send(it, component) }
		send(Bukkit.getConsoleSender(), component)
	}

	fun broadcast(vararg messages: String?) { messages.forEach { broadcast(it) } }

	fun broadcast(messages: Iterable<String>) { messages.forEach { broadcast(it) } }

	/** Broadcasts only to players holding [permission]. */
	fun broadcastPermission(message: String?, permission: String) {
		if (message.isNullOrEmpty()) return
		val component = legacy(message)
		Bukkit.getOnlinePlayers()
			.filter { it.hasPermission(permission) }
			.forEach { send(it, component) }
	}

	/** Sends [count] blank lines to a single recipient to push old messages off-screen. */
	fun clearChat(toWhom: CommandSender?, count: Int = 100) {
		if (toWhom == null) return
		repeat(count) { tell(toWhom, " ") }
	}

	/** Sends [count] blank lines to every online player. */
	fun clearChatAll(count: Int = 100) {
		repeat(count) { broadcast(" ") }
	}

	// ---------------------------------------------------------------------------------------------
	// Logging — routed through the plugin Logger so output is labelled with the plugin name
	// ---------------------------------------------------------------------------------------------

	fun log(message: String?) = logAt(Level.INFO, message)
	fun warn(message: String?) = logAt(Level.WARNING, message)
	fun severe(message: String?) = logAt(Level.SEVERE, message)

	/** Backwards-compatible alias for [severe]. */
	fun error(message: String?) = logAt(Level.SEVERE, message)

	fun log(vararg messages: String?) { messages.forEach { log(it) } }
	fun log(collection: Collection<*>) { collection.forEach { log(it?.toString()) } }

	private fun logAt(level: Level, message: String?) {
		if (message == null) return
		// A plugin Logger already prefixes "[PluginName]"; only add the provider prefix
		// when falling back to the global server logger.
		val usingServerLogger = logger === Bukkit.getLogger()
		val prefix = if (usingServerLogger && logProvider.isNotEmpty()) "[$logProvider] " else ""
		logger.log(level, colorize(prefix + message))
	}

	// ---------------------------------------------------------------------------------------------
	// PlaceholderAPI
	// ---------------------------------------------------------------------------------------------

	/** Resolves PlaceholderAPI placeholders in [text] for [player], or returns [text] unchanged. */
	fun setPlaceholders(player: OfflinePlayer?, text: String): String =
		if (placeholderApiPresent && player != null) applyPlaceholders(player, text) else text

	// Isolated so the PlaceholderAPI class is only loaded when the plugin is actually present.
	private fun applyPlaceholders(player: OfflinePlayer, text: String): String =
		PlaceholderAPI.setPlaceholders(player, text)

}
