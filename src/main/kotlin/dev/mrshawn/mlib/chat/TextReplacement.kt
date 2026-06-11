package dev.mrshawn.mlib.chat

/**
 * An ordered set of literal find/replace pairs, applied in insertion order.
 *
 * Insertion order matters: replacements run top-to-bottom, so a later pair can act on
 * text introduced by an earlier one. Build with the constructors, the [of] factories,
 * or the fluent [addReplacement].
 *
 * ```kotlin
 * val r = TextReplacement.of("%player%" to "Steve", "%coins%" to "100")
 * r.replace("%player% has %coins% coins")   // "Steve has 100 coins"
 * ```
 */
class TextReplacement {

	private val replacements: MutableMap<String, String> = LinkedHashMap()

	constructor()

	constructor(key: String, value: String) {
		addReplacement(key, value)
	}

	constructor(vararg replacements: Pair<String, String>) {
		replacements.forEach { addReplacement(it) }
	}

	companion object {
		fun of(key: String, value: String): TextReplacement = TextReplacement(key, value)
		fun of(vararg replacements: Pair<String, String>): TextReplacement = TextReplacement(*replacements)
	}

	fun addReplacement(key: String, value: String): TextReplacement {
		replacements[key] = value
		return this
	}

	fun addReplacement(replacement: Pair<String, String>): TextReplacement =
		addReplacement(replacement.first, replacement.second)

	fun replace(input: String): String {
		var output = input
		replacements.forEach { (key, value) -> output = output.replace(key, value) }
		return output
	}

	/** An immutable snapshot of the configured replacements, in application order. */
	fun getReplacements(): Map<String, String> = LinkedHashMap(replacements)

}
