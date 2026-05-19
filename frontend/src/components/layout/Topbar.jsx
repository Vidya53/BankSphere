import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { ChevronDown, LogOut, Settings, User, ShieldCheck } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { initials } from '../../utils/format';
import { ThemeToggle } from '../common/ThemeToggle';

export function Topbar() {
  const { user, logout } = useAuth();
  const [profileOpen, setProfileOpen] = useState(false);
  const profileRef = useRef(null);

  useEffect(() => {
    const onClick = (e) => {
      if (profileRef.current && !profileRef.current.contains(e.target)) setProfileOpen(false);
    };
    document.addEventListener('mousedown', onClick);
    return () => document.removeEventListener('mousedown', onClick);
  }, []);

  return (
    <header className="sticky top-0 z-30 bg-white/95 backdrop-blur border-b border-accent-line dark:bg-ink-900/95 dark:border-ink-700">
      <div className="h-16 px-6 flex items-center justify-end gap-2">
        <ThemeToggle />

        <div ref={profileRef} className="relative">
          <button
            onClick={() => setProfileOpen((v) => !v)}
            className="flex items-center gap-3 pl-2 pr-3 py-1.5 rounded-full hover:bg-accent-surface transition focus-ring dark:hover:bg-ink-800"
          >
            <span className="h-9 w-9 rounded-full bg-brand-700 text-white grid place-items-center font-semibold text-sm dark:bg-brand-600">
              {initials(user?.fullName || user?.email)}
            </span>
            <div className="hidden sm:block text-left leading-tight">
              <div className="text-sm font-semibold text-accent-ink dark:text-ink-100">{user?.fullName || 'Member'}</div>
              <div className="text-[11px] text-accent-mute uppercase tracking-wider dark:text-ink-400">{user?.role}</div>
            </div>
            <ChevronDown size={14} className="text-accent-mute dark:text-ink-400" />
          </button>
          {profileOpen && (
            <div className="absolute right-0 mt-2 w-64 card p-0 overflow-hidden shadow-elevated">
              <div className="px-4 py-3 border-b border-accent-line dark:border-ink-700">
                <div className="text-sm font-semibold dark:text-ink-100">{user?.fullName || 'Member'}</div>
                <div className="text-xs text-accent-mute mt-0.5 dark:text-ink-400">{user?.email}</div>
              </div>
              <ul className="py-1.5 text-sm">
                {user?.role === 'CUSTOMER' ? (
                  <>
                    <MenuLink to="/app/profile"  icon={User}>My profile</MenuLink>
                    <MenuLink to="/app/kyc"      icon={ShieldCheck}>KYC verification</MenuLink>
                    <MenuLink to="/app/settings" icon={Settings}>Settings</MenuLink>
                  </>
                ) : (
                  <>
                    <MenuLink to="/staff/profile"  icon={User}>My profile</MenuLink>
                    <MenuLink to="/staff/settings" icon={Settings}>Settings</MenuLink>
                  </>
                )}
              </ul>
              <div className="border-t border-accent-line dark:border-ink-700">
                <button
                  onClick={logout}
                  className="w-full flex items-center gap-3 px-4 py-2.5 text-sm text-accent-danger hover:bg-red-50 transition dark:hover:bg-red-950/30"
                >
                  <LogOut size={16} /> Sign out
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}

function MenuLink({ to, icon: Icon, children }) {
  return (
    <li>
      <Link to={to} className="flex items-center gap-3 px-4 py-2.5 hover:bg-accent-surface transition dark:hover:bg-ink-750 dark:text-ink-100">
        <Icon size={16} className="text-accent-slate dark:text-ink-300" />
        <span>{children}</span>
      </Link>
    </li>
  );
}
