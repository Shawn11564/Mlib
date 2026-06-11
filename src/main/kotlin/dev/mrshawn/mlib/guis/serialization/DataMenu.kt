package dev.mrshawn.mlib.guis.serialization

import dev.mrshawn.mlib.guis.Gui
import dev.mrshawn.mlib.guis.items.impl.BasicItem
import dev.mrshawn.mlib.guis.items.impl.UpdatingItem
import dev.mrshawn.mlib.guis.menus.Menu
import dev.mrshawn.mlib.guis.panes.Pane
import dev.mrshawn.mlib.guis.panes.impl.PaginatedPane
import dev.mrshawn.mlib.guis.types.ChestGui
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

/**
 * A [Menu] built entirely from a [MenuDTO]. This is the public, concrete, data-driven menu the
 * loader produces (mlib's own `Menu` is abstract). Built once per menu id and shared across
 * viewers, like any mlib menu.
 *
 * Note on `fillAreas`: mlib's [Gui.fillArea] treats width/height as COUNTS, while pane constructors
 * treat them as inclusive indices — the format always uses counts, so fill areas pass straight
 * through while [PaneFactory] does the conversion for panes.
 */
class DataMenu internal constructor(
    private val plugin: JavaPlugin,
    val id: String,
    private val dto: MenuDTO,
    private val ctx: MenuBuildContext
) : Menu() {

    override val gui: Gui = ChestGui(plugin, dto.title ?: " ", (dto.rows ?: 6).coerceIn(1, 6))

    override fun createGui() {
        dto.fill?.let { gui.fillWith(GuiItemFactory.create(it, ctx, this)) }

        dto.fillAreas?.forEach { area ->
            gui.fillArea(
                GuiItemFactory.create(area.item, ctx, this),
                area.x ?: 0,
                area.y ?: 0,
                (area.width ?: 1).coerceAtLeast(1),
                (area.height ?: 1).coerceAtLeast(1)
            )
        }

        dto.items?.forEach { node ->
            gui.addItem(GuiItemFactory.create(node, ctx, this), node.x ?: 0, node.y ?: 0)
        }

        dto.panes?.forEach { paneDto ->
            val pane = PaneFactory.create(paneDto, ctx, this)
            gui.addPane(pane)
            addPaneNavigation(paneDto, pane)
        }

        gui.update()
    }

    /** Wires optional next/prev/page-indicator buttons for a paginated pane (no-op for static panes). */
    private fun addPaneNavigation(dto: PaneDTO, pane: Pane) {
        val nav = dto.navigation ?: return
        if (pane !is PaginatedPane) return

        nav.prevButton?.let { button ->
            val slot = button.slot ?: return@let
            val stack = ItemMapper.toItemStack(button.item ?: ItemAppearanceDTO(material = "ARROW", name = "&cPrevious"), ctx.currentViewer)
            gui.addItem(BasicItem(stack) { event ->
                event.isCancelled = true
                pane.decrement()
                gui.update()
            }, slot.x ?: 0, slot.y ?: 0)
        }

        nav.nextButton?.let { button ->
            val slot = button.slot ?: return@let
            val stack = ItemMapper.toItemStack(button.item ?: ItemAppearanceDTO(material = "ARROW", name = "&aNext"), ctx.currentViewer)
            gui.addItem(BasicItem(stack) { event ->
                event.isCancelled = true
                pane.increment()
                gui.update()
            }, slot.x ?: 0, slot.y ?: 0)
        }

        nav.pageIndicator?.let { slot ->
            // UpdatingItem so the "x / y" text refreshes as the page changes.
            gui.addItem(UpdatingItem({ pane.getPageDisplayItem().getDisplayedItem() }), slot.x ?: 0, slot.y ?: 0)
        }
    }

    /** Links navigation once every menu in the project exists. Called by [MenuLoader]. */
    internal fun linkNavigation(previous: DataMenu?, next: DataMenu?) {
        this.previousMenu = previous
        this.nextMenu = next
    }

    /** Opens this menu for [player], recording them as the current viewer for placeholder resolution. */
    fun open(player: Player) {
        ctx.currentViewer = player
        show(player)
    }

}
