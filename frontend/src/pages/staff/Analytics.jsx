import { useState } from 'react';
import { TrendingUp, Wallet, ArrowDownRight, Users, ShieldCheck, Landmark } from 'lucide-react';
import { PageHeader } from '../../components/layout/PageHeader';
import { Card, CardHeader } from '../../components/common/Card';
import { Skeleton } from '../../components/common/Skeleton';
import { Badge } from '../../components/common/Badge';
import { KpiCard } from '../../components/dashboard/KpiCard';
import { SpendChart } from '../../components/dashboard/SpendChart';
import { CategoryDonut } from '../../components/dashboard/CategoryDonut';
import { BarTrend } from '../../components/dashboard/BarTrend';
import { LineTrend } from '../../components/dashboard/LineTrend';
import { useAsync } from '../../hooks/useAsync';
import { analyticsApi } from '../../api/analytics';
import { formatINR, formatCompactINR } from '../../utils/format';

export default function Analytics() {
  const [tab, setTab] = useState('spend');

  const tabs = [
    { key: 'spend',      label: 'Spend',      icon: Wallet },
    { key: 'revenue',    label: 'Revenue',    icon: TrendingUp },
    { key: 'loans',      label: 'Loans',      icon: Landmark },
    { key: 'compliance', label: 'Compliance', icon: ShieldCheck },
    { key: 'customers',  label: 'Customers',  icon: Users },
  ];

  return (
    <div className="space-y-8">
      <PageHeader
        breadcrumb="Staff · Analytics"
        title="Analytics & Insights"
        subtitle="Cross-domain analytics across spend, revenue, loans, compliance and customers."
      />

      <div className="card p-1.5 inline-flex flex-wrap gap-1">
        {tabs.map((t) => {
          const Icon = t.icon;
          const active = tab === t.key;
          return (
            <button
              key={t.key}
              onClick={() => setTab(t.key)}
              className={`inline-flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-semibold transition ${
                active ? 'bg-brand-700 text-white' : 'text-accent-slate dark:text-ink-300 hover:bg-accent-surface'
              }`}
            >
              <Icon size={14} /> {t.label}
            </button>
          );
        })}
      </div>

      {tab === 'spend'      && <SpendTab />}
      {tab === 'revenue'    && <RevenueTab />}
      {tab === 'loans'      && <LoansTab />}
      {tab === 'compliance' && <ComplianceTab />}
      {tab === 'customers'  && <CustomersTab />}
    </div>
  );
}

