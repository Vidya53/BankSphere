import { useEffect, useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link } from 'react-router-dom';
import {
  Landmark, Calculator, ArrowRight, TrendingUp, CheckCircle2, XCircle,
  Plus, RefreshCw,
} from 'lucide-react';
import toast from 'react-hot-toast';
import { PageHeader } from '../../components/layout/PageHeader';
import { Card, CardHeader } from '../../components/common/Card';
import { Input, Select } from '../../components/common/Input';
import { Button } from '../../components/common/Button';
import { Modal } from '../../components/common/Modal';
import { Skeleton } from '../../components/common/Skeleton';
import { EmptyState } from '../../components/common/EmptyState';
import { Badge, StatusBadge } from '../../components/common/Badge';
import { loanApi } from '../../api/loan';
import { customerApi } from '../../api/customer';
import { accountApi } from '../../api/account';
import { errorMessage } from '../../api/client';
import { formatINR } from '../../utils/format';

const PRODUCTS = [
  { code: 'PERSONAL',  name: 'Personal Loan',  rate: 12.5, max: 1_500_000,  tenure: '12–60 months',  icon: Landmark },
  { code: 'HOME',      name: 'Home Loan',      rate:  8.4, max: 50_000_000, tenure: '60–240 months', icon: Landmark },
  { code: 'CAR',       name: 'Auto Loan',      rate:  9.6, max:  2_500_000, tenure: '12–84 months',  icon: Landmark },
  { code: 'EDUCATION', name: 'Education Loan', rate: 10.2, max:  4_000_000, tenure: '12–120 months', icon: Landmark },
];
const PRODUCT_BY_CODE = Object.fromEntries(PRODUCTS.map((p) => [p.code, p]));

