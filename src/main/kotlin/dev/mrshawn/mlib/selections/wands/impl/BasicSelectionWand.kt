package dev.mrshawn.mlib.selections.wands.impl

import dev.mrshawn.mlib.items.builders.ItemBuilder
import dev.mrshawn.mlib.selections.Selection
import dev.mrshawn.mlib.selections.wands.SelectionWand
import org.bukkit.Material

object BasicSelectionWand: SelectionWand(
	ItemBuilder(Material.DIAMOND_AXE)
		.name("&bRegion Selection Wand")
		.addLoreLine("&7Left-click to select corner 1")
		.addLoreLine("&7Right-click to select corner 2")
		.build()
) {

	init { Selection.registerWand(this) }

}