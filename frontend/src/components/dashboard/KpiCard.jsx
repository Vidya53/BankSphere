import { ArrowDownRight, ArrowUpRight } from 'lucide-react';

export function KpiCard({ label, value, sub, delta, icon: Icon, accent = 'brand' }) {
  const positive = (delta ?? 0) >= 0;
  const accents = {
    brand: 'bg-brand-50 text-brand-700 dark:bg-brand-900/40 dark:text-brand-300',
    gold:  'bg-amber-50 text-accent-warning dark:bg-amber-900/30 dark:text-amber-300',
    info:  'bg-sky-50 text-accent-info dark:bg-sky-900/30 dark:text-sky-300',
    green: 'bg-green-50 text-accent-success dark:bg-green-900/30 dark:text-green-300',
  };
  return (
    <div className="card p-5">
      <div className="flex items-start justify-between">
        <span className={`h-10 w-10 rounded-xl grid place-items-center ${accents[accent]}`}>
          {Icon && <Icon size={18} />}
        </span>
        {typeof delta === 'number' && (
          <span className={`chip ${positive ? 'bg-green-50 text-accent-success dark:bg-green-900/30 dark:text-green-300' : 'bg-red-50 text-accent-danger dark:bg-red-900/30 dark:text-red-300'}`}>
            {positive ? <ArrowUpRight size={12} /> : <ArrowDownRight size={12} />}
            {Math.abs(delta).toFixed(1)}%
          </span>
        )}
      </div>
      <div className="mt-5">
        <p className="text-xs font-medium uppercase tracking-widest text-accent-mute dark:text-ink-400">{label}</p>
        <p className="mt-1 text-2xl font-display font-extrabold text-accent-ink dark:text-ink-50">{value}</p>
        {sub && <p className="mt-1 text-xs text-accent-mute dark:text-ink-400">{sub}</p>}
      </div>
    </div>
  );
}
