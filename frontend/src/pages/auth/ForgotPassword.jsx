import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import {
  Mail, KeyRound, Lock, Eye, EyeOff, ArrowLeft, ArrowRight, CheckCircle2,
  RefreshCw,
} from 'lucide-react';
import toast from 'react-hot-toast';
import { Input } from '../../components/common/Input';
import { Button } from '../../components/common/Button';
import { authApi } from '../../api/auth';
import { errorMessage } from '../../api/client';
import { applyServerErrors } from '../../api/serverErrors';
import { AuthLayout } from './Login';

/**
 * Three-step password reset:
 *   1. Email      — request the OTP
 *   2. OTP        — verify OTP, mint a single-use reset token
 *   3. Password   — set the new password
 *
 * The reset token from step 2 is held in component state only and never
 * persisted — once the password is set the session is forgotten.
 */
const STEPS = ['email', 'otp', 'password'];

export default function ForgotPassword() {
  const [step, setStep] = useState('email');
  const [email, setEmail] = useState('');
  const [resetToken, setResetToken] = useState('');

  const stepIndex = STEPS.indexOf(step);

  return (
    <AuthLayout
      title="Reset your password"
      subtitle="We'll email a 6-digit code to verify it's really you, then let you set a new password."
    >
      <Link to="/login" className="inline-flex items-center gap-1.5 text-sm text-accent-mute dark:text-ink-400 hover:text-accent-ink dark:hover:text-ink-100 mb-5">
        <ArrowLeft size={14} /> Back to sign in
      </Link>

      <Stepper current={stepIndex} />

      <div className="mt-8">
        {step === 'email' && (
          <EmailStep
            onSent={(addr) => { setEmail(addr); setStep('otp'); }}
          />
        )}
        {step === 'otp' && (
          <OtpStep
            email={email}
            onBack={() => setStep('email')}
            onVerified={(tok) => { setResetToken(tok); setStep('password'); }}
          />
        )}
        {step === 'password' && (
          <PasswordStep
            email={email}
            resetToken={resetToken}
          />
        )}
      </div>
    </AuthLayout>
  );
}

//  Step 1 — email
function EmailStep({ onSent }) {
  const [submitting, setSubmitting] = useState(false);
  const { register, handleSubmit, setError, formState: { errors } } = useForm({
    mode: 'onTouched',
    defaultValues: { email: '' },
  });

  const onSubmit = async (v) => {
    const email = v.email.trim().toLowerCase();
    setSubmitting(true);
    try {
      await authApi.forgotPassword(email);
      toast.success('A 6-digit code has been sent to your email.');
      onSent(email);
    } catch (err) {
      const status = err?.response?.status;
      // 404 = no account with this email → highlight the field and tell the user.
      if (status === 404) {
        setError('email', {
          type: 'server',
          message: 'No account is registered with this email. Please provide a valid email.',
        });
        toast.error('We couldn’t find an account with that email.');
        return;
      }
      // Other 4xx with a body → route field-level errors to the form.
      const applied = applyServerErrors(err, setError);
      if (applied) {
        toast.error('Please fix the highlighted fields and try again.');
      } else {
        toast.error(errorMessage(err, 'Could not start password reset.'));
      }
    } finally { setSubmitting(false); }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
      <Input
        label="Email address"
        required
        type="email"
        placeholder="you@example.com"
        leftIcon={Mail}
        autoComplete="email"
        autoFocus
        error={errors.email?.message}
        {...register('email', {
          required: 'Email is required',
          pattern: { value: /^\S+@\S+\.\S+$/, message: 'Enter a valid email' },
        })}
      />
      <Button type="submit" loading={submitting} iconRight={ArrowRight} className="w-full">
        Send verification code
      </Button>
      <p className="text-xs text-accent-mute dark:text-ink-400 text-center">
        Enter the email you used to sign up. We’ll send a 6-digit code to verify it’s you.
      </p>
    </form>
  );
}

