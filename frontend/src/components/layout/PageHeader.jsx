export function PageHeader({ title, subtitle, breadcrumb, actions, className = '' }) {
  return (
    <div className={`flex flex-wrap items-start justify-between gap-4 mb-8 ${className}`}>
      <div>
        {breadcrumb && (
          <p className="text-xs font-medium text-accent-mute uppercase tracking-widest mb-2 dark:text-ink-400">
            {breadcrumb}
          </p>
        )}
        <h1 className="font-display text-2xl md:text-3xl font-extrabold text-accent-ink dark:text-ink-50">{title}</h1>
        {subtitle && <p className="text-sm text-accent-slate mt-1.5 max-w-2xl dark:text-ink-300">{subtitle}</p>}
      </div>
      {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
    </div>
  );
}