export default function Loans() {
  const [profile, setProfile] = useState(null);
  const [loans, setLoans]     = useState([]);
  const [loading, setLoading] = useState(true);
  const [applyOpen, setApplyOpen] = useState(false);
  const [applyProduct, setApplyProduct] = useState(null);

  const reload = async () => {
    setLoading(true);
    try {
      // Resolve the customer profile first; my-loans needs the customerNo.
      const me = await customerApi.getMyProfile().catch(() => null);
      setProfile(me);
      if (me?.customerNo) {
        const list = await loanApi.byCustomer(me.customerNo).catch(() => []);
        setLoans(Array.isArray(list) ? list : []);
      } else {
        setLoans([]);
      }
    } finally { setLoading(false); }
  };

  useEffect(() => { reload(); }, []);

  const openApply = (product) => {
    if (!profile?.customerNo) {
      toast.error('Complete your customer profile (KYC) before applying for a loan.');
      return;
    }
    setApplyProduct(product);
    setApplyOpen(true);
  };

  return (
    <div className="space-y-8">
      <PageHeader
        breadcrumb="Banking"
        title="Loans"
        subtitle="Browse our products, check your eligibility instantly, and track your EMIs."
      />

      {/* Product showcase — each card opens the apply modal */}
      <section className="grid sm:grid-cols-2 lg:grid-cols-4 gap-5">
        {PRODUCTS.map(({ icon: Icon, ...p }) => (
          <div key={p.code} className="card-hover p-6 flex flex-col">
            <span className="h-11 w-11 rounded-full bg-brand-50 text-brand-700 grid place-items-center">
              <Icon size={20} />
            </span>
            <h3 className="mt-5 font-semibold text-accent-ink dark:text-ink-100">{p.name}</h3>
            <p className="mt-1 text-xs text-accent-mute dark:text-ink-400 uppercase tracking-wider">From {p.rate}% p.a.</p>
            <dl className="mt-4 space-y-1.5 text-sm">
              <div className="flex justify-between"><dt className="text-accent-mute dark:text-ink-400">Max amount</dt><dd className="font-semibold">{formatINR(p.max)}</dd></div>
              <div className="flex justify-between"><dt className="text-accent-mute dark:text-ink-400">Tenure</dt><dd className="font-semibold">{p.tenure}</dd></div>
            </dl>
            <button
              onClick={() => openApply(p)}
              className="mt-5 inline-flex items-center gap-1 text-sm font-semibold text-brand-700 hover:underline"
            >
              Apply now <ArrowRight size={14} />
            </button>
          </div>
        ))}
      </section>

      <section className="grid lg:grid-cols-[1.4fr_1fr] gap-6">
        <EligibilityCalculator defaultCustomerNo={profile?.customerNo} />
        <Card>
          <CardHeader title="Why borrow with BankSphere" className="mb-4" />
          <ul className="space-y-3 text-sm text-accent-slate dark:text-ink-300">
            {[
              'Decisions in minutes — fully digital eligibility checks',
              'No prepayment penalty after the first six EMIs',
              'Flexible EMI dates aligned with your salary credit',
              'Live tracking of disbursement and repayments',
            ].map((s) => (
              <li key={s} className="flex items-start gap-2">
                <CheckCircle2 size={14} className="text-accent-success mt-0.5 shrink-0" />
                <span>{s}</span>
              </li>
            ))}
          </ul>
          <div className="mt-6 rounded-xl bg-gradient-to-r from-brand-50 to-amber-50 dark:from-brand-900/20 dark:to-amber-900/20 p-4">
            <div className="flex items-center gap-3">
              <TrendingUp size={18} className="text-brand-700 dark:text-brand-300" />
              <p className="text-sm font-semibold dark:text-ink-100">Build your credit profile</p>
            </div>
            <p className="text-xs text-accent-slate dark:text-ink-300 mt-1.5">
              A timely-paid loan adds to your credit score and unlocks better terms next time.
            </p>
          </div>
        </Card>
      </section>

      <Card>
        <CardHeader
          title="My loans"
          subtitle="Active, disbursed and closed loans"
          action={<Button size="sm" variant="ghost" icon={RefreshCw} onClick={reload}>Refresh</Button>}
          className="mb-4"
        />
        {loading ? (
          <div className="space-y-2">
            {Array.from({ length: 3 }).map((_, i) => <Skeleton key={i} className="h-14" />)}
          </div>
        ) : loans.length === 0 ? (
          <EmptyState
            icon={Landmark}
            title="No loans yet"
            message="Apply for a loan from the products above. Once approved, your loans will appear here with their EMI schedule and payment history."
          />
        ) : (
          <ul className="divide-y divide-accent-line dark:divide-ink-700/60">
            {loans.map((loan) => {
              const product = PRODUCT_BY_CODE[loan.loanType] || { name: loan.loanType };
              return (
                <li key={loan.loanId} className="py-4 flex items-center justify-between gap-4">
                  <div className="min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <p className="font-semibold dark:text-ink-100">{product.name}</p>
                      <span className="text-xs font-mono text-accent-mute dark:text-ink-400">
                        LN-{String(loan.loanId).padStart(7, '0')}
                      </span>
                      <StatusBadge status={loan.status} />
                    </div>
                    <p className="text-xs text-accent-mute dark:text-ink-400 mt-1">
                      {formatINR(loan.amount)} @ {loan.interestRate}% · {loan.tenureMonths} months
                      {loan.emiAmount ? <> · EMI {formatINR(loan.emiAmount)}</> : null}
                      {loan.nextDueDate ? <> · next due {loan.nextDueDate}</> : null}
                    </p>
                  </div>
                  <Link to={`/app/loans/${loan.loanId}`} className="btn-secondary text-sm">View</Link>
                </li>
              );
            })}
          </ul>
        )}
      </Card>

      <ApplyLoanModal
        open={applyOpen}
        onClose={() => setApplyOpen(false)}
        product={applyProduct}
        profile={profile}
        onApplied={() => { setApplyOpen(false); reload(); }}
      />
    </div>
  );
}

