package dev.mrshawn.mlib.extensions

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

fun (() -> Any).runLater(plugin: JavaPlugin, delay: Long): Int {
	return Runnable { this() }.runLater(plugin, delay)
}

fun Runnable.runLater(plugin: JavaPlugin, delay: Long): Int {
	return Bukkit.getScheduler().runTaskLater(plugin, this, delay).taskId
}