import { useEffect, useMemo, useState } from 'react'
import { iconCandidates } from '../assets-pipeline/textures'

interface Props {
  material?: string
  size?: number
  className?: string
}

/**
 * Renders a vanilla item/block icon for a material name, trying item then block textures (and a
 * known-palette entry first). Falls back to the material's first letters if no texture loads.
 */
export default function ItemIcon({ material, size = 32, className = '' }: Props) {
  const candidates = useMemo(() => iconCandidates(material), [material])
  const [idx, setIdx] = useState(0)

  useEffect(() => setIdx(0), [material])

  if (!material) return null

  if (idx >= candidates.length) {
    return (
      <div
        style={{ width: size, height: size }}
        className={`flex items-center justify-center bg-black/20 text-[8px] uppercase text-gray-300 ${className}`}
        title={material}
      >
        {material.replace(/^.*:/, '').slice(0, 3)}
      </div>
    )
  }

  return (
    <img
      src={candidates[idx]}
      width={size}
      height={size}
      className={`pixelated ${className}`}
      alt={material}
      title={material}
      draggable={false}
      onError={() => setIdx((i) => i + 1)}
    />
  )
}
