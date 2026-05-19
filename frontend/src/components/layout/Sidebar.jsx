import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard, Wallet, ArrowLeftRight, Send, Landmark,
  ShieldCheck, Bell, Settings, HelpCircle, LogOut, BarChart3,
  Users, TicketCheck, KeyRound, SlidersHorizontal,
} from 'lucide-react';
import { Logo } from '../common/Logo';
import { useAuth } from '../../context/AuthContext';
import { ROLES } from '../../utils/roleRoutes';

const CUSTOMER_NAV = [
  { to: '/app',                  label: 'Dashboard',      icon: LayoutDashboard, end: true },
  { to: '/app/accounts',         label: 'My Accounts',    icon: Wallet },
  { to: '/app/transactions',     label: 'Transactions',   icon: ArrowLeftRight },
  { to: '/app/transfer',         label: 'Transfer Money', icon: Send },
  { to: '/app/security/pin',     label: 'Transaction PIN',icon: KeyRound },
  { to: '/app/loans',            label: 'Loans',          icon: Landmark },
  { to: '/app/kyc',              label: 'KYC',            icon: ShieldCheck },
  { to: '/app/support',          label: 'Support',        icon: TicketCheck },
  { to: '/app/notifications',    label: 'Notifications',  icon: Bell },
];

const STAFF_NAV = {
  CSR: [
    { to: '/staff/csr',                   label: 'CSR Dashboard',     icon: LayoutDashboard, end: true },
    { to: '/staff/cash',                  label: 'Cash Counter',      icon: ArrowLeftRight },
    { to: '/staff/kyc-review',            label: 'KYC Review',        icon: ShieldCheck },
    { to: '/staff/account-applications',  label: 'Account Applications', icon: Wallet },
    { to: '/staff/pending-transfers',     label: 'High-value Transfers', icon: Send },
    { to: '/staff/support',               label: 'Support Queue',     icon: TicketCheck },
    { to: '/staff/analytics',             label: 'Analytics',         icon: BarChart3 },
  ],
  BRANCH_MANAGER: [
    { to: '/staff/branch',                label: 'Branch Dashboard',  icon: LayoutDashboard, end: true },
    { to: '/staff/csr',                   label: 'CSR Operations',    icon: TicketCheck },
    { to: '/staff/cash',                  label: 'Cash Counter',      icon: ArrowLeftRight },
    { to: '/staff/kyc-review',            label: 'KYC Review',        icon: ShieldCheck },
    { to: '/staff/account-applications',  label: 'Account Applications', icon: Wallet },
    { to: '/staff/pending-transfers',     label: 'High-value Transfers', icon: Send },
    { to: '/staff/loans-ops',             label: 'Loan Pipeline',     icon: Landmark },
    { to: '/staff/analytics',             label: 'Analytics',         icon: BarChart3 },
  ],
  LOAN_OFFICER: [
    { to: '/staff/loans-ops',      label: 'Loan Dashboard', icon: LayoutDashboard, end: true },
    { to: '/staff/analytics',      label: 'Analytics',      icon: BarChart3 },
  ],
  ADMIN: [
    { to: '/staff/admin',                 label: 'Admin Console',     icon: LayoutDashboard, end: true },
    { to: '/staff/analytics',             label: 'Analytics',         icon: BarChart3 },
    { to: '/staff/csr',                   label: 'CSR Queue',         icon: TicketCheck },
    { to: '/staff/cash',                  label: 'Cash Counter',      icon: ArrowLeftRight },
    { to: '/staff/kyc-review',            label: 'KYC Review',        icon: ShieldCheck },
    { to: '/staff/account-applications',  label: 'Account Applications', icon: Wallet },
    { to: '/staff/pending-transfers',     label: 'High-value Transfers', icon: Send },
    { to: '/staff/branch',                label: 'Branch View',       icon: Users },
    { to: '/staff/loans-ops',             label: 'Loans',             icon: Landmark },
  ],
};

const FOOTER_NAV_CUSTOMER = [
  { to: '/app/settings',                label: 'Settings',          icon: Settings },
  { to: '/app/settings/security',       label: 'Change Password',   icon: KeyRound },
  { to: '/app/settings/notifications',  label: 'Notification Prefs',icon: SlidersHorizontal },
  { to: '/app/help',                    label: 'Help & Support',    icon: HelpCircle },
];

const FOOTER_NAV_STAFF = [
  { to: '/staff/settings/security',      label: 'Change Password',   icon: KeyRound },
  { to: '/staff/settings/notifications', label: 'Notification Prefs',icon: SlidersHorizontal },
];

export function Sidebar({ collapsed = false }) {
  const { user, logout } = useAuth();
  const role = user?.role;
  const isCustomer = role === ROLES.CUSTOMER;

  const main = isCustomer ? CUSTOMER_NAV : (STAFF_NAV[role] || STAFF_NAV.ADMIN);
  const footer = isCustomer ? FOOTER_NAV_CUSTOMER : FOOTER_NAV_STAFF;

  return (
    <aside className={`hidden lg:flex flex-col h-screen sticky top-0 border-r border-accent-line bg-white dark:bg-ink-900 dark:border-ink-700 ${collapsed ? 'w-20' : 'w-64'} transition-all`}>
      <div className="px-5 h-20 flex items-center border-b border-accent-line dark:border-ink-700">
        {collapsed ? (
          <div className="h-9 w-9 rounded-xl bg-brand-700 grid place-items-center text-white font-display font-extrabold dark:bg-brand-600">B</div>
        ) : (
          <Logo />
        )}
      </div>

      <nav className="flex-1 overflow-y-auto p-3 space-y-1">
        <p className={`px-3 pt-3 pb-1 text-[11px] font-semibold uppercase tracking-widest text-accent-mute dark:text-ink-400 ${collapsed ? 'hidden' : ''}`}>
          {isCustomer ? 'Banking' : `${role?.replace(/_/g, ' ')}`}
        </p>
        {main.map((item) => <NavItem key={item.to} {...item} collapsed={collapsed} />)}

        <p className={`px-3 pt-5 pb-1 text-[11px] font-semibold uppercase tracking-widest text-accent-mute dark:text-ink-400 ${collapsed ? 'hidden' : ''}`}>
          Account
        </p>
        {footer.map((item) => <NavItem key={item.to} {...item} collapsed={collapsed} />)}
      </nav>

      <div className="p-3 border-t border-accent-line dark:border-ink-700">
        <button
          onClick={logout}
          className="w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium text-accent-slate hover:bg-red-50 hover:text-accent-danger transition dark:text-ink-300 dark:hover:bg-red-950/30 dark:hover:text-red-400"
        >
          <LogOut size={18} />
          {!collapsed && <span>Sign out</span>}
        </button>
      </div>
    </aside>
  );
}

function NavItem({ to, label, icon: Icon, end, collapsed }) {
  return (
    <NavLink
      to={to}
      end={end}
      className={({ isActive }) =>
        `relative flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition group ${
          isActive
            ? 'bg-brand-50 text-brand-700 dark:bg-brand-900/40 dark:text-brand-200'
            : 'text-accent-slate hover:bg-accent-surface hover:text-accent-ink dark:text-ink-300 dark:hover:bg-ink-800 dark:hover:text-ink-100'
        }`
      }
      title={collapsed ? label : undefined}
    >
      {({ isActive }) => (
        <>
          {isActive && <span className="absolute left-0 top-2 bottom-2 w-1 rounded-r-full bg-brand-700 dark:bg-brand-400" />}
          <Icon size={18} />
          {!collapsed && <span>{label}</span>}
        </>
      )}
    </NavLink>
  );
}
