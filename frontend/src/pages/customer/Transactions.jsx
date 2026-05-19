import { useEffect, useMemo, useState } from 'react';
import { Search, Receipt, Filter } from 'lucide-react';
import { PageHeader } from '../../components/layout/PageHeader';
import { Card, CardHeader } from '../../components/common/Card';
import { Input, Select } from '../../components/common/Input';
import { Skeleton } from '../../components/common/Skeleton';
import { EmptyState } from '../../components/common/EmptyState';
import { TransactionRow } from '../../components/dashboard/TransactionRow';
import { useAsync } from '../../hooks/useAsync';
import { accountApi } from '../../api/account';
import { transactionApi } from '../../api/transaction';

const TYPE_OPTIONS = ['ALL', 'DEPOSIT', 'WITHDRAWAL', 'TRANSFER', 'PAYMENT', 'INTEREST', 'REVERSAL', 'REFUND'];
const STATUS_OPTIONS = ['ALL', 'PENDING', 'PROCESSING', 'SUCCESS', 'FAILED', 'CANCELLED', 'REVERSED'];

const PAGE_SIZE = 25;
// When "All accounts" is selected we pull this many rows per account before
// merging — comfortably more than what one customer typically has.
const PER_ACCOUNT_FETCH_LIMIT = 200;

export default function Transactions() {
  const { data: accounts } = useAsync(() => accountApi.myAccounts().catch(() => []), []);
  const [accountNo, setAccountNo] = useState('');
  const [page, setPage] = useState(0);
  const [type, setType] = useState('ALL');
  const [status, setStatus] = useState('ALL');
  const [query, setQuery] = useState('');

  // Default to a unified "All accounts" view so transfers, deposits,
  // withdrawals, EMI payments, loan disbursements and prepayments all show
  // up without the customer having to flip accounts.
  useEffect(() => {
    if (!accountNo && accounts) {
      setAccountNo(accounts.length > 0 ? 'ALL' : '');
    }
  }, [accounts, accountNo]);

  const { data: txPage, loading } = useAsync(
    async () => {
      if (!accountNo) return null;

      // Single account: backend handles pagination.
      if (accountNo !== 'ALL') {
        return transactionApi.getByAccount(accountNo, { page, size: PAGE_SIZE })
          .catch(() => null);
      }

      // All accounts: fan out, merge, sort newest-first, paginate client-side.
      if (!accounts || accounts.length === 0) return null;
      const pages = await Promise.all(
        accounts.map((a) =>
          transactionApi.getByAccount(a.accountNo, { page: 0, size: PER_ACCOUNT_FETCH_LIMIT })
            .catch(() => null)
        )
      );
      // Self-transfers (between two of the customer's own accounts) come back
      // twice — once per fan-out call. Dedupe on transactionId / reference so
      // the row appears exactly once in the combined view.
      const seen = new Set();
      const all = [];
      for (const p of pages.filter(Boolean)) {
        for (const tx of (p.content || [])) {
          const id = tx.transactionId || tx.referenceNumber;
          if (id && seen.has(id)) continue;
          if (id) seen.add(id);
          all.push(tx);
        }
      }
      all.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
      const totalElements = all.length;
      const totalPages = Math.max(1, Math.ceil(totalElements / PAGE_SIZE));
      const start = page * PAGE_SIZE;
      return {
        content: all.slice(start, start + PAGE_SIZE),
        totalElements,
        totalPages,
      };
    },
    [accountNo, page, accounts]
  );

  // List of every account number the customer owns — passed to TransactionRow
  // (in "All accounts" mode) so it can decide credit vs debit by checking
  // whether any of them is the receiver. In single-account mode we just pass
  // that one account.
  const myAccountNos = (accounts || []).map((a) => a.accountNo);

  const rows = useMemo(() => {
    const all = txPage?.content || [];
    return all.filter((tx) => {
      if (type !== 'ALL' && tx.transactionType !== type) return false;
      if (status !== 'ALL' && tx.status !== status) return false;
      if (query) {
        const q = query.toLowerCase();
        return (
          tx.referenceNumber?.toLowerCase().includes(q) ||
          tx.description?.toLowerCase().includes(q) ||
          tx.senderAccountId?.toLowerCase().includes(q) ||
          tx.receiverAccountId?.toLowerCase().includes(q)
        );
      }
      return true;
    });
  }, [txPage, type, status, query]);

  const totalPages = txPage?.totalPages || 1;

  return (
    <div className="space-y-8">
      <PageHeader
        breadcrumb="Banking"
        title="Transactions"
        subtitle="Search, filter and export your transaction history."
      />

      <Card>
        <div className="flex flex-wrap items-center gap-3">
          <Select className="min-w-[260px]" value={accountNo} onChange={(e) => { setAccountNo(e.target.value); setPage(0); }} label="Account">
            <option value="">Select an account</option>
            <option value="ALL">All my accounts (combined)</option>
            {(accounts || []).map((a) => (
              <option key={a.accountNo} value={a.accountNo}>{a.accountType} · {a.accountNo}</option>
            ))}
          </Select>
          <Select className="min-w-[160px]" value={type} onChange={(e) => setType(e.target.value)} label="Type">
            {TYPE_OPTIONS.map((t) => <option key={t} value={t}>{t}</option>)}
          </Select>
          <Select className="min-w-[160px]" value={status} onChange={(e) => setStatus(e.target.value)} label="Status">
            {STATUS_OPTIONS.map((t) => <option key={t} value={t}>{t}</option>)}
          </Select>
          <Input
            className="flex-1 min-w-[220px]"
            label="Search"
            leftIcon={Search}
            placeholder="Reference, description or counterparty"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
        </div>
      </Card>

      <Card>
        <CardHeader
          title="History"
          subtitle={`${txPage?.totalElements ?? 0} total · showing ${rows.length}`}
          action={
            <button className="btn-ghost"><Filter size={14} /> More filters</button>
          }
          className="mb-2"
        />
        {!accountNo ? (
          <EmptyState icon={Receipt} title="Pick an account" message="Choose an account above to view its transactions." />
        ) : loading ? (
          <div className="space-y-2 mt-4">
            {Array.from({ length: 8 }).map((_, i) => <Skeleton key={i} className="h-12" />)}
          </div>
        ) : rows.length === 0 ? (
          <EmptyState icon={Receipt} title="No matching transactions" message="Try clearing some filters." />
        ) : (
          <>
            <div className="mt-2 divide-y divide-accent-line/70">
              {rows.map((tx) => (
                <TransactionRow
                  key={tx.transactionId || tx.referenceNumber}
                  tx={tx}
                  myAccountNos={accountNo === 'ALL' ? myAccountNos : [accountNo]}
                />
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
