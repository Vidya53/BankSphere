import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import {
  ShieldCheck, FileText, CheckCircle2, AlertTriangle, ArrowRight,
  Copy, RefreshCw, IdCard,
} from 'lucide-react';
import toast from 'react-hot-toast';
import { PageHeader } from '../../components/layout/PageHeader';
import { Card, CardHeader } from '../../components/common/Card';
import { Input, Select, Textarea } from '../../components/common/Input';
import { Button } from '../../components/common/Button';
import { StatusBadge } from '../../components/common/Badge';
import { Skeleton } from '../../components/common/Skeleton';
import { customerApi, kycApi } from '../../api/customer';
import { errorMessage } from '../../api/client';
import { applyServerErrors } from '../../api/serverErrors';
import { formatDateTime } from '../../utils/format';

// Document type enum values must match com.cts.customerservices.enums.DocumentType
// on the backend exactly — Jackson rejects anything else.
const DOC_TYPES = [
  { value: 'AADHAR',   label: 'Aadhaar',     hint: '12 digits starting with 2-9 (e.g. 234567890123)' },
  { value: 'PAN',      label: 'PAN',         hint: '5 letters + 4 digits + 1 letter (e.g. ABCDE1234F)' },
  { value: 'PASSPORT', label: 'Passport',    hint: '1 letter + 7 digits (e.g. A1234567)' },
  { value: 'VOTER_ID', label: 'Voter ID',    hint: '3 letters + 7 digits (e.g. ABC1234567)' },
];

const docHintFor = (value) => DOC_TYPES.find((d) => d.value === value)?.hint;

