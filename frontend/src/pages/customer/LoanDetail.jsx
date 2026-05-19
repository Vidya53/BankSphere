import { useEffect, useMemo, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import {
  ArrowLeft, Landmark, Calendar, Banknote, CreditCard,
  CheckCircle2, AlertTriangle, Send, FileText, History, ZapOff,
  KeyRound, Download, Bell,
} from 'lucide-react';
import toast from 'react-hot-toast';
import { PageHeader } from '../../components/layout/PageHeader';
import { Card, CardHeader } from '../../components/common/Card';
import { Skeleton } from '../../components/common/Skeleton';
import { Button } from '../../components/common/Button';
import { Modal } from '../../components/common/Modal';
import { Input, Select } from '../../components/common/Input';
import { Badge, StatusBadge } from '../../components/common/Badge';
import { EmptyState } from '../../components/common/EmptyState';
import { loanApi } from '../../api/loan';
import { accountApi } from '../../api/account';
import { errorMessage } from '../../api/client';
import { formatINR, formatDate } from '../../utils/format';

const PRODUCT_NAME = {
  PERSONAL: 'Personal Loan', HOME: 'Home Loan', CAR: 'Auto Loan',
  EDUCATION: 'Education Loan', BUSINESS: 'Business Loan',
};

export default function LoanDetail() {
  const { id } = useParams();
  const [loan, setLoan] = useState(null);
  const [schedule, setSchedule] = useState(null);
  const [payments, setPayments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [payOpen, setPayOpen] = useState(false);
  const [prepayOpen, setPrepayOpen] = useState(false);
  const [tab, setTab] = useState('schedule');

  const reload = async () => {
    setLoading(true);
    try {
      const [l, s, p] = await Promise.all([
        loanApi.getById(id).catch(() => null),
        loanApi.emiSchedule(id).catch(() => null),
        loanApi.paymentHistory(id).catch(() => []),
      ]);
      setLoan(l);
      setSchedule(s);
      setPayments(Array.isArray(p) ? p : []);
    } finally { setLoading(false); }
  };

  useEffect(() => { reload(); /* eslint-disable-next-line */ }, [id]);

  if (loading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-32 rounded-xl2" />
        <div className="grid sm:grid-cols-2 xl:grid-cols-4 gap-5">
          {Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-24 rounded-xl2" />)}
        </div>
        <Skeleton className="h-96 rounded-xl2" />
      </div>
    );
  }

  if (!loan) {
    return (
      <EmptyState
        icon={Landmark}
        title="Loan not found"
        message="This loan does not exist or you don't have access to it."
        action={<Link to="/app/loans" className="btn-secondary">Back to loans</Link>}
      />
    );
  }

  const productName = PRODUCT_NAME[loan.loanType] || loan.loanType;
  const canPay      = loan.status === 'DISBURSED' && (loan.emiAmount || 0) > 0;
  const canPrepay   = loan.status === 'DISBURSED' && (loan.remainingAmount || 0) > 0;
  const isClosed    = loan.status === 'CLOSED' || (loan.remainingAmount === 0 && loan.status === 'DISBURSED');

  // Due-date proximity: surface a banner when EMI is overdue or due within 7 days
  const dueInfo = (() => {
    if (!loan.nextDueDate || loan.status !== 'DISBURSED') return null;
    const today = new Date(); today.setHours(0, 0, 0, 0);
    const due   = new Date(loan.nextDueDate);
    const days  = Math.round((due - today) / (1000 * 60 * 60 * 24));
    if (days < 0)  return { kind: 'overdue',  days: -days, due: loan.nextDueDate };
    if (days <= 7) return { kind: 'due-soon', days,        due: loan.nextDueDate };
    return null;
  })();

  const downloadStatement = () => downloadLoanStatementPdf({ loan, schedule, payments });

  return (
    <div className="space-y-8">
      <div>
        <Link to="/app/loans" className="inline-flex items-center gap-1.5 text-sm text-accent-mute dark:text-ink-400 hover:text-accent-ink dark:hover:text-ink-100 mb-2">
          <ArrowLeft size={14} /> Back to loans
        </Link>
        <PageHeader
          breadcrumb={`Loans · ${productName}`}
          title={`LN-${String(loan.loanId).padStart(7, '0')}`}
          subtitle={
            <span className="flex flex-wrap items-center gap-2 text-sm">
              <StatusBadge status={loan.status} />
              <Badge tone="neutral">{productName}</Badge>
              {loan.disbursedAt && (
                <span className="text-accent-mute dark:text-ink-400">
                  Disbursed on {formatDate(loan.disbursedAt)}
                </span>
              )}
            </span>
          }
          actions={
            <>
              <Button variant="ghost" icon={Download} onClick={downloadStatement}>
                Statement
              </Button>
              {canPrepay && (
                <Button variant="secondary" icon={ZapOff} onClick={() => setPrepayOpen(true)}>
                  Prepay / Foreclose
                </Button>
              )}
              {canPay && (
                <Button icon={Send} onClick={() => setPayOpen(true)}>
                  Pay EMI
                </Button>
              )}
            </>
          }
        />
      </div>

      {/* Due-date reminder banner — shown above KPIs when overdue or due within 7d */}
      {dueInfo && (
        <div className={`card p-5 flex items-start gap-3 ${
          dueInfo.kind === 'overdue'
            ? 'border-red-200 dark:border-red-900/40 bg-red-50/60 dark:bg-red-900/15'
            : 'border-amber-200 dark:border-amber-900/40 bg-amber-50/60 dark:bg-amber-900/15'
        }`}>
          {dueInfo.kind === 'overdue'
            ? <AlertTriangle className="text-accent-danger shrink-0 mt-0.5" />
            : <Bell           className="text-accent-warning shrink-0 mt-0.5" />}
          <div className="flex-1 min-w-0">
            <p className={`font-semibold ${dueInfo.kind === 'overdue' ? 'text-accent-danger' : 'text-accent-warning'}`}>
              {dueInfo.kind === 'overdue'
                ? `EMI overdue by ${dueInfo.days} ${dueInfo.days === 1 ? 'day' : 'days'}`
                : (dueInfo.days === 0
                    ? `EMI is due today`
                    : `EMI due in ${dueInfo.days} ${dueInfo.days === 1 ? 'day' : 'days'}`)}
            </p>
            <p className="text-xs text-accent-slate dark:text-ink-300 mt-0.5">
              {dueInfo.kind === 'overdue'
                ? <>A late-payment penalty of 2% of your EMI ({formatINR(Math.round((loan.emiAmount || 0) * 0.02))}) will be added when you pay. Settle now to stop further charges.</>
                : <>Schedule a payment before <span className="font-semibold">{dueInfo.due}</span> to avoid the 2% late penalty.</>}
            </p>
          </div>
          <Button size="sm" icon={Send} onClick={() => setPayOpen(true)}>
            Pay EMI now
          </Button>
        </div>
      )}

      {/* Summary KPIs */}
      <section className="grid sm:grid-cols-2 xl:grid-cols-4 gap-5">
        <KpiTile label="Principal"        value={formatINR(loan.amount)}          icon={Banknote}    accent="brand" />
        <KpiTile label="Remaining"        value={formatINR(loan.remainingAmount)} icon={CreditCard}  accent="info"
                 sub={loan.amount ? `${Math.round((loan.remainingAmount / loan.amount) * 100)}% left` : null} />
        <KpiTile label="EMI"              value={formatINR(loan.emiAmount)}       icon={Calendar}    accent="green"
                 sub={loan.nextDueDate ? `Next due ${loan.nextDueDate}` : null} />
        <KpiTile label="Tenure"           value={`${loan.tenureMonths} mo`}        icon={History}     accent="gold"
                 sub={`${loan.emiPaidCount || 0} paid · ${loan.emisRemaining ?? loan.tenureMonths} remaining`} />
      </section>

      {isClosed && (
        <div className="card p-5 border-green-100 dark:border-green-900/40 bg-green-50/60 dark:bg-green-900/15 flex items-center gap-3">
          <CheckCircle2 className="text-accent-success shrink-0" />
          <div>
            <p className="font-semibold text-accent-success">Loan closed</p>
            <p className="text-xs text-accent-slate dark:text-ink-300">This loan has been fully repaid. No further EMIs are due.</p>
          </div>
        </div>
      )}

      {loan.status === 'APPLIED' && (
        <div className="card p-5 border-amber-100 dark:border-amber-900/40 bg-amber-50/60 dark:bg-amber-900/15 flex items-start gap-3">
          <AlertTriangle className="text-accent-warning shrink-0 mt-0.5" />
          <div className="flex-1">
            <p className="font-semibold text-accent-warning">Awaiting loan officer review</p>
            <p className="text-sm text-accent-slate dark:text-ink-300 mt-1">
              Your application has been received and is queued for review by a loan officer.
              You'll see this status change once they approve or reject the application.
              <span className="block mt-1 text-xs text-accent-mute dark:text-ink-400">
                EMI payments aren't available until the loan is approved and disbursed.
              </span>
            </p>
          </div>
        </div>
      )}

      {loan.status === 'APPROVED' && (
        <div className="card p-5 border-blue-100 dark:border-blue-900/40 bg-sky-50/60 dark:bg-sky-900/15 flex items-start gap-3">
          <CheckCircle2 className="text-accent-info shrink-0 mt-0.5" />
          <div className="flex-1">
            <p className="font-semibold text-accent-info">
              Approved at {loan.interestRate}% p.a. — waiting for disbursement
            </p>
            <p className="text-sm text-accent-slate dark:text-ink-300 mt-1">
              Your loan is approved. A loan officer will disburse the funds to account{' '}
              <span className="font-mono font-semibold">{loan.accountId || '—'}</span>{' '}
              after which the EMI schedule starts.
              <span className="block mt-1 text-xs text-accent-mute dark:text-ink-400">
                Pay EMI and Prepay become available once disbursement is complete.
              </span>
            </p>
          </div>
        </div>
      )}

      {loan.status === 'REJECTED' && (
        <div className="card p-5 border-red-100 dark:border-red-900/40 bg-red-50/60 dark:bg-red-900/15 flex items-start gap-3">
          <AlertTriangle className="text-accent-danger shrink-0 mt-0.5" />
          <div className="flex-1">
            <p className="font-semibold text-accent-danger">Application rejected</p>
            <p className="text-sm text-accent-slate dark:text-ink-300 mt-1">
              {loan.remarks || 'Your loan application could not be approved. Please contact your branch for details, or reapply with adjusted terms.'}
            </p>
          </div>
        </div>
      )}

      {/* Tabs */}
      <div className="flex gap-2 border-b border-accent-line dark:border-ink-700">
        {[
          { key: 'schedule', label: `EMI schedule (${schedule?.schedule?.length ?? 0})` },
          { key: 'history',  label: `Payment history (${payments.length})` },
        ].map((t) => (
          <button
            key={t.key}
            onClick={() => setTab(t.key)}
            className={`px-4 py-2.5 text-sm font-semibold border-b-2 -mb-px transition focus-ring ${
              tab === t.key
                ? 'border-brand-700 text-brand-700 dark:text-brand-300'
                : 'border-transparent text-accent-mute dark:text-ink-400 hover:text-accent-ink dark:hover:text-ink-100'
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {tab === 'schedule' && <ScheduleTab schedule={schedule} paidCount={loan.emiPaidCount || 0} loanStatus={loan.status} />}
      {tab === 'history'  && <HistoryTab payments={payments} />}

      <PayEmiModal
        open={payOpen}
        loan={loan}
        onClose={() => setPayOpen(false)}
        onPaid={async () => { setPayOpen(false); await reload(); }}
      />
      <PrepayModal
        open={prepayOpen}
        loan={loan}
        onClose={() => setPrepayOpen(false)}
        onPrepaid={async () => { setPrepayOpen(false); await reload(); }}
      />
    </div>
  );
}

//  Schedule + History tabs
function ScheduleTab({ schedule, paidCount, loanStatus }) {
  if (!schedule || !Array.isArray(schedule.schedule) || schedule.schedule.length === 0) {
    return (
      <Card>
        <CardHeader title="EMI schedule" subtitle="Amortisation breakdown" className="mb-4" />
        <EmptyState icon={FileText} title="No schedule yet" message="The EMI schedule appears once the loan is disbursed." />
      </Card>
    );
  }
  // Once the loan is closed (full payment or foreclosure) every remaining row
  // is covered — show "Closed" instead of the misleading "Scheduled".
  const isClosed = loanStatus === 'CLOSED';
  return (
    <Card>
      <CardHeader
        title="EMI schedule"
        subtitle={`Total: ${schedule.totalMonths} months · EMI ${formatINR(schedule.emiAmount)}`}
        className="mb-4"
      />
      <div className="overflow-x-auto -mx-6">
        <table className="w-full text-sm">
          <thead className="text-left text-[11px] uppercase tracking-widest text-accent-mute dark:text-ink-400 border-b border-accent-line dark:border-ink-700">
            <tr>
              <th className="px-6 py-3">#</th>
              <th className="py-3 text-right">EMI</th>
              <th className="py-3 text-right">Principal</th>
              <th className="py-3 text-right">Interest</th>
              <th className="py-3 text-right">Balance</th>
              <th className="py-3">Status</th>
            </tr>
          </thead>
          <tbody>
            {schedule.schedule.map((row) => {
              const paid = row.month <= paidCount;
              const label = paid ? 'Paid' : isClosed ? 'Closed' : 'Scheduled';
              const tone  = paid || isClosed ? 'success' : 'neutral';
              return (
                <tr key={row.month} className="border-b border-accent-line/60 dark:border-ink-700/60 last:border-b-0 hover:bg-accent-surface/40 dark:hover:bg-ink-750/40">
                  <td className="px-6 py-2.5 font-mono text-xs">{row.month}</td>
                  <td className="py-2.5 text-right">{formatINR(Math.round(row.emi))}</td>
                  <td className="py-2.5 text-right">{formatINR(Math.round(row.principal))}</td>
                  <td className="py-2.5 text-right text-accent-mute dark:text-ink-400">{formatINR(Math.round(row.interest))}</td>
                  <td className="py-2.5 text-right">{formatINR(Math.round(row.balance))}</td>
                  <td className="py-2.5">
                    <Badge tone={tone}>{label}</Badge>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </Card>
  );
}

function HistoryTab({ payments }) {
  if (!payments || payments.length === 0) {
    return (
      <Card>
        <CardHeader title="Payment history" subtitle="EMIs paid against this loan" className="mb-4" />
        <EmptyState icon={History} title="No payments yet" message="Once you start paying EMIs, they will appear here." />
      </Card>
    );
  }
  return (
    <Card>
      <CardHeader title="Payment history" subtitle={`${payments.length} payments`} className="mb-4" />
      <div className="overflow-x-auto -mx-6">
        <table className="w-full text-sm">
          <thead className="text-left text-[11px] uppercase tracking-widest text-accent-mute dark:text-ink-400 border-b border-accent-line dark:border-ink-700">
            <tr>
              <th className="px-6 py-3">Date</th>
              <th className="py-3 text-right">Paid</th>
              <th className="py-3 text-right">Principal</th>
              <th className="py-3 text-right">Interest</th>
              <th className="py-3 text-right">Penalty</th>
              <th className="py-3 text-right">Balance after</th>
              <th className="py-3">Ref</th>
              <th className="py-3">Late?</th>
            </tr>
          </thead>
          <tbody>
            {payments.map((p) => (
              <tr key={p.paymentId} className="border-b border-accent-line/60 dark:border-ink-700/60 last:border-b-0 hover:bg-accent-surface/40 dark:hover:bg-ink-750/40">
                <td className="px-6 py-2.5">{p.paidDate || formatDate(p.createdAt)}</td>
                <td className="py-2.5 text-right font-semibold">{formatINR(p.amountPaid)}</td>
                <td className="py-2.5 text-right">{formatINR(p.principalComponent)}</td>
                <td className="py-2.5 text-right text-accent-mute dark:text-ink-400">{formatINR(p.interestComponent)}</td>
                <td className="py-2.5 text-right">{p.penaltyAmount ? formatINR(p.penaltyAmount) : '—'}</td>
                <td className="py-2.5 text-right">{formatINR(p.balanceAfterPayment)}</td>
                <td className="py-2.5 text-xs font-mono text-accent-mute dark:text-ink-400">{p.transactionRef || '—'}</td>
                <td className="py-2.5">
                  {p.late ? <Badge tone="warning">Late</Badge> : <Badge tone="success">On time</Badge>}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </Card>
  );
}

//  Pay EMI modal — PIN-protected, with late-fee preview
//
//  Late fee = 2% of EMI when paid after the scheduled due date (mirrors
//  LoanServiceImpl.LATE_PENALTY_RATE). We surface it in the UI BEFORE the
//  customer commits so there are no surprises on the debit.
const LATE_PENALTY_RATE = 0.02;

function PayEmiModal({ open, loan, onClose, onPaid }) {
  const [submitting, setSubmitting] = useState(false);
  const [accounts, setAccounts] = useState([]);
  const { register, handleSubmit, reset, watch, formState: { errors } } = useForm({
    defaultValues: { accountId: '', amount: loan?.emiAmount || 0, pin: '' },
  });

  useEffect(() => {
    if (!open) return;
    reset({ accountId: '', amount: loan?.emiAmount || 0, pin: '' });
    accountApi.myAccounts()
      .then((a) => setAccounts((a || []).filter((x) => x.status === 'ACTIVE')))
      .catch(() => setAccounts([]));
  }, [open]); // eslint-disable-line

  const amount  = Number(watch('amount')) || 0;
  const isLate  = loan?.nextDueDate && new Date(loan.nextDueDate) < new Date(new Date().toDateString());
  const latePenalty = isLate ? Math.round((loan?.emiAmount || 0) * LATE_PENALTY_RATE) : 0;
  const totalDebit  = amount + latePenalty;

  const onSubmit = async (v) => {
    setSubmitting(true);
    try {
      await loanApi.payEmi(loan.loanId, {
        accountId: v.accountId,
        amount:    Number(v.amount),
        pin:       v.pin,
      });
      toast.success(isLate
        ? `EMI ${formatINR(amount)} + late fee ${formatINR(latePenalty)} paid`
        : `EMI ${formatINR(amount)} paid successfully`);
      onPaid?.();
    } catch (err) {
      toast.error(errorMessage(err, 'Could not process EMI payment.'));
    } finally { setSubmitting(false); }
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Pay EMI"
      description={`EMI ${formatINR(loan?.emiAmount)} · Next due ${loan?.nextDueDate || '—'}`}
      size="md"
      footer={
        <div className="flex justify-end gap-2">
          <Button variant="ghost" onClick={onClose}>Cancel</Button>
          <Button icon={Send} loading={submitting} onClick={handleSubmit(onSubmit)}>Pay {formatINR(totalDebit)}</Button>
        </div>
      }
    >
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <Select
          label="Pay from account"
          {...register('accountId', { required: 'Select an account' })}
          error={errors.accountId?.message}
        >
          <option value="">Select an account…</option>
          {accounts.map((a) => (
            <option key={a.accountNo} value={a.accountNo}>
              {a.accountNo} · {a.accountType} · Bal {formatINR(a.balance ?? 0)}
            </option>
          ))}
        </Select>
        <Input
          label="Amount (₹)" type="number"
          hint="Defaults to your EMI. You can pay a partial amount if needed."
          error={errors.amount?.message}
          {...register('amount', {
            required: 'Required',
            min: { value: 1, message: 'Must be positive' },
          })}
        />

        {isLate && (
          <div className="card p-3 border-amber-200 dark:border-amber-900/40 bg-amber-50/60 dark:bg-amber-900/15 text-sm">
            <div className="flex items-center gap-2 font-semibold text-accent-warning">
              <AlertTriangle size={14} /> Late payment — 2% penalty applies
            </div>
            <p className="mt-1 text-xs text-accent-slate dark:text-ink-300">
              Your scheduled due date was <span className="font-semibold">{loan.nextDueDate}</span>.
              A penalty of <span className="font-semibold">{formatINR(latePenalty)}</span> will be debited together with your EMI.
            </p>
          </div>
        )}

        <Input
          label="Transaction PIN" type="password" inputMode="numeric" leftIcon={KeyRound}
          placeholder="4–6 digit PIN" maxLength={6}
          hint="Same PIN you use for transfers."
          error={errors.pin?.message}
          {...register('pin', {
            required: 'PIN is required',
            pattern: { value: /^[0-9]{4,6}$/, message: '4–6 digits only' },
          })}
        />

        <div className="card p-3 bg-brand-50/40 dark:bg-brand-900/15 border-brand-100 dark:border-brand-900/40">
          <p className="text-[11px] uppercase tracking-widest text-accent-mute dark:text-ink-400">Total debit</p>
          <p className="font-display text-xl font-extrabold dark:text-ink-100">{formatINR(totalDebit)}</p>
          {isLate && (
            <p className="text-xs text-accent-mute dark:text-ink-400 mt-1">
              EMI {formatINR(amount)} + Late fee {formatINR(latePenalty)}
            </p>
          )}
        </div>
      </form>
    </Modal>
  );
}

//  Prepay / foreclose modal — PIN-protected, with charge preview
//
//  Foreclosure charge = 2% of remaining balance when fullForeclosure is true.
//  Partial prepayments carry no charge. Both modes show the live total
//  before the customer commits.
const FORECLOSURE_RATE = 0.02;

function PrepayModal({ open, loan, onClose, onPrepaid }) {
  const [submitting, setSubmitting] = useState(false);
  const [accounts, setAccounts] = useState([]);
  // Foreclosure-only flow: the checkbox is rendered for clarity but locked on.
  // Customers can't toggle to partial prepay from this dialog.
  const { register, handleSubmit, reset, formState: { errors } } = useForm({
    defaultValues: { accountId: '', amount: loan?.remainingAmount || 0, fullForeclosure: true, pin: '' },
  });
  const fullForeclosure = true;

  useEffect(() => {
    if (!open) return;
    reset({
      accountId: '',
      amount: loan?.remainingAmount || 0,
      fullForeclosure: true,
      pin: '',
    });
    accountApi.myAccounts()
      .then((a) => setAccounts((a || []).filter((x) => x.status === 'ACTIVE')))
      .catch(() => setAccounts([]));
  }, [open]); // eslint-disable-line

  // Live preview of what will be debited (full foreclosure only).
  const remaining = loan?.remainingAmount || 0;
  const foreclosureCharge = Math.round(remaining * FORECLOSURE_RATE);
  const totalDebit = remaining + foreclosureCharge;

  const onSubmit = async (v) => {
    setSubmitting(true);
    try {
      await loanApi.prepay(loan.loanId, {
        accountId:       v.accountId,
        amount:          remaining,
        fullForeclosure: true,
        pin:             v.pin,
      });
      toast.success(`Loan foreclosed (paid ${formatINR(totalDebit)} incl. ${formatINR(foreclosureCharge)} charge)`);
      onPrepaid?.();
    } catch (err) {
      toast.error(errorMessage(err, 'Could not process foreclosure.'));
    } finally { setSubmitting(false); }
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Prepay / Foreclose loan"
      description={`Outstanding balance ${formatINR(remaining)} · Closing early reduces total interest paid.`}
      size="md"
      footer={
        <div className="flex justify-end gap-2">
          <Button variant="ghost" onClick={onClose}>Cancel</Button>
          <Button icon={ZapOff} loading={submitting} onClick={handleSubmit(onSubmit)}>
            Foreclose · {formatINR(totalDebit)}
          </Button>
        </div>
      }
    >
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <Select
          label="Debit from account"
          {...register('accountId', { required: 'Select an account' })}
          error={errors.accountId?.message}
        >
          <option value="">Select an account…</option>
          {accounts.map((a) => (
            <option key={a.accountNo} value={a.accountNo}>
              {a.accountNo} · {a.accountType} · Bal {formatINR(a.balance ?? 0)}
            </option>
          ))}
        </Select>

        <label className="flex items-center gap-2 cursor-not-allowed select-none">
          <input
            type="checkbox"
            className="h-4 w-4 accent-brand-700"
            checked
            disabled
            readOnly
            aria-readonly
          />
          <span className="text-sm dark:text-ink-100">Foreclose the loan completely (2% charge applies)</span>
        </label>

        <Input
          label="Amount (₹)" type="number"
          hint="Locked to the full outstanding balance."
          disabled
          value={remaining}
          readOnly
        />

        <Input
          label="Transaction PIN" type="password" inputMode="numeric" leftIcon={KeyRound}
          placeholder="4–6 digit PIN" maxLength={6}
          hint="Same PIN you use for transfers."
          error={errors.pin?.message}
          {...register('pin', {
            required: 'PIN is required',
            pattern: { value: /^[0-9]{4,6}$/, message: '4–6 digits only' },
          })}
        />

        <div className="card p-3 bg-brand-50/40 dark:bg-brand-900/15 border-brand-100 dark:border-brand-900/40">
          <p className="text-[11px] uppercase tracking-widest text-accent-mute dark:text-ink-400">Total debit</p>
          <p className="font-display text-xl font-extrabold dark:text-ink-100">{formatINR(totalDebit)}</p>
          <p className="text-xs text-accent-mute dark:text-ink-400 mt-1">
            Outstanding {formatINR(remaining)} + Foreclosure charge (2%) {formatINR(foreclosureCharge)}
          </p>
        </div>
      </form>
    </Modal>
  );
}

//  PDF statement generation
//
//  Browser-side rendering using a hidden <iframe> + window.print(). This
//  avoids adding a heavy PDF dependency and gives users the native
//  "Save as PDF" dialog. The HTML mirrors what a real bank statement looks
//  like: header, loan summary, EMI schedule, payment history, footer.
function downloadLoanStatementPdf({ loan, schedule, payments }) {
  const win = window.open('', '_blank', 'width=900,height=700');
  if (!win) {
    alert('Pop-up blocked — please allow pop-ups to download the statement.');
    return;
  }
  const inr = (n) => '₹' + Number(n || 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  const today = new Date().toISOString().slice(0, 10);
  const id    = 'LN-' + String(loan.loanId).padStart(7, '0');
  const totalInterestEstimate = Math.max(0, Math.round(((loan.emiAmount || 0) * (loan.tenureMonths || 0)) - (loan.amount || 0)));

  const scheduleRows = (schedule?.schedule || []).map((r) => `
    <tr>
      <td>${r.month}</td>
      <td class="r">${inr(r.emi)}</td>
      <td class="r">${inr(r.principal)}</td>
      <td class="r muted">${inr(r.interest)}</td>
      <td class="r">${inr(r.balance)}</td>
      <td>${r.month <= (loan.emiPaidCount || 0) ? 'Paid' : 'Scheduled'}</td>
    </tr>`).join('');

  const paymentRows = (payments || []).map((p) => `
    <tr>
      <td>${p.paidDate || (p.createdAt || '').slice(0, 10)}</td>
      <td class="r">${inr(p.amountPaid)}</td>
      <td class="r">${inr(p.principalComponent)}</td>
      <td class="r muted">${inr(p.interestComponent)}</td>
      <td class="r">${p.penaltyAmount ? inr(p.penaltyAmount) : '—'}</td>
      <td class="r">${inr(p.balanceAfterPayment)}</td>
      <td>${p.transactionRef || '—'}</td>
      <td>${p.late ? 'Late' : 'On time'}</td>
    </tr>`).join('');

  win.document.write(`<!doctype html>
<html><head>
<title>BankSphere · Loan Statement ${id}</title>
<style>
  @page { size: A4; margin: 14mm 16mm; }
  body { font-family: 'Segoe UI', system-ui, -apple-system, Arial, sans-serif; color:#1a1a1a; font-size: 11px; }
  h1 { margin: 0; font-size: 22px; color:#97144D; letter-spacing: 0.5px; }
  h2 { font-size: 13px; margin-top: 20px; border-bottom: 1px solid #e5d6c1; padding-bottom: 4px; color:#5d3737; text-transform: uppercase; letter-spacing: 1.2px; }
  .header { display: flex; justify-content: space-between; align-items: flex-start; border-bottom: 2px solid #97144D; padding-bottom: 12px; }
  .meta { text-align: right; color: #777; font-size: 10px; line-height: 1.6; }
  table { width: 100%; border-collapse: collapse; margin-top: 8px; }
  th, td { border-bottom: 1px solid #eee; padding: 5px 6px; text-align: left; }
  th { background: #fff7f1; color: #5d3737; text-transform: uppercase; font-size: 9px; letter-spacing: 1px; }
  td.r, th.r { text-align: right; }
  .muted { color: #999; }
  .summary { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; margin-top: 10px; }
  .summary .cell { border: 1px solid #f0e5d4; padding: 8px 10px; border-radius: 4px; }
  .summary .label { color: #999; text-transform: uppercase; font-size: 9px; letter-spacing: 1px; }
  .summary .value { font-size: 14px; font-weight: 700; margin-top: 3px; }
  .footer { margin-top: 30px; font-size: 9px; color: #999; border-top: 1px solid #eee; padding-top: 8px; text-align: center; }
  .badge { display: inline-block; padding: 2px 6px; background: #e6f7ec; color: #167948; border-radius: 3px; font-size: 9px; font-weight: 600; }
  @media print { .noprint { display: none; } }
</style>
</head><body>
  <div class="header">
    <div>
      <h1>BankSphere</h1>
      <div style="font-size:11px;color:#666;margin-top:2px">Loan Account Statement</div>
    </div>
    <div class="meta">
      Loan Reference: <strong>${id}</strong><br>
      Customer ID: ${loan.customerId || '—'}<br>
      Statement Date: ${today}<br>
      Status: <span class="badge">${loan.status}</span>
    </div>
  </div>

  <h2>Loan summary</h2>
  <div class="summary">
    <div class="cell"><div class="label">Product</div><div class="value">${loan.loanType || '—'}</div></div>
    <div class="cell"><div class="label">Principal</div><div class="value">${inr(loan.amount)}</div></div>
    <div class="cell"><div class="label">Outstanding</div><div class="value">${inr(loan.remainingAmount)}</div></div>
    <div class="cell"><div class="label">Interest Rate</div><div class="value">${loan.interestRate || 0}% p.a.</div></div>
    <div class="cell"><div class="label">EMI</div><div class="value">${inr(loan.emiAmount)}</div></div>
    <div class="cell"><div class="label">Tenure</div><div class="value">${loan.tenureMonths || 0} months</div></div>
    <div class="cell"><div class="label">EMIs Paid</div><div class="value">${loan.emiPaidCount || 0} / ${loan.tenureMonths || 0}</div></div>
    <div class="cell"><div class="label">Next Due Date</div><div class="value">${loan.nextDueDate || '—'}</div></div>
    <div class="cell"><div class="label">Total Interest (est.)</div><div class="value">${inr(totalInterestEstimate)}</div></div>
  </div>

  <h2>EMI schedule (${(schedule?.schedule || []).length} months)</h2>
  <table>
    <thead><tr><th>#</th><th class="r">EMI</th><th class="r">Principal</th><th class="r">Interest</th><th class="r">Balance</th><th>Status</th></tr></thead>
    <tbody>${scheduleRows || '<tr><td colspan="6" style="text-align:center;color:#999">Schedule not yet generated (loan not disbursed).</td></tr>'}</tbody>
  </table>

  <h2>Payment history (${(payments || []).length} payments)</h2>
  <table>
    <thead><tr><th>Date</th><th class="r">Paid</th><th class="r">Principal</th><th class="r">Interest</th><th class="r">Penalty</th><th class="r">Balance after</th><th>Ref</th><th>Late?</th></tr></thead>
    <tbody>${paymentRows || '<tr><td colspan="8" style="text-align:center;color:#999">No payments recorded yet.</td></tr>'}</tbody>
  </table>

  <div class="footer">
    This statement was generated on ${today} from your BankSphere account. For disputes or clarifications,
    please contact your branch or raise a support ticket from the app. This is a system-generated document
    and does not require a signature.
  </div>
</body></html>`);
  win.document.close();
  // Give the browser a tick to render before triggering the native print dialog.
  setTimeout(() => { try { win.focus(); win.print(); } catch (e) { /* ignore */ } }, 250);
}

//  KPI tile
function KpiTile({ label, value, sub, icon: Icon, accent = 'brand' }) {
  const tones = {
    brand: 'bg-brand-50 dark:bg-brand-900/30 text-brand-700 dark:text-brand-300',
    info:  'bg-sky-50 dark:bg-sky-900/30 text-accent-info dark:text-sky-300',
    green: 'bg-green-50 dark:bg-green-900/30 text-accent-success dark:text-green-300',
    gold:  'bg-amber-50 dark:bg-amber-900/30 text-accent-warning dark:text-amber-300',
  };
  return (
    <div className="card p-5">
      <span className={`h-10 w-10 rounded-xl grid place-items-center ${tones[accent] || tones.brand}`}>
        <Icon size={18} />
      </span>
      <p className="mt-4 text-[11px] uppercase tracking-widest text-accent-mute dark:text-ink-400">{label}</p>
      <p className="mt-1 font-display text-2xl font-extrabold dark:text-ink-100">{value}</p>
      {sub && <p className="mt-0.5 text-xs text-accent-mute dark:text-ink-400">{sub}</p>}
    </div>
  );
}
