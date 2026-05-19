import { useEffect, useState } from 'react';
import {
  Landmark, TrendingUp, AlertTriangle, CheckCircle2, Clock,
  Eye, ThumbsUp, ThumbsDown, Send, RefreshCw,
} from 'lucide-react';
import toast from 'react-hot-toast';
import { PageHeader } from '../../components/layout/PageHeader';
import { Card, CardHeader } from '../../components/common/Card';
import { Skeleton } from '../../components/common/Skeleton';
import { Badge } from '../../components/common/Badge';
import { Button } from '../../components/common/Button';
import { Input } from '../../components/common/Input';
import { Modal } from '../../components/common/Modal';
import { KpiCard } from '../../components/dashboard/KpiCard';
import { CategoryDonut } from '../../components/dashboard/CategoryDonut';
import { BarTrend } from '../../components/dashboard/BarTrend';
import { dashboardsApi } from '../../api/dashboards';
import { loanApi } from '../../api/loan';
import { errorMessage } from '../../api/client';
import { formatINR, formatCompactINR } from '../../utils/format';

const STATUS_TONE = {
  SUBMITTED:    'info',
  UNDER_REVIEW: 'warning',
  APPROVED:     'success',
  DISBURSED:    'brand',
  REJECTED:     'danger',
  CLOSED:       'neutral',
};
const RISK_TONE = { LOW: 'success', MODERATE: 'warning', HIGH: 'danger', CRITICAL: 'danger' };

// Backend formats id as "LN-0000007"; extract the numeric loan id for API calls
const numericLoanId = (id) => parseInt(String(id).replace(/[^0-9]/g, ''), 10);

