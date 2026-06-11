import { useStore } from '../state/store'
import type { ActionSet, ItemAppearance } from '../model/types'
import type { Mut } from '../lib/mutate'
import AppearanceForm from './AppearanceForm'
import ActionsEditor from './ActionsEditor'
import { Button, Checkbox, SectionTitle, TextInput } from './ui'

/** Kind-specific controls for the selected item (glow condition, toggle states, cycle states, etc.). */
export default function ItemKindEditor() {
  const selected = useStore((s) => s.selected)
  const project = useStore((s) => s.project)
  const activeMenuId = useStore((s) => s.activeMenuId)
  const update = useStore((s) => s.updateSelectedItem)

  if (!selected) return null
  const node = project.menus[activeMenuId]?.items?.find((i) => (i.x ?? 0) === selected.x && (i.y ?? 0) === selected.y)
  if (!node) return null
  const kind = node.kind ?? 'basic'

  const onApp: Mut<ItemAppearance> = (fn) =>
    update((n) => {
      n.on ??= { kind: 'basic' }
      n.on.item ??= { material: 'LIME_DYE' }
      fn(n.on.item)
    })
  const onActions: Mut<ActionSet> = (fn) =>
    update((n) => {
      n.on ??= { kind: 'basic' }
      n.on.actions ??= {}
      fn(n.on.actions)
    })
  const offApp: Mut<ItemAppearance> = (fn) =>
    update((n) => {
      n.off ??= { kind: 'basic' }
      n.off.item ??= { material: 'GRAY_DYE' }
      fn(n.off.item)
    })
  const offActions: Mut<ActionSet> = (fn) =>
    update((n) => {
      n.off ??= { kind: 'basic' }
      n.off.actions ??= {}
      fn(n.off.actions)
    })

  if (kind === 'glowing') {
    return (
      <div className="space-y-2 rounded border border-black/40 p-2">
        <SectionTitle>Glow condition</SectionTitle>
        <TextInput
          placeholder="permission:vip | op | (blank = always)"
          value={typeof node.glowCondition === 'string' ? node.glowCondition : node.glowCondition ? JSON.stringify(node.glowCondition) : ''}
          onChange={(e) => update((n) => (n.glowCondition = e.target.value || undefined))}
        />
      </div>
    )
  }

  if (kind === 'updating') {
    return (
      <div className="rounded border border-black/40 p-2 text-[11px] text-gray-400">
        Updating item: its appearance is rebuilt every render — use placeholders (e.g. <code>%server_online%</code>) in
        the name/lore.
      </div>
    )
  }

  if (kind === 'toggle') {
    return (
      <div className="space-y-2">
        <div className="flex gap-4">
          <Checkbox checked={node.initiallyToggledOn ?? true} onChange={(v) => update((n) => (n.initiallyToggledOn = v))} label="Starts ON" />
          <Checkbox checked={node.toggleOnClick ?? true} onChange={(v) => update((n) => (n.toggleOnClick = v))} label="Flip on click" />
        </div>
        <div className="rounded border border-emerald-900/50 p-2">
          <SectionTitle>ON state</SectionTitle>
          <AppearanceForm appearance={node.on?.item ?? { material: 'LIME_DYE' }} mutate={onApp} />
          <ActionsEditor title="ON click actions" value={node.on?.actions} mutate={onActions} />
        </div>
        <div className="rounded border border-red-900/40 p-2">
          <SectionTitle>OFF state</SectionTitle>
          <AppearanceForm appearance={node.off?.item ?? { material: 'GRAY_DYE' }} mutate={offApp} />
          <ActionsEditor title="OFF click actions" value={node.off?.actions} mutate={offActions} />
        </div>
      </div>
    )
  }

  if (kind === 'cycle') {
    const states = node.states ?? []
    return (
      <div className="space-y-2">
        <SectionTitle>Cycle states</SectionTitle>
        <Checkbox checked={node.cycleOnClick ?? true} onChange={(v) => update((n) => (n.cycleOnClick = v))} label="Advance on click" />
        {states.map((st, i) => (
          <div key={i} className="rounded border border-black/40 p-2">
            <div className="mb-1 flex items-center justify-between text-xs text-gray-400">
              <span>State {i + 1}</span>
              <Button variant="ghost" onClick={() => update((n) => n.states!.splice(i, 1))}>
                ✕
              </Button>
            </div>
            <AppearanceForm
              appearance={st.item ?? { material: 'STONE' }}
              mutate={(fn) =>
                update((n) => {
                  n.states ??= []
                  n.states[i] ??= { kind: 'basic' }
                  n.states[i].item ??= { material: 'STONE' }
                  fn(n.states[i].item!)
                })
              }
            />
          </div>
        ))}
        <Button
          variant="ghost"
          onClick={() => update((n) => { n.states ??= []; n.states.push({ kind: 'basic', item: { material: 'STONE' } }) })}
        >
          + State
        </Button>
      </div>
    )
  }

  if (kind === 'twoStage') {
    return (
      <div className="space-y-2">
        <Checkbox checked={node.glowOnFirstClick ?? true} onChange={(v) => update((n) => (n.glowOnFirstClick = v))} label="Glow after first click" />
        <ActionsEditor title="Confirm (2nd click) actions" value={node.secondClickActions} mutate={(fn) => update((n) => { n.secondClickActions ??= {}; fn(n.secondClickActions) })} />
      </div>
    )
  }

  return null
}
