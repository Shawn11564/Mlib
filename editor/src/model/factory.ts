import { FORMAT_VERSION, type ItemNode, type Menu, type MenuProject } from './types'

/** Slot index from grid coords, matching mlib (`x + y * 9`). */
export const slotIndex = (x: number, y: number) => x + y * 9
export const slotX = (index: number) => index % 9
export const slotY = (index: number) => Math.floor(index / 9)

export function newItemNode(material = 'STONE', x = 0, y = 0): ItemNode {
  return {
    x,
    y,
    kind: 'basic',
    item: { material, amount: 1 },
    actions: {},
  }
}

export function newMenu(title = '&8New Menu', rows = 3): Menu {
  return {
    title,
    type: 'chest',
    rows,
    items: [],
  }
}

export function newProject(): MenuProject {
  return {
    formatVersion: FORMAT_VERSION,
    project: 'my-menus',
    menus: {
      main_menu: newMenu('&8Main Menu', 3),
    },
  }
}

/** A short, file-safe id from a display name; ensures uniqueness against existing ids. */
export function uniqueMenuId(base: string, existing: Iterable<string>): string {
  const taken = new Set(existing)
  const slug = base
    .toLowerCase()
    .replace(/&[0-9a-fk-or#]/g, '')
    .replace(/[^a-z0-9]+/g, '_')
    .replace(/^_+|_+$/g, '') || 'menu'
  if (!taken.has(slug)) return slug
  let i = 2
  while (taken.has(`${slug}_${i}`)) i++
  return `${slug}_${i}`
}
