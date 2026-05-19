import { useEffect, useState } from 'react';
import {
  ShieldCheck, CheckCircle2, XCircle, RefreshCw, Search,
  IdCard, User as UserIcon, AlertTriangle,
} from 'lucide-react';
import toast from 'react-hot-toast';
import { PageHeader } from '../../components/layout/PageHeader';
import { Card, CardHeader } from '../../components/common/Card';
import { Skeleton } from '../../components/common/Skeleton';
import { StatusBadge } from '../../components/common/Badge';
import { Button } from '../../components/common/Button';
import { Input } from '../../components/common/Input';
import { Modal } from '../../components/common/Modal';
import { Textarea } from '../../components/common/Input';
import { EmptyState } from '../../components/common/EmptyState';
import { useAuth } from '../../context/AuthContext';
import { kycApi } from '../../api/customer';
import { errorMessage } from '../../api/client';
import { formatDateTime } from '../../utils/format';

export default function KycReview() {
  const { user } = useAuth();
  const branchCode = user?.branchCode || null;

  const [items, setItems]     = useState([]);
  const [loading, setLoading] = useState(true);
  const [query, setQuery]     = useState('');
  const [busyId, setBusyId]   = useState(null);
  const [rejectFor, setRejectFor] = useState(null);

  const reload = async () => {
    setLoading(true);
    try {
      // listPending returns KycResponseDTOs; the dashboard's kycQueue had richer
      // enrichment but this endpoint is the canonical pending list.
      const rows = await kycApi.listPending(branchCode);
      setItems(Array.isArray(rows) ? rows : []);
    } catch (err) {
      toast.error(errorMessage(err, 'Could not load pending KYC.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { reload(); }, [branchCode]); // eslint-disable-line react-hooks/exhaustive-deps

  const approve = async (customerNo) => {
    setBusyId(customerNo);
    try {
      await kycApi.approve(customerNo);
      toast.success(`KYC approved for ${customerNo}`);
      await reload();
    } catch (err) {
      toast.error(errorMessage(err, 'Could not approve KYC.'));
    } finally {
      setBusyId(null);
    }
  };

  const reject = async (customerNo, reason) => {
    setBusyId(customerNo);
    try {
      await kycApi.reject(customerNo, reason);
      toast.success(`KYC rejected for ${customerNo}`);
      setRejectFor(null);
      await reload();
    } catch (err) {
      toast.error(errorMessage(err, 'Could not reject KYC.'));
    } finally {
      setBusyId(null);
    }
  };

  const filtered = items.filter((r) => {
    if (!query.trim()) return true;
    const q = query.trim().toLowerCase();
    return (
      r.customerNo?.toLowerCase().includes(q) ||
      r.documentNumber?.toLowerCase().includes(q) ||
      r.documentType?.toLowerCase().includes(q)
    );
  });

  return (
    <div className="space-y-8">
      <PageHeader
        breadcrumb={`Staff · CSR · KYC Review${branchCode ? ' · ' + branchCode : ''}`}
        title="KYC Review Queue"
        subtitle={branchCode
          ? `Pending KYC submissions for ${branchCode}. Approve or reject each one.`
          : 'All pending KYC submissions in the system.'}
        actions={<Button variant="secondary" icon={RefreshCw} onClick={reload}>Refresh</Button>}
      />

      <Card>
        <div className="flex flex-wrap items-end gap-3 mb-5">
          <Input
            label="Search"
            leftIcon={Search}
            placeholder="Customer number, document number, doc type"
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
            {Array.from({ length: 5 }).map((_, i) => <Skeleton key={i} className="h-14" />)}
          </div>
        ) : filtered.length === 0 ? (
          <EmptyState
            icon={ShieldCheck}
            title="No pending KYC"
            message={query
              ? 'No submissions match your search.'
              : 'There are no KYC submissions awaiting review right now.'}
          />
        ) : (
          <div className="overflow-x-auto -mx-6">
            <table className="w-full text-sm">
              <thead className="text-left text-[11px] uppercase tracking-widest text-accent-mute dark:text-ink-400 border-b border-accent-line dark:border-ink-700">
                <tr>
                  <th className="px-6 py-3">Customer</th>
                  <th className="py-3">Document</th>
                  <th className="py-3">Status</th>
                  <th className="py-3">Submitted</th>
                  <th className="py-3 text-right pr-6">Actions</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((row) => (
                  <tr key={row.id || row.customerNo} className="border-b border-accent-line/60 dark:border-ink-700/60 last:border-b-0 hover:bg-accent-surface/40 dark:hover:bg-ink-750/40">
                    <td className="px-6 py-3">
                      <div className="flex items-center gap-2">
                        <UserIcon size={14} className="text-accent-mute dark:text-ink-400" />
                        <span className="font-mono text-xs dark:text-ink-100">{row.customerNo}</span>
                      </div>
                    </td>
                    <td className="py-3">
                      <div className="flex items-center gap-2">
                        <IdCard size={14} className="text-brand-700 dark:text-brand-300" />
                        <span className="font-semibold dark:text-ink-100">{row.documentType?.replace(/_/g, ' ')}</span>
                      </div>
                      <div className="font-mono text-xs text-accent-mute dark:text-ink-400 mt-0.5">{row.documentNumber}</div>
                    </td>
                    <td className="py-3"><StatusBadge status={row.status} /></td>
                    <td className="py-3 text-accent-mute dark:text-ink-400 text-xs">{formatDateTime(row.submittedDate || row.createdAt)}</td>
                    <td className="py-3 pr-6 text-right">
                      <div className="inline-flex gap-2">
                        <Button
                          variant="secondary"
                          size="sm"
                          icon={XCircle}
                          disabled={busyId === row.customerNo}
                          onClick={() => setRejectFor(row)}
                        >
                          Reject
                        </Button>
                        <Button
                          size="sm"
                          icon={CheckCircle2}
                          loading={busyId === row.customerNo}
                          onClick={() => approve(row.customerNo)}
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
        kyc={rejectFor}
        onClose={() => setRejectFor(null)}
        onSubmit={(reason) => reject(rejectFor.customerNo, reason)}
        busy={busyId !== null}
      />
    </div>
  );
}

function RejectModal({ open, kyc, onClose, onSubmit, busy }) {
  const [reason, setReason] = useState('');

  useEffect(() => { if (!open) setReason(''); }, [open]);

  if (!kyc) return null;

  const tooShort = reason.trim().length < 6;

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={`Reject KYC for ${kyc.customerNo}`}
      description="Tell the customer why their KYC was rejected. This will be visible on their KYC page."
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
            Reject KYC
          </Button>
        </div>
      }
    >
      <div className="rounded-xl bg-amber-50 dark:bg-amber-900/20 text-amber-900 dark:text-amber-200 p-4 flex gap-3 mb-4">
        <AlertTriangle size={18} className="shrink-0 mt-0.5" />
        <p className="text-sm">
          The customer will be able to resubmit a new KYC document after rejection. Be specific
          about what's wrong (e.g. "Document expired", "Number doesn't match Aadhaar format").
        </p>
      </div>
      <Textarea
        label="Rejection reason"
        placeholder="At least 6 characters…"
        value={reason}
        onChange={(e) => setReason(e.target.value)}
        rows={4}
      />
    </Modal>
  );
}
