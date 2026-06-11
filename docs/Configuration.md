# Mlib Configuration (`@Value`)

`Configuration` binds Kotlin properties to YAML config paths with an annotation —
the same idea as **Spring Boot's `@Value`**, but built for Bukkit plugins and, most
importantly, **reloadable at runtime**. When an admin edits the config file and runs
your `/reload` command, a single `reload()` call re-reads the file and updates every
bound property in place.

```kotlin
class Settings(plugin: JavaPlugin) : Configuration(plugin, "settings.yml") {

    @Value("prefix", comments = ["Prefix shown before plugin messages"])
    var prefix: String by config("&7[MyPlugin] ")

    @Value("economy.starting-balance")
    var startingBalance: Double by config(100.0)

    init { load() }   // must come after the property declarations (see §2)
}
```

```kotlin
val settings = Settings(this)
player.sendMessage(settings.prefix + "Balance: " + settings.startingBalance)
```

> This is the typed, declarative option. For untyped key/value access (or JSON), see
> [Files & Config](Files.md) (`KFile` / `Kson`). `Configuration` is best when you have
> a fixed, known set of settings you want as real, typed fields.

---

## 1. Defining a config class

Extend `Configuration`, choosing where the file lives:

```kotlin
class Settings(plugin: JavaPlugin) : Configuration(plugin, "settings.yml")  // -> plugins/MyPlugin/settings.yml
// or:  Configuration(filePath, fileName)   // a specific directory
// or:  Configuration(File(...))            // an exact file
```

Each setting is a property that combines:

- **`@Value("path", comments = [...])`** — the YAML path (dot-separated for nesting)
  and optional comment lines.
- **`by config(default)`** — the delegate, carrying the **default value** and binding
  the property's **type**.

```kotlin
@Value("economy.starting-balance", comments = ["Money new players start with"])
var startingBalance: Double by config(100.0)
```

The path, type, and default all live in one place — no manual `getDouble("...", 100.0)`.

The parent directory and file are created automatically on construction.

---

## 2. The `init { load() }` requirement

You **must call `load()` after the properties are declared** — put an `init { load() }`
block *below* them, or call `settings.load()` right after constructing the object.

Why: a subclass's delegate fields don't exist until *after* the `Configuration` base
constructor returns, so the base class can't load them for you. `load()`:

1. reads each bound path from disk into its property (or writes the default if the key
   is missing), and
2. saves the file back — so first run produces a fully-populated, commented config.

```kotlin
class Settings(plugin: JavaPlugin) : Configuration(plugin, "settings.yml") {
    @Value("prefix") var prefix: String by config("&7[MyPlugin] ")
    @Value("debug")  var debug: Boolean by config(false)

    init { load() }   // ✅ after the two properties above
}
```

Forgetting `load()` leaves every property at its compile-time default and never reads
the file.

---

## 3. Reading & writing values

Bound properties are normal Kotlin properties:

```kotlin
if (settings.debug) logger.info("debug on")     // read

settings.prefix = "&a[MyPlugin] "                // write (in-memory)
settings.save()                                  // persist all properties to disk
```

Reading returns the current in-memory value. Assigning updates it (and the in-memory
config), but **isn't written to disk until you call `save()`**.

### Ad-hoc values with `getOrSetDefault`

For one-off settings you don't want to declare as a property, `getOrSetDefault` reads
a path — and if it's absent, writes the given default, persists, and returns it:

```kotlin
val cooldown = settings.getOrSetDefault("limits.cooldown-seconds", 30)
```

The default's type drives coercion (see §6), so the result is correctly typed.

---

## 4. Reloading (the headline feature)

When an operator edits `settings.yml` by hand and runs your reload command, call
`reload()` — it re-reads the file from disk and refreshes **every** bound property:

```kotlin
@CommandAlias("myplugin")
class MyPluginCommand(private val settings: Settings) : MCommand(
    Precondition.Builder().hasPermission("myplugin.admin").build()
) {
    @Subcommand("reload")
    fun reload(sender: CommandSender) {
        settings.reload()                          // re-reads the file into the live object
        Chat.tell(sender, "${settings.prefix}&aConfig reloaded!")
    }
}
```

