import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Hourglass, RefreshCw, CheckCircle2, XCircle, ArrowLeft } from 'lucide-react';
import toast from 'react-hot-toast';
import { PageHeader } from '../../components/layout/PageHeader';
import { Card, CardHeader } from '../../components/common/Card';
import { Skeleton } from '../../components/common/Skeleton';
import { EmptyState } from '../../components/common/EmptyState';
import { Badge } from '../../components/common/Badge';
import { Button } from '../../components/common/Button';
import { transferApi } from '../../api/transfer';
import { errorMessage } from '../../api/client';
import { formatINR, formatDateTime } from '../../utils/format';

const TONE = {
  PENDING_APPROVAL: 'warning',
  APPROVED:         'success',
  REJECTED:         'danger',
  CANCELLED:        'neutral',
};

const ICON = {
  PENDING_APPROVAL: Hourglass,
  APPROVED:         CheckCircle2,
  REJECTED:         XCircle,
  CANCELLED:        XCircle,
};

export default function MyPendingTransfers() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);

  const reload = async () => {
    setLoading(true);
    try {
      const rows = await transferApi.myPending();
      setItems(Array.isArray(rows) ? rows : []);
    } catch (err) {
      toast.error(errorMessage(err, 'Could not load your pending transfers.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { reload(); }, []);

  return (
    <div className="space-y-8 max-w-4xl">
      <PageHeader
        breadcrumb="Banking · Transfers"
        title="My pending transfers"
        subtitle="High-value transfers awaiting CSR approval at your branch."
        actions={
          <>
            <Link to="/app/transfer" className="btn-secondary"><ArrowLeft size={14} /> New transfer</Link>
            <Button variant="secondary" icon={RefreshCw} onClick={reload}>Refresh</Button>
          </>
        }
      />

      <Card>
        {loading ? (
          <div className="space-y-2">
            {Array.from({ length: 3 }).map((_, i) => <Skeleton key={i} className="h-16" />)}
          </div>
        ) : items.length === 0 ? (
          <EmptyState
            icon={Hourglass}
            title="No pending high-value transfers"
            message="When you initiate a transfer above ₹1,00,000, it appears here while the branch reviews it."
          />
        ) : (
          <ul className="divide-y divide-accent-line dark:divide-ink-700">
            {items.map((t) => {
              const Icon = ICON[t.status] || Hourglass;
              return (
                <li key={t.reference} className="py-4 flex items-center gap-4">
                  <span className={`h-10 w-10 rounded-full grid place-items-center shrink-0 ${
                    t.status === 'APPROVED'
                      ? 'bg-green-50 text-accent-success dark:bg-green-900/30 dark:text-green-300'
                      : t.status === 'REJECTED'
                        ? 'bg-red-50 text-accent-danger dark:bg-red-900/30 dark:text-red-300'
                        : 'bg-amber-50 text-accent-warning dark:bg-amber-900/30 dark:text-amber-300'
                  }`}>
                    <Icon size={16} />
                  </span>
                  <div className="flex-1 min-w-0">
                    <p className="font-semibold text-sm dark:text-ink-100">
                      {formatINR(t.amount)} → {t.receiverAccountNo}
                    </p>
                    <p className="text-xs text-accent-mute dark:text-ink-400 font-mono">
                      {t.reference} · {formatDateTime(t.createdAt)}
                    </p>
                    {t.status === 'REJECTED' && t.rejectionReason && (
                      <p className="text-xs text-accent-danger dark:text-red-300 mt-1">
                        Reason: {t.rejectionReason}
                      </p>
                    )}
                    {t.status === 'APPROVED' && t.generatedTransactionRef && (
                      <p className="text-xs text-accent-success dark:text-green-300 mt-1 font-mono">
                        Txn: {t.generatedTransactionRef}
                      </p>
                    )}
                  </div>
                  <Badge tone={TONE[t.status] || 'neutral'} dot>{t.status.replace(/_/g, ' ')}</Badge>
                </li>
              );
            })}
          </ul>
        )}
      </Card>
    </div>
  );
}
