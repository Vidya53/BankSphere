import { useState } from 'react';
import { useForm } from 'react-hook-form';
import {
  ArrowDownLeft, ArrowUpRight, Search, Banknote,
  AlertTriangle, CheckCircle2, RefreshCw, Wallet,
} from 'lucide-react';
import toast from 'react-hot-toast';
import { PageHeader } from '../../components/layout/PageHeader';
import { Card, CardHeader } from '../../components/common/Card';
import { Input, Textarea } from '../../components/common/Input';
import { Button } from '../../components/common/Button';
import { Badge, StatusBadge } from '../../components/common/Badge';
import { useAuth } from '../../context/AuthContext';
import { accountApi } from '../../api/account';
import { errorMessage } from '../../api/client';
import { formatINR } from '../../utils/format';

// Counter operations always carry CASH as the channel — the customer is
// physically at the branch with an ID. The dropdown was removed so the CSR
// can't pick anything else by mistake.
const COUNTER_CHANNEL = 'CASH';

export default function CashCounter() {
  const { user } = useAuth();
  const [mode, setMode]     = useState('DEPOSIT');  // 'DEPOSIT' | 'WITHDRAWAL'
  const [account, setAccount] = useState(null);
  const [lookupBusy, setLookupBusy] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [recent, setRecent] = useState([]);

  const { register, handleSubmit, watch, reset, formState: { errors } } = useForm({
    defaultValues: { accountNo: '', amount: '', description: '' },
  });

  const lookup = async () => {
    const accNo = watch('accountNo')?.trim();
    if (!accNo) { toast.error('Enter an account number first.'); return; }
    setLookupBusy(true);
    try {
      const a = await accountApi.getAccount(accNo);
      if (a?.status !== 'ACTIVE') {
        toast.error(`Account is ${a?.status || 'inactive'}. Counter operations require ACTIVE status.`);
      }
      if (user?.branchCode && a?.branchCode && user.branchCode !== a.branchCode && user.role !== 'ADMIN') {
        toast.error(`Account belongs to branch ${a.branchCode}, you are at ${user.branchCode}.`);
      }
      setAccount(a);
    } catch (err) {
      setAccount(null);
      toast.error(errorMessage(err, 'Account not found.'));
    } finally {
      setLookupBusy(false);
    }
  };

  const onSubmit = async (values) => {
    if (!account) {
      toast.error('Look up the account first to confirm details.');
      return;
    }
    if (account.status !== 'ACTIVE') {
      toast.error(`Account is ${account.status}. Cannot perform counter operations.`);
      return;
    }
    setSubmitting(true);
    try {
      const payload = {
        accountNo:   values.accountNo.trim().toUpperCase(),
        amount:      Number(values.amount),
        channel:     COUNTER_CHANNEL,
        description: values.description || undefined,
      };
      const fn = mode === 'DEPOSIT' ? accountApi.cashDeposit : accountApi.cashWithdrawal;
      const res = await fn(payload);
      toast.success(`${mode === 'DEPOSIT' ? 'Deposited' : 'Withdrew'} ${formatINR(payload.amount)}. New balance: ${formatINR(res.balance)}`);

      // Update the displayed balance + log this op in the recent list
      setAccount((a) => a ? { ...a, balance: res.balance } : a);
      setRecent((r) => [{
        type:      mode,
        amount:    payload.amount,
        accountNo: payload.accountNo,
        reference: res.idempotencyKey,
        balance:   res.balance,
        at:        new Date().toISOString(),
      }, ...r].slice(0, 10));
      reset({ accountNo: payload.accountNo, amount: '', description: '' });
    } catch (err) {
      toast.error(errorMessage(err, `${mode} failed.`));
    } finally {
      setSubmitting(false);
    }
  };

  const amount = Number(watch('amount') || 0);
  const exceedsAvailable =
    mode === 'WITHDRAWAL' && account && amount > Number(account.balance || 0) - Number(account.minimumBalance || 0);

  return (
    <div className="space-y-8">
      <PageHeader
        breadcrumb={`Staff · CSR · Counter${user?.branchCode ? ' · ' + user.branchCode : ''}`}
        title="Cash counter"
        subtitle="Counter deposits and withdrawals for customers in your branch. No PIN required — the customer has presented ID at the branch."
      />

      <div className="card p-1.5 inline-flex flex-wrap gap-1">
        {[
          { key: 'DEPOSIT',    label: 'Deposit',    icon: ArrowDownLeft },
          { key: 'WITHDRAWAL', label: 'Withdrawal', icon: ArrowUpRight },
        ].map(({ key, label, icon: Icon }) => (
          <button
            key={key}
            onClick={() => setMode(key)}
            className={`inline-flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-semibold transition ${
              mode === key
                ? key === 'DEPOSIT'
                  ? 'bg-accent-success text-white dark:bg-green-600'
                  : 'bg-accent-danger text-white dark:bg-red-600'
                : 'text-accent-slate dark:text-ink-300 hover:bg-accent-surface dark:hover:bg-ink-750'
            }`}
          >
            <Icon size={14} /> {label}
          </button>
        ))}
      </div>

      <div className="grid lg:grid-cols-[1.4fr_1fr] gap-6">
        <Card>
          <CardHeader
            title={mode === 'DEPOSIT' ? 'Cash deposit' : 'Cash withdrawal'}
            subtitle={mode === 'DEPOSIT'
              ? 'Credit the customer\'s active account at the counter'
              : 'Debit the customer\'s active account at the counter — respects minimum balance'}
            className="mb-6"
          />

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
            <div className="flex gap-3 items-end">
              <Input
                label="Account number"
                placeholder="SAVxxxxxxxxxxxxxx"
                error={errors.accountNo?.message}
                className="flex-1"
                {...register('accountNo', {
                  required: 'Required',
                  pattern: { value: /^[A-Za-z0-9]{5,20}$/, message: 'Invalid account number' },
                  onChange: () => setAccount(null),
                })}
              />
              <Button type="button" variant="secondary" icon={Search} loading={lookupBusy} onClick={lookup}>
                Look up
              </Button>
            </div>

            {account && <AccountCard account={account} />}

            <Input
              label="Amount (₹)"
              type="number"
              step="0.01"
              placeholder="0.00"
              error={errors.amount?.message}
              {...register('amount', {
                required: 'Required',
                min: { value: 1, message: 'Minimum ₹1' },
              })}
            />

            <Textarea
              label="Note (optional)"
              placeholder="e.g. Salary cash deposit"
              maxLength={255}
              {...register('description')}
            />

            {exceedsAvailable && (
              <div className="rounded-xl bg-red-50 dark:bg-red-900/20 text-red-900 dark:text-red-200 p-4 flex gap-3">
                <AlertTriangle size={18} className="shrink-0 mt-0.5" />
                <p className="text-sm">
                  Withdrawal exceeds the available balance after minimum-balance.
                  Available: {formatINR(Number(account.balance || 0) - Number(account.minimumBalance || 0))}.
                </p>
              </div>
            )}

            <div className="flex items-center justify-end pt-4 border-t border-accent-line dark:border-ink-700">
              <Button
                type="submit"
                icon={mode === 'DEPOSIT' ? ArrowDownLeft : ArrowUpRight}
                loading={submitting}
                disabled={!account || account.status !== 'ACTIVE' || exceedsAvailable}
              >
                Confirm {mode === 'DEPOSIT' ? 'deposit' : 'withdrawal'}
              </Button>
            </div>
          </form>
        </Card>

        <Card>
          <CardHeader title="Today at this terminal" subtitle="Most recent counter operations" className="mb-4" />
          {recent.length === 0 ? (
            <p className="text-sm text-accent-mute dark:text-ink-400 text-center py-8">
              No counter operations yet in this session.
            </p>
          ) : (
            <ul className="divide-y divide-accent-line dark:divide-ink-700">
              {recent.map((r) => (
                <li key={r.reference} className="py-3 flex items-center gap-3">
                  <span className={`h-9 w-9 rounded-full grid place-items-center shrink-0 ${
                    r.type === 'DEPOSIT'
                      ? 'bg-green-50 text-accent-success dark:bg-green-900/30 dark:text-green-300'
                      : 'bg-red-50 text-accent-danger dark:bg-red-900/30 dark:text-red-300'
                  }`}>
                    {r.type === 'DEPOSIT' ? <ArrowDownLeft size={14} /> : <ArrowUpRight size={14} />}
                  </span>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-semibold dark:text-ink-100">
                      {r.type === 'DEPOSIT' ? '+' : '−'} {formatINR(r.amount)}
                    </p>
                    <p className="text-xs text-accent-mute dark:text-ink-400 font-mono">
                      {r.accountNo} · {r.reference}
                    </p>
                  </div>
                  <span className="text-xs text-accent-mute dark:text-ink-400">Bal {formatINR(r.balance)}</span>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>
    </div>
  );
}

function AccountCard({ account }) {
  return (
    <div className="rounded-xl border border-brand-100 dark:border-brand-900/40 bg-brand-50/60 dark:bg-brand-900/20 p-4">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-xs uppercase tracking-widest text-accent-mute dark:text-ink-400">Account holder</p>
          <p className="mt-0.5 font-semibold text-accent-ink dark:text-ink-100">{account.customerName}</p>
          <p className="text-xs text-accent-mute dark:text-ink-400">
            {account.accountType} · {account.branchCode} · {account.ifscCode}
          </p>
        </div>
        <div className="text-right">
          <p className="text-xs uppercase tracking-widest text-accent-mute dark:text-ink-400">Current balance</p>
          <p className="mt-0.5 font-display text-xl font-extrabold text-brand-700 dark:text-brand-300">
            {formatINR(account.balance)}
          </p>
          <p className="text-xs text-accent-mute dark:text-ink-400">Min: {formatINR(account.minimumBalance)}</p>
        </div>
      </div>
      <div className="mt-3 flex items-center gap-2 flex-wrap">
        <StatusBadge status={account.status} />
        {account.isTransactional ? (
          <Badge tone="success">Transactional</Badge>
        ) : (
          <Badge tone="danger">Not transactional</Badge>
        )}
      </div>
    </div>
  );
}