export default function Kyc() {
  const [profile, setProfile] = useState(null);
  const [kyc, setKyc] = useState(null);
  const [loading, setLoading] = useState(true);

  const reload = async () => {
    setLoading(true);
    try {
      const myProfile = await customerApi.getMyProfile().catch((err) => {
        if (err?.response?.status === 404) return null;
        throw err;
      });
      setProfile(myProfile);

      if (myProfile?.customerNo) {
        const kycRecord = await kycApi.getByCustomerNo(myProfile.customerNo).catch((err) => {
          if (err?.response?.status === 404) return null;
          throw err;
        });
        setKyc(kycRecord);
      } else {
        setKyc(null);
      }
    } catch (err) {
      toast.error(errorMessage(err, 'Could not load your KYC status.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { reload(); }, []); // eslint-disable-line react-hooks/exhaustive-deps

  if (loading) {
    return (
      <div className="space-y-6 max-w-5xl">
        <Skeleton className="h-10 w-72" />
        <Skeleton className="h-64 rounded-xl2" />
      </div>
    );
  }

  // No customer profile yet → user must complete the profile before KYC
  if (!profile) {
    return <NoProfileState />;
  }

  // KYC already submitted → show its status
  if (kyc) {
    return <KycStatusView profile={profile} kyc={kyc} onReload={reload} />;
  }

  // Profile exists, no KYC → show submission form
  return <KycSubmitForm profile={profile} onSubmitted={reload} />;
}

// ── No profile yet ───────────────────────────────────────────────────────────
function NoProfileState() {
  return (
    <div className="space-y-8 max-w-3xl">
      <PageHeader
        breadcrumb="Account · KYC"
        title="Complete your profile first"
        subtitle="Before we can verify your identity, we need a few customer details."
      />
      <Card className="p-10 text-center">
        <span className="mx-auto h-14 w-14 rounded-full bg-amber-50 dark:bg-amber-900/30 grid place-items-center text-accent-warning dark:text-amber-300">
          <AlertTriangle size={26} />
        </span>
        <h2 className="mt-5 font-display text-xl font-extrabold dark:text-ink-100">No customer profile found</h2>
        <p className="mt-2 text-sm text-accent-slate dark:text-ink-300 max-w-md mx-auto">
          You need to create your customer profile before you can submit KYC. It only takes a minute.
        </p>
        <Link to="/app/profile" className="btn-primary mt-6 inline-flex">
          Create profile <ArrowRight size={14} />
        </Link>
      </Card>
    </div>
  );
}

// ── Submission form ─────────────────────────────────────────────────────────
function KycSubmitForm({ profile, onSubmitted }) {
  const [submitting, setSubmitting] = useState(false);
  const { register, handleSubmit, watch, setError, formState: { errors } } = useForm({
    mode: 'onTouched',
    defaultValues: { documentType: 'AADHAR', documentNumber: '', expiryDate: '' },
  });
  const docType = watch('documentType');

  const onSubmit = async (values) => {
    setSubmitting(true);
    try {
      const payload = {
        documentType: values.documentType,
        documentNumber: values.documentNumber.trim().toUpperCase(),
        // Backend expects LocalDateTime; only send if user provided one
        expiryDate: values.expiryDate ? `${values.expiryDate}T00:00:00` : null,
      };
      await kycApi.submit(profile.customerNo, payload);
      toast.success('KYC submitted — our team will review it shortly.');
      onSubmitted?.();
    } catch (err) {
      const applied = applyServerErrors(err, setError);
      if (applied) {
        toast.error('Please fix the highlighted fields and try again.');
      } else {
        toast.error(errorMessage(err, 'Could not submit KYC.'));
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-8 max-w-5xl">
      <PageHeader
        breadcrumb="Account"
        title="KYC Verification"
        subtitle="Complete your Know Your Customer verification to unlock the full feature set."
      />

      <div className="grid lg:grid-cols-[1.4fr_1fr] gap-6">
        <Card>
          <CardHeader
            title="Submit your documents"
            subtitle="Provide a valid government-issued ID. We support Aadhaar, PAN, Passport and Voter ID."
            className="mb-6"
          />

          <CustomerNumberBanner customerNo={profile.customerNo} />

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-5 mt-5">
            <div className="grid sm:grid-cols-2 gap-4">
              <Select label="Document type" required {...register('documentType', { required: true })}>
                {DOC_TYPES.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}
              </Select>
              <Input
                label="Document number"
                required
                placeholder="As shown on the document"
                hint={docHintFor(docType)}
                error={errors.documentNumber?.message}
                {...register('documentNumber', {
                  required: 'Required',
                  minLength: { value: 5, message: 'Document number looks too short' },
                  maxLength: { value: 50, message: 'Document number is too long' },
                })}
              />
            </div>

            <Input
              label="Document expiry (optional)"
              type="date"
              hint="Only required for documents that expire (e.g. passport). Must be a future date."
              error={errors.expiryDate?.message}
              {...register('expiryDate', {
                validate: (v) => !v || new Date(v) > new Date() || 'Expiry must be a future date',
              })}
            />

            <div className="pt-4 border-t border-accent-line dark:border-ink-700 flex items-center justify-end gap-2">
              <Button type="submit" loading={submitting} icon={FileText}>Submit for review</Button>
            </div>
          </form>
        </Card>

        <BenefitsSidebar />
      </div>
    </div>
  );
}

// ── Status view (KYC already submitted) ─────────────────────────────────────
function KycStatusView({ profile, kyc, onReload }) {
  const toneOf = {
    SUBMITTED:    'info',
    UNDER_REVIEW: 'info',
    APPROVED:     'success',
    REJECTED:     'danger',
    EXPIRED:      'warning',
  };

  return (
    <div className="space-y-8 max-w-5xl">
      <PageHeader
        breadcrumb="Account"
        title="KYC Verification"
        subtitle="Track the status of your identity verification."
        actions={
          <Button variant="secondary" icon={RefreshCw} onClick={onReload}>Refresh</Button>
        }
      />

      <Card>
        <CardHeader title="My KYC submission" subtitle="Submitted documents and current status" className="mb-6" />

        <CustomerNumberBanner customerNo={profile.customerNo} />

        <dl className="mt-6 grid sm:grid-cols-2 gap-x-8 gap-y-5">
          <Field label="Status">
            <StatusBadge status={kyc.status} className={toneOf[kyc.status]} />
          </Field>
          <Field label="Document type">
            <span className="font-semibold dark:text-ink-100 inline-flex items-center gap-2">
              <IdCard size={14} className="text-brand-700 dark:text-brand-300" />
              {kyc.documentType?.replace(/_/g, ' ')}
            </span>
          </Field>
          <Field label="Document number">
            <span className="font-mono text-sm dark:text-ink-100">{kyc.documentNumber}</span>
          </Field>
          {kyc.expiryDate && <Field label="Expiry"><span className="dark:text-ink-100">{formatDateTime(kyc.expiryDate)}</span></Field>}
          {kyc.submittedAt && <Field label="Submitted on"><span className="dark:text-ink-100">{formatDateTime(kyc.submittedAt)}</span></Field>}
          {kyc.verifiedAt && <Field label="Verified on"><span className="dark:text-ink-100">{formatDateTime(kyc.verifiedAt)}</span></Field>}
          {kyc.verifiedBy && <Field label="Reviewed by"><span className="dark:text-ink-100">{kyc.verifiedBy}</span></Field>}
        </dl>

        {kyc.status === 'REJECTED' && kyc.rejectionReason && (
          <div className="mt-6 rounded-xl border border-red-200 dark:border-red-900/50 bg-red-50/60 dark:bg-red-900/20 p-4">
            <div className="flex items-start gap-3">
              <AlertTriangle size={18} className="text-accent-danger dark:text-red-300 shrink-0 mt-0.5" />
              <div>
                <p className="text-sm font-semibold text-accent-danger dark:text-red-300">Rejection reason</p>
                <p className="text-sm text-accent-slate dark:text-ink-300 mt-1">{kyc.rejectionReason}</p>
              </div>
            </div>
          </div>
        )}

        {kyc.status === 'SUBMITTED' || kyc.status === 'UNDER_REVIEW' ? (
          <div className="mt-6 rounded-xl bg-sky-50 dark:bg-sky-900/20 text-sky-900 dark:text-sky-200 p-4 flex gap-3">
            <AlertTriangle size={18} className="shrink-0 mt-0.5" />
            <p className="text-sm">
              Your KYC is currently under review. We typically complete reviews within 24 hours
              on business days. You'll be notified by email once a decision is made.
            </p>
          </div>
        ) : null}

        {kyc.status === 'APPROVED' && (
          <div className="mt-6 rounded-xl bg-green-50 dark:bg-green-900/20 text-green-900 dark:text-green-200 p-4 flex gap-3">
            <CheckCircle2 size={18} className="shrink-0 mt-0.5" />
            <div>
              <p className="text-sm font-semibold">KYC verified</p>
              <p className="text-sm mt-1">
                Your identity is verified. You can now open accounts, apply for loans, and transact
                without basic limits.
              </p>
            </div>
          </div>
        )}
      </Card>
    </div>
  );
}

// ── Bits and pieces ─────────────────────────────────────────────────────────
function CustomerNumberBanner({ customerNo }) {
  const copy = () => {
    navigator.clipboard?.writeText(customerNo);
    toast.success('Customer number copied');
  };
  return (
    <div className="rounded-xl bg-brand-50/60 dark:bg-brand-900/20 border border-brand-100 dark:border-brand-900/40 p-4 flex items-center justify-between gap-3">
      <div>
        <p className="text-xs uppercase tracking-widest text-accent-mute dark:text-ink-400">Linked to</p>
        <p className="mt-0.5 font-mono text-sm font-semibold text-brand-700 dark:text-brand-300">{customerNo}</p>
      </div>
      <button onClick={copy} className="text-accent-mute hover:text-brand-700 dark:text-ink-400 dark:hover:text-brand-300 transition" aria-label="Copy customer number">
        <Copy size={16} />
      </button>
    </div>
  );
}

function Field({ label, children }) {
  return (
    <div>
      <dt className="text-xs uppercase tracking-widest text-accent-mute dark:text-ink-400 mb-1">{label}</dt>
      <dd>{children}</dd>
    </div>
  );
}

function BenefitsSidebar() {
  return (
    <div className="space-y-5">
      <Card>
        <CardHeader title="Why we need this" className="mb-3" />
        <ul className="space-y-3 text-sm">
          {[
            'Regulatory requirement to prevent fraud and money laundering',
            'Lets you transact above basic limits and apply for loans',
            'Helps personalise your account and recommend products',
            'Encrypted in transit and at rest — only reviewers see it',
          ].map((s) => (
            <li key={s} className="flex items-start gap-2 text-accent-slate dark:text-ink-300">
              <CheckCircle2 size={14} className="text-accent-success dark:text-green-300 mt-0.5 shrink-0" />
              <span>{s}</span>
            </li>
          ))}
        </ul>
      </Card>

      <div className="card-hover bg-gradient-to-br from-brand-50 to-amber-50 dark:from-brand-900/30 dark:to-amber-900/20 border-brand-100 dark:border-brand-900/40 p-5">
        <span className="h-10 w-10 rounded-full bg-white dark:bg-ink-800 text-brand-700 dark:text-brand-300 grid place-items-center">
          <ShieldCheck size={18} />
        </span>
        <h4 className="mt-3 font-semibold dark:text-ink-100">Approved KYCs unlock</h4>
        <ul className="mt-3 space-y-2 text-sm text-accent-slate dark:text-ink-300">
          <li>• Higher daily transfer limits</li>
          <li>• Loan eligibility checks</li>
          <li>• Premium account upgrades</li>
        </ul>
      </div>
    </div>
  );
}

