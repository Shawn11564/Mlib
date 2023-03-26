package dev.mrshawn.mlib.scoreboards

import dev.mrshawn.mlib.chat.Chat
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scoreboard.DisplaySlot

class KPlayerBoard(
	private val player: Player,
	private var displayName: String = ""
): KBoard {

	private val scoreboard = Bukkit.getScoreboardManager()!!.newScoreboard
	private val objective = scoreboard.registerNewObjective(player.name.substring(0, 16), "dummy", Chat.colorize(displayName))
	private val lines = HashMap<Int, String>()

	private var visible = false

	override fun getLine(index: Int) = lines[index]

	override fun setVisible(visible: Boolean) {
		this.visible = visible
		objective.displaySlot = if (this.visible) DisplaySlot.SIDEBAR else null
		if (isVisible()) player.scoreboard = this.scoreboard else player.scoreboard = Bukkit.getScoreboardManager()!!.newScoreboard
	}

	override fun isVisible(): Boolean {
		return visible
	}

	override fun setName(name: String) {
		displayName = name
		objective.displayName = Chat.colorize(displayName)
	}

	override fun set(index: Int, line: String) {
		lines[index] = Chat.colorize(line)
		objective.getScore(Chat.colorize(line)).score = index
	}

	override fun setAll(vararg lines: String) {
		clear()
		lines.forEachIndexed { index, line ->
			set(index, line)
		}
	}

	override fun addEmpty(index: Int) {
		set(index, "")
	}

	override fun remove(index: Int) {
		val line = getLine(index) ?: return

		scoreboard.resetScores(line)
		lines.remove(index)
	}

	override fun clear() {
		for (index in lines.keys) remove(index)
		lines.clear()
	}

	override fun delete() {
		setVisible(false)
		objective.unregister()
	}

}