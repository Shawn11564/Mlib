package dev.mrshawn.mlib.guis.serialization

import dev.mrshawn.mlib.chat.Chat
import dev.mrshawn.mlib.sounds.SoundUtils
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent

/**
 * Executes a single [ActionDTO]. Every branch maps onto an existing mlib/Bukkit primitive, so the
 * action vocabulary needs no new dependencies. Unknown action types and unresolved references are
 * logged and skipped rather than thrown.
 */
internal object ActionExecutor {

    fun execute(action: ActionDTO, event: InventoryClickEvent, player: Player, owningMenu: DataMenu, ctx: MenuBuildContext) {
        when ((action.type ?: "").uppercase()) {
            "RUN_COMMAND" -> runCommand(action, player)
            "MESSAGE" -> message(action, player)
            "BROADCAST" -> broadcast(action)
            "ACTIONBAR" -> Chat.tellActionbar(player, action.text)
            "TITLE" -> Chat.sendTitle(player, action.title, action.subtitle, action.fadeIn ?: 10, action.stay ?: 70, action.fadeOut ?: 20)
            "PLAY_SOUND" -> playSound(action, player)
            "OPEN_MENU" -> openMenu(action, player, ctx)
            "CLOSE" -> player.closeInventory()
            "BACK" -> { ctx.currentViewer = player; owningMenu.back(player) }
            "NEXT" -> { ctx.currentViewer = player; owningMenu.next(player) }
            "GIVE_ITEM" -> giveItem(action, player)
            "CONSOLE_LOG" -> consoleLog(action)
            "CONDITIONAL" -> conditional(action, event, player, owningMenu, ctx)
            "CANCEL" -> event.isCancelled = true
            "ALLOW" -> event.isCancelled = false
            "CUSTOM" -> custom(action, event, player, owningMenu)
            "" -> Chat.warn("[mlib] menu action is missing a 'type'")
            else -> Chat.warn("[mlib] unknown menu action type '${action.type}'")
        }
    }

    private fun runCommand(action: ActionDTO, player: Player) {
        val raw = action.command ?: return
        val command = Chat.setPlaceholders(player, raw).removePrefix("/")
        when ((action.runAs ?: "PLAYER").uppercase()) {
            "CONSOLE" -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
            else -> Bukkit.dispatchCommand(player, command)
        }
    }

    private fun message(action: ActionDTO, player: Player) {
        val messages = action.lines ?: action.text?.let { listOf(it) } ?: return
        val mini = action.mini == true
        messages.forEach { if (mini) Chat.tellMini(player, it) else Chat.tell(player, it) }
    }

    private fun broadcast(action: ActionDTO) {
        val messages = action.lines ?: action.text?.let { listOf(it) } ?: return
        val permission = action.permission
        messages.forEach { if (permission != null) Chat.broadcastPermission(it, permission) else Chat.broadcast(it) }
    }

    private fun playSound(action: ActionDTO, player: Player) {
        val name = action.sound ?: return
        if (!SoundUtils.isSound(name)) {
            Chat.warn("[mlib] unknown sound '$name'")
            return
        }
        SoundUtils.playSound(player, SoundUtils.getSound(name), action.volume ?: 1f, action.pitch ?: 1f)
    }

    private fun openMenu(action: ActionDTO, player: Player, ctx: MenuBuildContext) {
        val id = action.menu ?: return
        val target = ctx.registry.get(id)
        if (target == null) {
            Chat.warn("[mlib] OPEN_MENU references unknown menu '$id'")
            return
        }
        target.open(player)
    }

    private fun giveItem(action: ActionDTO, player: Player) {
        val appearance = action.item ?: return
        val stack = ItemMapper.toItemStack(appearance, player)
        action.amount?.let { stack.amount = it.coerceIn(1, 64) }
        player.inventory.addItem(stack)
    }

    private fun consoleLog(action: ActionDTO) {
        val text = action.text ?: return
        when ((action.level ?: "INFO").uppercase()) {
            "WARN", "WARNING" -> Chat.warn(text)
            "SEVERE", "ERROR" -> Chat.severe(text)
            else -> Chat.log(text)
        }
    }

    private fun conditional(action: ActionDTO, event: InventoryClickEvent, player: Player, owningMenu: DataMenu, ctx: MenuBuildContext) {
        val branch = if (ConditionEvaluator.evaluate(action.condition, player)) action.thenActions else action.elseActions
        branch?.forEach { execute(it, event, player, owningMenu, ctx) }
    }

    private fun custom(action: ActionDTO, event: InventoryClickEvent, player: Player, owningMenu: DataMenu) {
        val id = action.id
        if (id == null) {
            Chat.warn("[mlib] CUSTOM action is missing an 'id'")
            return
        }
        val handler = MenuHooks.getAction(id)
        if (handler == null) {
            Chat.warn("[mlib] no custom action registered for id '$id' (MenuHooks.registerAction)")
            return
        }
        handler.accept(ActionContext(event, player, event.click, action.data ?: emptyMap(), owningMenu))
    }

}
