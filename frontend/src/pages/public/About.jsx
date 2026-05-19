import { useEffect } from 'react';
import { Link, useLocation } from 'react-router-dom';
import {
  ArrowRight, ShieldCheck, TrendingUp, Globe2, Sparkles, Briefcase,
  Newspaper, Mail, Phone, MapPin, Scale, Cookie, FileText, AlertTriangle,
} from 'lucide-react';
import PublicLayout from '../../components/public/PublicLayout';

// About Us / Company / Legal — single long page with deep links to each section
// so footer entries like /about#careers and /about#privacy land in the right place.
export default function About() {
  const { hash } = useLocation();

  // Scroll the anchor target into view on initial mount + when the hash changes.
  useEffect(() => {
    if (!hash) return;
    const el = document.getElementById(hash.slice(1));
    if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }, [hash]);

  return (
    <PublicLayout>
      <Hero />
      <Mission />
      <Values />
      <Leadership />
      <Careers />
      <Press />
      <Contact />
      <Legal />
    </PublicLayout>
  );
}

// ── Hero ──────────────────────────────────────────────────────────────────────
function Hero() {
  return (
    <section className="relative overflow-hidden bg-gradient-to-br from-brand-700 to-brand-900 text-white">
      <div className="container-wide py-16 sm:py-24 relative z-10">
        <span className="chip bg-white/15 border border-white/20">
          <Sparkles size={14} /> Our story
        </span>
        <h1 className="mt-5 font-display text-3xl sm:text-4xl md:text-5xl font-extrabold leading-tight max-w-3xl">
          Building the bank we always wished we had
        </h1>
        <p className="mt-5 text-base sm:text-lg text-white/85 max-w-2xl">
          BankSphere is a modern digital banking platform designed to simplify financial management for customers and institutions alike.
We provide secure, scalable, and user‑friendly solutions for everyday banking needs — from account creation and transactions to loan management and compliance tracking.
        </p>
      </div>
      <div className="absolute -bottom-32 -right-32 h-[420px] w-[420px] rounded-full bg-accent-gold/20 blur-3xl" />
      <div className="absolute -top-24 -left-24 h-[320px] w-[320px] rounded-full bg-brand-500/30 blur-3xl" />
    </section>
  );
}

// ── Mission ───────────────────────────────────────────────────────────────────
function Mission() {
  return (
    <section className="container-wide py-14 sm:py-20 grid lg:grid-cols-2 gap-10 lg:gap-16 items-start">
      <div>
        <span className="chip bg-brand-50 dark:bg-brand-900/40 text-brand-700 dark:text-brand-300">
          Our mission
        </span>
        <h2 className="mt-3 font-display text-2xl sm:text-3xl md:text-4xl font-extrabold leading-tight">
          Banking that feels like it was designed for humans, not auditors
        </h2>
      </div>
      <div className="space-y-4 text-base sm:text-lg text-accent-slate dark:text-ink-300 leading-relaxed">
        <p>
          We started BankSphere because traditional banking software felt like it was built in
          a different decade. Every screen asked you a question the bank already knew the answer to.
          Every form felt like punishment for choosing this bank in the first place.
        </p>
        <p>
          We're rebuilding banking from first principles — paperless onboarding, real-time dashboards,
          mobile-first design, and security that's tight without getting in your way. And because we
          care about the people behind the counter as much as the people in front of it, our staff
          tools are every bit as thoughtful as our customer app.
        </p>
      </div>
    </section>
  );
}

