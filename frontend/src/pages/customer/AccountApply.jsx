import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import {
  Wallet, ArrowRight, CheckCircle2, ShieldCheck, ShieldAlert, UserPlus,
} from 'lucide-react';
import toast from 'react-hot-toast';
import { PageHeader } from '../../components/layout/PageHeader';
import { Card, CardHeader } from '../../components/common/Card';
import { Input, Select, Textarea } from '../../components/common/Input';
import { Button } from '../../components/common/Button';
import { Skeleton } from '../../components/common/Skeleton';
import { Badge, StatusBadge } from '../../components/common/Badge';
import { accountApi } from '../../api/account';
import { branchApi } from '../../api/branch';
import { customerApi, kycApi } from '../../api/customer';
import { useAsync } from '../../hooks/useAsync';
import { errorMessage } from '../../api/client';
import { applyServerErrors } from '../../api/serverErrors';

const ACCOUNT_TYPES = [
  { value: 'SAVINGS',           label: 'Savings — for everyday banking',          minDeposit: 500 },
  { value: 'CURRENT',           label: 'Current — for businesses',                 minDeposit: 5000 },
  { value: 'FIXED_DEPOSIT',     label: 'Fixed Deposit — locked, higher interest',  minDeposit: 10000 },
  { value: 'RECURRING_DEPOSIT', label: 'Recurring Deposit — monthly savings',      minDeposit: 1000 },
];

export default function AccountApply() {
  const navigate = useNavigate();

  // ── Prerequisites: profile must exist, KYC must be APPROVED ───────────────
  const [profile, setProfile] = useState(null);
  const [kyc, setKyc] = useState(null);
  const [prereqLoading, setPrereqLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const p = await customerApi.getMyProfile().catch((err) => {
          if (err?.response?.status === 404) return null;
          throw err;
        });
        if (cancelled) return;
        setProfile(p);
        if (p?.customerNo) {
          const k = await kycApi.getByCustomerNo(p.customerNo).catch((err) => {
            if (err?.response?.status === 404) return null;
            throw err;
          });
          if (!cancelled) setKyc(k);
        }
      } catch (err) {
        if (!cancelled) toast.error(errorMessage(err, 'Could not check your account-application prerequisites.'));
      } finally {
        if (!cancelled) setPrereqLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, []);

  if (prereqLoading) {
    return (
      <div className="space-y-6 max-w-4xl">
        <Skeleton className="h-10 w-72" />
        <Skeleton className="h-64 rounded-xl2" />
      </div>
    );
  }

  if (!profile) {
    return (
      <Prerequisite
        icon={UserPlus}
        title="Complete your profile first"
        message="You need to create your customer profile before you can open an account."
        ctaLabel="Create profile"
        ctaTo="/app/profile"
      />
    );
  }

  if (!kyc) {
    return (
      <Prerequisite
        icon={ShieldAlert}
        tone="warning"
        title="Submit your KYC first"
        message="Your identity must be verified before you can open a bank account. Submit your documents and we'll review them within 24 hours."
        ctaLabel="Submit KYC"
        ctaTo="/app/kyc"
        statusBadge={null}
      />
    );
  }

  if (kyc.status !== 'APPROVED') {
    const friendly =
      kyc.status === 'REJECTED' ? 'Your KYC was rejected. Please submit a fresh KYC with valid documents.'
      : 'Your KYC is currently under review. You will be able to apply for an account once it is approved (typically within 24 hours).';
    return (
      <Prerequisite
        icon={ShieldCheck}
        tone={kyc.status === 'REJECTED' ? 'danger' : 'warning'}
        title="KYC approval required"
        message={friendly}
        ctaLabel={kyc.status === 'REJECTED' ? 'Resubmit KYC' : 'Check KYC status'}
        ctaTo="/app/kyc"
        statusBadge={kyc.status}
      />
    );
  }

  return <ApplyForm profile={profile} navigate={navigate} />;
}

// ── Prerequisite blocker ─────────────────────────────────────────────────────
function Prerequisite({ icon: Icon, tone = 'warning', title, message, ctaLabel, ctaTo, statusBadge }) {
  const accentBg = tone === 'danger'
    ? 'bg-red-50 dark:bg-red-900/30 text-accent-danger dark:text-red-300'
    : 'bg-amber-50 dark:bg-amber-900/30 text-accent-warning dark:text-amber-300';
  return (
    <div className="space-y-6 max-w-3xl">
      <PageHeader breadcrumb="Banking · Accounts" title="Open a new account" />
      <Card className="p-10 text-center">
        <span className={`mx-auto h-14 w-14 rounded-full grid place-items-center ${accentBg}`}>
          <Icon size={26} />
        </span>
        <h2 className="mt-5 font-display text-xl font-extrabold dark:text-ink-100">{title}</h2>
        {statusBadge && (
          <div className="mt-3"><StatusBadge status={statusBadge} /></div>
        )}
        <p className="mt-3 text-sm text-accent-slate dark:text-ink-300 max-w-md mx-auto">{message}</p>
        <Link to={ctaTo} className="btn-primary mt-6 inline-flex">
          {ctaLabel} <ArrowRight size={14} />
        </Link>
      </Card>
    </div>
  );
}

