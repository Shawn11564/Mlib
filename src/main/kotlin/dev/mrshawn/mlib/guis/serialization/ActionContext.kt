package dev.mrshawn.mlib.guis.serialization

import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent

/**
 * Passed to handlers registered via [MenuHooks.registerAction]. Gives a custom action everything it
 * needs to do arbitrary work: the raw [event], the [player] who clicked, the [clickType], the
 * `data` map authored in the menu file, and the [menu] the item belongs to (handy for re-showing
 * or closing it).
 */
class ActionContext(
    val event: InventoryClickEvent,
    val player: Player,
    val clickType: ClickType,
    val data: Map<String, Any?>,
    val menu: DataMenu
) {

    /** Convenience: read a string from [data], or null. */
    fun string(key: String): String? = data[key]?.toString()

    /** Convenience: read an int from [data] (Gson numbers arrive as Double), or null. */
    fun int(key: String): Int? = (data[key] as? Number)?.toInt()

    /** Convenience: read a boolean from [data], or null. */
    fun bool(key: String): Boolean? = data[key] as? Boolean

}