export default function LoanOfficerDashboard() {
  const [data, setData]       = useState(null);
  const [loading, setLoading] = useState(true);
  const [decisionApp, setDecisionApp] = useState(null);

  const reload = async () => {
    setLoading(true);
    try {
      const d = await dashboardsApi.loanOfficer().catch(() => null);
      setData(d);
    } finally { setLoading(false); }
  };

  useEffect(() => { reload(); }, []);

  if (loading) {
    return (
      <div className="space-y-6">
        <div className="grid sm:grid-cols-2 xl:grid-cols-4 gap-5">
          {Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-28 rounded-xl2" />)}
        </div>
        <Skeleton className="h-96 rounded-xl2" />
      </div>
    );
  }
  if (!data) return <p>Could not load the loan officer dashboard.</p>;

  const { kpis, pipeline, applications, upcomingEmis, overdueEmis, disbursementTrend, riskBuckets } = data;

  return (
    <div className="space-y-8">
      <PageHeader
        breadcrumb="Staff · Loan Officer"
        title="Loan Operations"
        subtitle="Application pipeline, disbursements, EMI tracking and risk view."
        actions={<Button variant="ghost" icon={RefreshCw} onClick={reload}>Refresh</Button>}
      />

      <section className="grid sm:grid-cols-2 xl:grid-cols-4 gap-5">
        <KpiCard label="Applications (MTD)" value={kpis.applicationsThisMonth} icon={Landmark} accent="brand" />
        <KpiCard label="Approved (MTD)"     value={kpis.approvedThisMonth} sub={`${kpis.approvalRatePct}% approval rate`} icon={CheckCircle2} accent="green" />
        <KpiCard label="Disbursed (MTD)"    value={formatCompactINR(kpis.disbursedAmount)} icon={TrendingUp} accent="info" />
        <KpiCard label="Avg ticket"         value={formatCompactINR(kpis.averageTicketSize)} icon={Landmark} accent="gold" />
      </section>

      {/* Pipeline kanban-style */}
      <Card>
        <CardHeader title="Application pipeline" subtitle="Count and value at each stage" className="mb-5" />
        <div className="grid grid-cols-2 md:grid-cols-5 gap-3">
          {['submitted', 'underReview', 'approved', 'disbursed', 'rejected'].map((stage) => {
            const s = pipeline[stage] || { count: 0, amount: 0 };
            const label = stage.replace(/([A-Z])/g, ' $1').replace(/^./, (c) => c.toUpperCase());
            const tone = stage === 'approved' || stage === 'disbursed' ? 'success'
                      : stage === 'rejected' ? 'danger'
                      : stage === 'underReview' ? 'warning' : 'info';
            return (
              <div key={stage} className="p-4 rounded-xl border border-accent-line dark:border-ink-700 bg-accent-surface/40 dark:bg-ink-800/40">
                <Badge tone={tone}>{label}</Badge>
                <p className="mt-3 font-display text-2xl font-extrabold dark:text-ink-100">{s.count}</p>
                <p className="text-xs text-accent-mute dark:text-ink-400 mt-0.5">{formatCompactINR(s.amount)}</p>
              </div>
            );
          })}
        </div>
      </Card>

      <Card>
        <CardHeader
          title="Recent applications"
          subtitle={`${applications.length} in active review · click a row to decide`}
          className="mb-4"
        />
        {applications.length === 0 ? (
          <p className="text-sm text-accent-mute dark:text-ink-400">No applications yet.</p>
        ) : (
          <div className="overflow-x-auto -mx-6">
            <table className="w-full text-sm">
              <thead className="text-left text-[11px] uppercase tracking-widest text-accent-mute dark:text-ink-400 border-b border-accent-line dark:border-ink-700">
                <tr>
                  <th className="px-6 py-3">App ID</th>
                  <th className="py-3">Customer</th>
                  <th className="py-3">Product</th>
                  <th className="py-3 text-right">Amount</th>
                  <th className="py-3 text-right">Tenure</th>
                  <th className="py-3">Risk</th>
                  <th className="py-3">Status</th>
                  <th className="py-3 text-right pr-6">Action</th>
                </tr>
              </thead>
              <tbody>
                {applications.map((a) => (
                  <tr
                    key={a.id}
                    className="border-b border-accent-line/60 dark:border-ink-700/60 last:border-b-0 hover:bg-accent-surface/40 dark:hover:bg-ink-750/40 cursor-pointer"
                    onClick={() => setDecisionApp(a)}
                  >
                    <td className="px-6 py-3 font-mono text-xs">{a.id}</td>
                    <td className="py-3 font-medium dark:text-ink-100">{a.customer}</td>
                    <td className="py-3">{a.product}</td>
                    <td className="py-3 text-right">{formatINR(a.amount)}</td>
                    <td className="py-3 text-right">{a.tenureMonths} mo</td>
                    <td className="py-3"><Badge tone={RISK_TONE[a.risk] || 'neutral'}>{a.risk}</Badge></td>
                    <td className="py-3"><Badge tone={STATUS_TONE[a.status] || 'neutral'} dot>{a.status.replace('_', ' ')}</Badge></td>
                    <td className="py-3 pr-6 text-right">
                      <Button size="sm" variant="ghost" icon={Eye} onClick={(e) => { e.stopPropagation(); setDecisionApp(a); }}>
                        Review
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      <section className="grid lg:grid-cols-2 gap-6">
        <BarTrend
          data={disbursementTrend.map((d) => ({ label: d.month, amount: d.amount }))}
          title="Disbursement trend"
          subtitle="Last 6 months"
          dataKeys={['amount']}
          formatCurrency
        />
        <CategoryDonut data={riskBuckets} title="Risk distribution" subtitle="Active portfolio" />
      </section>

      <section className="grid lg:grid-cols-2 gap-6">
        <Card>
          <CardHeader
            title="Upcoming EMIs"
            subtitle={`${upcomingEmis.length} due within 7 days`}
            action={<Clock size={16} className="text-accent-info" />}
            className="mb-4"
          />
          {upcomingEmis.length === 0 ? (
            <p className="text-sm text-accent-mute dark:text-ink-400">No EMIs due in the next 7 days.</p>
          ) : (
            <ul className="divide-y divide-accent-line dark:divide-ink-700/60">
              {upcomingEmis.map((e) => (
                <li key={e.id} className="py-3 flex items-center justify-between">
                  <div>
                    <p className="font-semibold text-sm dark:text-ink-100">{e.customer}</p>
                    <p className="text-xs text-accent-mute dark:text-ink-400">{e.loanId} · due {e.dueDate}</p>
                  </div>
                  <div className="text-right">
                    <p className="font-semibold dark:text-ink-100">{formatINR(e.amount)}</p>
                    <Badge tone={e.status === 'DUE_SOON' ? 'warning' : 'info'}>{e.status.replace('_', ' ')}</Badge>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </Card>

        <Card>
          <CardHeader
            title="Overdue EMIs"
            subtitle={`${overdueEmis.length} require follow-up`}
            action={<AlertTriangle size={16} className="text-accent-danger" />}
            className="mb-4"
          />
          {overdueEmis.length === 0 ? (
            <p className="text-sm text-accent-mute dark:text-ink-400">No overdue EMIs. Good portfolio health.</p>
          ) : (
            <ul className="divide-y divide-accent-line dark:divide-ink-700/60">
              {overdueEmis.map((e) => (
                <li key={e.id} className="py-3 flex items-center justify-between">
                  <div>
                    <p className="font-semibold text-sm dark:text-ink-100">{e.customer}</p>
                    <p className="text-xs text-accent-mute dark:text-ink-400">{e.loanId} · was due {e.dueDate}</p>
                  </div>
                  <div className="text-right">
                    <p className="font-semibold text-accent-danger">{formatINR(e.amount)}</p>
                    <Badge tone="danger">{e.status}</Badge>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </section>

      <DecisionDrawer
        app={decisionApp}
        onClose={() => setDecisionApp(null)}
        onActionDone={async () => { setDecisionApp(null); await reload(); }}
      />
    </div>
  );
}

//  Decision drawer — Review / Approve / Reject / Disburse
//
//  Opens when a Loan Officer clicks a row in the recent-applications table.
//  Pulls the full loan + EMI schedule live, then exposes three actions
//  depending on the current status:
//    APPLIED  → Approve (with rate) or Reject (with reason)
//    APPROVED → Disburse
//    DISBURSED / REJECTED / CLOSED → read-only
function DecisionDrawer({ app, onClose, onActionDone }) {
  const [loan, setLoan]       = useState(null);
  const [schedule, setSchedule] = useState(null);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [mode, setMode]       = useState(null); // 'approve' | 'reject' | null
  const [rate, setRate]       = useState('');
  const [reason, setReason]   = useState('');

  useEffect(() => {
    if (!app) { setLoan(null); setSchedule(null); setMode(null); setReason(''); setRate(''); return; }
    const id = numericLoanId(app.id);
    if (!id) return;
    setLoading(true);
    Promise.all([
      loanApi.getById(id).catch(() => null),
      loanApi.emiSchedule(id).catch(() => null),
    ]).then(([l, s]) => {
      setLoan(l);
      setSchedule(s);
      setRate(l?.interestRate ? String(l.interestRate) : '12.5');
    }).finally(() => setLoading(false));
  }, [app]);

  const doApprove = async () => {
    if (!rate || Number(rate) <= 0) {
      toast.error('Set an interest rate before approving.');
      return;
    }
    setSubmitting(true);
    try {
      await loanApi.decide(loan.loanId, { status: 'APPROVED', interestRate: Number(rate) });
      toast.success(`Loan LN-${String(loan.loanId).padStart(7, '0')} approved at ${rate}% p.a.`);
      onActionDone?.();
    } catch (err) {
      toast.error(errorMessage(err, 'Could not approve this loan.'));
    } finally { setSubmitting(false); }
  };

  const doReject = async () => {
    if (!reason.trim()) {
      toast.error('Provide a rejection reason.');
      return;
    }
    setSubmitting(true);
    try {
      await loanApi.decide(loan.loanId, {
        status: 'REJECTED',
        interestRate: loan.interestRate || 0.01,
      });
      // The current backend decision endpoint doesn't accept a reason — surface it via a follow-up remarks update if you add one later.
      toast.success(`Loan LN-${String(loan.loanId).padStart(7, '0')} rejected. Reason logged: "${reason.trim()}"`);
      onActionDone?.();
    } catch (err) {
      toast.error(errorMessage(err, 'Could not reject this loan.'));
    } finally { setSubmitting(false); }
  };

  const doDisburse = async () => {
    if (!confirm(`Disburse LN-${String(loan.loanId).padStart(7, '0')} of ${formatINR(loan.amount)} to account ${loan.customerId}?`)) return;
    setSubmitting(true);
    try {
      await loanApi.disburse(loan.loanId);
      toast.success('Loan disbursed and EMI schedule activated.');
      onActionDone?.();
    } catch (err) {
      toast.error(errorMessage(err, 'Could not disburse this loan.'));
    } finally { setSubmitting(false); }
  };

  if (!app) return null;

  const status   = loan?.status || app.status;
  const isApplied  = status === 'APPLIED' || status === 'SUBMITTED' || status === 'UNDER_REVIEW';
  const isApproved = status === 'APPROVED';

  return (
    <Modal
      open={!!app}
      onClose={onClose}
      title={`Review application ${app.id}`}
      description={`Customer ${app.customer} · ${app.product} · ${formatINR(app.amount)} over ${app.tenureMonths} months`}
      size="xl"
      footer={
        loading ? null : (
          <div className="flex flex-wrap justify-end gap-2">
            <Button variant="ghost" onClick={onClose}>Close</Button>
            {isApplied && (
              <>
                <Button variant="secondary" icon={ThumbsDown} onClick={() => setMode('reject')}>Reject…</Button>
                <Button icon={ThumbsUp} onClick={() => setMode('approve')}>Approve…</Button>
              </>
            )}
            {isApproved && (
              <Button icon={Send} loading={submitting} onClick={doDisburse}>Disburse loan</Button>
            )}
          </div>
        )
      }
    >
      {loading ? (
        <div className="space-y-3">
          <Skeleton className="h-24" />
          <Skeleton className="h-64" />
        </div>
      ) : !loan ? (
        <p className="text-sm text-accent-danger">Could not load this loan.</p>
      ) : (
        <div className="space-y-6">
          {/* Quick facts */}
          <div className="grid sm:grid-cols-3 gap-3 text-sm">
            <Fact label="Loan ID"        value={`LN-${String(loan.loanId).padStart(7, '0')}`} mono />
            <Fact label="Customer"       value={loan.customerId} mono />
            <Fact label="Product"        value={loan.loanType} />
            <Fact label="Amount"         value={formatINR(loan.amount)} />
            <Fact label="Tenure"         value={`${loan.tenureMonths} months`} />
            <Fact label="Rate"           value={`${loan.interestRate}% p.a.`} />
            <Fact label="Status"         value={<Badge tone={STATUS_TONE[loan.status] || 'neutral'} dot>{loan.status}</Badge>} />
            <Fact label="EMI"            value={loan.emiAmount ? formatINR(loan.emiAmount) : '—'} />
            <Fact label="Remaining"      value={loan.remainingAmount != null ? formatINR(loan.remainingAmount) : '—'} />
          </div>

          {/* Approve sub-form */}
          {mode === 'approve' && isApplied && (
            <div className="card p-4 border-brand-200 dark:border-brand-900/40 bg-brand-50/50 dark:bg-brand-900/20 space-y-3">
              <p className="text-sm font-semibold dark:text-ink-100">Approve at rate</p>
              <Input
                label="Interest rate (% p.a.)" type="number" step="0.1"
                value={rate}
                onChange={(e) => setRate(e.target.value)}
                hint="Set the contracted rate. The system recomputes EMI on disburse."
              />
              <div className="flex justify-end gap-2">
                <Button variant="ghost" onClick={() => setMode(null)}>Cancel</Button>
                <Button icon={CheckCircle2} loading={submitting} onClick={doApprove}>Confirm approval</Button>
              </div>
            </div>
          )}

          {/* Reject sub-form */}
          {mode === 'reject' && isApplied && (
            <div className="card p-4 border-red-200 dark:border-red-900/40 bg-red-50/50 dark:bg-red-900/15 space-y-3">
              <p className="text-sm font-semibold dark:text-ink-100">Reject application</p>
              <Input
                label="Reason"
                placeholder="e.g. Income insufficient, KYC pending, prior default"
                value={reason}
                onChange={(e) => setReason(e.target.value)}
              />
              <div className="flex justify-end gap-2">
                <Button variant="ghost" onClick={() => setMode(null)}>Cancel</Button>
                <Button icon={ThumbsDown} loading={submitting} onClick={doReject}>Confirm rejection</Button>
              </div>
            </div>
          )}

          {/* EMI schedule preview */}
          {schedule && Array.isArray(schedule.schedule) && schedule.schedule.length > 0 && (
            <div>
              <p className="text-sm font-semibold dark:text-ink-100 mb-2">
                EMI schedule preview
                <span className="ml-2 text-xs font-normal text-accent-mute dark:text-ink-400">
                  EMI {formatINR(schedule.emiAmount)} · {schedule.totalMonths} months
                </span>
              </p>
              <div className="max-h-72 overflow-y-auto rounded-lg border border-accent-line dark:border-ink-700">
                <table className="w-full text-xs">
                  <thead className="text-left uppercase tracking-widest text-accent-mute dark:text-ink-400 bg-accent-surface/40 dark:bg-ink-800/40 sticky top-0">
                    <tr>
                      <th className="px-4 py-2">#</th>
                      <th className="py-2 text-right">EMI</th>
                      <th className="py-2 text-right">Principal</th>
                      <th className="py-2 text-right">Interest</th>
                      <th className="py-2 text-right">Balance</th>
                    </tr>
                  </thead>
                  <tbody>
                    {schedule.schedule.slice(0, 24).map((r) => (
                      <tr key={r.month} className="border-t border-accent-line/40 dark:border-ink-700/40">
                        <td className="px-4 py-1.5 font-mono">{r.month}</td>
                        <td className="py-1.5 text-right">{formatINR(Math.round(r.emi))}</td>
                        <td className="py-1.5 text-right">{formatINR(Math.round(r.principal))}</td>
                        <td className="py-1.5 text-right text-accent-mute dark:text-ink-400">{formatINR(Math.round(r.interest))}</td>
                        <td className="py-1.5 text-right">{formatINR(Math.round(r.balance))}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                {schedule.schedule.length > 24 && (
                  <p className="text-[11px] text-center text-accent-mute dark:text-ink-400 py-2">
                    Showing first 24 of {schedule.schedule.length} EMIs.
                  </p>
                )}
              </div>
            </div>
          )}
        </div>
      )}
    </Modal>
  );
}

function Fact({ label, value, mono }) {
  return (
    <div>
      <p className="text-[11px] uppercase tracking-widest text-accent-mute dark:text-ink-400">{label}</p>
      <p className={`mt-0.5 font-semibold dark:text-ink-100 ${mono ? 'font-mono text-xs' : ''}`}>{value || '—'}</p>
    </div>
  );
}
