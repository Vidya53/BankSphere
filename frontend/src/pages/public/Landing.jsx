import { Link } from 'react-router-dom';
import {
  ArrowRight, ShieldCheck, CreditCard, Landmark, PiggyBank,
  TrendingUp, Smartphone, Sparkles, Globe2, Bell, Wallet, BarChart3,
} from 'lucide-react';
import PublicLayout from '../../components/public/PublicLayout';

export default function Landing() {
  return (
    <PublicLayout>
      <Hero />
      <ProductPills />
      <FeatureShowcase />
      <CategoriesGrid />
      <CTABanner />
    </PublicLayout>
  );
}

// ── Hero ──────────────────────────────────────────────────────────────────────
function Hero() {
  return (
    <section className="relative overflow-hidden bg-gradient-to-br from-brand-50 via-white to-white dark:from-brand-950/30 dark:via-ink-900 dark:to-ink-900">
      <div className="container-wide pt-10 pb-14 sm:pt-16 sm:pb-24 grid lg:grid-cols-2 gap-10 lg:gap-12 items-center">
        <div>
          <span className="chip bg-white border border-brand-200 text-brand-700">
            <Sparkles size={14} /> open to your aspirations
          </span>
          <h1 className="mt-5 font-display text-4xl sm:text-5xl md:text-6xl font-extrabold leading-[1.1] sm:leading-[1.05] tracking-tight">
            <span className="text-brand-700">open</span> to your learning <br />
            <span className="text-brand-700">open</span> to your future
          </h1>
          <p className="mt-5 text-base sm:text-lg text-accent-slate dark:text-ink-300 max-w-xl">
            India's next-generation digital banking platform — savings, payments, loans, investments and
            insights, all in one place. Built with security, designed for clarity.
          </p>
          <div className="mt-8 flex flex-wrap gap-3">
            <Link to="/signup" className="btn-primary">
              Open an account <ArrowRight size={16} />
            </Link>
            <Link to="/login" className="btn-secondary">I already have an account</Link>
          </div>
          <ul className="mt-8 grid grid-cols-2 sm:grid-cols-3 gap-4 max-w-xl text-sm text-accent-slate dark:text-ink-300">
            <li className="flex items-center gap-2"><ShieldCheck size={16} className="text-brand-700 shrink-0" /> ISO 27001 certified</li>
            <li className="flex items-center gap-2"><Globe2 size={16} className="text-brand-700 shrink-0" /> 24×7 banking</li>
            <li className="flex items-center gap-2"><Smartphone size={16} className="text-brand-700 shrink-0" /> Mobile + web</li>
          </ul>
        </div>
        <HeroCardStack />
      </div>
      <DecorationBlob />
    </section>
  );
}

function HeroCardStack() {
  return (
    <div className="relative h-[460px] hidden lg:block">
      {/* Account card */}
      <div className="absolute right-10 top-0 w-[360px] rounded-2xl bg-gradient-to-br from-brand-700 to-brand-900 text-white p-6 shadow-elevated rotate-[-4deg]">
        <div className="flex items-center justify-between">
          <span className="text-xs uppercase tracking-widest opacity-70">Savings · Premium</span>
          <Wallet size={20} />
        </div>
        <div className="mt-12">
          <div className="text-sm opacity-70">Available balance</div>
          <div className="text-3xl font-bold mt-1">₹ 2,84,650.00</div>
        </div>
        <div className="mt-6 flex items-center justify-between text-sm">
          <span>•••• 7421</span>
          <span className="font-display">BankSphere</span>
        </div>
      </div>
      {/* Stats card */}
      <div className="absolute right-0 top-44 w-[320px] card p-5 rotate-[3deg]">
        <div className="flex items-center justify-between">
          <span className="text-xs font-semibold text-accent-mute dark:text-ink-400 uppercase">This month</span>
          <BarChart3 size={18} className="text-brand-700" />
        </div>
        <div className="mt-3 flex items-end justify-between">
          <div>
            <div className="text-2xl font-bold">₹ 48,200</div>
            <div className="text-xs text-accent-mute dark:text-ink-400">Total spends</div>
          </div>
          <div className="chip bg-green-50 text-accent-success"><TrendingUp size={12} /> 12.4%</div>
        </div>
        <div className="mt-4 grid grid-cols-7 gap-1.5 items-end h-16">
          {[40, 60, 30, 80, 55, 70, 90].map((h, i) => (
            <div key={i} className="bg-brand-200 rounded" style={{ height: `${h}%` }} />
          ))}
        </div>
      </div>
      {/* Notification card */}
      <div className="absolute left-0 bottom-6 w-[300px] card p-4 rotate-[-2deg]">
        <div className="flex items-start gap-3">
          <span className="h-9 w-9 rounded-full bg-brand-50 grid place-items-center text-brand-700">
            <Bell size={16} />
          </span>
          <div className="flex-1">
            <div className="text-sm font-semibold">Salary credited</div>
            <div className="text-xs text-accent-mute dark:text-ink-400 mt-0.5">₹ 78,400 from Cognizant • just now</div>
          </div>
        </div>
      </div>
    </div>
  );
}

