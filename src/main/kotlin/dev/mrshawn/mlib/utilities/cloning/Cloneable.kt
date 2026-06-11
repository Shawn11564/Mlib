package dev.mrshawn.mlib.utilities.cloning

/**
 * Contract for types that can be deep-copied.
 *
 * Two complementary operations are provided:
 *
 *  - [clone] returns a brand-new, fully independent instance.
 *  - [copyFrom] overwrites *this* instance's mutable state from another instance,
 *    which is useful for refreshing a long-lived object in place (e.g. reloading a
 *    cached object without swapping the reference held elsewhere).
 *
 * Implementations are expected to be **deep**: nested mutable structures should be
 * copied, not shared, so the result can be mutated without affecting the original.
 *
 * Note: this is intentionally distinct from [kotlin.Cloneable]/`java.lang.Cloneable`,
 * which are marker interfaces with no typed contract.
 *
 * ```kotlin
 * data class Kit(val name: String, val items: MutableList<ItemStack>) : Cloneable<Kit> {
 *     override fun clone(): Kit = Kit(name, items.map { it.clone() }.toMutableList())
 *     override fun copyFrom(other: Kit) {
 *         items.clear()
 *         items.addAll(other.items.map { it.clone() })
 *     }
 * }
 * ```
 */
interface Cloneable<T> {

	/** Returns a new, independent deep copy of this instance. */
	fun clone(): T

	/** Overwrites this instance's state with a deep copy of [other]'s state. */
	fun copyFrom(other: T)

}
