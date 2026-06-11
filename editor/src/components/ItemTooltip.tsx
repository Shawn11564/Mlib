import { parseMcText } from '../lib/mcText'
import type { ItemAppearance } from '../model/types'

/** A Minecraft-style hover tooltip showing an item's name and lore with color codes applied. */
export default function ItemTooltip({ appearance }: { appearance?: ItemAppearance }) {
  if (!appearance) return null
  const lore = appearance.lore ?? []
  return (
    <div className="pointer-events-none w-max max-w-xs rounded border border-[#250a45] bg-[#100010]/95 px-2 py-1 shadow-xl">
      <div className="text-sm leading-tight">
        {appearance.name ? parseMcText(appearance.name) : <span className="text-gray-100">{appearance.material}</span>}
      </div>
      {lore.map((line, i) => (
        <div key={i} className="text-xs leading-tight">
          {parseMcText(line)}
        </div>
      ))}
    </div>
  )
}
