package dev.mrshawn.mlib.guis.serialization

/**
 * Thrown when a menu file cannot be parsed or its [MenuProjectDTO.formatVersion] is unsupported.
 *
 * Field-level problems (an unknown material, a bad enchantment name, a dangling menu reference)
 * are **not** fatal — they are logged via [dev.mrshawn.mlib.chat.Chat] and degraded gracefully
 * (e.g. a `BARRIER` placeholder) so the menu still opens. This exception is reserved for failures
 * that make the whole document unusable.
 */
class MenuParseException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
