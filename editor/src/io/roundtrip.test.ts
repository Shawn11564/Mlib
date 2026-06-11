import { describe, expect, it } from 'vitest'
import type { MenuProject } from '../model/types'
import { FORMAT_VERSION } from '../model/types'
import { toJson, toYaml } from './serialize'
import { parseProject } from './parse'

const sample: MenuProject = {
  formatVersion: FORMAT_VERSION,
  project: 'demo',
  menus: {
    main_menu: {
      title: '&8Main',
      type: 'chest',
      rows: 3,
      nextMenu: 'settings',
      fill: { kind: 'basic', item: { material: 'BLACK_STAINED_GLASS_PANE', name: ' ' } },
      items: [
        {
          x: 4,
          y: 1,
          kind: 'basic',
          item: { material: 'COMPASS', name: '&aSettings', lore: ['&7Open settings'] },
          actions: {
            requirements: ['permission:menu.use'],
            left: [
              { type: 'PLAY_SOUND', sound: 'UI_BUTTON_CLICK' },
              { type: 'OPEN_MENU', menu: 'settings' },
            ],
          },
        },
      ],
    },
    settings: {
      title: '&8Settings',
      type: 'chest',
      rows: 3,
      previousMenu: 'main_menu',
      items: [
        {
          x: 2,
          y: 1,
          kind: 'toggle',
          initiallyToggledOn: false,
          on: { kind: 'basic', item: { material: 'LIME_DYE', name: '&aON' }, actions: { left: [{ type: 'MESSAGE', text: 'on' }] } },
          off: { kind: 'basic', item: { material: 'GRAY_DYE', name: '&7OFF' } },
        },
        {
          x: 6,
          y: 1,
          kind: 'twoStage',
          item: { material: 'TNT', name: '&cReset' },
          secondClickActions: { default: [{ type: 'CUSTOM', id: 'reset', data: { scope: 'all' } }, { type: 'CLOSE' }] },
        },
      ],
    },
  },
}

describe('round-trip', () => {
  it('YAML and JSON exports parse to the same structure', () => {
    const fromYaml = parseProject(toYaml(sample))
    const fromJson = parseProject(toJson(sample))
    expect(fromYaml).toEqual(fromJson)
  })

  it('re-exporting a parsed project is idempotent (YAML)', () => {
    const once = parseProject(toYaml(sample))
    const twice = parseProject(toYaml(once))
    expect(twice).toEqual(once)
  })

  it('preserves key fields through a round-trip', () => {
    const p = parseProject(toYaml(sample))
    expect(p.formatVersion).toBe(FORMAT_VERSION)
    expect(Object.keys(p.menus)).toEqual(['main_menu', 'settings'])
    expect(p.menus.main_menu.items![0].actions!.left![1]).toEqual({ type: 'OPEN_MENU', menu: 'settings' })
    expect(p.menus.settings.items![0].kind).toBe('toggle')
    expect(p.menus.settings.items![1].secondClickActions!.default![0]).toEqual({
      type: 'CUSTOM',
      id: 'reset',
      data: { scope: 'all' },
    })
  })

  it('normalizes a bare single menu into a one-entry project', () => {
    const bare = parseProject('formatVersion: 1\ntitle: "&8Bare"\nrows: 1\n')
    expect(Object.keys(bare.menus)).toEqual(['main'])
    expect(bare.menus.main.title).toBe('&8Bare')
  })
})
