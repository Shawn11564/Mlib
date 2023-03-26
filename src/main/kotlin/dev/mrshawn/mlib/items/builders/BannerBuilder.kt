package dev.mrshawn.mlib.items.builders

import dev.mrshawn.mlib.items.BannerType
import org.bukkit.DyeColor
import org.bukkit.block.banner.Pattern
import org.bukkit.block.banner.PatternType
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BannerMeta

class BannerBuilder(
	bannerType: BannerType
): ItemBuilder(bannerType.banner) {

	fun addPattern(color: DyeColor, pattern: PatternType): BannerBuilder {
		(meta as BannerMeta).addPattern(Pattern(color, pattern))
		return this
	}

	override fun build(): ItemStack {
		addItemFlag(ItemFlag.HIDE_ATTRIBUTES)
		return super.build()
	}

}