import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { useNavigate } from 'react-router-dom';
import {
  User, Mail, Phone, MapPin, Save, Pencil, X, BadgeCheck,
  Calendar, CreditCard, Building2, IndianRupee,
} from 'lucide-react';
import toast from 'react-hot-toast';
import { PageHeader } from '../../components/layout/PageHeader';
import { Card, CardHeader } from '../../components/common/Card';
import { Input, Select, Textarea } from '../../components/common/Input';
import { Button } from '../../components/common/Button';
import { Skeleton } from '../../components/common/Skeleton';
import { Badge, StatusBadge } from '../../components/common/Badge';
import { useAuth } from '../../context/AuthContext';
import { customerApi } from '../../api/customer';
import { errorMessage } from '../../api/client';
import { applyServerErrors } from '../../api/serverErrors';
import { initials, formatINR, formatDate } from '../../utils/format';

const GENDERS = ['MALE', 'FEMALE', 'OTHER'];

export default function Profile() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [mode, setMode] = useState('view'); // 'view' | 'edit' | 'create'

  // ── Load existing profile on mount. 404 → no profile yet → create mode. ──
  useEffect(() => {
    let cancelled = false;
    customerApi.getMyProfile()
      .then((data) => {
        if (cancelled) return;
        setProfile(data);
        setMode('view');
      })
      .catch((err) => {
        if (cancelled) return;
        if (err?.response?.status === 404) {
          setMode('create');
        } else {
          toast.error(errorMessage(err, 'Could not load your profile.'));
        }
      })
      .finally(() => !cancelled && setLoading(false));
    return () => { cancelled = true; };
  }, []);

  if (loading) {
    return (
      <div className="space-y-6 max-w-5xl">
        <Skeleton className="h-10 w-72" />
        <div className="grid lg:grid-cols-[1fr_2.4fr] gap-6">
          <Skeleton className="h-72 rounded-xl2" />
          <Skeleton className="h-96 rounded-xl2" />
        </div>
      </div>
    );
  }

  const refreshProfile = (next) => { setProfile(next); setMode('view'); };

  return (
    <div className="space-y-8 max-w-5xl">
      <PageHeader
        breadcrumb="Account"
        title="My profile"
        subtitle={
          mode === 'create' ? 'Create your customer profile to get started with banking.'
          : mode === 'edit' ? 'Update your personal details, address and contact information.'
          : 'Your customer profile. Use Edit to update your details.'
        }
        actions={
          mode === 'view' && profile && (
            <Button icon={Pencil} onClick={() => setMode('edit')}>Edit profile</Button>
          )
        }
      />

      <div className="grid lg:grid-cols-[1fr_2.4fr] gap-6">
        <ProfileSidebar user={user} profile={profile} />

        {mode === 'view'  && profile && <ProfileView profile={profile} />}
        {(mode === 'edit'  || mode === 'create') && (
          <ProfileForm
            mode={mode}
            profile={profile}
            user={user}
            onCancel={() => mode === 'edit' ? setMode('view') : navigate('/app')}
            onSaved={(saved, isCreate) => {
              refreshProfile(saved);
              if (isCreate) navigate('/app');
            }}
          />
        )}
      </div>
    </div>
  );
}

