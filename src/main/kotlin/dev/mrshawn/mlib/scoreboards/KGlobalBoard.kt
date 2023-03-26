package dev.mrshawn.mlib.scoreboards

import org.bukkit.entity.Player

class KGlobalBoard(
	private var displayName: String = ""
): KBoard {

	private val lines = HashMap<Int, String>()
	private val viewers = HashMap<Player, KBoard>()

	private var visible = false

	fun addViewer(player: Player) {
		if (viewers.containsKey(player)) return
		viewers[player] = KPlayerBoard(player, displayName)
	}

	fun removeViewer(player: Player) {
		if (!viewers.containsKey(player)) return
		viewers[player]?.delete()
		viewers.remove(player)
	}

	override fun getLine(index: Int): String? {
		return lines[index]
	}

	override fun setVisible(visible: Boolean) {
		this.visible = visible
		viewers.values.forEach { it.setVisible(this.visible) }
	}

	override fun isVisible(): Boolean {
		return visible
	}

	override fun setName(name: String) {
		displayName = name
		viewers.values.forEach { it.setName(name) }
	}

	override fun set(index: Int, line: String) {
		lines[index] = line
		viewers.values.forEach { it.set(index, line) }
	}

	override fun setAll(vararg lines: String) {
		this.lines.clear()
		lines.forEachIndexed { index, line -> this.lines[index] = line }
		viewers.values.forEach { it.setAll(*lines) }
	}

	override fun addEmpty(index: Int) {
		lines[index] = ""
		viewers.values.forEach { it.addEmpty(index) }
	}

	override fun remove(index: Int) {
		lines.remove(index)
		viewers.values.forEach { it.remove(index) }
	}

	override fun clear() {
		lines.clear()
		viewers.values.forEach { it.clear() }
	}

	override fun delete() {
		lines.clear()
		viewers.values.forEach { it.delete() }
		viewers.clear()
	}

}