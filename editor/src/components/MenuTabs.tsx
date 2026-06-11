import { useStore } from '../state/store'
import { stripMcCodes } from '../lib/mcText'

export default function MenuTabs() {
  const project = useStore((s) => s.project)
  const activeMenuId = useStore((s) => s.activeMenuId)
  const setActiveMenu = useStore((s) => s.setActiveMenu)
  const addMenu = useStore((s) => s.addMenu)

  const ids = Object.keys(project.menus)

  return (
    <div className="flex items-center gap-1 overflow-x-auto border-b border-black/40 bg-[#222] px-2 py-1">
      {ids.map((id) => {
        const menu = project.menus[id]
        const label = stripMcCodes(menu.title) || id
        return (
          <button
            key={id}
            onClick={() => setActiveMenu(id)}
            title={id}
            className={`whitespace-nowrap rounded-t px-3 py-1 text-sm ${
              id === activeMenuId ? 'bg-[#2f2f2f] text-white' : 'text-gray-400 hover:bg-[#2a2a2a]'
            }`}
          >
            {label}
            <span className="ml-1 text-[10px] text-gray-500">{id}</span>
          </button>
        )
      })}
      <button
        onClick={() => addMenu('&8New Menu')}
        className="ml-1 rounded px-2 py-1 text-sm text-emerald-400 hover:bg-[#2a2a2a]"
        title="Add a menu"
      >
        + Menu
      </button>
    </div>
  )
}