// ── Sidebar — avatar + identity summary ──────────────────────────────────────
function ProfileSidebar({ user, profile }) {
  const displayName = profile
    ? `${profile.firstName || ''} ${profile.lastName || ''}`.trim()
    : user?.fullName;
  const displayEmail = profile?.email || user?.email;

  return (
    <Card>
      <div className="text-center">
        <span className="mx-auto h-24 w-24 rounded-full bg-brand-700 text-white grid place-items-center font-display text-3xl font-extrabold">
          {initials(displayName || displayEmail)}
        </span>
        <h3 className="mt-4 font-semibold text-accent-ink dark:text-ink-100">{displayName || 'Member'}</h3>
        <p className="text-xs text-accent-mute dark:text-ink-400">{displayEmail}</p>
        <div className="mt-3 flex justify-center gap-2 flex-wrap">
          <Badge tone="brand">{user?.role}</Badge>
          {profile?.status && <StatusBadge status={profile.status} />}
        </div>
      </div>

      <ul className="mt-6 space-y-2 text-sm">
        {profile?.customerNo && (
          <SidebarItem icon={BadgeCheck} label={`Customer ${profile.customerNo}`} />
        )}
        <SidebarItem icon={Mail} label={displayEmail || '—'} />
        <SidebarItem icon={User} label={`User ID: ${user?.userId}`} />
        {profile?.branchCode && <SidebarItem icon={Building2} label={`Branch ${profile.branchCode}`} />}
        {profile?.createdAt && (
          <SidebarItem icon={Calendar} label={`Member since ${formatDate(profile.createdAt)}`} />
        )}
      </ul>
    </Card>
  );
}

function SidebarItem({ icon: Icon, label }) {
  return (
    <li className="flex items-center gap-3 px-3 py-2 rounded-lg bg-accent-surface/60">
      <Icon size={14} className="text-accent-mute dark:text-ink-400 shrink-0" />
      <span className="truncate">{label}</span>
    </li>
  );
}

// ── Read-only view ───────────────────────────────────────────────────────────
function ProfileView({ profile }) {
  return (
    <div className="space-y-6">
      <Card>
        <CardHeader title="Personal details" subtitle="Identity information on file" className="mb-5" />
        <dl className="grid sm:grid-cols-2 gap-x-8 gap-y-5">
          <Field label="First name"     value={profile.firstName} icon={User} />
          <Field label="Last name"      value={profile.lastName}  icon={User} />
          <Field label="Date of birth"  value={formatDate(profile.dateOfBirth)} icon={Calendar} />
          <Field label="Gender"         value={profile.gender} icon={User} />
          <Field label="Email"          value={profile.email} icon={Mail} />
          <Field label="Mobile number"  value={profile.mobileNumber} icon={Phone} />
          {profile.alternateMobileNumber && (
            <Field label="Alt. mobile" value={profile.alternateMobileNumber} icon={Phone} />
          )}
        </dl>
      </Card>

      <Card>
        <CardHeader title="Address" className="mb-5" />
        <dl className="grid sm:grid-cols-2 gap-x-8 gap-y-5">
          <Field label="Address line 1" value={profile.addressLine1} icon={MapPin} />
          {profile.addressLine2 && <Field label="Address line 2" value={profile.addressLine2} icon={MapPin} />}
          <Field label="City"           value={profile.city} icon={MapPin} />
          <Field label="State"          value={profile.state} icon={MapPin} />
          <Field label="Postal code"    value={profile.postalCode} icon={MapPin} />
          <Field label="Country"        value={profile.country} icon={MapPin} />
        </dl>
      </Card>

      <Card>
        <CardHeader title="Banking" className="mb-5" />
        <dl className="grid sm:grid-cols-2 gap-x-8 gap-y-5">
          <Field label="Branch code"    value={profile.branchCode} icon={Building2} />
          <Field label="Customer No."   value={profile.customerNo} icon={CreditCard} mono />
          <Field label="Monthly income" value={profile.incomeAmount ? formatINR(profile.incomeAmount) : '—'} icon={IndianRupee} />
          {profile.riskCategory && (
            <Field label="Risk category" value={profile.riskCategory} icon={User} />
          )}
        </dl>
      </Card>
    </div>
  );
}

function Field({ label, value, icon: Icon, mono }) {
  return (
    <div>
      <dt className="text-xs uppercase tracking-widest text-accent-mute dark:text-ink-400 flex items-center gap-1.5 mb-1">
        {Icon && <Icon size={12} />} {label}
      </dt>
      <dd className={`text-sm font-semibold text-accent-ink dark:text-ink-100 ${mono ? 'font-mono' : ''}`}>
        {value || '—'}
      </dd>
    </div>
  );
}

