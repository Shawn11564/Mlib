package dev.mrshawn.mlib.sounds

import org.bukkit.Sound
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player

class PlaySound(
	private val sound: Sound
) {

	fun play(player: Player) {
		SoundUtils.playSound(player, sound)
	}

	fun play(player: HumanEntity) {
		SoundUtils.playSound(player, sound)
	}

}