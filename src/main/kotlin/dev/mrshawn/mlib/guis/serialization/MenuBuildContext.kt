package dev.mrshawn.mlib.guis.serialization

import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

/**
 * Shared state threaded through menu construction and click handling for one loaded project.
 *
 * [currentViewer] is set whenever a menu is opened ([DataMenu.open] / `OPEN_MENU`) so that item
 * names, lore, and `updating` items can resolve PlaceholderAPI placeholders for the viewing player.
 * Because a [DataMenu] instance is shared across viewers, this is a best-effort "last opener" value
 * (a documented limitation of rendering against a single shared inventory).
 */
class MenuBuildContext(
    val plugin: JavaPlugin,
    val registry: MenuRegistry
) {

    @Volatile
    var currentViewer: Player? = null

}
