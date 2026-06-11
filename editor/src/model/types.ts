// In-memory model for the editor. Mirrors the Kotlin DTOs (MenuFormat.kt) and the JSON Schema
// (schema/menu-schema.json) field-for-field, so export/import is essentially an identity transform.
//
// FORMAT_VERSION is mirrored in MenuFormat.kt (FORMAT_VERSION) and schema/menu-schema.json. Keep in sync.
export const FORMAT_VERSION = 1

export type ItemKind = 'basic' | 'glowing' | 'updating' | 'toggle' | 'cycle' | 'twoStage'

export const ITEM_KINDS: ItemKind[] = ['basic', 'glowing', 'updating', 'toggle', 'cycle', 'twoStage']

export type ActionType =
  | 'RUN_COMMAND'
  | 'MESSAGE'
  | 'BROADCAST'
  | 'ACTIONBAR'
  | 'TITLE'
  | 'PLAY_SOUND'
  | 'OPEN_MENU'
  | 'CLOSE'
  | 'BACK'
  | 'NEXT'
  | 'GIVE_ITEM'
  | 'CONSOLE_LOG'
  | 'CONDITIONAL'
  | 'CANCEL'
  | 'ALLOW'
  | 'CUSTOM'

export const ACTION_TYPES: ActionType[] = [
  'OPEN_MENU', 'BACK', 'NEXT', 'CLOSE',
  'RUN_COMMAND', 'MESSAGE', 'BROADCAST', 'ACTIONBAR', 'TITLE', 'PLAY_SOUND',
  'GIVE_ITEM', 'CONSOLE_LOG', 'CONDITIONAL', 'CANCEL', 'ALLOW', 'CUSTOM',
]

export type ClickBranch =
  | 'default'
  | 'left'
  | 'right'
  | 'shiftLeft'
  | 'shiftRight'
  | 'middle'
  | 'drop'
  | 'doubleClick'

export const CLICK_BRANCHES: ClickBranch[] = [
  'default', 'left', 'right', 'shiftLeft', 'shiftRight', 'middle', 'drop', 'doubleClick',
]

export interface Enchantment {
  type: string
  level?: number
}

export interface Skull {
  owner?: string
  texture?: string
}

export interface PotionEffect {
  type: string
  duration?: number
  amplifier?: number
}

export interface Potion {
  color?: string
  effects?: PotionEffect[]
}

export interface BannerPattern {
  color: string
  pattern: string
}

export interface Banner {
  patterns?: BannerPattern[]
}

export interface ItemAppearance {
  material: string
  amount?: number
  name?: string
  lore?: string[]
  enchantments?: Enchantment[]
  glow?: boolean
  customModelData?: number
  itemFlags?: string[]
  hideAttributes?: boolean
  skull?: Skull
  potion?: Potion
  banner?: Banner
}

export type Condition =
  | string
  | {
      type?: 'permission' | 'placeholder' | 'gamemode' | 'world' | 'op' | 'custom' | 'all' | 'any' | 'not'
      value?: string
      all?: Condition[]
      any?: Condition[]
      not?: Condition
    }

export interface Action {
  type: ActionType
  command?: string
  as?: 'PLAYER' | 'CONSOLE'
  text?: string
  lines?: string[]
  mini?: boolean
  permission?: string
  title?: string
  subtitle?: string
  fadeIn?: number
  stay?: number
  fadeOut?: number
  sound?: string
  volume?: number
  pitch?: number
  menu?: string
  item?: ItemAppearance
  amount?: number
  level?: 'INFO' | 'WARN' | 'SEVERE'
  id?: string
  data?: Record<string, unknown>
  if?: Condition
  then?: Action[]
  else?: Action[]
}

export interface ActionSet {
  requirements?: Condition[]
  denyActions?: Action[]
  default?: Action[]
  left?: Action[]
  right?: Action[]
  shiftLeft?: Action[]
  shiftRight?: Action[]
  middle?: Action[]
  drop?: Action[]
  doubleClick?: Action[]
}

export interface ItemNode {
  x?: number
  y?: number
  kind?: ItemKind
  item?: ItemAppearance
  actions?: ActionSet
  // glowing
  glowCondition?: Condition
  // toggle
  initiallyToggledOn?: boolean
  toggleOnClick?: boolean
  on?: ItemNode
  off?: ItemNode
  // cycle
  cycleOnClick?: boolean
  states?: ItemNode[]
  // twoStage
  glowOnFirstClick?: boolean
  secondClickActions?: ActionSet
}

export interface FillArea {
  x: number
  y: number
  width: number
  height: number
  item: ItemNode
}

export interface NavSlot {
  x: number
  y: number
}

export interface NavButton {
  slot?: NavSlot
  item?: ItemAppearance
}

export interface PaneNav {
  nextButton?: NavButton
  prevButton?: NavButton
  pageIndicator?: NavSlot
}

export type PaneKind = 'static' | 'paginated'

export interface Pane {
  kind?: PaneKind
  x: number
  y: number
  width: number
  height: number
  priority?: 'HIGHEST' | 'HIGH' | 'NORMAL' | 'LOW' | 'LOWEST'
  clearOld?: boolean
  contents?: ItemNode[]
  navigation?: PaneNav
}

export interface Menu {
  title?: string
  type?: 'chest'
  rows?: number
  previousMenu?: string | null
  nextMenu?: string | null
  fill?: ItemNode
  fillAreas?: FillArea[]
  items?: ItemNode[]
  panes?: Pane[]
}

export interface MenuProject {
  formatVersion: number
  project?: string
  menus: Record<string, Menu>
}
