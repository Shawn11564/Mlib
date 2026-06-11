# Mlib File & Config Storage

Mlib gives you two file abstractions so you don't hand-roll Bukkit's
`YamlConfiguration` or raw JSON parsing:

| Class | Format | Best for |
|-------|--------|----------|
| **KFile** | YAML (`.yml`) | Bukkit-style config files, especially ones shipped as a jar resource |
| **Kson** | JSON (`.json`) | Structured data + typed object (de)serialization via Gson |

Both live in the `files` package, take a `JavaPlugin`, default to the plugin's
data folder, and can **extract a bundled default** from the jar with
`isResource = true`. Both share the **`KFile.IConfigList`** enum-path pattern (see
§3) so you can address keys with type-safe enums instead of string literals.

> **Persistence:** both classes keep edits in memory and expose an explicit
> **`save()`** to write them to disk — call it after your `set(...)` calls when you
> want the changes to survive a restart. `KFile` is YAML (Bukkit-conventional,
> hand-editable); `Kson` is JSON with typed object mapping.

---

## 1. KFile (YAML)

### Loading

```kotlin
// plugin data folder, created if missing
val config = KFile(plugin, "config.yml")

// extract the bundled resources/config.yml on first run
val defaults = KFile(plugin, "config.yml", isResource = true)

// custom directory
val data = KFile(plugin, "stats.yml", filePath = "${plugin.dataFolder}/data")
```

On construction `KFile` creates parent directories, copies the resource (if
`isResource`) or creates an empty file, then **flattens every key** (via
`getKeys(true)`) into an in-memory map for fast lookups.

### Reading

Keys are flattened dot-paths, matching YAML nesting:

```yaml
# config.yml
spawn:
  world: world
  x: 100
messages:
  - "&aLine one"
  - "&aLine two"
```

```kotlin
config.getString("spawn.world")     // "world"
config.getInt("spawn.x")            // 100
config.getDouble("spawn.x")         // null (stored as Int, cast fails -> null)
config.getBoolean("flags.pvp")      // null if absent
config.getStringList("messages")    // ["&aLine one", "&aLine two"]

config.isValue("spawn.world")       // true
```

> **Caveat:** the typed getters are plain casts. `getInt` on a value that isn't an
> `Int` returns `null`; `getStringList` on a **missing** key throws (it casts
> `null` to `List`). Guard with `isValue(...)` or `getOrSet(...)` if the key may be
> absent.

### Writing & defaults

```kotlin
config.set("spawn.x", 250)                  // updates the in-memory config
val max = config.getOrSet("limits.max", 32) // returns existing, or sets + returns the default
config.save()                               // persist in-memory changes to disk
config.reload()                             // re-read the file from disk
```

`set`/`getOrSet` mutate the in-memory config; call `save()` to write the file.
`KFile` is also great for **seeding runtime defaults** from a resource-backed file
(`isResource = true`) and reading operator-edited config.

---

## 2. Kson (JSON)

`Kson` is the Gson-backed counterpart. It does **two jobs at once**: flat config
access by dot-path, *and* typed object mapping for whole documents or fragments.

### Loading & saving

```kotlin
val store = Kson(plugin, "arenas.json")     // pretty-printed by default
val compact = Kson(plugin, "cache.json", prettyPrint = false)
val seeded = Kson(plugin, "defaults.json", isResource = true)

store.reload()   // discard in-memory changes, re-read disk
store.save()     // persist in-memory changes to disk
```

A brand-new non-resource file is initialised to `{}`.

### Flat config access

Paths are dot-separated and **nested objects are created on demand**:

```kotlin
store.set("lobby.maxPlayers", 16)
// in-memory document is now { "lobby": { "maxPlayers": 16 } }

store.getInt("lobby.maxPlayers")    // 16
store.getString("lobby.name")       // null if absent
store.getLong(...) / getDouble(...) / getBoolean(...)
store.getStringList("lobby.motd")   // [] if absent or not an array
store.contains("lobby.maxPlayers")  // true

store.getOrSet("lobby.name", "Main Lobby")  // existing value, or set + return default
store.set("lobby.name", null)               // remove a path

store.save()
```

The typed getters are null-safe: they check the JSON kind, so calling `getInt` on
a string value returns `null` rather than throwing.

### Typed object mapping

Persist and retrieve whole data classes / POJOs with Gson:

```kotlin
data class Arena(val name: String, val maxPlayers: Int, val spawn: Location)

// a fragment at a path
store.set("lobby.spawn", spawnLocation)
val spawn = store.get("lobby.spawn", Spawn::class.java)

// the whole document as an object…
store.write(listOf(arenaA, arenaB))
store.save()

// …and back, preserving generics via the reified helper
val arenas: List<Arena>? = store.readValue()         // List<Arena>
val one: Arena? = store.get("arenas.0", Arena::class.java)
```

| Method | Reads/writes | Notes |
|--------|--------------|-------|
| `get(path, Class<T>)` / `get(path, Type)` | a path fragment → `T?` | `null` if the path is absent |
| `getValue<T>(path)` | a path fragment → `T?` | reified; keeps generic args (e.g. `List<Arena>`) |
| `read(Class<T>)` / `read(Type)` | the whole document → `T?` | |
| `readValue<T>()` | the whole document → `T?` | reified; keeps generic args |
| `write(obj)` | replaces the whole document | in-memory; call `save()` to persist |
| `set(path, obj)` | serializes `obj` at a path | any object, not just primitives |

> The document **root may be an object or an array**: `write(aList)` and
> `readValue<List<…>>()` work for top-level arrays, while `set`/`get` path access
> operates on (and creates) an object root.

---

## 3. Enum-driven paths (`IConfigList`)

Rather than scatter string literals, implement `KFile.IConfigList` on an enum and
pass the enum constant directly. Both `KFile` and `Kson` accept it.

```kotlin
enum class Settings(private val path: String, private val def: Any?) : KFile.IConfigList {
    PREFIX("messages.prefix", "&7[MyPlugin] "),
    MAX_HOMES("limits.maxHomes", 3);

    override fun getPath() = path
    override fun getDefault() = def
}

// KFile
config.getOrSetDefault(Settings.MAX_HOMES)   // 3 (and seeds it if absent)
config.getString(Settings.PREFIX)

// Kson
store.set(Settings.MAX_HOMES, 5)
store.getOrSetDefault(Settings.PREFIX)
store.save()
```

`getOrSetDefault(...)` reads the value, seeding the enum's `getDefault()` when the
key is missing — a one-liner for "load config with built-in defaults".

---

## 4. Choosing between them

- **Operator-edited config, shipped with the jar?** → `KFile` (YAML is the Bukkit
  convention and friendlier to hand-edit; use `isResource = true`).
- **Structured data with typed (de)serialization of data classes/POJOs?** → `Kson`
  (object mapping via Gson, plus array-rooted documents).
- **Need both?** Use `KFile` for the human-facing config and a separate `Kson` for
  the typed data store. Both call `save()` to persist.
- **Want typed fields with a fixed schema and live reload?** Use the annotation-driven
  [`Configuration` (`@Value`)](Configuration.md) instead — Spring-Boot-style binding of
  properties to YAML paths with a one-call `reload()`.
