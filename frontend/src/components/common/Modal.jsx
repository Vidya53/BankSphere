import { useEffect } from 'react';
import { X } from 'lucide-react';

export function Modal({ open, onClose, title, description, children, footer, size = 'md' }) {
  useEffect(() => {
    if (!open) return;
    const onKey = (e) => e.key === 'Escape' && onClose?.();
    document.addEventListener('keydown', onKey);
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', onKey);
      document.body.style.overflow = '';
    };
  }, [open, onClose]);

  if (!open) return null;

  const widths = { sm: 'max-w-md', md: 'max-w-lg', lg: 'max-w-2xl', xl: 'max-w-4xl' };

  return (
    <div className="fixed inset-0 z-50 grid place-items-center p-4">
      <div className="absolute inset-0 bg-accent-ink/50 backdrop-blur-sm animate-in fade-in dark:bg-black/70" onClick={onClose} />
      {/* Modal frame — capped at 90vh so the body can scroll internally on small screens.
          Header and footer stay pinned via flex layout; only the children area scrolls. */}
      <div className={`relative w-full ${widths[size]} max-h-[90vh] bg-white dark:bg-ink-800 rounded-2xl shadow-elevated border border-accent-line dark:border-ink-700 overflow-hidden flex flex-col`}>
        <header className="flex items-start justify-between gap-4 p-6 border-b border-accent-line dark:border-ink-700 shrink-0">
          <div>
            <h2 className="text-lg font-semibold text-accent-ink dark:text-ink-100">{title}</h2>
            {description && <p className="text-sm text-accent-mute mt-0.5 dark:text-ink-400">{description}</p>}
          </div>
          <button onClick={onClose} className="text-accent-mute hover:text-accent-ink transition dark:text-ink-400 dark:hover:text-ink-100 shrink-0" aria-label="Close">
            <X size={20} />
          </button>
        </header>
        <div className="p-6 overflow-y-auto flex-1 min-h-0">{children}</div>
        {footer && <footer className="px-6 py-4 border-t border-accent-line bg-accent-surface/40 dark:border-ink-700 dark:bg-ink-850 shrink-0">{footer}</footer>}
      </div>
    </div>
  );
}
