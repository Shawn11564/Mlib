package dev.mrshawn.mlib.guis

import org.bukkit.event.inventory.ClickType
import java.util.*

/**
 * Stops a double-click from running a GUI button three times.
 *
 * A vanilla client (AbstractContainerScreen) sends, for one double-click on a slot:
 *  1. press 1: a PICKUP (Bukkit [ClickType.LEFT]);
 *  2. press 2: another PICKUP, but only if the server's resync has already emptied the client's
 *     cursor (the server cancelled the first pickup) — i.e. on any low-latency connection;
 *  3. release 2: a PICKUP_ALL ([ClickType.DOUBLE_CLICK]), always.
 *
 * The player clicked twice. On a slow connection that arrives as LEFT + DOUBLE_CLICK, so the
 * DOUBLE_CLICK *is* the second click; on a fast one it arrives as LEFT + LEFT + DOUBLE_CLICK, and the
 * DOUBLE_CLICK is an extra. This filter drops the DOUBLE_CLICK only in the second case: when the
 * player's last two clicks were LEFTs on that same slot of that same inventory, close enough together
 * ([PAIR_WINDOW_MS]) to be the two presses of this double-click.
 *
 * Several [Gui]s can see the same event (a shared item, e.g. a filler, lives in more than one), so a
 * repeated call for the same event returns the first decision without counting it again.
 *
 * Regression checks: `DoubleClickFilterTest` (the packet sequences), and on a real Paper server
 * OreRegenerator's `src/mctest/oreregen-gui-double-click.mctest.yml` (`./gradlew guiMcTest`).
 */
object DoubleClickFilter {

	/** The two presses of a double-click are at most 250 ms apart on the client; this leaves room for jitter. */
	const val PAIR_WINDOW_MS = 500L

	private class Last(
		val event: Any,
		val handled: Boolean,
		val inventory: Any,
		val slot: Int,
		val type: ClickType,
		val time: Long,
		/** When this was a LEFT following a LEFT on the same slot: that earlier LEFT's time. */
		val previousLeftTime: Long?
	)

	private val last = HashMap<UUID, Last>()

	/**
	 * Whether [event] (a click by [player] on [slot] of [inventory]) should run the button's handler.
	 * A false answer means the caller should just cancel the event.
	 */
	@JvmStatic
	@JvmOverloads
	fun shouldHandle(
		player: UUID,
		event: Any,
		inventory: Any,
		slot: Int,
		type: ClickType,
		now: Long = System.currentTimeMillis()
	): Boolean {
		val prev = last[player]
		if (prev != null && prev.event === event) return prev.handled

		val sameSpot = prev != null && prev.inventory === inventory && prev.slot == slot
		val handled = when (type) {
			ClickType.DOUBLE_CLICK -> !(sameSpot && prev!!.type == ClickType.LEFT && prev.previousLeftTime != null &&
				prev.time - prev.previousLeftTime <= PAIR_WINDOW_MS)
			else -> true
		}
		val previousLeftTime = if (type == ClickType.LEFT && sameSpot && prev!!.type == ClickType.LEFT) prev.time else null
		last[player] = Last(event, handled, inventory, slot, type, now, previousLeftTime)
		return handled
	}

	/** Forgets [player] (on quit), so the map doesn't hold their last inventory. */
	@JvmStatic
	fun forget(player: UUID) {
		last.remove(player)
	}
}
