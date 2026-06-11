package dev.mrshawn.mlib.guis.serialization

import dev.mrshawn.mlib.guis.items.GuiItem
import dev.mrshawn.mlib.guis.items.impl.BasicItem
import dev.mrshawn.mlib.guis.items.impl.CycleItem
import dev.mrshawn.mlib.guis.items.impl.GlowingItem
import dev.mrshawn.mlib.guis.items.impl.ToggleItem
import dev.mrshawn.mlib.guis.items.impl.TwoStageItem
import dev.mrshawn.mlib.guis.items.impl.UpdatingItem
import dev.mrshawn.mlib.items.builders.ItemBuilder
import org.bukkit.Material

/**
 * Builds a [GuiItem] from an [ItemNodeDTO]. [kind] selects the implementation; nested kinds
 * (`toggle`, `cycle`) recurse. All click behavior is compiled by [ActionCompiler].
 */
internal object GuiItemFactory {

    fun create(node: ItemNodeDTO?, ctx: MenuBuildContext, owningMenu: DataMenu): GuiItem {
        if (node == null) return BasicItem(ItemBuilder(Material.BARRIER).name("&cmissing item").build())

        val onClick = ActionCompiler.compile(node.actions, ctx, owningMenu)

        return when ((node.kind ?: "basic").lowercase()) {
            "glowing" -> GlowingItem(
                ItemMapper.toItemStack(node.item, ctx.currentViewer),
                onClick,
                glowIf = { ConditionEvaluator.evaluate(node.glowCondition, ctx.currentViewer) }
            )

            "updating" -> UpdatingItem(
                itemBuilder = { ItemMapper.toItemStack(node.item, ctx.currentViewer) },
                onClick = onClick
            )

            "toggle" -> ToggleItem(
                create(node.on, ctx, owningMenu),
                create(node.off, ctx, owningMenu),
                node.initiallyToggledOn ?: true,
                node.toggleOnClick ?: true
            )

            "cycle" -> {
                val states = (node.states ?: emptyList()).map { create(it, ctx, owningMenu) }
                if (states.isEmpty()) BasicItem(ItemMapper.toItemStack(node.item, ctx.currentViewer), onClick)
                else CycleItem(states, node.cycleOnClick ?: true)
            }

            "twostage" -> TwoStageItem(
                ItemMapper.toItemStack(node.item, ctx.currentViewer),
                ActionCompiler.compile(node.secondClickActions, ctx, owningMenu),
                node.glowOnFirstClick ?: true
            )

            else -> BasicItem(ItemMapper.toItemStack(node.item, ctx.currentViewer), onClick)
        }
    }

}
