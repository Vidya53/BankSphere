import { Link } from 'react-router-dom';
import {
  User, Mail, BadgeCheck, Building2, ShieldCheck,
  KeyRound, Bell, ChevronRight, Calendar,
} from 'lucide-react';
import { PageHeader } from '../../components/layout/PageHeader';
import { Card, CardHeader } from '../../components/common/Card';
import { Badge } from '../../components/common/Badge';
import { useAuth } from '../../context/AuthContext';
import { initials } from '../../utils/format';

const ROLE_LABEL = {
  CSR:                'Customer Service Representative',
  BRANCH_MANAGER:     'Branch Manager',
  LOAN_OFFICER:       'Loan Officer',
  ADMIN:              'Administrator',
  // Legacy fallback if a pre-existing user still carries this role in the DB
  COMPLIANCE_OFFICER: 'Compliance Officer (legacy)',
};

const SHORTCUTS = [
  { to: '/staff/settings/security',      icon: KeyRound, title: 'Change password',         desc: 'Rotate your password — minimum 8 characters' },
  { to: '/staff/settings/notifications', icon: Bell,     title: 'Notification preferences', desc: 'Channels, categories and quiet hours' },
];

export default function StaffProfile() {
  const { user } = useAuth();
  const roleLabel = ROLE_LABEL[user?.role] || user?.role || 'Staff';

  return (
    <div className="space-y-8 max-w-5xl">
      <PageHeader
        breadcrumb="Account"
        title="My profile"
        subtitle="Your staff account details. Personal details on file with HR are managed separately."
      />

      <div className="grid lg:grid-cols-[1fr_2.4fr] gap-6">
        <Card>
          <div className="text-center">
            <span className="mx-auto h-24 w-24 rounded-full bg-brand-700 text-white grid place-items-center font-display text-3xl font-extrabold dark:bg-brand-600">
              {initials(user?.fullName || user?.email)}
            </span>
            <h3 className="mt-4 font-semibold text-accent-ink dark:text-ink-100">{user?.fullName || 'Staff member'}</h3>
            <p className="text-xs text-accent-mute dark:text-ink-400">{user?.email}</p>
            <div className="mt-3 flex justify-center gap-2 flex-wrap">
              <Badge tone="brand">{roleLabel}</Badge>
              {user?.branchCode && <Badge tone="neutral">{user.branchCode}</Badge>}
            </div>
          </div>

          <ul className="mt-6 space-y-2 text-sm">
            <SidebarItem icon={Mail}       label={user?.email || '—'} />
            <SidebarItem icon={BadgeCheck} label={`Staff ID: ${user?.userId || '—'}`} />
            {user?.branchCode && (
              <SidebarItem icon={Building2} label={`Branch ${user.branchCode}`} />
            )}
          </ul>
        </Card>

        <div className="space-y-6">
          <Card>
            <CardHeader title="Identity" subtitle="From your sign-in session" className="mb-5" />
            <dl className="grid sm:grid-cols-2 gap-x-8 gap-y-5">
              <Field label="Full name"   value={user?.fullName || '—'} icon={User} />
              <Field label="Email"        value={user?.email    || '—'} icon={Mail} mono />
              <Field label="Role"         value={roleLabel}             icon={ShieldCheck} />
              {user?.branchCode && (
                <Field label="Branch code" value={user.branchCode} icon={Building2} mono />
              )}
              <Field label="Staff ID"     value={user?.userId   || '—'} icon={BadgeCheck} mono />
              {user?.exp && (
                <Field label="Session expires"
                       value={new Date(user.exp * 1000).toLocaleString('en-IN')}
                       icon={Calendar} />
              )}
            </dl>
          </Card>

          <Card>
            <CardHeader title="Quick actions" subtitle="Common account settings" className="mb-4" />
            <ul className="divide-y divide-accent-line dark:divide-ink-700">
              {SHORTCUTS.map(({ to, icon: Icon, title, desc }) => (
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

          <div className="rounded-xl bg-brand-50/60 dark:bg-brand-900/20 border border-brand-100 dark:border-brand-900/40 p-4 text-sm text-accent-slate dark:text-ink-300">
            Personal information (date of birth, address, etc.) is held in the HR system and isn't editable here.
            Reach out to your branch manager or HR to update those details.
          </div>
        </div>
      </div>
    </div>
  );
}

function SidebarItem({ icon: Icon, label }) {
  return (
    <li className="flex items-center gap-3 px-3 py-2 rounded-lg bg-accent-surface/60 dark:bg-ink-850">
      <Icon size={14} className="text-accent-mute dark:text-ink-400 shrink-0" />
      <span className="truncate dark:text-ink-100">{label}</span>
    </li>
  );
}

function Field({ label, value, icon: Icon, mono }) {
  return (
    <div>
      <dt className="text-xs uppercase tracking-widest text-accent-mute dark:text-ink-400 flex items-center gap-1.5 mb-1">
        {Icon && <Icon size={12} />} {label}
      </dt>
      <dd className={`text-sm font-semibold text-accent-ink dark:text-ink-100 ${mono ? 'font-mono' : ''}`}>
        {value || '—'}
      </dd>
    </div>
  );
}
