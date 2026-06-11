package dev.mrshawn.mlib.guis.serialization

import com.google.gson.JsonElement
import org.bukkit.configuration.ConfigurationSection

/**
 * Converts a Bukkit [ConfigurationSection] (loaded from YAML via `YamlConfiguration`) into a Gson
 * [JsonElement] so the same Gson DTO mapping handles both YAML and JSON. Bukkit represents nested
 * maps as [ConfigurationSection]s, so we recursively flatten those into plain maps before handing
 * the tree to Gson. No new dependency: `YamlConfiguration` ships with spigot-api and Gson with the
 * server.
 */
internal object YamlToJson {

    fun toJsonTree(section: ConfigurationSection): JsonElement =
        MenuGson.GSON.toJsonTree(normalize(section.getValues(false)))

    private fun normalize(value: Any?): Any? = when (value) {
        is ConfigurationSection -> normalizeMap(value.getValues(false))
        is Map<*, *> -> normalizeMap(value)
        is List<*> -> value.map { normalize(it) }
        else -> value
    }

    private fun normalizeMap(map: Map<*, *>): Map<String, Any?> {
        val out = LinkedHashMap<String, Any?>()
        for ((k, v) in map) out[k.toString()] = normalize(v)
        return out
    }

}
