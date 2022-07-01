package dev.mrshawn.mlib.sounds

import org.bukkit.Sound
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player

object SoundUtils {

	fun playSound(player: HumanEntity, sound: Sound) {
		if (player is Player) {
			playSound(player as Player, sound)
		}
	}

	fun playSound(player: Player, sound: Sound) {
		player.playSound(player.location, sound, 1f, 1f)
	}

	fun isSound(sound: String): Boolean {
		return Sound.values().map { it.name }.contains(sound.uppercase())
	}

	fun getSound(sound: String): Sound {
		return Sound.valueOf(sound.uppercase())
	}

}