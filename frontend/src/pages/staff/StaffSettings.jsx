import { Link } from 'react-router-dom';
import {
  KeyRound, Bell, ChevronRight, LogOut, User, Smartphone, Moon, Sun, Monitor,
} from 'lucide-react';
import { PageHeader } from '../../components/layout/PageHeader';
import { Card, CardHeader } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';

const SETTINGS_LINKS = [
  { to: '/staff/profile',                icon: User,    title: 'My profile',               desc: 'Your name, role and branch' },
  { to: '/staff/settings/security',      icon: KeyRound, title: 'Password & security',     desc: 'Change your password' },
  { to: '/staff/settings/notifications', icon: Bell,    title: 'Notification preferences', desc: 'Channels, categories and quiet hours' },
];

export default function StaffSettings() {
  const { user, logout } = useAuth();
  return (
    <div className="space-y-8 max-w-3xl">
      <PageHeader
        breadcrumb="Account"
        title="Settings"
        subtitle="Security, sessions, theme and notification preferences."
      />

      <Card>
        <CardHeader title="Account" subtitle="Quick access to your account settings" className="mb-4" />
        <ul className="divide-y divide-accent-line dark:divide-ink-700">
          {SETTINGS_LINKS.map(({ to, icon: Icon, title, desc }) => (
            <li key={to}>
              <Link to={to} className="flex items-center gap-4 py-4 hover:bg-accent-surface/40 dark:hover:bg-ink-750/40 -mx-6 px-6 transition group">
                <span className="h-10 w-10 rounded-xl bg-brand-50 text-brand-700 dark:bg-brand-900/40 dark:text-brand-300 grid place-items-center">
                  <Icon size={18} />
                </span>
                <div className="flex-1">
                  <p className="font-semibold dark:text-ink-100">{title}</p>
                  <p className="text-xs text-accent-mute dark:text-ink-400 mt-0.5">{desc}</p>
                </div>
                <ChevronRight size={16} className="text-accent-mute dark:text-ink-400 group-hover:text-brand-700 transition" />
              </Link>
            </li>
          ))}
        </ul>
      </Card>

      <AppearanceCard />

      <Card>
        <CardHeader title="Active sessions" subtitle="Devices currently signed in" className="mb-4" />
        <ul className="divide-y divide-accent-line dark:divide-ink-700">
          <li className="py-4 flex items-center gap-4">
            <span className="h-10 w-10 rounded-xl bg-accent-line/60 dark:bg-ink-700 text-accent-slate dark:text-ink-300 grid place-items-center">
              <Smartphone size={16} />
            </span>
            <div className="flex-1">
              <p className="text-sm font-semibold dark:text-ink-100">This device</p>
              <p className="text-xs text-accent-mute dark:text-ink-400">{user?.email}</p>
            </div>
            <Badge tone="success" dot>Current session</Badge>
          </li>
        </ul>
        <div className="mt-4 pt-4 border-t border-accent-line dark:border-ink-700 flex justify-end">
          <Button variant="secondary" icon={LogOut} onClick={logout}>Sign out of this session</Button>
        </div>
      </Card>
    </div>
  );
}

function AppearanceCard() {
  const { theme, setTheme, followSystem, resetToSystem } = useTheme();
  const current = followSystem ? 'system' : theme;

  const options = [
    { value: 'light',  label: 'Light',  desc: 'Bright background for daytime', icon: Sun },
    { value: 'dark',   label: 'Dark',   desc: 'Easier on the eyes at night',   icon: Moon },
    { value: 'system', label: 'System', desc: "Follow your device's theme",    icon: Monitor },
  ];

  const onPick = (v) => v === 'system' ? resetToSystem() : setTheme(v);

  return (
    <Card>
      <CardHeader title="Appearance" subtitle="Choose how BankSphere looks to you" className="mb-4" />
      <div className="grid sm:grid-cols-3 gap-3">
        {options.map(({ value, label, desc, icon: Icon }) => {
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
                  active ? 'bg-brand-700 text-white dark:bg-brand-600' : 'bg-accent-line/60 dark:bg-ink-700 text-accent-slate dark:text-ink-300'
                }`}>
                  <Icon size={18} />
                </span>
                {active && <Badge tone="brand" dot>Active</Badge>}
              </div>
              <p className="mt-4 font-semibold text-accent-ink dark:text-ink-100">{label}</p>
              <p className="text-xs text-accent-mute dark:text-ink-400 mt-1">{desc}</p>
            </button>
          );
        })}
      </div>
    </Card>
  );
}
