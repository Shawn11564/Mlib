import type { Condition } from '../model/types'
import type { Mut } from '../lib/mutate'
import { Button, SectionTitle, TextInput } from './ui'

/**
 * Edits a list of conditions (action `requirements`). String shorthands are edited inline;
 * object conditions (all/any/not — typically hand-authored) are shown read-only so they aren't lost.
 */
export function ConditionListEditor({
  value,
  mutate,
  label = 'Requirements',
}: {
  value?: Condition[]
  mutate: Mut<Condition[]>
  label?: string
}) {
  const conds = value ?? []
  return (
    <div className="space-y-1">
      <SectionTitle>{label}</SectionTitle>
      {conds.map((c, i) =>
        typeof c === 'string' ? (
          <div key={i} className="flex gap-1">
            <TextInput
              value={c}
              placeholder="permission:node | op | gamemode:CREATIVE | placeholder:%x% >= 1"
              onChange={(e) => mutate((list) => (list[i] = e.target.value))}
              className="flex-1"
            />
            <Button variant="ghost" onClick={() => mutate((list) => list.splice(i, 1))}>
              ✕
            </Button>
          </div>
        ) : (
          <div key={i} className="flex items-center gap-1">
            <code className="flex-1 truncate rounded bg-black/30 px-1 py-0.5 text-[10px] text-gray-400">
              {JSON.stringify(c)}
            </code>
            <Button variant="ghost" onClick={() => mutate((list) => list.splice(i, 1))}>
              ✕
            </Button>
          </div>
        ),
      )}
      <Button variant="ghost" onClick={() => mutate((list) => list.push(''))}>
        + Requirement
      </Button>
    </div>
  )
}
