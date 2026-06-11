package dev.mrshawn.mlib.guis.serialization

import dev.mrshawn.mlib.guis.panes.Pane
import dev.mrshawn.mlib.guis.panes.impl.PaginatedPane
import dev.mrshawn.mlib.guis.panes.impl.StaticPane

/**
 * Builds a [Pane] from a [PaneDTO].
 *
 * The format stores `width`/`height` as **sizes** (column/row counts). mlib's pane constructors,
 * however, treat those args as the **inclusive last index** the pane covers (its fill/render loops
 * run `x until width+1`). We convert here: `lastCol = x + width - 1`, `lastRow = y + height - 1`.
 * With the matching [PaginatedPane] page-size fix, this makes both pane types behave consistently
 * for any `x`/`y` origin.
 */
internal object PaneFactory {

    fun create(dto: PaneDTO, ctx: MenuBuildContext, owningMenu: DataMenu): Pane {
        val x = dto.x ?: 0
        val y = dto.y ?: 0
        val sizeW = (dto.width ?: 1).coerceAtLeast(1)
        val sizeH = (dto.height ?: 1).coerceAtLeast(1)
        val lastCol = x + sizeW - 1
        val lastRow = y + sizeH - 1
        val clearOld = dto.clearOld ?: true
        val priority = runCatching { Pane.Priority.valueOf((dto.priority ?: "NORMAL").trim().uppercase()) }
            .getOrDefault(Pane.Priority.NORMAL)

        val contents = (dto.contents ?: emptyList()).map { GuiItemFactory.create(it, ctx, owningMenu) }

        return when ((dto.kind ?: "static").lowercase()) {
            "paginated" -> PaginatedPane(x, y, lastCol, lastRow, clearOld, priority).also { it.fillWith(contents) }
            else -> StaticPane(x, y, lastCol, lastRow, clearOld, priority).also { it.fillWith(contents) }
        }
    }

}
