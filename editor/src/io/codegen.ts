import type { Action, ActionSet, Condition, ItemAppearance, ItemNode, Menu, MenuProject } from '../model/types'

// Generates mlib Kotlin source from a project — a scaffold you paste into your plugin. The
// declarative YAML/JSON export is the full-fidelity format; codegen covers the common path and
// leaves TODOs for things that need code (custom actions, complex conditions).

const kstr = (s: string | undefined) =>
  '"' +
  (s ?? '')
    .replace(/\\/g, '\\\\')
    .replace(/"/g, '\\"')
    .replace(/\$/g, '\\$')
    .replace(/\n/g, '\\n') +
  '"'

const pascal = (id: string) =>
  id
    .split(/[^a-zA-Z0-9]+/)
    .filter(Boolean)
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join('') || 'Menu'

const className = (id: string) => pascal(id) + 'Menu'

function builderExpr(app: ItemAppearance | undefined): string {
  if (!app) return 'ItemBuilder(Material.STONE).build()'
  const mat = (app.material ?? 'STONE').toUpperCase()
  let base: string
  if (mat === 'PLAYER_HEAD' && app.skull?.owner) base = `SkullBuilder(${kstr(app.skull.owner)})`
  else if (mat === 'POTION') base = 'PotionBuilder()'
  else if (mat.endsWith('_BANNER')) base = `BannerBuilder(BannerType.${mat.replace('_BANNER', '')})`
  else base = `ItemBuilder(Material.${mat}${app.amount && app.amount > 1 ? `, ${app.amount}` : ''})`

  const calls: string[] = []
  if (app.name !== undefined) calls.push(`.name(${kstr(app.name)})`)
  for (const line of app.lore ?? []) calls.push(`.addLoreLine(${kstr(line)})`)
  for (const e of app.enchantments ?? [])
    calls.push(`.addEnchantment(Enchantment.getByKey(NamespacedKey.minecraft(${kstr((e.type ?? '').toLowerCase())}))!!, ${e.level ?? 1})`)
  if (app.glow) calls.push('.glow()')
  if (app.customModelData != null) calls.push(`.setCustomModelData(${app.customModelData})`)
  for (const f of app.itemFlags ?? []) calls.push(`.addItemFlag(ItemFlag.${f.toUpperCase()})`)
  if (app.hideAttributes) calls.push('.hideAttributes()')
  return base + calls.join('') + '.build()'
}

function conditionExpr(c: Condition | undefined): string {
  if (!c) return 'true'
  if (typeof c !== 'string') return 'true /* TODO: complex condition */'
  const s = c.trim()
  if (s.toLowerCase() === 'op') return '(e.whoClicked.isOp)'
  const [prefix, rest] = s.split(/:(.*)/)
  switch (prefix.toLowerCase()) {
    case 'permission':
      return `(e.whoClicked as Player).hasPermission(${kstr(rest)})`
    case 'gamemode':
      return `((e.whoClicked as Player).gameMode.name.equals(${kstr(rest)}, true))`
    case 'world':
      return `((e.whoClicked as Player).world.name.equals(${kstr(rest)}, true))`
    default:
      return `true /* TODO: condition ${s} */`
  }
}

function actionStmt(a: Action, indent: string): string {
  const cmd = (a.command ?? '').replace(/^\//, '')
  switch (a.type) {
    case 'RUN_COMMAND':
      return `${indent}Bukkit.dispatchCommand(${a.as === 'CONSOLE' ? 'Bukkit.getConsoleSender()' : 'e.whoClicked'}, ${kstr(cmd)})`
    case 'MESSAGE':
      return `${indent}Chat.${a.mini ? 'tellMini' : 'tell'}(e.whoClicked, ${kstr(a.text)})`
    case 'BROADCAST':
      return a.permission
        ? `${indent}Chat.broadcastPermission(${kstr(a.text)}, ${kstr(a.permission)})`
        : `${indent}Chat.broadcast(${kstr(a.text)})`
    case 'ACTIONBAR':
      return `${indent}Chat.tellActionbar(e.whoClicked, ${kstr(a.text)})`
    case 'TITLE':
      return `${indent}Chat.sendTitle(e.whoClicked, ${kstr(a.title)}, ${kstr(a.subtitle)}, ${a.fadeIn ?? 10}, ${a.stay ?? 70}, ${a.fadeOut ?? 20})`
    case 'PLAY_SOUND':
      return `${indent}SoundUtils.playSound(e.whoClicked as Player, Sound.${(a.sound ?? 'UI_BUTTON_CLICK').toUpperCase()}, ${a.volume ?? 1}f, ${a.pitch ?? 1}f)`
    case 'OPEN_MENU':
      return `${indent}${a.menu ? `${className(a.menu)}(plugin).show(e.whoClicked as Player)` : '/* OPEN_MENU: no target */'}`
    case 'CLOSE':
      return `${indent}e.whoClicked.closeInventory()`
    case 'BACK':
      return `${indent}back(e.whoClicked)`
    case 'NEXT':
      return `${indent}next(e.whoClicked)`
    case 'GIVE_ITEM':
      return `${indent}(e.whoClicked as Player).inventory.addItem(${builderExpr(a.item)})`
    case 'CONSOLE_LOG':
      return `${indent}Chat.${a.level === 'SEVERE' ? 'severe' : a.level === 'WARN' ? 'warn' : 'log'}(${kstr(a.text)})`
    case 'CANCEL':
      return `${indent}e.isCancelled = true`
    case 'ALLOW':
      return `${indent}e.isCancelled = false`
    case 'CONDITIONAL':
      return `${indent}if (${conditionExpr(a.if)}) {\n${(a.then ?? []).map((x) => actionStmt(x, indent + '    ')).join('\n')}\n${indent}} else {\n${(a.else ?? []).map((x) => actionStmt(x, indent + '    ')).join('\n')}\n${indent}}`
    case 'CUSTOM':
      return `${indent}// TODO custom action: MenuHooks.registerAction(${kstr(a.id)}) { ctx -> /* ... */ }`
    default:
      return `${indent}// ${a.type}`
  }
}

const CLICK_GUARD: Record<string, string> = {
  left: 'e.isLeftClick && !e.isShiftClick',
  right: 'e.isRightClick && !e.isShiftClick',
  shiftLeft: 'e.isLeftClick && e.isShiftClick',
  shiftRight: 'e.isRightClick && e.isShiftClick',
  middle: 'e.click == ClickType.MIDDLE',
  doubleClick: 'e.click == ClickType.DOUBLE_CLICK',
  drop: 'e.click == ClickType.DROP',
}

function clickLambda(actions: ActionSet | undefined, indent: string): string {
  const a = actions ?? {}
  const body: string[] = [`${indent}    e.isCancelled = true`]

  const branches = (['left', 'right', 'shiftLeft', 'shiftRight', 'middle', 'doubleClick', 'drop'] as const).filter(
    (b) => a[b]?.length,
  )

  const inner = (i2: string) => {
    const lines: string[] = []
    if (branches.length) {
      lines.push(`${i2}when {`)
      for (const b of branches) {
        lines.push(`${i2}    ${CLICK_GUARD[b]} -> {`)
        lines.push((a[b] ?? []).map((x) => actionStmt(x, i2 + '        ')).join('\n'))
        lines.push(`${i2}    }`)
      }
      lines.push(`${i2}    else -> {`)
      lines.push((a.default ?? []).map((x) => actionStmt(x, i2 + '        ')).join('\n'))
      lines.push(`${i2}    }`)
      lines.push(`${i2}}`)
    } else {
      lines.push((a.default ?? []).map((x) => actionStmt(x, i2)).join('\n'))
    }
    return lines.filter((l) => l.trim().length).join('\n')
  }

  if (a.requirements?.length) {
    const guard = a.requirements.map(conditionExpr).join(' && ')
    body.push(`${indent}    if (${guard}) {`)
    body.push(inner(indent + '        '))
    body.push(`${indent}    } else {`)
    body.push(`${indent}        Chat.tell(e.whoClicked, "&cYou can't do that.")`)
    body.push(`${indent}    }`)
  } else {
    body.push(inner(indent + '    '))
  }

  return `{ e ->\n${body.filter((l) => l.trim().length).join('\n')}\n${indent}}`
}

function guiItemExpr(node: ItemNode, indent: string): string {
  const kind = node.kind ?? 'basic'
  switch (kind) {
    case 'toggle':
      return `ToggleItem(\n${indent}    BasicItem(${builderExpr(node.on?.item)}, ${clickLambda(node.on?.actions, indent + '    ')}),\n${indent}    BasicItem(${builderExpr(node.off?.item)}, ${clickLambda(node.off?.actions, indent + '    ')}),\n${indent}    ${node.initiallyToggledOn ?? true}, ${node.toggleOnClick ?? true}\n${indent})`
    case 'cycle':
      return `CycleItem(\n${(node.states ?? [])
        .map((s) => `${indent}    BasicItem(${builderExpr(s.item)}, ${clickLambda(s.actions, indent + '    ')})`)
        .join(',\n')}\n${indent})`
    case 'twoStage':
      return `TwoStageItem(${builderExpr(node.item)}, ${clickLambda(node.secondClickActions, indent)}, ${node.glowOnFirstClick ?? true})`
    case 'updating':
      return `UpdatingItem({ ${builderExpr(node.item)} }, ${clickLambda(node.actions, indent)})`
    case 'glowing':
      return `GlowingItem(${builderExpr(node.item)}, ${clickLambda(node.actions, indent)})`
    default:
      return `BasicItem(${builderExpr(node.item)}, ${clickLambda(node.actions, indent)})`
  }
}

function menuClass(id: string, menu: Menu, pkg: string): string {
  const ind = '        '
  const lines: string[] = []
  lines.push(`class ${className(id)}(private val plugin: JavaPlugin) : Menu() {`)
  lines.push('')
  lines.push(`    override val gui = ChestGui(plugin, ${kstr(menu.title ?? ' ')}, ${menu.rows ?? 6})`)
  lines.push('')
  lines.push('    override fun createGui() {')
  if (menu.fill?.item) lines.push(`${ind}gui.fillWith(BasicItem(${builderExpr(menu.fill.item)}))`)
  for (const fa of menu.fillAreas ?? [])
    lines.push(`${ind}gui.fillArea(BasicItem(${builderExpr(fa.item?.item)}), ${fa.x}, ${fa.y}, ${fa.width}, ${fa.height})`)
  for (const node of menu.items ?? [])
    lines.push(`${ind}gui.addItem(${guiItemExpr(node, ind)}, ${node.x ?? 0}, ${node.y ?? 0})`)
  if (menu.panes?.length) lines.push(`${ind}// NOTE: ${menu.panes.length} pane(s) omitted from codegen — see the YAML export.`)
  lines.push(`${ind}gui.update()`)
  lines.push('    }')
  lines.push('}')
  return lines.join('\n')
}

export function toKotlin(project: MenuProject, pkg = 'your.plugin.menus'): string {
  const header = [
    `package ${pkg}`,
    '',
    'import dev.mrshawn.mlib.chat.Chat',
    'import dev.mrshawn.mlib.guis.items.impl.*',
    'import dev.mrshawn.mlib.guis.menus.Menu',
    'import dev.mrshawn.mlib.guis.serialization.MenuHooks',
    'import dev.mrshawn.mlib.guis.types.ChestGui',
    'import dev.mrshawn.mlib.items.BannerType',
    'import dev.mrshawn.mlib.items.builders.*',
    'import dev.mrshawn.mlib.sounds.SoundUtils',
    'import org.bukkit.Bukkit',
    'import org.bukkit.Material',
    'import org.bukkit.NamespacedKey',
    'import org.bukkit.Sound',
    'import org.bukkit.enchantments.Enchantment',
    'import org.bukkit.entity.Player',
    'import org.bukkit.event.inventory.ClickType',
    'import org.bukkit.inventory.ItemFlag',
    'import org.bukkit.plugin.java.JavaPlugin',
    '',
    '// Generated by the mlib menu editor. The declarative YAML/JSON export is the full-fidelity',
    '// format; this scaffold covers the common path — wire up CUSTOM actions and review TODOs.',
    '',
  ].join('\n')

  const classes = Object.entries(project.menus)
    .map(([id, menu]) => menuClass(id, menu, pkg))
    .join('\n\n')

  return `${header}\n${classes}\n`
}
