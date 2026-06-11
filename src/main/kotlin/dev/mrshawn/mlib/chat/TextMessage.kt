package dev.mrshawn.mlib.chat

import org.bukkit.command.CommandSender

/**
 * A reusable, multi-line message with deferred placeholder/token replacement.
 *
 * Build it up with [addMessages] and [addReplacements], then [send] it (or pass it to
 * [Chat.tell]). Replacements are applied lazily in [getMessage], so the same template
 * can be reused with different replacements.
 */
class TextMessage {

	private val messages = ArrayList<String>()
	private val replacements = ArrayList<TextReplacement>()

	fun addMessages(vararg messages: String?): TextMessage {
		messages.forEach { if (it != null) this.messages.add(it) }
		return this
	}

	fun addMessages(vararg messages: List<String>?): TextMessage {
		messages.forEach { if (it != null) this.messages.addAll(it) }
		return this
	}

	fun addReplacements(vararg replacements: TextReplacement?): TextMessage {
		replacements.forEach { if (it != null) this.replacements.add(it) }
		return this
	}

	private fun replaceAll(input: String): String {
		var output = input
		replacements.forEach { output = it.replace(output) }
		return output
	}

	/** Returns the lines with all replacements applied. */
	fun getMessage(): List<String> = messages.map { replaceAll(it) }

	/** Sends this message to [toWhom] via [Chat.tell]. */
	fun send(toWhom: CommandSender?) {
		Chat.tell(toWhom, this)
	}

}
