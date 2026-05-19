import { useCallback, useEffect, useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';
import {
  PiggyBank, Wallet, Banknote, CreditCard, Filter, Users,
  PauseCircle, PlayCircle, XCircle, Trash2, RefreshCw,
} from 'lucide-react';
import toast from 'react-hot-toast';
import { Card, CardHeader } from '../common/Card';
import { Badge, StatusBadge } from '../common/Badge';
import { Skeleton } from '../common/Skeleton';
import { Button } from '../common/Button';
import { Select, Textarea } from '../common/Input';
import { Modal } from '../common/Modal';
import { EmptyState } from '../common/EmptyState';
import { accountApi } from '../../api/account';
import { customerApi } from '../../api/customer';
import { errorMessage } from '../../api/client';
import { formatINR, formatDate } from '../../utils/format';

/**
 * Branch Manager + Admin-view panel that lists every customer + account at
 * the caller's own branch (via the X-Branch-Code header).
 *
 *  Two tables side by side:
 *    Customers — one row per unique customer, with a soft-delete button.
 *                Soft-delete cascades to close *every* account they hold, so
 *                showing one button per customer prevents the duplicate-click
 *                / 409 problem the old per-row UI had.
 *
 *    Accounts  — one row per account, with freeze / unfreeze / close per row.
 *                Each mutation requires a reason; the modal collects it.
 *
 *  `canMutate` is a hard switch — pass false to render everything read-only
 *  (used by the admin "preview" path; admins are no longer authorized to
 *  freeze/close/soft-delete from the BM dashboard).
 */
const ACCOUNT_TYPE_META = {
  SAVINGS:           { icon: PiggyBank, label: 'Savings',           tone: 'bg-brand-50 dark:bg-brand-900/40 text-brand-700 dark:text-brand-300' },
  CURRENT:           { icon: Wallet,    label: 'Current',           tone: 'bg-sky-50 dark:bg-sky-900/30 text-accent-info dark:text-sky-300' },
  SALARY:            { icon: Banknote,  label: 'Salary',            tone: 'bg-emerald-50 dark:bg-emerald-900/30 text-emerald-700 dark:text-emerald-300' },
  FIXED_DEPOSIT:     { icon: Banknote,  label: 'Fixed Deposit',     tone: 'bg-amber-50 dark:bg-amber-900/30 text-accent-warning dark:text-amber-300' },
  RECURRING_DEPOSIT: { icon: Banknote,  label: 'Recurring Deposit', tone: 'bg-amber-50 dark:bg-amber-900/30 text-accent-warning dark:text-amber-300' },
  LOAN:              { icon: CreditCard,label: 'Loan',              tone: 'bg-red-50 dark:bg-red-900/30 text-accent-danger dark:text-red-300' },
};

export default function BranchAccountsPanel({ canMutate = true, branchLabel }) {
  const [accounts, setAccounts] = useState([]);
  const [summary, setSummary] = useState(null);
  const [breakdown, setBreakdown] = useState([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState('');
  const [activeType, setActiveType] = useState('');

  // Per-customer / per-account in-flight tracking
  const [pendingUserId, setPendingUserId] = useState(null);
  const [pendingAccountNo, setPendingAccountNo] = useState(null);

  // Rows the operator has soft-deleted in this session — keeps the soft-delete
  // button suppressed even before the reload completes, and prevents the
  // "second click → 409" follow-up the user pointed out.
  const [deletedUserIds, setDeletedUserIds] = useState(() => new Set());

  // Reason modal: { kind: 'freeze' | 'close', account: AccountResponse }
  const [reasonModal, setReasonModal] = useState(null);

  const reload = useCallback(async () => {
    setLoading(true);
    try {
      const [list, sum, bd] = await Promise.all([
        accountApi.staffMyBranchAccounts({ status: statusFilter || undefined }).catch(() => []),
        accountApi.staffMyBranchSummary().catch(() => null),
        accountApi.staffMyBranchBreakdown().catch(() => []),
      ]);
      setAccounts(Array.isArray(list) ? list : []);
      setSummary(sum);
      setBreakdown(Array.isArray(bd) ? bd : []);
    } finally {
      setLoading(false);
    }
  }, [statusFilter]);

  useEffect(() => { reload(); }, [reload]);

  // De-dup accounts → customers list (used by the Customers table)
  const customers = useMemo(() => {
    const map = new Map();
    for (const a of accounts) {
      if (!a.customerId) continue;
      const existing = map.get(a.customerId);
      const balance = Number(a.balance ?? 0);
      const isClosed = a.status === 'CLOSED';
      if (existing) {
        existing.accountCount += 1;
        existing.totalBalance += balance;
        if (!isClosed) existing.openAccounts += 1;
      } else {
        map.set(a.customerId, {
          customerId:   a.customerId,
          customerName: a.customerName || '—',
          accountCount: 1,
          openAccounts: isClosed ? 0 : 1,
          totalBalance: balance,
        });
      }
    }
    return Array.from(map.values()).sort((a, b) =>
      a.customerName.localeCompare(b.customerName),
    );
  }, [accounts]);

  // Accounts table → filtered by account type chip + status select
  const filteredAccounts = useMemo(() => {
    if (!activeType) return accounts;
    return accounts.filter((a) => a.accountType === activeType);
  }, [accounts, activeType]);

  // ── Mutation handlers ─────────────────────────────────────────────────────

  const softDeleteCustomer = async (customer) => {
    if (!canMutate || !customer?.customerId) return;
    if (deletedUserIds.has(customer.customerId)) return;
    if (!confirm(
      `Soft-delete ${customer.customerName}?\n\n` +
      `This marks the customer as CLOSED and closes every account they hold ` +
      `at every branch (${customer.openAccounts} open, ${customer.accountCount} total). ` +
      `The customer record stays in the database but they cannot transact or ` +
      `apply for loans.`,
    )) return;

    setPendingUserId(customer.customerId);
    try {
      await customerApi.softDeleteByUser(customer.customerId);
      setDeletedUserIds((prev) => {
        const next = new Set(prev);
        next.add(customer.customerId);
        return next;
      });
      toast.success(`${customer.customerName} soft-deleted — accounts cascade-closed`);
      await reload();
    } catch (err) {
      toast.error(errorMessage(err, 'Could not soft-delete customer.'));
    } finally {
      setPendingUserId(null);
    }
  };

  const unfreezeAccount = async (account) => {
    if (!canMutate) return;
    if (!confirm(`Unfreeze account ${account.accountNo}?`)) return;
    setPendingAccountNo(account.accountNo);
    try {
      await accountApi.staffUnfreezeAccount(account.accountNo);
      toast.success(`Account ${account.accountNo} unfrozen`);
      await reload();
    } catch (err) {
      toast.error(errorMessage(err, 'Could not unfreeze account.'));
    } finally {
      setPendingAccountNo(null);
    }
  };

  const submitReason = async ({ reason }) => {
    if (!reasonModal) return;
    const { kind, account } = reasonModal;
    setPendingAccountNo(account.accountNo);
    try {
      if (kind === 'freeze') {
        await accountApi.staffFreezeAccount(account.accountNo, { reason });
        toast.success(`Account ${account.accountNo} frozen`);
      } else if (kind === 'close') {
        await accountApi.staffCloseAccount(account.accountNo, { reason });
        toast.success(`Account ${account.accountNo} closed`);
      }
      setReasonModal(null);
      await reload();
    } catch (err) {
      toast.error(errorMessage(err, `Could not ${kind} account.`));
    } finally {
      setPendingAccountNo(null);
    }
  };

  // ── Render ────────────────────────────────────────────────────────────────

  return (
    <>
      <Card>
        <CardHeader
          title={`Customer accounts${branchLabel ? ` — ${branchLabel}` : ''}`}
          subtitle={canMutate
            ? 'Soft-delete customers and freeze / unfreeze / close individual accounts at this branch.'
            : 'Read-only view of customer accounts at this branch.'}
          action={
            <div className="flex gap-2">
              <Button variant="ghost" size="sm" icon={RefreshCw} onClick={reload}>
                Refresh
              </Button>
            </div>
          }
          className="mb-4"
        />

        {/* KPI strip */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
          <MiniKpi label="Total accounts" value={summary?.totalAccounts ?? '—'} icon={Users} />
          <MiniKpi label="Active"         value={summary?.activeAccounts ?? '—'} icon={PlayCircle} tone="emerald" />
          <MiniKpi
            label="Frozen / closed"
            value={summary == null ? '—' : (summary.frozenAccounts + summary.closedAccounts)}
            icon={PauseCircle}
            tone="amber"
          />
          <MiniKpi
            label="Total balance"
            value={summary?.totalBalance != null ? formatINR(summary.totalBalance) : '—'}
            icon={Wallet}
            tone="brand"
          />
        </div>

        {/* Breakdown chips */}
        <div className="mt-6">
          <h3 className="text-sm font-semibold dark:text-ink-100 mb-3 uppercase tracking-wide">
            Customers by account type
          </h3>
          {loading && breakdown.length === 0 ? (
            <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-3">
              {Array.from({ length: 3 }).map((_, i) => <Skeleton key={i} className="h-24" />)}
            </div>
          ) : breakdown.length === 0 ? (
            <EmptyState
              icon={Wallet}
              title="No accounts at this branch yet"
              message="The breakdown will appear once customers open accounts here."
            />
          ) : (
            <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-3">
              <BreakdownChip
                active={activeType === ''}
                onClick={() => setActiveType('')}
                tone="bg-accent-surface dark:bg-ink-750 text-accent-ink dark:text-ink-100"
                Icon={Filter}
                label="All types"
                primary={`${breakdown.reduce((s, r) => s + r.uniqueCustomers, 0)} customers`}
                secondary={`${breakdown.reduce((s, r) => s + r.totalAccounts, 0)} accounts`}
              />
              {breakdown.map((row) => {
                const meta = ACCOUNT_TYPE_META[row.accountType] || {};
                const Icon = meta.icon || Wallet;
                return (
                  <BreakdownChip
                    key={row.accountType}
                    active={activeType === row.accountType}
                    onClick={() => setActiveType((t) => t === row.accountType ? '' : row.accountType)}
                    tone={meta.tone || 'bg-accent-surface dark:bg-ink-750 text-accent-ink dark:text-ink-100'}
                    Icon={Icon}
                    label={meta.label || row.accountType}
                    primary={`${row.uniqueCustomers} customer${row.uniqueCustomers === 1 ? '' : 's'}`}
                    secondary={`${row.totalAccounts} account${row.totalAccounts === 1 ? '' : 's'} · ${formatINR(row.totalBalance)}`}
                  />
                );
              })}
            </div>
          )}
        </div>
      </Card>

      {/* Customers table */}
      <Card className="mt-6">
        <CardHeader
          title="Customers"
          subtitle={`${customers.length} unique customer${customers.length === 1 ? '' : 's'} at this branch · soft-delete cascades to every account they hold`}
          className="mb-4"
        />
        {loading && customers.length === 0 ? (
          <div className="space-y-2">{Array.from({ length: 5 }).map((_, i) => <Skeleton key={i} className="h-12" />)}</div>
        ) : customers.length === 0 ? (
          <EmptyState
            icon={Users}
            title="No customers yet"
            message="Customers appear here once accounts are opened at this branch."
          />
        ) : (
          <div className="overflow-x-auto -mx-6">
            <table className="w-full text-sm">
              <thead className="text-left text-[11px] uppercase tracking-widest text-accent-mute dark:text-ink-400 border-b border-accent-line dark:border-ink-700">
                <tr>
                  <th className="px-6 py-3">Customer</th>
                  <th className="py-3 text-right">Accounts</th>
                  <th className="py-3 text-right">Total balance</th>
                  {canMutate && <th className="py-3 pr-6 text-right">Actions</th>}
                </tr>
              </thead>
              <tbody>
                {customers.map((c) => {
                  const isDeleted = deletedUserIds.has(c.customerId);
                  return (
                    <tr
                      key={c.customerId}
                      className={`border-b border-accent-line/60 dark:border-ink-700/60 last:border-b-0 hover:bg-accent-surface/40 dark:hover:bg-ink-750/40 ${isDeleted ? 'opacity-50' : ''}`}
                    >
                      <td className="px-6 py-3">
                        <div className="font-semibold dark:text-ink-100">{c.customerName}</div>
                        <div className="text-[11px] font-mono text-accent-mute dark:text-ink-400">#{c.customerId}</div>
                      </td>
                      <td className="py-3 text-right">
                        <span className="font-semibold dark:text-ink-100">{c.openAccounts}</span>
                        <span className="text-accent-mute dark:text-ink-400"> open · {c.accountCount} total</span>
                      </td>
                      <td className="py-3 text-right font-semibold dark:text-ink-100">
                        {formatINR(c.totalBalance)}
                      </td>
                      {canMutate && (
                        <td className="py-3 pr-6 text-right">
                          {isDeleted ? (
                            <Badge tone="warning">Soft-deleted</Badge>
                          ) : (
                            <Button
                              size="sm"
                              variant="ghost"
                              icon={Trash2}
                              loading={pendingUserId === c.customerId}
                              disabled={pendingUserId !== null}
                              onClick={() => softDeleteCustomer(c)}
                            >
                              Soft-delete
                            </Button>
                          )}
                        </td>
                      )}
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      {/* Accounts table */}
      <Card className="mt-6">
        <CardHeader
          title={`Accounts${activeType ? ` — ${ACCOUNT_TYPE_META[activeType]?.label || activeType}` : ''}`}
          subtitle={`${filteredAccounts.length} account${filteredAccounts.length === 1 ? '' : 's'}${canMutate ? ' · freeze, unfreeze or close individual accounts' : ''}`}
          action={
            <Select
              label=""
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="w-44"
            >
              <option value="">All statuses</option>
              <option value="ACTIVE">Active</option>
              <option value="FROZEN">Frozen</option>
              <option value="CLOSED">Closed</option>
            </Select>
          }
          className="mb-4"
        />
        {loading && filteredAccounts.length === 0 ? (
          <div className="space-y-2">{Array.from({ length: 5 }).map((_, i) => <Skeleton key={i} className="h-12" />)}</div>
        ) : filteredAccounts.length === 0 ? (
          <EmptyState
            icon={Wallet}
            title="No matching accounts"
            message={activeType
              ? `No ${ACCOUNT_TYPE_META[activeType]?.label || activeType} accounts match the current filters.`
              : 'No accounts match the current filters.'}
          />
        ) : (
          <div className="overflow-x-auto -mx-6">
            <table className="w-full text-sm">
              <thead className="text-left text-[11px] uppercase tracking-widest text-accent-mute dark:text-ink-400 border-b border-accent-line dark:border-ink-700">
                <tr>
                  <th className="px-6 py-3">Customer</th>
                  <th className="py-3">Account</th>
                  <th className="py-3">Type</th>
                  <th className="py-3 text-right">Balance</th>
                  <th className="py-3">Status</th>
                  <th className="py-3">Opened</th>
                  {canMutate && <th className="py-3 pr-6 text-right">Actions</th>}
                </tr>
              </thead>
              <tbody>
                {filteredAccounts.map((a) => {
                  const isPending = pendingAccountNo === a.accountNo;
                  const isClosed  = a.status === 'CLOSED';
                  const isFrozen  = a.status === 'FROZEN';
                  return (
                    <tr key={a.accountNo} className="border-b border-accent-line/60 dark:border-ink-700/60 last:border-b-0 hover:bg-accent-surface/40 dark:hover:bg-ink-750/40">
                      <td className="px-6 py-3">
                        <div className="font-semibold dark:text-ink-100">{a.customerName || '—'}</div>
                        <div className="text-[11px] font-mono text-accent-mute dark:text-ink-400">#{a.customerId || '—'}</div>
                      </td>
                      <td className="py-3 font-mono text-xs">{a.accountNo}</td>
                      <td className="py-3">
                        <Badge tone="brand">
                          {ACCOUNT_TYPE_META[a.accountType]?.label || a.accountType}
                        </Badge>
                      </td>
                      <td className="py-3 text-right font-semibold dark:text-ink-100">
                        {a.balance != null ? formatINR(a.balance) : '—'}
                      </td>
                      <td className="py-3"><StatusBadge status={a.status} /></td>
                      <td className="py-3 text-xs text-accent-slate dark:text-ink-300">
                        {a.openedAt ? formatDate(a.openedAt) : '—'}
                      </td>
                      {canMutate && (
                        <td className="py-3 pr-6 text-right space-x-1">
                          {!isClosed && !isFrozen && (
                            <Button
                              size="sm"
                              variant="ghost"
                              icon={PauseCircle}
                              loading={isPending}
                              disabled={pendingAccountNo !== null}
                              onClick={() => setReasonModal({ kind: 'freeze', account: a })}
                            >
                              Freeze
                            </Button>
                          )}
                          {isFrozen && (
                            <Button
                              size="sm"
                              variant="ghost"
                              icon={PlayCircle}
                              loading={isPending}
                              disabled={pendingAccountNo !== null}
                              onClick={() => unfreezeAccount(a)}
                            >
                              Unfreeze
                            </Button>
                          )}
                          {!isClosed && (
                            <Button
                              size="sm"
                              variant="ghost"
                              icon={XCircle}
                              loading={isPending}
                              disabled={pendingAccountNo !== null}
                              onClick={() => setReasonModal({ kind: 'close', account: a })}
                            >
                              Close
                            </Button>
                          )}
                          {isClosed && <Badge tone="neutral">Closed</Badge>}
                        </td>
                      )}
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      <ReasonModal
        open={!!reasonModal}
        kind={reasonModal?.kind}
        account={reasonModal?.account}
        submitting={pendingAccountNo !== null}
        onClose={() => setReasonModal(null)}
        onSubmit={submitReason}
      />
    </>
  );
}

// ── Helpers ───────────────────────────────────────────────────────────────────

function MiniKpi({ label, value, icon: Icon, tone = 'brand' }) {
  const tones = {
    brand:   'bg-brand-50 dark:bg-brand-900/40 text-brand-700 dark:text-brand-300',
    emerald: 'bg-emerald-50 dark:bg-emerald-900/30 text-emerald-700 dark:text-emerald-300',
    amber:   'bg-amber-50 dark:bg-amber-900/30 text-accent-warning dark:text-amber-300',
  };
  return (
    <div className="card p-4 flex items-center gap-3">
      <span className={`h-10 w-10 rounded-xl grid place-items-center shrink-0 ${tones[tone]}`}>
        <Icon size={18} />
      </span>
      <div className="min-w-0">
        <div className="text-[11px] uppercase tracking-wide text-accent-mute dark:text-ink-400 truncate">{label}</div>
        <div className="font-display font-bold text-lg dark:text-ink-100 truncate">{value}</div>
      </div>
    </div>
  );
}

function BreakdownChip({ active, onClick, tone, Icon, label, primary, secondary }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`text-left p-4 rounded-xl border transition focus-ring ${
        active
          ? 'border-brand-700 ring-2 ring-brand-200 dark:ring-brand-900 bg-white dark:bg-ink-800'
          : 'border-accent-line dark:border-ink-700 hover:border-brand-200 dark:hover:border-brand-900 bg-white dark:bg-ink-800'
      }`}
    >
      <div className="flex items-center gap-3">
        <span className={`h-9 w-9 rounded-lg grid place-items-center shrink-0 ${tone}`}>
          <Icon size={16} />
        </span>
        <div className="min-w-0">
          <div className="text-xs uppercase tracking-wide text-accent-mute dark:text-ink-400">{label}</div>
          <div className="font-semibold dark:text-ink-100 truncate">{primary}</div>
          <div className="text-xs text-accent-mute dark:text-ink-400 truncate">{secondary}</div>
        </div>
      </div>
    </button>
  );
}

// Reason modal — backend requires 5–500 chars on both freeze and close.
function ReasonModal({ open, kind, account, submitting, onClose, onSubmit }) {
  const { register, handleSubmit, reset, formState: { errors } } = useForm({
    defaultValues: { reason: '' },
  });
  useEffect(() => { if (!open) reset(); }, [open, reset]);

  if (!open || !account) return null;

  const title = kind === 'freeze' ? 'Freeze account' : 'Close account';
  const danger = kind === 'close';
  const description = kind === 'freeze'
    ? `Account ${account.accountNo} (${account.customerName || '—'}) will be marked as FROZEN. Transfers and disbursements will be refused until it is unfrozen.`
    : `Account ${account.accountNo} (${account.customerName || '—'}) will be CLOSED permanently. Balance must already be zero; closure is irreversible.`;

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={title}
      description={description}
      size="md"
      footer={
        <div className="flex justify-end gap-2">
          <Button variant="ghost" onClick={onClose} disabled={submitting}>Cancel</Button>
          <Button
            onClick={handleSubmit(onSubmit)}
            loading={submitting}
            variant={danger ? 'danger' : 'primary'}
          >
            {danger ? 'Close account' : 'Freeze account'}
          </Button>
        </div>
      }
    >
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <Textarea
          label="Reason"
          placeholder={kind === 'freeze'
            ? 'e.g. Suspicious transactions flagged by fraud team'
            : 'e.g. Customer requested closure on 2026-05-12'}
          rows={4}
          maxLength={500}
          error={errors.reason?.message}
          {...register('reason', {
            required: 'Reason is required',
            minLength: { value: 5,   message: 'At least 5 characters' },
            maxLength: { value: 500, message: 'At most 500 characters' },
          })}
        />
      </form>
    </Modal>
  );
}