function DecorationBlob() {
  return (
    <div aria-hidden="true" className="pointer-events-none absolute -top-32 -right-32 h-[420px] w-[420px] rounded-full bg-brand-100 blur-3xl opacity-50" />
  );
}

// ── Product pills row ─────────────────────────────────────────────────────────
function ProductPills() {
  const items = [
    { icon: PiggyBank, label: 'Savings Account', to: '/products/accounts' },
    { icon: CreditCard, label: 'Credit Card',    to: '/products/cards' },
    { icon: Landmark,   label: 'Personal Loan',  to: '/products/loans' },
    { icon: TrendingUp, label: 'Investments',    to: '/products/investments' },
  ];
  return (
    <section className="border-y border-accent-line dark:border-ink-700 bg-white dark:bg-ink-900">
      <div className="container-wide py-6 grid grid-cols-2 md:grid-cols-4 gap-4">
        {items.map(({ icon: Icon, label, to }) => (
          <Link
            key={label}
            to={to}
            className="flex items-center gap-3 p-3 rounded-xl hover:bg-accent-surface/60 dark:hover:bg-ink-800 transition group"
          >
            <span className="h-11 w-11 rounded-full bg-brand-50 dark:bg-brand-900/40 grid place-items-center text-brand-700 dark:text-brand-300 group-hover:scale-105 transition-transform">
              <Icon size={20} />
            </span>
            <div>
              <div className="text-sm font-semibold">{label}</div>
              <div className="text-xs text-accent-mute dark:text-ink-400 inline-flex items-center gap-1">
                Apply now <ArrowRight size={12} className="-translate-x-0.5 opacity-0 group-hover:translate-x-0 group-hover:opacity-100 transition" />
              </div>
            </div>
          </Link>
        ))}
      </div>
    </section>
  );
}

