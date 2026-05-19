import { Link } from 'react-router-dom';
import { Plus, Wallet, ArrowRight, Receipt } from 'lucide-react';
import { PageHeader } from '../../components/layout/PageHeader';
import { Card, CardHeader } from '../../components/common/Card';
import { Skeleton } from '../../components/common/Skeleton';
import { EmptyState } from '../../components/common/EmptyState';
import { StatusBadge } from '../../components/common/Badge';
import { useAsync } from '../../hooks/useAsync';
import { accountApi } from '../../api/account';
import { formatINR, formatDate, maskAccountNo } from '../../utils/format';

export default function Accounts() {
  const { data: accounts, loading } = useAsync(
    () => accountApi.myAccounts().catch(() => []),
    []
  );

  const { data: applications, loading: appsLoading } = useAsync(
    () => accountApi.myApplications().catch(() => []),
    []
  );

  return (
    <div className="space-y-8">
      <PageHeader
        breadcrumb="Banking"
        title="My Accounts"
        subtitle="Manage your savings, current and term deposit accounts in one place."
        actions={
          <Link to="/app/accounts/apply" className="btn-primary"><Plus size={14} /> Open new account</Link>
        }
      />

      {loading ? (
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-5">
          {Array.from({ length: 3 }).map((_, i) => <Skeleton key={i} className="h-48 rounded-xl2" />)}
        </div>
      ) : (accounts || []).length === 0 ? (
        <Card className="p-10">
          <EmptyState
            icon={Wallet}
            title="No active accounts yet"
            message="Once your account application is approved, it will appear here."
            action={<Link to="/app/accounts/apply" className="btn-primary"><Plus size={14} /> Apply now</Link>}
          />
        </Card>
      ) : (
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-5">
          {accounts.map((account) => (
            <Link
              key={account.accountNo}
              to={`/app/accounts/${account.accountNo}`}
              className="card-hover p-6 block group"
            >
              <div className="flex items-start justify-between">
                <div>
                  <p className="text-xs uppercase tracking-widest text-accent-mute dark:text-ink-400">{account.accountType}</p>
                  <p className="mt-1 font-mono text-sm text-accent-slate dark:text-ink-300">{maskAccountNo(account.accountNo)}</p>
                </div>
                <StatusBadge status={account.status} />
              </div>
              <div className="mt-8">
                <p className="text-xs text-accent-mute dark:text-ink-400">Available balance</p>
                <p className="mt-1 font-display text-2xl font-extrabold">{formatINR(account.balance)}</p>
              </div>
              <div className="mt-6 flex items-center justify-between text-sm">
                <span className="text-accent-mute dark:text-ink-400">{account.branchCode}</span>
                <span className="inline-flex items-center gap-1 text-brand-700 font-semibold group-hover:gap-2 transition-all">
                  Manage <ArrowRight size={14} />
                </span>
              </div>
            </Link>
          ))}
        </div>
      )}

      <Card>
        <CardHeader
          title="Account applications"
          subtitle="Track the status of your account applications"
          className="mb-4"
        />
        {appsLoading ? (
          <div className="space-y-2">
            {Array.from({ length: 3 }).map((_, i) => <Skeleton key={i} className="h-12" />)}
          </div>
        ) : (applications || []).length === 0 ? (
          <EmptyState
            icon={Receipt}
            title="No applications yet"
            message="When you apply for a new account, the status will be tracked here."
          />
        ) : (
          <ApplicationsTable rows={applications} />
        )}
      </Card>
    </div>
  );
}

function ApplicationsTable({ rows }) {
  return (
    <div className="overflow-x-auto -mx-6">
      <table className="w-full text-sm">
        <thead>
          <tr className="text-left text-[11px] uppercase tracking-widest text-accent-mute dark:text-ink-400 border-b border-accent-line dark:border-ink-700">
            <th className="px-6 py-3 font-medium">Reference</th>
            <th className="py-3 font-medium">Type</th>
            <th className="py-3 font-medium">Branch</th>
            <th className="py-3 font-medium">Initial deposit</th>
            <th className="py-3 font-medium">Applied on</th>
            <th className="py-3 font-medium">Status</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((r) => (
            <tr key={r.applicationRef || r.id} className="border-b border-accent-line/60 last:border-b-0 hover:bg-accent-surface/40">
              <td className="px-6 py-3.5 font-mono text-xs">{r.applicationRef}</td>
              <td className="py-3.5">{r.accountType}</td>
              <td className="py-3.5">{r.branchCode}</td>
              <td className="py-3.5">{formatINR(r.initialDeposit)}</td>
              <td className="py-3.5 text-accent-mute dark:text-ink-400">{formatDate(r.createdAt)}</td>
              <td className="py-3.5"><StatusBadge status={r.status} /></td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
