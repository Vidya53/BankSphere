import { useState } from 'react';
import { Link } from 'react-router-dom';
import { KeyRound, Bell, Trash2, ChevronRight, LogOut, ShieldCheck, Smartphone, Sun, Moon, Monitor } from 'lucide-react';
import toast from 'react-hot-toast';
import { PageHeader } from '../../components/layout/PageHeader';
import { Card, CardHeader } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { Modal } from '../../components/common/Modal';
import { Badge } from '../../components/common/Badge';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';

const SETTINGS_LINKS = [
  { to: '/app/settings/security',      icon: KeyRound, title: 'Password & security', desc: 'Change your password and manage 2FA' },
  { to: '/app/settings/notifications', icon: Bell,     title: 'Notification preferences', desc: 'Channels, categories and quiet hours' },
  { to: '/app/profile',                icon: ShieldCheck, title: 'Profile & KYC',         desc: 'Personal details, address and verification' },
];

export default function Settings() {
  const { logout } = useAuth();
  const [deleteOpen, setDeleteOpen] = useState(false);

  return (
    <div className="space-y-8 max-w-3xl">
      <PageHeader
        breadcrumb="Account"
        title="Settings"
        subtitle="Manage security, sessions, notification preferences and account controls."
      />

      <AppearanceCard />

      <Card>
        <CardHeader title="Account" subtitle="Quick access to your account settings" className="mb-4" />
        <ul className="divide-y divide-accent-line">
          {SETTINGS_LINKS.map(({ to, icon: Icon, title, desc }) => (
            <li key={to}>
              <Link to={to} className="flex items-center gap-4 py-4 hover:bg-accent-surface/40 -mx-6 px-6 transition group">
                <span className="h-10 w-10 rounded-xl bg-brand-50 text-brand-700 grid place-items-center">
                  <Icon size={18} />
                </span>
                <div className="flex-1">
                  <p className="font-semibold">{title}</p>
                  <p className="text-xs text-accent-mute dark:text-ink-400 mt-0.5">{desc}</p>
                </div>
                <ChevronRight size={16} className="text-accent-mute dark:text-ink-400 group-hover:text-brand-700 transition" />
              </Link>
            </li>
          ))}
        </ul>
      </Card>

      <Card>
        <CardHeader title="Active sessions" subtitle="Devices that are currently signed in" className="mb-4" />
        <ul className="divide-y divide-accent-line">
          {[
            { device: 'Chrome on Windows', location: 'Pune, IN', current: true,  lastActive: 'Just now',     icon: Smartphone },
            { device: 'Safari on iPhone',  location: 'Pune, IN', current: false, lastActive: '2 hours ago',  icon: Smartphone },
          ].map((s, i) => {
            const Icon = s.icon;
            return (
              <li key={i} className="py-4 flex items-center gap-4">
                <span className="h-10 w-10 rounded-xl bg-accent-line/60 text-accent-slate dark:text-ink-300 grid place-items-center">
                  <Icon size={16} />
                </span>
                <div className="flex-1">
                  <p className="text-sm font-semibold">{s.device}</p>
                  <p className="text-xs text-accent-mute dark:text-ink-400">{s.location} · {s.lastActive}</p>
                </div>
                {s.current ? (
                  <Badge tone="success" dot>Current session</Badge>
                ) : (
                  <Button variant="ghost" size="sm" icon={LogOut}>Sign out</Button>
                )}
              </li>
            );
          })}
        </ul>
        <div className="mt-4 pt-4 border-t border-accent-line dark:border-ink-700 flex justify-end">
          <Button variant="secondary" icon={LogOut} onClick={logout}>Sign out of this session</Button>
        </div>
      </Card>

      <Card>
        <CardHeader title="Danger zone" subtitle="Irreversible actions — please proceed with caution" className="mb-4" />
        <div className="rounded-xl border border-red-100 bg-red-50/40 p-5 flex flex-wrap items-center justify-between gap-3">
          <div>
            <p className="font-semibold text-accent-danger">Delete my account</p>
            <p className="text-sm text-accent-slate dark:text-ink-300 mt-1 max-w-md">
              Permanently delete your profile and revoke all sessions. You'll lose access to all linked accounts.
            </p>
          </div>
          <Button variant="danger" icon={Trash2} onClick={() => setDeleteOpen(true)}>Delete account</Button>
        </div>
      </Card>

      <Modal
        open={deleteOpen}
        onClose={() => setDeleteOpen(false)}
        title="Are you sure?"
        description="This will permanently delete your account."
        footer={
          <div className="flex justify-end gap-2">
            <Button variant="ghost" onClick={() => setDeleteOpen(false)}>Cancel</Button>
            <Button variant="danger" onClick={() => { setDeleteOpen(false); toast.error('Deletion flow is staffed manually for safety. Please reach out to support.'); }}>Yes, delete</Button>
          </div>
        }
      >
        <p className="text-sm text-accent-slate dark:text-ink-300">
          For your safety, account deletion requires confirmation by our customer service team.
          Once you confirm here, a request will be raised and we'll reach out within 24 hours.
        </p>
      </Modal>
    </div>
  );
}

// ── Appearance / theme picker ────────────────────────────────────────────────
function AppearanceCard() {
  const { theme, setTheme, followSystem, resetToSystem } = useTheme();
  const current = followSystem ? 'system' : theme;

  const options = [
    { value: 'light',  label: 'Light',  description: 'Bright background for daytime', icon: Sun },
    { value: 'dark',   label: 'Dark',   description: 'Easier on the eyes at night',   icon: Moon },
    { value: 'system', label: 'System', description: "Follow your device's theme",    icon: Monitor },
  ];

  const onPick = (value) => {
    if (value === 'system') resetToSystem();
    else setTheme(value);
  };

  return (
    <Card>
      <CardHeader title="Appearance" subtitle="Choose how BankSphere looks to you" className="mb-4" />
      <div className="grid sm:grid-cols-3 gap-3">
        {options.map(({ value, label, description, icon: Icon }) => {
          const active = current === value;
          return (
            <button
              key={value}
              type="button"
              onClick={() => onPick(value)}
              className={`p-4 rounded-xl border text-left transition focus-ring ${
                active
                  ? 'border-brand-500 bg-brand-50 dark:bg-brand-900/30 dark:border-brand-400'
                  : 'border-accent-line dark:border-ink-700 hover:bg-accent-surface/40 dark:hover:bg-ink-750'
              }`}
            >
              <div className="flex items-center justify-between">
                <span className={`h-10 w-10 rounded-xl grid place-items-center ${
                  active ? 'bg-brand-700 text-white dark:bg-brand-600' : 'bg-accent-line/60 text-accent-slate dark:bg-ink-700 dark:text-ink-300'
                }`}>
                  <Icon size={18} />
                </span>
                {active && <Badge tone="brand" dot>Active</Badge>}
              </div>
              <p className="mt-4 font-semibold text-accent-ink dark:text-ink-100">{label}</p>
              <p className="text-xs text-accent-mute dark:text-ink-400 mt-1">{description}</p>
            </button>
          );
        })}
      </div>
    </Card>
  );
}
