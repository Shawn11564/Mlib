package dev.mrshawn.mlib.items.builders

import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.potion.PotionEffectType

class PotionBuilder: ItemBuilder(Material.POTION) {

	fun setPotionColor(color: Color): PotionBuilder {
		(meta as PotionMeta).color = color
		return this
	}

	fun addPotionEffect(effect: PotionEffectType, duration: Int, amplifier: Int): PotionBuilder {
		(meta as PotionMeta).addCustomEffect(effect.createEffect(duration, amplifier), true)
		return this
	}

}