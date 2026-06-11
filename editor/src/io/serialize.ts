import YAML from 'yaml'
import type { MenuProject } from '../model/types'

const isPlainObject = (v: unknown): v is Record<string, unknown> =>
  typeof v === 'object' && v !== null && !Array.isArray(v)

/** Recursively drops undefined/null, empty arrays, and empty objects for tidy export. */
function prune(value: unknown): unknown {
  if (Array.isArray(value)) {
    return value.map(prune).filter((v) => v !== undefined)
  }
  if (isPlainObject(value)) {
    const out: Record<string, unknown> = {}
    for (const [k, v] of Object.entries(value)) {
      const pv = prune(v)
      if (pv === undefined) continue
      if (Array.isArray(pv) && pv.length === 0) continue
      if (isPlainObject(pv) && Object.keys(pv).length === 0) continue
      out[k] = pv
    }
    return out
  }
  if (value === null) return undefined
  return value
}

function cleaned(project: MenuProject): Record<string, unknown> {
  return prune(project) as Record<string, unknown>
}

export function toYaml(project: MenuProject): string {
  return YAML.stringify(cleaned(project))
}

export function toJson(project: MenuProject): string {
  return JSON.stringify(cleaned(project), null, 2)
}
