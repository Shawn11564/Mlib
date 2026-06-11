package dev.mrshawn.mlib.guis.panes

import dev.mrshawn.mlib.guis.items.GuiItem
import org.bukkit.inventory.Inventory
import java.util.*

abstract class Pane(
	protected val x: Int,
	protected val y: Int,
	protected val width: Int,
	protected val height: Int,
	protected val clearOld: Boolean = true,
	val priority: Priority = Priority.NORMAL
) {

	val uuid = UUID.randomUUID()
	protected val offset = x + (y * 9)

	abstract fun fillWith(items: Collection<GuiItem>)

	abstract fun render(inventory: Inventory)

	abstract fun getItem(id: String): GuiItem?

	class PaneComparator: Comparator<Pane> {
		override fun compare(paneOne: Pane, paneTwo: Pane): Int {
			// Sort by priority first; break ties on uuid so two panes with the same
			// priority are treated as distinct (a TreeSet drops elements that compare == 0).
			val priorityComparison = paneOne.priority.compareTo(paneTwo.priority)
			if (priorityComparison != 0) return priorityComparison
			return paneOne.uuid.compareTo(paneTwo.uuid)
		}
	}

	enum class Priority {
		HIGHEST,
		HIGH,
		NORMAL,
		LOW,
		LOWEST;
	}

}