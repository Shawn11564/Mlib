# Mlib Command Framework

Mlib ships an **annotation-driven command framework** that replaces Bukkit's
`plugin.yml` command boilerplate. You write a class, annotate it, and the manager
wires up execution, nested subcommands, tab-completion, argument parsing, and
permission/sender checks for you.

| Piece | Package | Purpose |
|-------|---------|---------|
| **MCommand** | `commands` | Base class for a command (one per command/subcommand) |
| **MCommandManager** | `commands` | Registers commands into the live Bukkit command map, parses args, drives tab-complete |
| **Annotations** | `commands.annotations` | `@CommandAlias`, `@CommandExecutor`, `@Subcommand`, `@CommandCompletion`, `@Optional` |
| **Preconditions** | `commands.preconditions` | Reusable sender/permission gates checked before execution |
| **ExecutionContext** | `commands.enhancements` | Convenience wrapper around `(sender, args)` |

**Why it's a standout:** commands self-register into the server command map via
reflection — **no `plugin.yml` `commands:` block is required**. Argument types
are resolved automatically from the method signature, and tab-completion is
declarative.

---

## 1. A minimal command

```kotlin
@CommandAlias("heal|h")
class HealCommand : MCommand() {

    @CommandExecutor
    fun execute(player: Player) {
        player.health = 20.0
        Chat.tell(player, "&aYou have been healed!")
    }
}
```

Register it once (typically in your plugin's `registerCommands()`):

```kotlin
val mcm = MCommandManager()
mcm.registerCommand(HealCommand())
```

That's it — `/heal` and `/h` are now live. (If you extend `MPlugin`, an
`MCommandManager` named `mcm` already exists and `registerCommands()` is called
for you.)

### How a command class is shaped

- `@CommandAlias("a|b|c")` on the **class** declares the command names. The first
  alias is the canonical one (used in usage messages); the rest are aliases. This
  annotation is **required** — a command without it is rejected with an error log.
- Exactly one method marked `@CommandExecutor` is the default handler that runs
  when no subcommand matches.
- The **first parameter** may be `CommandSender` or `Player`. If it is, the
  command sender is injected there automatically and is **not** consumed from the
  typed arguments. Declaring `Player` effectively requires a player sender.

---

## 2. Automatic argument resolution

Every parameter after the (optional) sender is parsed from the typed arguments
using a registered **context resolver**. The method signature *is* the argument
spec — no manual `args[0].toInt()`.

```kotlin
@CommandAlias("pay")
class PayCommand : MCommand() {

    @CommandExecutor
    fun execute(sender: Player, target: Player, amount: Double) {
        // /pay <target> <amount>
        // `target` is resolved from an online player name, `amount` from a number
        economy.transfer(sender, target, amount)
    }
}
```

### Built-in context resolvers

| Parameter type | Resolved from |
|----------------|---------------|
| `Player` | online player name (fails with *"Player not found"* if absent) |
| `String` | the raw argument |
| `Int` / `Double` | parsed number (fails with a friendly message if not numeric) |
| `Boolean` | `true`/`false` |
| `Array<String>` | all remaining arguments |
| `ExecutionContext` | wrapper exposing `getSender()`, `getPlayer()`, `getArgs()` |

If parsing fails, the resolver throws and the sender is told the error message
automatically — the method never runs with bad input.

### Optional parameters

Mark a trailing parameter `@Optional` to allow it to be omitted; it arrives as
`null`:

```kotlin
@CommandExecutor
fun execute(sender: Player, @Optional reason: String?) {
    val msg = reason ?: "No reason given"
    ...
}
```

A non-optional parameter with no argument supplied stops execution with
*"Invalid command syntax."*

### Registering custom argument types

Teach the manager to resolve your own types:

```kotlin
mcm.registerContext(Arena::class.java) { sender, args ->
    arenaManager.get(args[0]) ?: throw ContextResolverFailedException("Unknown arena: ${args[0]}")
}
```

Now any command method can take an `Arena` parameter directly.

---

## 3. Subcommands

There are **two** ways to add subcommands; mix them freely.

### a) `@Subcommand` methods (lightweight)

Add a method per subcommand on the same class:

```kotlin
@CommandAlias("clan")
class ClanCommand : MCommand() {

    @CommandExecutor
    fun help(sender: CommandSender) {
        Chat.tell(sender, "&7/clan create|invite ...")
    }

    @Subcommand("create|new")
    fun create(sender: Player, name: String) {
        // /clan create <name>
    }

    @Subcommand("invite")
    fun invite(sender: Player, target: Player) {
        // /clan invite <target>
    }
}
```

