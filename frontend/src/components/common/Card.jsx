export function Card({ className = '', children, padded = true, hover = false }) {
  const base = hover ? 'card-hover' : 'card';
  return <div className={`${base} ${padded ? 'p-6' : ''} ${className}`}>{children}</div>;
}

export function CardHeader({ title, subtitle, action, className = '' }) {
  return (
    <div className={`flex items-start justify-between gap-4 ${className}`}>
      <div>
        <h3 className="text-lg font-semibold text-accent-ink dark:text-ink-100">{title}</h3>
        {subtitle && <p className="text-sm text-accent-mute mt-0.5 dark:text-ink-400">{subtitle}</p>}
      </div>
      {action}
    </div>
  );
}