// ── Application form (only reached when prerequisites are met) ───────────────
function ApplyForm({ profile, navigate }) {
  const [submitting, setSubmitting] = useState(false);

  const { data: branches, loading: branchesLoading } = useAsync(
    () => branchApi.active().catch(() => []),
    []
  );

  // mode: 'onTouched' validates each field once the user blurs it, instead of
  // waiting for submit. This gives immediate feedback without nagging on first paint.
  const { register, handleSubmit, watch, trigger, setError, formState: { errors, isValid } } = useForm({
    mode: 'onTouched',
    defaultValues: {
      accountType: 'SAVINGS',
      branchCode: profile?.branchCode || '',
      initialDeposit: '',
      nomineeName: '',
      nomineeRelation: '',
      nomineePhone: '',
      purpose: '',
    },
  });

  const selectedType = ACCOUNT_TYPES.find((t) => t.value === watch('accountType'));
  const nomineeName     = watch('nomineeName');
  const nomineeRelation = watch('nomineeRelation');
  const nomineePhone    = watch('nomineePhone');
  // If the customer fills any nominee field, all three become required —
  // banks won't accept half-nominee details on file.
  const nomineeStarted = Boolean((nomineeName || '').trim() || (nomineeRelation || '').trim() || (nomineePhone || '').trim());

  const onSubmit = async (values) => {
    // Final cross-field check — nominee fields are all-or-nothing.
    const ok = await trigger();
    if (!ok) return;

    setSubmitting(true);
    try {
      const payload = {
        accountType: values.accountType,
        branchCode:  values.branchCode.trim(),
        initialDeposit: values.initialDeposit ? Number(values.initialDeposit) : 0,
        // Strip optional fields that are blank so the backend doesn't reject empty strings.
        nomineeName:     (values.nomineeName     || '').trim() || null,
        nomineeRelation: (values.nomineeRelation || '').trim() || null,
        nomineePhone:    (values.nomineePhone    || '').trim() || null,
        purpose:         (values.purpose         || '').trim() || null,
      };
      const res = await accountApi.applyForAccount(payload);
      toast.success(`Application submitted: ${res?.applicationRef}`);
      navigate('/app/accounts');
    } catch (err) {
      const applied = applyServerErrors(err, setError);
      if (applied) {
        toast.error('Please fix the highlighted fields and try again.');
      } else {
        toast.error(errorMessage(err, 'Application could not be submitted.'));
      }
    } finally {
      setSubmitting(false);
    }
  };

  const branchList = Array.isArray(branches) ? branches : [];

  return (
    <div className="space-y-8 max-w-4xl">
      <PageHeader
        breadcrumb="Banking · Accounts"
        title="Open a new account"
        subtitle="Tell us what you need and we'll get it ready. Most applications are reviewed within one business day."
        actions={<Badge tone="success" dot>KYC verified</Badge>}
      />

      <div className="grid lg:grid-cols-[1.6fr_1fr] gap-6">
        <Card>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
            <Section title="Account type & branch">
              <div className="grid sm:grid-cols-2 gap-4">
                <Select
                  label="Account type"
                  required
                  error={errors.accountType?.message}
                  {...register('accountType', { required: 'Choose an account type' })}
                >
                  {ACCOUNT_TYPES.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}
                </Select>
                <Select
                  label="Branch"
                  required
                  disabled={branchesLoading}
                  hint={branchesLoading ? 'Loading branches…' : branchList.length === 0 ? 'No active branches available' : null}
                  error={errors.branchCode?.message}
                  {...register('branchCode', {
                    required: 'Select a branch to open your account at',
                    validate: (v) => branchList.length === 0
                      ? true
                      : branchList.some((b) => b.branchCode === v) || 'Pick a branch from the list',
                  })}
                >
                  <option value="">Select a branch</option>
                  {branchList.map((b) => (
                    <option key={b.branchCode} value={b.branchCode}>
                      {b.branchCode} — {b.branchName || b.city || b.state}
                    </option>
                  ))}
                </Select>
              </div>
            </Section>

            <Section title="Initial deposit">
              <Input
                label="Amount (₹)"
                type="number"
                inputMode="decimal"
                step="0.01"
                min="0"
                placeholder={selectedType ? `Minimum ₹${selectedType.minDeposit}` : '0'}
                hint={selectedType ? `Minimum initial deposit for ${selectedType.value}: ₹${selectedType.minDeposit.toLocaleString('en-IN')}` : null}
                error={errors.initialDeposit?.message}
                {...register('initialDeposit', {
                  validate: (v) => {
                    if (v === '' || v === null || v === undefined) return true;
                    const n = Number(v);
                    if (Number.isNaN(n)) return 'Enter a valid amount';
                    if (n < 0) return 'Cannot be negative';
                    if (n > 0 && selectedType && n < selectedType.minDeposit) {
                      return `Minimum is ₹${selectedType.minDeposit.toLocaleString('en-IN')} for ${selectedType.value}`;
                    }
                    if (n > 10_000_000) return 'Initial deposit cannot exceed ₹1,00,00,000';
                    // Two-decimal cap mirrors the backend @Digits(13,2) constraint.
                    if (!/^\d+(\.\d{1,2})?$/.test(String(v))) return 'At most 2 decimal places';
                    return true;
                  },
                })}
              />
            </Section>

            <Section title="Nominee details (optional — but if you add one, fill all three)">
              <div className="grid sm:grid-cols-2 gap-4">
                <Input
                  label="Nominee name"
                  placeholder="Full name"
                  error={errors.nomineeName?.message}
                  {...register('nomineeName', {
                    validate: (v) => {
                      if (!nomineeStarted) return true;
                      if (!v?.trim()) return 'Required once you add nominee details';
                      if (v.trim().length > 100) return 'Maximum 100 characters';
                      if (!/^[A-Za-z][A-Za-z .'-]*$/.test(v.trim())) return 'Letters, spaces, hyphens and apostrophes only';
                      return true;
                    },
                  })}
                />
                <Input
                  label="Relation"
                  placeholder="Spouse, parent, etc."
                  error={errors.nomineeRelation?.message}
                  {...register('nomineeRelation', {
                    validate: (v) => {
                      if (!nomineeStarted) return true;
                      if (!v?.trim()) return 'Required once you add nominee details';
                      if (v.trim().length > 50) return 'Maximum 50 characters';
                      return true;
                    },
                  })}
                />
                <Input
                  label="Nominee phone"
                  placeholder="10-digit Indian mobile"
                  inputMode="numeric"
                  maxLength={15}
                  error={errors.nomineePhone?.message}
                  {...register('nomineePhone', {
                    validate: (v) => {
                      if (!nomineeStarted) return true;
                      if (!v?.trim()) return 'Required once you add nominee details';
                      if (!/^[0-9]{10,15}$/.test(v.trim())) return '10–15 digits, numbers only';
                      return true;
                    },
                  })}
                />
              </div>
            </Section>

            <Section title="Purpose">
              <Textarea
                label="What will you use this account for? (optional)"
                maxLength={500}
                hint="Helps the branch tailor offers — capped at 500 characters."
                error={errors.purpose?.message}
                {...register('purpose', {
                  maxLength: { value: 500, message: 'Maximum 500 characters' },
                })}
              />
            </Section>

            <div className="flex items-center justify-end gap-2 pt-2 border-t border-accent-line dark:border-ink-700">
              <Button type="button" variant="ghost" onClick={() => navigate(-1)}>Cancel</Button>
              <Button
                type="submit"
                loading={submitting}
                disabled={!isValid && Object.keys(errors).length > 0}
                iconRight={ArrowRight}
              >
                Submit application
              </Button>
            </div>
          </form>
        </Card>

        <div className="space-y-5">
          <Card>
            <CardHeader title="Why open an account" subtitle="Some good reasons" className="mb-4" />
            <ul className="space-y-3 text-sm">
              {[
                'Paperless application, approved within one business day',
                'No paperwork courier — fully digital onboarding',
                'Free debit card and net banking on activation',
                'Personalised insights and saving goals',
              ].map((s) => (
                <li key={s} className="flex items-start gap-2">
                  <CheckCircle2 size={16} className="text-accent-success dark:text-green-300 mt-0.5 shrink-0" />
                  <span className="text-accent-slate dark:text-ink-300">{s}</span>
                </li>
              ))}
            </ul>
          </Card>

          <div className="card-hover p-5 bg-gradient-to-br from-brand-50 to-amber-50 dark:from-brand-900/30 dark:to-amber-900/20 border-brand-100 dark:border-brand-900/40">
            <span className="h-10 w-10 rounded-full bg-white dark:bg-ink-800 grid place-items-center text-brand-700 dark:text-brand-300">
              <Wallet size={18} />
            </span>
            <h4 className="mt-3 font-semibold dark:text-ink-100">Already a customer?</h4>
            <p className="mt-1 text-sm text-accent-slate dark:text-ink-300">
              You can open multiple accounts under the same profile. Your existing KYC remains valid.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}

function Section({ title, children }) {
  return (
    <div>
      <h3 className="text-sm font-semibold text-accent-ink dark:text-ink-100 mb-3 pb-2 border-b border-accent-line/70 dark:border-ink-700 uppercase tracking-wide">{title}</h3>
      {children}
    </div>
  );
}
