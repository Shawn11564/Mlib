package dev.mrshawn.mlib.files

import org.bukkit.configuration.MemorySection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

open class KFile(
	plugin: JavaPlugin,
	fileName: String,
	filePath: String = plugin.dataFolder.path,
	isResource: Boolean = false
) {

	private val file = File(filePath + File.separator + fileName.removeSuffix(".yml") + ".yml")
	private lateinit var config: YamlConfiguration

	private val values = mutableMapOf<String, Any?>()


	init {
		if (!file.exists()) {
			val path: Path = Paths.get(file.toURI())
			if (path.parent != null) {
				Files.createDirectories(path.parent)
			}
			if (isResource) {
				plugin.saveResource(fileName.removeSuffix(".yml") + ".yml", false)
			} else {
				file.createNewFile()
			}
		}
		setConfig()
		loadValues()
	}

	private fun setConfig() { config = YamlConfiguration.loadConfiguration(file) }

	fun reload() {
		setConfig()
		loadValues()
	}

	private fun loadValues() {
		values.clear()
		for (key in config.getKeys(true)) {
			val value = config.get(key)
			if (value !is MemorySection) values[key] = value
		}
	}

	fun isValue(key: String): Boolean = values.containsKey(key)
	fun <T> isValue(key: T): Boolean = if (key is IConfigList) isValue(key.getPath()) else { isValue(key) }

	fun <T> set(path: T, value: Any?) {
		if (path is IConfigList) {
			set(path.getPath(), value)
		} else {
			config.set(path.toString(), value)
			values[path.toString()] = value
		}
	}

	fun <T> get(path: T): Any? = if (path is IConfigList) get(path.getPath()) else { values[path.toString()] }
	fun <T> getString(path: T): String? = get(path) as String?
	fun <T> getInt(path: T): Int? = get(path) as Int?
	fun <T> getDouble(path: T): Double? = get(path) as Double?
	fun <T> getBoolean(path: T): Boolean? = get(path) as Boolean?
	fun <T> getStringList(path: T): List<String> {
		val list = get(path) as List<*>
		return list.map { it as String }
	}
	fun <T> getOrSet(path: T, value: Any?): Any? {
		if (!isValue(path)) {
			set(path, value)
		}
		return get(path)
	}
	fun getOrSetDefault(path: IConfigList): Any? = getOrSet(path.getPath(), path.getDefault())

	interface IConfigList {
		fun getPath(): String
		fun getDefault(): Any?
	}

}