import { Link } from 'react-router-dom';
import { ShieldAlert } from 'lucide-react';
import { ThemeToggle } from '../../components/common/ThemeToggle';

export default function Unauthorized() {
  return (
    <div className="min-h-screen grid place-items-center bg-accent-surface/40 dark:bg-ink-900 px-6 relative">
      <div className="absolute top-4 right-4"><ThemeToggle /></div>
      <div className="card p-10 text-center max-w-md">
        <div className="mx-auto h-14 w-14 rounded-full bg-red-50 text-accent-danger grid place-items-center">
          <ShieldAlert size={26} />
        </div>
        <h1 className="mt-5 font-display text-2xl font-extrabold">Access denied</h1>
        <p className="mt-2 text-accent-slate dark:text-ink-300">
          Your role does not have permission to view this page. If you believe this is a mistake,
          please contact your administrator.
        </p>
        <div className="mt-6 flex justify-center gap-3">
          <Link to="/" className="btn-secondary">Go home</Link>
          <Link to="/login" className="btn-primary">Sign in as another user</Link>
        </div>
      </div>
    </div>
  );
}
