/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        // Minecraft-ish inventory chrome
        panel: '#2b2b2b',
        slot: '#8b8b8b',
        slotDark: '#373737',
        inv: '#c6c6c6',
      },
      fontFamily: {
        mc: ['"Minecraftia"', 'ui-monospace', 'monospace'],
      },
    },
  },
  plugins: [],
}
