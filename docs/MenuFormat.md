# Mlib Menu Format & Loader

Mlib can build GUIs from a declarative **YAML or JSON** file at runtime — no Kotlin/Java required.
This is the "mlib format": a portable description of a menu (or a set of chained menus) that the
[visual editor](../editor) exports and that [`MenuLoader`](../src/main/kotlin/dev/mrshawn/mlib/guis/serialization/MenuLoader.kt)
turns into real [`Menu`](../src/main/kotlin/dev/mrshawn/mlib/guis/menus/Menu.kt) objects.

The canonical schema is [`schema/menu-schema.json`](../schema/menu-schema.json) (JSON Schema
2020-12) — point your editor/IDE at it for autocomplete and validation.

> **Code vs. format.** The [code-based GUI API](GUIs.md) is still the most powerful option. The
> format covers the common case (lay out items, wire clicks to a fixed vocabulary, chain menus) and
> escapes to code for anything else via [custom hooks](#custom-actions--conditions).

---

## 1. Loading a menu

```kotlin
// from the plugin data folder (menus/main.yml)
val project = MenuLoader.load(plugin, "menus/main.yml")
project.open("main_menu", player)

// or a one-liner via the project
MenuLoader.load(plugin, "menus/main.yml").firstMenu()?.open(player)
```

`MenuLoader` reads `.yml`/`.yaml` and `.json`. Both parse into the same model, so you can author in
whichever you prefer. A file may contain **one menu** or **many** (a "project"); chaining and
`OPEN_MENU` resolve menus by their id within the same file.

Field-level mistakes are **non-fatal**: an unknown material/enchantment/sound is logged and
degraded (e.g. a labelled `BARRIER`) so the menu still opens and you can see the problem in-game.
Only whole-document failures (bad YAML/JSON, an unsupported `formatVersion`) throw
`MenuParseException`.

---

## 2. Document structure

```yaml
formatVersion: 1            # required; rejected if newer than this mlib supports
project: example-guis       # optional
menus:                      # map of id -> menu
  main_menu:
    title: "&8Main Menu"    # & codes and &#RRGGBB hex supported
    type: chest             # only 'chest' in v1
    rows: 3                 # 1..6
    previousMenu: null      # menu id -> auto red-wool "Previous Menu" button at (0,0) + BACK
    nextMenu: settings_menu # menu id -> auto lime-wool "Next Menu" button at (8,0) + NEXT
    fill:                   # optional background filler placed in every slot first
      item: { material: BLACK_STAINED_GLASS_PANE, name: " " }
    fillAreas:              # optional rectangles (width/height are COUNTS)
      - { x: 0, y: 0, width: 9, height: 1, item: { item: { material: GRAY_STAINED_GLASS_PANE, name: " " } } }
    items: [ ... ]          # positioned clickable items
    panes: [ ... ]          # static / paginated regions
```

Coordinates are `x` (column 0–8) and `y` (row 0-based), exactly like the code API.

---

## 3. Items

Every entry under `items` (and `fill`, `fillAreas[].item`, pane `contents`) is an **item node**:
a position, a `kind`, an `item` appearance, and an `actions` set.

```yaml
items:
  - x: 4
    y: 1
    kind: basic            # basic | glowing | updating | toggle | cycle | twoStage
    item: { material: COMPASS, name: "&aSettings", lore: ["&7Click to open"] }
    actions: { left: [ { type: OPEN_MENU, menu: settings_menu } ] }
```

### Appearance (`item`)

Maps onto mlib's builder family ([`ItemBuilder`](../src/main/kotlin/dev/mrshawn/mlib/items/builders/ItemBuilder.kt) etc.):

| Field | Notes |
|-------|-------|
| `material` | Bukkit `Material` name (e.g. `DIAMOND_SWORD`), or `hook:<id>` for a code-provided icon |
| `amount` | 1–64 |
| `name`, `lore` | `&` / `&#hex` codes; PlaceholderAPI resolved for the viewer when present |
| `enchantments` | `[{ type: sharpness, level: 5 }]` (key or legacy name) |
| `glow` | enchant-glow without showing enchants |
| `customModelData` | resource-pack model id |
| `itemFlags` | `[HIDE_ATTRIBUTES, HIDE_ENCHANTS, ...]` |
| `hideAttributes` | shortcut for the flag |
| `skull` | `{ owner: "Notch" | "<uuid>" | "mhf:MHF_QUESTION" }` (PLAYER_HEAD) |
| `potion` | `{ color: "#FF0000", effects: [{ type: SPEED, duration: 600, amplifier: 1 }] }` |
| `banner` | `{ patterns: [{ color: RED, pattern: STRIPE_TOP }] }` (a `*_BANNER`) |

> Base64 skull `texture` is reserved in the schema but not yet applied by the loader — use
> `MenuHooks.registerItem` for custom-textured heads.

### Item kinds

Each kind maps to a [`GuiItem`](GUIs.md#3-guiitems) implementation:

```yaml
# basic — a plain button (default)
- { kind: basic, item: {...}, actions: {...} }

# glowing — glows while a condition holds
- { kind: glowing, item: {...}, glowCondition: "permission:vip", actions: {...} }

# updating — rebuilt every render (great with placeholders)
- { kind: updating, item: { material: CLOCK, name: "&eOnline: %server_online%" } }

# toggle — flips between two full item states, each with its own actions
- kind: toggle
  initiallyToggledOn: false
  on:  { item: { material: LIME_DYE, name: "&aON" },  actions: { left: [ { type: RUN_COMMAND, command: "feature on" } ] } }
  off: { item: { material: GRAY_DYE, name: "&7OFF" }, actions: { left: [ { type: RUN_COMMAND, command: "feature off" } ] } }

# cycle — rotates through N states on click
- kind: cycle
  states:
    - { item: { material: GREEN_WOOL, name: "&aEasy" } }
    - { item: { material: RED_WOOL,   name: "&cHard" } }

# twoStage — click-to-confirm; secondClickActions run on the 2nd click
- kind: twoStage
  item: { material: TNT, name: "&cReset (click twice)" }
  glowOnFirstClick: true
  secondClickActions: { default: [ { type: RUN_COMMAND, as: CONSOLE, command: "resetdata %player_name%" }, { type: CLOSE } ] }
```

---

## 4. Actions

An `actions` block routes a click to a list of steps. Per-click-type branches override `default`:

```yaml
actions:
  requirements: ["permission:menus.use"]   # gate every click; on fail -> denyActions (or a default deny msg)
  denyActions: [ { type: MESSAGE, text: "&cNo permission." } ]
  default:    [ { type: PLAY_SOUND, sound: UI_BUTTON_CLICK } ]
  left:       [ { type: OPEN_MENU, menu: shop } ]
  right:      [ { type: RUN_COMMAND, as: PLAYER,  command: "spawn" } ]
  shiftLeft:  [ { type: RUN_COMMAND, as: CONSOLE, command: "give %player_name% diamond 1" } ]
```

Branches: `default`, `left`, `right`, `shiftLeft`, `shiftRight`, `middle`, `drop`, `doubleClick`.
Clicks are **cancelled by default** (players can't take items); use `ALLOW` to opt out.

### Action vocabulary

| `type` | Fields |
|--------|--------|
| `OPEN_MENU` | `menu` (id in this file) |
| `BACK` / `NEXT` | — (uses `previousMenu`/`nextMenu`) |
| `CLOSE` | — |
| `RUN_COMMAND` | `command`, `as: PLAYER\|CONSOLE` (PAPI-expanded) |
| `MESSAGE` | `text` or `lines[]`, `mini: bool` |
| `BROADCAST` | `text`/`lines[]`, optional `permission` |
| `ACTIONBAR` | `text` |
| `TITLE` | `title`, `subtitle`, `fadeIn`, `stay`, `fadeOut` (ticks) |
| `PLAY_SOUND` | `sound`, `volume`, `pitch` |
| `GIVE_ITEM` | `item: {appearance}`, `amount` |
| `CONSOLE_LOG` | `text`, `level: INFO\|WARN\|SEVERE` |
| `CONDITIONAL` | `if: <condition>`, `then: [...]`, `else: [...]` |
| `CANCEL` / `ALLOW` | — (force the click cancelled / allowed) |
| `CUSTOM` | `id`, optional `data: {...}` — see below |

### Conditions / requirements

A condition is a shorthand string or an object:

```yaml
requirements:
  - "permission:rank.vip"
  - "gamemode:SURVIVAL"
  - { any: ["op", "permission:menus.admin"] }
  - { not: "world:world_nether" }
  - "placeholder:%vault_eco_balance% >= 100"   # needs PlaceholderAPI
  - "custom:hasCompletedTutorial"               # MenuHooks.registerCondition
```

String forms: `permission:<node>`, `op`, `gamemode:<MODE>`, `world:<name>`,
`placeholder:%x% <op> y` (`==`, `!=`, `>`, `<`, `>=`, `<=`, `contains`), `custom:<id>`.
Object forms: `{ all: [...] }`, `{ any: [...] }`, `{ not: <condition> }`.

---

## 5. Custom actions & conditions

The format can only *name* custom behavior; **you** define it in code by registering handlers once
at startup (e.g. in an `MPlugin`'s `initObjects()`). Ids are matched case-insensitively.

```yaml
# in the menu file
actions:
  left:
    - type: CUSTOM
      id: openPlayerShop
      data: { category: weapons, page: 1 }
```

```kotlin
// in your plugin
MenuHooks.registerAction("openPlayerShop") { ctx ->
    val category = ctx.string("category") ?: "all"
    val page = ctx.int("page") ?: 1
    ShopMenu(plugin, category, page).show(ctx.player)   // arbitrary logic
}

MenuHooks.registerCondition("hasCompletedTutorial") { player -> tutorialService.isDone(player) }

// optional: a fully code-driven icon, referenced by material: "hook:balanceHead"
MenuHooks.registerItem("balanceHead") { player -> /* build an ItemStack */ }
```

[`ActionContext`](../src/main/kotlin/dev/mrshawn/mlib/guis/serialization/ActionContext.kt) gives the
handler the click `event`, `player`, `clickType`, the `data` map, and the owning `menu`.

---

## 6. Panes

```yaml
panes:
  - kind: paginated        # static | paginated
    x: 0
    y: 1
    width: 9               # column COUNT  (loader converts to mlib's inclusive bounds)
    height: 3              # row COUNT
    priority: NORMAL
    contents:
      - { item: { material: PAPER, name: "&fEntry" }, actions: {...} }
```

`width`/`height` are **sizes** (counts). The loader translates them to mlib's pane-constructor
convention and a matching [`PaginatedPane`](../src/main/kotlin/dev/mrshawn/mlib/guis/panes/impl/PaginatedPane.kt)
page-size fix keeps static and paginated panes consistent for any origin.

---

## 7. Assets & licensing (editor)

The [visual editor](../editor) renders vanilla item/block icons sourced from
[InventivetalentDev/minecraft-assets](https://github.com/InventivetalentDev/minecraft-assets) at the
configured Minecraft version. **Minecraft assets are © Mojang** and are **not** committed to this
repository — the editor fetches them at build time and bakes a sprite atlas into the deployed site
only. Do not redistribute the textures outside that context.
