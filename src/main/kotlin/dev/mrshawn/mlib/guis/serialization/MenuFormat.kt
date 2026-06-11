package dev.mrshawn.mlib.guis.serialization

import com.google.gson.annotations.SerializedName

/**
 * The current major version of the declarative "mlib menu format".
 *
 * This constant is mirrored in two other places that MUST stay in sync:
 *  - `schema/menu-schema.json` (`formatVersion.const`)
 *  - `editor/src/model/types.ts` (`FORMAT_VERSION`)
 *
 * [MenuLoader] treats it as a *major* version: unknown fields are ignored (forward-compatible),
 * but a document declaring a higher major is rejected.
 */
const val FORMAT_VERSION = 1

// ---------------------------------------------------------------------------------------------
// Wire DTOs
//
// These mirror the JSON Schema 1:1 and are populated by Gson. EVERY field is nullable on purpose:
// Gson instantiates Kotlin classes via Unsafe and does NOT honour Kotlin default values, so a
// missing JSON key leaves a field at its JVM default (null / 0 / false) regardless of the declared
// default. Declaring everything nullable makes that explicit and the mappers coalesce to real
// defaults (see ItemMapper / GuiItemFactory / PaneFactory). Do not "tidy" these into non-null
// types — that reintroduces NPE landmines when a key is absent.
// ---------------------------------------------------------------------------------------------

data class MenuProjectDTO(
    val formatVersion: Int? = null,
    val project: String? = null,
    val menus: Map<String, MenuDTO>? = null
)

data class MenuDTO(
    val title: String? = null,
    val type: String? = null,
    val rows: Int? = null,
    val previousMenu: String? = null,
    val nextMenu: String? = null,
    val fill: ItemNodeDTO? = null,
    val fillAreas: List<FillAreaDTO>? = null,
    val items: List<ItemNodeDTO>? = null,
    val panes: List<PaneDTO>? = null
)

data class FillAreaDTO(
    val x: Int? = null,
    val y: Int? = null,
    val width: Int? = null,
    val height: Int? = null,
    val item: ItemNodeDTO? = null
)

/**
 * A clickable item placed in a menu. [kind] selects which [dev.mrshawn.mlib.guis.items.GuiItem]
 * implementation is built; the kind-specific fields below are only read for that kind.
 */
data class ItemNodeDTO(
    val x: Int? = null,
    val y: Int? = null,
    val kind: String? = null,             // basic | glowing | updating | toggle | cycle | twoStage
    val item: ItemAppearanceDTO? = null,
    val actions: ActionSetDTO? = null,
    // glowing
    val glowCondition: ConditionDTO? = null,
    // toggle
    val initiallyToggledOn: Boolean? = null,
    val toggleOnClick: Boolean? = null,
    val on: ItemNodeDTO? = null,
    val off: ItemNodeDTO? = null,
    // cycle
    val cycleOnClick: Boolean? = null,
    val states: List<ItemNodeDTO>? = null,
    // twoStage
    val glowOnFirstClick: Boolean? = null,
    val secondClickActions: ActionSetDTO? = null
)

data class ItemAppearanceDTO(
    val material: String? = null,
    val amount: Int? = null,
    val name: String? = null,
    val lore: List<String>? = null,
    val enchantments: List<EnchantmentDTO>? = null,
    val glow: Boolean? = null,
    val customModelData: Int? = null,
    val itemFlags: List<String>? = null,
    val hideAttributes: Boolean? = null,
    val skull: SkullDTO? = null,
    val potion: PotionDTO? = null,
    val banner: BannerDTO? = null
)

data class EnchantmentDTO(val type: String? = null, val level: Int? = null)

/** Skull owner: a player [owner] (name/uuid/`mhf:NAME`) OR a base64 [texture] value. */
data class SkullDTO(val owner: String? = null, val texture: String? = null)

data class PotionDTO(val color: String? = null, val effects: List<PotionEffectDTO>? = null)
data class PotionEffectDTO(val type: String? = null, val duration: Int? = null, val amplifier: Int? = null)

