import { useState } from 'react';
import { Bell, CheckCheck, Info, ShieldCheck, AlertTriangle } from 'lucide-react';
import toast from 'react-hot-toast';
import { PageHeader } from '../../components/layout/PageHeader';
import { Card, CardHeader } from '../../components/common/Card';
import { Badge } from '../../components/common/Badge';
import { Button } from '../../components/common/Button';
import { Skeleton } from '../../components/common/Skeleton';
import { EmptyState } from '../../components/common/EmptyState';
import { useAsync } from '../../hooks/useAsync';
import { notificationApi } from '../../api/notification';
import { formatRelative } from '../../utils/format';

const ICONS = {
  ACCOUNT:  Info,
  SECURITY: ShieldCheck,
  ALERT:    AlertTriangle,
  DEFAULT:  Bell,
};

const FILTERS = [
  { key: 'all',    label: 'All' },
  { key: 'unread', label: 'Unread' },
  { key: 'alerts', label: 'Alerts' },
];

export default function Notifications() {
  const [filter, setFilter] = useState('all');

  const { data: items, loading, reload } = useAsync(
    () => notificationApi.myNotifications().catch(() => []),
    []
  );

  const list = Array.isArray(items) ? items : (items?.content || []);

  const filtered = list.filter((n) => {
    if (filter === 'unread') return !n.read;
    if (filter === 'alerts') return n.type === 'ALERT';
    return true;
  });

  const markAllRead = async () => {
    try {
      await notificationApi.markAllRead();
      toast.success('All notifications marked as read');
      reload();
    } catch {
      toast.error('Could not update notifications');
    }
  };

  return (
    <div className="space-y-8 max-w-4xl">
      <PageHeader
        breadcrumb="Account"
        title="Notifications"
        subtitle="Account alerts, transaction confirmations and important system messages."
        actions={
          <Button variant="secondary" icon={CheckCheck} onClick={markAllRead}>
            Mark all as read
          </Button>
        }
      />

      <Card>
        <div className="flex items-center gap-2 mb-6">
          {FILTERS.map((f) => (
            <button
              key={f.key}
              onClick={() => setFilter(f.key)}
              className={`px-4 py-1.5 rounded-full text-sm font-medium transition ${
                filter === f.key
                  ? 'bg-brand-700 text-white'
                  : 'bg-accent-surface dark:bg-ink-850 text-accent-slate dark:text-ink-300 hover:text-accent-ink dark:text-ink-100'
              }`}
            >
              {f.label}
            </button>
          ))}
        </div>

        {loading ? (
          <div className="space-y-2">
            {Array.from({ length: 5 }).map((_, i) => <Skeleton key={i} className="h-16" />)}
          </div>
        ) : filtered.length === 0 ? (
          <EmptyState icon={Bell} title="All caught up" message="You don't have any notifications in this view." />
        ) : (
          <ul className="divide-y divide-accent-line">
            {filtered.map((n) => {
              const Icon = ICONS[n.type] || ICONS.DEFAULT;
              return (
                <li key={n.id} className={`py-4 flex items-start gap-4 ${!n.read ? 'bg-brand-50/30 -mx-6 px-6' : ''}`}>
                  <span className={`h-10 w-10 rounded-full grid place-items-center shrink-0 ${!n.read ? 'bg-brand-100 text-brand-700' : 'bg-accent-line/60 text-accent-slate dark:text-ink-300'}`}>
                    <Icon size={16} />
                  </span>
                  <div className="flex-1">
                    <div className="flex items-center justify-between gap-2">
                      <p className="text-sm font-semibold">{n.title}</p>
                      <div className="flex items-center gap-2">
                        {!n.read && <Badge tone="brand" dot>New</Badge>}
                        <span className="text-xs text-accent-mute dark:text-ink-400">{formatRelative(n.createdAt)}</span>
                      </div>
                    </div>
                    <p className="text-sm text-accent-slate dark:text-ink-300 mt-1">{n.message}</p>
                  </div>
                </li>
              );
            })}
          </ul>
        )}
      </Card>

      <Card>
        <CardHeader title="Notification preferences" subtitle="Control how we reach you" className="mb-4" />
        <div className="space-y-3">
          {[
            { key: 'tx',    label: 'Transaction alerts',     desc: 'Every deposit, withdrawal and transfer', enabled: true },
            { key: 'sec',   label: 'Security alerts',         desc: 'Sign-ins, password changes, etc.',       enabled: true },
            { key: 'promo', label: 'Offers & promotions',      desc: 'Personalised offers and product news',   enabled: false },
            { key: 'emi',   label: 'EMI reminders',            desc: 'Loan EMI due in 3 days',                  enabled: true },
          ].map((pref) => (
            <label key={pref.key} className="flex items-center justify-between gap-4 p-4 rounded-xl border border-accent-line dark:border-ink-700 cursor-pointer hover:bg-accent-surface/40">
              <div>
                <p className="text-sm font-semibold">{pref.label}</p>
                <p className="text-xs text-accent-mute dark:text-ink-400">{pref.desc}</p>
              </div>
              <input type="checkbox" defaultChecked={pref.enabled} className="h-4 w-4 accent-brand-700" />
            </label>
          ))}
        </div>
      </Card>
    </div>
  );
}
