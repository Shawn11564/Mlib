# mlib Menu Editor

A visual, browser-based editor for designing Minecraft inventory GUIs and exporting them to the
declarative **[mlib menu format](../docs/MenuFormat.md)** (YAML/JSON) — or to mlib Kotlin source.

It is a standalone static site. It lives in this repo but is **invisible to the library build**:
Maven/JitPack only compile `src/main/kotlin` (see the root `pom.xml`), so nothing here affects
publishing of the `dev.mrshawn:mlib` artifact.

## Develop

```bash
cd editor
npm install
npm run dev        # http://localhost:5173
```

## Build & test

```bash
npm run build      # type-check + production build into dist/
npm test           # round-trip serializer tests (Vitest)
```

## Features

- Browse the full vanilla item/block palette (Minecraft **26.1.2**) and drag assets onto a 1–6 row
  inventory grid.
- Edit every mlib item type (basic, glowing, updating, toggle, cycle, two-stage), full appearance
  (name, lore, enchants, flags, skull/potion/banner, custom model data), and per-click-type
  **actions** with requirements — including **custom actions** wired to your plugin by id.
- Manage multiple menus and visualize their links in the **chaining graph**.
- Export **YAML**, **JSON** (the runtime format), or **Kotlin** (a scaffold), and re-import files for
  round-trip editing. Projects autosave to `localStorage`.

## Assets & licensing

Item/block icons are loaded at runtime from the
[InventivetalentDev/minecraft-assets](https://github.com/InventivetalentDev/minecraft-assets) mirror
via the jsDelivr CDN. **Minecraft assets are © Mojang** and are **not** committed to this repo.
`npm run build:assets` regenerates the material palette (`src/assets-pipeline/materials.json`) from a
version's texture index — retarget with `MC_VERSION=1.21.4 npm run build:assets`.

## Deploy (GitHub Pages)

`.github/workflows/deploy-editor.yml` builds and publishes `dist/` to Pages on pushes that touch
`editor/` or `schema/`. Enable it once in the repo: **Settings → Pages → Build and deployment →
Source: GitHub Actions**. The Vite `base` is `./`, so it also works from any subpath or a custom
host (e.g. Vercel/Netlify with the project root set to `editor`).
