/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        poke: {
          red: '#ef5350',
          blue: '#3b82f6',
          yellow: '#fbbf24',
          dark: '#1e293b',
        },
      },
    },
  },
  plugins: [],
};
