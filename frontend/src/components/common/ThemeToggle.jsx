import { Sun, Moon } from 'lucide-react';
import { useTheme } from '../../context/ThemeContext';

export function ThemeToggle({ className = '', size = 18 }) {
  const { isDark, toggleTheme } = useTheme();
  return (
    <button
      type="button"
      onClick={toggleTheme}
      aria-label={isDark ? 'Switch to light mode' : 'Switch to dark mode'}
      title={isDark ? 'Switch to light mode' : 'Switch to dark mode'}
      className={`relative h-10 w-10 rounded-full grid place-items-center transition focus-ring text-accent-slate hover:bg-accent-surface dark:text-gray-300 dark:hover:bg-gray-800 ${className}`}
    >
      <span className="relative h-[1em] w-[1em] grid place-items-center" style={{ fontSize: size }}>
        <Sun size={size} className={`absolute transition-all duration-300 ${isDark ? 'opacity-0 -rotate-90 scale-50' : 'opacity-100 rotate-0 scale-100'}`} />
        <Moon size={size} className={`absolute transition-all duration-300 ${isDark ? 'opacity-100 rotate-0 scale-100' : 'opacity-0 rotate-90 scale-50'}`} />
      </span>
    </button>
  );
}
