import { Link } from 'react-router-dom';
import { Eye, EyeOff, Copy, ArrowRight } from 'lucide-react';
import { useState } from 'react';
import toast from 'react-hot-toast';
import { formatINR, maskAccountNo } from '../../utils/format';
import { StatusBadge } from '../common/Badge';

export function AccountSummaryCard({ account }) {
  const [show, setShow] = useState(false);
  if (!account) return null;

  const copy = () => {
    navigator.clipboard?.writeText(account.accountNo);
    toast.success('Account number copied');
  };

  return (
    <div className="relative overflow-hidden rounded-2xl bg-gradient-to-br from-brand-700 to-brand-900 text-white shadow-elevated p-7">
      <div className="absolute -right-16 -top-16 h-48 w-48 rounded-full bg-accent-gold/20 blur-3xl" />
      <div className="absolute -left-10 -bottom-10 h-40 w-40 rounded-full bg-brand-500/30 blur-3xl" />

      <div className="relative">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-xs uppercase tracking-widest opacity-70">{account.accountType} Account</p>
            <p className="mt-1 text-sm opacity-80">{account.branchCode} · {account.ifscCode || 'IFSC N/A'}</p>
          </div>
          <StatusBadge status={account.status} className="bg-white/15 text-white border border-white/20" />
        </div>

        <div className="mt-10">
          <p className="text-xs uppercase tracking-widest opacity-70">Available balance</p>
          <p className="mt-1 font-display text-4xl font-extrabold">{formatINR(account.balance)}</p>
        </div>

        <div className="mt-8 flex items-end justify-between gap-4">
          <div>
            <div className="flex items-center gap-2 text-sm">
              <span className="font-mono opacity-90">{show ? account.accountNo : maskAccountNo(account.accountNo)}</span>
              <button onClick={() => setShow((v) => !v)} className="opacity-70 hover:opacity-100 transition" aria-label="Toggle visibility">
                {show ? <EyeOff size={14} /> : <Eye size={14} />}
              </button>
              <button onClick={copy} className="opacity-70 hover:opacity-100 transition" aria-label="Copy">
                <Copy size={14} />
              </button>
            </div>
            <p className="mt-1 text-xs opacity-70">Min balance: {formatINR(account.minimumBalance)}</p>
          </div>
          <Link to={`/app/accounts/${account.accountNo}`} className="btn bg-white text-brand-700 hover:bg-brand-50">
            View details <ArrowRight size={14} />
          </Link>
        </div>
      </div>
    </div>
  );
}
