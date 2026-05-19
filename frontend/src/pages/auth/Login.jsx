import { useState } from 'react';
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { Mail, Lock, Eye, EyeOff, ArrowRight, ShieldCheck } from 'lucide-react';
import toast from 'react-hot-toast';
import { Logo } from '../../components/common/Logo';
import { ThemeToggle } from '../../components/common/ThemeToggle';
import { Input } from '../../components/common/Input';
import { Button } from '../../components/common/Button';
import { useAuth } from '../../context/AuthContext';
import { errorMessage } from '../../api/client';
import { roleHomePath } from '../../utils/roleRoutes';

// Marketing copy shown on the brand panel. AuthLayout falls back to this when
// a page doesn't override it (Signup and ForgotPassword reuse it as-is).
const DEFAULT_CATCHCOPY = {
  heading: 'Banking, beautifully done.',
  body: 'Your accounts, your loans, your money — all in one secure app. Bank on your terms, any time of the day.',
  bullets: [
    'Savings, current and deposit accounts opened in minutes',
    'Send money instantly — UPI, NEFT, IMPS and RTGS',
    'Bank-grade security on every transaction, 24×7 access',
  ],
};

/**
 * One sign-in form for every kind of user. The backend authenticates by email
 * + password and returns the user's role inside the JWT; we just navigate to
 * the home page for whatever role came back.
 */
export default function Login() {
  const navigate = useNavigate();
  const location = useLocation();
  const [params] = useSearchParams();
  const { login } = useAuth();
  const [showPassword, setShowPassword] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const { register, handleSubmit, formState: { errors } } = useForm({
    defaultValues: { email: '', password: '', remember: false },
  });

  const reason = params.get('reason');

  const onSubmit = async (values) => {
    setSubmitting(true);
    try {
      const user = await login(values);
      toast.success(`Welcome back, ${user.fullName || user.email}`);
      const from = location.state?.from || roleHomePath(user.role);
      navigate(from, { replace: true });
    } catch (err) {
      toast.error(errorMessage(err, 'Invalid email or password.'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AuthLayout
      title="Sign in to BankSphere"
      subtitle="Enter your registered email and password — we'll route you to the right dashboard."
    >
      {reason === 'session_expired' && (
        <div className="mb-5 chip bg-amber-50 text-accent-warning w-full justify-start">
          <ShieldCheck size={14} /> Your session expired. Please sign in again.
        </div>
      )}

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
        <Input
          label="Email address"
          required
          type="email"
          placeholder="you@example.com"
          leftIcon={Mail}
          autoComplete="email"
          error={errors.email?.message}
          {...register('email', {
            required: 'Email is required',
            pattern: { value: /^\S+@\S+\.\S+$/, message: 'Enter a valid email' },
          })}
        />

        <Input
          label="Password"
          required
          type={showPassword ? 'text' : 'password'}
          placeholder="Enter your password"
          leftIcon={Lock}
          autoComplete="current-password"
          error={errors.password?.message}
          rightSlot={
            <button
              type="button"
              onClick={() => setShowPassword((v) => !v)}
              className="text-accent-mute dark:text-ink-400 hover:text-accent-ink dark:text-ink-100 transition"
              aria-label={showPassword ? 'Hide password' : 'Show password'}
            >
              {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
            </button>
          }
          {...register('password', { required: 'Password is required' })}
        />

        <div className="flex items-center justify-between text-sm">
          <label className="inline-flex items-center gap-2 cursor-pointer select-none">
            <input
              type="checkbox"
              className="accent-brand-700 h-4 w-4"
              {...register('remember')}
            />
            <span className="text-accent-slate dark:text-ink-300">Remember me on this device</span>
          </label>
          <Link to="/forgot-password" className="link">Forgot password?</Link>
        </div>

        <Button type="submit" loading={submitting} iconRight={ArrowRight} className="w-full">
          Sign in
        </Button>
      </form>

      <p className="mt-8 text-center text-sm text-accent-slate dark:text-ink-300">
        New to BankSphere?{' '}
        <Link to="/signup" className="link font-semibold">Create an account</Link>
      </p>
    </AuthLayout>
  );
}

/**
 * Shared split-screen layout for the auth flow. Signup and ForgotPassword
 * reuse this as well, so the brand panel stays consistent across the three
 * unauthenticated screens.
 */
export function AuthLayout({
  title, subtitle, children,
  accent = 'from-brand-700 to-brand-900',
  catchcopy = DEFAULT_CATCHCOPY,
}) {
  return (
    <div className="min-h-screen grid lg:grid-cols-[1.1fr_1fr]">
      <div className={`hidden lg:flex relative overflow-hidden bg-gradient-to-br ${accent} text-white p-12`}>
        <div className="relative z-10 max-w-md self-center">
          <Link to="/" aria-label="BankSphere home" className="inline-block focus-ring rounded-md">
            <Logo tone="white" />
          </Link>
          <h1 className="mt-10 font-display text-4xl font-extrabold leading-tight">
            {catchcopy.heading}
          </h1>
          <p className="mt-4 text-white/80 leading-relaxed">
            {catchcopy.body}
          </p>
          <ul className="mt-10 space-y-3 text-sm text-white/85">
            {catchcopy.bullets.map((b) => <Bullet key={b}>{b}</Bullet>)}
          </ul>
        </div>
        <Blob />
      </div>

      <div className="flex flex-col relative">
        <header className="flex items-center justify-between p-6 lg:p-4 lg:absolute lg:top-0 lg:right-0 lg:left-0 lg:z-10 lg:border-0 border-b border-accent-line dark:border-ink-700">
          <Link to="/" className="lg:hidden"><Logo /></Link>
          <span className="lg:hidden" />
          <ThemeToggle className="ml-auto" />
        </header>
        <main className="flex-1 grid place-items-center px-6 py-10">
          <div className="w-full max-w-md">
            <h2 className="font-display text-3xl font-extrabold dark:text-ink-100">{title}</h2>
            <p className="mt-2 text-accent-slate dark:text-ink-300">{subtitle}</p>
            <div className="mt-8">{children}</div>
          </div>
        </main>
        <footer className="px-6 py-4 text-xs text-accent-mute dark:text-ink-400 text-center border-t border-accent-line dark:border-ink-700">
          © {new Date().getFullYear()} BankSphere — for educational use only.
        </footer>
      </div>
    </div>
  );
}

function Bullet({ children }) {
  return (
    <li className="flex items-start gap-2">
      <span className="mt-1.5 h-1.5 w-1.5 rounded-full bg-accent-gold" />
      <span>{children}</span>
    </li>
  );
}

function Blob() {
  return (
    <>
      <div className="absolute -bottom-24 -left-24 h-96 w-96 rounded-full bg-white/5 blur-3xl" aria-hidden />
      <div className="absolute -bottom-32 right-0 h-72 w-72 rounded-full bg-accent-gold/15 blur-3xl" aria-hidden />
      <div className="absolute top-10 right-10 h-48 w-48 rounded-full bg-white/5 blur-2xl" aria-hidden />
    </>
  );
}
