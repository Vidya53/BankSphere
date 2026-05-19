import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Copy, Send, Receipt, Download, Loader2 } from 'lucide-react';
import toast from 'react-hot-toast';
import { PageHeader } from '../../components/layout/PageHeader';
import { Card, CardHeader } from '../../components/common/Card';
import { Skeleton } from '../../components/common/Skeleton';
import { StatusBadge } from '../../components/common/Badge';
import { EmptyState } from '../../components/common/EmptyState';
import { TransactionRow } from '../../components/dashboard/TransactionRow';
import { useAsync } from '../../hooks/useAsync';
import { accountApi } from '../../api/account';
import { transactionApi } from '../../api/transaction';
import { formatINR, formatDate } from '../../utils/format';
import { errorMessage } from '../../api/client';

export default function AccountDetail() {
  const { accountNo } = useParams();
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [downloading, setDownloading] = useState(false);

  const { data: account, loading } = useAsync(
    () => accountApi.getAccount(accountNo).catch(() => null),
    [accountNo]
  );

  const { data: txPage, loading: txLoading } = useAsync(
    () => transactionApi.getByAccount(accountNo, { page, size: 10 }).catch(() => null),
    [accountNo, page]
  );

  const txs = txPage?.content || [];
  const totalPages = txPage?.totalPages || 1;

  const copy = (v) => {
    navigator.clipboard?.writeText(v);
    toast.success('Copied');
  };

  const downloadStatement = async () => {
    if (downloading) return;
    setDownloading(true);
    try {
      const today = new Date();
      const fromDate = new Date(today.getFullYear(), today.getMonth() - 11, 1);
      const allTxs = await fetchAllTransactions(accountNo);

      const csv = buildStatementCsv(account, allTxs);
      triggerDownload(
        csv,
        `statement-${accountNo}-${toIsoDate(fromDate)}-to-${toIsoDate(today)}.csv`,
      );

      // Fire-and-forget email notification — never block the download on it.
      accountApi
        .notifyStatementDownloaded(accountNo, {
          from: toIsoDate(fromDate),
          to: toIsoDate(today),
        })
        .then(() => toast.success('Statement downloaded — confirmation email sent'))
        .catch(() => toast.success('Statement downloaded'));
    } catch (err) {
      toast.error(errorMessage(err, 'Could not download statement'));
    } finally {
      setDownloading(false);
    }
  };

  if (loading) return <div className="space-y-6"><Skeleton className="h-64 rounded-2xl" /><Skeleton className="h-64 rounded-2xl" /></div>;
  if (!account) {
    return (
      <Card className="p-10">
        <EmptyState
          title="Account not found"
          message="The account you're looking for doesn't exist or you don't have access to it."
          action={<Link to="/app/accounts" className="btn-primary"><ArrowLeft size={14} /> Back to accounts</Link>}
        />
      </Card>
    );
  }

  return (
    <div className="space-y-8">
      <PageHeader
        breadcrumb={<button onClick={() => navigate(-1)} className="inline-flex items-center gap-1 hover:text-brand-700"><ArrowLeft size={12} /> Accounts</button>}
        title={`${account.accountType} Account`}
        subtitle={`Account ${account.accountNo}`}
        actions={
          <>
            <Link to={`/app/transfer?from=${account.accountNo}`} className="btn-secondary"><Send size={14} /> Send money</Link>
            <button
              type="button"
              className="btn-primary disabled:opacity-60"
              onClick={downloadStatement}
              disabled={downloading}
            >
              {downloading ? <Loader2 size={14} className="animate-spin" /> : <Download size={14} />}
              {downloading ? 'Preparing…' : 'Statement'}
            </button>
          </>
        }
      />

      {/* Account hero */}
      <div className="relative overflow-hidden rounded-2xl bg-gradient-to-br from-brand-700 to-brand-900 text-white shadow-elevated p-8">
        <div className="absolute -right-16 -top-16 h-48 w-48 rounded-full bg-accent-gold/20 blur-3xl" />
        <div className="relative grid md:grid-cols-3 gap-6">
          <div className="md:col-span-2">
            <p className="text-xs uppercase tracking-widest opacity-70">Available balance</p>
            <p className="mt-1 font-display text-5xl font-extrabold">{formatINR(account.balance)}</p>
            <p className="mt-2 text-sm opacity-80">Minimum balance: {formatINR(account.minimumBalance)}</p>
          </div>
          <div className="md:text-right">
            <StatusBadge status={account.status} className="bg-white/15 text-white border border-white/20" />
            <p className="mt-3 text-sm opacity-80">IFSC: {account.ifscCode || '—'}</p>
            <p className="text-sm opacity-80">Branch: {account.branchCode}</p>
          </div>
        </div>
      </div>

      {/* Detail grid */}
      <div className="grid lg:grid-cols-3 gap-6">
        <Card>
          <CardHeader title="Account info" className="mb-4" />
          <dl className="space-y-3 text-sm">
            <Field label="Account number" value={account.accountNo} mono copyable copy={copy} />
            <Field label="Account type" value={account.accountType} />
            <Field label="Currency" value={account.currency || 'INR'} />
            <Field label="Opened on" value={formatDate(account.createdAt)} />
            <Field label="IFSC" value={account.ifscCode || '—'} mono />
          </dl>
        </Card>
        <Card>
          <CardHeader title="Limits" className="mb-4" />
          <dl className="space-y-3 text-sm">
            <Field label="Daily transfer limit" value={formatINR(account.dailyTransferLimit)} />
            <Field label="Daily withdrawal limit" value={formatINR(account.dailyWithdrawalLimit)} />
            <Field label="Transactional" value={account.isTransactional ? 'Yes' : 'No'} />
          </dl>
        </Card>
        <Card>
          <CardHeader title="Nominee" className="mb-4" />
          {account.nomineeName ? (
            <dl className="space-y-3 text-sm">
              <Field label="Name" value={account.nomineeName} />
              <Field label="Relation" value={account.nomineeRelation || '—'} />
              <Field label="Phone" value={account.nomineePhone || '—'} />
            </dl>
          ) : (
            <p className="text-sm text-accent-mute dark:text-ink-400">No nominee added yet.</p>
          )}
        </Card>
      </div>

      {/* Transactions */}
      <Card>
        <CardHeader title="Transaction history" subtitle={`${txPage?.totalElements ?? 0} transactions`} className="mb-2" />
        {txLoading ? (
          <div className="space-y-2 mt-4">
            {Array.from({ length: 8 }).map((_, i) => <Skeleton key={i} className="h-12" />)}
          </div>
        ) : txs.length === 0 ? (
          <EmptyState icon={Receipt} title="No transactions yet" message="Activity on this account will appear here." />
        ) : (
          <>
            <div className="mt-2 divide-y divide-accent-line/70">
              {txs.map((tx) => (
                <TransactionRow key={tx.transactionId || tx.referenceNumber} tx={tx} accountId={accountNo} />
              ))}
            </div>
            {totalPages > 1 && (
              <div className="mt-4 flex items-center justify-between text-sm">
                <span className="text-accent-mute dark:text-ink-400">Page {page + 1} of {totalPages}</span>
                <div className="flex gap-2">
                  <button className="btn-secondary" disabled={page === 0} onClick={() => setPage((p) => Math.max(0, p - 1))}>Previous</button>
                  <button className="btn-secondary" disabled={page >= totalPages - 1} onClick={() => setPage((p) => p + 1)}>Next</button>
                </div>
              </div>
            )}
          </>
        )}
      </Card>
    </div>
  );
}

