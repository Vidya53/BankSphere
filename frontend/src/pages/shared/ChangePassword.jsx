import { useState, useMemo } from 'react';
import { useForm } from 'react-hook-form';
import { useNavigate } from 'react-router-dom';
import { Lock, Eye, EyeOff, ShieldCheck, KeyRound, CheckCircle2, XCircle } from 'lucide-react';
import toast from 'react-hot-toast';
import { PageHeader } from '../../components/layout/PageHeader';
import { Card, CardHeader } from '../../components/common/Card';
import { Input } from '../../components/common/Input';
import { Button } from '../../components/common/Button';
import { passwordApi } from '../../api/preferences';
import { errorMessage } from '../../api/client';
import { applyServerErrors } from '../../api/serverErrors';

const RULES = [
  { id: 'length',   label: 'At least 8 characters',                test: (v) => v.length >= 8 },
  { id: 'upper',    label: 'Contains an uppercase letter',         test: (v) => /[A-Z]/.test(v) },
  { id: 'lower',    label: 'Contains a lowercase letter',          test: (v) => /[a-z]/.test(v) },
  { id: 'digit',    label: 'Contains a number',                    test: (v) => /\d/.test(v) },
  { id: 'symbol',   label: 'Contains a special character (! @ # …)', test: (v) => /[^A-Za-z0-9]/.test(v) },
];

function scorePassword(pw) {
  if (!pw) return 0;
  return RULES.reduce((s, r) => s + (r.test(pw) ? 1 : 0), 0);
}

function strengthLabel(score) {
  if (score <= 1) return { label: 'Very weak',  tone: 'text-accent-danger',  bar: 'bg-accent-danger' };
  if (score === 2) return { label: 'Weak',      tone: 'text-accent-danger',  bar: 'bg-accent-danger' };
  if (score === 3) return { label: 'Fair',      tone: 'text-accent-warning', bar: 'bg-accent-warning' };
  if (score === 4) return { label: 'Strong',    tone: 'text-accent-success', bar: 'bg-accent-success' };
  return { label: 'Excellent', tone: 'text-accent-success', bar: 'bg-accent-success' };
}

export default function ChangePassword() {
  const navigate = useNavigate();
  const [showCurrent, setShowCurrent] = useState(false);
  const [showNew, setShowNew] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const { register, handleSubmit, watch, reset, setError, formState: { errors } } = useForm({
    mode: 'onTouched',
    defaultValues: { current: '', next: '', confirm: '' },
  });

  // Backend DTO uses currentPassword/newPassword — local form fields use shorter
  // names. Translate before forwarding to react-hook-form's setError.
  const FIELD_MAP = { currentPassword: 'current', newPassword: 'next', confirmPassword: 'confirm' };
  const mappedSetError = (field, opts) => setError(FIELD_MAP[field] || field, opts);

  const next = watch('next');
  const score = useMemo(() => scorePassword(next), [next]);
  const strength = strengthLabel(score);

  const onSubmit = async ({ current, next, confirm }) => {
    if (next !== confirm) {
      toast.error('New password and confirmation do not match.');
      return;
    }
    if (score < 3) {
      toast.error('Please choose a stronger password.');
      return;
    }
    setSubmitting(true);
    try {
      await passwordApi.change({ currentPassword: current, newPassword: next });
      toast.success('Password changed successfully');
      reset();
      navigate(-1);
    } catch (err) {
      const applied = applyServerErrors(err, mappedSetError);
      if (applied) {
        toast.error('Please fix the highlighted fields and try again.');
      } else {
        toast.error(errorMessage(err, 'Could not change password.'));
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-8 max-w-3xl">
      <PageHeader
        breadcrumb="Account · Security"
        title="Change password"
        subtitle="Use a strong, unique password you don't reuse on other sites. Your new password will be effective immediately."
      />

      <div className="grid lg:grid-cols-[1.4fr_1fr] gap-6">
        <Card>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
            <Input
              label="Current password"
              required
              type={showCurrent ? 'text' : 'password'}
              leftIcon={KeyRound}
              autoComplete="current-password"
              error={errors.current?.message}
              rightSlot={
                <button type="button" onClick={() => setShowCurrent((v) => !v)}
                        className="text-accent-mute dark:text-ink-400 hover:text-accent-ink dark:text-ink-100 transition" aria-label="Toggle visibility">
                  {showCurrent ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              }
              {...register('current', { required: 'Current password is required' })}
            />

            <Input
              label="New password"
              required
              type={showNew ? 'text' : 'password'}
              leftIcon={Lock}
              autoComplete="new-password"
              error={errors.next?.message}
              rightSlot={
                <button type="button" onClick={() => setShowNew((v) => !v)}
                        className="text-accent-mute dark:text-ink-400 hover:text-accent-ink dark:text-ink-100 transition" aria-label="Toggle visibility">
                  {showNew ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              }
              {...register('next', {
                required: 'New password is required',
                minLength: { value: 8, message: 'At least 8 characters' },
              })}
            />

            {/* Strength meter */}
            {next && (
              <div className="-mt-3">
                <div className="flex items-center justify-between mb-1.5">
                  <span className="text-xs text-accent-mute dark:text-ink-400">Password strength</span>
                  <span className={`text-xs font-semibold ${strength.tone}`}>{strength.label}</span>
                </div>
                <div className="grid grid-cols-5 gap-1.5">
                  {Array.from({ length: 5 }).map((_, i) => (
                    <span
                      key={i}
                      className={`h-1.5 rounded-full ${i < score ? strength.bar : 'bg-accent-line/60'}`}
                    />
                  ))}
                </div>
              </div>
            )}

            <Input
              label="Confirm new password"
              required
              type={showConfirm ? 'text' : 'password'}
              leftIcon={Lock}
              error={errors.confirm?.message}
              rightSlot={
                <button type="button" onClick={() => setShowConfirm((v) => !v)}
                        className="text-accent-mute dark:text-ink-400 hover:text-accent-ink dark:text-ink-100 transition" aria-label="Toggle visibility">
                  {showConfirm ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              }
              {...register('confirm', {
                required: 'Please confirm your new password',
                validate: (v) => v === watch('next') || 'Passwords do not match',
              })}
            />

            <div className="flex items-center justify-end gap-2 pt-4 border-t border-accent-line dark:border-ink-700">
              <Button type="button" variant="ghost" onClick={() => navigate(-1)}>Cancel</Button>
              <Button type="submit" icon={ShieldCheck} loading={submitting}>Update password</Button>
            </div>
          </form>
        </Card>

        <Card>
          <CardHeader title="Password requirements" subtitle="A strong password meets all of these" className="mb-4" />
          <ul className="space-y-2.5 text-sm">
            {RULES.map((r) => {
              const ok = next && r.test(next);
              return (
                <li key={r.id} className="flex items-start gap-2">
                  {ok ? (
                    <CheckCircle2 size={16} className="text-accent-success mt-0.5 shrink-0" />
                  ) : (
                    <XCircle size={16} className="text-accent-mute dark:text-ink-400 mt-0.5 shrink-0" />
                  )}
                  <span className={ok ? 'text-accent-success' : 'text-accent-slate dark:text-ink-300'}>{r.label}</span>
                </li>
              );
            })}
          </ul>
          <div className="mt-6 rounded-xl bg-brand-50 border border-brand-100 p-4 text-sm text-accent-slate dark:text-ink-300">
            <p className="font-semibold text-brand-700">Tip</p>
            <p className="mt-1">
              Consider using a long passphrase like four random words plus a number and symbol.
              Easier to remember, harder to crack.
            </p>
          </div>
        </Card>
      </div>
    </div>
  );
}
