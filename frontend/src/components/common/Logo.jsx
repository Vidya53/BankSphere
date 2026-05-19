export function Logo({ className = '', tone = 'brand' }) {
  const color = tone === 'white' ? '#fff' : '#97144D';
  const gold = '#c9a35d';
  return (
    <span className={`inline-flex items-center gap-2.5 ${className}`}>
      <svg viewBox="0 0 64 64" width="34" height="34" aria-hidden="true">
        <rect width="64" height="64" rx="14" fill={color} className="dark:[fill:#cf2f64]" />
        <path d="M14 46 L32 14 L50 46 Z" fill="none" stroke="#fff" strokeWidth="4" strokeLinejoin="round" />
        <circle cx="32" cy="38" r="4" fill={gold} />
      </svg>
      <span className={`font-display font-extrabold text-lg tracking-tight ${tone === 'white' ? 'text-white' : 'text-accent-ink dark:text-ink-100'}`}>
        Bank<span className={tone === 'white' ? '' : 'text-brand-700 dark:text-brand-300'} style={tone === 'white' ? { color } : undefined}>Sphere</span>
      </span>
    </span>
  );
}