// ── Values ────────────────────────────────────────────────────────────────────
function Values() {
  const items = [
    { icon: ShieldCheck, title: 'Trust by design',
      copy: 'Bank-grade encryption, secure sign-in, and an extra check on every sensitive action — your money is protected at every step.' },
    { icon: Sparkles, title: 'Clarity over jargon',
      copy: 'Every screen is honest about fees, rates and timelines. If we can show you what is happening, we will.' },
    { icon: TrendingUp, title: 'Always improving',
      copy: 'We listen, we learn, we ship. Your feedback turns into real product improvements in days, not quarters.' },
    { icon: Globe2, title: 'Built to last',
      copy: 'Always-on banking you can rely on — steady on the busiest days, simple on the calmest ones, and ready whenever you are.' },
  ];
  return (
    <section className="bg-accent-surface/50 dark:bg-ink-850/40 border-y border-accent-line dark:border-ink-700">
      <div className="container-wide py-14 sm:py-20">
        <div className="max-w-2xl">
          <span className="chip bg-brand-50 dark:bg-brand-900/40 text-brand-700 dark:text-brand-300">
            What we believe
          </span>
          <h2 className="mt-3 font-display text-2xl sm:text-3xl md:text-4xl font-extrabold leading-tight">
            Four principles that shape every release
          </h2>
        </div>
        <div className="mt-10 grid sm:grid-cols-2 lg:grid-cols-4 gap-5">
          {items.map(({ icon: Icon, title, copy }) => (
            <article key={title} className="card-hover p-6">
              <span className="h-11 w-11 rounded-xl bg-brand-50 dark:bg-brand-900/40 text-brand-700 dark:text-brand-300 grid place-items-center">
                <Icon size={20} />
              </span>
              <h3 className="mt-5 font-semibold">{title}</h3>
              <p className="mt-2 text-sm text-accent-slate dark:text-ink-300 leading-relaxed">{copy}</p>
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}

// ── Leadership ────────────────────────────────────────────────────────────────
function Leadership() {
  const people = [
    { initials: 'VR', name: 'Vidya Rani',     role: 'Chief Executive Officer',
      bio: 'Former retail-banking head with 18 years across HDFC, ICICI and Standard Chartered.' },
    { initials: 'SA', name: 'Surya Adhethya',      role: 'Chief Technology Officer',
      bio: 'Built core-banking platforms at Razorpay and Stripe. Loves Spring Boot, hates flaky tests.' },
    { initials: 'AR', name: 'Ankit Raj',      role: 'Chief Operating Officer',
      bio: 'Ran branch operations for one of India’s largest PSU banks. Believes ops is product.' },
    { initials: 'VK', name: 'Vamsi Krishna',  role: 'Chief Risk Officer',
      bio: 'RBI-certified risk officer; built the credit-decision engine that powers our loan products.' },
  ];
  return (
    <section className="container-wide py-14 sm:py-20">
      <div className="max-w-2xl">
        <span className="chip bg-brand-50 dark:bg-brand-900/40 text-brand-700 dark:text-brand-300">
          Leadership
        </span>
        <h2 className="mt-3 font-display text-2xl sm:text-3xl md:text-4xl font-extrabold">
          The people accountable for every decision
        </h2>
      </div>
      <div className="mt-10 grid sm:grid-cols-2 lg:grid-cols-4 gap-5">
        {people.map((p) => (
          <article key={p.name} className="card p-6">
            <span className="h-14 w-14 rounded-full bg-brand-700 text-white grid place-items-center font-display font-bold text-lg">
              {p.initials}
            </span>
            <h3 className="mt-5 font-semibold">{p.name}</h3>
            <p className="text-sm text-brand-700 dark:text-brand-300">{p.role}</p>
            <p className="mt-2 text-sm text-accent-slate dark:text-ink-300 leading-relaxed">{p.bio}</p>
          </article>
        ))}
      </div>
    </section>
  );
}

// ── Careers ───────────────────────────────────────────────────────────────────
function Careers() {
  const roles = [
    { team: 'Engineering', title: 'Senior Backend Engineer', loc: 'Bengaluru / Remote-India' },
    { team: 'Engineering', title: 'Frontend Engineer (React)', loc: 'Bengaluru / Hyderabad' },
    { team: 'Product',     title: 'Senior Product Manager — Lending', loc: 'Bengaluru' },
    { team: 'Risk',        title: 'Credit Risk Analyst', loc: 'Mumbai' },
    { team: 'Operations',  title: 'Branch Operations Lead', loc: 'Pune' },
    { team: 'Design',      title: 'Senior Product Designer', loc: 'Bengaluru / Remote-India' },
  ];
  return (
    <section id="careers" className="bg-accent-surface/50 dark:bg-ink-850/40 border-y border-accent-line dark:border-ink-700 scroll-mt-24">
      <div className="container-wide py-14 sm:py-20">
        <div className="flex items-end justify-between gap-4 flex-wrap">
          <div className="max-w-2xl">
            <span className="chip bg-brand-50 dark:bg-brand-900/40 text-brand-700 dark:text-brand-300">
              <Briefcase size={14} /> Careers
            </span>
            <h2 className="mt-3 font-display text-2xl sm:text-3xl md:text-4xl font-extrabold">
              Build the future of Indian banking with us
            </h2>
            <p className="mt-3 text-accent-slate dark:text-ink-300">
              We're hiring across engineering, product, design and operations.
              Roles are based out of Bengaluru, Mumbai, Hyderabad and Pune — many open to remote-India.
            </p>
          </div>
          <a
            href="mailto:careers@banksphere.example.com"
            className="btn-primary"
          >
            Email careers team <ArrowRight size={16} />
          </a>
        </div>
        <div className="mt-10 grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {roles.map((r) => (
            <article key={r.title} className="card-hover p-5">
              <div className="text-xs uppercase tracking-wide text-accent-mute dark:text-ink-400">
                {r.team}
              </div>
              <h3 className="mt-1 font-semibold">{r.title}</h3>
              <p className="mt-2 text-sm text-accent-slate dark:text-ink-300">{r.loc}</p>
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}

// ── Press ─────────────────────────────────────────────────────────────────────
function Press() {
  const items = [
    { date: 'Mar 2026', title: 'BankSphere launches AI-powered spend insights for retail customers' },
    { date: 'Jan 2026', title: 'BankSphere crosses 10 million accounts in its second year of operations' },
    { date: 'Nov 2025', title: 'Why BankSphere bet everything on microservices — engineering blog' },
    { date: 'Sep 2025', title: 'BankSphere named Best Digital Bank at the IBA Banking Awards 2025' },
  ];
  return (
    <section id="press" className="container-wide py-14 sm:py-20 scroll-mt-24">
      <div className="max-w-2xl">
        <span className="chip bg-brand-50 dark:bg-brand-900/40 text-brand-700 dark:text-brand-300">
          <Newspaper size={14} /> Press & news
        </span>
        <h2 className="mt-3 font-display text-2xl sm:text-3xl md:text-4xl font-extrabold">
          What people are saying about us
        </h2>
      </div>
      <ul className="mt-10 divide-y divide-accent-line dark:divide-ink-700 border-y border-accent-line dark:border-ink-700">
        {items.map((p) => (
          <li key={p.title} className="py-5 flex items-center justify-between gap-4">
            <div>
              <div className="text-xs uppercase tracking-wide text-accent-mute dark:text-ink-400">{p.date}</div>
              <p className="mt-0.5 font-medium">{p.title}</p>
            </div>
            <ArrowRight size={16} className="text-accent-mute dark:text-ink-400 shrink-0" />
          </li>
        ))}
      </ul>
      <p className="mt-6 text-sm text-accent-slate dark:text-ink-300">
        Media enquiries — please email <a className="link" href="mailto:press@banksphere.example.com">press@banksphere.example.com</a>.
      </p>
    </section>
  );
}

// ── Contact ───────────────────────────────────────────────────────────────────
function Contact() {
  const channels = [
    { icon: Phone, title: '24×7 Customer Care', value: '1800-123-BANK',                 sub: 'Toll-free, in 11 Indian languages' },
    { icon: Mail,  title: 'Email Support',       value: 'help@banksphere.example.com',  sub: 'Replies within 4 hours, 7 days a week' },
    { icon: MapPin,title: 'Corporate Office',    value: '23 Brigade Road, Bengaluru — 560001', sub: 'Visits by appointment only' },
  ];
  return (
    <section id="contact" className="bg-accent-surface/50 dark:bg-ink-850/40 border-y border-accent-line dark:border-ink-700 scroll-mt-24">
      <div className="container-wide py-14 sm:py-20">
        <div className="max-w-2xl">
          <span className="chip bg-brand-50 dark:bg-brand-900/40 text-brand-700 dark:text-brand-300">
            <Mail size={14} /> Contact us
          </span>
          <h2 className="mt-3 font-display text-2xl sm:text-3xl md:text-4xl font-extrabold">
            We're here, every day
          </h2>
          <p className="mt-3 text-accent-slate dark:text-ink-300">
            Existing customers should always log in to the app and start a support ticket — it routes
            straight to the right team. For everything else, use the channels below.
          </p>
        </div>
        <div className="mt-10 grid sm:grid-cols-2 lg:grid-cols-3 gap-5">
          {channels.map(({ icon: Icon, title, value, sub }) => (
            <article key={title} className="card-hover p-6">
              <span className="h-11 w-11 rounded-xl bg-brand-50 dark:bg-brand-900/40 text-brand-700 dark:text-brand-300 grid place-items-center">
                <Icon size={20} />
              </span>
              <h3 className="mt-5 font-semibold">{title}</h3>
              <p className="mt-1 font-mono text-sm text-accent-ink dark:text-ink-100">{value}</p>
              <p className="mt-2 text-xs text-accent-slate dark:text-ink-300">{sub}</p>
            </article>
          ))}
        </div>
        <div className="mt-10">
          <Link to="/branches" className="btn-secondary">
            Find a branch near you <ArrowRight size={16} />
          </Link>
        </div>
      </div>
    </section>
  );
}

// ── Legal (anchor targets: privacy / terms / cookies / disclosure / grievance) ─
function Legal() {
  const blocks = [
    {
      id: 'privacy',
      icon: ShieldCheck,
      title: 'Privacy Policy',
      lead: 'We collect only what we need to run your account and keep it safe.',
      points: [
        'We process personal data lawfully under the Digital Personal Data Protection Act, 2023.',
        'You can request a copy of all data we hold on you at any time, in machine-readable form.',
        'We never sell customer data — full stop.',
        'Encryption in transit (TLS 1.3) and at rest (AES-256) on all data stores.',
      ],
    },
    {
      id: 'terms',
      icon: FileText,
      title: 'Terms of Use',
      lead: 'The rules of the road for using BankSphere — written in plain English.',
      points: [
        'You must be 18+ and an Indian resident to open a personal account.',
        'You agree to keep your credentials confidential and to enable 2FA.',
        'We may suspend accounts engaged in suspicious activity pending review.',
        'The bank is regulated by the Reserve Bank of India and the IBA framework.',
      ],
    },
    {
      id: 'cookies',
      icon: Cookie,
      title: 'Cookie Notice',
      lead: 'Three kinds of cookies — essential, analytics and preferences. You control all but the first.',
      points: [
        'Essential cookies keep you signed in and protect against CSRF attacks.',
        'Analytics cookies help us understand which screens cause friction.',
        'Preference cookies remember your dark-mode and language choice.',
        'Tweak preferences any time in Settings → Privacy.',
      ],
    },
    {
      id: 'disclosure',
      icon: Scale,
      title: 'Disclosure',
      lead: 'Required regulatory disclosures and customer-protection statements.',
      points: [
        'Deposits up to ₹5 lakh are insured by DICGC, a wholly-owned subsidiary of the RBI.',
        'Loan and investment products carry risks — read the product documents carefully.',
        'Past performance does not guarantee future returns.',
        'BankSphere is a registered AMFI distributor of mutual funds.',
      ],
    },
    {
      id: 'grievance',
      icon: AlertTriangle,
      title: 'Grievance Redressal',
      lead: 'How to escalate a complaint, and the timelines we commit to.',
      points: [
        'Level 1 — Log a ticket in the app. We respond within 24 hours.',
        'Level 2 — Email grievance@banksphere.example.com if unresolved within 7 days.',
        'Level 3 — Escalate to the Principal Nodal Officer at PNO@banksphere.example.com.',
        'Final — RBI Banking Ombudsman if unresolved within 30 days. Details at cms.rbi.org.in.',
      ],
    },
  ];

  return (
    <section className="container-wide py-14 sm:py-20">
      <div className="max-w-2xl">
        <span className="chip bg-brand-50 dark:bg-brand-900/40 text-brand-700 dark:text-brand-300">
          <Scale size={14} /> Legal & policies
        </span>
        <h2 className="mt-3 font-display text-2xl sm:text-3xl md:text-4xl font-extrabold">
          The fine print — but with the print made larger
        </h2>
      </div>
      <div className="mt-10 grid lg:grid-cols-2 gap-5">
        {blocks.map(({ id, icon: Icon, title, lead, points }) => (
          <article
            id={id}
            key={id}
            className="card p-6 sm:p-7 scroll-mt-24"
          >
            <div className="flex items-center gap-3">
              <span className="h-11 w-11 rounded-xl bg-brand-50 dark:bg-brand-900/40 text-brand-700 dark:text-brand-300 grid place-items-center">
                <Icon size={20} />
              </span>
              <h3 className="font-display text-lg font-bold">{title}</h3>
            </div>
            <p className="mt-3 text-sm text-accent-slate dark:text-ink-300">{lead}</p>
            <ul className="mt-3 space-y-1.5 text-sm text-accent-slate dark:text-ink-300 list-disc pl-5">
              {points.map((p) => <li key={p}>{p}</li>)}
            </ul>
          </article>
        ))}
      </div>
      <p className="mt-8 text-xs text-accent-mute dark:text-ink-400">
        BankSphere is an educational, learning-grade platform. The legal text on this page is illustrative
        and does not constitute legal advice from a regulated bank.
      </p>
    </section>
  );
}

