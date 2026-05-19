import { Link, useParams, Navigate } from 'react-router-dom';
import { ArrowRight, Check, Sparkles } from 'lucide-react';
import PublicLayout from '../../components/public/PublicLayout';
import { PRODUCTS, SEGMENTS } from '../../data/marketingContent';

// Generic, content-driven marketing page. Reads either PRODUCTS[slug] or
// SEGMENTS[slug] based on the route, then renders a consistent hero + sections
// layout. Adding a new product/segment is purely a data change.
export default function InfoPage({ kind }) {
  const { slug } = useParams();
  const content =
    kind === 'segments' ? SEGMENTS[slug] :
    kind === 'products' ? PRODUCTS[slug] : null;

  if (!content) return <Navigate to="/" replace />;

  const HeroIcon = content.icon || Sparkles;

  return (
    <PublicLayout>
      {/* Hero */}
      <section className="relative overflow-hidden bg-gradient-to-br from-brand-50 via-white to-white dark:from-brand-950/30 dark:via-ink-900 dark:to-ink-900">
        <div className="container-wide py-12 sm:py-20 grid lg:grid-cols-[1.2fr_1fr] gap-10 items-center">
          <div>
            <span className="chip bg-white dark:bg-ink-800 border border-brand-200 dark:border-brand-900 text-brand-700 dark:text-brand-300">
              <HeroIcon size={14} /> {content.eyebrow}
            </span>
            <h1 className="mt-5 font-display text-3xl sm:text-4xl md:text-5xl font-extrabold leading-tight tracking-tight">
              {content.heading}
            </h1>
            <p className="mt-5 text-base sm:text-lg text-accent-slate dark:text-ink-300 max-w-2xl">
              {content.subhead}
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Link to={content.cta.to} className="btn-primary">
                {content.cta.label} <ArrowRight size={16} />
              </Link>
              <Link to="/login" className="btn-secondary">I already have an account</Link>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 lg:grid-cols-1 gap-4">
            {content.highlights.map(({ icon: Icon, title }) => (
              <div
                key={title}
                className="card p-5 flex items-center gap-4 hover:shadow-cardHover dark:hover:shadow-cardHoverDark transition"
              >
                <span className="h-12 w-12 rounded-2xl bg-brand-50 dark:bg-brand-900/40 text-brand-700 dark:text-brand-300 grid place-items-center shrink-0">
                  <Icon size={22} />
                </span>
                <p className="font-semibold text-sm sm:text-base">{title}</p>
              </div>
            ))}
          </div>
        </div>
        {/* Decorative blob */}
        <div aria-hidden="true" className="pointer-events-none absolute -top-32 -right-32 h-[420px] w-[420px] rounded-full bg-brand-100 dark:bg-brand-900/30 blur-3xl opacity-50" />
      </section>

      {/* Sections grid */}
      <section className="container-wide py-14 sm:py-20">
        <span className="chip bg-brand-50 dark:bg-brand-900/40 text-brand-700 dark:text-brand-300">
          Explore {content.title}
        </span>
        <h2 className="mt-3 font-display text-2xl sm:text-3xl md:text-4xl font-extrabold">
          Pick the option that fits you best
        </h2>

        <div className="mt-10 grid sm:grid-cols-2 gap-6">
          {content.sections.map(({ icon: Icon, title, body, bullets }) => (
            <article key={title} className="card-hover p-6 sm:p-7">
              <span className="h-11 w-11 rounded-xl bg-brand-50 dark:bg-brand-900/40 text-brand-700 dark:text-brand-300 grid place-items-center">
                <Icon size={20} />
              </span>
              <h3 className="mt-5 font-display text-xl font-bold">{title}</h3>
              <p className="mt-2 text-sm text-accent-slate dark:text-ink-300 leading-relaxed">
                {body}
              </p>
              <ul className="mt-4 space-y-2 text-sm">
                {bullets.map((b) => (
                  <li key={b} className="flex items-start gap-2">
                    <Check size={16} className="text-accent-success dark:text-green-300 mt-0.5 shrink-0" />
                    <span className="text-accent-slate dark:text-ink-300">{b}</span>
                  </li>
                ))}
              </ul>
            </article>
          ))}
        </div>
      </section>

      {/* CTA banner */}
      <section className="container-wide pb-16 sm:pb-24">
        <div className="relative overflow-hidden rounded-3xl bg-gradient-to-r from-brand-800 to-brand-700 text-white p-8 sm:p-10 md:p-16">
          <div className="max-w-2xl">
            <h2 className="font-display text-2xl sm:text-3xl md:text-4xl font-extrabold">
              Ready to get started with {content.title}?
            </h2>
            <p className="mt-3 opacity-90">
              Open a fully digital BankSphere account in minutes — no branch visits, no paperwork.
            </p>
            <div className="mt-7 flex flex-wrap gap-3">
              <Link to={content.cta.to} className="btn bg-white text-brand-700 hover:bg-brand-50">
                {content.cta.label} <ArrowRight size={16} />
              </Link>
              <Link to="/branches" className="btn border border-white/40 text-white hover:bg-white/10">
                Find a branch
              </Link>
            </div>
          </div>
          <div className="absolute -right-20 -bottom-20 h-72 w-72 rounded-full bg-accent-gold/20 blur-3xl" />
        </div>
      </section>
    </PublicLayout>
  );
}
