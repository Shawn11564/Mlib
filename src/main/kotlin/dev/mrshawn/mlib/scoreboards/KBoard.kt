package dev.mrshawn.mlib.scoreboards

interface KBoard {

	fun getLine(index: Int): String?

	fun setVisible(visible: Boolean)

	fun isVisible(): Boolean

	fun setName(name: String)

	fun set(index: Int, line: String)

	fun setAll(vararg lines: String)

	fun addEmpty(index: Int)

	fun remove(index: Int)

	fun clear()

	fun delete()

}