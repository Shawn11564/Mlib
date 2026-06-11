package dev.mrshawn.mlib.guis.serialization

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Predicate

/**
 * The bridge between the declarative menu format and your plugin code.
 *
 * The format can only *name* custom behavior (`{ type: CUSTOM, id: "openShop" }`,
 * `"custom:hasVip"`); you decide what those names do by registering handlers here, once, at
 * startup (e.g. in an `MPlugin`'s `initObjects()`):
 *
 * ```kotlin
 * MenuHooks.registerAction("openShop") { ctx -> ShopMenu(plugin).show(ctx.player) }
 * MenuHooks.registerCondition("hasVip") { it.hasPermission("rank.vip") }
 * MenuHooks.registerItem("balanceHead") { player -> /* build a live ItemStack */ }
 * ```
 *
 * Ids are matched case-insensitively. Unknown ids are not fatal: the loader logs a warning and the
 * action/condition no-ops so the menu still works.
 */
object MenuHooks {

    private val actions = HashMap<String, Consumer<ActionContext>>()
    private val conditions = HashMap<String, Predicate<Player>>()
    private val items = HashMap<String, Function<Player, ItemStack>>()

    /** Registers a custom action handler invoked by `{ type: CUSTOM, id: "<id>" }`. */
    @JvmStatic
    fun registerAction(id: String, handler: Consumer<ActionContext>) {
        actions[id.lowercase()] = handler
    }

    /** Registers a custom condition predicate referenced by `"custom:<id>"` requirements. */
    @JvmStatic
    fun registerCondition(id: String, predicate: Predicate<Player>) {
        conditions[id.lowercase()] = predicate
    }

    /** Registers a code-provided dynamic icon, usable by an item appearance `material: "hook:<id>"`. */
    @JvmStatic
    fun registerItem(id: String, provider: Function<Player, ItemStack>) {
        items[id.lowercase()] = provider
    }

    @JvmStatic fun unregisterAction(id: String) { actions.remove(id.lowercase()) }
    @JvmStatic fun unregisterCondition(id: String) { conditions.remove(id.lowercase()) }
    @JvmStatic fun unregisterItem(id: String) { items.remove(id.lowercase()) }

    /** Clears every registered hook. Mainly useful for tests / plugin reloads. */
    @JvmStatic
    fun clear() {
        actions.clear()
        conditions.clear()
        items.clear()
    }

    internal fun getAction(id: String): Consumer<ActionContext>? = actions[id.lowercase()]
    internal fun getCondition(id: String): Predicate<Player>? = conditions[id.lowercase()]
    internal fun getItem(id: String): Function<Player, ItemStack>? = items[id.lowercase()]

}
