package dev.mrshawn.mlib.files

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.google.gson.reflect.TypeToken
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.lang.reflect.Type

/**
 * A JSON-backed file, the Gson counterpart to [KFile].
 *
 * It serves two purposes at once:
 *
 *  1. **Flat config access** by dot-separated path (`get`/`set`/`getString`/`getInt`/…),
 *     mirroring [KFile] but persisted as JSON. Nested objects are created on demand,
 *     so `set("arena.spawn.x", 10)` produces `{ "arena": { "spawn": { "x": 10 } } }`.
 *  2. **Typed object mapping** via Gson: read whole documents or path fragments straight
 *     into data classes/POJOs with [read]/[get], and persist objects with [write]/[set].
 *
 * Unlike [KFile], writes are kept in memory until [save] is called.
 *
 * ```kotlin
 * val store = Kson(plugin, "arenas.json")
 * store.set("lobby.maxPlayers", 16)
 * store.write(listOf(arenaA, arenaB))          // whole-document object mapping
 * store.save()
 *
 * val maxPlayers = store.getInt("lobby.maxPlayers")
 * val arenas: List<Arena>? = store.readValue() // reified, keeps generics
 * ```
 */
open class Kson(
	plugin: JavaPlugin,
	fileName: String,
	filePath: String = plugin.dataFolder.path,
	isResource: Boolean = false,
	private val prettyPrint: Boolean = true
) {

	companion object {
		private val PRETTY_GSON: Gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
		private val COMPACT_GSON: Gson = GsonBuilder().disableHtmlEscaping().create()
	}

	private val file = File(filePath + File.separator + fileName.removeSuffix(".json") + ".json")
	private val gson: Gson get() = if (prettyPrint) PRETTY_GSON else COMPACT_GSON

	/** The in-memory document root. Defaults to an empty object so path access works immediately. */
	private var root: JsonElement = JsonObject()

	init {
		if (!file.exists()) {
			file.parentFile?.mkdirs()
			if (isResource) {
				plugin.saveResource(fileName.removeSuffix(".json") + ".json", false)
			} else {
				file.createNewFile()
				file.writeText("{}")
			}
		}
		reload()
	}

	/** Re-reads the document from disk, discarding unsaved in-memory changes. */
	fun reload() {
		val content = file.readText().ifBlank { "{}" }
		root = JsonParser.parseString(content)
	}

	/** Writes the current in-memory document to disk. */
	fun save() {
		file.writeText(gson.toJson(root))
	}

	// ---------------------------------------------------------------------------------------------
	// Path navigation
	// ---------------------------------------------------------------------------------------------

	private fun rootObject(create: Boolean): JsonObject? {
		if (root.isJsonObject) return root.asJsonObject
		if (create) {
			val obj = JsonObject()
			root = obj
			return obj
		}
		return null
	}

	/** Resolves the parent object holding the leaf of [path], optionally creating intermediate objects. */
	private fun resolveParent(path: String, create: Boolean): JsonObject? {
		val parts = path.split(".")
		var current = rootObject(create) ?: return null
		for (i in 0 until parts.size - 1) {
			val key = parts[i]
			val child = current.get(key)
			current = when {
				child != null && child.isJsonObject -> child.asJsonObject
				create -> JsonObject().also { current.add(key, it) }
				else -> return null
			}
		}
		return current
	}

	private fun leafKey(path: String): String = path.substringAfterLast(".")

	// ---------------------------------------------------------------------------------------------
	// Flat config access
	// ---------------------------------------------------------------------------------------------

	/** Whether a value (including explicit JSON null) exists at [path]. */
	fun contains(path: String): Boolean {
		val parent = resolveParent(path, false) ?: return false
		return parent.has(leafKey(path))
	}

	/**
	 * Stores [value] at [path]. Any object is serialized via Gson; passing `null` removes the path.
	 * Intermediate objects are created as needed. Call [save] to persist to disk.
	 */
	fun set(path: String, value: Any?) {
		if (value == null) {
			val parent = resolveParent(path, false) ?: return
			parent.remove(leafKey(path))
			return
		}
		val parent = resolveParent(path, true) ?: return
		parent.add(leafKey(path), if (value is JsonElement) value else gson.toJsonTree(value))
	}

	/** The raw [JsonElement] at [path], or `null` if absent or explicitly JSON null. */
	fun get(path: String): JsonElement? {
		val parent = resolveParent(path, false) ?: return null
		val element = parent.get(leafKey(path)) ?: return null
		return if (element.isJsonNull) null else element
	}

	private fun primitive(path: String): JsonPrimitive? =
		get(path)?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive

	fun getString(path: String): String? = primitive(path)?.asString
	fun getInt(path: String): Int? = primitive(path)?.takeIf { it.isNumber }?.asInt
	fun getLong(path: String): Long? = primitive(path)?.takeIf { it.isNumber }?.asLong
	fun getDouble(path: String): Double? = primitive(path)?.takeIf { it.isNumber }?.asDouble
	fun getBoolean(path: String): Boolean? = primitive(path)?.takeIf { it.isBoolean }?.asBoolean

	fun getStringList(path: String): List<String> {
		val element = get(path) ?: return emptyList()
		if (!element.isJsonArray) return emptyList()
		return element.asJsonArray.mapNotNull { if (it.isJsonNull) null else it.asString }
	}

	/** Returns the value at [path]; if absent, stores [default] first and returns it. */
	fun getOrSet(path: String, default: Any?): JsonElement? {
		if (!contains(path)) set(path, default)
		return get(path)
	}

	// ---------------------------------------------------------------------------------------------
	// Typed object mapping
	// ---------------------------------------------------------------------------------------------

	/** Deserializes the value at [path] into [type], or `null` if the path is absent. */
	fun <T> get(path: String, type: Type): T? {
		val element = get(path) ?: return null
		return gson.fromJson(element, type)
	}

	/** Deserializes the value at [path] into [clazz], or `null` if the path is absent. */
	fun <T> get(path: String, clazz: Class<T>): T? = get(path, clazz as Type)

	/** Deserializes the entire document into [type]. */
	fun <T> read(type: Type): T? = gson.fromJson(root, type)

	/** Deserializes the entire document into [clazz]. */
	fun <T> read(clazz: Class<T>): T? = gson.fromJson(root, clazz)

	/** Replaces the entire document with the serialized form of [obj]. Call [save] to persist. */
	fun write(obj: Any?) {
		root = gson.toJsonTree(obj)
	}

	/** Reified path read that preserves generic type arguments (e.g. `getValue<List<Arena>>("arenas")`). */
	inline fun <reified T> getValue(path: String): T? = get(path, object : TypeToken<T>() {}.type)

	/** Reified whole-document read that preserves generic type arguments. */
	inline fun <reified T> readValue(): T? = read(object : TypeToken<T>() {}.type)

	// ---------------------------------------------------------------------------------------------
	// Enum-driven paths (shared with KFile)
	// ---------------------------------------------------------------------------------------------

	fun contains(path: KFile.IConfigList): Boolean = contains(path.getPath())
	fun get(path: KFile.IConfigList): JsonElement? = get(path.getPath())
	fun set(path: KFile.IConfigList, value: Any?) = set(path.getPath(), value)
	fun getOrSetDefault(path: KFile.IConfigList): JsonElement? = getOrSet(path.getPath(), path.getDefault())

}
