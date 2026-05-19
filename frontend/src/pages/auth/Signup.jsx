import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { Mail, Lock, User, Phone, Calendar, ArrowRight, CheckCircle2 } from 'lucide-react';
import toast from 'react-hot-toast';
import { Input } from '../../components/common/Input';
import { Button } from '../../components/common/Button';
import { useAuth } from '../../context/AuthContext';
import { errorMessage } from '../../api/client';
import { applyServerErrors } from '../../api/serverErrors';
import { AuthLayout } from './Login';

export default function Signup() {
  const navigate = useNavigate();
  const { signup } = useAuth();
  const [submitting, setSubmitting] = useState(false);

  const { register, handleSubmit, watch, setError, formState: { errors } } = useForm({
    mode: 'onTouched',
    defaultValues: {
      fullName: '', email: '', password: '', confirmPassword: '', phoneNumber: '', dateOfBirth: '',
    },
  });

  const onSubmit = async ({ confirmPassword, ...payload }) => {
    setSubmitting(true);
    try {
      await signup(payload);
      toast.success('Account created. Please sign in.');
      navigate('/login', { replace: true });
    } catch (err) {
      const applied = applyServerErrors(err, setError);
      if (applied) {
        toast.error('Please fix the highlighted fields and try again.');
      } else {
        toast.error(errorMessage(err, 'Could not create account. Please review and try again.'));
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AuthLayout
      title="Open your BankSphere account"
      subtitle="It only takes a minute. Bring your phone number, email and date of birth."
    >
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
        <Input
          label="Full name"
          required
          leftIcon={User}
          placeholder="As per your government ID"
          autoComplete="name"
          error={errors.fullName?.message}
          {...register('fullName', {
            required: 'Full name is required',
            minLength: { value: 3, message: 'Must be at least 3 characters' },
          })}
        />
        <Input
          label="Email address"
          required
          type="email"
          leftIcon={Mail}
          placeholder="you@example.com"
          autoComplete="email"
          error={errors.email?.message}
          {...register('email', {
            required: 'Email is required',
            pattern: { value: /^\S+@\S+\.\S+$/, message: 'Enter a valid email' },
          })}
        />
        <div className="grid sm:grid-cols-2 gap-4">
          <Input
            label="Phone number"
            required
            leftIcon={Phone}
            placeholder="10-digit mobile"
            autoComplete="tel"
            error={errors.phoneNumber?.message}
            {...register('phoneNumber', {
              required: 'Phone is required',
              pattern: { value: /^[6-9]\d{9}$/, message: 'Valid 10-digit Indian number required' },
            })}
          />
          <Input
            label="Date of birth"
            required
            type="date"
            leftIcon={Calendar}
            error={errors.dateOfBirth?.message}
            {...register('dateOfBirth', { required: 'Date of birth is required' })}
          />
        </div>
        <Input
          label="Password"
          required
          type="password"
          leftIcon={Lock}
          placeholder="At least 6 characters"
          hint="Use a mix of letters, numbers and a symbol."
          error={errors.password?.message}
          {...register('password', {
            required: 'Password is required',
            minLength: { value: 6, message: 'Must be at least 6 characters' },
          })}
        />
        <Input
          label="Confirm password"
          required
          type="password"
          leftIcon={Lock}
          placeholder="Re-enter password"
          error={errors.confirmPassword?.message}
          {...register('confirmPassword', {
            required: 'Please confirm your password',
            validate: (v) => v === watch('password') || 'Passwords do not match',
          })}
        />

        <p className="text-xs text-accent-mute dark:text-ink-400">
          By creating an account, you agree to the BankSphere Terms of Use and Privacy Policy.
        </p>

        <Button type="submit" loading={submitting} iconRight={ArrowRight} className="w-full">
          Create account
        </Button>
      </form>

      <ul className="mt-6 grid grid-cols-1 gap-2 text-sm text-accent-slate dark:text-ink-300">
        <li className="flex items-center gap-2"><CheckCircle2 size={14} className="text-accent-success" /> Paperless onboarding</li>
        <li className="flex items-center gap-2"><CheckCircle2 size={14} className="text-accent-success" /> No minimum balance to start</li>
        <li className="flex items-center gap-2"><CheckCircle2 size={14} className="text-accent-success" /> Complete KYC inside the app after signup</li>
      </ul>

      <p className="mt-8 text-center text-sm text-accent-slate dark:text-ink-300">
        Already have an account?{' '}
        <Link to="/login" className="link font-semibold">Sign in</Link>
      </p>
    </AuthLayout>
  );
}
