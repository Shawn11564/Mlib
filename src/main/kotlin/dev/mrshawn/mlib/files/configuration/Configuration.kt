package dev.mrshawn.mlib.files.configuration

import dev.mrshawn.mlib.files.configuration.annotations.Value
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import kotlin.reflect.KProperty

/** Thrown when a configuration value is misconfigured or cannot be loaded as its expected type. */
class ConfigurationException(message: String) : RuntimeException(message)

/**
 * Annotation-driven YAML configuration.
 *
 * Declare properties with a [Value] annotation and the [config] delegate, then call
 * [load] **after** the properties are initialised — i.e. from an `init { load() }`
 * block placed below the property declarations, or right after construction:
 *
 * ```kotlin
 * class Settings(plugin: JavaPlugin) : Configuration(plugin, "settings.yml") {
 *     @Value("settings.prefix", comments = ["The chat prefix"])
 *     var prefix: String by config("&7[MyPlugin] ")
 *
 *     init { load() }   // must come after the delegates above
 * }
 * ```
 *
 * [load] cannot run from this base constructor because a subclass's delegate fields
 * are not created until after the superclass constructor returns.
 */
abstract class Configuration(private val file: File) {

	private val config = YamlConfiguration.loadConfiguration(file)
	private val delegates = mutableListOf<ConfigDelegate<*>>()

	constructor(filePath: String, fileName: String): this(File(filePath, fileName))
	constructor(plugin: JavaPlugin, fileName: String): this(File(plugin.dataFolder, fileName))

	init {
		file.parentFile?.let { if (!it.exists()) it.mkdirs() }
		if (!file.exists()) file.createNewFile()
	}

	fun save() {
		delegates.forEach { it.save(config) }
		persist()
	}

	fun load() {
		delegates.forEach { it.load(config) }
		persist()
	}

	/** Discards in-memory state and re-reads everything from disk. */
	fun reload() {
		config.load(file)
		load()
	}

	/**
	 * Returns the value at [path]. If the path is absent (or null), stores [default],
	 * persists the file, and returns [default]. Useful for ad-hoc settings that aren't
	 * declared as `@Value` delegates. Values are type-coerced like the delegates, so a
	 * mismatch raises a [ConfigurationException] rather than a cryptic cast failure.
	 */
	fun <T> getOrSetDefault(path: String, default: T): T {
		val raw = if (config.contains(path)) config.get(path) else null
		if (raw == null) {
			config.set(path, default)
			persist()
			return default
		}
		@Suppress("UNCHECKED_CAST")
		return coerceValue(path, raw, (default as Any?)?.javaClass) as T
	}

	/** Writes the data, then re-applies comments (which `config.save` would otherwise strip). */
	private fun persist() {
		config.save(file)
		saveComments()
	}

	/**
	 * Coerces [raw] to [expected], applying lenient numeric widening/narrowing. Throws a
	 * descriptive [ConfigurationException] when the value genuinely can't be used.
	 */
	private fun coerceValue(path: String, raw: Any, expected: Class<*>?): Any {
		if (expected == null || expected.isInstance(raw)) return raw

		if (raw is Number) {
			val coerced: Any? = when (expected) {
				java.lang.Integer::class.java -> raw.toInt()
				java.lang.Long::class.java -> raw.toLong()
				java.lang.Double::class.java -> raw.toDouble()
				java.lang.Float::class.java -> raw.toFloat()
				java.lang.Short::class.java -> raw.toShort()
				java.lang.Byte::class.java -> raw.toByte()
				else -> null
			}
			if (coerced != null) return coerced
		}

		if (Collection::class.java.isAssignableFrom(expected) && raw is Collection<*>) return raw

		throw ConfigurationException(
			"Could not load '$path' from '${file.name}': expected ${expected.simpleName} " +
				"but found ${raw.javaClass.simpleName} (value: $raw). " +
				"Fix the value, or remove the line to regenerate its default."
		)
	}

	/**
	 * Re-injects `@Value` comments above their keys. Tracks the full dotted path by
	 * indentation depth, so comments work for nested keys (e.g. `economy.start-balance`),
	 * not just top-level ones. Assumes the 2-space indentation Bukkit writes.
	 */
	private fun saveComments() {
		val commentMap = delegates
			.filter { it.getComments().isNotEmpty() }
			.associate { it.path to it.getComments().toList() }
		if (commentMap.isEmpty()) return

		val output = ArrayList<String>()
		val pathStack = ArrayList<String>()

		for (line in file.readLines()) {
			val trimmed = line.trim()

			// Pass blanks, existing comments, list items, and non key/value lines straight through.
			if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("-") || !trimmed.contains(":")) {
				output.add(line)
				continue
			}

			val indent = line.indexOfFirst { it != ' ' }.let { if (it < 0) 0 else it }
			val depth = indent / 2
			val key = trimmed.substringBefore(":").trim()

			// Rebuild the dotted path for this key from the indentation depth.
			while (pathStack.size > depth) pathStack.removeAt(pathStack.size - 1)
			pathStack.add(key)
			val fullPath = pathStack.joinToString(".")

			val comments = commentMap[fullPath]
			if (comments != null && (output.isEmpty() || !output.last().trim().startsWith("#"))) {
				val pad = " ".repeat(indent)
				comments.forEach { output.add("$pad# $it") }
			}
			output.add(line)
		}

		file.writeText(output.joinToString("\n"))
	}

	protected inner class ConfigDelegate<T>(private val defaultValue: T) {
		private var value: T = defaultValue
		// Captured from the default so [load] can validate/coerce without an erased cast.
		private val expectedType: Class<*>? = (defaultValue as Any?)?.javaClass
		lateinit var path: String
		private var comments: Array<String> = emptyArray()

		/**
		 * Runs when the property is delegated (`by config(...)`). This captures the
		 * [Value] annotation's path/comments and registers the delegate so [load] and
		 * [save] can find it — without relying on the property ever being reassigned.
		 */
		operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): ConfigDelegate<T> {
			val annotation = property.annotations.filterIsInstance<Value>().firstOrNull()
				?: throw ConfigurationException(
					"Property '${property.name}' in ${this@Configuration.javaClass.simpleName} " +
						"uses config() but is missing the @Value annotation."
				)
			path = annotation.path
			comments = annotation.comments
			if (!config.contains(path)) config.set(path, value)
			delegates.add(this)
			return this
		}

		operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
			return value
		}

		operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
			this.value = value
			config.set(path, value)
		}

		fun save(config: YamlConfiguration) {
			config.set(path, value)
		}

		fun load(config: YamlConfiguration) {
			val raw = if (config.contains(path)) config.get(path) else null
			if (raw == null) {
				// Absent (or explicitly null) -> fall back to the declared default and write it.
				value = defaultValue
				config.set(path, defaultValue)
				return
			}
			@Suppress("UNCHECKED_CAST")
			value = coerceValue(path, raw, expectedType) as T
		}

		fun getComments(): Array<String> = comments
	}

	protected fun <T> config(defaultValue: T) = ConfigDelegate(defaultValue)
}
