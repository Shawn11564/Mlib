package dev.mrshawn.mlib.files.configuration

import dev.mrshawn.mlib.files.configuration.annotations.Value
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import kotlin.reflect.KProperty

abstract class Configuration(private val file: File) {

	private val config = YamlConfiguration.loadConfiguration(file)

	constructor(filePath: String, fileName: String): this(File(filePath, fileName))
	constructor(plugin: JavaPlugin, fileName: String): this(File(plugin.dataFolder, fileName))

	init {
		if (!file.parentFile.exists()) file.mkdirs()
		if (!file.exists()) file.createNewFile()
		load()
	}

	fun save() {
		javaClass.declaredFields.forEach { field ->
			if (field.type == ConfigDelegate::class.java) {
				field.isAccessible = true
				val delegate = field.get(this) as ConfigDelegate<*>
				delegate.save(config)
			}
		}
		saveComments()
		config.save(file)
	}

	fun load() {
		javaClass.declaredFields.forEach { field ->
			if (field.type == ConfigDelegate::class.java) {
				field.isAccessible = true
				val delegate = field.get(this) as ConfigDelegate<*>
				delegate.load(config)
			}
		}
		saveComments()
		config.save(file)
	}

	private fun saveComments() {
		val commentMap = mutableMapOf<String, List<String>>()

		javaClass.declaredFields.forEach { field ->
			if (field.type == ConfigDelegate::class.java) {
				field.isAccessible = true
				val delegate = field.get(this) as ConfigDelegate<*>
				delegate.getComments().let { comments ->
					if (comments.isNotEmpty()) {
						commentMap[delegate.path] = comments.toList()
					}
				}
			}
		}

		val lines = file.readLines().toMutableList()
		var i = 0
		while (i < lines.size) {
			val line = lines[i].trim()
			if (line.isNotEmpty() && !line.startsWith("#")) {
				val key = line.substringBefore(":").trim()
				commentMap[key]?.let { comments ->
					if (i == 0 || !lines[i - 1].trim().startsWith("#")) {
						lines.addAll(i, comments.map { "# $it" })
						i += comments.size
					}
				}
			}
			i++
		}

		file.writeText(lines.joinToString("\n"))
	}

	protected inner class ConfigDelegate<T>(private val defaultValue: T) {
		private var value: T = defaultValue
		lateinit var path: String
		private var comments: Array<String> = emptyArray()

		operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
			return value
		}

		operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
			this.value = value
			property.annotations.filterIsInstance<Value>().firstOrNull()?.let { annotation ->
				path = annotation.path
				comments = annotation.comments
				config.set(path, value)
			}
		}

		fun save(config: YamlConfiguration) {
			config.set(path, value)
		}

		fun load(config: YamlConfiguration) {
			if (config.contains(path)) {
				@Suppress("UNCHECKED_CAST")
				value = config.get(path) as T
			} else {
				config.set(path, value)
			}
		}

		fun getComments(): Array<String> = comments
	}

	protected fun <T> config(defaultValue: T) = ConfigDelegate(defaultValue)
}