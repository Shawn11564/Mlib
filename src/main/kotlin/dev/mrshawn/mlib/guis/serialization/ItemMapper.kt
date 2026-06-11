package dev.mrshawn.mlib.guis.serialization

import dev.mrshawn.mlib.chat.Chat
import dev.mrshawn.mlib.items.BannerType
import dev.mrshawn.mlib.items.ItemUtils
import dev.mrshawn.mlib.items.builders.BannerBuilder
import dev.mrshawn.mlib.items.builders.ItemBuilder
import dev.mrshawn.mlib.items.builders.PotionBuilder
import dev.mrshawn.mlib.items.builders.SkullBuilder
import org.bukkit.Color
import org.bukkit.DyeColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.OfflinePlayer
import org.bukkit.block.banner.PatternType
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.potion.PotionEffectType
import java.util.UUID

/**
 * Maps an [ItemAppearanceDTO] onto a Bukkit [ItemStack] using mlib's builder family
 * ([ItemBuilder], [SkullBuilder], [PotionBuilder], [BannerBuilder]).
 *
 * Resolution is defensive: an unknown material/enchantment/flag is logged and skipped (or replaced
 * with a labelled `BARRIER`) rather than aborting the whole menu, so designers see their mistake
 * in-game instead of an empty inventory.
 */
internal object ItemMapper {

    fun toItemStack(appearance: ItemAppearanceDTO?, viewer: OfflinePlayer? = null): ItemStack {
        if (appearance == null) return ItemStack(Material.STONE)

        val materialName = (appearance.material ?: "STONE").trim()

        // Code-provided dynamic icon: material: "hook:<id>" resolved via MenuHooks.registerItem.
        if (materialName.startsWith("hook:", ignoreCase = true)) {
            val id = materialName.substring(5)
            val provider = MenuHooks.getItem(id)
            val player = viewer as? Player
            if (provider != null && player != null) return provider.apply(player)
            return badItem("unresolved item hook '$id'")
        }

        val material = resolveMaterial(materialName) ?: return badItem("unknown material '$materialName'")
        val amount = (appearance.amount ?: 1).coerceIn(1, 64)
        val builder = chooseBuilder(material, amount, appearance)

        appearance.name?.let { builder.name(papi(viewer, it)) }
        appearance.lore?.let { lore -> builder.addLoreLines(lore.map { papi(viewer, it) }) }
        appearance.enchantments?.forEach { ench ->
            val enchantment = resolveEnchantment(ench.type) ?: return@forEach
            builder.addEnchantment(enchantment, ench.level ?: 1)
        }
        if (appearance.glow == true) builder.glow()
        appearance.customModelData?.let { builder.setCustomModelData(it) }
        appearance.itemFlags?.forEach { flagName -> resolveFlag(flagName)?.let { builder.addItemFlag(it) } }
        if (appearance.hideAttributes == true) builder.hideAttributes()

        val stack = builder.build()
        // PotionBuilder only ever produces Material.POTION; apply color/effects to splash/lingering here.
        applyPotionMetaIfNeeded(stack, material, appearance.potion)
        return stack
    }

    // ---------------------------------------------------------------------------------------------
    // Builder selection
    // ---------------------------------------------------------------------------------------------

    private fun chooseBuilder(material: Material, amount: Int, appearance: ItemAppearanceDTO): ItemBuilder = when {
        material == Material.PLAYER_HEAD && appearance.skull != null -> buildSkull(appearance.skull, amount)
        material == Material.POTION && appearance.potion != null -> buildPotion(appearance.potion)
        material.name.endsWith("_BANNER") && appearance.banner != null -> buildBanner(material, appearance.banner)
        else -> ItemBuilder(material, amount)
    }

    private fun buildSkull(skull: SkullDTO, amount: Int): ItemBuilder {
        val owner = skull.owner
        if (!owner.isNullOrBlank()) {
            if (owner.startsWith("mhf:", ignoreCase = true)) {
                val head = runCatching { ItemUtils.MhfHeads.valueOf(owner.substring(4).trim().uppercase()) }.getOrNull()
                if (head != null) return SkullBuilder(head, amount)
                Chat.warn("[mlib] unknown MHF head '$owner' -> using a plain head")
            } else {
                val uuid = runCatching { UUID.fromString(owner) }.getOrNull()
                return if (uuid != null) SkullBuilder(uuid, amount) else SkullBuilder(owner, amount)
            }
        }
        if (skull.texture != null) {
            // Base64 textures need version-specific profile handling; defer to a custom item hook for now.
            Chat.warn("[mlib] base64 skull textures aren't supported by the loader yet; use MenuHooks.registerItem. Falling back to a plain head.")
        }
        return ItemBuilder(Material.PLAYER_HEAD, amount)
    }

