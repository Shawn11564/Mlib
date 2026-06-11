import { useMemo, useState } from 'react'
import { useDraggable } from '@dnd-kit/core'
import { MATERIALS, type MaterialEntry } from '../assets-pipeline/textures'
import { useStore } from '../state/store'
import ItemIcon from './ItemIcon'

const LIMIT = 500

type Cat = 'all' | 'item' | 'block'

export default function PaletteBrowser() {
  const [q, setQ] = useState('')
  const [cat, setCat] = useState<Cat>('all')

  const filtered = useMemo(() => {
    const query = q.trim().toLowerCase()
    let list: MaterialEntry[] = MATERIALS
    if (cat !== 'all') list = list.filter((m) => m.category === cat)
    if (query) {
      list = list.filter(
        (m) =>
          m.material.toLowerCase().includes(query) ||
          m.name.toLowerCase().includes(query) ||
          m.id.includes(query),
      )
    }
    return list
  }, [q, cat])

  const shown = filtered.slice(0, LIMIT)

  return (
    <div className="flex h-full w-60 shrink-0 flex-col border-r border-black/40 bg-[#262626]">
      <div className="space-y-2 border-b border-black/40 p-2">
        <div className="text-xs font-semibold uppercase tracking-wide text-gray-400">Assets</div>
        <input
          value={q}
          onChange={(e) => setQ(e.target.value)}
          placeholder="Search materials…"
          className="w-full rounded bg-[#1b1b1b] px-2 py-1 text-sm outline-none ring-1 ring-black/40 focus:ring-emerald-500"
        />
        <div className="flex gap-1 text-xs">
          {(['all', 'item', 'block'] as Cat[]).map((c) => (
            <button
              key={c}
              onClick={() => setCat(c)}
              className={`flex-1 rounded px-2 py-1 capitalize ${
                cat === c ? 'bg-emerald-600 text-white' : 'bg-[#1b1b1b] text-gray-300 hover:bg-[#333]'
              }`}
            >
              {c}
            </button>
          ))}
        </div>
      </div>

      <div className="scroll-thin grid flex-1 grid-cols-5 content-start gap-1 overflow-y-auto p-2">
        {shown.map((m) => (
          <PaletteTile key={`${m.category}:${m.id}`} entry={m} />
        ))}
      </div>

      <div className="border-t border-black/40 p-2 text-[10px] text-gray-500">
        {filtered.length > LIMIT ? `Showing ${LIMIT} of ${filtered.length} — refine search` : `${filtered.length} materials`}
      </div>
    </div>
  )
}

function PaletteTile({ entry }: { entry: MaterialEntry }) {
  const { attributes, listeners, setNodeRef, isDragging } = useDraggable({ id: `palette:${entry.material}` })
  const selected = useStore((s) => s.selected)
  const placeMaterial = useStore((s) => s.placeMaterial)

  return (
    <button
      ref={setNodeRef}
      {...attributes}
      {...listeners}
      title={entry.material}
      onClick={() => selected && placeMaterial(selected.x, selected.y, entry.material)}
      className={`mc-slot flex h-11 w-11 items-center justify-center ${isDragging ? 'opacity-40' : ''}`}
    >
      <ItemIcon material={entry.material} size={28} />
    </button>
  )
}
