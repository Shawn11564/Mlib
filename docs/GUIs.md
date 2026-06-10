# Mlib GUI / Menu Guide

This guide explains how to build inventory-based GUIs (menus) with Mlib. The system
is split into four layers that build on one another:

| Layer | Package | Purpose |
|-------|---------|---------|
| **Gui** | `guis` | The raw inventory container + click/close event handling |
| **ChestGui** | `guis.types` | A concrete chest-style `Gui` (1–6 rows) |
| **GuiItem** | `guis.items` | Clickable items placed in a `Gui` |
| **Pane** | `guis.panes` | Grouped item regions (static grids, paginated lists) |
| **Menu** | `guis.menus` | Optional high-level wrapper that adds back/next navigation & rebuilding |

The fastest path: create a `ChestGui`, add `GuiItem`s with `(x, y)` coordinates,
and call `open(player)`. Everything else (panes, menus, special item types) is
optional sugar on top of that.

---

## 1. Coordinates

All positioning uses **`x` (column 0–8)** and **`y` (row, starting at 0)**.
Internally the slot index is `x + y * 9`, so a chest is always 9 columns wide.

```
        x=0  x=1  x=2  ...  x=8
 y=0     0    1    2   ...   8
 y=1     9   10   11   ...  17
 y=2    18   19   20   ...  26
```

---

## 2. Creating a GUI

`ChestGui` is the ready-to-use implementation. It needs your plugin instance, a
title, and an optional row count (defaults to 6).

```kotlin
val gui = ChestGui(
    plugin = this,            // your JavaPlugin
    name = "&8My Menu",       // color codes supported (& codes)
    rows = 3                  // 1–6 rows (9–54 slots)
)

gui.open(player)
```

The title is automatically run through `Chat.colorize`, so `&`-style color codes
work out of the box.

### GUI lifecycle

A `Gui` registers itself as a Bukkit `Listener` the moment it's constructed, and by
default **destroys itself when closed** (`destroyOnClose = true`), unregistering its
listeners. Override the protected hooks if you need lifecycle callbacks:

```kotlin
class MyGui(plugin: JavaPlugin) : Gui(plugin) {
    override val inventory = Bukkit.createInventory(null, 27, "Custom")

    override fun onCreate() { /* called once on construction */ }
    override fun onClose(player: Player) { /* player closed it */ }
    override fun onDestroy() { /* listeners about to be unregistered */ }
}
```

If you need a GUI to survive being closed (e.g. you reopen it later), set
`doNotDestroy = true`. The `Menu.close(..., doNotDestroy = true)` wrapper does this
for you.

---

## 3. GuiItems

A `GuiItem` is an `ItemStack` plus a click handler. Each item is tagged with a
hidden UUID (persistent data) so clicks can be routed back to the right handler.

Click handlers are `Consumer<InventoryClickEvent>`. **The default handler simply
cancels the click** (`it.isCancelled = true`) so players can't take items out — if
you write your own handler, remember to cancel the event yourself when you don't
want the item to move.

### BasicItem — a plain clickable button

```kotlin
val button = BasicItem(
    ItemBuilder(Material.DIAMOND)
        .name("&bClick me")
        .addLoreLine("&7A simple button")
        .build()
) { event ->
    event.isCancelled = true
    event.whoClicked.sendMessage("You clicked it!")
}

gui.addItem(button, x = 4, y = 1)
gui.update()   // push items into the inventory
```

> **Important:** `addItem` stores the item but only writes it into the live
> inventory when `update = true` is passed, or when you later call `gui.update()`.
> A common pattern is to add everything, then call `update()` once.

### GlowingItem — conditional enchant glow

Wraps a `BasicItem` and adds an enchant glow when a condition is true.

```kotlin
GlowingItem(
    ItemBuilder(Material.EMERALD).name("&aSelected").build(),
    glowIf = { player.isSelected() }   // re-evaluated each render
)
```

### UpdatingItem — rebuilt on every render

The item stack is produced by a lambda that runs every time the item is displayed,
so it always reflects current state. Great for live counters, balances, toggles
driven by external data.

