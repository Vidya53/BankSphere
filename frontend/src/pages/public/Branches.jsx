import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  MapPin, Search, Phone, Clock, ArrowRight, Building2, Compass,
  ParkingCircle, ShieldCheck,
} from 'lucide-react';
import PublicLayout from '../../components/public/PublicLayout';

const BRANCHES = [
  { code: 'BLR001', name: 'Brigade Road',  city: 'Bengaluru',  state: 'Karnataka',
    address: '23 Brigade Road, Bengaluru — 560001', phone: '080-4012-1212',
    hours: 'Mon-Fri 09:30 – 16:30 · Sat 10:00 – 14:00', tags: ['Flagship', 'Locker', 'Forex'] },
  { code: 'BLR002', name: 'Indiranagar',   city: 'Bengaluru',  state: 'Karnataka',
    address: '100ft Road, Indiranagar, Bengaluru — 560038', phone: '080-4012-1213',
    hours: 'Mon-Fri 09:30 – 16:30 · Sat 10:00 – 14:00', tags: ['Locker'] },
  { code: 'BOM001', name: 'Bandra West',   city: 'Mumbai',     state: 'Maharashtra',
    address: 'Linking Road, Bandra West, Mumbai — 400050', phone: '022-4012-1212',
    hours: 'Mon-Fri 09:30 – 16:30 · Sat 10:00 – 14:00', tags: ['Flagship', 'Forex'] },
  { code: 'BOM002', name: 'Powai',         city: 'Mumbai',     state: 'Maharashtra',
    address: 'Hiranandani Gardens, Powai, Mumbai — 400076', phone: '022-4012-1213',
    hours: 'Mon-Fri 09:30 – 16:30 · Sat 10:00 – 14:00', tags: ['Locker'] },
  { code: 'DEL001', name: 'Connaught Place', city: 'New Delhi', state: 'Delhi',
    address: 'M-Block, Connaught Place, New Delhi — 110001', phone: '011-4012-1212',
    hours: 'Mon-Fri 09:30 – 16:30 · Sat 10:00 – 14:00', tags: ['Flagship', 'Locker', 'Forex'] },
  { code: 'HYD001', name: 'Banjara Hills', city: 'Hyderabad',  state: 'Telangana',
    address: 'Road No. 12, Banjara Hills, Hyderabad — 500034', phone: '040-4012-1212',
    hours: 'Mon-Fri 09:30 – 16:30 · Sat 10:00 – 14:00', tags: ['Locker'] },
  { code: 'PUN001', name: 'Koregaon Park', city: 'Pune',       state: 'Maharashtra',
    address: 'North Main Road, Koregaon Park, Pune — 411001', phone: '020-4012-1212',
    hours: 'Mon-Fri 09:30 – 16:30 · Sat 10:00 – 14:00', tags: ['Locker'] },
  { code: 'CHN001', name: 'Anna Nagar',     city: 'Chennai',   state: 'Tamil Nadu',
    address: '2nd Avenue, Anna Nagar, Chennai — 600040', phone: '044-4012-1212',
    hours: 'Mon-Fri 09:30 – 16:30 · Sat 10:00 – 14:00', tags: ['Forex'] },
  { code: 'CCU001', name: 'Park Street',    city: 'Kolkata',   state: 'West Bengal',
    address: 'Park Street, Kolkata — 700016', phone: '033-4012-1212',
    hours: 'Mon-Fri 09:30 – 16:30 · Sat 10:00 – 14:00', tags: ['Flagship'] },
  { code: 'GGN001', name: 'Cyber Hub',      city: 'Gurugram',  state: 'Haryana',
    address: 'DLF Cyber Hub, Gurugram — 122002', phone: '0124-4012-1212',
    hours: 'Mon-Fri 09:30 – 16:30 · Sat 10:00 – 14:00', tags: ['Locker'] },
];

const CITIES = ['All', ...Array.from(new Set(BRANCHES.map((b) => b.city))).sort()];

