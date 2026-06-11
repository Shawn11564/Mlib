import { type ReactNode, useState } from 'react'
import { useDraggable, useDroppable } from '@dnd-kit/core'
import type { ItemNode } from '../model/types'
import { displayAppearance, isGlowing, KIND_BADGE } from '../lib/render'
import ItemIcon from './ItemIcon'
import ItemTooltip from './ItemTooltip'

interface Props {
  x: number
  y: number
  node?: ItemNode
  /** Background filler shown (dimmed) when the slot has no item. */
  fillMaterial?: string
  selected: boolean
  onSelect: () => void
}

export default function Slot({ x, y, node, fillMaterial, selected, onSelect }: Props) {
  const { setNodeRef, isOver } = useDroppable({ id: `slot:${x}:${y}` })
  const [hover, setHover] = useState(false)
  const app = displayAppearance(node)
  const badge = node?.kind && node.kind !== 'basic' ? KIND_BADGE[node.kind] : undefined

  return (
    <div
      ref={setNodeRef}
      onClick={onSelect}
      onMouseEnter={() => setHover(true)}
      onMouseLeave={() => setHover(false)}
      className={[
        'mc-slot relative flex h-12 w-12 cursor-pointer items-center justify-center',
        node ? '' : 'mc-slot--empty',
        isOver ? 'outline outline-2 outline-emerald-400' : '',
        selected ? 'z-10 ring-2 ring-yellow-300' : '',
      ].join(' ')}
    >
      {node && app ? (
        <DraggableItem x={x} y={y}>
          <div className={`relative ${isGlowing(node) ? 'mc-glow' : ''}`}>
            <ItemIcon material={app.material} size={32} />
            {(app.amount ?? 1) > 1 && <span className="mc-amount">{app.amount}</span>}
          </div>
        </DraggableItem>
      ) : (
        fillMaterial && <ItemIcon material={fillMaterial} size={32} className="opacity-25" />
      )}

      {badge && (
        <span className="absolute left-0 top-0 bg-black/70 px-[2px] text-[8px] font-bold leading-none text-emerald-300">
          {badge}
        </span>
      )}

      {hover && app && (
        <div className="absolute bottom-full left-1/2 z-50 mb-1 -translate-x-1/2">
          <ItemTooltip appearance={app} />
        </div>
      )}
    </div>
  )
}

function DraggableItem({ x, y, children }: { x: number; y: number; children: ReactNode }) {
  const { attributes, listeners, setNodeRef, isDragging } = useDraggable({ id: `item:${x}:${y}` })
  return (
    <div ref={setNodeRef} {...attributes} {...listeners} className={isDragging ? 'opacity-30' : ''}>
      {children}
    </div>
  )
}
