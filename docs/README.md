# Mlib Documentation

**Mlib** is a Kotlin utility library for Spigot/Bukkit plugins — the shared
foundation used across MrShawn's plugins. These guides cover its standout
features: the systems that save the most boilerplate compared to the raw Bukkit
API.

## Guides

| Guide | What it covers |
|-------|----------------|
| [Commands](Commands.md) | Annotation-driven command framework — self-registering commands (no `plugin.yml`), auto-parsed arguments, nested subcommands, tab-completion, and permission/sender preconditions. |
| [Chat & Messaging](Chat.md) | Color (`&` + hex), MiniMessage, titles/action bars, plugin-labelled logging, and PlaceholderAPI — delivered via Adventure (Paper-native when available). |
| [GUIs](GUIs.md) | Inventory menu system — `ChestGui`, clickable `GuiItem`s (toggle/cycle/two-stage/updating), static & paginated panes, and high-level `Menu` navigation. |
| [Item Builders](ItemBuilders.md) | Fluent `ItemStack` builders for general items, player heads, potions, and banners — with auto color codes, conditional lore, glow, and persistent data. |
| [Scoreboards](Scoreboards.md) | Index-addressed sidebar scoreboards — per-player (`KPlayerBoard`) and shared (`KGlobalBoard`) behind one `KBoard` interface. |
| [Scheduling & Tasks](Scheduling.md) | Wall-clock scheduling — run tasks at real calendar times in any time zone, with serializable schedules, plus `MTask`/`TaskChain` primitives. |
| [Regions & Selections](Selections.md) | WorldEdit-style selection wands and serializable cuboid `Region`s with containment/overlap math. |
| [Files & Config](Files.md) | YAML config via `KFile` and JSON + typed object mapping via `Kson`, with shared enum-driven paths (`IConfigList`). |
| [Configuration (`@Value`)](Configuration.md) | Spring-Boot-style `@Value` binding of typed properties to YAML paths, with live `reload()`. |
| [Cloneable](Cloneable.md) | Typed deep-copy contract — `clone()` for a fresh copy, `copyFrom()` for in-place refresh of shared instances. |

## Getting started

Most plugins extend **`MPlugin`** (package `plugins`), which wires up a
`MCommandManager`, registers your listeners, and gives you `initObjects()` /
`registerCommands()` hooks:

```kotlin
class MyPlugin : MPlugin() {
    override val listeners = arrayOf<Listener>(/* your listeners */)
    override fun initObjects() { /* touch kotlin `object`s that must init early */ }
    override fun registerCommands() {
        mcm.registerCommand(MyCommand())
    }
}
```

From there, reach for the guide matching what you're building.