function Field({ label, value, mono, copyable, copy }) {
  return (
    <div className="flex items-start justify-between gap-3">
      <dt className="text-accent-mute dark:text-ink-400">{label}</dt>
      <dd className={`text-right font-medium text-accent-ink dark:text-ink-100 flex items-center gap-2 ${mono ? 'font-mono text-xs' : ''}`}>
        {value}
        {copyable && <button onClick={() => copy(value)} className="text-accent-mute dark:text-ink-400 hover:text-accent-ink dark:text-ink-100" aria-label="Copy"><Copy size={12} /></button>}
      </dd>
    </div>
  );
}

// Walks paginated /transactions/account/{id} until we've pulled every transaction
// or hit the safety cap. Backend caps page size at 100, so MAX_PAGES * 100 is the
// effective row ceiling for a single statement.
async function fetchAllTransactions(accountNo) {
  const PAGE_SIZE = 100;
  const MAX_PAGES = 50;
  const all = [];
  for (let p = 0; p < MAX_PAGES; p += 1) {
    const res = await transactionApi.getByAccount(accountNo, { page: p, size: PAGE_SIZE });
    const rows = res?.content || [];
    all.push(...rows);
    if (p + 1 >= (res?.totalPages || 1)) break;
  }
  return all;
}

function buildStatementCsv(account, txs) {
  const header = [
    'Reference', 'Date', 'Type', 'Description', 'Channel',
    'Direction', 'Amount', 'Currency', 'Status', 'Counterparty',
  ];
  const rows = txs.map((tx) => {
    const credit =
      tx.transactionType === 'DEPOSIT' ||
      tx.transactionType === 'INTEREST' ||
      tx.transactionType === 'REVERSAL' ||
      tx.transactionType === 'REFUND' ||
      (tx.transactionType === 'TRANSFER' && tx.receiverAccountId === account?.accountNo);
    const counterparty = tx.transactionType === 'TRANSFER'
      ? (credit ? tx.senderAccountId : tx.receiverAccountId) || ''
      : '';
    return [
      tx.referenceNumber || tx.transactionId || '',
      tx.createdAt || '',
      tx.transactionType || '',
      tx.description || '',
      tx.channel || '',
      credit ? 'CREDIT' : 'DEBIT',
      tx.amount ?? '',
      tx.currency || 'INR',
      tx.status || '',
      counterparty,
    ];
  });

  const banner = [
    [`Statement for ${account?.accountType || ''} account ${account?.accountNo || ''}`],
    [`Account holder: ${account?.customerName || ''}`],
    [`IFSC: ${account?.ifscCode || ''}`],
    [`Generated: ${new Date().toISOString()}`],
    [],
  ];

  return [...banner, header, ...rows]
    .map((row) => row.map(csvEscape).join(','))
    .join('\r\n');
}

function csvEscape(value) {
  if (value === null || value === undefined) return '';
  const s = String(value);
  if (/[",\r\n]/.test(s)) return `"${s.replace(/"/g, '""')}"`;
  return s;
}

function triggerDownload(csv, filename) {
  const blob = new Blob([`﻿${csv}`], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

function toIsoDate(d) {
  return d.toISOString().slice(0, 10);
}
