import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';

const STORAGE_KEY = 'bs.theme';
const ThemeContext = createContext(null);

const getSystemPreference = () => {
  if (typeof window === 'undefined' || !window.matchMedia) return 'light';
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
};

const readInitialTheme = () => {
  if (typeof window === 'undefined') return 'light';
  const stored = localStorage.getItem(STORAGE_KEY);
  if (stored === 'light' || stored === 'dark') return stored;
  return getSystemPreference();
};

const applyTheme = (theme) => {
  if (typeof document === 'undefined') return;
  const root = document.documentElement;
  if (theme === 'dark') root.classList.add('dark');
  else root.classList.remove('dark');
  root.style.colorScheme = theme;
};

export function ThemeProvider({ children }) {
  const [theme, setThemeState] = useState(() => readInitialTheme());
  const [followSystem, setFollowSystem] = useState(() => {
    if (typeof window === 'undefined') return false;
    return localStorage.getItem(STORAGE_KEY) == null;
  });

  // Apply on mount and whenever theme changes
  useEffect(() => { applyTheme(theme); }, [theme]);

  // Track system preference changes while the user is in "follow system" mode
  useEffect(() => {
    if (!followSystem || typeof window === 'undefined' || !window.matchMedia) return;
    const mq = window.matchMedia('(prefers-color-scheme: dark)');
    const onChange = (e) => setThemeState(e.matches ? 'dark' : 'light');
    mq.addEventListener?.('change', onChange);
    return () => mq.removeEventListener?.('change', onChange);
  }, [followSystem]);

  const setTheme = useCallback((next) => {
    setFollowSystem(false);
    localStorage.setItem(STORAGE_KEY, next);
    setThemeState(next);
  }, []);

  const toggleTheme = useCallback(() => {
    setTheme(theme === 'dark' ? 'light' : 'dark');
  }, [theme, setTheme]);

  const resetToSystem = useCallback(() => {
    localStorage.removeItem(STORAGE_KEY);
    setFollowSystem(true);
    setThemeState(getSystemPreference());
  }, []);

  const value = useMemo(() => ({
    theme,
    isDark: theme === 'dark',
    followSystem,
    setTheme,
    toggleTheme,
    resetToSystem,
  }), [theme, followSystem, setTheme, toggleTheme, resetToSystem]);

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

export function useTheme() {
  const ctx = useContext(ThemeContext);
  if (!ctx) throw new Error('useTheme must be used inside a ThemeProvider.');
  return ctx;
}
