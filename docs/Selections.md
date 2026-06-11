# Mlib Regions & Selections

Mlib bundles a **WorldEdit-style selection system** plus a lightweight **region**
primitive. Give players a "wand" item, let them click two corners, and turn the
result into a serializable cuboid you can test points against.

| Piece | Package | Purpose |
|-------|---------|---------|
| **Selection** | `selections` | Per-player two-corner selection state + the wand click listener |
| **SelectionWand** | `selections.wands` | A wand item + its click behavior |
| **BasicSelectionWand** | `selections.wands.impl` | Ready-made diamond-axe wand (L-click = corner 1, R-click = corner 2) |
| **Region** | `regions` | A serializable cuboid between two corners with containment/overlap math |

**Why it's a standout:** wands are item-driven and self-listening — register one
listener, hand out the wand item, and corner selection "just works" with no
per-plugin event boilerplate.

---

## 1. Enable selections

`Selection` registers a single global interact-listener. Call this once on enable:

```kotlin
Selection.register(plugin)   // idempotent — safe to call once
```

Then give the player the built-in wand and they can start selecting:

```kotlin
player.inventory.addItem(Selection.BASIC_SELECTION_WAND.getWandItem())
```

The `BasicSelectionWand` is a named **diamond axe**:

- **Left-click a block** → corner one ("&aCorner one set!")
- **Right-click a block** → corner two ("&aCorner two set!")

Selection state is tracked per player UUID and created lazily:

```kotlin
val sel = Selection.get(player)
if (sel.bothSet()) {
    val region = Region(sel.cornerOne!!, sel.cornerTwo!!)
    // save / use the region
}
```

---

## 2. Custom wands

Make your own wand by subclassing `SelectionWand` with your item and (optionally)
your own click handler. If you omit the handler you inherit the default
left/right-corner behavior.

```kotlin
object MyWand : SelectionWand(
    ItemBuilder(Material.GOLDEN_HOE)
        .name("&eMy Selection Wand")
        .addLoreLine("&7Left/right-click to pick corners")
        .build()
) {
    init { Selection.registerWand(this) }   // make the listener aware of it
}
```

Custom click logic — e.g. only allow selection in certain worlds:

```kotlin
object ArenaWand : SelectionWand(
    ItemBuilder(Material.BLAZE_ROD).name("&cArena Wand").build(),
    onClick = { event ->
        if (event.player.world.name != "arena_world") return@SelectionWand
        when (event.action) {
            Action.LEFT_CLICK_BLOCK  -> Selection.get(event.player).cornerOne = event.clickedBlock!!.location
            Action.RIGHT_CLICK_BLOCK -> Selection.get(event.player).cornerTwo = event.clickedBlock!!.location
            else -> {}
        }
        event.isCancelled = true
    }
) {
    init { Selection.registerWand(this) }
}
```

Helpers on every wand:

- `getWandItem()` — the `ItemStack` to hand out (matched with `isSimilar`)
- `removeFromInventory(player)` — strip the wand back out of a player's inventory
- `Selection.isWand(item)` / `Selection.getWand(item)` — identify a wand from a stack

The listener resolves a clicked item to its wand by `isSimilar`, ignores
off-hand clicks on 1.9+, and dispatches to that wand's handler.

---

## 3. Regions

A `Region` is a cuboid defined by two corners. It normalizes them into
`min`/`max` bounds on construction and recalculates whenever a corner changes:

```kotlin
val region = Region(cornerOne, cornerTwo)

region.contains(player.location)   // is a point inside? (same-world + within bounds)
region.getCenter()                 // center Location
region.overlaps(otherRegion)       // do two cuboids intersect on x/y/z?
region.minX; region.maxY           // exposed normalized bounds
```

`contains` snaps to the block location and checks world equality, so it's safe for
"is the player standing in this zone?" checks every tick.

### Persisting regions

`Region` is `ConfigurationSerializable` — it serializes both corner locations, so
you can store it straight in YAML:

```kotlin
config.set("regions.spawn", region)
val spawn = config.get("regions.spawn") as Region
```

### Extending regions

`Region` is `open`, and `getCenter`, `contains`, and `overlaps` are overridable —
subclass it for non-cuboid shapes (spheres, weighted zones) while reusing the
corner/serialization plumbing.

---

## 4. End-to-end: claim a region with a wand

```kotlin
// on enable
Selection.register(plugin)

// give the wand
player.inventory.addItem(Selection.BASIC_SELECTION_WAND.getWandItem())

// in a /claim command
val sel = Selection.get(player)
if (!sel.bothSet()) {
    Chat.tell(player, "&cSelect both corners first!")
    return
}
val claim = Region(sel.cornerOne!!, sel.cornerTwo!!)
if (claims.any { it.overlaps(claim) }) {
    Chat.tell(player, "&cThat overlaps an existing claim.")
    return
}
claims.add(claim)
config.set("claims.${player.uniqueId}", claim)
Chat.tell(player, "&aClaim created!")
```
