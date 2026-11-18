package dev.mrshawn.mlib.guis

import org.bukkit.event.inventory.ClickType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.*

/**
 * The click sequences a vanilla client sends (see [DoubleClickFilter]); each double-click must run a
 * button exactly twice, however the presses arrive.
 */
class DoubleClickFilterTest {

	private val player = UUID.randomUUID()
	private val inventory = Any()

	/** Feeds clicks as (type, slot, time) and returns how many were handled. */
	private fun handled(vararg clicks: Triple<ClickType, Int, Long>, inv: Any = inventory): Int =
		clicks.count { (type, slot, time) -> DoubleClickFilter.shouldHandle(player, Any(), inv, slot, type, time) }

	private fun left(time: Long, slot: Int = 10) = Triple(ClickType.LEFT, slot, time)
	private fun double(time: Long, slot: Int = 10) = Triple(ClickType.DOUBLE_CLICK, slot, time)

	@Test
	fun `a fast-link double-click (left, left, double) counts twice`() {
		DoubleClickFilter.forget(player)
		assertEquals(2, handled(left(0), left(150), double(160)))
	}

	@Test
	fun `a slow-link double-click (left, double) counts twice`() {
		DoubleClickFilter.forget(player)
		assertEquals(2, handled(left(0), double(160)))
	}

	@Test
	fun `two double-clicks in a row count four times`() {
		DoubleClickFilter.forget(player)
		assertEquals(4, handled(left(0), left(150), double(160), left(1000), left(1150), double(1160)))
	}

	@Test
	fun `a slow-link double-click after an earlier single click still counts twice`() {
		DoubleClickFilter.forget(player)
		assertEquals(3, handled(left(0), left(900), double(1060)))
	}

	@Test
	fun `single clicks always count`() {
		DoubleClickFilter.forget(player)
		assertEquals(3, handled(left(0), left(100), left(200)))
	}

	@Test
	fun `a double-click on another slot is not mistaken for a repeat`() {
		DoubleClickFilter.forget(player)
		assertEquals(3, handled(left(0, slot = 1), left(100, slot = 2), double(110, slot = 3)))
	}

	@Test
	fun `the same event seen by two guis is counted once and answered the same`() {
		DoubleClickFilter.forget(player)
		DoubleClickFilter.shouldHandle(player, Any(), inventory, 10, ClickType.LEFT, 0)
		DoubleClickFilter.shouldHandle(player, Any(), inventory, 10, ClickType.LEFT, 150)
		val event = Any()
		val first = DoubleClickFilter.shouldHandle(player, event, inventory, 10, ClickType.DOUBLE_CLICK, 160)
		val second = DoubleClickFilter.shouldHandle(player, event, inventory, 10, ClickType.DOUBLE_CLICK, 160)
		assertEquals(false, first)
		assertEquals(first, second)
		// ...and the next real click is unaffected.
		assertTrue(DoubleClickFilter.shouldHandle(player, Any(), inventory, 10, ClickType.LEFT, 400))
	}
}
