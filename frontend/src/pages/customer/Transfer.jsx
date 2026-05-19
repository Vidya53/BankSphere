import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import {
  Send, ArrowRight, ShieldCheck, CheckCircle2, AlertTriangle,
  KeyRound, Eye, EyeOff, Hourglass,
} from 'lucide-react';
import toast from 'react-hot-toast';
import { PageHeader } from '../../components/layout/PageHeader';
import { Card, CardHeader } from '../../components/common/Card';
import { Input, Select } from '../../components/common/Input';
import { Button } from '../../components/common/Button';
import { Skeleton } from '../../components/common/Skeleton';
import { Badge } from '../../components/common/Badge';
import { EmptyState } from '../../components/common/EmptyState';
import { accountApi } from '../../api/account';
import { pinApi, transferApi } from '../../api/transfer';
import { errorMessage } from '../../api/client';
import { applyServerErrors } from '../../api/serverErrors';
import { formatINR, formatDateTime } from '../../utils/format';

// All customer-initiated transfers ride the INTERNAL rail (same-bank). The
// channel field was removed from the form because the customer doesn't need
// to pick it — the backend always classifies these as internal transfers.
const TRANSFER_CHANNEL = 'INTERNAL';
const HIGH_VALUE_THRESHOLD = 100000;

export default function Transfer() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const fromParam = params.get('from') || '';

  const [accounts,   setAccounts]   = useState([]);
  const [loading,    setLoading]    = useState(true);
  const [pinSet,     setPinSet]     = useState(null);
  const [myPending,  setMyPending]  = useState([]);

  const [step,    setStep]    = useState(1);    // 1 = compose, 2 = review+PIN, 3 = done
  const [draft,   setDraft]   = useState(null);
  const [result,  setResult]  = useState(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    (async () => {
      try {
        const [list, pending] = await Promise.all([
          accountApi.myAccounts().catch(() => []),
          transferApi.myPending().catch(() => []),
        ]);
        const active = (Array.isArray(list) ? list : []).filter((a) => a.status === 'ACTIVE');
        setAccounts(active);
        setMyPending(Array.isArray(pending) ? pending : []);
      } catch (err) {
        toast.error(errorMessage(err, 'Could not load accounts.'));
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const { register, handleSubmit, watch, setValue, setError, formState: { errors } } = useForm({
    mode: 'onTouched',
    defaultValues: {
      senderAccountNo:   fromParam,
      receiverAccountNo: '',
      amount: '',
      description: '',
    },
  });

  const senderNo = watch('senderAccountNo');
  const amount   = Number(watch('amount') || 0);
  const sender   = useMemo(
    () => accounts.find((a) => a.accountNo === senderNo),
    [accounts, senderNo]
  );

  useEffect(() => {
    if (!senderNo && accounts.length > 0) setValue('senderAccountNo', accounts[0].accountNo);
  }, [accounts, senderNo, setValue]);

  useEffect(() => {
    if (!senderNo) { setPinSet(null); return; }
    pinApi.status(senderNo)
      .then((res) => setPinSet(!!res?.isSet))
      .catch(() => setPinSet(null));
  }, [senderNo]);

  if (loading) {
    return (
      <div className="space-y-6 max-w-4xl">
        <Skeleton className="h-10 w-72" />
        <Skeleton className="h-72 rounded-xl2" />
      </div>
    );
  }

  if (accounts.length === 0) {
    return (
      <div className="space-y-6 max-w-4xl">
        <PageHeader breadcrumb="Banking" title="Send money" />
        <Card className="p-10">
          <EmptyState
            title="No active accounts"
            message="You need an active account before you can transfer money."
            action={<Link to="/app/accounts/apply" className="btn-primary">Open an account</Link>}
          />
        </Card>
      </div>
    );
  }

  const review = (values) => { setDraft(values); setStep(2); };
  const isHigh = (amt) => Number(amt) > HIGH_VALUE_THRESHOLD;

  const confirm = async (pin) => {
    setSubmitting(true);
    try {
      const res = await transferApi.initiate({
        senderAccountNo:   draft.senderAccountNo,
        receiverAccountNo: draft.receiverAccountNo,
        amount:            Number(draft.amount),
        pin,
        channel:           TRANSFER_CHANNEL,
        description:       draft.description || undefined,
      });
      setResult(res);
      setStep(3);
      if (res.status === 'PENDING_APPROVAL') {
        toast.success(`Transfer queued for CSR approval — ${res.reference}`);
      } else {
        toast.success('Transfer completed');
      }
    } catch (err) {
      const applied = applyServerErrors(err, setError);
      if (applied) {
        toast.error('Please fix the highlighted fields and try again.');
      } else {
        toast.error(errorMessage(err, 'Transfer failed.'));
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-8 max-w-4xl">
      <PageHeader
        breadcrumb="Banking · Move money"
        title="Send money"
        subtitle="Transfer between accounts. A transaction PIN is required; transfers above ₹1,00,000 are reviewed by your branch CSR."
      />

      <Stepper step={step} />

      {step === 1 && (
        <Card>
          <form onSubmit={handleSubmit(review)} className="space-y-5">
            <div className="grid sm:grid-cols-2 gap-4">
              <Select label="From account" required error={errors.senderAccountNo?.message} {...register('senderAccountNo', { required: 'Required' })}>
                <option value="">Select an account</option>
                {accounts.map((a) => (
                  <option key={a.accountNo} value={a.accountNo}>
                    {a.accountType} · {a.accountNo} ({formatINR(a.balance)})
                  </option>
                ))}
              </Select>
              <Input
                label="Receiver account"
                required
                placeholder="e.g. SAV6F5C63D0786C4A"
                error={errors.receiverAccountNo?.message}
                {...register('receiverAccountNo', {
                  required: 'Required',
                  pattern: { value: /^[A-Z0-9]{5,20}$/, message: 'Uppercase letters and digits, 5–20 chars' },
                })}
              />
            </div>

            <Input
              label="Amount (₹)"
              required
              type="number"
              step="0.01"
              placeholder="0.00"
              error={errors.amount?.message}
              {...register('amount', {
                required: 'Required',
                min: { value: 1, message: 'Minimum ₹1' },
              })}
            />

            <Input label="Description (optional)" placeholder="e.g. Rent for April" {...register('description', { maxLength: 255 })} />

            {pinSet === false && (
              <BlockerCard
                tone="warning"
                icon={KeyRound}
                title="Set a transaction PIN first"
                message="You haven't set a transaction PIN on this account yet. Set one before sending money."
                action={<Link to="/app/security/pin" className="btn-primary"><KeyRound size={14} /> Set PIN</Link>}
              />
            )}

            {sender && amount > 0 && amount > Number(sender.balance || 0) - Number(sender.minimumBalance || 0) && (
              <BlockerCard
                tone="danger"
                icon={AlertTriangle}
                title="Insufficient balance"
                message={`Available after minimum-balance: ${formatINR(Number(sender.balance || 0) - Number(sender.minimumBalance || 0))}.`}
              />
            )}

            {amount > 0 && isHigh(amount) && (
              <BlockerCard
                tone="info"
                icon={Hourglass}
                title="High-value transfer"
                message={`Amounts above ${formatINR(HIGH_VALUE_THRESHOLD)} require CSR approval at your branch. Your account will not be debited until a CSR approves it.`}
              />
            )}

            <div className="flex items-center justify-between pt-4 border-t border-accent-line dark:border-ink-700">
              <p className="text-xs text-accent-mute dark:text-ink-400 inline-flex items-center gap-2">
                <ShieldCheck size={14} className="text-brand-700 dark:text-brand-300" />
                PIN required at the next step. Wrong PIN locks the account for 15 minutes after 5 attempts.
              </p>
              <Button type="submit" iconRight={ArrowRight} disabled={pinSet === false}>Review</Button>
            </div>
          </form>
        </Card>
      )}

      {step === 2 && draft && (
        <ReviewStep
          draft={draft}
          sender={sender}
          isHighValue={isHigh(draft.amount)}
          submitting={submitting}
          onBack={() => setStep(1)}
          onConfirm={confirm}
        />
      )}

      {step === 3 && result && (
        <SuccessStep
          result={result}
          onDone={() => navigate(result.status === 'PENDING_APPROVAL' ? '/app/transfer/pending' : '/app/transactions')}
        />
      )}

      {myPending.length > 0 && step === 1 && (
        <Card>
          <CardHeader
            title="Your pending high-value transfers"
            subtitle={`${myPending.length} awaiting CSR approval`}
            action={<Link to="/app/transfer/pending" className="text-sm font-semibold text-brand-700 dark:text-brand-300 hover:underline">View all</Link>}
            className="mb-4"
          />
          <ul className="divide-y divide-accent-line dark:divide-ink-700">
            {myPending.slice(0, 5).map((p) => (
              <li key={p.reference} className="py-3 flex items-center justify-between gap-3">
                <div>
                  <p className="font-semibold dark:text-ink-100">{formatINR(p.amount)} → {p.receiverAccountNo}</p>
                  <p className="text-xs text-accent-mute dark:text-ink-400 font-mono">{p.reference} · {formatDateTime(p.createdAt)}</p>
                </div>
                <Badge tone="warning" dot>{p.status?.replace('_', ' ')}</Badge>
              </li>
            ))}
          </ul>
        </Card>
      )}
    </div>
  );
}

function Stepper({ step }) {
  const steps = [
    { id: 1, label: 'Compose' },
    { id: 2, label: 'Confirm with PIN' },
    { id: 3, label: 'Done' },
  ];
  return (
    <ol className="flex items-center gap-3">
      {steps.map((s, i) => (
        <li key={s.id} className="flex items-center gap-3 flex-1">
          <span className={`h-8 w-8 rounded-full grid place-items-center text-xs font-bold border-2 ${
            step >= s.id
              ? 'bg-brand-700 border-brand-700 text-white dark:bg-brand-600 dark:border-brand-600'
              : 'bg-white border-accent-line text-accent-mute dark:bg-ink-800 dark:border-ink-700 dark:text-ink-400'
          }`}>
            {step > s.id ? <CheckCircle2 size={14} /> : s.id}
          </span>
          <span className={`text-sm font-medium ${step >= s.id ? 'text-accent-ink dark:text-ink-100' : 'text-accent-mute dark:text-ink-400'}`}>{s.label}</span>
          {i < steps.length - 1 && (
            <div className={`flex-1 h-px ${step > s.id ? 'bg-brand-700 dark:bg-brand-500' : 'bg-accent-line dark:bg-ink-700'}`} />
          )}
        </li>
      ))}
    </ol>
  );
}

function ReviewStep({ draft, sender, isHighValue, submitting, onBack, onConfirm }) {
  const [pin, setPin] = useState('');
  const [show, setShow] = useState(false);

  return (
    <Card>
      <CardHeader title="Confirm the transfer" subtitle="Review the details and enter your transaction PIN" className="mb-6" />

      <div className="rounded-xl bg-accent-surface/60 dark:bg-ink-850 p-6 grid sm:grid-cols-2 gap-y-3 gap-x-6 text-sm">
        <Row label="From"   value={`${sender?.accountType || ''} · ${draft.senderAccountNo}`} />
        <Row label="To"     value={draft.receiverAccountNo} />
        <Row label="Amount" value={formatINR(draft.amount)} highlight />
        {draft.description && <Row label="Description" value={draft.description} fullWidth />}
      </div>

      {isHighValue && (
        <div className="mt-5 rounded-xl bg-sky-50 dark:bg-sky-900/20 text-sky-900 dark:text-sky-200 p-4 flex gap-3">
          <Hourglass size={18} className="shrink-0 mt-0.5" />
          <p className="text-sm">
            This is a high-value transfer. After PIN confirmation it will be queued for CSR approval at your branch.
            Your account will not be debited until the CSR approves it.
          </p>
        </div>
      )}

      <div className="mt-6">
        <Input
          label="Transaction PIN"
          type={show ? 'text' : 'password'}
          inputMode="numeric"
          leftIcon={KeyRound}
          placeholder="Enter your 4–6 digit PIN"
          value={pin}
          onChange={(e) => setPin(e.target.value.replace(/\D/g, '').slice(0, 6))}
          rightSlot={
            <button type="button" onClick={() => setShow((v) => !v)} className="text-accent-mute dark:text-ink-400" aria-label="Toggle">
              {show ? <EyeOff size={16} /> : <Eye size={16} />}
            </button>
          }
        />
      </div>

      <div className="mt-6 flex items-center justify-end gap-2">
        <Button variant="ghost" onClick={onBack}>Back</Button>
        <Button
          icon={Send}
          loading={submitting}
          disabled={pin.length < 4}
          onClick={() => onConfirm(pin)}
        >
          {isHighValue ? 'Submit for approval' : 'Confirm & Send'}
        </Button>
      </div>
    </Card>
  );
}

function Row({ label, value, highlight, fullWidth }) {
  return (
    <div className={fullWidth ? 'sm:col-span-2' : ''}>
      <dt className="text-[11px] uppercase tracking-widest text-accent-mute dark:text-ink-400">{label}</dt>
      <dd className={`mt-0.5 font-semibold ${highlight ? 'text-brand-700 dark:text-brand-300 text-lg' : 'text-accent-ink dark:text-ink-100'}`}>{value}</dd>
    </div>
  );
}

function SuccessStep({ result, onDone }) {
  const isPending = result.status === 'PENDING_APPROVAL';
  const Icon = isPending ? Hourglass : CheckCircle2;
  return (
    <Card className="text-center p-10">
      <span className={`mx-auto h-16 w-16 rounded-full grid place-items-center ${
        isPending
          ? 'bg-sky-50 text-accent-info dark:bg-sky-900/30 dark:text-sky-300'
          : 'bg-green-50 text-accent-success dark:bg-green-900/30 dark:text-green-300'
      }`}>
        <Icon size={28} />
      </span>
      <h2 className="mt-5 font-display text-2xl font-extrabold dark:text-ink-100">
        {isPending ? 'Pending CSR approval' : 'Transfer successful'}
      </h2>
      <p className="mt-2 text-accent-slate dark:text-ink-300 max-w-md mx-auto">
        {isPending
          ? `Your transfer of ${formatINR(result.amount)} has been queued for review at your branch. You'll be notified once a decision is made.`
          : 'Your transfer has been completed and the recipient has been credited.'}
      </p>
      <div className="mt-6 inline-flex flex-col gap-1.5 rounded-lg bg-accent-surface/60 dark:bg-ink-850 px-5 py-4 text-sm">
        <div className="flex gap-6">
          <span className="text-accent-mute dark:text-ink-400">Reference</span>
          <span className="font-mono font-semibold dark:text-ink-100">{result.reference || result.idempotencyKey}</span>
        </div>
        <div className="flex gap-6">
          <span className="text-accent-mute dark:text-ink-400">Status</span>
          <span className="font-semibold text-brand-700 dark:text-brand-300">{result.status}</span>
        </div>
        {result.senderBalance != null && (
          <div className="flex gap-6">
            <span className="text-accent-mute dark:text-ink-400">New balance</span>
            <span className="font-semibold dark:text-ink-100">{formatINR(result.senderBalance)}</span>
          </div>
        )}
      </div>
      <div className="mt-7">
        <Button onClick={onDone} iconRight={ArrowRight}>
          {isPending ? 'Track approval' : 'View transactions'}
        </Button>
      </div>
    </Card>
  );
}

function BlockerCard({ tone = 'warning', icon: Icon, title, message, action }) {
  const styles = {
    warning: 'bg-amber-50 dark:bg-amber-900/20 text-amber-900 dark:text-amber-200',
    danger:  'bg-red-50   dark:bg-red-900/20   text-red-900   dark:text-red-200',
    info:    'bg-sky-50   dark:bg-sky-900/20   text-sky-900   dark:text-sky-200',
  };
  return (
    <div className={`rounded-xl p-4 flex items-start gap-3 ${styles[tone]}`}>
      <Icon size={18} className="shrink-0 mt-0.5" />
      <div className="flex-1">
        <p className="text-sm font-semibold">{title}</p>
        <p className="text-sm mt-0.5">{message}</p>
      </div>
      {action}
    </div>
  );
}
