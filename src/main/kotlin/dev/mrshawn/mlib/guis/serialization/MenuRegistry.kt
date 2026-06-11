package dev.mrshawn.mlib.guis.serialization

/**
 * Holds the [DataMenu]s of a single loaded project, keyed by id (case-insensitive). Used to resolve
 * `OPEN_MENU` actions and `previousMenu`/`nextMenu` links. Built in two passes by [MenuLoader]:
 * instantiate every menu, then link navigation.
 */
class MenuRegistry {

    private val menus = LinkedHashMap<String, DataMenu>()

    fun register(id: String, menu: DataMenu) {
        menus[id.lowercase()] = menu
    }

    fun get(id: String): DataMenu? = menus[id.lowercase()]

    fun all(): Map<String, DataMenu> = menus

    fun first(): DataMenu? = menus.values.firstOrNull()

    fun ids(): Set<String> = menus.keys

}
