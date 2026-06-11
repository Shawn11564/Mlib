import { useStore } from '../state/store'
import { toJson, toYaml } from '../io/serialize'
import { parseProject } from '../io/parse'
import { downloadText, pickTextFile } from '../io/download'
import { toKotlin } from '../io/codegen'
import { Button } from './ui'

export default function TopBar({ onShowGraph }: { onShowGraph: () => void }) {
  const project = useStore((s) => s.project)
  const loadProject = useStore((s) => s.loadProject)
  const resetProject = useStore((s) => s.resetProject)

  const fileBase = (project.project || 'menus').replace(/[^a-z0-9_-]+/gi, '_') || 'menus'

  const doImport = async () => {
    const text = await pickTextFile()
    if (!text) return
    try {
      loadProject(parseProject(text))
    } catch (e) {
      alert(`Import failed: ${(e as Error).message}`)
    }
  }

  return (
    <div className="flex items-center gap-2 border-b border-black/40 bg-[#1d1d1d] px-3 py-2">
      <div className="text-sm font-bold text-emerald-400">
        mlib<span className="text-gray-300"> menu editor</span>
      </div>
      <div className="ml-4 flex gap-1">
        <Button variant="ghost" onClick={() => confirm('Start a new project? (Your current one stays autosaved until you overwrite it.)') && resetProject()}>
          New
        </Button>
        <Button variant="ghost" onClick={doImport}>
          Import
        </Button>
        <Button variant="ghost" onClick={() => downloadText(`${fileBase}.yml`, toYaml(project), 'text/yaml')}>
          Export YAML
        </Button>
        <Button variant="ghost" onClick={() => downloadText(`${fileBase}.json`, toJson(project), 'application/json')}>
          Export JSON
        </Button>
        <Button variant="ghost" onClick={() => downloadText(`${fileBase}.kt`, toKotlin(project), 'text/plain')} title="Generate mlib Kotlin source">
          Export Kotlin
        </Button>
        <span className="mx-1 w-px self-stretch bg-black/40" />
        <Button variant="ghost" onClick={onShowGraph} title="Visualize menu chaining">
          Chaining
        </Button>
      </div>
      <div className="ml-auto text-[11px] text-gray-500">format v{project.formatVersion} · autosaved locally</div>
    </div>
  )
}
