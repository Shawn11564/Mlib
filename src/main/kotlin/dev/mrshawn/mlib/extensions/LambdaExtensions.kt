package dev.mrshawn.mlib.extensions

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

fun (() -> Unit).runLater(plugin: JavaPlugin, delay: Long): Int {
	return Bukkit.getScheduler().runTaskLater(plugin, Runnable {
		this()
	}, delay).taskId
}