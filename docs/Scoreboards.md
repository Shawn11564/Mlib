# Mlib Scoreboards

Mlib's scoreboard system lets you drive sidebar scoreboards by **line index**
instead of fighting Bukkit's `Objective`/`Score`/`Team` API. There are two
implementations behind a shared `KBoard` interface:

| Type | Scope | Use when |
|------|-------|----------|
| **KPlayerBoard** | one player | each player needs their own values (coins, stats, position) |
| **KGlobalBoard** | many players | everyone sees the same board (event timer, server info) |

Both colorize lines with `&` codes and address lines by an integer **score
index** — higher index renders higher on the sidebar.

---

## 1. Per-player boards

```kotlin
val board = KPlayerBoard(player, displayName = "&6&lMY SERVER")

board.setAll(
    "&7Welcome back,",
    "&f${player.name}",
    "",
    "&eCoins: &f${coins}",
)
board.setVisible(true)     // shows it in the sidebar
```

`setAll(vararg)` clears the board and lays the lines out top-to-bottom (index 0 is
the bottom score). To address lines individually:

```kotlin
board.set(3, "&eCoins: &f${coins}")  // update a single line in place
board.addEmpty(2)                    // blank spacer line at index 2
board.remove(1)                      // drop a line
board.setName("&c&lNEW TITLE")       // change the sidebar title live
board.clear()                        // wipe all lines (board stays registered)
board.delete()                       // hide + unregister the objective entirely
```

Toggle visibility without losing the lines:

```kotlin
board.setVisible(false)   // restores the player's default (blank) scoreboard
board.setVisible(true)    // re-displays this board
```

Query state with `isVisible()` and `getLine(index)`.

---

## 2. Global boards (shared content, per-player rendering)

`KGlobalBoard` holds one set of lines and fans them out to a set of viewers. Under
the hood each viewer gets their own `KPlayerBoard`, so it renders correctly for
everyone while you mutate a single object.

```kotlin
val event = KGlobalBoard("&b&lEVENT")
event.setAll(
    "&7Time left:",
    "&f05:00",
    "",
    "&7Players: &f${online}",
)

// manage the audience
event.addViewer(player)
event.removeViewer(player)

// update once -> every viewer updates
event.set(1, "&f04:59")
event.setName("&b&lFINAL ROUND")

event.setVisible(true)
```

Adding a viewer immediately gives them a board matching the current title;
`removeViewer` cleans up their objective; `delete()` tears down every viewer's
board and clears the audience.

A typical countdown loop:

```kotlin
object : BukkitRunnable() {
    var seconds = 300
    override fun run() {
        if (seconds-- <= 0) { event.delete(); cancel(); return }
        event.set(1, "&f${formatTime(seconds)}")
    }
}.runTaskTimer(plugin, 0L, 20L)
```

---

## 3. The `KBoard` contract

Both types implement the same interface, so you can write display code against
`KBoard` and swap per-player vs. global without changes:

```kotlin
fun render(board: KBoard, coins: Int) {
    board.setAll("&eCoins: &f$coins")
    board.setVisible(true)
}
```

Interface members: `set`, `setAll`, `addEmpty`, `remove`, `getLine`, `clear`,
`setName`, `setVisible`, `isVisible`, `delete`.

---

## 4. Notes

- **Line index = score**, so larger indices sit higher on the sidebar. Lay out
  `setAll(...)` with that in mind (it assigns ascending indices in argument order).
- Lines are colorized with `&` codes automatically.
- Duplicate line *text* across indices can collide on the underlying scoreboard
  (Bukkit keys scores by string) — vary lines with color codes or spacers if you
  need two visually identical rows.
- Always `delete()` a board you're done with (e.g. on quit/round end) to unregister
  its objective.
