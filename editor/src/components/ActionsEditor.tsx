import { useState } from 'react'
import { CLICK_BRANCHES, type ActionSet, type ClickBranch } from '../model/types'
import type { Mut } from '../lib/mutate'
import { SectionTitle } from './ui'
import { ConditionListEditor } from './ConditionEditor'
import { ActionList } from './ActionList'

const BRANCH_LABEL: Record<ClickBranch, string> = {
  default: 'Default',
  left: 'Left',
  right: 'Right',
  shiftLeft: 'Shift-L',
  shiftRight: 'Shift-R',
  middle: 'Middle',
  drop: 'Drop',
  doubleClick: 'Double',
}

/**
 * Edits an ActionSet: gating requirements plus per-click-type action branches. Generic over where
 * the set lives (top-level item click, or a nested toggle/two-stage state) via [mutate].
 */
export default function ActionsEditor({
  value,
  mutate,
  title = 'Click actions',
}: {
  value?: ActionSet
  mutate: Mut<ActionSet>
  title?: string
}) {
  const [branch, setBranch] = useState<ClickBranch>('left')
  const set = value ?? {}
  const branchActions = set[branch] ?? []

  return (
    <div className="space-y-2 rounded border border-black/40 bg-[#222] p-2">
      <SectionTitle>{title}</SectionTitle>

      <ConditionListEditor
        value={set.requirements}
        mutate={(fn) => mutate((s) => { s.requirements ??= []; fn(s.requirements) })}
      />

      <div className="flex flex-wrap gap-1">
        {CLICK_BRANCHES.map((b) => {
          const count = set[b]?.length ?? 0
          return (
            <button
              key={b}
              onClick={() => setBranch(b)}
              className={`rounded px-2 py-0.5 text-[11px] ${
                branch === b ? 'bg-emerald-600 text-white' : 'bg-[#1b1b1b] text-gray-300 hover:bg-[#333]'
              }`}
            >
              {BRANCH_LABEL[b]}
              {count > 0 && <span className="ml-1 rounded bg-black/40 px-1">{count}</span>}
            </button>
          )
        })}
      </div>

      <ActionList
        actions={branchActions}
        mutate={(fn) => mutate((s) => { if (!s[branch]) s[branch] = []; fn(s[branch]!) })}
      />
    </div>
  )
}
