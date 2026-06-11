import type { ItemAppearance, ItemNode } from '../model/types'

/** The appearance a node shows at rest in the canvas/preview (toggle -> on, cycle -> first state). */
export function displayAppearance(node: ItemNode | undefined): ItemAppearance | undefined {
  if (!node) return undefined
  switch (node.kind) {
    case 'toggle':
      return node.on?.item ?? node.item
    case 'cycle':
      return node.states?.[0]?.item ?? node.item
    default:
      return node.item
  }
}

export function isGlowing(node: ItemNode | undefined): boolean {
  if (!node) return false
  if (node.kind === 'glowing') return true
  const app = displayAppearance(node)
  return !!app?.glow || (app?.enchantments?.length ?? 0) > 0
}

/** Short human label for an item kind, for canvas corner badges. */
export const KIND_BADGE: Record<string, string> = {
  glowing: 'G',
  updating: 'U',
  toggle: 'T',
  cycle: 'C',
  twoStage: '2',
}
