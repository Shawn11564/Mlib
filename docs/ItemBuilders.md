# Mlib Item Builders

Mlib provides a family of **fluent `ItemStack` builders** so you never touch
`ItemMeta` casting, lore lists, or color codes by hand. Every builder returns
`this`, so calls chain, and the base `ItemBuilder` automatically colorizes names
and lore with `&` codes.

| Builder | Base item | Adds |
|---------|-----------|------|
| **ItemBuilder** | any `Material` | name, lore, enchants, glow, flags, model data, persistent data |
| **SkullBuilder** | `PLAYER_HEAD` | player/UUID/name/decorative-head ownership |
| **PotionBuilder** | `POTION` | custom potion color + effects |
| **BannerBuilder** | any banner | layered dye patterns |

`SkullBuilder`, `PotionBuilder`, and `BannerBuilder` all **extend `ItemBuilder`**,
so every base method (name, lore, glow, …) is available on them too.

---

## 1. ItemBuilder — the core

```kotlin
val sword = ItemBuilder(Material.DIAMOND_SWORD)
    .name("&b&lFrostbrand")
    .addLoreLine("&7A blade of pure ice.")
    .addLoreLine("")
    .addLoreLines("&8Damage: &f9", "&8Speed: &f1.6")
    .addEnchantment(Enchantment.DAMAGE_ALL, 5)
    .hideAttributes()
    .build()                       // -> ItemStack
```

Key points:

- `name(...)` and every `addLoreLine(...)` run the text through `Chat.colorize`,
  so `&`-style codes work out of the box. Use `setNoName()` for a blank-named item.
- `build()` flushes the accumulated lore into the meta and returns the finished
  `ItemStack`. **Lore is only applied on `build()`** — that's the commit step.
- Amount is the second constructor arg: `ItemBuilder(Material.ARROW, 16)`.

### Conditional building

Builders support inline conditions so you can avoid breaking the chain:

```kotlin
ItemBuilder(Material.PAPER)
    .name("&eQuest")
    .addLoreLineIf("&aCompleted!") { quest.isComplete() }
    .addLoreLinesIf(listOf("&7Reward:", "&6100 coins")) { quest.hasReward() }
    .glowIf { quest.isComplete() }      // enchant glow only when true
    .build()
```

### Glow without a visible enchant

`glow()` adds a hidden `DURABILITY 1` enchant and hides the enchant text, giving
the item the enchanted shimmer with no lore line. (The static
`ItemBuilder.glow(itemStack)` does the same to an already-built stack.)

### Persistent data & model data

```kotlin
val key = NamespacedKey(plugin, "coin_value")

ItemBuilder(Material.GOLD_NUGGET)
    .name("&6Coin")
    .addData(key, 100)                 // Int or String overloads
    .setCustomModelData(1001)          // resource-pack model
    .build()
```

### From an existing stack

`ItemBuilder.fromItemStack(stack)` clones an existing `ItemStack` into a builder
so you can tweak it:

```kotlin
val upgraded = ItemBuilder.fromItemStack(existing)
    .addLoreLine("&a+ Upgraded")
    .build()
```

---

## 2. SkullBuilder — player heads

Construct from anything that identifies a head — an `OfflinePlayer`, a `UUID`, a
name, or a built-in decorative head from `ItemUtils.MhfHeads`:

```kotlin
// by online/offline player
SkullBuilder(player).name("&f${player.name}").build()

// by UUID or name
SkullBuilder(uuid).build()
SkullBuilder("Notch").build()

// a decorative "MHF" head (arrows, blocks, mobs, etc.)
SkullBuilder(ItemUtils.MhfHeads.MHF_ARROW_UP)
    .name("&aScroll up")
    .build()
```

Because it extends `ItemBuilder`, you keep chaining lore/glow/etc. Change the
owner later with `setOwner(...)`.

---

## 3. PotionBuilder — custom potions

```kotlin
PotionBuilder()
    .name("&dElixir of Haste")
    .setPotionColor(Color.FUCHSIA)                       // bottle tint
    .addPotionEffect(PotionEffectType.FAST_DIGGING, 20 * 60, 1)  // duration ticks, amplifier
    .addLoreLine("&7Mine faster for 60s.")
    .build()
```

`addPotionEffect` adds a **custom** effect (overriding any existing), so you can
stack multiple effects on one bottle.

---

## 4. BannerBuilder — layered banners

Start from a `BannerType` (an enum covering every banner color) and layer dye
patterns on top:

```kotlin
BannerBuilder(BannerType.RED)
    .name("&cGuild Banner")
    .addPattern(DyeColor.WHITE, PatternType.CROSS)
    .addPattern(DyeColor.BLACK, PatternType.BORDER)
    .build()
```

`build()` automatically hides attributes so the banner tooltip stays clean.

---

## 5. Notes

- All four builders defer meta writes; **nothing is final until `build()`**.
- Names/lore are colorized for you — pass `&` codes, not `§`.
- Need to read a hidden tag back off an item later? See `ItemUtils.getItemID`,
  which the GUI system uses to route clicks (see [GUIs.md](GUIs.md)).
