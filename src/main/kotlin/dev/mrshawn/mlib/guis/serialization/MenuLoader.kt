package dev.mrshawn.mlib.guis.serialization

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import dev.mrshawn.mlib.chat.Chat
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

/**
 * Public entry point for the declarative menu format.
 *
 * ```kotlin
 * val project = MenuLoader.load(plugin, "menus/main.yml")
 * project.open("main_menu", player)
 * ```
 *
 * Reads YAML (`.yml`/`.yaml`) and JSON (`.json`), parsing both into the same [MenuProjectDTO] via
 * Gson. A document may contain many menus (a "project") or a single bare menu (normalized to a
 * one-entry project keyed `main`). Throws [MenuParseException] only for whole-document failures;
 * field-level problems are logged and degraded.
 */
object MenuLoader {

    enum class Format { JSON, YAML }

    /** Loads from a file under [path] (defaults to the plugin's data folder). */
    fun load(plugin: JavaPlugin, fileName: String, path: String = plugin.dataFolder.path): LoadedProject =
        load(plugin, File(path, fileName))

    fun load(plugin: JavaPlugin, file: File): LoadedProject {
        if (!file.exists()) throw MenuParseException("Menu file not found: ${file.path}")
        val format = if (file.extension.lowercase() in setOf("yml", "yaml")) Format.YAML else Format.JSON
        return loadFromString(plugin, file.readText(), format)
    }

    /** Parses [content] directly. Handy for tests and for embedding menus in other configs. */
    fun loadFromString(plugin: JavaPlugin, content: String, format: Format): LoadedProject {
        val project = parse(content, format)

        val version = project.formatVersion ?: FORMAT_VERSION
        if (version > FORMAT_VERSION) {
            throw MenuParseException("Unsupported menu formatVersion $version (this mlib supports up to $FORMAT_VERSION)")
        }

        val menus = project.menus.orEmpty()
        if (menus.isEmpty()) throw MenuParseException("Menu document has no 'menus'")

        val registry = MenuRegistry()
        val ctx = MenuBuildContext(plugin, registry)

        // Pass 1: instantiate every menu so references resolve regardless of declaration order.
        for ((id, dto) in menus) registry.register(id, DataMenu(plugin, id, dto, ctx))

        // Pass 2: link previous/next navigation.
        for ((id, dto) in menus) {
            val menu = registry.get(id) ?: continue
            val previous = dto.previousMenu?.let { resolveRef(id, "previousMenu", it, registry) }
            val next = dto.nextMenu?.let { resolveRef(id, "nextMenu", it, registry) }
            menu.linkNavigation(previous, next)
        }

        return LoadedProject(project.project, registry)
    }

    private fun resolveRef(menuId: String, field: String, ref: String, registry: MenuRegistry): DataMenu? {
        val target = registry.get(ref)
        if (target == null) Chat.warn("[mlib] menu '$menuId' $field references unknown menu '$ref'")
        return target
    }

    private fun parse(content: String, format: Format): MenuProjectDTO {
        val tree: JsonElement = when (format) {
            Format.JSON -> runCatching { JsonParser.parseString(content) }
                .getOrElse { throw MenuParseException("Invalid JSON: ${it.message}", it) }

            Format.YAML -> {
                val config = YamlConfiguration()
                runCatching { config.loadFromString(content) }
                    .getOrElse { throw MenuParseException("Invalid YAML: ${it.message}", it) }
                YamlToJson.toJsonTree(config)
            }
        }

        if (!tree.isJsonObject) throw MenuParseException("Menu document root must be an object")
        val obj = tree.asJsonObject

        // Bare single-menu support: wrap a root that looks like a menu into a one-entry project.
        val dto = if (obj.has("menus")) {
            MenuGson.GSON.fromJson(obj, MenuProjectDTO::class.java)
        } else {
            val menu = MenuGson.GSON.fromJson(obj, MenuDTO::class.java)
            val version = obj.get("formatVersion")?.takeIf { it.isJsonPrimitive }?.asInt
            MenuProjectDTO(formatVersion = version, project = null, menus = mapOf("main" to menu))
        }

        return dto ?: throw MenuParseException("Failed to parse menu document")
    }

}

/**
 * The result of loading a menu document: a set of openable [DataMenu]s keyed by id.
 */
class LoadedProject internal constructor(val name: String?, private val registry: MenuRegistry) {

    val menus: Map<String, DataMenu> get() = registry.all()

    fun menu(id: String): DataMenu? = registry.get(id)

    fun firstMenu(): DataMenu? = registry.first()

    /** Resolves [menuId] and opens it for [player]; logs a warning if the id is unknown. */
    fun open(menuId: String, player: Player) {
        val menu = registry.get(menuId)
        if (menu == null) {
            Chat.warn("[mlib] cannot open unknown menu '$menuId'")
            return
        }
        menu.open(player)
    }

}
