import { create } from 'zustand'
import type { ItemKind, ItemNode, Menu, MenuProject } from '../model/types'
import { newItemNode, newMenu, uniqueMenuId } from '../model/factory'
import { newProject } from '../model/factory'

interface Selection {
  x: number
  y: number
}

const STORAGE_KEY = 'mlib.editor.project'

function loadFromStorage(): MenuProject | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    const p = JSON.parse(raw) as MenuProject
    if (p && p.menus && Object.keys(p.menus).length) return p
  } catch {
    /* ignore corrupt autosave */
  }
  return null
}

interface EditorState {
  project: MenuProject
  activeMenuId: string
  selected: Selection | null

  // queries
  activeMenu: () => Menu
  itemAt: (x: number, y: number) => ItemNode | undefined
  selectedItem: () => ItemNode | undefined

  // project
  loadProject: (p: MenuProject) => void
  resetProject: () => void

  // menus
  setActiveMenu: (id: string) => void
  addMenu: (title?: string) => void
  deleteMenu: (id: string) => void
  renameMenu: (oldId: string, newId: string) => void
  patchActiveMenu: (patch: Partial<Menu>) => void

  // selection
  select: (x: number, y: number) => void
  deselect: () => void

  // items
  placeMaterial: (x: number, y: number, material: string) => void
  placeNode: (x: number, y: number, node: ItemNode) => void
  moveItem: (fromX: number, fromY: number, toX: number, toY: number) => void
  removeItem: (x: number, y: number) => void
  updateSelectedItem: (mutate: (node: ItemNode) => void) => void
}

const initial = loadFromStorage() ?? newProject()
const firstMenuId = Object.keys(initial.menus)[0]

function findIndexAt(menu: Menu, x: number, y: number): number {
  if (!menu.items) return -1
  return menu.items.findIndex((i) => (i.x ?? 0) === x && (i.y ?? 0) === y)
}

export const useStore = create<EditorState>((set, get) => {
  /** Clone the project, run fn against the active menu, and commit. */
  const mutateActiveMenu = (fn: (menu: Menu, project: MenuProject) => void) => {
    set((s) => {
      const next = structuredClone(s.project)
      const menu = next.menus[s.activeMenuId]
      if (!menu) return {}
      if (!menu.items) menu.items = []
      fn(menu, next)
      return { project: next }
    })
  }

  return {
    project: initial,
    activeMenuId: firstMenuId,
    selected: null,

    activeMenu: () => get().project.menus[get().activeMenuId],
    itemAt: (x, y) => {
      const menu = get().project.menus[get().activeMenuId]
      return menu?.items?.find((i) => (i.x ?? 0) === x && (i.y ?? 0) === y)
    },
    selectedItem: () => {
      const sel = get().selected
      if (!sel) return undefined
      return get().itemAt(sel.x, sel.y)
    },

    loadProject: (p) =>
      set({ project: p, activeMenuId: Object.keys(p.menus)[0], selected: null }),

    resetProject: () => {
      const p = newProject()
      set({ project: p, activeMenuId: Object.keys(p.menus)[0], selected: null })
    },

    setActiveMenu: (id) => set({ activeMenuId: id, selected: null }),

    addMenu: (title = '&8New Menu') =>
      set((s) => {
        const next = structuredClone(s.project)
        const id = uniqueMenuId(title, Object.keys(next.menus))
        next.menus[id] = newMenu(title, 3)
        return { project: next, activeMenuId: id, selected: null }
      }),

    deleteMenu: (id) =>
      set((s) => {
        if (Object.keys(s.project.menus).length <= 1) return {}
        const next = structuredClone(s.project)
        delete next.menus[id]
        // scrub references
        for (const m of Object.values(next.menus)) {
          if (m.previousMenu === id) m.previousMenu = null
          if (m.nextMenu === id) m.nextMenu = null
        }
        const activeMenuId = s.activeMenuId === id ? Object.keys(next.menus)[0] : s.activeMenuId
        return { project: next, activeMenuId, selected: null }
      }),

    renameMenu: (oldId, newId) =>
      set((s) => {
        if (oldId === newId || !newId || s.project.menus[newId]) return {}
        const next = structuredClone(s.project)
        const rekeyed: Record<string, Menu> = {}
        for (const [k, v] of Object.entries(next.menus)) rekeyed[k === oldId ? newId : k] = v
        next.menus = rekeyed
        for (const m of Object.values(next.menus)) {
          if (m.previousMenu === oldId) m.previousMenu = newId
          if (m.nextMenu === oldId) m.nextMenu = newId
        }
        return {
          project: next,
          activeMenuId: s.activeMenuId === oldId ? newId : s.activeMenuId,
        }
      }),

    patchActiveMenu: (patch) => mutateActiveMenu((menu) => Object.assign(menu, patch)),

    select: (x, y) => set({ selected: { x, y } }),
    deselect: () => set({ selected: null }),

    placeMaterial: (x, y, material) => {
      mutateActiveMenu((menu) => {
        const idx = findIndexAt(menu, x, y)
        const node = newItemNode(material, x, y)
        if (idx >= 0) menu.items![idx] = node
        else menu.items!.push(node)
      })
      set({ selected: { x, y } })
    },

    placeNode: (x, y, node) => {
      mutateActiveMenu((menu) => {
        const placed = { ...node, x, y }
        const idx = findIndexAt(menu, x, y)
        if (idx >= 0) menu.items![idx] = placed
        else menu.items!.push(placed)
      })
      set({ selected: { x, y } })
    },

    moveItem: (fromX, fromY, toX, toY) => {
      if (fromX === toX && fromY === toY) return
      mutateActiveMenu((menu) => {
        const fromIdx = findIndexAt(menu, fromX, fromY)
        if (fromIdx < 0) return
        // remove anything already at the destination
        const toIdx = findIndexAt(menu, toX, toY)
        if (toIdx >= 0) menu.items!.splice(toIdx, 1)
        const moving = menu.items!.find((i) => (i.x ?? 0) === fromX && (i.y ?? 0) === fromY)!
        moving.x = toX
        moving.y = toY
      })
      set({ selected: { x: toX, y: toY } })
    },

    removeItem: (x, y) => {
      mutateActiveMenu((menu) => {
        const idx = findIndexAt(menu, x, y)
        if (idx >= 0) menu.items!.splice(idx, 1)
      })
      set((s) => (s.selected && s.selected.x === x && s.selected.y === y ? { selected: null } : {}))
    },

    updateSelectedItem: (mutate) => {
      const sel = get().selected
      if (!sel) return
      mutateActiveMenu((menu) => {
        const idx = findIndexAt(menu, sel.x, sel.y)
        if (idx >= 0) mutate(menu.items![idx])
      })
    },
  }
})

// Debounced autosave to localStorage.
let saveTimer: ReturnType<typeof setTimeout> | undefined
useStore.subscribe((state) => {
  clearTimeout(saveTimer)
  saveTimer = setTimeout(() => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(state.project))
    } catch {
      /* storage full / unavailable */
    }
  }, 400)
})

export type { ItemKind }
