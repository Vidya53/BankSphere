import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  TicketCheck, ShieldCheck, UserPlus, Users, Search, RefreshCw,
  ChevronRight, Wallet, PauseCircle, ShieldOff, Trash2,
} from 'lucide-react';
import toast from 'react-hot-toast';
import { PageHeader } from '../../components/layout/PageHeader';
import { Card, CardHeader } from '../../components/common/Card';
import { Skeleton } from '../../components/common/Skeleton';
import { Badge, StatusBadge } from '../../components/common/Badge';
import { KpiCard } from '../../components/dashboard/KpiCard';
import { Input } from '../../components/common/Input';
import { Button } from '../../components/common/Button';
import { EmptyState } from '../../components/common/EmptyState';
import { useAuth } from '../../context/AuthContext';
import { dashboardsApi } from '../../api/dashboards';
import { supportApi } from '../../api/support';
import { accountApi } from '../../api/account';
import { customerApi } from '../../api/customer';
import { formatDateTime } from '../../utils/format';
import { errorMessage } from '../../api/client';

const PRIORITY_TONE = { HIGH: 'danger', MEDIUM: 'warning', LOW: 'neutral', NORMAL: 'neutral' };

export default function CsrDashboard() {
  const { user } = useAuth();
  const branchCode = user?.branchCode || null;

  const [dashboard, setDashboard] = useState(null);
  const [tickets,   setTickets]   = useState([]);
  const [pendingApps, setPendingApps] = useState([]);
  const [loading, setLoading] = useState(true);

  const reload = async () => {
    setLoading(true);
    try {
      const [dash, tx, apps] = await Promise.all([
        dashboardsApi.csr(branchCode).catch(() => null),
        supportApi.list({ status: 'OPEN' }).catch(() => []),
        accountApi.pendingApplications().catch(() => []),
      ]);
      setDashboard(dash);
      setTickets(Array.isArray(tx) ? tx : []);
      setPendingApps(Array.isArray(apps) ? apps : []);
    } catch (err) {
      toast.error(errorMessage(err, 'Could not load the CSR dashboard.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { reload(); }, [branchCode]); // eslint-disable-line react-hooks/exhaustive-deps

  if (loading && !dashboard) {
    return (
      <div className="space-y-6">
        <div className="grid sm:grid-cols-2 xl:grid-cols-4 gap-5">
          {Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-28 rounded-xl2" />)}
        </div>
        <Skeleton className="h-96 rounded-xl2" />
      </div>
    );
  }

  const k        = dashboard?.kpis || {};
  const kycQueue = dashboard?.kycQueue || [];
  const recents  = dashboard?.recentCustomers || [];

  return (
    <div className="space-y-8">
      <PageHeader
        breadcrumb={`Staff · CSR${branchCode ? ' · ' + branchCode : ''}`}
        title="Customer Service Dashboard"
        subtitle="Live queues and recent customer activity — pulled directly from the database."
        actions={<Button variant="secondary" icon={RefreshCw} onClick={reload}>Refresh</Button>}
      />

      <section className="grid sm:grid-cols-2 xl:grid-cols-4 gap-5">
        <KpiCard label="Open tickets"          value={tickets.length}            icon={TicketCheck} accent="brand" />
        <KpiCard label="Pending KYC"           value={k.pendingKyc ?? 0}         icon={ShieldCheck} accent="info" />
        <KpiCard label="New customers (today)" value={k.newCustomersToday ?? 0}  icon={UserPlus}    accent="green" />
        <KpiCard label="Pending applications"  value={pendingApps.length}        icon={Wallet}      accent="gold" />
      </section>

      <section className="grid lg:grid-cols-2 gap-6">
        <Card>
          <CardHeader
            title="KYC review queue"
            subtitle={`${kycQueue.length} pending review${kycQueue.length === 1 ? '' : 's'}`}
            action={
              <Link to="/staff/kyc-review" className="text-sm font-semibold text-brand-700 dark:text-brand-300 hover:underline inline-flex items-center gap-1">
                Open queue <ChevronRight size={14} />
              </Link>
            }
            className="mb-4"
          />
          {kycQueue.length === 0 ? (
            <EmptyState icon={ShieldCheck} title="No pending KYC" message="When customers submit KYC, they'll appear here for review." />
          ) : (
            <ul className="divide-y divide-accent-line dark:divide-ink-700">
              {kycQueue.slice(0, 6).map((row) => (
                <li key={row.id} className="py-3 flex items-center justify-between gap-3">
                  <div>
                    <p className="font-semibold text-sm dark:text-ink-100">{row.name}</p>
                    <p className="text-xs text-accent-mute dark:text-ink-400">
                      {row.customerNo} · {row.documentType} · {formatDateTime(row.submittedAt || row.createdAt)}
                    </p>
                  </div>
                  <StatusBadge status={row.status} />
                </li>
              ))}
            </ul>
          )}
        </Card>

        <Card>
          <CardHeader
            title="Account applications"
            subtitle={`${pendingApps.length} application${pendingApps.length === 1 ? '' : 's'} waiting`}
            action={
              <Link to="/staff/account-applications" className="text-sm font-semibold text-brand-700 dark:text-brand-300 hover:underline inline-flex items-center gap-1">
                Open queue <ChevronRight size={14} />
              </Link>
            }
            className="mb-4"
          />
          {pendingApps.length === 0 ? (
            <EmptyState icon={Wallet} title="No applications waiting" message="New account applications for your branch will appear here." />
          ) : (
            <ul className="divide-y divide-accent-line dark:divide-ink-700">
              {pendingApps.slice(0, 6).map((a) => (
                <li key={a.id || a.applicationRef} className="py-3 flex items-center justify-between gap-3">
                  <div>
                    <p className="font-semibold text-sm dark:text-ink-100">{a.customerName}</p>
                    <p className="text-xs text-accent-mute dark:text-ink-400">
                      {a.applicationRef} · {a.accountType} · {formatDateTime(a.createdAt)}
                    </p>
                  </div>
                  <StatusBadge status={a.status} />
                </li>
              ))}
            </ul>
          )}
        </Card>
      </section>

      <section className="grid lg:grid-cols-[1.4fr_1fr] gap-6">
        <Card>
          <CardHeader
            title="Open support tickets"
            subtitle={`${tickets.length} ticket${tickets.length === 1 ? '' : 's'} open`}
            action={
              <Link to="/staff/support" className="text-sm font-semibold text-brand-700 dark:text-brand-300 hover:underline inline-flex items-center gap-1">
                Open queue <ChevronRight size={14} />
              </Link>
            }
            className="mb-2"
          />
          {tickets.length === 0 ? (
            <EmptyState icon={TicketCheck} title="No open tickets" message="When customers raise support requests, they'll appear here." />
          ) : (
            <div className="overflow-x-auto -mx-6">
              <table className="w-full text-sm">
                <thead className="text-left text-[11px] uppercase tracking-widest text-accent-mute dark:text-ink-400 border-b border-accent-line dark:border-ink-700">
                  <tr>
                    <th className="px-6 py-3">ID</th>
                    <th className="py-3">Subject</th>
                    <th className="py-3">Customer</th>
                    <th className="py-3">Priority</th>
                    <th className="py-3">Opened</th>
                  </tr>
                </thead>
                <tbody>
                  {tickets.slice(0, 8).map((t) => (
                    <tr key={t.id} className="border-b border-accent-line/60 dark:border-ink-700/60 last:border-b-0 hover:bg-accent-surface/40 dark:hover:bg-ink-750/40">
                      <td className="px-6 py-3 font-mono text-xs">{t.id}</td>
                      <td className="py-3 font-medium dark:text-ink-100">{t.subject}</td>
                      <td className="py-3 dark:text-ink-300">{t.customerName}</td>
                      <td className="py-3"><Badge tone={PRIORITY_TONE[t.priority] || 'neutral'}>{t.priority}</Badge></td>
                      <td className="py-3 text-accent-mute dark:text-ink-400 text-xs">{formatDateTime(t.createdAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Card>

        <CustomerSearchCard onChanged={reload} />
      </section>

      <Card>
        <CardHeader
          title="Recently onboarded customers"
          subtitle="Customers go ACTIVE automatically once KYC and an account are approved · block or deactivate when needed"
          className="mb-4"
        />
        {recents.length === 0 ? (
          <EmptyState icon={Users} title="No customers yet" />
        ) : (
          <div className="overflow-x-auto -mx-6">
            <table className="w-full text-sm">
              <thead className="text-left text-[11px] uppercase tracking-widest text-accent-mute dark:text-ink-400 border-b border-accent-line dark:border-ink-700">
                <tr>
                  <th className="px-6 py-3">Customer</th>
                  <th className="py-3">Contact</th>
                  <th className="py-3">City</th>
                  <th className="py-3">Status</th>
                  <th className="py-3">Created</th>
                  <th className="py-3 text-right pr-6">Actions</th>
                </tr>
              </thead>
              <tbody>
                {recents.map((c) => (
                  <tr key={c.customerNo} className="border-b border-accent-line/60 dark:border-ink-700/60 last:border-b-0 hover:bg-accent-surface/40 dark:hover:bg-ink-750/40">
                    <td className="px-6 py-3">
                      <div className="font-semibold dark:text-ink-100">{c.name || c.customerNo}</div>
                      <div className="text-xs font-mono text-accent-mute dark:text-ink-400">{c.customerNo}</div>
                    </td>
                    <td className="py-3 text-accent-slate dark:text-ink-300">
                      <div>{c.email || '—'}</div>
                      <div className="text-xs text-accent-mute dark:text-ink-400">{c.mobile || ''}</div>
                    </td>
                    <td className="py-3 dark:text-ink-300">{c.city || '—'}</td>
                    <td className="py-3"><StatusBadge status={c.status} /></td>
                    <td className="py-3 text-accent-mute dark:text-ink-400 text-xs">{formatDateTime(c.createdAt)}</td>
                    <td className="py-3 pr-6 text-right">
                      <CustomerStatusActions customer={c} onChanged={reload} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </div>
  );
}

//  Customer status actions
//
//  Activation is automatic: account-service promotes REGISTERED → ACTIVE the
//  moment an account application is approved (which already required KYC).
//  CSRs are left with the exception-state controls only:
//    ACTIVE     → INACTIVE  (account dormant)
//    ACTIVE     → BLOCKED   (fraud / regulatory hold)
//
//  Each click confirms with the operator, calls the customer-service API,
//  and refreshes the parent list so the new status is reflected immediately.
function CustomerStatusActions({ customer, onChanged }) {
  const { user } = useAuth();
  const role = user?.role;
  const canSoftDelete = role === 'ADMIN' || role === 'BRANCH_MANAGER';
  const isDeleted = customer?.isDeleted === true;

  const [busy, setBusy] = useState(null); // 'deactivate' | 'block' | 'delete'
  const status = (customer?.status || '').toUpperCase();
  // Hide status transitions for already-deleted customers — they can't transact anyway.
  const canDeactivate = !isDeleted && status === 'ACTIVE';
  const canBlock      = !isDeleted && (status === 'ACTIVE' || status === 'REGISTERED');

  const run = async (kind, fn, verb) => {
    if (!confirm(`${verb} ${customer.name || customer.customerNo} (${customer.customerNo})?`)) return;
    setBusy(kind);
    try {
      await fn(customer.customerNo);
      toast.success(`${customer.customerNo} → ${verb.toLowerCase()}d`);
      onChanged?.();
    } catch (err) {
      toast.error(errorMessage(err, `Could not ${verb.toLowerCase()} this customer.`));
    } finally { setBusy(null); }
  };

  const runSoftDelete = async () => {
    if (!confirm(
      `Soft-delete ${customer.name || customer.customerNo} (${customer.customerNo})?\n\n` +
      `This marks the customer as CLOSED and closes every account they hold. ` +
      `The customer record stays in the database but they cannot transact ` +
      `or apply for loans.`
    )) return;
    setBusy('delete');
    try {
      await customerApi.softDelete(customer.customerNo);
      toast.success(`${customer.customerNo} soft-deleted`);
      onChanged?.();
    } catch (err) {
      toast.error(errorMessage(err, 'Could not soft-delete this customer.'));
    } finally { setBusy(null); }
  };

  return (
    <div className="inline-flex flex-wrap gap-1 justify-end">
      {canDeactivate && (
        <Button size="sm" variant="ghost" icon={PauseCircle}
                loading={busy === 'deactivate'}
                onClick={() => run('deactivate', customerApi.deactivate, 'Deactivate')}>
          Deactivate
        </Button>
      )}
      {canBlock && (
        <Button size="sm" variant="ghost" icon={ShieldOff}
                loading={busy === 'block'}
                onClick={() => run('block', customerApi.block, 'Block')}>
          Block
        </Button>
      )}
      {canSoftDelete && !isDeleted && (
        <Button size="sm" variant="ghost" icon={Trash2}
                loading={busy === 'delete'}
                onClick={runSoftDelete}>
          Soft-delete
        </Button>
      )}
      {isDeleted && (
        <span className="text-xs text-accent-mute dark:text-ink-400 italic">Deleted</span>
      )}
      {!isDeleted && !canDeactivate && !canBlock && !canSoftDelete && (
        <span className="text-xs text-accent-mute dark:text-ink-400 italic">No actions</span>
      )}
    </div>
  );
}

// ── Customer search card (live DB-backed lookup with inline actions) ─────────
function CustomerSearchCard({ onChanged }) {
  const [query,   setQuery]   = useState('');
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);

  const runSearch = async () => {
    if (query.trim().length < 2) { setResults([]); return; }
    setLoading(true);
    try {
      const rows = await dashboardsApi.csrSearchCustomers(query.trim(), 10);
      setResults(Array.isArray(rows) ? rows : []);
    } catch {
      setResults([]);
    } finally { setLoading(false); }
  };

  useEffect(() => {
    const t = setTimeout(runSearch, 350);
    return () => clearTimeout(t);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [query]);

  return (
    <Card>
      <CardHeader
        title="Customer search"
        subtitle="Find by name, email, mobile or customer number"
        className="mb-4"
      />
      <Input
        leftIcon={Search}
        placeholder="Type at least 2 characters…"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
      />
      <div className="mt-4 min-h-[140px]">
        {loading ? (
          <div className="space-y-2">
            {Array.from({ length: 3 }).map((_, i) => <Skeleton key={i} className="h-10" />)}
          </div>
        ) : results.length === 0 ? (
          <p className="text-sm text-accent-mute dark:text-ink-400 text-center py-6">
            {query.trim().length < 2 ? 'Start typing to search…' : 'No matches.'}
          </p>
        ) : (
          <ul className="divide-y divide-accent-line dark:divide-ink-700">
            {results.map((c) => (
              <li key={c.customerNo} className="py-2.5">
                <div className="flex items-center justify-between gap-3">
                  <div className="min-w-0">
                    <p className="font-semibold text-sm dark:text-ink-100 truncate">{c.name || c.customerNo}</p>
                    <p className="text-xs text-accent-mute dark:text-ink-400 truncate">
                      {c.customerNo} · {c.email || c.mobile || ''}
                    </p>
                  </div>
                  <div className="flex items-center gap-2 shrink-0">
                    <StatusBadge status={c.status} />
                  </div>
                </div>
                <div className="mt-1.5 -mr-1.5">
                  <CustomerStatusActions
                    customer={c}
                    onChanged={() => { runSearch(); onChanged?.(); }}
                  />
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </Card>
  );
}
