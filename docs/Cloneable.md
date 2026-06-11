# Mlib Cloneable

`Cloneable<T>` (package `utilities.cloning`) is a small, typed contract for
**deep-copying** your own objects. It's deliberately distinct from
`kotlin.Cloneable` / `java.lang.Cloneable`, which are marker interfaces with no
typed method — this one actually defines how to copy.

It exposes two complementary operations:

| Method | Does | Use when |
|--------|------|----------|
| `clone(): T` | returns a brand-new, independent copy | you need a fresh instance to hand out or mutate |
| `copyFrom(other: T)` | overwrites *this* instance's state from `other` | you must refresh a long-lived object **in place** without swapping the reference others hold |

Implementations are expected to be **deep**: copy nested mutable structures rather
than sharing them, so the copy can be mutated without affecting the original.

---

## Implementing it

```kotlin
import dev.mrshawn.mlib.utilities.cloning.Cloneable
import org.bukkit.inventory.ItemStack

data class Kit(
    val name: String,
    val items: MutableList<ItemStack>
) : Cloneable<Kit> {

    override fun clone(): Kit =
        Kit(name, items.map { it.clone() }.toMutableList())   // deep: each ItemStack cloned

    override fun copyFrom(other: Kit) {
        items.clear()
        items.addAll(other.items.map { it.clone() })
    }
}
```

---

## Why both methods?

`clone()` and `copyFrom()` solve different problems:

```kotlin
// clone(): produce a throwaway copy to mutate, leaving the template intact
val working = template.clone()
working.items.add(bonusItem)        // template is untouched

// copyFrom(): mutate an object others already reference
val live = activeKits[player]       // handed out elsewhere, can't reassign
live.copyFrom(reloadedKit)          // everyone holding `live` sees the refresh
```

Use `copyFrom` for cached singletons / shared instances you reload at runtime (it
pairs naturally with config reloads — see [Files.md](Files.md)). Use `clone` when
you want an independent instance.

> **Note:** this is an interface-only contract — there's no reflection-based
> auto-copy. Each implementor writes its own copy logic, which keeps copying
> explicit and predictable (important around Bukkit types like `ItemStack` and
> `Location` that need their own `.clone()`).
