import { useState } from 'react'
import {
  DndContext,
  DragOverlay,
  PointerSensor,
  useSensor,
  useSensors,
  type DragEndEvent,
  type DragStartEvent,
} from '@dnd-kit/core'
import { useStore } from '../state/store'
import TopBar from './TopBar'
import MenuTabs from './MenuTabs'
import PaletteBrowser from './PaletteBrowser'
import InventoryCanvas from './InventoryCanvas'
import InspectorPanel from './InspectorPanel'
import ChainGraph from './ChainGraph'
import ItemIcon from './ItemIcon'

export default function App() {
  const placeMaterial = useStore((s) => s.placeMaterial)
  const moveItem = useStore((s) => s.moveItem)
  const [dragMaterial, setDragMaterial] = useState<string | null>(null)
  const [showGraph, setShowGraph] = useState(false)

  // distance constraint so a click selects but a drag (>5px) moves/places.
  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 5 } }))

  const onDragStart = (e: DragStartEvent) => {
    const id = String(e.active.id)
    setDragMaterial(id.startsWith('palette:') ? id.slice('palette:'.length) : null)
  }

  const onDragEnd = (e: DragEndEvent) => {
    setDragMaterial(null)
    const overId = e.over ? String(e.over.id) : null
    if (!overId?.startsWith('slot:')) return
    const [, sx, sy] = overId.split(':')
    const x = Number(sx)
    const y = Number(sy)
    const activeId = String(e.active.id)
    if (activeId.startsWith('palette:')) {
      placeMaterial(x, y, activeId.slice('palette:'.length))
    } else if (activeId.startsWith('item:')) {
      const [, ix, iy] = activeId.split(':')
      moveItem(Number(ix), Number(iy), x, y)
    }
  }

  return (
    <DndContext sensors={sensors} onDragStart={onDragStart} onDragEnd={onDragEnd}>
      <div className="relative flex h-full flex-col">
        <TopBar onShowGraph={() => setShowGraph(true)} />
        <MenuTabs />
        <div className="flex min-h-0 flex-1">
          <PaletteBrowser />
          <InventoryCanvas />
          <InspectorPanel />
        </div>
        {showGraph && <ChainGraph onClose={() => setShowGraph(false)} />}
      </div>
      <DragOverlay>
        {dragMaterial ? (
          <div className="mc-slot flex h-12 w-12 items-center justify-center">
            <ItemIcon material={dragMaterial} size={32} />
          </div>
        ) : null}
      </DragOverlay>
    </DndContext>
  )
}
