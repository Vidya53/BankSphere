import { Inbox } from 'lucide-react';

export function EmptyState({ title = 'Nothing here yet', message, icon: Icon = Inbox, action, className = '' }) {
  return (
    <div className={`flex flex-col items-center justify-center text-center py-12 ${className}`}>
      <div className="h-14 w-14 rounded-full bg-brand-50 grid place-items-center text-brand-700 dark:bg-brand-900/40 dark:text-brand-300">
        <Icon size={24} />
      </div>
      <h4 className="mt-4 text-base font-semibold text-accent-ink dark:text-ink-100">{title}</h4>
      {message && <p className="mt-1 text-sm text-accent-mute max-w-sm dark:text-ink-400">{message}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  );
}
