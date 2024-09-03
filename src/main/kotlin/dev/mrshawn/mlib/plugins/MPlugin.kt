package dev.mrshawn.mlib.plugins

import dev.mrshawn.mlib.chat.Chat
import dev.mrshawn.mlib.commands.MCommandManager
import dev.mrshawn.mlib.utilities.events.EventUtils
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin

abstract class MPlugin: JavaPlugin() {

	protected val mcm = MCommandManager()
	protected abstract val listeners: Array<Listener>

	companion object {
		lateinit var instance: MPlugin
	}

	override fun onEnable() {
		instance = getProvidingPlugin(MPlugin::class.java) as MPlugin
		Chat.setLogProvider(instance.name)

		if (!dataFolder.exists()) dataFolder.mkdir()

		initObjects()
		EventUtils.registerEvents(this, *listeners)
		registerCommands()

		postEnable()
	}

	override fun onDisable() {
		postDisable()
	}

	open fun postEnable() {}
	open fun postDisable() {}

	/**
	 * Register any commands, command contexts, and command completions
	 */
	abstract fun registerCommands()

	/**
	 * Access kotlin objects that we need the init to run on plugin load
	 */
	abstract fun initObjects()

}