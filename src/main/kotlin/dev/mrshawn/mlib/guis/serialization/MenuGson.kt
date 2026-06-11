package dev.mrshawn.mlib.guis.serialization

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

/**
 * The Gson instance used by the loader.
 *
 * We build a dedicated Gson (rather than routing through [dev.mrshawn.mlib.files.Kson]) because
 * the format needs a custom [ConditionDTO] adapter that accepts both a string shorthand and an
 * object form. Gson itself is already on the classpath (bundled with Spigot/Paper), so this adds
 * no new dependency.
 */
internal object MenuGson {

    val GSON: Gson = GsonBuilder()
        .registerTypeAdapter(ConditionDTO::class.java, ConditionDeserializer())
        .create()

}

/**
 * Deserializes [ConditionDTO] from either:
 *  - a string: `"permission:node"`, `"placeholder:%x% == y"`, `"gamemode:CREATIVE"`, `"op"`, `"custom:id"`
 *  - an object: `{ all: [..] }`, `{ any: [..] }`, `{ not: <cond> }`, or explicit `{ type, value }`
 */
private class ConditionDeserializer : JsonDeserializer<ConditionDTO> {

    override fun deserialize(json: JsonElement, typeOfT: Type, ctx: JsonDeserializationContext): ConditionDTO {
        if (json.isJsonPrimitive) return parseString(json.asString)

        if (json.isJsonObject) {
            val obj = json.asJsonObject
            obj.get("all")?.let { return ConditionDTO(type = "all", children = parseList(it, ctx)) }
            obj.get("any")?.let { return ConditionDTO(type = "any", children = parseList(it, ctx)) }
            obj.get("not")?.let { return ConditionDTO(type = "not", child = ctx.deserialize(it, ConditionDTO::class.java)) }
            val type = obj.get("type")?.takeIf { !it.isJsonNull }?.asString ?: ""
            val value = obj.get("value")?.takeIf { !it.isJsonNull }?.asString
            return ConditionDTO(type = type.lowercase(), value = value)
        }

        return ConditionDTO(type = "")
    }

    private fun parseList(el: JsonElement, ctx: JsonDeserializationContext): List<ConditionDTO> {
        if (!el.isJsonArray) return emptyList()
        return el.asJsonArray.map { ctx.deserialize(it, ConditionDTO::class.java) }
    }

    private fun parseString(raw: String): ConditionDTO {
        val s = raw.trim()
        if (s.equals("op", ignoreCase = true)) return ConditionDTO(type = "op")
        val idx = s.indexOf(':')
        if (idx < 0) return ConditionDTO(type = s.lowercase())
        val prefix = s.substring(0, idx).trim().lowercase()
        val rest = s.substring(idx + 1).trim()
        return ConditionDTO(type = prefix, value = rest)
    }

}
