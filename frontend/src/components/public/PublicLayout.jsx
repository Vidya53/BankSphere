import { useEffect, useRef, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { ArrowRight, Menu, X, ChevronDown } from 'lucide-react';
import { Logo } from '../common/Logo';
import { ThemeToggle } from '../common/ThemeToggle';
import {
  MEGA_MENUS, SEGMENT_LINKS, SECONDARY_LINKS, FOOTER_COLUMNS,
} from '../../data/marketingContent';

// Wraps every marketing/public page with the shared TopNav + Footer.
// Landing keeps its bespoke hero, but the nav and footer come from here so
// every page (Accounts, Loans, About, Branches…) looks identical.
export default function PublicLayout({ children }) {
  return (
    <div className="min-h-screen flex flex-col bg-white dark:bg-ink-900 text-accent-ink dark:text-ink-100">
      <TopNav />
      <main className="flex-1">{children}</main>
      <Footer />
    </div>
  );
}

// ── Top navigation ────────────────────────────────────────────────────────────
const PRODUCT_LINK_NAMES = Object.keys(MEGA_MENUS);

function TopNav() {
  const [mobileOpen, setMobileOpen] = useState(false);
  const [openMenu, setOpenMenu] = useState(null);
  const closeTimer = useRef(null);
  const location = useLocation();

  // Close any open menu when the route changes.
  useEffect(() => {
    setMobileOpen(false);
    setOpenMenu(null);
  }, [location.pathname]);

  // Small delay before closing — lets the cursor travel from trigger to panel
  // without the panel vanishing under it.
  const scheduleClose = () => {
    closeTimer.current = setTimeout(() => setOpenMenu(null), 120);
  };
  const cancelClose = () => {
    if (closeTimer.current) {
      clearTimeout(closeTimer.current);
      closeTimer.current = null;
    }
  };

  return (
    <>
      {/* Utility bar — segment links scroll horizontally on phones */}
      <div className="bg-brand-700 text-white text-xs">
        <div className="container-wide h-9 flex items-center justify-between gap-4">
          <div className="flex items-center gap-3 sm:gap-5 opacity-90 overflow-x-auto whitespace-nowrap no-scrollbar">
            {SEGMENT_LINKS.map(({ label, to }, i) => (
              <span key={label} className="flex items-center gap-3 sm:gap-5">
                <Link to={to} className="hover:text-white/100 hover:underline underline-offset-4 transition">
                  {label}
                </Link>
                {i < SEGMENT_LINKS.length - 1 && <span className="opacity-60">|</span>}
              </span>
            ))}
          </div>
          <div className="hidden md:flex items-center gap-4 opacity-90 shrink-0">
            {SECONDARY_LINKS.map(({ label, to }, i) => (
              <span key={label} className="flex items-center gap-4">
                <Link to={to} className="hover:text-white/100 hover:underline underline-offset-4 transition">
                  {label}
                </Link>
                {i < SECONDARY_LINKS.length - 1 && <span className="opacity-60">|</span>}
              </span>
            ))}
          </div>
        </div>
      </div>

      <header
        className="sticky top-0 z-40 bg-white/95 dark:bg-ink-900/95 backdrop-blur border-b border-accent-line dark:border-ink-700"
        onMouseLeave={scheduleClose}
      >
        <div className="container-wide flex items-center justify-between gap-3 h-16 sm:h-20">
          <Link to="/" className="shrink-0"><Logo /></Link>

          <nav className="hidden lg:flex items-center gap-1 xl:gap-2 text-sm font-medium text-accent-slate dark:text-ink-300">
            {PRODUCT_LINK_NAMES.map((label) => {
              const menu = MEGA_MENUS[label];
              const active = openMenu === label;
              return (
                <div
                  key={label}
                  className="relative"
                  onMouseEnter={() => {
                    cancelClose();
                    setOpenMenu(label);
                  }}
                >
                  <Link
                    to={menu.to}
                    className={`group relative inline-flex items-center gap-1 px-3 py-2 rounded-lg transition ${
                      active
                        ? 'text-brand-700 dark:text-brand-300'
                        : 'hover:text-brand-700 dark:hover:text-brand-300'
                    }`}
                  >
                    <span>{label}</span>
                    <ChevronDown
                      size={14}
                      className={`transition-transform ${active ? 'rotate-180' : ''}`}
                    />
                    <span
                      className={`absolute left-3 right-3 -bottom-0.5 h-0.5 rounded-full bg-brand-700 dark:bg-brand-300 transition-transform origin-left ${
                        active ? 'scale-x-100' : 'scale-x-0 group-hover:scale-x-100'
                      }`}
                    />
                  </Link>
                </div>
              );
            })}
          </nav>

          <div className="flex items-center gap-2 sm:gap-3">
            <ThemeToggle />
            <Link to="/signup" className="btn-secondary hidden md:inline-flex whitespace-nowrap">
              Open Digital A/C
            </Link>
            <Link to="/login" className="btn-primary whitespace-nowrap">
              Login <ArrowRight size={16} />
            </Link>
            <button
              type="button"
              aria-label={mobileOpen ? 'Close menu' : 'Open menu'}
              aria-expanded={mobileOpen}
              onClick={() => setMobileOpen((v) => !v)}
              className="lg:hidden inline-flex h-10 w-10 items-center justify-center rounded-lg border border-accent-line dark:border-ink-700 text-accent-ink dark:text-ink-100"
            >
              {mobileOpen ? <X size={18} /> : <Menu size={18} />}
            </button>
          </div>
        </div>

        {/* Mega menu panel — full width, animates on open */}
        {openMenu && (
          <div
            className="hidden lg:block absolute left-0 right-0 top-full"
            onMouseEnter={cancelClose}
            onMouseLeave={scheduleClose}
          >
            <div className="bg-white dark:bg-ink-900 border-y border-accent-line dark:border-ink-700 shadow-elevated animate-fadeSlideDown">
              <div className="container-wide grid lg:grid-cols-[1fr_2fr] gap-8 py-8">
                <div className="max-w-sm">
                  <div className="chip bg-brand-50 dark:bg-brand-900/40 text-brand-700 dark:text-brand-300 mb-3">
                    {openMenu}
                  </div>
                  <p className="text-sm text-accent-slate dark:text-ink-300 leading-relaxed">
                    {MEGA_MENUS[openMenu].blurb}
                  </p>
                  <Link
                    to={MEGA_MENUS[openMenu].to}
                    className="mt-4 inline-flex items-center gap-1 text-sm font-semibold text-brand-700 dark:text-brand-300 hover:gap-2 transition-all"
                  >
                    Explore all <ArrowRight size={14} />
                  </Link>
                </div>
                <ul className="grid sm:grid-cols-2 gap-2">
                  {MEGA_MENUS[openMenu].items.map((item) => (
                    <li key={item.title}>
                      <Link
                        to={item.to}
                        className="group block p-3 rounded-xl hover:bg-brand-50/60 dark:hover:bg-brand-900/20 transition"
                      >
                        <div className="flex items-center justify-between">
                          <span className="font-semibold text-accent-ink dark:text-ink-100 group-hover:text-brand-700 dark:group-hover:text-brand-300 transition">
                            {item.title}
                          </span>
                          <ArrowRight
                            size={14}
                            className="text-accent-mute dark:text-ink-400 -translate-x-1 opacity-0 group-hover:translate-x-0 group-hover:opacity-100 transition"
                          />
                        </div>
                        <p className="mt-0.5 text-xs text-accent-mute dark:text-ink-400">{item.desc}</p>
                      </Link>
                    </li>
                  ))}
                </ul>
              </div>
            </div>
          </div>
        )}

        {/* Mobile / tablet expandable menu */}
        {mobileOpen && (
          <div className="lg:hidden border-t border-accent-line dark:border-ink-700 bg-white dark:bg-ink-900">
            <nav className="container-wide py-4 grid grid-cols-1 sm:grid-cols-2 gap-x-4 gap-y-1 text-sm font-medium text-accent-slate dark:text-ink-300">
              {PRODUCT_LINK_NAMES.map((label) => (
                <Link
                  key={label}
                  to={MEGA_MENUS[label].to}
                  className="flex items-center justify-between py-2.5 px-2 rounded-lg hover:bg-brand-50/60 dark:hover:bg-brand-900/20 hover:text-brand-700 dark:hover:text-brand-300 transition"
                >
                  <span>{label}</span>
                  <ArrowRight size={14} />
                </Link>
              ))}
            </nav>
            <div className="container-wide pb-4 grid grid-cols-2 gap-3 md:hidden">
              {SECONDARY_LINKS.map(({ label, to }) => (
                <Link
                  key={label}
                  to={to}
                  className="btn-secondary justify-center text-xs"
                >
                  {label}
                </Link>
              ))}
            </div>
          </div>
        )}
      </header>
    </>
  );
}

// ── Footer ────────────────────────────────────────────────────────────────────
function Footer() {
  return (
    <footer className="bg-accent-ink text-white/70">
      <div className="container-wide py-10 sm:py-14 grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-8 sm:gap-10">
        <div className="sm:col-span-2 md:col-span-1">
          <Logo tone="white" />
          <p className="mt-4 text-sm leading-relaxed max-w-sm">
           Our mission is to deliver trustworthy, transparent, and technology‑driven banking experiences that empower customers to manage their finances with confidence.
          </p>
        </div>
        {FOOTER_COLUMNS.map((col) => (
          <FooterCol key={col.title} title={col.title} items={col.items} />
        ))}
      </div>
      <div className="border-t border-white/10">
        <div className="container-wide py-5 flex flex-col md:flex-row items-center justify-between gap-2 sm:gap-3 text-xs text-center md:text-left">
          <span>© {new Date().getFullYear()} BankSphere — for educational use.</span>
          <span>Made with care · No real money was harmed.</span>
        </div>
      </div>
    </footer>
  );
}

function FooterCol({ title, items }) {
  return (
    <div>
      <h4 className="font-semibold text-white">{title}</h4>
      <ul className="mt-4 space-y-2 text-sm">
        {items.map((item) => (
          <li key={item.label}>
            <Link
              to={item.to}
              className="hover:text-white hover:underline underline-offset-4 transition"
            >
              {item.label}
            </Link>
          </li>
        ))}
      </ul>
    </div>
  );
}
