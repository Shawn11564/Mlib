package dev.mrshawn.mlib.guis.serialization

import dev.mrshawn.mlib.chat.Chat
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import java.util.function.Consumer

/**
 * Compiles an [ActionSetDTO] into the `Consumer<InventoryClickEvent>` that mlib's GuiItems expect.
 *
 * The compiled handler:
 *  1. cancels the event by default (matching mlib's `DEFAULT_ON_CLICK`),
 *  2. evaluates `requirements` and runs `denyActions` (or a default deny message) on failure,
 *  3. routes to the branch matching the click type, falling back to `default`,
 *  4. runs each step via [ActionExecutor].
 */
internal object ActionCompiler {

    fun compile(set: ActionSetDTO?, ctx: MenuBuildContext, owningMenu: DataMenu): Consumer<InventoryClickEvent> =
        Consumer { event ->
            event.isCancelled = true
            val player = event.whoClicked as? Player ?: return@Consumer
            if (set == null) return@Consumer

            if (!ConditionEvaluator.evaluateAll(set.requirements, player)) {
                runDeny(set, event, player, owningMenu, ctx)
                return@Consumer
            }

            val branch = branchFor(set, event.click) ?: set.default
            branch?.forEach { ActionExecutor.execute(it, event, player, owningMenu, ctx) }
        }

    private fun branchFor(set: ActionSetDTO, click: ClickType): List<ActionDTO>? = when (click) {
        ClickType.LEFT -> set.left
        ClickType.RIGHT -> set.right
        ClickType.SHIFT_LEFT -> set.shiftLeft
        ClickType.SHIFT_RIGHT -> set.shiftRight
        ClickType.MIDDLE -> set.middle
        ClickType.DROP, ClickType.CONTROL_DROP -> set.drop
        ClickType.DOUBLE_CLICK -> set.doubleClick
        else -> null
    }

    private fun runDeny(set: ActionSetDTO, event: InventoryClickEvent, player: Player, owningMenu: DataMenu, ctx: MenuBuildContext) {
        val deny = set.denyActions
        if (!deny.isNullOrEmpty()) {
            deny.forEach { ActionExecutor.execute(it, event, player, owningMenu, ctx) }
        } else {
            Chat.tell(player, "&cYou can't do that.")
        }
    }

}
