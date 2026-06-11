import { type ReactNode } from 'react'

const inputCls =
  'w-full rounded bg-[#1b1b1b] px-2 py-1 text-sm outline-none ring-1 ring-black/40 focus:ring-emerald-500'

export function Field({ label, hint, children }: { label: string; hint?: string; children: ReactNode }) {
  return (
    <label className="block space-y-1">
      <div className="text-xs font-medium text-gray-400">{label}</div>
      {children}
      {hint && <div className="text-[10px] text-gray-500">{hint}</div>}
    </label>
  )
}

export function SectionTitle({ children }: { children: ReactNode }) {
  return <div className="mt-2 text-xs font-semibold uppercase tracking-wide text-emerald-400">{children}</div>
}

export function TextInput(props: React.InputHTMLAttributes<HTMLInputElement>) {
  return <input {...props} className={`${inputCls} ${props.className ?? ''}`} />
}

export function NumberInput(props: React.InputHTMLAttributes<HTMLInputElement>) {
  return <input type="number" {...props} className={`${inputCls} ${props.className ?? ''}`} />
}

export function TextArea(props: React.TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return <textarea {...props} className={`${inputCls} font-mono ${props.className ?? ''}`} />
}

export function Select(props: React.SelectHTMLAttributes<HTMLSelectElement>) {
  return <select {...props} className={`${inputCls} ${props.className ?? ''}`} />
}

export function Checkbox({
  checked,
  onChange,
  label,
}: {
  checked: boolean
  onChange: (v: boolean) => void
  label: string
}) {
  return (
    <label className="flex cursor-pointer items-center gap-2 text-sm">
      <input type="checkbox" checked={checked} onChange={(e) => onChange(e.target.checked)} className="accent-emerald-500" />
      {label}
    </label>
  )
}

export function Button({
  children,
  onClick,
  variant = 'default',
  disabled,
  title,
}: {
  children: ReactNode
  onClick?: () => void
  variant?: 'default' | 'primary' | 'danger' | 'ghost'
  disabled?: boolean
  title?: string
}) {
  const styles: Record<string, string> = {
    default: 'bg-[#333] hover:bg-[#3d3d3d] text-gray-100',
    primary: 'bg-emerald-600 hover:bg-emerald-500 text-white',
    danger: 'bg-red-700 hover:bg-red-600 text-white',
    ghost: 'hover:bg-[#2a2a2a] text-gray-300',
  }
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      title={title}
      className={`rounded px-3 py-1 text-sm transition disabled:cursor-not-allowed disabled:opacity-40 ${styles[variant]}`}
    >
      {children}
    </button>
  )
}