    private fun buildPotion(potion: PotionDTO): ItemBuilder {
        val builder = PotionBuilder()
        potion.color?.let { hex -> parseColor(hex)?.let { builder.setPotionColor(it) } }
        potion.effects?.forEach { eff ->
            val type = resolvePotionEffect(eff.type) ?: return@forEach
            builder.addPotionEffect(type, eff.duration ?: 200, eff.amplifier ?: 0)
        }
        return builder
    }

    private fun buildBanner(material: Material, banner: BannerDTO): ItemBuilder {
        val type = BannerType.values().find { it.banner == material } ?: BannerType.WHITE
        val builder = BannerBuilder(type)
        banner.patterns?.forEach { p ->
            val color = runCatching { DyeColor.valueOf((p.color ?: "WHITE").trim().uppercase()) }.getOrNull() ?: return@forEach
            val pattern = resolvePattern(p.pattern) ?: return@forEach
            builder.addPattern(color, pattern)
        }
        return builder
    }

    private fun applyPotionMetaIfNeeded(stack: ItemStack, material: Material, potion: PotionDTO?) {
        if (potion == null || material == Material.POTION) return
        if (material != Material.SPLASH_POTION && material != Material.LINGERING_POTION) return
        val meta = stack.itemMeta as? PotionMeta ?: return
        potion.color?.let { hex -> parseColor(hex)?.let { meta.color = it } }
        potion.effects?.forEach { eff ->
            val type = resolvePotionEffect(eff.type) ?: return@forEach
            meta.addCustomEffect(type.createEffect(eff.duration ?: 200, eff.amplifier ?: 0), true)
        }
        stack.itemMeta = meta
    }

    // ---------------------------------------------------------------------------------------------
    // Resolvers
    // ---------------------------------------------------------------------------------------------

    fun resolveMaterial(name: String): Material? =
        Material.matchMaterial(name) ?: runCatching { Material.valueOf(name.trim().uppercase()) }.getOrNull()

    private fun resolveEnchantment(name: String?): Enchantment? {
        if (name.isNullOrBlank()) return null
        val n = name.trim()
        runCatching { Enchantment.getByKey(NamespacedKey.minecraft(n.lowercase())) }.getOrNull()?.let { return it }
        @Suppress("DEPRECATION")
        return runCatching { Enchantment.getByName(n.uppercase()) }.getOrNull()
    }

    private fun resolveFlag(name: String?): ItemFlag? {
        if (name.isNullOrBlank()) return null
        return runCatching { ItemFlag.valueOf(name.trim().uppercase()) }.getOrNull()
    }

    private fun resolvePotionEffect(name: String?): PotionEffectType? {
        if (name.isNullOrBlank()) return null
        return PotionEffectType.getByName(name.trim().uppercase())
    }

    // PatternType is an enum on the spigot-api the library targets; runCatching also shields us from
    // newer servers where it became a registry (NoSuchMethodError is a Throwable and is caught).
    private fun resolvePattern(name: String?): PatternType? {
        if (name.isNullOrBlank()) return null
        return runCatching { PatternType.valueOf(name.trim().uppercase()) }.getOrNull()
    }

    private fun parseColor(hex: String): Color? {
        val cleaned = hex.trim().removePrefix("#")
        val rgb = runCatching { cleaned.toInt(16) }.getOrNull() ?: return null
        return runCatching { Color.fromRGB(rgb) }.getOrNull()
    }

    private fun papi(viewer: OfflinePlayer?, text: String): String = Chat.setPlaceholders(viewer, text)

    private fun badItem(reason: String): ItemStack {
        Chat.warn("[mlib] menu item: $reason -> using BARRIER")
        return ItemBuilder(Material.BARRIER).name("&c$reason").build()
    }

}
