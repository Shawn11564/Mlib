# Mlib Scheduling & Tasks

Beyond Bukkit's tick-based scheduler, Mlib offers a **wall-clock scheduling
system**: run tasks at real calendar times ("every Friday at 18:00", "daily at
midnight") in a configurable time zone. It also includes lightweight task
primitives for chaining work.

| Piece | Package | Purpose |
|-------|---------|---------|
| **MTask** | `tasks` | The unit of work: an abstract `run()` with a random `taskID` |
| **TaskChain** | `tasks` | Runs a sequence of `MTask`s on a background thread |
| **ScheduledMTask** | `tasks` | An `MTask` bound to a `Schedule` of calendar times |
| **Schedule** | `tasks.schedules` | A set of `ScheduledTime`s; serializable to config |
| **ScheduledTime** | `tasks.schedules` | A single `(day, hour, minute)` trigger |
| **MCalendar** | `tasks.schedules` | The ticking clock that fires due tasks in a time zone |

**Why it's a standout:** schedules are expressed in human calendar terms, are
**`ConfigurationSerializable`** (save/restore them straight to YAML), and run on a
single repeating Bukkit task you control the resolution of.

---

## 1. Tasks and chains

An `MTask` is just a named piece of work:

```kotlin
val notify = object : MTask() {
    override fun run() { Chat.broadcast("&aBackup starting...") }
}
```

`TaskChain` executes a series of them in order on a background single-thread
executor:

```kotlin
TaskChain()
    .then(notify)
    .then(backupTask)
    .then(doneTask)
    .start()
```

> **Threading note:** `TaskChain` runs off the main server thread. Only touch the
> Bukkit API from inside a task that hops back to the main thread (e.g. via
> `Bukkit.getScheduler().runTask`). Use chains for I/O-style work.

---

## 2. Wall-clock scheduling

The flow is: build an `MCalendar` (the clock), create `ScheduledMTask`s with the
calendar times they should run at, register them, and start the scheduler.

### a) Build with the fluent builders

```kotlin
val task = ScheduledMTask.Builder()
    .init(calendar)
    .withRunnable { Chat.broadcast("&6Daily reset!") }
    .addToSchedule(MCalendar.Day.DAILY, hour = 0, minute = 0)   // every day at 00:00
    .addToSchedule(MCalendar.Day.FRIDAY, hour = 18, minute = 0) // and Fridays at 18:00
    .build()
```

`MCalendar.Day` covers `SUNDAY..SATURDAY` plus **`DAILY`** (runs every day,
regardless of weekday). You can also pass a raw `Calendar` day-of-week int.

### b) Register and start the clock

```kotlin
// timeZone is any java TimeZone id; checkInterval is in seconds
val calendar = MCalendar(plugin, timeZone = "America/New_York", checkInterval = 30)

calendar.addTask(task)
calendar.startScheduler()   // checks every 30s and runs anything due "now"
```

`startScheduler()` runs a repeating Bukkit task every `checkInterval` seconds; on
each tick it runs the tasks whose schedule matches the current minute in the
configured time zone. **Pick a `checkInterval` ≤ 60s** so minute-precise triggers
aren't missed.

### c) The `Schedule.Builder` shortcut

`Schedule.Builder` bundles calendar creation and hands you straight into the task
builder:

```kotlin
val builder = Schedule.Builder()
    .init(plugin, timeZone = "UTC", checkInterval = 30)

val task = builder.withTask()
    .withRunnable { runBackup() }
    .addToSchedule(MCalendar.Day.DAILY, 3, 0)   // 03:00 UTC daily
    .build()
```

---

## 3. Persisting schedules

`Schedule` and `ScheduledTime` are `ConfigurationSerializable`, so a player- or
admin-defined schedule round-trips through YAML:

```kotlin
// save
config.set("events.reset", task.getSchedule())

// restore (you supply the calendar the times attach to)
val schedule = Schedule.deserialize(calendar, config.getMap("events.reset"))
```

Each `ScheduledTime` also has a readable `toString()` — e.g. `"Friday at 18:0"` —
handy for admin list commands.

---

## 4. How matching works

- A `ScheduledTime(day, hour, minute)` is "due" when the current calendar's
  hour and minute match, **and** the day matches — where `day == 8` (`Day.DAILY`)
  matches every weekday.
- A `Schedule` is due when **any** of its times match and it isn't on cooldown
  (`Schedule.onCooldown`). Set `onCooldown = true` yourself after firing if you
  need to debounce within the same minute across multiple checks.
- `MCalendar.getCalendar()` always reads "now" in the configured time zone, so DST
  and regional time are handled by the JVM.

---

## 5. Notes

- One `MCalendar` can drive many tasks — create it once per plugin and
  `addTask(...)` everything.
- `checkInterval` is a trade-off: smaller = tighter timing but more frequent
  checks. 30s is a good default for minute-resolution schedules.
- For simple tick-based repeats you still want plain Bukkit `BukkitRunnable`; this
  system is specifically for *calendar*-time events.
