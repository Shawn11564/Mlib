package dev.mrshawn.mlib.guis.serialization

import dev.mrshawn.mlib.chat.Chat
import org.bukkit.entity.Player

/**
 * Evaluates [ConditionDTO]s (used for action `requirements`, `CONDITIONAL` branches, and the
 * `glowCondition` of glowing items) against a [Player].
 *
 * Placeholder comparisons require PlaceholderAPI; without it, [Chat.setPlaceholders] returns the
 * text unchanged, so such comparisons generally evaluate to `false` (documented limitation).
 */
internal object ConditionEvaluator {

    /** True when every condition passes (an empty/absent list is vacuously true). */
    fun evaluateAll(conditions: List<ConditionDTO>?, player: Player?): Boolean {
        if (conditions.isNullOrEmpty()) return true
        return conditions.all { evaluate(it, player) }
    }

    fun evaluate(condition: ConditionDTO?, player: Player?): Boolean {
        if (condition == null) return true
        if (player == null) return false
        return when (condition.type) {
            "" -> true
            "op" -> player.isOp
            "permission" -> condition.value?.let { player.hasPermission(it) } ?: false
            "gamemode" -> condition.value?.let { player.gameMode.name.equals(it.trim(), ignoreCase = true) } ?: false
            "world" -> condition.value?.let { player.world.name.equals(it.trim(), ignoreCase = true) } ?: false
            "custom" -> condition.value?.let { MenuHooks.getCondition(it)?.test(player) } ?: false
            "placeholder" -> evaluatePlaceholder(condition.value, player)
            "all" -> condition.children.all { evaluate(it, player) }
            "any" -> condition.children.any { evaluate(it, player) }
            "not" -> !evaluate(condition.child, player)
            else -> {
                Chat.warn("[mlib] unknown condition type '${condition.type}' -> treating as false")
                false
            }
        }
    }

    private val OPERATORS = listOf(">=", "<=", "==", "!=", ">", "<", "contains")

    private fun evaluatePlaceholder(expr: String?, player: Player): Boolean {
        if (expr.isNullOrBlank()) return false
        val op = OPERATORS.firstOrNull { expr.contains(it) }
            ?: run {
                // No operator: truthy if the resolved value is a non-empty, non-"false", non-"0" string.
                val v = Chat.setPlaceholders(player, expr.trim())
                return v.isNotBlank() && !v.equals("false", ignoreCase = true) && v != "0"
            }
        val idx = expr.indexOf(op)
        val left = Chat.setPlaceholders(player, expr.substring(0, idx).trim())
        val right = Chat.setPlaceholders(player, expr.substring(idx + op.length).trim())
        return when (op) {
            "==" -> left.equals(right, ignoreCase = true)
            "!=" -> !left.equals(right, ignoreCase = true)
            "contains" -> left.contains(right, ignoreCase = true)
            else -> {
                val l = left.toDoubleOrNull()
                val r = right.toDoubleOrNull()
                if (l == null || r == null) false
                else when (op) {
                    ">=" -> l >= r
                    "<=" -> l <= r
                    ">" -> l > r
                    "<" -> l < r
                    else -> false
                }
            }
        }
    }

}
