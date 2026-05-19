import { useEffect, useState } from 'react';
import {
  Hourglass, CheckCircle2, XCircle, RefreshCw, Search,
  ArrowRight, AlertTriangle, Building2,
} from 'lucide-react';
import toast from 'react-hot-toast';
import { PageHeader } from '../../components/layout/PageHeader';
import { Card, CardHeader } from '../../components/common/Card';
import { Skeleton } from '../../components/common/Skeleton';
import { EmptyState } from '../../components/common/EmptyState';
import { Badge } from '../../components/common/Badge';
import { Button } from '../../components/common/Button';
import { Input, Textarea } from '../../components/common/Input';
import { Modal } from '../../components/common/Modal';
import { KpiCard } from '../../components/dashboard/KpiCard';
import { useAuth } from '../../context/AuthContext';
import { pendingTransferStaffApi } from '../../api/transfer';
import { errorMessage } from '../../api/client';
import { formatINR, formatDateTime } from '../../utils/format';

export default function PendingTransfersReview() {
  const { user } = useAuth();
  const branchCode = user?.branchCode || null;

  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [query, setQuery] = useState('');
  const [busy, setBusy] = useState(null);
  const [rejectFor, setRejectFor] = useState(null);

  const reload = async () => {
    setLoading(true);
    try {
      const rows = await pendingTransferStaffApi.list();
      setItems(Array.isArray(rows) ? rows : []);
    } catch (err) {
      toast.error(errorMessage(err, 'Could not load pending transfers.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { reload(); }, []);

  const approve = async (ref) => {
    setBusy(ref);
    try {
      const res = await pendingTransferStaffApi.approve(ref);
      toast.success(`Transfer approved — funds moved (${res?.idempotencyKey || ''})`);
      await reload();
    } catch (err) {
      toast.error(errorMessage(err, 'Could not approve transfer.'));
    } finally {
      setBusy(null);
    }
  };

  const reject = async (ref, reason) => {
    setBusy(ref);
    try {
      await pendingTransferStaffApi.reject(ref, reason);
      toast.success('Transfer rejected — sender notified');
      setRejectFor(null);
      await reload();
    } catch (err) {
      toast.error(errorMessage(err, 'Could not reject transfer.'));
    } finally {
      setBusy(null);
    }
  };

  const filtered = items.filter((t) => {
    if (!query.trim()) return true;
    const q = query.trim().toLowerCase();
    return (
      t.reference?.toLowerCase().includes(q) ||
      t.senderAccountNo?.toLowerCase().includes(q) ||
      t.receiverAccountNo?.toLowerCase().includes(q) ||
      t.initiatedByName?.toLowerCase().includes(q)
    );
  });

  const totalAmount = items.reduce((s, t) => s + Number(t.amount || 0), 0);

  return (
    <div className="space-y-8">
      <PageHeader
        breadcrumb={`Staff · CSR · Pending Transfers${branchCode ? ' · ' + branchCode : ''}`}
        title="High-value transfer approvals"
        subtitle="Customers with transfers above ₹1,00,000 — review, approve or reject. Funds are only moved on approval."
        actions={<Button variant="secondary" icon={RefreshCw} onClick={reload}>Refresh</Button>}
      />

      <section className="grid sm:grid-cols-2 xl:grid-cols-3 gap-5">
        <KpiCard label="Pending count"   value={items.length}                icon={Hourglass} accent="gold" />
        <KpiCard label="Total amount"    value={formatINR(totalAmount)}      icon={Hourglass} accent="brand" />
        <KpiCard label="Branch"          value={branchCode || 'ALL'}         icon={Building2} accent="info" />
      </section>

      <Card>
        <div className="flex flex-wrap items-end gap-3 mb-5">
          <Input
            label="Search"
            leftIcon={Search}
            placeholder="Reference, account number or customer name"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            className="flex-1 min-w-[260px]"
          />
          <span className="text-sm text-accent-mute dark:text-ink-400 self-center pb-2">
            {filtered.length} of {items.length} shown
          </span>
        </div>

        {loading ? (
          <div className="space-y-2">
            {Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-14" />)}
          </div>
        ) : filtered.length === 0 ? (
          <EmptyState
            icon={Hourglass}
            title="No pending high-value transfers"
            message="High-value transfers from customers in your branch will appear here for approval."
          />
        ) : (
          <div className="overflow-x-auto -mx-6">
            <table className="w-full text-sm">
              <thead className="text-left text-[11px] uppercase tracking-widest text-accent-mute dark:text-ink-400 border-b border-accent-line dark:border-ink-700">
                <tr>
                  <th className="px-6 py-3">Reference</th>
                  <th className="py-3">From</th>
                  <th className="py-3">To</th>
                  <th className="py-3 text-right">Amount</th>
                  <th className="py-3">Initiated by</th>
                  <th className="py-3">Submitted</th>
                  <th className="py-3 text-right pr-6">Actions</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((t) => (
                  <tr key={t.reference} className="border-b border-accent-line/60 dark:border-ink-700/60 last:border-b-0 hover:bg-accent-surface/40 dark:hover:bg-ink-750/40">
                    <td className="px-6 py-3 font-mono text-xs">{t.reference}</td>
                    <td className="py-3 font-mono text-xs dark:text-ink-100">{t.senderAccountNo}</td>
                    <td className="py-3 font-mono text-xs dark:text-ink-100 inline-flex items-center gap-1.5">
                      <ArrowRight size={12} className="text-accent-mute dark:text-ink-400" />
                      {t.receiverAccountNo}
                    </td>
                    <td className="py-3 text-right font-semibold dark:text-ink-100">{formatINR(t.amount)}</td>
                    <td className="py-3 dark:text-ink-300">{t.initiatedByName || t.initiatedBy}</td>
                    <td className="py-3 text-accent-mute dark:text-ink-400 text-xs">{formatDateTime(t.createdAt)}</td>
                    <td className="py-3 pr-6 text-right">
                      <div className="inline-flex gap-2">
                        <Button
                          variant="secondary"
                          size="sm"
                          icon={XCircle}
                          disabled={busy === t.reference}
                          onClick={() => setRejectFor(t)}
                        >
                          Reject
                        </Button>
                        <Button
                          size="sm"
                          icon={CheckCircle2}
                          loading={busy === t.reference}
                          onClick={() => approve(t.reference)}
                        >
                          Approve
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      <RejectModal
        open={!!rejectFor}
        transfer={rejectFor}
        onClose={() => setRejectFor(null)}
        onSubmit={(reason) => reject(rejectFor.reference, reason)}
        busy={busy !== null}
      />
    </div>
  );
}

function RejectModal({ open, transfer, onClose, onSubmit, busy }) {
  const [reason, setReason] = useState('');

  useEffect(() => { if (!open) setReason(''); }, [open]);
  if (!transfer) return null;

  const tooShort = reason.trim().length < 6;

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={`Reject transfer ${transfer.reference}`}
      description={`${formatINR(transfer.amount)} from ${transfer.senderAccountNo} to ${transfer.receiverAccountNo}`}
      size="md"
      footer={
        <div className="flex justify-end gap-2">
          <Button variant="ghost" onClick={onClose}>Cancel</Button>
          <Button
            variant="danger"
            icon={XCircle}
            disabled={tooShort}
            loading={busy}
            onClick={() => onSubmit(reason.trim())}
          >
            Reject transfer
          </Button>
        </div>
      }
    >
      <div className="rounded-xl bg-amber-50 dark:bg-amber-900/20 text-amber-900 dark:text-amber-200 p-4 flex gap-3 mb-4">
        <AlertTriangle size={18} className="shrink-0 mt-0.5" />
        <p className="text-sm">
          The sender's balance has not been touched. Rejecting this transfer notifies the customer
          with your reason and they can choose to resubmit.
        </p>
      </div>
      <Textarea
        label="Rejection reason"
        placeholder="At least 6 characters — e.g. 'Insufficient KYC for high-value transfer'"
        value={reason}
        onChange={(e) => setReason(e.target.value)}
        rows={4}
      />
    </Modal>
  );
}
