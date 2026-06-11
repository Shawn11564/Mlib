import { useMemo } from 'react'
import ReactFlow, { Background, Controls, MarkerType, type Edge, type Node } from 'reactflow'
import 'reactflow/dist/style.css'
import { useStore } from '../state/store'
import type { Action, ActionSet, ItemNode, Menu } from '../model/types'
import { stripMcCodes } from '../lib/mcText'

/** All menu ids referenced by OPEN_MENU actions anywhere in a menu. */
function collectOpenTargets(menu: Menu): Set<string> {
  const out = new Set<string>()
  const walkAction = (a: Action) => {
    if (a.type === 'OPEN_MENU' && a.menu) out.add(a.menu)
    a.then?.forEach(walkAction)
    a.else?.forEach(walkAction)
  }
  const walkSet = (set?: ActionSet) => {
    if (!set) return
    ;(['default', 'left', 'right', 'shiftLeft', 'shiftRight', 'middle', 'drop', 'doubleClick', 'denyActions'] as const).forEach(
      (k) => set[k]?.forEach(walkAction),
    )
  }
  const walkNode = (n?: ItemNode) => {
    if (!n) return
    walkSet(n.actions)
    walkSet(n.secondClickActions)
    walkNode(n.on)
    walkNode(n.off)
    n.states?.forEach(walkNode)
  }
  menu.items?.forEach(walkNode)
  menu.fillAreas?.forEach((fa) => walkNode(fa.item))
  menu.panes?.forEach((p) => p.contents?.forEach(walkNode))
  return out
}

export default function ChainGraph({ onClose }: { onClose: () => void }) {
  const project = useStore((s) => s.project)
  const activeMenuId = useStore((s) => s.activeMenuId)
  const setActiveMenu = useStore((s) => s.setActiveMenu)

  const ids = Object.keys(project.menus)

  const nodes: Node[] = useMemo(
    () =>
      ids.map((id, i) => ({
        id,
        position: { x: (i % 4) * 240, y: Math.floor(i / 4) * 150 },
        data: { label: `${stripMcCodes(project.menus[id].title) || id}\n(${id})` },
        style: {
          padding: 8,
          borderRadius: 8,
          border: id === activeMenuId ? '2px solid #34d399' : '1px solid #555',
          background: id === activeMenuId ? '#10231c' : '#2b2b2b',
          color: '#e6e6e6',
          fontSize: 12,
          whiteSpace: 'pre-line',
          width: 180,
          textAlign: 'center',
        },
      })),
    [ids, project, activeMenuId],
  )

  const edges: Edge[] = useMemo(() => {
    const es: Edge[] = []
    const arrow = { markerEnd: { type: MarkerType.ArrowClosed } }
    for (const id of ids) {
      const m = project.menus[id]
      if (m.nextMenu && project.menus[m.nextMenu])
        es.push({ id: `${id}-next`, source: id, target: m.nextMenu, label: 'next', animated: true, style: { stroke: '#34d399' }, ...arrow })
      if (m.previousMenu && project.menus[m.previousMenu])
        es.push({ id: `${id}-prev`, source: id, target: m.previousMenu, label: 'prev', style: { stroke: '#60a5fa', strokeDasharray: '5 3' }, ...arrow })
      for (const t of collectOpenTargets(m))
        if (project.menus[t]) es.push({ id: `${id}-open-${t}`, source: id, target: t, label: 'open', style: { stroke: '#9ca3af', strokeDasharray: '2 2' }, ...arrow })
    }
    return es
  }, [ids, project])

  return (
    <div className="absolute inset-0 z-40 flex flex-col bg-[#181818]">
      <div className="flex items-center justify-between border-b border-black/40 px-3 py-2">
        <div className="text-sm font-semibold">
          Menu chaining <span className="text-[11px] text-gray-500">— click a node to open it · solid green = next · blue = prev · gray = OPEN_MENU</span>
        </div>
        <button className="rounded px-2 py-1 text-sm text-gray-300 hover:bg-[#2a2a2a]" onClick={onClose}>
          ✕ Close
        </button>
      </div>
      <div className="flex-1">
        <ReactFlow nodes={nodes} edges={edges} onNodeClick={(_, n) => setActiveMenu(n.id)} fitView proOptions={{ hideAttribution: true }}>
          <Background color="#333" />
          <Controls />
        </ReactFlow>
      </div>
    </div>
  )
}
