import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// base: './' keeps the build host-agnostic so it works from a GitHub Pages subpath,
// a custom domain, or `vite preview` without further config.
export default defineConfig({
  base: './',
  plugins: [react()],
})
