import React from 'react'

// Legacy color code -> hex, matching Bukkit's ChatColor palette.
const COLORS: Record<string, string> = {
  '0': '#000000', '1': '#0000AA', '2': '#00AA00', '3': '#00AAAA',
  '4': '#AA0000', '5': '#AA00AA', '6': '#FFAA00', '7': '#AAAAAA',
  '8': '#555555', '9': '#5555FF', a: '#55FF55', b: '#55FFFF',
  c: '#FF5555', d: '#FF55FF', e: '#FFFF55', f: '#FFFFFF',
}

interface Style {
  color: string
  bold: boolean
  italic: boolean
  underline: boolean
  strikethrough: boolean
}

const baseStyle = (): Style => ({
  color: '#FFFFFF',
  bold: false,
  italic: false,
  underline: false,
  strikethrough: false,
})

/**
 * Parses a Bukkit-style string (`&` / `§` legacy codes plus `&#RRGGBB` hex, as mlib's Chat.colorize
 * supports) into styled React spans, so names/lore/titles preview the way they look in-game.
 */
export function parseMcText(input: string | undefined): React.ReactNode[] {
  if (!input) return []
  const nodes: React.ReactNode[] = []
  let style = baseStyle()
  let buffer = ''
  let key = 0

  const flush = () => {
    if (!buffer) return
    nodes.push(
      <span
        key={key++}
        style={{
          color: style.color,
          fontWeight: style.bold ? 700 : 400,
          fontStyle: style.italic ? 'italic' : 'normal',
          textDecoration:
            [style.underline && 'underline', style.strikethrough && 'line-through']
              .filter(Boolean)
              .join(' ') || 'none',
        }}
      >
        {buffer}
      </span>,
    )
    buffer = ''
  }

  for (let i = 0; i < input.length; i++) {
    const ch = input[i]
    const isCode = ch === '&' || ch === '§'
    if (isCode && i + 1 < input.length) {
      const next = input[i + 1]
      // hex: &#RRGGBB
      if (next === '#' && i + 8 <= input.length) {
        const hex = input.slice(i + 2, i + 8)
        if (/^[0-9a-fA-F]{6}$/.test(hex)) {
          flush()
          style = { ...style, color: `#${hex}` }
          i += 7
          continue
        }
      }
      const code = next.toLowerCase()
      if (COLORS[code]) {
        flush()
        style = { ...baseStyle(), color: COLORS[code] }
        i++
        continue
      }
      switch (code) {
        case 'l': flush(); style = { ...style, bold: true }; i++; continue
        case 'o': flush(); style = { ...style, italic: true }; i++; continue
        case 'n': flush(); style = { ...style, underline: true }; i++; continue
        case 'm': flush(); style = { ...style, strikethrough: true }; i++; continue
        case 'k': i++; continue // obfuscated: render text as-is
        case 'r': flush(); style = baseStyle(); i++; continue
        default: break
      }
    }
    buffer += ch
  }
  flush()
  return nodes
}

/** Strips color/format codes (for plain-text contexts like tab labels). */
export function stripMcCodes(input: string | undefined): string {
  if (!input) return ''
  return input.replace(/&#[0-9a-fA-F]{6}/g, '').replace(/[&§][0-9a-fk-orA-FK-OR]/g, '')
}
