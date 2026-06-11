import { useState } from 'react'
import { ACTION_TYPES, type Action, type ActionType } from '../model/types'
import type { Mut } from '../lib/mutate'
import { useStore } from '../state/store'
import { Button, Field, NumberInput, Select, TextArea, TextInput } from './ui'

/** An orderable list of action steps. Reused for click branches and CONDITIONAL then/else. */
export function ActionList({ actions, mutate }: { actions: Action[]; mutate: Mut<Action[]> }) {
  return (
    <div className="space-y-2">
      {actions.map((a, i) => (
        <ActionCard
          key={i}
          action={a}
          mutate={(fn) => mutate((list) => fn(list[i]))}
          onRemove={() => mutate((list) => list.splice(i, 1))}
          onMoveUp={i > 0 ? () => mutate((list) => swap(list, i, i - 1)) : undefined}
          onMoveDown={i < actions.length - 1 ? () => mutate((list) => swap(list, i, i + 1)) : undefined}
        />
      ))}
      <AddActionButton onAdd={(type) => mutate((list) => list.push({ type }))} />
    </div>
  )
}

function AddActionButton({ onAdd }: { onAdd: (type: ActionType) => void }) {
  return (
    <Select
      value=""
      onChange={(e) => {
        if (e.target.value) onAdd(e.target.value as ActionType)
        e.currentTarget.value = ''
      }}
    >
      <option value="">+ Add action…</option>
      {ACTION_TYPES.map((t) => (
        <option key={t} value={t}>
          {t}
        </option>
      ))}
    </Select>
  )
}

function ActionCard({
  action,
  mutate,
  onRemove,
  onMoveUp,
  onMoveDown,
}: {
  action: Action
  mutate: Mut<Action>
  onRemove: () => void
  onMoveUp?: () => void
  onMoveDown?: () => void
}) {
  return (
    <div className="rounded border border-black/40 bg-[#1f1f1f] p-2">
      <div className="mb-1 flex items-center gap-1">
        <span className="flex-1 text-xs font-semibold text-emerald-300">{action.type}</span>
        <button className="px-1 text-gray-400 enabled:hover:text-white disabled:opacity-30" disabled={!onMoveUp} onClick={onMoveUp}>↑</button>
        <button className="px-1 text-gray-400 enabled:hover:text-white disabled:opacity-30" disabled={!onMoveDown} onClick={onMoveDown}>↓</button>
        <button className="px-1 text-red-400 hover:text-red-300" onClick={onRemove}>✕</button>
      </div>
      <ActionFields action={action} mutate={mutate} />
    </div>
  )
}

