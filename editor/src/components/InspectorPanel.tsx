import { useState } from 'react'
import { useStore } from '../state/store'
import { ITEM_KINDS, type ActionSet, type ItemAppearance, type ItemKind } from '../model/types'
import type { Mut } from '../lib/mutate'
import { Button, Field, NumberInput, SectionTitle, Select, TextInput } from './ui'
import AppearanceForm from './AppearanceForm'
import ItemKindEditor from './ItemKindEditor'
import ActionsEditor from './ActionsEditor'

export default function InspectorPanel() {
  const selected = useStore((s) => s.selected)
  const project = useStore((s) => s.project)
  const activeMenuId = useStore((s) => s.activeMenuId)

  const node = selected
    ? project.menus[activeMenuId]?.items?.find((i) => (i.x ?? 0) === selected.x && (i.y ?? 0) === selected.y)
    : undefined

  return (
    <div className="scroll-thin flex h-full w-80 shrink-0 flex-col gap-3 overflow-y-auto border-l border-black/40 bg-[#262626] p-3">
      {selected && node ? <ItemInspector /> : <MenuInspector />}
    </div>
  )
}

function ItemInspector() {
  const selected = useStore((s) => s.selected)!
  const project = useStore((s) => s.project)
  const activeMenuId = useStore((s) => s.activeMenuId)
  const update = useStore((s) => s.updateSelectedItem)
  const removeItem = useStore((s) => s.removeItem)

  const node = project.menus[activeMenuId].items!.find((i) => (i.x ?? 0) === selected.x && (i.y ?? 0) === selected.y)!
  const kind = node.kind ?? 'basic'

  const appMutate: Mut<ItemAppearance> = (fn) =>
    update((n) => {
      n.item ??= { material: 'STONE' }
      fn(n.item)
    })
  const actionsMutate: Mut<ActionSet> = (fn) =>
    update((n) => {
      n.actions ??= {}
      fn(n.actions)
    })

  const changeKind = (k: ItemKind) =>
    update((n) => {
      n.kind = k
      if (k === 'toggle' && !n.on) {
        n.on = { kind: 'basic', item: structuredClone(n.item ?? { material: 'LIME_DYE' }) }
        n.off ??= { kind: 'basic', item: { material: 'GRAY_DYE' } }
      }
      if (k === 'cycle' && !(n.states && n.states.length)) {
        n.states = [{ kind: 'basic', item: structuredClone(n.item ?? { material: 'STONE' }) }]
      }
    })

  const showAppearance = kind !== 'toggle' && kind !== 'cycle'

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <div className="text-sm font-semibold">
          Item <span className="text-gray-500">({selected.x}, {selected.y})</span>
        </div>
        <Button variant="danger" onClick={() => removeItem(selected.x, selected.y)}>
          Delete
        </Button>
      </div>

      <Field label="Type">
        <Select value={kind} onChange={(e) => changeKind(e.target.value as ItemKind)}>
          {ITEM_KINDS.map((k) => (
            <option key={k} value={k}>
              {k}
            </option>
          ))}
        </Select>
      </Field>

      {showAppearance && (
        <>
          <SectionTitle>Appearance</SectionTitle>
          <AppearanceForm appearance={node.item ?? { material: 'STONE' }} mutate={appMutate} />
        </>
      )}

      <ItemKindEditor />

      <ActionsEditor value={node.actions} mutate={actionsMutate} />
    </div>
  )
}

function MenuInspector() {
  const project = useStore((s) => s.project)
  const activeMenuId = useStore((s) => s.activeMenuId)
  const patch = useStore((s) => s.patchActiveMenu)
  const deleteMenu = useStore((s) => s.deleteMenu)
  const renameMenu = useStore((s) => s.renameMenu)

  const menu = project.menus[activeMenuId]
  const otherIds = Object.keys(project.menus).filter((id) => id !== activeMenuId)
  const [renameTo, setRenameTo] = useState(activeMenuId)

  return (
    <div className="space-y-3">
      <div className="text-sm font-semibold">Menu settings</div>
      <div className="text-[11px] text-gray-500">Select a slot to edit an item, or edit this menu below.</div>

      <Field label="Title" hint="& color codes supported">
        <TextInput value={menu.title ?? ''} onChange={(e) => patch({ title: e.target.value })} />
      </Field>

      <Field label="Rows">
        <NumberInput
          min={1}
          max={6}
          value={menu.rows ?? 6}
          onChange={(e) => patch({ rows: Math.max(1, Math.min(6, Number(e.target.value) || 6)) })}
        />
      </Field>

      <Field label="Previous menu" hint="adds an auto 'Previous' button at (0,0)">
        <Select value={menu.previousMenu ?? ''} onChange={(e) => patch({ previousMenu: e.target.value || null })}>
          <option value="">(none)</option>
          {otherIds.map((id) => (
            <option key={id} value={id}>
              {id}
            </option>
          ))}
        </Select>
      </Field>

      <Field label="Next menu" hint="adds an auto 'Next' button at (8,0)">
        <Select value={menu.nextMenu ?? ''} onChange={(e) => patch({ nextMenu: e.target.value || null })}>
          <option value="">(none)</option>
          {otherIds.map((id) => (
            <option key={id} value={id}>
              {id}
            </option>
          ))}
        </Select>
      </Field>

      <Field label="Background fill" hint="material placed in every empty slot (blank to clear)">
        <TextInput
          value={menu.fill?.item?.material ?? ''}
          onChange={(e) =>
            patch({ fill: e.target.value ? { kind: 'basic', item: { material: e.target.value } } : undefined })
          }
        />
      </Field>

      <SectionTitle>Manage</SectionTitle>
      <Field label="Menu id">
        <div className="flex gap-2">
          <TextInput value={renameTo} onChange={(e) => setRenameTo(e.target.value)} />
          <Button onClick={() => renameMenu(activeMenuId, renameTo.trim())} disabled={!renameTo.trim() || renameTo === activeMenuId}>
            Rename
          </Button>
        </div>
      </Field>

      <Button variant="danger" onClick={() => deleteMenu(activeMenuId)} disabled={Object.keys(project.menus).length <= 1}>
        Delete this menu
      </Button>
    </div>
  )
}
