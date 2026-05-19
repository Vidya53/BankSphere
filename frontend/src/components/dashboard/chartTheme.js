import { useTheme } from '../../context/ThemeContext';

// Centralised Recharts colour tokens. Components call useChartTheme() to get
// the right values for the current theme, so chart aesthetics stay consistent
// with the rest of the app when the user toggles light/dark.
export function useChartTheme() {
  const { isDark } = useTheme();
  return {
    isDark,
    grid:       isDark ? '#2d2d36' : '#f0f0f0',
    axis:       isDark ? '#a3a3b0' : '#737373',
    axisLine:   isDark ? '#3f3f4a' : '#e5e5e5',
    label:      isDark ? '#e6e6eb' : '#1a1a1a',
    tooltipBg:  isDark ? '#1c1c24' : '#ffffff',
    tooltipBorder: isDark ? '#3f3f4a' : '#e5e5e5',
    tooltipShadow: isDark
      ? '0 8px 24px rgba(0,0,0,0.5)'
      : '0 8px 24px rgba(0,0,0,0.06)',
    // Series palette — brighter shades in dark for contrast on the dark surface
    series: isDark
      ? ['#ee8eaf', '#d4b777', '#5fa3e8', '#5cc28a', '#e2a55a', '#a3a3b0']
      : ['#97144D', '#c9a35d', '#1e6fd6', '#0a7b3f', '#c47a00', '#7d1241'],
    // Chart-specific
    areaFillStart: isDark ? '#ee8eaf' : '#97144D',
    areaFillOpacityStart: isDark ? 0.35 : 0.28,
    areaStroke: isDark ? '#ee8eaf' : '#97144D',
  };
}

export const tooltipStyle = (t) => ({
  borderRadius: 12,
  border: `1px solid ${t.tooltipBorder}`,
  backgroundColor: t.tooltipBg,
  boxShadow: t.tooltipShadow,
  color: t.label,
});

export const labelStyle = (t) => ({
  fontWeight: 600,
  color: t.label,
});
