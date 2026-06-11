import { useStore } from '../state/store'
import type { ItemNode } from '../model/types'
import Slot from './Slot'
import { parseMcText } from '../lib/mcText'
import { displayAppearance } from '../lib/render'

export default function InventoryCanvas() {
  const project = useStore((s) => s.project)
  const activeMenuId = useStore((s) => s.activeMenuId)
  const selected = useStore((s) => s.selected)
  const select = useStore((s) => s.select)
  const deselect = useStore((s) => s.deselect)

  const menu = project.menus[activeMenuId]
  const rows = Math.min(Math.max(menu.rows ?? 6, 1), 6)
  const items = menu.items ?? []

  const map = new Map<number, ItemNode>()
  for (const it of items) map.set((it.x ?? 0) + (it.y ?? 0) * 9, it)
  const fillMaterial = displayAppearance(menu.fill)?.material

  return (
    <div className="flex flex-1 flex-col items-center justify-center gap-4 overflow-auto p-6" onClick={deselect}>
      <div className="text-xl font-semibold" onClick={(e) => e.stopPropagation()}>
        {menu.title ? parseMcText(menu.title) : <span className="text-gray-500">(untitled)</span>}
      </div>

      <div className="inline-block rounded-md bg-[#c6c6c6] p-2 shadow-2xl" onClick={(e) => e.stopPropagation()}>
        <div className="grid gap-1" style={{ gridTemplateColumns: 'repeat(9, 3rem)' }}>
          {Array.from({ length: rows * 9 }, (_, idx) => {
            const x = idx % 9
            const y = Math.floor(idx / 9)
            const node = map.get(idx)
            // Ghost the auto nav buttons mlib injects at runtime when chaining is set.
            const ghost = node
              ? undefined
              : idx === 0 && menu.previousMenu
                ? 'RED_WOOL'
                : idx === 8 && menu.nextMenu
                  ? 'LIME_WOOL'
                  : fillMaterial
            return (
              <Slot
                key={idx}
                x={x}
                y={y}
                node={node}
                fillMaterial={ghost}
                selected={!!selected && selected.x === x && selected.y === y}
                onSelect={() => select(x, y)}
              />
            )
          })}
        </div>
      </div>

      <div className="text-xs text-gray-500">
        {rows} × 9 &middot; click a slot to select &middot; drag assets from the left
      </div>
    </div>
  )
}