```kotlin
UpdatingItem(
    itemBuilder = {
        ItemBuilder(Material.CLOCK)
            .name("&eOnline: ${Bukkit.getOnlinePlayers().size}")
            .build()
    }
)
```

Call `gui.update()` (or `gui.update(x, y)`) to refresh what the player sees.

### ToggleItem — two-state switch

Swaps between an "on" item and an "off" item. By default it flips state on every
click (`toggleOnClick = true`). Each underlying item keeps its own click handler.

```kotlin
val toggle = ToggleItem(
    toggledOnItem  = BasicItem(ItemBuilder(Material.LIME_DYE).name("&aON").build()) {
        it.isCancelled = true; enableFeature()
    },
    toggledOffItem = BasicItem(ItemBuilder(Material.GRAY_DYE).name("&7OFF").build()) {
        it.isCancelled = true; disableFeature()
    },
    initiallyToggledOn = false
)

// query state later:
if (toggle.isToggled()) { ... }
```

### CycleItem — rotate through N items

Cycles through any number of items, advancing on click by default.

```kotlin
val difficulty = CycleItem(
    BasicItem(ItemBuilder(Material.GREEN_WOOL).name("&aEasy").build())  { it.isCancelled = true },
    BasicItem(ItemBuilder(Material.YELLOW_WOOL).name("&eMedium").build()){ it.isCancelled = true },
    BasicItem(ItemBuilder(Material.RED_WOOL).name("&cHard").build())    { it.isCancelled = true }
)
// also accepts a Collection<GuiItem>
```

### TwoStageItem — click-to-confirm

First click "arms" the item (optionally glows); the second click fires your
handler. Perfect for destructive confirmations.

```kotlin
TwoStageItem(
    item = ItemBuilder(Material.TNT).name("&cClick twice to delete").build(),
    onSecondClick = { it.isCancelled = true; deleteEverything() },
    glowOnFirstClick = true
)
```

---

## 4. Placing items in a Gui

```kotlin
// single item
gui.addItem(item, x, y, update = false)

// fill the entire inventory
gui.fillWith(item, update = true)

// fill a rectangular region (x, y = top-left; width × height)
gui.fillArea(Gui.FILLER_ITEM, x = 0, y = 0, width = 9, height = 1)

// remove / query
gui.removeItem(x, y)
val stack: ItemStack? = gui.getInventoryItem(x, y)

// refresh
gui.update()          // clears + redraws all items and panes
gui.update(x, y)      // refresh a single slot
gui.update(clear = false)  // redraw without clearing first
gui.clear()           // remove all items + panes, then update
```

`Gui.FILLER_ITEM` is a shared, nameless black stained-glass-pane `BasicItem` handy
for backgrounds.

---

## 5. Panes

Panes are reusable, prioritized regions of items. They render *on top of* the
GUI's loose items, and higher-priority panes render over lower-priority ones.

Constructor args are shared by all panes: `x, y` (top-left), `width, height`,
`clearOld` (clear the region before drawing, default `true`), and `priority`
(`HIGHEST → LOWEST`, default `NORMAL`).

```kotlin
gui.addPane(pane)
gui.removePane(pane)
```

> **Note on `width`/`height`:** in the current implementation the pane fill/render
> loops iterate `x .. width` and `y .. height` inclusively (not `x until x+width`),
> so treat `width`/`height` as the **last column/row index** the pane covers rather
> than a pure size. Verify your bounds when laying out panes.

### StaticPane — a fixed grid

```kotlin
val pane = StaticPane(x = 1, y = 1, width = 7, height = 3)

pane.addItem(myItem, x = 1, y = 1)
pane.removeItem(x = 1, y = 1)

// or auto-fill from a collection, left-to-right, top-to-bottom
pane.fillWith(listOf(item1, item2, item3))

gui.addPane(pane)
gui.update()
```

### PaginatedPane — multi-page lists

Splits a collection across pages sized `width * height`, and renders one page at a
time.