After `reload()`, code holding the same `settings` instance immediately sees the new
values — no restart, no re-wiring. Keys removed from the file fall back to their
defaults (and are written back), so a partial/edited file can't break you.

> Keep a **single, shared** `Settings` instance (e.g. a property on your plugin or a
> Kotlin `object`) so a `reload()` is visible everywhere. If different parts of your
> plugin construct their own copies, only the reloaded one updates.

---

## 5. Comments & the generated file

Comment lines from `@Value(..., comments = [...])` are written above their key:

```kotlin
@Value("prefix", comments = ["Prefix shown before plugin messages", "Supports & color codes"])
var prefix: String by config("&7[MyPlugin] ")

@Value("debug", comments = ["Enable verbose logging"])
var debug: Boolean by config(false)
```

produces:

```yaml
# Prefix shown before plugin messages
# Supports & color codes
prefix: '&7[MyPlugin] '
# Enable verbose logging
debug: false
```

Comments work for **nested** keys too — the writer tracks the full dotted path by
indentation, so `@Value("economy.start-balance", comments = [...])` is placed correctly:

```yaml
economy:
  # Money new players start with
  start-balance: 100.0
```

(It assumes the 2-space indentation Bukkit writes; if you hand-edit with a different
indent, the next `save()`/`reload()` rewrites the file back to normal form anyway.)

---

## 6. Supported types

The property type drives how the value is read back from YAML. Use types Bukkit's
`YamlConfiguration` round-trips cleanly:

| Type | Notes |
|------|-------|
| `String` | |
| `Int`, `Long`, `Double` | the property type must match how the value is stored (don't read an `Int` value as `Double`) |
| `Boolean` | |
| `List<String>` | and other YAML-native lists |
| `ConfigurationSerializable` | e.g. `Location`, or your own (see [Selections.md](Selections.md) `Region`) |

On load the stored value is matched against the property's type:

- **Numbers are coerced** between forms, so an operator writing `start-balance: 100`
  (an int) into a `Double` field works — it loads as `100.0`.
- **A genuine mismatch raises a `ConfigurationException`** with a clear message instead
  of a cryptic cast failure, e.g.:
  > Could not load 'economy.max-homes' from 'settings.yml': expected Integer but found
  > String (value: notanumber). Fix the value, or remove the line to regenerate its default.
- **A missing (or `null`) key falls back to the declared default** and is written back,
  so an operator deleting a line just regenerates it.

---

## 7. End-to-end example

```kotlin
import dev.mrshawn.mlib.files.configuration.Configuration
import dev.mrshawn.mlib.files.configuration.annotations.Value
import org.bukkit.plugin.java.JavaPlugin

class Settings(plugin: JavaPlugin) : Configuration(plugin, "settings.yml") {

    @Value("prefix", comments = ["Prefix for all plugin messages"])
    var prefix: String by config("&7[MyPlugin] ")

    @Value("max-homes", comments = ["How many homes a player may set"])
    var maxHomes: Int by config(3)

    @Value("welcome-message", comments = ["Lines shown on join"])
    var welcomeMessage: List<String> by config(listOf("&aWelcome!", "&7Enjoy your stay."))

    init { load() }
}

class MyPlugin : JavaPlugin() {
    lateinit var settings: Settings
        private set

    override fun onEnable() {
        settings = Settings(this)   // creates + fills settings.yml on first run
    }

    fun reloadConfigFile() = settings.reload()   // call from your /reload command
}
```

---

## 8. Notes

- **Always `load()`** after the properties (an `init { load() }` block is the idiom).
- **`@Value` is required** on every `config()` property — a missing annotation throws
  a `ConfigurationException` naming the offending property.
- **Writes are in-memory until `save()`**; `load()`/`reload()` both persist defaults
  and comments as a side effect (so the on-disk file stays complete).
- **Type mismatches throw `ConfigurationException`** (numbers are coerced first); a
  missing key regenerates its default.
- **Share one instance** so `reload()` propagates everywhere.
- For untyped or JSON configuration, use [`KFile` / `Kson`](Files.md) instead.