//  Step 2 — OTP
function OtpStep({ email, onBack, onVerified }) {
  const [submitting, setSubmitting] = useState(false);
  const [resending, setResending]   = useState(false);
  const [secondsLeft, setSecondsLeft] = useState(60);
  const { register, handleSubmit, setError, formState: { errors } } = useForm({
    mode: 'onTouched',
    defaultValues: { otp: '' },
  });

  // Countdown timer — disables "Resend code" until it hits zero.
  useEffect(() => {
    if (secondsLeft <= 0) return;
    const t = setTimeout(() => setSecondsLeft(secondsLeft - 1), 1000);
    return () => clearTimeout(t);
  }, [secondsLeft]);

  const onSubmit = async (v) => {
    setSubmitting(true);
    try {
      const data = await authApi.verifyOtp({ email, otp: v.otp });
      if (!data?.resetToken) throw new Error('No reset token returned');
      toast.success('Code verified.');
      onVerified(data.resetToken);
    } catch (err) {
      const applied = applyServerErrors(err, setError);
      if (applied) {
        toast.error('Please fix the highlighted fields and try again.');
      } else {
        toast.error(errorMessage(err, 'Could not verify the OTP.'));
      }
    } finally { setSubmitting(false); }
  };

  const resend = async () => {
    setResending(true);
    try {
      await authApi.forgotPassword(email);
      toast.success('A new code has been sent to your inbox.');
      setSecondsLeft(60);
    } catch (err) {
      toast.error(errorMessage(err, 'Could not resend the OTP.'));
    } finally { setResending(false); }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
      <p className="text-sm text-accent-slate dark:text-ink-300">
        We sent a 6-digit code to <span className="font-semibold dark:text-ink-100">{email}</span>.
        Enter it below to continue. The code expires in 10 minutes.
      </p>
      <Input
        label="Verification code"
        required
        leftIcon={KeyRound}
        inputMode="numeric"
        maxLength={6}
        placeholder="6-digit code"
        autoFocus
        error={errors.otp?.message}
        {...register('otp', {
          required: 'OTP is required',
          pattern: { value: /^[0-9]{6}$/, message: '6 digits required' },
        })}
      />
      <Button type="submit" loading={submitting} iconRight={ArrowRight} className="w-full">
        Verify code
      </Button>
      <div className="flex items-center justify-between text-sm">
        <button type="button" onClick={onBack} className="text-accent-mute dark:text-ink-400 hover:text-accent-ink dark:hover:text-ink-100">
          Wrong email?
        </button>
        <button
          type="button"
          onClick={resend}
          disabled={secondsLeft > 0 || resending}
          className="inline-flex items-center gap-1 text-brand-700 dark:text-brand-300 hover:underline disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <RefreshCw size={12} />
          {secondsLeft > 0 ? `Resend in ${secondsLeft}s` : (resending ? 'Sending…' : 'Resend code')}
        </button>
      </div>
    </form>
  );
}

//  Step 3 — new password
function PasswordStep({ email, resetToken }) {
  const navigate = useNavigate();
  const [submitting, setSubmitting] = useState(false);
  const [show, setShow] = useState(false);
  const { register, handleSubmit, watch, setError, formState: { errors } } = useForm({
    mode: 'onTouched',
    defaultValues: { newPassword: '', confirm: '' },
  });

  const pw = watch('newPassword');

  const onSubmit = async (v) => {
    setSubmitting(true);
    try {
      await authApi.resetPassword({
        email,
        resetToken,
        newPassword: v.newPassword,
      });
      toast.success('Password updated — please sign in.');
      navigate('/login', { replace: true });
    } catch (err) {
      const applied = applyServerErrors(err, setError);
      if (applied) {
        toast.error('Please fix the highlighted fields and try again.');
      } else {
        toast.error(errorMessage(err, 'Could not update password.'));
      }
    } finally { setSubmitting(false); }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
      <div className="flex items-center gap-2 text-sm text-accent-success">
        <CheckCircle2 size={16} /> Identity verified
      </div>
      <Input
        label="New password"
        required
        type={show ? 'text' : 'password'}
        leftIcon={Lock}
        placeholder="At least 8 characters"
        autoFocus
        rightSlot={
          <button type="button" onClick={() => setShow((v) => !v)} aria-label={show ? 'Hide password' : 'Show password'}
                  className="text-accent-mute dark:text-ink-400">
            {show ? <EyeOff size={16} /> : <Eye size={16} />}
          </button>
        }
        error={errors.newPassword?.message}
        {...register('newPassword', {
          required: 'Password is required',
          minLength: { value: 8, message: 'At least 8 characters' },
        })}
      />
      <Input
        label="Confirm new password"
        required
        type={show ? 'text' : 'password'}
        leftIcon={Lock}
        placeholder="Re-enter the password"
        error={errors.confirm?.message}
        {...register('confirm', {
          required: 'Please confirm your password',
          validate: (v) => v === pw || 'Passwords do not match',
        })}
      />
      <Button type="submit" loading={submitting} iconRight={ArrowRight} className="w-full">
        Update password
      </Button>
    </form>
  );
}

//  Stepper
function Stepper({ current }) {
  const labels = ['Email', 'Verify', 'New password'];
  return (
    <ol className="flex items-center gap-2">
      {labels.map((label, i) => {
        const active = i === current;
        const done   = i < current;
        return (
          <li key={label} className="flex items-center gap-2 flex-1">
            <span className={`h-7 w-7 rounded-full grid place-items-center text-xs font-semibold border ${
              done   ? 'bg-brand-700 border-brand-700 text-white'
              : active ? 'border-brand-700 text-brand-700 dark:border-brand-300 dark:text-brand-300'
              : 'border-accent-line dark:border-ink-700 text-accent-mute dark:text-ink-400'
            }`}>
              {done ? '✓' : i + 1}
            </span>
            <span className={`text-xs ${active ? 'font-semibold dark:text-ink-100' : 'text-accent-mute dark:text-ink-400'}`}>
              {label}
            </span>
            {i < labels.length - 1 && <span className="flex-1 h-px bg-accent-line dark:bg-ink-700" />}
          </li>
        );
      })}
    </ol>
  );
}
