# Mlib Chat & Messaging

The `chat` package is the one place all player/console output flows through. It
handles color, modern text formatting, titles/action bars, logging, and optional
PlaceholderAPI — and it delivers everything through [Adventure](https://docs.papermc.io/adventure/),
using **Paper's native Adventure** when available and falling back to
`adventure-platform-bukkit` (and finally plain Bukkit) otherwise.

| Piece | Purpose |
|-------|---------|
| **Chat** | The entry point: formatting, sending, broadcasting, titles, logging, PAPI |
| **TextMessage** | A reusable multi-line message with deferred token replacement |
| **TextReplacement** | An ordered set of literal find/replace pairs |
| **ChatPlatform** | Delivery strategy — `PaperNativePlatform` or `BukkitAudiencesPlatform` |

---

## 1. Setup

If your plugin extends **`MPlugin`**, this is done for you (`Chat.init(this)` on
enable, `Chat.shutdown()` on disable). For a standalone `JavaPlugin`:

```kotlin
override fun onEnable() {
    Chat.init(this)        // selects a platform, adopts the plugin logger, detects PAPI
}

override fun onDisable() {
    Chat.shutdown()        // releases the platform
}
```

Until `init` is called, `Chat` still works — it just falls back to the plain
Bukkit API (no action bars/titles via Adventure).

### Platform selection

`init` takes an optional `Chat.Platform`:

```kotlin
Chat.init(this, Chat.Platform.AUTO)            // default: Paper-native if available, else BukkitAudiences
Chat.init(this, Chat.Platform.PAPER_NATIVE)    // force native (falls back if unsupported)
Chat.init(this, Chat.Platform.BUKKIT_AUDIENCES)// force the bridge (works everywhere)
```

| Platform | What it does |
|----------|--------------|
| `PaperNativePlatform` | Sends through the **server's own Adventure** on Paper/forks — no bridge layer |
| `BukkitAudiencesPlatform` | Uses `adventure-platform-bukkit`'s `BukkitAudiences` — works on Spigot too |

> **Why a Paper-specific path?** Mlib shades and **relocates** Adventure (to avoid
> clashing with the copy Paper already ships). That means our `Component` type isn't
> the same class as Paper's native one, so `PaperNativePlatform` bridges them by
> serializing our component to JSON and re-parsing it with the server's native
> serializer, then calling the native `Audience` methods. The upshot: on Paper you
> get native delivery; on Spigot you transparently get the bridge. `AUTO` picks the
> right one and falls back safely if native reflection ever fails.

---

## 2. Formatting

Three formats are supported. All of them resolve PlaceholderAPI placeholders when a
player recipient is known (see §6).

### Legacy `&` codes + hex

```kotlin
Chat.colorize("&aGreen and &#FF8800orange")   // -> Bukkit-ready String (hex on 1.16+)
```

`colorize` understands both `&a`-style codes and `&#RRGGBB` hex. It returns a
`String`, so it's what you use for item names, inventory titles, scoreboard lines —
anywhere Bukkit wants a string.

### MiniMessage

```kotlin
Chat.mini("<gradient:#FF0000:#00FF00>Rainbow</gradient> <bold>text</bold>")
```

[MiniMessage](https://docs.papermc.io/adventure/minimessage/) tags (`<red>`,
`<gradient>`, `<click>`, `<hover>`, …) are parsed into an Adventure `Component`.

### Components

`Chat.legacy(str)` and `Chat.mini(str)` both return a `Component` you can pass to
`Chat.send(...)` directly, or build/compose yourself with the Adventure API.

---

## 3. Sending to players

```kotlin
Chat.tell(player, "&aSaved!")                  // legacy + hex
Chat.tell(sender, "&7Line 1", "&7Line 2")      // vararg
Chat.tell(sender, listOf("&7a", "&7b"))        // any Iterable<String>

Chat.tellMini(player, "<green>Saved!</green>")  // MiniMessage
Chat.send(player, Chat.mini("<rainbow>hi"))     // raw Component

Chat.tellActionbar(player, "&e+10 coins")
Chat.sendTitle(player, "&6Welcome", "&7to the server", fadeIn = 10, stay = 70, fadeOut = 20)
```

All recipients are `CommandSender?` (null is ignored). Player-only features (action
bar, title) no-op gracefully for the console.

> **Blank lines:** `tell` skips an **empty** string (`""`). To send an intentional
> spacer line, pass a single space `" "`.

---

## 4. Broadcasting & clearing

```kotlin
Chat.broadcast("&bServer restarting in 5m")
Chat.broadcast(listOf("&bline 1", "&bline 2"))
Chat.broadcastPermission("&cStaff only notice", "myplugin.staff")

Chat.clearChat(player)        // push this player's chat off-screen (100 blank lines)
Chat.clearChat(player, 50)
Chat.clearChatAll()           // do it for everyone online
```

---

## 5. Logging

Logging routes through the **plugin's** `Logger`, so console output is automatically
labelled `[YourPlugin]` and respects log levels:

```kotlin
Chat.log("&aStarted")     // INFO
Chat.warn("Low on memory")// WARNING
Chat.severe("DB down")    // SEVERE
Chat.error("DB down")     // alias for severe()
```

`&` color codes work in log messages too. (`Chat.init` adopts `plugin.logger`; if
you never call `init`, logs go to the server logger prefixed with the provider name
from `setLogProvider`.)

---

## 6. PlaceholderAPI

If the **PlaceholderAPI** plugin is installed, placeholders are resolved
automatically whenever a `Player` recipient is known:

```kotlin
Chat.tell(player, "Welcome %player_name%, balance: %vault_eco_balance%")
```

You can also resolve manually:

```kotlin
val resolved = Chat.setPlaceholders(player, "Hello %player_name%")
```

When PlaceholderAPI isn't installed, the text is returned unchanged — the
integration is a soft dependency (the API class is only touched when the plugin is
present), so Mlib runs fine without it.

> **MiniMessage + PAPI:** PlaceholderAPI emits legacy/section text. If you feed a
> PAPI result into `tellMini`, those legacy codes won't be re-parsed as MiniMessage
> tags. Prefer `tell` for PAPI-heavy strings, or expanders that output MiniMessage.

---

## 7. TextMessage & TextReplacement

For reusable, templated messages, build a `TextMessage` with token replacements:

```kotlin
val replacements = TextReplacement.of(
    "%player%" to player.name,
    "%coins%" to coins.toString()
)

val message = TextMessage()
    .addMessages("&aWelcome %player%!", "&7You have &e%coins% &7coins.")
    .addReplacements(replacements)

message.send(player)            // or Chat.tell(player, message)
val lines: List<String> = message.getMessage()   // replacements applied, lazily
```

`TextReplacement` applies pairs **in insertion order** (so a later pair can act on
text introduced by an earlier one). Build it with the constructors, the `of(...)`
factories, or fluent `addReplacement(...)`:

```kotlin
TextReplacement.of("%a%" to "1", "%b%" to "2")
TextReplacement().addReplacement("%a%", "1").addReplacement("%b%", "2")
```

---

## 8. Testing

A manual test harness lives in [`test-plugin/`](../test-plugin/README.md) — a
standalone Gradle project (excluded from the published artifact) that uses
[`run-paper`](https://github.com/jpenilla/run-task) to launch a real Paper server:

```bash
mvn install                 # from the repo root, installs Mlib to ~/.m2
cd test-plugin && ./gradlew runServer
```

Then `/chattest <legacy|mini|actionbar|title|broadcast|clear|template|papi|platform>`.
On Paper it reports `Active chat platform: PaperNativePlatform`; on Spigot it falls
back to `BukkitAudiencesPlatform`.

## 9. Notes

- **Relocation:** Adventure is shaded under `dev.mrshawn.mlib.libs.kyori`. You won't
  collide with a server that ships its own Adventure.
- **Paper version skew:** `PaperNativePlatform` bridges components via JSON. Standard
  formatting (color, decoration, hex, click/hover) is stable across Adventure 4.x; if
  you hit an exotic component the server's older Adventure can't parse, force
  `Chat.Platform.BUKKIT_AUDIENCES`.
- **Off-main-thread:** as with all Bukkit messaging, send from the main thread.
