/** @type {import('tailwindcss').Config} */
export default {
  darkMode: 'class',
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        brand: {
          50:  '#fdf2f5',
          100: '#fbe5ed',
          200: '#f6c0d4',
          300: '#ee8eaf',
          400: '#e25584',
          500: '#cf2f64',
          600: '#b81e54',
          700: '#97144D',
          800: '#7d1241',
          900: '#5e0d31',
          950: '#3a0719',
        },
        accent: {
          gold:    '#c9a35d',
          ink:     '#1a1a1a',
          slate:   '#4a4a4a',
          mute:    '#737373',
          line:    '#e5e5e5',
          surface: '#fafafa',
          success: '#0a7b3f',
          warning: '#c47a00',
          danger:  '#c52828',
          info:    '#1e6fd6',
        },
        // Dark-mode surface tokens — used directly in components with `dark:` prefix
        ink: {
          DEFAULT: '#0b0b10',  // page background
          50:  '#f5f5f7',
          100: '#e6e6eb',
          200: '#c9c9d2',
          300: '#a3a3b0',
          400: '#7c7c8a',
          500: '#5a5a66',
          600: '#3f3f4a',
          700: '#2d2d36',  // card border
          750: '#23232b',  // raised surface
          800: '#1c1c24',  // card background
          850: '#161620',
          900: '#0f0f15',  // app shell background
          950: '#08080c',
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'Segoe UI', 'Roboto', 'sans-serif'],
        display: ['"Plus Jakarta Sans"', 'Inter', 'system-ui', 'sans-serif'],
      },
      boxShadow: {
        card: '0 1px 3px rgba(0,0,0,0.04), 0 1px 2px rgba(0,0,0,0.03)',
        cardHover: '0 8px 24px rgba(0,0,0,0.06), 0 2px 6px rgba(0,0,0,0.04)',
        elevated: '0 16px 40px rgba(0,0,0,0.08)',
        cardDark: '0 1px 3px rgba(0,0,0,0.5), 0 1px 2px rgba(0,0,0,0.3)',
        cardHoverDark: '0 12px 32px rgba(0,0,0,0.5), 0 2px 8px rgba(0,0,0,0.4)',
      },
      borderRadius: {
        xl2: '14px',
      },
    },
  },
  plugins: [],
};