function ActionFields({ action, mutate }: { action: Action; mutate: Mut<Action> }) {
  const menuIds = useStore((s) => Object.keys(s.project.menus))

  switch (action.type) {
    case 'RUN_COMMAND':
      return (
        <div className="space-y-1">
          <TextInput placeholder="command without slash" value={action.command ?? ''} onChange={(e) => mutate((a) => (a.command = e.target.value))} />
          <Select value={action.as ?? 'PLAYER'} onChange={(e) => mutate((a) => (a.as = e.target.value as 'PLAYER' | 'CONSOLE'))}>
            <option value="PLAYER">as player</option>
            <option value="CONSOLE">as console</option>
          </Select>
        </div>
      )
    case 'MESSAGE':
      return (
        <div className="space-y-1">
          <TextInput placeholder="&aMessage text" value={action.text ?? ''} onChange={(e) => mutate((a) => (a.text = e.target.value))} />
        </div>
      )
    case 'BROADCAST':
      return (
        <div className="space-y-1">
          <TextInput placeholder="&aBroadcast text" value={action.text ?? ''} onChange={(e) => mutate((a) => (a.text = e.target.value))} />
          <TextInput placeholder="permission (optional)" value={action.permission ?? ''} onChange={(e) => mutate((a) => (a.permission = e.target.value || undefined))} />
        </div>
      )
    case 'ACTIONBAR':
      return <TextInput placeholder="&eAction bar text" value={action.text ?? ''} onChange={(e) => mutate((a) => (a.text = e.target.value))} />
    case 'TITLE':
      return (
        <div className="space-y-1">
          <TextInput placeholder="&6Title" value={action.title ?? ''} onChange={(e) => mutate((a) => (a.title = e.target.value))} />
          <TextInput placeholder="&7Subtitle" value={action.subtitle ?? ''} onChange={(e) => mutate((a) => (a.subtitle = e.target.value || undefined))} />
          <div className="flex gap-1">
            <NumberInput title="fadeIn" placeholder="in" value={action.fadeIn ?? ''} onChange={(e) => mutate((a) => (a.fadeIn = num(e.target.value)))} />
            <NumberInput title="stay" placeholder="stay" value={action.stay ?? ''} onChange={(e) => mutate((a) => (a.stay = num(e.target.value)))} />
            <NumberInput title="fadeOut" placeholder="out" value={action.fadeOut ?? ''} onChange={(e) => mutate((a) => (a.fadeOut = num(e.target.value)))} />
          </div>
        </div>
      )
    case 'PLAY_SOUND':
      return (
        <div className="space-y-1">
          <TextInput placeholder="UI_BUTTON_CLICK" value={action.sound ?? ''} onChange={(e) => mutate((a) => (a.sound = e.target.value))} />
          <div className="flex gap-1">
            <NumberInput title="volume" placeholder="vol" step="0.1" value={action.volume ?? ''} onChange={(e) => mutate((a) => (a.volume = num(e.target.value)))} />
            <NumberInput title="pitch" placeholder="pitch" step="0.1" value={action.pitch ?? ''} onChange={(e) => mutate((a) => (a.pitch = num(e.target.value)))} />
          </div>
        </div>
      )
    case 'OPEN_MENU':
      return (
        <Select value={action.menu ?? ''} onChange={(e) => mutate((a) => (a.menu = e.target.value))}>
          <option value="">(choose menu)</option>
          {menuIds.map((id) => (
            <option key={id} value={id}>{id}</option>
          ))}
        </Select>
      )
    case 'GIVE_ITEM':
      return (
        <div className="space-y-1">
          <TextInput placeholder="MATERIAL" value={action.item?.material ?? ''} onChange={(e) => mutate((a) => (a.item = { material: e.target.value }))} />
          <NumberInput placeholder="amount" min={1} max={64} value={action.amount ?? 1} onChange={(e) => mutate((a) => (a.amount = num(e.target.value)))} />
        </div>
      )
    case 'CONSOLE_LOG':
      return (
        <div className="space-y-1">
          <TextInput placeholder="log text" value={action.text ?? ''} onChange={(e) => mutate((a) => (a.text = e.target.value))} />
          <Select value={action.level ?? 'INFO'} onChange={(e) => mutate((a) => (a.level = e.target.value as 'INFO' | 'WARN' | 'SEVERE'))}>
            <option>INFO</option>
            <option>WARN</option>
            <option>SEVERE</option>
          </Select>
        </div>
      )
    case 'CUSTOM':
      return (
        <div className="space-y-1">
          <Field label="Custom id" hint="matched to MenuHooks.registerAction in your plugin">
            <TextInput placeholder="openShop" value={action.id ?? ''} onChange={(e) => mutate((a) => (a.id = e.target.value))} />
          </Field>
          <DataEditor action={action} mutate={mutate} />
        </div>
      )
    case 'CONDITIONAL':
      return (
        <div className="space-y-1">
          <TextInput
            placeholder="if (e.g. permission:vip)"
            value={typeof action.if === 'string' ? action.if : action.if ? JSON.stringify(action.if) : ''}
            onChange={(e) => mutate((a) => (a.if = e.target.value))}
          />
          <div className="text-[10px] text-emerald-400">then:</div>
          <ActionList actions={action.then ?? []} mutate={(fn) => mutate((a) => { a.then ??= []; fn(a.then) })} />
          <div className="text-[10px] text-emerald-400">else:</div>
          <ActionList actions={action.else ?? []} mutate={(fn) => mutate((a) => { a.else ??= []; fn(a.else) })} />
        </div>
      )
    default:
      return <div className="text-[10px] text-gray-500">No options.</div>
  }
}

function DataEditor({ action, mutate }: { action: Action; mutate: Mut<Action> }) {
  const [text, setText] = useState(() => (action.data ? JSON.stringify(action.data, null, 2) : ''))
  const [err, setErr] = useState<string | null>(null)
  return (
    <Field label="Data (JSON, optional)">
      <TextArea
        rows={3}
        value={text}
        placeholder='{ "category": "weapons" }'
        onChange={(e) => {
          setText(e.target.value)
          const raw = e.target.value.trim()
          if (!raw) {
            setErr(null)
            mutate((a) => (a.data = undefined))
            return
          }
          try {
            const parsed = JSON.parse(raw)
            setErr(null)
            mutate((a) => (a.data = parsed))
          } catch {
            setErr('invalid JSON')
          }
        }}
      />
      {err && <div className="text-[10px] text-red-400">{err}</div>}
    </Field>
  )
}

const num = (v: string) => (v === '' ? undefined : Number(v))
function swap<T>(list: T[], a: number, b: number) {
  const t = list[a]
  list[a] = list[b]
  list[b] = t
}