```kotlin
val pane = PaginatedPane(x = 0, y = 1, width = 8, height = 4)
pane.fillWith(allItems)        // auto-chunked into pages
gui.addPane(pane)

// navigation (wire these to buttons)
pane.increment()   // next page (clamped to last)
pane.decrement()   // previous page (clamped to 0)

pane.pageCount()   // total pages

// a ready-made "x / y" page indicator item (PAPER)
gui.addItem(pane.getPageDisplayItem(), x = 4, y = 5)
```

A typical next/prev button looks like:

```kotlin
val next = BasicItem(ItemBuilder(Material.ARROW).name("&aNext").build()) {
    it.isCancelled = true
    pane.increment()
    gui.update()
}
```

---

## 6. Menus (high-level wrapper)

`Menu` wraps a `Gui` and adds linked-list navigation (`previousMenu` / `nextMenu`)
plus automatic back/next buttons and a rebuild utility. Use it when you have a set
of related screens the player moves between.

Implement two members:

- `gui` — the backing `Gui` (usually a `ChestGui`)
- `createGui()` — populate `gui` with items; called each time the menu is shown

```kotlin
class MainMenu(private val plugin: JavaPlugin) : Menu() {

    override val gui = ChestGui(plugin, "&8Main Menu", rows = 3)

    override fun createGui() {
        gui.addItem(
            BasicItem(ItemBuilder(Material.COMPASS).name("&aSettings").build()) {
                it.isCancelled = true
                // open another menu, etc.
            },
            x = 4, y = 1
        )
        gui.update()
    }
}

// show it
MainMenu(plugin).show(player)
```

### Navigation

If you pass `previousMenu` / `nextMenu` to the `Menu` constructor, `show()`
automatically adds:

- a **red wool "Previous Menu"** button at slot `(0, 0)` when `previousMenu != null`
- a **lime wool "Next Menu"** button at slot `(8, 0)` when `nextMenu != null`

(both only on `ChestGui`-backed menus). You can also navigate programmatically:

```kotlin
menu.next(player)   // shows nextMenu
menu.back(player)   // shows previousMenu
```

### Showing / closing

```kotlin
menu.show(player)                       // clears, rebuilds, adds nav buttons, opens
menu.show(player,
    reCreateOnShow = true,
    clearOnShow = true)
menu.close(player)                      // closes; gui is destroyed by default
menu.close(player, doNotDestroy = true) // closes but keeps gui alive for reuse
```

### Rebuilding open menus

`Menu.rebuild(menuClass)` finds **static `Menu` fields** on a class, and for any
that are currently being viewed, clears + recreates + updates their GUI. Use this
to live-refresh a singleton menu when underlying data changes.

```kotlin
object Menus {
    @JvmField val shop = ShopMenu(plugin)   // static field
}

// later, when shop stock changes:
Menu.rebuild(Menus::class.java)
```

---

## 7. Click handling internals (good to know)

- The `Gui` listens to `InventoryClickEvent`. It looks up the clicked item's hidden
  UUID via `ItemUtils.getItemID`, finds the matching `GuiItem` (searching panes
  first, then loose items), and invokes its handler.
- If a `GuiItem` has `updateOnClick = true` (the default for most), the clicked slot
  is redrawn immediately after the handler runs — this is what makes `ToggleItem`,
  `CycleItem`, and `UpdatingItem` visually flip on click.
- Dropping an item (`ClickType.DROP`) onto the GUI triggers the `onItemPlace`
  handler, which by default cancels the action.
- **Always cancel the click event** in custom handlers unless you intentionally want
  the player to move the item.

---

## 8. Minimal end-to-end example

```kotlin
fun openExample(plugin: JavaPlugin, player: Player) {
    val gui = ChestGui(plugin, "&8Example", rows = 3)

    // border
    gui.fillArea(Gui.FILLER_ITEM, 0, 0, 9, 1)
    gui.fillArea(Gui.FILLER_ITEM, 0, 2, 9, 1)

    // a toggle in the middle
    gui.addItem(
        ToggleItem(
            BasicItem(ItemBuilder(Material.LIME_DYE).name("&aEnabled").build())  { it.isCancelled = true },
            BasicItem(ItemBuilder(Material.GRAY_DYE).name("&7Disabled").build()) { it.isCancelled = true }
        ),
        x = 4, y = 1
    )

    gui.update()
    gui.open(player)
}
```