data class BannerDTO(val patterns: List<BannerPatternDTO>? = null)
data class BannerPatternDTO(val color: String? = null, val pattern: String? = null)

/**
 * A pane region. [width]/[height] are sizes (column/row COUNTS), 1-based; [PaneFactory] converts
 * them to mlib's inclusive last-index pane constructor convention.
 */
data class PaneDTO(
    val kind: String? = null,             // static | paginated
    val x: Int? = null,
    val y: Int? = null,
    val width: Int? = null,
    val height: Int? = null,
    val priority: String? = null,         // HIGHEST | HIGH | NORMAL | LOW | LOWEST
    val clearOld: Boolean? = null,
    val contents: List<ItemNodeDTO>? = null,
    val navigation: PaneNavDTO? = null
)

data class PaneNavDTO(
    val nextButton: NavButtonDTO? = null,
    val prevButton: NavButtonDTO? = null,
    val pageIndicator: NavSlotDTO? = null
)

data class NavButtonDTO(val slot: NavSlotDTO? = null, val item: ItemAppearanceDTO? = null)
data class NavSlotDTO(val x: Int? = null, val y: Int? = null)

/**
 * Per-item actions. [requirements] gate the whole set; on failure [denyActions] run. The click
 * is routed to the matching click-type branch (falling back to [default]). Each branch is an
 * ordered list of [ActionDTO] steps.
 */
data class ActionSetDTO(
    val requirements: List<ConditionDTO>? = null,
    val denyActions: List<ActionDTO>? = null,
    val default: List<ActionDTO>? = null,
    val left: List<ActionDTO>? = null,
    val right: List<ActionDTO>? = null,
    val shiftLeft: List<ActionDTO>? = null,
    val shiftRight: List<ActionDTO>? = null,
    val middle: List<ActionDTO>? = null,
    val drop: List<ActionDTO>? = null,
    val doubleClick: List<ActionDTO>? = null
)

/**
 * A single action step. This is intentionally a flat "union" record: [type] is the discriminator
 * and only the relevant fields are read by [ActionExecutor]. This avoids a polymorphic Gson
 * adapter while keeping the wire format readable.
 */
data class ActionDTO(
    val type: String? = null,
    // RUN_COMMAND
    val command: String? = null,
    @SerializedName("as") val runAs: String? = null,   // PLAYER | CONSOLE
    // MESSAGE / BROADCAST / ACTIONBAR
    val text: String? = null,
    val lines: List<String>? = null,
    val mini: Boolean? = null,
    val permission: String? = null,
    // TITLE
    val title: String? = null,
    val subtitle: String? = null,
    val fadeIn: Int? = null,
    val stay: Int? = null,
    val fadeOut: Int? = null,
    // PLAY_SOUND
    val sound: String? = null,
    val volume: Float? = null,
    val pitch: Float? = null,
    // OPEN_MENU
    val menu: String? = null,
    // GIVE_ITEM
    val item: ItemAppearanceDTO? = null,
    val amount: Int? = null,
    // CONSOLE_LOG
    val level: String? = null,            // INFO | WARN | SEVERE
    // CUSTOM
    val id: String? = null,
    val data: Map<String, Any?>? = null,
    // CONDITIONAL
    @SerializedName("if") val condition: ConditionDTO? = null,
    @SerializedName("then") val thenActions: List<ActionDTO>? = null,
    @SerializedName("else") val elseActions: List<ActionDTO>? = null
)

/**
 * A requirement / condition. Built by [ConditionDeserializer], which accepts BOTH a shorthand
 * string (`"permission:menus.use"`, `"op"`, `"gamemode:CREATIVE"`) and an object form
 * (`{ all: [...] }`, `{ any: [...] }`, `{ not: ... }`, `{ type, value }`). Because it is produced
 * by a custom deserializer (not Gson reflection), the Kotlin defaults below DO apply.
 */
data class ConditionDTO(
    val type: String = "",                // permission | placeholder | gamemode | world | op | custom | all | any | not
    val value: String? = null,
    val children: List<ConditionDTO> = emptyList(),
    val child: ConditionDTO? = null
)