function SpendTab() {
  const { data, loading } = useAsync(() => analyticsApi.spendAnalysis().catch(() => null), []);
  if (loading) return <DashboardSkeleton />;
  if (!data) return <p className="text-accent-mute dark:text-ink-400">Could not load spend analysis.</p>;

  const categories  = data.categories  || [];
  const dailyTrend  = data.dailyTrend  || [];
  const topMerchants = data.topMerchants || [];
  const hasData     = Number(data.totalSpend || 0) > 0 || dailyTrend.length > 0;

  if (!hasData) {
    return <EmptyTab message="No transactions in the last 30 days. Once customers start transacting, daily spend and category breakdowns will appear here." />;
  }

  return (
    <div className="space-y-6">
      <section className="grid sm:grid-cols-2 xl:grid-cols-4 gap-5">
        <KpiCard label="Total volume (30d)" value={formatCompactINR(data.totalSpend)} sub={`Last ${data.windowDays} days`} icon={Wallet} accent="brand" />
        <KpiCard label="Avg daily"           value={formatINR(data.avgDaily)} icon={TrendingUp} accent="info" />
        <KpiCard label="Channels"            value={categories.length} sub="active channels" icon={Wallet} accent="gold" />
        <KpiCard label="Days with activity"  value={dailyTrend.filter((d) => Number(d.amount) > 0).length} icon={TrendingUp} accent="green" />
      </section>

      <section className="grid lg:grid-cols-[1.4fr_1fr] gap-6">
        <SpendChart data={dailyTrend} title="Daily volume trend" subtitle={`${data.windowDays}-day window`} />
        {categories.length > 0 ? (
          <CategoryDonut
            data={categories.map((c) => ({ name: c.name, value: Number(c.amount) }))}
            title="By channel"
            subtitle="Share of successful volume"
          />
        ) : <EmptyCard title="No channel breakdown yet" />}
      </section>

      {topMerchants.length > 0 && (
        <Card>
          <CardHeader title="Top merchants" subtitle="Highest spend in the current window" className="mb-4" />
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="text-left text-[11px] uppercase tracking-widest text-accent-mute dark:text-ink-400 border-b border-accent-line dark:border-ink-700">
                <tr><th className="py-3">Merchant</th><th className="py-3">Category</th><th className="py-3 text-right">Transactions</th><th className="py-3 text-right">Amount</th></tr>
              </thead>
              <tbody>
                {topMerchants.map((m) => (
                  <tr key={m.name} className="border-b border-accent-line/60 last:border-b-0">
                    <td className="py-3 font-semibold">{m.name}</td>
                    <td className="py-3"><Badge tone="brand">{m.category}</Badge></td>
                    <td className="py-3 text-right">{m.count}</td>
                    <td className="py-3 text-right font-semibold">{formatINR(m.amount)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}
    </div>
  );
}

function EmptyTab({ message }) {
  return (
    <Card className="p-10 text-center">
      <p className="text-accent-mute dark:text-ink-400">{message}</p>
    </Card>
  );
}

function EmptyCard({ title }) {
  return (
    <Card className="p-10 text-center">
      <p className="text-sm text-accent-mute dark:text-ink-400">{title}</p>
    </Card>
  );
}

function RevenueTab() {
  const { data, loading } = useAsync(() => analyticsApi.revenueTrends().catch(() => null), []);
  if (loading) return <DashboardSkeleton />;
  if (!data) return <p className="text-accent-mute dark:text-ink-400">Could not load revenue trends.</p>;

  const series    = data.series    || [];
  const breakdown = data.breakdown || [];
  const hasData   = Number(data.totalRevenue || 0) > 0;

  if (!hasData) {
    return <EmptyTab message="No revenue yet — revenue is derived from transaction fees and loan interest. Once the bank has transactions and disbursed loans, the breakdown appears here." />;
  }

  return (
    <div className="space-y-6">
      <section className="grid sm:grid-cols-2 xl:grid-cols-4 gap-5">
        <KpiCard label="Total revenue" value={formatCompactINR(data.totalRevenue)} sub={`Derived from real transactions + loans`} icon={TrendingUp} accent="brand" />
        <KpiCard label="Months"         value={data.windowMonths} sub="rolling window" icon={TrendingUp} accent="info" />
        <KpiCard label="Active months"  value={series.filter((s) => Number(s.total) > 0).length} icon={TrendingUp} accent="gold" />
        <KpiCard label="Channels"       value={breakdown.length} sub="revenue streams" icon={Wallet} accent="green" />
      </section>

      <section className="grid lg:grid-cols-[1.6fr_1fr] gap-6">
        <BarTrend data={series.map((d) => ({ label: d.month, Interest: Number(d.interest), Fees: Number(d.fees), FX: Number(d.fx) }))}
                  title="Revenue by month" subtitle={`${data.windowMonths}-month window`}
                  dataKeys={['Interest', 'Fees', 'FX']} formatCurrency stacked />
        {breakdown.length > 0 ? (
          <CategoryDonut
            data={breakdown.map((b) => ({ name: b.name, value: b.value }))}
            title="Revenue mix" subtitle="Share of total"
          />
        ) : <EmptyCard title="No revenue mix yet" />}
      </section>
    </div>
  );
}

function LoansTab() {
  const { data, loading } = useAsync(() => analyticsApi.loanPortfolio().catch(() => null), []);
  if (loading) return <DashboardSkeleton />;
  if (!data) return <p className="text-accent-mute dark:text-ink-400">Could not load loan portfolio.</p>;

  const byProduct = data.byProduct || [];
  const byStatus  = data.byStatus  || [];
  const npaTrend  = data.npaTrend  || [];
  const riskBuckets = data.riskBuckets || [];
  const portfolioValue = Number(data.portfolioValue || 0);

  if (portfolioValue === 0 && byProduct.length === 0) {
    return <EmptyTab message="No loans have been disbursed yet. Once a loan officer approves loan applications, portfolio breakdowns will appear here." />;
  }

  return (
    <div className="space-y-6">
      <section className="grid sm:grid-cols-2 xl:grid-cols-4 gap-5">
        <KpiCard label="Portfolio"    value={formatCompactINR(portfolioValue)} icon={Landmark} accent="brand" />
        <KpiCard label="Active loans" value={Number(data.activeLoanCount || 0).toLocaleString('en-IN')} icon={Landmark} accent="info" />
        <KpiCard label="Avg ticket"   value={formatCompactINR(data.averageTicketSize || 0)} icon={TrendingUp} accent="gold" />
        <KpiCard label="NPA ratio"    value={`${data.npaRatioPct || 0}%`} icon={ArrowDownRight} accent="green" />
      </section>

      <section className="grid lg:grid-cols-2 gap-6">
        {byProduct.length > 0
          ? <CategoryDonut data={byProduct} title="Portfolio by product" subtitle="Share by amount" />
          : <EmptyCard title="No product breakdown yet" />}
        {byStatus.length > 0
          ? <CategoryDonut data={byStatus}  title="Portfolio by status"  subtitle="Active vs. overdue" />
          : <EmptyCard title="No status breakdown yet" />}
      </section>

      {(npaTrend.length > 0 || riskBuckets.length > 0) && (
        <section className="grid lg:grid-cols-[1.6fr_1fr] gap-6">
          {npaTrend.length > 0
            ? <LineTrend data={npaTrend} title="NPA trend" subtitle="Last 12 months" xKey="month" dataKeys={['npaPct']} />
            : <EmptyCard title="NPA trend not tracked yet" />}
          {riskBuckets.length > 0
            ? <CategoryDonut data={riskBuckets} title="Risk distribution" subtitle="By risk bucket" />
            : <EmptyCard title="Risk distribution not tracked yet" />}
        </section>
      )}
    </div>
  );
}

function ComplianceTab() {
  const { data, loading } = useAsync(() => analyticsApi.complianceMetrics().catch(() => null), []);
  if (loading) return <DashboardSkeleton />;
  if (!data) return <p className="text-accent-mute dark:text-ink-400">Could not load compliance metrics.</p>;

  const toneFor = (s) => s === 'PASS' ? 'success' : s === 'WARN' ? 'warning' : 'danger';

  return (
    <div className="space-y-6">
      <section className="grid sm:grid-cols-2 xl:grid-cols-4 gap-5">
        <KpiCard label="Overall score"    value={`${data.overallScore}`} sub={`Rating ${data.complianceRating}`} icon={ShieldCheck} accent="brand" />
        <KpiCard label="Pass"             value={data.passCount} sub={`${data.checksTotal} checks total`} icon={ShieldCheck} accent="green" />
        <KpiCard label="Warnings"         value={data.warnCount} icon={ShieldCheck} accent="gold" />
        <KpiCard label="Failures"         value={data.failCount} icon={ShieldCheck} accent="info" />
      </section>

      <Card>
        <CardHeader title="Compliance checklist" subtitle="Real-time status across critical controls" className="mb-4" />
        <ul className="divide-y divide-accent-line">
          {data.checklist.map((c) => (
            <li key={c.name} className="py-4 flex items-center justify-between gap-4">
              <div className="flex-1 min-w-0">
                <p className="font-semibold text-accent-ink dark:text-ink-100">{c.name}</p>
                <p className="text-xs text-accent-mute dark:text-ink-400 mt-0.5">{c.detail}</p>
              </div>
              <div className="flex items-center gap-4">
                <span className="text-sm text-accent-mute dark:text-ink-400 hidden sm:inline">Score {c.score}</span>
                <Badge tone={toneFor(c.status)} dot>{c.status}</Badge>
              </div>
            </li>
          ))}
        </ul>
      </Card>

      <Card>
        <CardHeader title="Recent breaches & alerts" subtitle="Last 30 days" className="mb-4" />
        <ul className="divide-y divide-accent-line">
          {data.recentBreaches.map((b) => (
            <li key={b.id} className="py-3 flex items-center justify-between gap-4">
              <div>
                <p className="font-semibold text-sm">{b.title}</p>
                <p className="text-xs text-accent-mute dark:text-ink-400">{b.id} · opened {b.openedAt}</p>
              </div>
              <div className="flex items-center gap-3">
                <Badge tone={b.severity === 'HIGH' ? 'danger' : b.severity === 'MEDIUM' ? 'warning' : 'info'}>{b.severity}</Badge>
                <Badge tone="neutral">{b.status}</Badge>
              </div>
            </li>
          ))}
        </ul>
      </Card>
    </div>
  );
}

function CustomersTab() {
  const { data, loading } = useAsync(() => analyticsApi.customerInsights().catch(() => null), []);
  if (loading) return <DashboardSkeleton />;
  if (!data) return <p className="text-accent-mute dark:text-ink-400">Could not load customer insights.</p>;

  const total      = Number(data.totalCustomers || 0);
  const segments   = data.segments || [];
  const acq        = data.acquisitionTrend || [];
  const topCities  = data.topCities || [];

  if (total === 0) {
    return <EmptyTab message="No customers yet. Once customers sign up and complete their profile, segments and acquisition trends will appear here." />;
  }

  return (
    <div className="space-y-6">
      <section className="grid sm:grid-cols-2 xl:grid-cols-4 gap-5">
        <KpiCard label="Total customers"   value={total.toLocaleString('en-IN')} icon={Users} accent="brand" />
        <KpiCard label="Active"            value={Number(data.activeCustomers30d || 0).toLocaleString('en-IN')} icon={Users} accent="info" />
        <KpiCard label="Churn rate"        value={`${data.churnRatePct || 0}%`} icon={ArrowDownRight} accent="gold" />
        <KpiCard label="New today"         value={Number(data.newCustomersToday || 0).toLocaleString('en-IN')} sub="Joined today" icon={TrendingUp} accent="green" />
      </section>

      <section className="grid lg:grid-cols-2 gap-6">
        {segments.length > 0
          ? <CategoryDonut data={segments} title="Customer segments" subtitle="Share of base by status" />
          : <EmptyCard title="No segment breakdown yet" />}
        {acq.length > 0
          ? <BarTrend data={acq.map((a) => ({ label: a.month, New: Number(a.newCustomers), Churned: Number(a.churned) }))}
                      title="Acquisition vs. churn" subtitle="Last 12 months"
                      dataKeys={['New', 'Churned']} />
          : <EmptyCard title="No acquisition trend yet" />}
      </section>

      {topCities.length > 0 && (
        <Card>
          <CardHeader title="Top cities by customer count" className="mb-4" />
          <ul className="space-y-2.5">
            {topCities.map((c, i) => {
              const max = Math.max(...topCities.map((x) => Number(x.value)));
              const pct = max > 0 ? (Number(c.value) / max) * 100 : 0;
              return (
                <li key={c.name}>
                  <div className="flex items-center justify-between text-sm mb-1">
                    <span className="font-medium">{i + 1}. {c.name}</span>
                    <span className="font-semibold text-accent-ink dark:text-ink-100">{Number(c.value).toLocaleString('en-IN')}</span>
                  </div>
                  <div className="h-2 rounded-full bg-accent-line/60 dark:bg-ink-700 overflow-hidden">
                    <div className="h-full bg-brand-700 dark:bg-brand-500 rounded-full" style={{ width: `${pct}%` }} />
                  </div>
                </li>
              );
            })}
          </ul>
        </Card>
      )}
    </div>
  );
}

function DashboardSkeleton() {
  return (
    <div className="space-y-6">
      <div className="grid sm:grid-cols-2 xl:grid-cols-4 gap-5">
        {Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-32 rounded-xl2" />)}
      </div>
      <div className="grid lg:grid-cols-2 gap-6">
        <Skeleton className="h-72 rounded-xl2" />
        <Skeleton className="h-72 rounded-xl2" />
      </div>
    </div>
  );
}
