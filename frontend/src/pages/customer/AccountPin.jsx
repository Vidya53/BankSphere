import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import {
  KeyRound, ShieldCheck, Save, AlertTriangle, RefreshCw,
  Eye, EyeOff, CheckCircle2, Wallet,
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
import { pinApi } from '../../api/transfer';
import { errorMessage } from '../../api/client';
import { applyServerErrors } from '../../api/serverErrors';
import { maskAccountNo } from '../../utils/format';

export default function AccountPin() {
  const navigate = useNavigate();
  const [accounts, setAccounts] = useState([]);
  const [accountNo, setAccountNo] = useState('');
  const [pinSet, setPinSet] = useState(null);   // null = unknown, true/false once loaded
  const [loading, setLoading] = useState(true);
  const [checking, setChecking] = useState(false);

  useEffect(() => {
    (async () => {
      try {
        const list = await accountApi.myAccounts().catch(() => []);
        const active = (Array.isArray(list) ? list : []).filter((a) => a.status === 'ACTIVE');
        setAccounts(active);
        if (active[0]) setAccountNo(active[0].accountNo);
      } catch (err) {
        toast.error(errorMessage(err, 'Could not load your accounts.'));
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  // Check PIN status whenever the picked account changes
  useEffect(() => {
    if (!accountNo) { setPinSet(null); return; }
    setChecking(true);
    pinApi.status(accountNo)
      .then((res) => setPinSet(!!res?.isSet))
      .catch(() => setPinSet(null))
      .finally(() => setChecking(false));
  }, [accountNo]);

  if (loading) {
    return (
      <div className="space-y-6 max-w-3xl">
        <Skeleton className="h-10 w-72" />
        <Skeleton className="h-64 rounded-xl2" />
      </div>
    );
  }

  if (accounts.length === 0) {
    return (
      <div className="space-y-6 max-w-3xl">
        <PageHeader breadcrumb="Banking · Security" title="Transaction PIN" />
        <Card className="p-10">
          <EmptyState
            icon={Wallet}
            title="No active accounts"
            message="You need at least one active account before you can set a transaction PIN."
            action={<Button onClick={() => navigate('/app/accounts')}>Go to accounts</Button>}
          />
        </Card>
      </div>
    );
  }

  return (
    <div className="space-y-8 max-w-3xl">
      <PageHeader
        breadcrumb="Banking · Security"
        title="Transaction PIN"
        subtitle="A 4–6 digit PIN is required for every money transfer. Set it once per account."
      />

      <Card>
        <CardHeader title="Pick account" subtitle="PINs are set per-account" className="mb-4" />
        <Select label="Account" value={accountNo} onChange={(e) => setAccountNo(e.target.value)}>
          {accounts.map((a) => (
            <option key={a.accountNo} value={a.accountNo}>
              {a.accountType} · {maskAccountNo(a.accountNo)} · {a.accountNo}
            </option>
          ))}
        </Select>

        <div className="mt-4">
          {checking ? (
            <Badge tone="neutral" dot>Checking PIN status…</Badge>
          ) : pinSet === true ? (
            <Badge tone="success" dot>PIN is already set for this account</Badge>
          ) : pinSet === false ? (
            <Badge tone="warning" dot>No PIN set yet</Badge>
          ) : null}
        </div>
      </Card>

      {pinSet === false && <SetPinCard accountNo={accountNo} onDone={() => setPinSet(true)} />}
      {pinSet === true  && <ChangePinCard accountNo={accountNo} />}

      <SecurityTips />
    </div>
  );
}

// ── Set PIN ──────────────────────────────────────────────────────────────────
function SetPinCard({ accountNo, onDone }) {
  const [submitting, setSubmitting] = useState(false);
  const [show1, setShow1] = useState(false);
  const [show2, setShow2] = useState(false);
  const { register, handleSubmit, watch, reset, setError, formState: { errors } } = useForm({
    mode: 'onTouched',
    defaultValues: { pin: '', confirm: '' },
  });

  const onSubmit = async ({ pin, confirm }) => {
    if (pin !== confirm) {
      toast.error('Both PINs must match.');
      return;
    }
    setSubmitting(true);
    try {
      await pinApi.set(accountNo, pin);
      toast.success('Transaction PIN set successfully.');
      reset();
      onDone?.();
    } catch (err) {
      const applied = applyServerErrors(err, setError);
      if (applied) {
        toast.error('Please fix the highlighted fields and try again.');
      } else {
        toast.error(errorMessage(err, 'Could not set PIN.'));
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Card>
      <CardHeader
        title="Set your transaction PIN"
        subtitle="Choose 4–6 digits. You'll be asked for this PIN before every transfer."
        className="mb-6"
      />
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <Input
          label="New PIN"
          required
          type={show1 ? 'text' : 'password'}
          leftIcon={KeyRound}
          inputMode="numeric"
          autoComplete="new-password"
          placeholder="4–6 digits"
          error={errors.pin?.message}
          rightSlot={
            <button type="button" onClick={() => setShow1((v) => !v)} className="text-accent-mute dark:text-ink-400" aria-label="Toggle">
              {show1 ? <EyeOff size={16} /> : <Eye size={16} />}
            </button>
          }
          {...register('pin', {
            required: 'Required',
            pattern: { value: /^\d{4,6}$/, message: 'PIN must be 4–6 digits' },
          })}
        />
        <Input
          label="Confirm PIN"
          required
          type={show2 ? 'text' : 'password'}
          leftIcon={KeyRound}
          inputMode="numeric"
          autoComplete="new-password"
          error={errors.confirm?.message}
          rightSlot={
            <button type="button" onClick={() => setShow2((v) => !v)} className="text-accent-mute dark:text-ink-400" aria-label="Toggle">
              {show2 ? <EyeOff size={16} /> : <Eye size={16} />}
            </button>
          }
          {...register('confirm', {
            required: 'Required',
            validate: (v) => v === watch('pin') || 'PINs do not match',
          })}
        />
        <div className="flex items-center justify-end pt-4 border-t border-accent-line dark:border-ink-700">
          <Button type="submit" icon={Save} loading={submitting}>Set PIN</Button>
        </div>
      </form>
    </Card>
  );
}

// ── Change PIN ───────────────────────────────────────────────────────────────
function ChangePinCard({ accountNo }) {
  const [submitting, setSubmitting] = useState(false);
  const [showC, setShowC] = useState(false);
  const [showN, setShowN] = useState(false);
  const [showR, setShowR] = useState(false);
  const { register, handleSubmit, watch, reset, setError, formState: { errors } } = useForm({
    mode: 'onTouched',
    defaultValues: { currentPin: '', newPin: '', confirm: '' },
  });

  const onSubmit = async ({ currentPin, newPin, confirm }) => {
    if (newPin !== confirm) {
      toast.error('Both new PINs must match.');
      return;
    }
    setSubmitting(true);
    try {
      await pinApi.change(accountNo, currentPin, newPin);
      toast.success('Transaction PIN updated.');
      reset();
    } catch (err) {
      const applied = applyServerErrors(err, setError);
      if (applied) {
        toast.error('Please fix the highlighted fields and try again.');
      } else {
        toast.error(errorMessage(err, 'Could not change PIN.'));
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Card>
      <CardHeader
        title="Change your transaction PIN"
        subtitle="You'll need your current PIN to confirm the change."
        className="mb-6"
      />
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <Input
          label="Current PIN"
          required
          type={showC ? 'text' : 'password'}
          leftIcon={KeyRound}
          inputMode="numeric"
          autoComplete="current-password"
          error={errors.currentPin?.message}
          rightSlot={
            <button type="button" onClick={() => setShowC((v) => !v)} className="text-accent-mute dark:text-ink-400" aria-label="Toggle">
              {showC ? <EyeOff size={16} /> : <Eye size={16} />}
            </button>
          }
          {...register('currentPin', { required: 'Required' })}
        />
        <Input
          label="New PIN"
          required
          type={showN ? 'text' : 'password'}
          leftIcon={KeyRound}
          inputMode="numeric"
          autoComplete="new-password"
          placeholder="4–6 digits"
          error={errors.newPin?.message}
          rightSlot={
            <button type="button" onClick={() => setShowN((v) => !v)} className="text-accent-mute dark:text-ink-400" aria-label="Toggle">
              {showN ? <EyeOff size={16} /> : <Eye size={16} />}
            </button>
          }
          {...register('newPin', {
            required: 'Required',
            pattern: { value: /^\d{4,6}$/, message: 'PIN must be 4–6 digits' },
            validate: (v) => v !== watch('currentPin') || 'New PIN must differ from current',
          })}
        />
        <Input
          label="Confirm new PIN"
          required
          type={showR ? 'text' : 'password'}
          leftIcon={KeyRound}
          inputMode="numeric"
          error={errors.confirm?.message}
          rightSlot={
            <button type="button" onClick={() => setShowR((v) => !v)} className="text-accent-mute dark:text-ink-400" aria-label="Toggle">
              {showR ? <EyeOff size={16} /> : <Eye size={16} />}
            </button>
          }
          {...register('confirm', {
            required: 'Required',
            validate: (v) => v === watch('newPin') || 'PINs do not match',
          })}
        />
        <div className="flex items-center justify-end pt-4 border-t border-accent-line dark:border-ink-700">
          <Button type="submit" icon={RefreshCw} loading={submitting}>Update PIN</Button>
        </div>
      </form>
    </Card>
  );
}

function SecurityTips() {
  return (
    <Card>
      <CardHeader title="PIN safety tips" className="mb-4" />
      <ul className="space-y-3 text-sm text-accent-slate dark:text-ink-300">
        {[
          'Never share your PIN — bank staff will never ask for it.',
          'Avoid obvious sequences like 1234, 0000 or your date of birth.',
          'Different account → different PIN whenever possible.',
          'PIN is locked for 15 minutes after 5 wrong attempts.',
          'Transfers above ₹1,00,000 require a CSR approval in addition to the PIN.',
        ].map((s) => (
          <li key={s} className="flex items-start gap-2">
            <CheckCircle2 size={14} className="text-accent-success dark:text-green-300 mt-0.5 shrink-0" />
            <span>{s}</span>
          </li>
        ))}
      </ul>
    </Card>
  );
}
