import { useState } from 'react'
import type { ItemAppearance } from '../model/types'
import type { Mut } from '../lib/mutate'
import { Button, Checkbox, Field, NumberInput, SectionTitle, TextArea, TextInput } from './ui'

/**
 * Reusable editor for an ItemAppearance. Used for the top-level item and recursively for nested
 * nodes (toggle on/off states, cycle states). Edits are applied through [mutate] so the caller
 * controls where in the project tree the change lands.
 */
export default function AppearanceForm({ appearance, mutate }: { appearance: ItemAppearance; mutate: Mut<ItemAppearance> }) {
  const [advanced, setAdvanced] = useState(false)
  const material = appearance.material ?? ''
  const isSkull = material.toUpperCase() === 'PLAYER_HEAD'
  const isPotion = material.toUpperCase().includes('POTION')
  const isBanner = material.toUpperCase().endsWith('_BANNER')

  return (
    <div className="space-y-3">
      <Field label="Material" hint="Bukkit Material name or hook:id">
        <TextInput value={material} onChange={(e) => mutate((a) => (a.material = e.target.value))} />
      </Field>

      <Field label="Display name" hint="& color codes and &#RRGGBB supported">
        <TextInput value={appearance.name ?? ''} onChange={(e) => mutate((a) => (a.name = e.target.value || undefined))} />
      </Field>

      <Field label="Amount">
        <NumberInput
          min={1}
          max={64}
          value={appearance.amount ?? 1}
          onChange={(e) => mutate((a) => (a.amount = clamp(Number(e.target.value) || 1, 1, 64)))}
        />
      </Field>

      <Field label="Lore" hint="one line per row">
        <TextArea
          rows={3}
          value={(appearance.lore ?? []).join('\n')}
          onChange={(e) =>
            mutate((a) => {
              const lines = e.target.value.split('\n')
              a.lore = lines.length === 1 && lines[0] === '' ? undefined : lines
            })
          }
        />
      </Field>

      <div className="flex flex-wrap gap-x-4 gap-y-1">
        <Checkbox checked={!!appearance.glow} onChange={(v) => mutate((a) => (a.glow = v || undefined))} label="Glow" />
        <Checkbox
          checked={!!appearance.hideAttributes}
          onChange={(v) => mutate((a) => (a.hideAttributes = v || undefined))}
          label="Hide attributes"
        />
      </div>

      <button className="text-xs text-emerald-400 hover:underline" onClick={() => setAdvanced((v) => !v)}>
        {advanced ? '▾ Hide advanced' : '▸ Advanced (enchants, flags, model data…)'}
      </button>

      {advanced && (
        <div className="space-y-3 rounded border border-black/40 bg-[#1f1f1f] p-2">
          <Field label="Custom model data">
            <NumberInput
              value={appearance.customModelData ?? ''}
              onChange={(e) => mutate((a) => (a.customModelData = e.target.value ? Number(e.target.value) : undefined))}
            />
          </Field>

          <Field label="Item flags" hint="comma-separated, e.g. HIDE_ENCHANTS, HIDE_DYE">
            <TextInput
              value={(appearance.itemFlags ?? []).join(', ')}
              onChange={(e) =>
                mutate((a) => {
                  const flags = e.target.value
                    .split(',')
                    .map((s) => s.trim().toUpperCase())
                    .filter(Boolean)
                  a.itemFlags = flags.length ? flags : undefined
                })
              }
            />
          </Field>

          <EnchantmentsEditor appearance={appearance} mutate={mutate} />

          {isSkull && (
            <Field label="Skull owner" hint="player name, UUID, or mhf:MHF_QUESTION">
              <TextInput
                value={appearance.skull?.owner ?? ''}
                onChange={(e) =>
                  mutate((a) => {
                    a.skull = e.target.value ? { owner: e.target.value } : undefined
                  })
                }
              />
            </Field>
          )}

          {isPotion && (
            <Field label="Potion color" hint="hex, e.g. #FF0000">
              <TextInput
                value={appearance.potion?.color ?? ''}
                onChange={(e) =>
                  mutate((a) => {
                    a.potion = { ...(a.potion ?? {}), color: e.target.value || undefined }
                  })
                }
              />
            </Field>
          )}

          {isBanner && (
            <div className="text-[10px] text-gray-500">
              Banner patterns are supported by the format; edit them in the exported file for now.
            </div>
          )}
        </div>
      )}
    </div>
  )
}

function EnchantmentsEditor({ appearance, mutate }: { appearance: ItemAppearance; mutate: Mut<ItemAppearance> }) {
  const enchants = appearance.enchantments ?? []
  return (
    <div className="space-y-1">
      <SectionTitle>Enchantments</SectionTitle>
      {enchants.map((e, i) => (
        <div key={i} className="flex items-center gap-1">
          <TextInput
            placeholder="sharpness"
            value={e.type ?? ''}
            onChange={(ev) => mutate((a) => (a.enchantments![i].type = ev.target.value))}
            className="flex-1"
          />
          <NumberInput
            value={e.level ?? 1}
            onChange={(ev) => mutate((a) => (a.enchantments![i].level = Number(ev.target.value) || 1))}
            className="w-16"
          />
          <Button variant="ghost" onClick={() => mutate((a) => a.enchantments!.splice(i, 1))}>
            ✕
          </Button>
        </div>
      ))}
      <Button
        variant="ghost"
        onClick={() =>
          mutate((a) => {
            a.enchantments = [...(a.enchantments ?? []), { type: 'unbreaking', level: 1 }]
          })
        }
      >
        + Enchantment
      </Button>
    </div>
  )
}

const clamp = (n: number, lo: number, hi: number) => Math.max(lo, Math.min(hi, n))
