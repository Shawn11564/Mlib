import materialsData from './materials.json'

export interface MaterialEntry {
  id: string
  name: string
  category: 'item' | 'block'
  material: string
}

interface MaterialsFile {
  mcVersion: string
  count: number
  materials: MaterialEntry[]
}

const data = materialsData as MaterialsFile

export const MC_VERSION = data.mcVersion
export const MATERIALS: MaterialEntry[] = data.materials

// jsDelivr serves the InventivetalentDev/minecraft-assets repo over a real CDN (cached, no GitHub
// raw rate limits). Textures are © Mojang — loaded at runtime, never bundled/committed.
const CDN = `https://cdn.jsdelivr.net/gh/InventivetalentDev/minecraft-assets@${MC_VERSION}/assets/minecraft/textures`

const byMaterial = new Map<string, MaterialEntry>()
for (const m of MATERIALS) byMaterial.set(m.material, m)

export function textureUrl(category: 'item' | 'block', id: string): string {
  return `${CDN}/${category}/${id}.png`
}

export function lookupMaterial(material: string): MaterialEntry | undefined {
  return byMaterial.get(material.trim().toUpperCase())
}

/**
 * Ordered list of icon URLs to try for a material name. A known palette entry wins; otherwise we
 * guess item/<id>.png then block/<id>.png (covers user-typed materials not in the palette). The
 * <ItemIcon> component walks these on <img> error.
 */
export function iconCandidates(material: string | undefined): string[] {
  if (!material || !material.trim()) return []
  const known = byMaterial.get(material.trim().toUpperCase())
  const id = material.trim().toLowerCase()
  const urls: string[] = []
  if (known) urls.push(textureUrl(known.category, known.id))
  urls.push(textureUrl('item', id))
  urls.push(textureUrl('block', id))
  return [...new Set(urls)]
}