export default function Branches() {
  const [query, setQuery] = useState('');
  const [city, setCity] = useState('All');

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    return BRANCHES.filter((b) => {
      if (city !== 'All' && b.city !== city) return false;
      if (!q) return true;
      return [b.name, b.city, b.state, b.address, b.code].some((f) =>
        f.toLowerCase().includes(q),
      );
    });
  }, [query, city]);

  return (
    <PublicLayout>
      <Hero />

      {/* Search controls */}
      <section className="container-wide -mt-8 sm:-mt-12 relative z-10">
        <div className="card p-4 sm:p-5 grid sm:grid-cols-[1fr_auto] gap-3">
          <label className="relative block">
            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-accent-mute" />
            <input
              type="search"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Search by city, branch name, pincode, or branch code"
              className="input pl-9"
            />
          </label>
          <select
            value={city}
            onChange={(e) => setCity(e.target.value)}
            className="input sm:w-48"
            aria-label="Filter by city"
          >
            {CITIES.map((c) => <option key={c} value={c}>{c === 'All' ? 'All cities' : c}</option>)}
          </select>
        </div>
        <p className="mt-3 text-xs text-accent-mute dark:text-ink-400">
          Showing {filtered.length} of {BRANCHES.length} branches.
        </p>
      </section>

      {/* Results */}
      <section className="container-wide py-10 sm:py-14">
        {filtered.length === 0 ? (
          <div className="card p-10 text-center">
            <Compass size={28} className="mx-auto text-accent-mute dark:text-ink-400" />
            <h3 className="mt-3 font-display text-xl font-bold">No branches matched</h3>
            <p className="mt-2 text-sm text-accent-slate dark:text-ink-300">
              Try a different city or check our 24×7 customer-care number.
            </p>
          </div>
        ) : (
          <ul className="grid sm:grid-cols-2 lg:grid-cols-3 gap-5">
            {filtered.map((b) => (
              <li key={b.code}>
                <article className="card-hover h-full p-6 flex flex-col">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <div className="text-xs uppercase tracking-wide text-accent-mute dark:text-ink-400">
                        {b.city}, {b.state}
                      </div>
                      <h3 className="mt-0.5 font-display text-lg font-bold">{b.name}</h3>
                    </div>
                    <span className="font-mono text-[10px] tracking-wider px-2 py-1 rounded-md bg-accent-surface dark:bg-ink-750 text-accent-mute dark:text-ink-300 shrink-0">
                      {b.code}
                    </span>
                  </div>
                  <p className="mt-3 text-sm text-accent-slate dark:text-ink-300 flex items-start gap-2">
                    <MapPin size={14} className="mt-0.5 text-brand-700 dark:text-brand-300 shrink-0" />
                    {b.address}
                  </p>
                  <p className="mt-2 text-sm text-accent-slate dark:text-ink-300 flex items-center gap-2">
                    <Phone size={14} className="text-brand-700 dark:text-brand-300 shrink-0" />
                    <a href={`tel:${b.phone.replace(/\D/g, '')}`} className="link">{b.phone}</a>
                  </p>
                  <p className="mt-2 text-sm text-accent-slate dark:text-ink-300 flex items-center gap-2">
                    <Clock size={14} className="text-brand-700 dark:text-brand-300 shrink-0" />
                    {b.hours}
                  </p>
                  <div className="mt-4 flex flex-wrap gap-1.5">
                    {b.tags.map((t) => (
                      <span
                        key={t}
                        className="chip bg-brand-50 dark:bg-brand-900/40 text-brand-700 dark:text-brand-300 text-[11px]"
                      >
                        {t}
                      </span>
                    ))}
                  </div>
                  <div className="mt-5 pt-5 border-t border-accent-line dark:border-ink-700 flex items-center justify-between">
                    <a
                      href={`https://maps.google.com/?q=${encodeURIComponent(b.address)}`}
                      target="_blank"
                      rel="noreferrer noopener"
                      className="inline-flex items-center gap-1 text-sm font-semibold text-brand-700 dark:text-brand-300 hover:gap-2 transition-all"
                    >
                      Get directions <ArrowRight size={14} />
                    </a>
                  </div>
                </article>
              </li>
            ))}
          </ul>
        )}
      </section>

      <Services />
      <CTABanner />
    </PublicLayout>
  );
}

function Hero() {
  return (
    <section className="bg-gradient-to-br from-brand-700 to-brand-900 text-white">
      <div className="container-wide pt-14 pb-20 sm:pt-20 sm:pb-28">
        <span className="chip bg-white/15 border border-white/20">
          <Building2 size={14} /> 1,200+ branches across India
        </span>
        <h1 className="mt-5 font-display text-3xl sm:text-4xl md:text-5xl font-extrabold leading-tight max-w-3xl">
          Find a BankSphere branch near you
        </h1>
        <p className="mt-4 text-base sm:text-lg text-white/85 max-w-2xl">
          Looking for in-person help, locker access, or forex services? Search by city, branch name, or
          pincode. All our branches are accessible and open six days a week.
        </p>
      </div>
    </section>
  );
}

function Services() {
  const items = [
    { icon: ShieldCheck,     title: 'Lockers',   copy: 'Safe deposit lockers in three sizes — quarterly rentals, biometric-locked vault.' },
    { icon: Compass,         title: 'Forex',     copy: 'Travel cards, currency exchange and remittances at flagship branches.' },
    { icon: ParkingCircle,   title: 'Parking',   copy: 'Validated customer parking at all flagship branches.' },
    { icon: Phone,           title: 'Doorstep',  copy: 'Cash pickup, KYC, and senior-citizen banking — available across all metros.' },
  ];
  return (
    <section className="bg-accent-surface/50 dark:bg-ink-850/40 border-y border-accent-line dark:border-ink-700">
      <div className="container-wide py-14 sm:py-20">
        <div className="max-w-2xl">
          <span className="chip bg-brand-50 dark:bg-brand-900/40 text-brand-700 dark:text-brand-300">
            In-branch services
          </span>
          <h2 className="mt-3 font-display text-2xl sm:text-3xl md:text-4xl font-extrabold">
            More than just a counter
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

function CTABanner() {
  return (
    <section className="container-wide py-14 sm:py-20">
      <div className="relative overflow-hidden rounded-3xl bg-gradient-to-r from-brand-800 to-brand-700 text-white p-8 sm:p-10 md:p-16">
        <div className="max-w-2xl">
          <h2 className="font-display text-2xl sm:text-3xl md:text-4xl font-extrabold">
            Skip the branch — open an account online in minutes
          </h2>
          <p className="mt-3 opacity-90">
            Most banking journeys are now fully digital. Drop into a branch only when you need to.
          </p>
          <div className="mt-7 flex flex-wrap gap-3">
            <Link to="/signup" className="btn bg-white text-brand-700 hover:bg-brand-50">
              Open an account <ArrowRight size={16} />
            </Link>
            <Link to="/about#contact" className="btn border border-white/40 text-white hover:bg-white/10">
              Talk to support
            </Link>
          </div>
        </div>
        <div className="absolute -right-20 -bottom-20 h-72 w-72 rounded-full bg-accent-gold/20 blur-3xl" />
      </div>
    </section>
  );
}