//  Apply for loan
function ApplyLoanModal({ open, onClose, product, profile, onApplied }) {
  const [submitting, setSubmitting] = useState(false);
  const [accounts, setAccounts] = useState([]);

  const { register, handleSubmit, reset, watch, formState: { errors } } = useForm({
    defaultValues: { amount: 500_000, tenureMonths: 36, income: 75_000, accountId: '' },
  });

  // Pull the customer's accounts to populate the disbursement-account dropdown.
  // Backend requires accountId; we restrict to ACTIVE accounts only.
  useEffect(() => {
    if (!open) return;
    accountApi.myAccounts()
      .then((a) => setAccounts((a || []).filter((x) => x.status === 'ACTIVE')))
      .catch(() => setAccounts([]));
  }, [open]);

  useEffect(() => { if (!open) reset(); }, [open]); // eslint-disable-line

  const amount = Number(watch('amount')) || 0;
  const tenure = Number(watch('tenureMonths')) || 1;
  const monthlyRate = (product?.rate || 12) / 12 / 100;
  const indicativeEmi = useMemo(() => {
    if (!amount || !tenure || !monthlyRate) return 0;
    return amount * monthlyRate * Math.pow(1 + monthlyRate, tenure) /
           (Math.pow(1 + monthlyRate, tenure) - 1);
  }, [amount, tenure, monthlyRate]);

  const onSubmit = async (v) => {
    if (!profile?.customerNo) {
      toast.error('Customer profile not found.');
      return;
    }
    if (!v.accountId) {
      toast.error('Select the account where you want the loan disbursed.');
      return;
    }
    setSubmitting(true);
    try {
      const created = await loanApi.apply({
        customerId:   profile.customerNo,
        accountId:    v.accountId,
        loanType:     product.code,
        amount:       Number(v.amount),
        tenureMonths: Number(v.tenureMonths),
        income:       Number(v.income),
      });
      toast.success(`Application submitted: LN-${String(created.loanId).padStart(7, '0')}`);
      onApplied?.();
    } catch (err) {
      toast.error(errorMessage(err, 'Could not submit loan application.'));
    } finally { setSubmitting(false); }
  };

  if (!product) return null;
  return (
    <Modal
      open={open}
      onClose={onClose}
      title={`Apply for ${product.name}`}
      description={`Rate from ${product.rate}% p.a. · Max ${formatINR(product.max)} · ${product.tenure}`}
      size="lg"
      footer={
        <div className="flex justify-end gap-2">
          <Button variant="ghost" onClick={onClose}>Cancel</Button>
          <Button icon={Plus} loading={submitting} onClick={handleSubmit(onSubmit)}>Submit application</Button>
        </div>
      }
    >
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
        <div className="grid sm:grid-cols-2 gap-4">
          <Input label="Customer number" value={profile?.customerNo || ''} disabled hint="Pulled from your profile" />
          <Select label="Disburse to account" {...register('accountId', { required: 'Select an account' })} error={errors.accountId?.message}>
            <option value="">Select an active account…</option>
            {accounts.map((a) => (
              <option key={a.accountNo} value={a.accountNo}>
                {a.accountNo} · {a.accountType} · {formatINR(a.balance ?? 0)}
              </option>
            ))}
          </Select>
        </div>

        <div className="grid sm:grid-cols-3 gap-4">
          <Input
            label="Amount (₹)" type="number"
            error={errors.amount?.message}
            {...register('amount', {
              required: 'Required',
              min: { value: 10_000, message: 'Minimum ₹10,000' },
              max: { value: product.max, message: `Maximum ${formatINR(product.max)}` },
            })}
          />
          <Input
            label="Tenure (months)" type="number"
            error={errors.tenureMonths?.message}
            {...register('tenureMonths', {
              required: 'Required',
              min: { value: 6,   message: 'Min 6 months' },
              max: { value: 240, message: 'Max 240 months' },
            })}
          />
          <Input
            label="Monthly income (₹)" type="number"
            hint="We use this to assess EMI affordability."
            error={errors.income?.message}
            {...register('income', { required: 'Required', min: { value: 10_000, message: 'Required' } })}
          />
        </div>

        <div className="card p-4 bg-brand-50/40 dark:bg-brand-900/15 border-brand-100 dark:border-brand-900/40">
          <p className="text-xs uppercase tracking-widest text-accent-mute dark:text-ink-400">Indicative monthly EMI</p>
          <p className="font-display text-2xl font-extrabold dark:text-ink-100">{formatINR(Math.round(indicativeEmi))}</p>
          <p className="text-xs text-accent-mute dark:text-ink-400 mt-1">
            Subject to final approval — the loan officer may adjust the rate based on your credit profile.
          </p>
        </div>
      </form>
    </Modal>
  );
}

