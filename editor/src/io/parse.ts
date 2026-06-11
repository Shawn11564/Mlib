import YAML from 'yaml'
import { FORMAT_VERSION, type Menu, type MenuProject } from '../model/types'

/** Parses YAML or JSON text into a MenuProject, normalizing a bare single menu and migrating versions. */
export function parseProject(text: string): MenuProject {
  const trimmed = text.trim()
  if (!trimmed) throw new Error('Empty file')

  let data: unknown
  if (trimmed.startsWith('{') || trimmed.startsWith('[')) {
    data = JSON.parse(trimmed)
  } else {
    data = YAML.parse(trimmed)
  }

  if (typeof data !== 'object' || data === null) throw new Error('Menu document must be an object')
  return migrate(normalize(data as Record<string, unknown>))
}

function normalize(obj: Record<string, unknown>): MenuProject {
  if (obj.menus && typeof obj.menus === 'object') {
    return {
      formatVersion: typeof obj.formatVersion === 'number' ? obj.formatVersion : FORMAT_VERSION,
      project: typeof obj.project === 'string' ? obj.project : undefined,
      menus: obj.menus as Record<string, Menu>,
    }
  }
  // bare single menu -> one-entry project
  const { formatVersion, project, ...menu } = obj
  return {
    formatVersion: typeof formatVersion === 'number' ? formatVersion : FORMAT_VERSION,
    project: typeof project === 'string' ? project : undefined,
    menus: { main: menu as Menu },
  }
}

/** Hook for future format upgrades. v1 is the only version today. */
function migrate(project: MenuProject): MenuProject {
  if (project.formatVersion > FORMAT_VERSION) {
    throw new Error(`This editor supports format v${FORMAT_VERSION}, but the file is v${project.formatVersion}.`)
  }
  project.formatVersion = FORMAT_VERSION
  if (!project.menus || Object.keys(project.menus).length === 0) {
    throw new Error('No menus found in document')
  }
  return project
}
