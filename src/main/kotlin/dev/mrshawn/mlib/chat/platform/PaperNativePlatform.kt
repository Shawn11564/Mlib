package dev.mrshawn.mlib.chat.platform

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.bukkit.command.CommandSender
import java.lang.reflect.Method
import java.time.Duration

/**
 * Delivers components through the **server's native Adventure** (Paper and forks),
 * bypassing adventure-platform-bukkit.
 *
 * Because this library shades and relocates Adventure, our [Component] type is *not*
 * the same class as the server's native one. We bridge the gap by serializing our
 * component to JSON with our (relocated) [GsonComponentSerializer], re-parsing it with
 * the server's native `GsonComponentSerializer`, and invoking the native `Audience`
 * methods reflectively. All native types are touched only via reflection, so this class
 * loads fine on servers that don't ship Adventure (it simply isn't selected there).
 *
 * Use [isAvailable] to check support before constructing.
 */
class PaperNativePlatform : ChatPlatform {

	private val ourGson: GsonComponentSerializer = GsonComponentSerializer.gson()

	private val nativeGson: Any
	private val nativeDeserialize: Method
	private val sendMessageMethod: Method
	private val sendActionBarMethod: Method
	private val showTitleMethod: Method
	private val titleFactory: Method
	private val timesFactory: Method

	init {
		val componentClass = Class.forName(COMPONENT)
		val gsonSerializerClass = Class.forName(GSON_SERIALIZER)
		nativeGson = gsonSerializerClass.getMethod("gson").invoke(null)
		// ComponentSerializer#deserialize(R) erases its type parameter to Object, so look the
		// method up by name/arity rather than getMethod("deserialize", String::class.java).
		nativeDeserialize = gsonSerializerClass.methods.first {
			it.name == "deserialize" && it.parameterCount == 1 &&
				it.parameterTypes[0].isAssignableFrom(String::class.java)
		}

		val senderClass = CommandSender::class.java
		sendMessageMethod = senderClass.getMethod("sendMessage", componentClass)
		sendActionBarMethod = senderClass.getMethod("sendActionBar", componentClass)

		val titleClass = Class.forName(TITLE)
		val timesClass = Class.forName(TIMES)
		showTitleMethod = senderClass.getMethod("showTitle", titleClass)
		titleFactory = titleClass.getMethod("title", componentClass, componentClass, timesClass)
		timesFactory = try {
			timesClass.getMethod("times", Duration::class.java, Duration::class.java, Duration::class.java)
		} catch (e: NoSuchMethodException) {
			// Older Adventure used Times.of(...)
			timesClass.getMethod("of", Duration::class.java, Duration::class.java, Duration::class.java)
		}
	}

	/** Converts our relocated component into the server's native component via JSON. */
	private fun toNative(component: Component): Any =
		nativeDeserialize.invoke(nativeGson, ourGson.serialize(component))

	override fun sendMessage(sender: CommandSender, component: Component) {
		sendMessageMethod.invoke(sender, toNative(component))
	}

	override fun sendActionBar(sender: CommandSender, component: Component) {
		sendActionBarMethod.invoke(sender, toNative(component))
	}

	override fun showTitle(
		sender: CommandSender,
		title: Component,
		subtitle: Component,
		fadeIn: Int,
		stay: Int,
		fadeOut: Int
	) {
		val times = timesFactory.invoke(null, ticks(fadeIn), ticks(stay), ticks(fadeOut))
		val nativeTitle = titleFactory.invoke(null, toNative(title), toNative(subtitle), times)
		showTitleMethod.invoke(sender, nativeTitle)
	}

	override fun close() {
		// Native audiences are owned by the server; nothing to release.
	}

	private fun ticks(ticks: Int): Duration = Duration.ofMillis(ticks * 50L)

	companion object {
		// These class names are assembled at runtime from separate fragments so the shade
		// plugin's `net.kyori` -> `dev.mrshawn.mlib.libs.kyori` relocation does NOT rewrite
		// them. We need the SERVER's (non-relocated) Adventure classes here, not our own —
		// a literal "net.kyori..." string in this class would otherwise be relocated too.
		private val ADVENTURE: String = listOf("net", "kyori", "adventure").joinToString(".")
		private val COMPONENT: String = "$ADVENTURE.text.Component"
		private val GSON_SERIALIZER: String = "$ADVENTURE.text.serializer.gson.GsonComponentSerializer"
		private val TITLE: String = "$ADVENTURE.title.Title"
		private val TIMES: String = "$ADVENTURE.title.Title\$Times"

		/** True when the server exposes native Adventure that accepts components directly (Paper/forks). */
		fun isAvailable(): Boolean = try {
			val componentClass = Class.forName(COMPONENT)
			Class.forName(GSON_SERIALIZER)
			// Confirm the server's CommandSender actually accepts native components.
			CommandSender::class.java.getMethod("sendMessage", componentClass)
			true
		} catch (e: Throwable) {
			false
		}
	}

}
