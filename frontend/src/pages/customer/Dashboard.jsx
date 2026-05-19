import { Link } from 'react-router-dom';
import {
  Wallet, TrendingUp, ArrowLeftRight, Landmark,
  Plus, Send, Smartphone, Receipt, ShieldCheck, ChevronRight, Sparkles,
  UserPlus, FileText, CheckCircle2,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { PageHeader } from '../../components/layout/PageHeader';
import { Card, CardHeader } from '../../components/common/Card';
import { Badge } from '../../components/common/Badge';
import { KpiCard } from '../../components/dashboard/KpiCard';
import { AccountSummaryCard } from '../../components/dashboard/AccountSummaryCard';
import { SpendChart } from '../../components/dashboard/SpendChart';
import { CategoryDonut } from '../../components/dashboard/CategoryDonut';
import { TransactionRow } from '../../components/dashboard/TransactionRow';
import { EmptyState } from '../../components/common/EmptyState';
import { Skeleton } from '../../components/common/Skeleton';
import { useAsync } from '../../hooks/useAsync';
import { accountApi } from '../../api/account';
import { transactionApi } from '../../api/transaction';
import { formatINR, formatCompactINR } from '../../utils/format';

export default function Dashboard() {
  const { user } = useAuth();

  const { data: accounts, loading: accountsLoading } = useAsync(
    () => accountApi.myAccounts().catch(() => []),
    []
  );

  const primaryAccount = accounts?.[0];
  const hasAccount = (accounts || []).length > 0;

  // Pull transaction history from EVERY account the customer holds and
  // interleave the results — so transfers, deposits, withdrawals, loan
  // disbursements, EMI payments and prepayments all show up regardless of
  // which account they hit.
  const { data: txData, loading: txLoading } = useAsync(
    async () => {
      if (!accounts || accounts.length === 0) {
        return { recent: [], primary: [], totalCount: 0 };
      }
      const pages = await Promise.all(
        accounts.map((a) =>
          transactionApi.getByAccount(a.accountNo, { page: 0, size: 30 }).catch(() => null)
        )
      );
      const valid = pages.filter(Boolean);

      // Self-transfers (between two of the customer's own accounts) come back
      // twice — once per fan-out call. Dedupe on transactionId / reference.
      const seen = new Set();
      const merged = [];
      for (const p of valid) {
        for (const tx of (p.content || [])) {
          const id = tx.transactionId || tx.referenceNumber;
          if (id && seen.has(id)) continue;
          if (id) seen.add(id);
          merged.push(tx);
        }
      }
      merged.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
      const primary = pages[0]?.content || [];
      // totalCount also corrects for the same double-count.
      const totalCount = merged.length;
      return { recent: merged.slice(0, 6), primary, totalCount };
    },
    [accounts]
  );

  const recentTransactions = txData?.recent || [];
  const primaryTxs = txData?.primary || [];
  const hasTransactions = recentTransactions.length > 0;
  const totalBalance = (accounts || []).reduce((sum, a) => sum + Number(a.balance || 0), 0);

  // List of every account number the customer owns — passed to TransactionRow
  // so it can decide credit vs debit by checking whether any of them is the
  // receiver. Normalising happens inside the row.
  const myAccountNos = (accounts || []).map((a) => a.accountNo);

  // ── New user / no accounts yet → onboarding flow ──────────────────────────
  if (!accountsLoading && !hasAccount) {
    return <OnboardingDashboard user={user} />;
  }

  return (
    <div className="space-y-8">
      <PageHeader
        breadcrumb={`Customer · ${user?.role || 'Member'}`}
        title={`Welcome back, ${(user?.fullName || 'there').split(' ')[0]}`}
        subtitle="Here's a snapshot of your accounts, recent activity and personalised insights."
        actions={
          <>
            <Link to="/app/transfer" className="btn-secondary"><Send size={14} /> Send money</Link>
            <Link to="/app/accounts/apply" className="btn-primary"><Plus size={14} /> Open account</Link>
          </>
        }
      />

      {/* ── KPI strip — values come from real data, no fabricated deltas ──── */}
      <section className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-5">
        <KpiCard
          label="Total balance"
          value={accountsLoading ? '…' : formatINR(totalBalance)}
          sub={`${accounts?.length || 0} active account${(accounts?.length || 0) === 1 ? '' : 's'}`}
          icon={Wallet}
          accent="brand"
        />
        <KpiCard
          label="Transactions"
          value={txData?.totalCount ?? recentTransactions.length}
          sub={hasTransactions ? `across ${accounts?.length || 0} account${(accounts?.length || 0) === 1 ? '' : 's'}` : 'No transactions yet'}
          icon={ArrowLeftRight}
          accent="info"
        />
        <KpiCard
          label="Spend (7d)"
          value={primaryTxs.length > 0 ? formatCompactINR(computeSpendLast7Days(primaryTxs, primaryAccount?.accountNo)) : '—'}
          sub={primaryTxs.length > 0 ? 'last 7 days · primary account' : 'No spend data yet'}
          icon={TrendingUp}
          accent="gold"
        />
        <KpiCard
          label="Active loans"
          value="0"
          sub="No loans yet"
          icon={Landmark}
          accent="green"
        />
      </section>

      {/* ── Account hero + quick actions ──────────────────────────────────── */}
      <section className="grid lg:grid-cols-[1.4fr_1fr] gap-6">
        {accountsLoading ? (
          <Skeleton className="h-64 rounded-2xl" />
        ) : (
          <AccountSummaryCard account={primaryAccount} />
        )}

        <Card>
          <CardHeader title="Quick actions" subtitle="Common things you might want to do" />
          <div className="mt-5 grid grid-cols-2 gap-3">
            <QuickAction to="/app/transfer"        icon={Send}        label="Send money" />
            <QuickAction to="/app/transactions"    icon={Receipt}     label="View statement" />
            <QuickAction to="/app/transfer?bill=1" icon={Smartphone}  label="Pay bills" />
            <QuickAction to="/app/loans/apply"     icon={Landmark}    label="Apply for loan" />
          </div>
          <div className="mt-6 rounded-xl bg-gradient-to-r from-brand-50 to-amber-50 p-4 flex items-center gap-3">
            <span className="h-10 w-10 rounded-full bg-white grid place-items-center text-brand-700">
              <Sparkles size={18} />
            </span>
            <div className="flex-1">
              <p className="text-sm font-semibold">Personalised offer</p>
              <p className="text-xs text-accent-mute dark:text-ink-400">5% off on your next loan EMI when paid via UPI.</p>
            </div>
          </div>
        </Card>
      </section>

      {/* ── Charts — primary-account only (spend trends per account are clearer) */}
      {primaryTxs.length > 0 ? (
        <section className="grid lg:grid-cols-[1.4fr_1fr] gap-6">
          <SpendChart data={buildSpendTrend(primaryTxs, primaryAccount?.accountNo)} />
          <CategoryDonut data={buildSpendByCategory(primaryTxs, primaryAccount?.accountNo)} />
        </section>
      ) : (
        <Card>
          <CardHeader
            title="Spend insights"
            subtitle="Your spending patterns will appear here once you start transacting"
            className="mb-4"
          />
          <EmptyState
            icon={TrendingUp}
            title="No spend data yet"
            message="Make your first transaction and we'll start showing trends, categories and personalised insights."
          />
        </Card>
      )}

      {/* ── Recent transactions + KYC status ──────────────────────────────── */}
      <section className="grid lg:grid-cols-[1.4fr_1fr] gap-6">
        <Card>
          <CardHeader
            title="Recent transactions"
            subtitle="Latest activity across all your accounts — transfers, deposits, withdrawals, EMI payments and loan disbursements"
            action={
              <Link to="/app/transactions" className="text-sm font-semibold text-brand-700 hover:underline inline-flex items-center gap-1">
                View all <ChevronRight size={14} />
              </Link>
            }
            className="mb-2"
          />
          {txLoading ? (
            <div className="space-y-3 mt-4">
              {Array.from({ length: 5 }).map((_, i) => <Skeleton key={i} className="h-12 w-full" />)}
            </div>
          ) : recentTransactions.length === 0 ? (
            <EmptyState
              icon={Receipt}
              title="No transactions yet"
              message="Once you start using your account, your activity will show up here."
            />
          ) : (
            <div className="mt-2 divide-y divide-accent-line/70">
              {recentTransactions.map((tx) => (
                <TransactionRow
                  key={tx.transactionId || tx.referenceNumber}
                  tx={tx}
                  myAccountNos={myAccountNos}
                />
              ))}
            </div>
          )}
        </Card>

        <KycStatusCard />
      </section>
    </div>
  );
}

// ── Onboarding dashboard for users with no accounts ──────────────────────────
function OnboardingDashboard({ user }) {
  const firstName = (user?.fullName || 'there').split(' ')[0];
  return (
    <div className="space-y-8">
      <PageHeader
        breadcrumb="Customer · Getting started"
        title={`Welcome to BankSphere, ${firstName}`}
        subtitle="Let's get you set up. Complete these steps to start banking."
      />

      {/* Hero welcome card */}
      <div className="relative overflow-hidden rounded-2xl bg-gradient-to-br from-brand-700 to-brand-900 text-white p-8 shadow-elevated">
        <div className="absolute -right-16 -top-16 h-48 w-48 rounded-full bg-accent-gold/20 blur-3xl" />
        <div className="absolute -left-10 -bottom-10 h-40 w-40 rounded-full bg-brand-500/30 blur-3xl" />
        <div className="relative max-w-2xl">
          <Badge tone="brand" className="bg-white/15 text-white border border-white/20"><Sparkles size={12} /> Get started</Badge>
          <h2 className="mt-4 font-display text-3xl font-extrabold leading-tight">
            Your account is ready.<br />Now let's open one.
          </h2>
          <p className="mt-3 text-white/80 leading-relaxed max-w-lg">
            We've created your profile — the next step is to complete your customer details, verify your identity,
            and open a bank account. It only takes a few minutes.
          </p>
        </div>
      </div>

      {/* Step-by-step onboarding */}
      <Card>
        <CardHeader
          title="Your onboarding journey"
          subtitle="Follow these steps in order — each one unlocks the next"
          className="mb-6"
        />
        <ol className="space-y-3">
          <OnboardingStep
            step={1}
            to="/app/profile"
            icon={UserPlus}
            title="Complete your customer profile"
            description="Personal details, address and contact information. This creates your customer number."
            cta="Add my details"
            status="pending"
          />
          <OnboardingStep
            step={2}
            to="/app/kyc"
            icon={ShieldCheck}
            title="Submit your KYC documents"
            description="Aadhaar or PAN so we can verify your identity. Required for all banking activities."
            cta="Start KYC"
            status="pending"
          />
          <OnboardingStep
            step={3}
            to="/app/accounts/apply"
            icon={Wallet}
            title="Open your first bank account"
            description="Savings, Current, FD or RD — pick what fits your needs. Approved in one business day."
            cta="Apply now"
            status="pending"
          />
          <OnboardingStep
            step={4}
            to="/app/transfer"
            icon={Send}
            title="Make your first transaction"
            description="Once your account is active, you can deposit funds and start transacting."
            cta="Coming next"
            status="locked"
          />
        </ol>
      </Card>

      {/* Why bank with us */}
      <section className="grid sm:grid-cols-3 gap-5">
        <FeatureTile
          icon={ShieldCheck}
          title="Bank-grade security"
          description="Encrypted from end to end, secure logins, and instant alerts on every transaction."
        />
        <FeatureTile
          icon={TrendingUp}
          title="Smart insights"
          description="Personalised analytics, spend categories and saving goals."
        />
        <FeatureTile
          icon={Smartphone}
          title="24×7 banking"
          description="Access your accounts and transact anytime, from any device."
        />
      </section>
    </div>
  );
}

function OnboardingStep({ step, to, icon: Icon, title, description, cta, status }) {
  const locked = status === 'locked';
  const done = status === 'done';
  return (
    <li className={`flex items-start gap-4 p-5 rounded-xl border transition ${
      locked ? 'bg-accent-surface/40 border-accent-line dark:border-ink-700 opacity-60' :
      done ? 'bg-green-50/40 border-green-100' :
      'bg-white border-accent-line dark:border-ink-700 hover:border-brand-200'
    }`}>
      <span className={`h-11 w-11 rounded-xl grid place-items-center shrink-0 ${
        done ? 'bg-accent-success text-white' :
        locked ? 'bg-accent-line/60 text-accent-mute dark:text-ink-400' :
        'bg-brand-700 text-white'
      }`}>
        {done ? <CheckCircle2 size={18} /> : <Icon size={18} />}
      </span>
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2">
          <span className="text-[10px] font-bold uppercase tracking-widest text-accent-mute dark:text-ink-400">Step {step}</span>
          {done && <Badge tone="success" dot>Done</Badge>}
        </div>
        <h3 className="mt-1 font-semibold text-accent-ink dark:text-ink-100">{title}</h3>
        <p className="text-sm text-accent-slate dark:text-ink-300 mt-1">{description}</p>
      </div>
      {!locked && !done && (
        <Link to={to} className="btn-primary self-center shrink-0">
          {cta} <ChevronRight size={14} />
        </Link>
      )}
      {locked && (
        <span className="text-xs text-accent-mute dark:text-ink-400 self-center shrink-0">{cta}</span>
      )}
    </li>
  );
}

function FeatureTile({ icon: Icon, title, description }) {
  return (
    <div className="card-hover p-6">
      <span className="h-11 w-11 rounded-full bg-brand-50 text-brand-700 grid place-items-center">
        <Icon size={20} />
      </span>
      <h4 className="mt-4 font-semibold text-accent-ink dark:text-ink-100">{title}</h4>
      <p className="mt-1.5 text-sm text-accent-slate dark:text-ink-300 leading-relaxed">{description}</p>
    </div>
  );
}

// ── Existing helpers ─────────────────────────────────────────────────────────
function QuickAction({ to, icon: Icon, label }) {
  return (
    <Link to={to} className="card-hover p-4 flex flex-col items-start gap-3 group">
      <span className="h-10 w-10 rounded-xl bg-brand-50 text-brand-700 grid place-items-center group-hover:bg-brand-700 group-hover:text-white transition">
        <Icon size={18} />
      </span>
      <span className="text-sm font-semibold text-accent-ink dark:text-ink-100">{label}</span>
    </Link>
  );
}

function KycStatusCard() {
  return (
    <Card>
      <CardHeader title="Account hygiene" subtitle="Keep your profile up to date" className="mb-4" />
      <ul className="space-y-3">
        <HygieneItem
          icon={ShieldCheck}
          title="Complete your KYC"
          message="Submit PAN and Aadhaar to unlock the full set of banking features."
          status="pending"
          to="/app/kyc"
        />
        <HygieneItem
          icon={Smartphone}
          title="Verify your phone"
          message="Used for OTPs and important account alerts."
          status="done"
        />
        <HygieneItem
          icon={Receipt}
          title="Set a nominee"
          message="Add a nominee for your accounts and deposits."
          status="pending"
          to="/app/profile"
        />
      </ul>
    </Card>
  );
}

function HygieneItem({ icon: Icon, title, message, status, to }) {
  const tone = status === 'done' ? 'success' : 'warning';
  return (
    <li className="flex items-start gap-3">
      <span className={`h-9 w-9 rounded-full grid place-items-center shrink-0 ${status === 'done' ? 'bg-green-50 text-accent-success' : 'bg-amber-50 text-accent-warning'}`}>
        <Icon size={15} />
      </span>
      <div className="flex-1 min-w-0">
        <div className="flex items-center justify-between gap-2">
          <p className="text-sm font-semibold text-accent-ink dark:text-ink-100">{title}</p>
          <Badge tone={tone} dot>{status === 'done' ? 'Done' : 'Pending'}</Badge>
        </div>
        <p className="text-xs text-accent-mute dark:text-ink-400 mt-0.5">{message}</p>
        {to && status !== 'done' && (
          <Link to={to} className="mt-1.5 inline-flex items-center gap-1 text-xs font-semibold text-brand-700 hover:underline">
            Take action <ChevronRight size={12} />
          </Link>
        )}
      </div>
    </li>
  );
}

// ── Real spend aggregation from actual transactions ──────────────────────────
function isDebit(tx, accountId) {
  if (!accountId) return ['WITHDRAWAL', 'PAYMENT'].includes(tx.transactionType);
  return tx.transactionType === 'WITHDRAWAL'
      || tx.transactionType === 'PAYMENT'
      || (tx.transactionType === 'TRANSFER' && tx.senderAccountId === accountId);
}

function computeSpendLast7Days(transactions, accountId) {
  const cutoff = Date.now() - 7 * 24 * 60 * 60 * 1000;
  return transactions
    .filter((tx) => isDebit(tx, accountId))
    .filter((tx) => new Date(tx.createdAt).getTime() >= cutoff)
    .reduce((sum, tx) => sum + Number(tx.amount || 0), 0);
}

function buildSpendTrend(transactions, accountId) {
  const days = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
  const buckets = new Map();
  for (let i = 6; i >= 0; i--) {
    const d = new Date();
    d.setDate(d.getDate() - i);
    buckets.set(d.toISOString().slice(0, 10), { label: days[d.getDay()], amount: 0 });
  }
  for (const tx of transactions) {
    if (!isDebit(tx, accountId)) continue;
    const key = new Date(tx.createdAt).toISOString().slice(0, 10);
    if (buckets.has(key)) buckets.get(key).amount += Number(tx.amount || 0);
  }
  return Array.from(buckets.values());
}

function buildSpendByCategory(transactions, accountId) {
  // Until the backend exposes a category field on transactions, group by
  // channel as a reasonable proxy (UPI, NEFT, IMPS, RTGS, INTERNAL).
  const buckets = new Map();
  for (const tx of transactions) {
    if (!isDebit(tx, accountId)) continue;
    const key = tx.channel || 'Other';
    buckets.set(key, (buckets.get(key) || 0) + Number(tx.amount || 0));
  }
  return Array.from(buckets.entries()).map(([name, value]) => ({ name, value }));
}
