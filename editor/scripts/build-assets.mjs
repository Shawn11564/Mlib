// Generates the material palette (src/assets-pipeline/materials.json) for the editor by reading the
// vanilla texture index from InventivetalentDev/minecraft-assets at a given Minecraft version.
//
// Icons themselves are loaded at runtime from the jsDelivr CDN mirror of that repo, so this script
// does NOT download or bundle any textures — Minecraft assets are © Mojang and are not committed.
// Requires Node 18+ (global fetch). Re-run to retarget a version:  MC_VERSION=1.21.4 npm run build:assets
import { writeFile, mkdir } from 'node:fs/promises'
import { dirname } from 'node:path'
import { fileURLToPath } from 'node:url'

const MC_VERSION = process.env.MC_VERSION || '26.1.2'
const BASE = `https://raw.githubusercontent.com/InventivetalentDev/minecraft-assets/${MC_VERSION}/assets/minecraft/textures`
const OUT = fileURLToPath(new URL('../src/assets-pipeline/materials.json', import.meta.url))

// Block textures that are sub-faces / animation frames rather than standalone block icons.
const BLOCK_SUFFIX_DROP = /_(top|bottom|side|front|back|inner|outer|overlay|end|flow|still|base|connect|debug)$/
const BLOCK_DROP_CONTAINS = /(_stage|_age[0-9]|destroy_stage|_side[0-9]|sapling$)/

async function fetchList(category) {
  const res = await fetch(`${BASE}/${category}/_list.json`)
  if (!res.ok) throw new Error(`Failed to fetch ${category}/_list.json: ${res.status}`)
  const json = await res.json()
  return (json.files || []).filter((f) => f.endsWith('.png')).map((f) => f.replace(/\.png$/, ''))
}

const titleCase = (id) =>
  id.split('_').map((w) => w.charAt(0).toUpperCase() + w.slice(1)).join(' ')

async function run() {
  console.log(`Fetching texture lists for Minecraft ${MC_VERSION}...`)
  const [items, blocksRaw] = await Promise.all([fetchList('item'), fetchList('block')])

  const itemSet = new Set(items)
  const blocks = blocksRaw.filter(
    (id) => !itemSet.has(id) && !BLOCK_SUFFIX_DROP.test(id) && !BLOCK_DROP_CONTAINS.test(id),
  )

  const materials = [
    ...items.map((id) => ({ id, name: titleCase(id), category: 'item', material: id.toUpperCase() })),
    ...blocks.map((id) => ({ id, name: titleCase(id), category: 'block', material: id.toUpperCase() })),
  ].sort((a, b) => a.name.localeCompare(b.name))

  await mkdir(dirname(OUT), { recursive: true })
  await writeFile(OUT, JSON.stringify({ mcVersion: MC_VERSION, count: materials.length, materials }))
  console.log(`Wrote ${materials.length} materials (${items.length} items, ${blocks.length} blocks) -> ${OUT}`)
}

run().catch((e) => {
  console.error(e)
  process.exit(1)
})