// ── Form (used for both create and edit modes) ───────────────────────────────
function ProfileForm({ mode, profile, user, onCancel, onSaved }) {
  const isCreate = mode === 'create';
  const [submitting, setSubmitting] = useState(false);

  const { register, handleSubmit, setError, formState: { errors } } = useForm({
    mode: 'onTouched',
    defaultValues: isCreate ? {
      firstName: user?.fullName?.split(' ')[0] || '',
      lastName:  user?.fullName?.split(' ').slice(1).join(' ') || '',
      email: user?.email || '',
      mobileNumber: '',
      alternateMobileNumber: '',
      dateOfBirth: '',
      gender: 'MALE',
      branchCode: user?.branchCode || '',
      addressLine1: '',
      addressLine2: '',
      city: '',
      state: '',
      country: 'India',
      postalCode: '',
      incomeAmount: '',
    } : {
      firstName: profile?.firstName || '',
      lastName:  profile?.lastName || '',
      email: profile?.email || '',
      mobileNumber: profile?.mobileNumber || '',
      alternateMobileNumber: profile?.alternateMobileNumber || '',
      dateOfBirth: profile?.dateOfBirth || '',
      gender: profile?.gender || 'MALE',
      branchCode: profile?.branchCode || '',
      addressLine1: profile?.addressLine1 || '',
      addressLine2: profile?.addressLine2 || '',
      city: profile?.city || '',
      state: profile?.state || '',
      country: profile?.country || 'India',
      postalCode: profile?.postalCode || '',
      incomeAmount: profile?.incomeAmount ?? '',
    },
  });

  const onSubmit = async (values) => {
    setSubmitting(true);
    try {
      const payload = buildPayload(values, profile);
      const saved = isCreate
        ? await customerApi.register(payload)
        : await customerApi.updateMyProfile(payload);
      toast.success(isCreate ? 'Profile created — welcome to BankSphere!' : 'Profile updated');
      onSaved(saved, isCreate);
    } catch (err) {
      const applied = applyServerErrors(err, setError);
      if (applied) {
        toast.error('Please fix the highlighted fields and try again.');
      } else {
        toast.error(errorMessage(err, isCreate ? 'Could not create your profile.' : 'Could not save your changes.'));
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Card>
      <CardHeader
        title={isCreate ? 'Create your profile' : 'Edit profile'}
        subtitle={isCreate
          ? 'Fill in your details — this creates your customer profile and unlocks KYC.'
          : 'Update fields below. Date of birth, gender and customer number cannot be changed.'}
        className="mb-6"
      />
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        <Section title="Personal">
          <div className="grid sm:grid-cols-2 gap-4">
            <Input label="First name" required leftIcon={User}
                   {...register('firstName', { required: 'Required' })} error={errors.firstName?.message} />
            <Input label="Last name" required
                   {...register('lastName', { required: 'Required' })} error={errors.lastName?.message} />
            <Input
              label="Date of birth"
              required={isCreate}
              type="date"
              disabled={!isCreate}
              hint={!isCreate ? 'Date of birth cannot be changed after profile creation.' : null}
              {...register('dateOfBirth', { required: 'Required' })}
              error={errors.dateOfBirth?.message}
            />
            <Select
              label="Gender"
              required={isCreate}
              disabled={!isCreate}
              hint={!isCreate ? 'Gender cannot be changed after profile creation.' : null}
              {...register('gender')}
            >
              {GENDERS.map((g) => <option key={g} value={g}>{g}</option>)}
            </Select>
            <Input label="Email" required type="email" leftIcon={Mail}
                   {...register('email', { required: 'Required' })} error={errors.email?.message} />
            <Input
              label="Mobile number"
              required
              leftIcon={Phone}
              {...register('mobileNumber', {
                required: 'Required',
                pattern: { value: /^\d{10}$/, message: '10 digits required' },
              })}
              error={errors.mobileNumber?.message}
            />
            <Input
              label="Alternate mobile (optional)"
              leftIcon={Phone}
              {...register('alternateMobileNumber', {
                pattern: { value: /^\d{10}$/, message: '10 digits required' },
              })}
              error={errors.alternateMobileNumber?.message}
            />
          </div>
        </Section>

        <Section title="Address">
          <div className="grid sm:grid-cols-2 gap-4">
            <Input label="Address line 1" required
                   {...register('addressLine1', { required: 'Required' })} error={errors.addressLine1?.message} />
            <Input label="Address line 2" {...register('addressLine2')} />
            <Input label="City"  required {...register('city',  { required: 'Required' })} error={errors.city?.message} />
            <Input label="State" required {...register('state', { required: 'Required' })} error={errors.state?.message} />
            <Input
              label="Postal code"
              {...register('postalCode', { pattern: { value: /^\d{5,6}$/, message: '5–6 digits' } })}
              error={errors.postalCode?.message}
            />
            <Input label="Country" required {...register('country', { required: 'Required' })} />
          </div>
        </Section>

        <Section title="Banking">
          <div className="grid sm:grid-cols-2 gap-4">
            <Input
              label="Branch code"
              required={isCreate}
              disabled={!isCreate}
              hint={!isCreate ? 'Branch can only be changed by a CSR.' : null}
              {...register('branchCode', { required: 'Required' })}
              error={errors.branchCode?.message}
            />
            <Input label="Monthly income (₹)" type="number" {...register('incomeAmount')} />
          </div>
        </Section>

        <div className="flex items-center justify-end gap-2 pt-4 border-t border-accent-line dark:border-ink-700">
          <Button type="button" variant="ghost" onClick={onCancel} icon={X}>Cancel</Button>
          <Button type="submit" loading={submitting} icon={Save}>
            {isCreate ? 'Create profile' : 'Save changes'}
          </Button>
        </div>
      </form>
    </Card>
  );
}

function Section({ title, children }) {
  return (
    <div>
      <h3 className="text-sm font-semibold text-accent-ink dark:text-ink-100 mb-3 pb-2 border-b border-accent-line/70 uppercase tracking-wide">
        {title}
      </h3>
      {children}
    </div>
  );
}

// ── Payload normalisation ────────────────────────────────────────────────────
// The backend's @Pattern on alternateMobileNumber (and a couple of other optional
// fields) doesn't accept the empty string — only null or a valid value. Anything
// blank in the form needs to go up as null. Also normalise the date string just
// in case the date input gave us anything other than YYYY-MM-DD.
function buildPayload(values, currentProfile) {
  const blankToNull = (v) => (v === undefined || v === null || String(v).trim() === '') ? null : v;
  const normaliseDate = (v) => {
    if (!v) return null;
    if (Array.isArray(v) && v.length === 3) {
      // Some Jackson configs serialise LocalDate as [yyyy, m, d] — convert back to ISO.
      const [y, m, d] = v;
      return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
    }
    const s = String(v);
    return s.length >= 10 ? s.slice(0, 10) : s;
  };

  return {
    firstName: values.firstName,
    lastName:  values.lastName,
    email:     values.email,
    mobileNumber: values.mobileNumber,
    alternateMobileNumber: blankToNull(values.alternateMobileNumber),
    // Disabled inputs in edit mode aren't submitted by React Hook Form —
    // fall back to the current profile so the payload stays valid.
    dateOfBirth: normaliseDate(values.dateOfBirth || currentProfile?.dateOfBirth),
    gender:      values.gender || currentProfile?.gender,
    branchCode:  values.branchCode || currentProfile?.branchCode,
    addressLine1: values.addressLine1,
    addressLine2: blankToNull(values.addressLine2),
    city:    values.city,
    state:   values.state,
    country: values.country,
    postalCode: values.postalCode,
    incomeAmount: values.incomeAmount === '' || values.incomeAmount == null
      ? null
      : Number(values.incomeAmount),
  };
}
