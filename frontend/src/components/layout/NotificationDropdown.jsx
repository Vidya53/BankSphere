import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Bell, CheckCheck, Info, AlertTriangle, ShieldCheck } from 'lucide-react';
import { Spinner } from '../common/Spinner';
import { notificationApi } from '../../api/notification';
import { formatRelative } from '../../utils/format';

const ICONS = {
  ACCOUNT: Info,
  SECURITY: ShieldCheck,
  ALERT: AlertTriangle,
  DEFAULT: Bell,
};

// Fallback notifications shown when the backend endpoint isn't reachable.
// These mirror the kinds of events the notification-service emits.
const FALLBACK = [
  { id: 'demo-1', type: 'ACCOUNT',  title: 'Account application submitted', message: 'Your SAVINGS application is under review.', createdAt: new Date(Date.now() - 6 * 60_000).toISOString(), read: false },
  { id: 'demo-2', type: 'SECURITY', title: 'New device sign-in',             message: 'Sign-in detected from Chrome on Windows.',  createdAt: new Date(Date.now() - 90 * 60_000).toISOString(), read: false },
  { id: 'demo-3', type: 'ALERT',    title: 'KYC documents pending',          message: 'Submit your PAN and Aadhaar to unlock transfers.', createdAt: new Date(Date.now() - 26 * 3600_000).toISOString(), read: true },
];

export function NotificationDropdown({ onClose }) {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    notificationApi.myNotifications()
      .then((data) => { if (!cancelled) setItems(Array.isArray(data) ? data : (data?.content || FALLBACK)); })
      .catch(() => !cancelled && setItems(FALLBACK))
      .finally(() => !cancelled && setLoading(false));
    return () => { cancelled = true; };
  }, []);

  const markAllRead = async () => {
    try { await notificationApi.markAllRead(); } catch {}
    setItems((cur) => cur.map((n) => ({ ...n, read: true })));
  };

  const unreadCount = items.filter((n) => !n.read).length;

  return (
    <div className="absolute right-0 mt-2 w-96 card p-0 overflow-hidden shadow-elevated">
      <div className="px-4 py-3 border-b border-accent-line dark:border-ink-700 flex items-center justify-between">
        <div>
          <h4 className="text-sm font-semibold dark:text-ink-100">Notifications</h4>
          <p className="text-xs text-accent-mute dark:text-ink-400">{unreadCount} unread</p>
        </div>
        {unreadCount > 0 && (
          <button onClick={markAllRead} className="text-xs font-semibold text-brand-700 hover:underline inline-flex items-center gap-1 dark:text-brand-300">
            <CheckCheck size={14} /> Mark all read
          </button>
        )}
      </div>

      <ul className="max-h-96 overflow-y-auto">
        {loading ? (
          <li className="px-4 py-8 grid place-items-center"><Spinner size={20} /></li>
        ) : items.length === 0 ? (
          <li className="px-4 py-10 text-center text-sm text-accent-mute dark:text-ink-400">No notifications yet.</li>
        ) : items.map((n) => {
          const Icon = ICONS[n.type] || ICONS.DEFAULT;
          return (
            <li key={n.id} className={`px-4 py-3 border-b border-accent-line/70 dark:border-ink-700/70 last:border-b-0 hover:bg-accent-surface/60 dark:hover:bg-ink-750/60 ${!n.read ? 'bg-brand-50/30 dark:bg-brand-900/20' : ''}`}>
              <div className="flex items-start gap-3">
                <span className={`h-9 w-9 rounded-full grid place-items-center shrink-0 ${!n.read ? 'bg-brand-100 text-brand-700 dark:bg-brand-900/50 dark:text-brand-300' : 'bg-accent-line/70 text-accent-slate dark:bg-ink-700 dark:text-ink-300'}`}>
                  <Icon size={15} />
                </span>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between gap-2">
                    <p className="text-sm font-semibold text-accent-ink dark:text-ink-100 truncate">{n.title}</p>
                    <span className="text-[11px] text-accent-mute dark:text-ink-400 shrink-0">{formatRelative(n.createdAt)}</span>
                  </div>
                  <p className="text-xs text-accent-slate dark:text-ink-300 mt-0.5 line-clamp-2">{n.message}</p>
                </div>
              </div>
            </li>
          );
        })}
      </ul>

      <div className="px-4 py-2.5 border-t border-accent-line dark:border-ink-700 text-center">
        <Link to="/app/notifications" onClick={onClose} className="text-sm font-semibold text-brand-700 hover:underline dark:text-brand-300">
          View all notifications
        </Link>
      </div>
    </div>
  );
}
