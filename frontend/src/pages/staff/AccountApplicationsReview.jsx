import { useEffect, useMemo, useState } from 'react';
import {
  Wallet, CheckCircle2, XCircle, RefreshCw, Search,
  AlertTriangle, Building2,
} from 'lucide-react';
import toast from 'react-hot-toast';
import { PageHeader } from '../../components/layout/PageHeader';
import { Card, CardHeader } from '../../components/common/Card';
import { Skeleton } from '../../components/common/Skeleton';
import { Badge, StatusBadge } from '../../components/common/Badge';
import { Button } from '../../components/common/Button';
import { Input, Select, Textarea } from '../../components/common/Input';
import { Modal } from '../../components/common/Modal';
import { EmptyState } from '../../components/common/EmptyState';
import { useAuth } from '../../context/AuthContext';
import { accountApi } from '../../api/account';
import { errorMessage } from '../../api/client';
import { formatDateTime, formatINR } from '../../utils/format';

export default function AccountApplicationsReview() {
  const { user } = useAuth();
  const branchCode = user?.branchCode || null;

  const [items, setItems]     = useState([]);
  const [loading, setLoading] = useState(true);
  const [scope, setScope]     = useState('PENDING');  // 'PENDING' | 'ALL'
  const [query, setQuery]     = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [busyId, setBusyId]   = useState(null);
  const [rejectFor, setRejectFor] = useState(null);

  const reload = async () => {
    setLoading(true);
    try {
      const rows = scope === 'PENDING'
        ? await accountApi.pendingApplications()
        : await accountApi.allApplications();
      setItems(Array.isArray(rows) ? rows : []);
    } catch (err) {
      toast.error(errorMessage(err, 'Could not load account applications.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { reload(); }, [scope]); // eslint-disable-line react-hooks/exhaustive-deps

  const filtered = useMemo(() => items.filter((a) => {
    if (statusFilter !== 'ALL' && a.status !== statusFilter) return false;
    if (!query.trim()) return true;
    const q = query.trim().toLowerCase();
    return (
      a.applicationRef?.toLowerCase().includes(q) ||
      a.customerName?.toLowerCase().includes(q) ||
      a.customerId?.toLowerCase().includes(q) ||
      a.accountType?.toLowerCase().includes(q)
    );
  }), [items, query, statusFilter]);

  const approve = async (id) => {
    setBusyId(id);
    try {
      const result = await accountApi.approveApplication(id);
      toast.success(`Application approved. Account created: ${result?.generatedAccountNo || '—'}`);
      await reload();
    } catch (err) {
      toast.error(errorMessage(err, 'Could not approve application.'));
    } finally {
      setBusyId(null);
    }
  };

  const reject = async (id, reason, remarks) => {
    setBusyId(id);
    try {
      await accountApi.rejectApplication(id, { reason, remarks });
      toast.success('Application rejected');
      setRejectFor(null);
      await reload();
    } catch (err) {
      toast.error(errorMessage(err, 'Could not reject application.'));
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div className="space-y-8">
      <PageHeader
        breadcrumb={`Staff · CSR · Account Applications${branchCode ? ' · ' + branchCode : ''}`}
        title="Account Applications"
        subtitle="Review, approve and reject account applications for your branch."
        actions={<Button variant="secondary" icon={RefreshCw} onClick={reload}>Refresh</Button>}
      />

      <div className="card p-1.5 inline-flex flex-wrap gap-1">
        {[
          { key: 'PENDING', label: 'Pending review' },
          { key: 'ALL',     label: 'All applications' },
        ].map((s) => (
          <button
            key={s.key}
            onClick={() => setScope(s.key)}
            className={`px-4 py-2 rounded-lg text-sm font-semibold transition ${
              scope === s.key
                ? 'bg-brand-700 text-white dark:bg-brand-600'
                : 'text-accent-slate dark:text-ink-300 hover:bg-accent-surface dark:hover:bg-ink-750'
            }`}
          >
            {s.label}
          </button>
        ))}
      </div>

      <Card>
        <div className="flex flex-wrap items-end gap-3 mb-5">
          <Input
            label="Search"
            leftIcon={Search}
            placeholder="Reference, customer name or ID, account type"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            className="flex-1 min-w-[260px]"
          />
          {scope === 'ALL' && (
            <Select label="Status" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} className="min-w-[180px]">
              <option value="ALL">All statuses</option>
              <option value="SUBMITTED">Submitted</option>
              <option value="UNDER_REVIEW">Under review</option>
              <option value="APPROVED">Approved</option>
              <option value="REJECTED">Rejected</option>
            </Select>
          )}
          <span className="text-sm text-accent-mute dark:text-ink-400 self-center pb-2">
            {filtered.length} of {items.length} shown
          </span>
        </div>

        {loading ? (
          <div className="space-y-2">
            {Array.from({ length: 5 }).map((_, i) => <Skeleton key={i} className="h-16" />)}
          </div>
        ) : filtered.length === 0 ? (
          <EmptyState
            icon={Wallet}
            title={scope === 'PENDING' ? 'No applications pending review' : 'No applications match these filters'}
            message={scope === 'PENDING'
              ? 'When customers apply for new accounts, they will appear here for approval.'
              : 'Try clearing the search or status filter.'}
          />
        ) : (
          <div className="overflow-x-auto -mx-6">
            <table className="w-full text-sm">
              <thead className="text-left text-[11px] uppercase tracking-widest text-accent-mute dark:text-ink-400 border-b border-accent-line dark:border-ink-700">
                <tr>
                  <th className="px-6 py-3">Reference</th>
                  <th className="py-3">Customer</th>
                  <th className="py-3">Account type</th>
                  <th className="py-3 text-right">Initial deposit</th>
                  <th className="py-3">Branch</th>
                  <th className="py-3">Applied</th>
                  <th className="py-3">Status</th>
                  <th className="py-3 text-right pr-6">Actions</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((a) => {
                  const canDecide = a.status === 'SUBMITTED' || a.status === 'UNDER_REVIEW';
                  return (
                    <tr key={a.id || a.applicationRef} className="border-b border-accent-line/60 dark:border-ink-700/60 last:border-b-0 hover:bg-accent-surface/40 dark:hover:bg-ink-750/40">
                      <td className="px-6 py-3 font-mono text-xs">{a.applicationRef}</td>
                      <td className="py-3">
                        <div className="font-semibold dark:text-ink-100">{a.customerName || '—'}</div>
                        <div className="text-xs text-accent-mute dark:text-ink-400">{a.customerEmail || a.customerPhone || a.customerId}</div>
                      </td>
                      <td className="py-3"><Badge tone="brand">{a.accountType}</Badge></td>
                      <td className="py-3 text-right">{a.initialDeposit ? formatINR(a.initialDeposit) : '—'}</td>
                      <td className="py-3">
                        <span className="inline-flex items-center gap-1 text-accent-slate dark:text-ink-300">
                          <Building2 size={12} className="text-accent-mute dark:text-ink-400" />
                          <span className="font-mono text-xs">{a.branchCode}</span>
                        </span>
                      </td>
                      <td className="py-3 text-accent-mute dark:text-ink-400 text-xs">{formatDateTime(a.createdAt)}</td>
                      <td className="py-3"><StatusBadge status={a.status} /></td>
                      <td className="py-3 pr-6 text-right">
                        {canDecide ? (
                          <div className="inline-flex gap-2">
                            <Button
                              variant="secondary"
                              size="sm"
                              icon={XCircle}
                              disabled={busyId === a.id}
                              onClick={() => setRejectFor(a)}
                            >
                              Reject
                            </Button>
                            <Button
                              size="sm"
                              icon={CheckCircle2}
                              loading={busyId === a.id}
                              onClick={() => approve(a.id)}
                            >
                              Approve
                            </Button>
                          </div>
                        ) : a.generatedAccountNo ? (
                          <span className="text-xs font-mono text-accent-mute dark:text-ink-400">
                            → {a.generatedAccountNo}
                          </span>
                        ) : (
                          <span className="text-xs text-accent-mute dark:text-ink-400">No action</span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      <RejectAppModal
        open={!!rejectFor}
        application={rejectFor}
        onClose={() => setRejectFor(null)}
        onSubmit={(reason, remarks) => reject(rejectFor.id, reason, remarks)}
        busy={busyId !== null}
      />
    </div>
  );
}

function RejectAppModal({ open, application, onClose, onSubmit, busy }) {
  const [reason, setReason]   = useState('');
  const [remarks, setRemarks] = useState('');

  useEffect(() => { if (!open) { setReason(''); setRemarks(''); } }, [open]);

  if (!application) return null;

  const trimmed = reason.trim();
  const tooShort = trimmed.length < 10;
  const tooLong  = trimmed.length > 500;

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={`Reject application ${application.applicationRef}`}
      description="Tell the customer why their application was rejected. The reason is shown on their applications list."
      size="md"
      footer={
        <div className="flex justify-end gap-2">
          <Button variant="ghost" onClick={onClose}>Cancel</Button>
          <Button
            variant="danger"
            icon={XCircle}
            disabled={tooShort || tooLong}
            loading={busy}
            onClick={() => onSubmit(trimmed, remarks.trim())}
          >
            Reject application
          </Button>
        </div>
      }
    >
      <div className="rounded-xl bg-amber-50 dark:bg-amber-900/20 text-amber-900 dark:text-amber-200 p-4 flex gap-3 mb-4">
        <AlertTriangle size={18} className="shrink-0 mt-0.5" />
        <p className="text-sm">
          Rejection is reversible only by the customer reapplying. Be specific about what
          would let them succeed next time (e.g. "Initial deposit below minimum for SAVINGS").
        </p>
      </div>
      <Textarea
        label="Rejection reason"
        placeholder="10–500 characters"
        value={reason}
        onChange={(e) => setReason(e.target.value)}
        rows={4}
        error={tooShort && reason.length > 0 ? 'Reason must be at least 10 characters' : tooLong ? 'Reason must not exceed 500 characters' : null}
      />
      <Input
        label="Internal remarks (optional)"
        placeholder="For audit trail — not shown to the customer"
        maxLength={100}
        value={remarks}
        onChange={(e) => setRemarks(e.target.value)}
        className="mt-4"
      />
    </Modal>
  );
}