//  Eligibility calculator (already worked; now auto-fills customerNo)
function EligibilityCalculator({ defaultCustomerNo }) {
  const [result, setResult] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const { register, handleSubmit, reset, formState: { errors } } = useForm({
    defaultValues: { customerNo: '', requestedAmount: 500000, repayDurationMonths: 36 },
  });

  // Auto-populate customerNo once the profile loads.
  useEffect(() => {
    if (defaultCustomerNo) reset({ customerNo: defaultCustomerNo, requestedAmount: 500000, repayDurationMonths: 36 });
  }, [defaultCustomerNo, reset]);

  const onSubmit = async (values) => {
    setSubmitting(true);
    setResult(null);
    try {
      const data = await loanApi.evaluate({
        customerNo: values.customerNo,
        requestedAmount: Number(values.requestedAmount),
        repayDurationMonths: Number(values.repayDurationMonths),
      });
      setResult(data);
    } catch (err) {
      toast.error(errorMessage(err, 'Could not check eligibility.'));
    } finally { setSubmitting(false); }
  };

  return (
    <Card>
      <CardHeader
        title="Check your eligibility"
        subtitle="Get an instant decision based on your profile and income"
        className="mb-5"
      />
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <Input
          label="Customer number"
          placeholder="CUST-XXXXXXXX"
          hint={defaultCustomerNo ? 'Auto-filled from your profile' : undefined}
          error={errors.customerNo?.message}
          {...register('customerNo', { required: 'Required' })}
        />
        <div className="grid sm:grid-cols-2 gap-4">
          <Input
            label="Amount (₹)" type="number"
            error={errors.requestedAmount?.message}
            {...register('requestedAmount', {
              required: 'Required',
              min: { value: 10000, message: 'Minimum ₹10,000' },
            })}
          />
          <Input
            label="Tenure (months)" type="number"
            error={errors.repayDurationMonths?.message}
            {...register('repayDurationMonths', {
              required: 'Required',
              min: { value: 6,   message: 'Minimum 6 months' },
              max: { value: 240, message: 'Maximum 240 months' },
            })}
          />
        </div>
        <div className="flex justify-end">
          <Button type="submit" loading={submitting} icon={Calculator}>Check eligibility</Button>
        </div>
      </form>

      {result && <EligibilityResult result={result} />}
    </Card>
  );
}

function EligibilityResult({ result }) {
  const eligible = result.isEligible || result.eligible || result.decision === 'APPROVED';
  return (
    <div className={`mt-6 rounded-xl p-5 border ${eligible ? 'bg-green-50 dark:bg-green-900/20 border-green-100 dark:border-green-900/40' : 'bg-red-50 dark:bg-red-900/20 border-red-100 dark:border-red-900/40'}`}>
      <div className="flex items-center gap-3">
        {eligible ? <CheckCircle2 className="text-accent-success" size={20} /> : <XCircle className="text-accent-danger" size={20} />}
        <h4 className="font-semibold dark:text-ink-100">
          {eligible ? 'You are pre-approved!' : 'Application not eligible'}
        </h4>
      </div>
      <dl className="mt-4 grid sm:grid-cols-3 gap-4 text-sm">
        <div>
          <dt className="text-accent-mute dark:text-ink-400 text-xs uppercase tracking-widest">Calculated EMI</dt>
          <dd className="font-display font-extrabold text-lg dark:text-ink-100">{formatINR(result.calculatedEmi)}</dd>
        </div>
        <div>
          <dt className="text-accent-mute dark:text-ink-400 text-xs uppercase tracking-widest">Max allowed EMI</dt>
          <dd className="font-display font-extrabold text-lg dark:text-ink-100">{formatINR(result.maxAllowedEmi)}</dd>
        </div>
        <div>
          <dt className="text-accent-mute dark:text-ink-400 text-xs uppercase tracking-widest">Decision</dt>
          <dd className="font-semibold dark:text-ink-100">{result.decision || (eligible ? 'APPROVED' : 'REJECTED')}</dd>
        </div>
      </dl>
      {!eligible && Array.isArray(result.rejectionReasons) && result.rejectionReasons.length > 0 && (
        <ul className="mt-4 text-sm text-accent-slate dark:text-ink-300 space-y-1.5">
          {result.rejectionReasons.map((r, i) => (
            <li key={i} className="flex items-start gap-2"><XCircle size={14} className="text-accent-danger mt-0.5 shrink-0" /> {r}</li>
          ))}
        </ul>
      )}
    </div>
  );
}