The manager walks the typed arguments, matches `create`/`new`/`invite`, then
parses the remaining args into that method's parameters.

### b) Nested `MCommand` classes (full subtrees)

For deeper trees, make each subcommand its own `MCommand` and attach it:

```kotlin
@CommandAlias("admin")
class AdminCommand : MCommand() {
    init {
        addSubcommands(AdminReloadCommand(), AdminBanCommand())
    }

    @CommandExecutor
    fun execute(sender: CommandSender) { /* shown for bare /admin */ }
}

@CommandAlias("reload")
class AdminReloadCommand : MCommand() {
    @CommandExecutor
    fun execute(sender: CommandSender) { /* /admin reload */ }
}
```

Nested commands can themselves have subcommands, so arbitrarily deep trees work.
`getUsageMessage()` walks up the parent chain to build a fully-qualified
`/admin reload ...` usage string.

---

## 4. Tab-completion

Annotate the executor with `@CommandCompletion`, supplying one **completion id per
parameter**, space-separated and positional:

```kotlin
@CommandAlias("gamemode")
class GamemodeCommand : MCommand() {

    @CommandExecutor
    @CommandCompletion("@players @boolean")
    fun execute(sender: CommandSender, target: Player, silent: Boolean) { ... }
}
```

When the player tabs the first argument they see online players; the second
argument offers `true`/`false`.

### Built-in completion ids

| Id | Suggests |
|----|----------|
| `@players` | online player names |
| `@boolean` | `true`, `false` |
| `@nothing` | a blank (suppresses suggestions) |

### Custom completions

```kotlin
mcm.registerCompletion("@arenas") { sender ->
    arenaManager.all().map { it.name }
}
```

Then reference it positionally: `@CommandCompletion("@arenas @boolean")`.

---

## 5. Preconditions (sender & permission gates)

Preconditions are checked **before** execution *and* before tab-completion; a
failed check tells the sender the fail message and aborts. Pass them to the
`MCommand` constructor, built fluently:

```kotlin
@CommandAlias("ban")
class BanCommand : MCommand(
    Precondition.Builder()
        .isPlayer()
        .hasPermission("myplugin.ban")
        .build()
) {
    @CommandExecutor
    fun execute(sender: Player, target: Player) { ... }
}
```

Built-ins:

- `isPlayer()` → *"You must be a player to execute this command."*
- `hasPermission(node)` → *"You do not have permission to execute this command."*

### Custom preconditions

Implement `Precondition` and add it with `.addPrecondition(...)`:

```kotlin
class InArenaPrecondition : Precondition {
    override fun check(sender: CommandSender) =
        sender is Player && arenaManager.isPlaying(sender)
    override fun failMessage() = "You must be in an arena."
}

class LeaveCommand : MCommand(
    Precondition.Builder().addPrecondition(InArenaPrecondition()).build()
)
```

---

## 6. Putting it together

```kotlin
class MyPlugin : MPlugin() {

    override val listeners = arrayOf<Listener>(/* ... */)

    override fun initObjects() { /* touch kotlin `object`s that must init */ }

    override fun registerCommands() {
        // custom argument types & completions first
        mcm.registerContext(Arena::class.java) { _, args ->
            arenaManager.get(args[0]) ?: throw ContextResolverFailedException("Unknown arena")
        }
        mcm.registerCompletion("@arenas") { arenaManager.all().map { it.name } }

        // then the commands
        mcm.registerCommand(HealCommand())
        mcm.registerCommand(PayCommand())
        mcm.registerCommand(AdminCommand())
    }
}
```

---

## 7. Internals worth knowing

- **No `plugin.yml` needed.** `MCommandManager` reflectively grabs the server's
  `SimpleCommandMap` and registers a `Command` per alias, routing
  `execute`/`tabComplete` back through the manager.
- **Resolution order per invocation:** match nested subcommands → match
  `@Subcommand` methods → fall back to the `@CommandExecutor`. Preconditions are
  evaluated on the resolved (deepest) command.
- **Failures are user-friendly.** A `ContextResolverFailedException` thrown by any
  resolver is caught and its message is sent to the player (colorized); the method
  is never invoked with partially-parsed arguments.
- **Sender types are configurable.** `MCommandManager.addCommandSenderType(type)`
  lets you register additional first-parameter sender types beyond
  `CommandSender`/`Player`.