// ── Feature showcase ──────────────────────────────────────────────────────────
function FeatureShowcase() {
  const features = [
    { icon: Wallet, title: 'Smart Accounts',
      copy: 'Premium, regular, women’s and senior savings accounts — designed for every life stage.' },
    { icon: TrendingUp, title: 'Track and grow',
      copy: 'Personalised insights, spend analytics and saving goals — built directly into your dashboard.' },
    { icon: ShieldCheck, title: 'Bank-grade security',
      copy: 'Encrypted at every step, secure sign-in on every device, and instant alerts on every transaction.' },
    { icon: Globe2, title: 'Always available',
      copy: 'Bank anytime, from anywhere — your account is there for you 24×7, 365 days a year.' },
  ];
  return (
    <section className="container-wide py-14 sm:py-20">
      <div className="grid lg:grid-cols-2 gap-10 lg:gap-16 items-start">
        <div>
          <span className="chip bg-brand-50 text-brand-700">Why BankSphere</span>
          <h2 className="mt-4 font-display text-3xl sm:text-4xl font-extrabold leading-tight">
            The best of digital banking is now <span className="text-brand-700">open</span>
          </h2>
          <p className="mt-4 text-accent-slate dark:text-ink-300 max-w-lg">
            From the moment you open an account to the day you pay off your last EMI — every screen
            is built with clarity, speed and trust in mind.
          </p>
        </div>
        <div className="grid sm:grid-cols-2 gap-5">
          {features.map(({ icon: Icon, title, copy }) => (
            <div key={title} className="card-hover p-6">
              <span className="h-11 w-11 rounded-full bg-brand-50 text-brand-700 grid place-items-center">
                <Icon size={20} />
              </span>
              <h3 className="mt-4 font-semibold">{title}</h3>
              <p className="mt-1.5 text-sm text-accent-slate dark:text-ink-300 leading-relaxed">{copy}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

// ── Categories ───────────────────────────────────────────────────────────────
function CategoriesGrid() {
  const cards = [
    { tag: 'For the elite',     title: 'Premium Account',  icon: Sparkles,    to: '/products/accounts' },
    { tag: 'For the always-on', title: 'Regular Account',  icon: Wallet,      to: '/products/accounts' },
    { tag: 'For the empowered', title: "Women's Account",  icon: PiggyBank,   to: '/products/accounts' },
    { tag: 'For the elders',    title: 'Senior Citizens',  icon: ShieldCheck, to: '/products/accounts' },
  ];
  return (
    <section className="container-wide py-14 sm:py-20">
      <div className="flex items-end justify-between gap-4 flex-wrap">
        <div>
          <span className="chip bg-brand-50 text-brand-700">Open an account</span>
          <h2 className="mt-3 font-display text-2xl sm:text-3xl md:text-4xl font-extrabold">
            Find an account that suits your saving style
          </h2>
        </div>
        <Link to="/products/accounts" className="btn-secondary">Explore More <ArrowRight size={16} /></Link>
      </div>
      <div className="mt-8 sm:mt-10 grid sm:grid-cols-2 lg:grid-cols-4 gap-5">
        {cards.map(({ tag, title, icon: Icon, to }) => (
          <Link to={to} key={title} className="card-hover p-6 group block">
            <span className="h-11 w-11 rounded-full bg-brand-50 grid place-items-center text-brand-700">
              <Icon size={20} />
            </span>
            <div className="mt-6">
              <div className="text-xs uppercase tracking-wide text-accent-mute dark:text-ink-400">{tag}</div>
              <h3 className="mt-1 text-lg font-semibold">{title}</h3>
            </div>
            <span className="mt-6 inline-flex items-center gap-1 text-sm font-semibold text-brand-700 group-hover:gap-2 transition-all">
              Apply now <ArrowRight size={14} />
            </span>
          </Link>
        ))}
      </div>
    </section>
  );
}

// ── CTA banner ───────────────────────────────────────────────────────────────
function CTABanner() {
  return (
    <section className="container-wide pb-16 sm:pb-24">
      <div className="relative overflow-hidden rounded-3xl bg-gradient-to-r from-brand-800 to-brand-700 text-white p-8 sm:p-10 md:p-16">
        <div className="max-w-2xl">
          <h2 className="font-display text-2xl sm:text-3xl md:text-4xl font-extrabold">
            The smartest way to bank, learn and grow
          </h2>
          <p className="mt-3 opacity-90">
            Open an account in minutes — fully digital, paperless, and ready when you are.
          </p>
          <div className="mt-7 flex flex-wrap gap-3">
            <Link to="/signup" className="btn bg-white text-brand-700 hover:bg-brand-50">
              Get started — it's free <ArrowRight size={16} />
            </Link>
            <Link to="/login" className="btn border border-white/40 text-white hover:bg-white/10">
              Sign in
            </Link>
          </div>
        </div>
        <div className="absolute -right-20 -bottom-20 h-72 w-72 rounded-full bg-accent-gold/20 blur-3xl" />
      </div>
    </section>
  );
}
