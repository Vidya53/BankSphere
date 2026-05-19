import { useEffect, useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';
import {
  Building2, UserPlus, Users, ShieldCheck, Plus, RefreshCw,
  Mail, Phone, Calendar, KeyRound, Send, Power, PauseCircle,
  PlayCircle, IdCard, Briefcase, AlertTriangle,
} from 'lucide-react';
import toast from 'react-hot-toast';
import { PageHeader } from '../../components/layout/PageHeader';
import { Card, CardHeader } from '../../components/common/Card';
import { Input, Select, Textarea } from '../../components/common/Input';
import { Button } from '../../components/common/Button';
import { Modal } from '../../components/common/Modal';
import { Badge, StatusBadge } from '../../components/common/Badge';
import { Skeleton } from '../../components/common/Skeleton';
import { EmptyState } from '../../components/common/EmptyState';
import { KpiCard } from '../../components/dashboard/KpiCard';
import { branchApi } from '../../api/branch';
import { adminApi } from '../../api/admin';
import { errorMessage } from '../../api/client';
import { applyServerErrors } from '../../api/serverErrors';
import { formatDate } from '../../utils/format';

// ADMIN intentionally omitted from the dropdown — a bootstrap admin is auto-seeded
// (admin@banksphere.com / Admin@12345). Creating more admins from the UI is a
// self-promotion risk; if a second admin is truly required, hit the API directly.
const STAFF_ROLES = [
  { value: 'CSR',                label: 'Customer Service Representative', needsBranch: true  },
  { value: 'LOAN_OFFICER',       label: 'Loan Officer',                     needsBranch: true  },
  { value: 'BRANCH_MANAGER',     label: 'Branch Manager',                   needsBranch: true  },
];
const ROLE_LABEL = Object.fromEntries(STAFF_ROLES.map((r) => [r.value, r.label]));
ROLE_LABEL.ADMIN = 'Administrator';
// Legacy: existing rows in identity_service_db.users with role=COMPLIANCE_OFFICER
// (predating the role being retired) get a friendly label in lists even though
// the role can no longer be assigned through the UI.
ROLE_LABEL.COMPLIANCE_OFFICER = 'Compliance Officer (legacy)';

const BRANCH_TYPES  = ['METRO', 'URBAN', 'SEMI_URBAN', 'RURAL', 'DIGITAL_ONLY'];
const BRANCH_STATES = ['ACTIVE', 'INACTIVE', 'TEMPORARILY_CLOSED', 'UNDER_RENOVATION'];

export default function AdminDashboard() {
  const [tab, setTab] = useState('overview');
  const [branches, setBranches] = useState([]);
  const [staff, setStaff] = useState([]);
  const [summary, setSummary] = useState(null);
  const [loadingBranches, setLoadingBranches] = useState(true);
  const [loadingStaff, setLoadingStaff] = useState(true);
  const [createBranchOpen, setCreateBranchOpen] = useState(false);
  const [createStaffOpen, setCreateStaffOpen] = useState(false);

  // Staff filters
  const [roleFilter,   setRoleFilter]   = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [branchFilter, setBranchFilter] = useState('');

  const reloadBranches = async () => {
    setLoadingBranches(true);
    try {
      const list = await branchApi.listAll().catch(() => []);
      setBranches(list);
    } finally { setLoadingBranches(false); }
  };

  const reloadStaff = async () => {
    setLoadingStaff(true);
    try {
      const params = {};
      if (roleFilter)   params.role       = roleFilter;
      if (statusFilter) params.status     = statusFilter;
      if (branchFilter) params.branchCode = branchFilter;
      params.size = 100;
      const [page, sum] = await Promise.all([
        adminApi.listStaff(params).catch(() => ({ content: [] })),
        adminApi.staffSummary().catch(() => null),
      ]);
      setStaff(page?.content || []);
      setSummary(sum);
    } finally { setLoadingStaff(false); }
  };

  useEffect(() => { reloadBranches(); }, []);
  useEffect(() => { reloadStaff(); /* eslint-disable-next-line */ }, [roleFilter, statusFilter, branchFilter]);

  // ── KPI counts (real, from DB) ──────────────────────────────────────────
  const branchCounts = useMemo(() => ({
    total:    branches.length,
    active:   branches.filter((b) => b.status === 'ACTIVE').length,
    inactive: branches.filter((b) => b.status !== 'ACTIVE').length,
  }), [branches]);

  const staffCounts = useMemo(() => ({
    total:   summary?.totalStaff ?? 0,
    active:  summary?.byStatus?.ACTIVE   ?? 0,
    blocked: (summary?.byStatus?.BLOCKED ?? 0) + (summary?.byStatus?.SUSPENDED ?? 0),
  }), [summary]);

  // Staff suspension / blocking / reactivation stays in admin scope. Branch
  // status mutations live in the BM dashboard now — admin only creates them.
  const setStaffStatus = async (user, status) => {
    const verb = status === 'ACTIVE' ? 'Activate' : status === 'BLOCKED' ? 'Block' : 'Suspend';
    if (!confirm(`${verb} ${user.fullName} (${user.email})?`)) return;
    try {
      await adminApi.updateStaffStatus(user.id, status);
      toast.success(`${user.fullName} → ${status}`);
      reloadStaff();
    } catch (err) { toast.error(errorMessage(err, 'Could not update staff status.')); }
  };

  return (
    <div className="space-y-8">
      <PageHeader
        breadcrumb="Staff · Administrator"
        title="Admin Console"
        subtitle="Manage branches and staff users. All counts and lists are pulled live from the database."
        actions={
          <>
            <Button variant="secondary" icon={UserPlus} onClick={() => setCreateStaffOpen(true)}>
              Create staff user
            </Button>
            <Button icon={Plus} onClick={() => setCreateBranchOpen(true)}>
              Create branch
            </Button>
          </>
        }
      />

      {/* KPIs */}
      <section className="grid sm:grid-cols-2 xl:grid-cols-4 gap-5">
        <KpiCard label="Total branches" value={branchCounts.total}
                 sub={`${branchCounts.active} active`} icon={Building2} accent="brand" />
        <KpiCard label="Inactive branches" value={branchCounts.inactive}
                 icon={AlertTriangle} accent="gold" />
        <KpiCard label="Total staff" value={staffCounts.total}
                 sub={`${staffCounts.active} active`} icon={Users} accent="info" />
        <KpiCard label="Blocked / suspended" value={staffCounts.blocked}
                 icon={PauseCircle} accent="green" />
      </section>

      {/* Tabs */}
      <div className="flex gap-2 border-b border-accent-line dark:border-ink-700">
        {[
          { key: 'overview', label: 'Overview' },
          { key: 'branches', label: `Branches (${branchCounts.total})` },
          { key: 'staff',    label: `Staff (${staffCounts.total})` },
        ].map((t) => (
          <button
            key={t.key}
            onClick={() => setTab(t.key)}
            className={`px-4 py-2.5 text-sm font-semibold border-b-2 -mb-px transition focus-ring ${
              tab === t.key
                ? 'border-brand-700 text-brand-700 dark:text-brand-300'
                : 'border-transparent text-accent-mute dark:text-ink-400 hover:text-accent-ink dark:hover:text-ink-100'
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {tab === 'overview' && (
        <OverviewPanel
          branches={branches}
          summary={summary}
          loadingBranches={loadingBranches}
          openCreateBranch={() => setCreateBranchOpen(true)}
          openCreateStaff={() => setCreateStaffOpen(true)}
          onReloadBranches={reloadBranches}
        />
      )}

      {tab === 'branches' && (
        <BranchesPanel
          branches={branches}
          loading={loadingBranches}
          onReload={reloadBranches}
          onCreate={() => setCreateBranchOpen(true)}
        />
      )}

      {tab === 'staff' && (
        <StaffPanel
          staff={staff}
          loading={loadingStaff}
          summary={summary}
          branches={branches}
          roleFilter={roleFilter}     setRoleFilter={setRoleFilter}
          statusFilter={statusFilter} setStatusFilter={setStatusFilter}
          branchFilter={branchFilter} setBranchFilter={setBranchFilter}
          onReload={reloadStaff}
          onSetStatus={setStaffStatus}
          onCreate={() => setCreateStaffOpen(true)}
        />
      )}

      <CreateBranchModal
        open={createBranchOpen}
        onClose={() => setCreateBranchOpen(false)}
        onCreated={() => { setCreateBranchOpen(false); reloadBranches(); }}
      />

      <CreateStaffModal
        open={createStaffOpen}
        branches={branches}
        onClose={() => setCreateStaffOpen(false)}
        onCreated={() => { setCreateStaffOpen(false); reloadStaff(); }}
      />
    </div>
  );
}

//  OVERVIEW
function OverviewPanel({ branches, summary, loadingBranches, openCreateBranch, openCreateStaff, onReloadBranches }) {
  const recentBranches = branches.slice(0, 5);
  const roleEntries    = summary ? Object.entries(summary.byRole || {}) : [];

  return (
    <div className="grid lg:grid-cols-[1.4fr_1fr] gap-6">
      <Card>
        <CardHeader
          title="Recent branches"
          subtitle="Latest branches in the system"
          action={<Button variant="ghost" size="sm" icon={RefreshCw} onClick={onReloadBranches}>Refresh</Button>}
          className="mb-4"
        />
        {loadingBranches ? (
          <div className="space-y-2">{Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-14" />)}</div>
        ) : recentBranches.length === 0 ? (
          <EmptyState
            icon={Building2}
            title="No branches yet"
            message="Create your first branch to start onboarding staff and customers."
            action={<Button icon={Plus} onClick={openCreateBranch}>Create first branch</Button>}
          />
        ) : (
          <BranchTable branches={recentBranches} />
        )}
      </Card>

      <div className="space-y-5">
        <Card>
          <CardHeader title="Staff by role" subtitle="Counts pulled from identity-service" className="mb-4" />
          {summary == null ? (
            <Skeleton className="h-32" />
          ) : roleEntries.length === 0 ? (
            <p className="text-sm text-accent-mute dark:text-ink-400">No staff yet.</p>
          ) : (
            <ul className="space-y-2.5">
              {roleEntries.map(([role, count]) => (
                <li key={role} className="flex items-center justify-between text-sm">
                  <span className="text-accent-slate dark:text-ink-300">{ROLE_LABEL[role] || role}</span>
                  <Badge tone="brand">{count}</Badge>
                </li>
              ))}
            </ul>
          )}
        </Card>

        <Card>
          <CardHeader title="Quick actions" subtitle="Most common admin tasks" className="mb-4" />
          <div className="space-y-2.5">
            <QuickAction icon={Plus}     label="Create new branch"    onClick={openCreateBranch} />
            <QuickAction icon={UserPlus} label="Create staff user"     onClick={openCreateStaff} />
          </div>
        </Card>

        <div className="card p-5 bg-gradient-to-br from-brand-50 to-amber-50 dark:from-brand-900/30 dark:to-amber-900/20 border-brand-100 dark:border-brand-900/40">
          <span className="h-10 w-10 rounded-full bg-white dark:bg-ink-800 grid place-items-center text-brand-700 dark:text-brand-300">
            <ShieldCheck size={18} />
          </span>
          <h4 className="mt-3 font-semibold dark:text-ink-100">Admin scope</h4>
          <ul className="mt-3 space-y-2 text-sm text-accent-slate dark:text-ink-300">
            <li>• Create / deactivate branches</li>
            <li>• Create / suspend / block staff users</li>
            <li>• Filter staff by role, status, branch</li>
            <li>• All counts are live from the database</li>
          </ul>
        </div>
      </div>
    </div>
  );
}

//  BRANCHES PANEL
function BranchesPanel({ branches, loading, onReload, onCreate }) {
  return (
    <Card>
      <CardHeader
        title="All branches"
        subtitle="Branches you've created. Operational controls (view accounts, activate/deactivate) live in the Branch Manager dashboard."
        action={
          <div className="flex gap-2">
            <Button variant="ghost" size="sm" icon={RefreshCw} onClick={onReload}>Refresh</Button>
            <Button size="sm" icon={Plus} onClick={onCreate}>New branch</Button>
          </div>
        }
        className="mb-4"
      />
      {loading ? (
        <div className="space-y-2">{Array.from({ length: 5 }).map((_, i) => <Skeleton key={i} className="h-14" />)}</div>
      ) : branches.length === 0 ? (
        <EmptyState icon={Building2} title="No branches yet" message="Create the first branch to begin." />
      ) : (
        <BranchTable branches={branches} />
      )}
    </Card>
  );
}

// Read-only branch table. The admin role is reduced to creating branches and
// surveying them — the View / Activate / Deactivate actions now live in the
// Branch Manager dashboard.
function BranchTable({ branches }) {
  return (
    <div className="overflow-x-auto -mx-6">
      <table className="w-full text-sm">
        <thead className="text-left text-[11px] uppercase tracking-widest text-accent-mute dark:text-ink-400 border-b border-accent-line dark:border-ink-700">
          <tr>
            <th className="px-6 py-3">Branch</th>
            <th className="py-3">Type</th>
            <th className="py-3">Location</th>
            <th className="py-3">IFSC</th>
            <th className="py-3 pr-6">Status</th>
          </tr>
        </thead>
        <tbody>
          {branches.map((b) => (
            <tr key={b.branchCode} className="border-b border-accent-line/60 dark:border-ink-700/60 last:border-b-0 hover:bg-accent-surface/40 dark:hover:bg-ink-750/40">
              <td className="px-6 py-3">
                <div className="font-semibold dark:text-ink-100">{b.branchName}</div>
                <div className="text-xs font-mono text-accent-mute dark:text-ink-400">{b.branchCode}</div>
              </td>
              <td className="py-3"><Badge tone="neutral">{b.branchType}</Badge></td>
              <td className="py-3 text-accent-slate dark:text-ink-300">
                {b.address?.city || '—'}, {b.address?.state || '—'}
              </td>
              <td className="py-3 font-mono text-xs">{b.ifscCode || '—'}</td>
              <td className="py-3 pr-6"><StatusBadge status={b.status} /></td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

//  STAFF PANEL
function StaffPanel({
  staff, loading, summary, branches,
  roleFilter, setRoleFilter, statusFilter, setStatusFilter, branchFilter, setBranchFilter,
  onReload, onSetStatus, onCreate,
}) {
  return (
    <div className="space-y-5">
      <Card>
        <CardHeader
          title="Filters"
          subtitle="Slice the staff directory"
          className="mb-4"
        />
        <div className="grid sm:grid-cols-3 gap-3">
          <Select label="Role" value={roleFilter} onChange={(e) => setRoleFilter(e.target.value)}>
            <option value="">All roles</option>
            {STAFF_ROLES.map((r) => <option key={r.value} value={r.value}>{r.label}</option>)}
            <option value="ADMIN">Administrator</option>
          </Select>
          <Select label="Status" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
            <option value="">All statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="BLOCKED">Blocked</option>
            <option value="SUSPENDED">Suspended</option>
          </Select>
          <Select label="Branch" value={branchFilter} onChange={(e) => setBranchFilter(e.target.value)}>
            <option value="">All branches</option>
            {branches.map((b) => (
              <option key={b.branchCode} value={b.branchCode}>{b.branchCode} — {b.branchName}</option>
            ))}
          </Select>
        </div>
      </Card>

      <Card>
        <CardHeader
          title="Staff directory"
          subtitle={`${staff.length} matching · live data from identity-service`}
          action={
            <div className="flex gap-2">
              <Button variant="ghost" size="sm" icon={RefreshCw} onClick={onReload}>Refresh</Button>
              <Button size="sm" icon={UserPlus} onClick={onCreate}>New staff user</Button>
            </div>
          }
          className="mb-4"
        />
        {loading ? (
          <div className="space-y-2">{Array.from({ length: 6 }).map((_, i) => <Skeleton key={i} className="h-14" />)}</div>
        ) : staff.length === 0 ? (
          <EmptyState icon={Users} title="No matching staff" message="Adjust filters or create a new staff user." />
        ) : (
          <div className="overflow-x-auto -mx-6">
            <table className="w-full text-sm">
              <thead className="text-left text-[11px] uppercase tracking-widest text-accent-mute dark:text-ink-400 border-b border-accent-line dark:border-ink-700">
                <tr>
                  <th className="px-6 py-3">Name</th>
                  <th className="py-3">Role</th>
                  <th className="py-3">Branch</th>
                  <th className="py-3">Email · Phone</th>
                  <th className="py-3">Status</th>
                  <th className="py-3">Last login</th>
                  <th className="py-3 text-right pr-6">Actions</th>
                </tr>
              </thead>
              <tbody>
                {staff.map((u) => (
                  <tr key={u.id} className="border-b border-accent-line/60 dark:border-ink-700/60 last:border-b-0 hover:bg-accent-surface/40 dark:hover:bg-ink-750/40">
                    <td className="px-6 py-3">
                      <div className="font-semibold dark:text-ink-100">{u.fullName}</div>
                      <div className="text-[11px] font-mono text-accent-mute dark:text-ink-400">#{u.id}</div>
                    </td>
                    <td className="py-3"><Badge tone="brand">{ROLE_LABEL[u.role] || u.role}</Badge></td>
                    <td className="py-3 font-mono text-xs">{u.branchCode || '—'}</td>
                    <td className="py-3 text-accent-slate dark:text-ink-300">
                      <div className="truncate max-w-[220px]">{u.email}</div>
                      <div className="text-xs text-accent-mute dark:text-ink-400">{u.phoneNumber || '—'}</div>
                    </td>
                    <td className="py-3"><StatusBadge status={u.status} /></td>
                    <td className="py-3 text-xs text-accent-slate dark:text-ink-300">
                      {u.lastLogin ? formatDate(u.lastLogin) : 'Never'}
                    </td>
                    <td className="py-3 pr-6 text-right space-x-1">
                      {u.status === 'ACTIVE' ? (
                        <>
                          <Button size="sm" variant="ghost" icon={PauseCircle}
                                  onClick={() => onSetStatus(u, 'SUSPENDED')}>
                            Suspend
                          </Button>
                          <Button size="sm" variant="ghost" icon={Power}
                                  onClick={() => onSetStatus(u, 'BLOCKED')}>
                            Block
                          </Button>
                        </>
                      ) : (
                        <Button size="sm" variant="ghost" icon={PlayCircle}
                                onClick={() => onSetStatus(u, 'ACTIVE')}>
                          Reactivate
                        </Button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </div>
  );
}

//  Reusable UI
function QuickAction({ icon: Icon, label, onClick }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="w-full flex items-center gap-3 px-4 py-3 rounded-xl border border-accent-line dark:border-ink-700 hover:bg-brand-50/60 dark:hover:bg-brand-900/20 hover:border-brand-200 dark:hover:border-brand-900 transition group focus-ring"
    >
      <span className="h-10 w-10 rounded-xl bg-brand-50 dark:bg-brand-900/40 grid place-items-center text-brand-700 dark:text-brand-300 group-hover:bg-brand-700 group-hover:text-white dark:group-hover:bg-brand-600 transition">
        <Icon size={16} />
      </span>
      <span className="font-semibold text-accent-ink dark:text-ink-100">{label}</span>
    </button>
  );
}

//  Create Branch modal
function CreateBranchModal({ open, onClose, onCreated }) {
  const [submitting, setSubmitting] = useState(false);
  const { register, handleSubmit, reset, setError, formState: { errors } } = useForm({
    mode: 'onTouched',
    defaultValues: {
      branchCode: '', branchName: '', branchType: 'URBAN',
      addressLine1: '', addressLine2: '', city: '', state: '',
      postalCode: '', country: 'India',
      primaryPhone: '', secondaryPhone: '', email: '',
      hasAtm: false, has24x7Service: false, remarks: '',
    },
  });

  useEffect(() => { if (!open) reset(); }, [open]); // eslint-disable-line react-hooks/exhaustive-deps

  const onSubmit = async (v) => {
    setSubmitting(true);
    try {
      const payload = {
        branchCode: v.branchCode.toUpperCase(),
        branchName: v.branchName,
        branchType: v.branchType,
        address: {
          addressLine1: v.addressLine1,
          addressLine2: v.addressLine2 || null,
          city: v.city,
          state: v.state,
          postalCode: v.postalCode,
          country: v.country,
        },
        contact: {
          primaryPhone: v.primaryPhone,
          secondaryPhone: v.secondaryPhone || null,
          email: v.email || null,
        },
        hasAtm: !!v.hasAtm,
        has24x7Service: !!v.has24x7Service,
        remarks: v.remarks || null,
      };
      const created = await branchApi.create(payload);
      toast.success(`Branch created: ${created.branchCode}`);
      reset();
      onCreated?.();
    } catch (err) {
      // Nested DTO fields (e.g. "address.city") come back from the backend as
      // qualified paths — strip the prefix so they land on the flat form field.
      const mappedSetError = (field, opts) => {
        const flat = String(field).includes('.') ? field.split('.').pop() : field;
        setError(flat, opts);
      };
      const applied = applyServerErrors(err, mappedSetError);
      if (applied) {
        toast.error('Please fix the highlighted fields and try again.');
      } else {
        toast.error(errorMessage(err, 'Could not create branch.'));
      }
    } finally { setSubmitting(false); }
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Create a new branch"
      description="Branch details and contact information. IFSC is auto-generated if you don't supply one."
      size="xl"
      footer={
        <div className="flex justify-end gap-2">
          <Button variant="ghost" onClick={onClose}>Cancel</Button>
          <Button onClick={handleSubmit(onSubmit)} loading={submitting} icon={Plus}>Create branch</Button>
        </div>
      }
    >
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
        <Section title="Identity">
          <div className="grid sm:grid-cols-2 gap-4">
            <Input
              label="Branch code" required placeholder="e.g. BR001"
              hint="2-10 uppercase letters/digits"
              error={errors.branchCode?.message}
              {...register('branchCode', {
                required: 'Required',
                pattern: { value: /^[A-Za-z0-9]{2,10}$/, message: '2-10 alphanumeric chars' },
              })}
            />
            <Input
              label="Branch name" required placeholder="e.g. Pune Koregaon Park"
              error={errors.branchName?.message}
              {...register('branchName', {
                required: 'Required',
                minLength: { value: 3, message: 'At least 3 characters' },
              })}
            />
            <Select label="Branch type" required {...register('branchType')}>
              {BRANCH_TYPES.map((t) => <option key={t} value={t}>{t.replace(/_/g, ' ')}</option>)}
            </Select>
          </div>
        </Section>

        <Section title="Address">
          <div className="grid sm:grid-cols-2 gap-4">
            <Input label="Address line 1" required {...register('addressLine1', { required: 'Required' })} error={errors.addressLine1?.message} />
            <Input label="Address line 2" {...register('addressLine2')} />
            <Input label="City"  required {...register('city',  { required: 'Required' })} error={errors.city?.message} />
            <Input label="State" required {...register('state', { required: 'Required' })} error={errors.state?.message} />
            <Input
              label="Postal code"
              {...register('postalCode', { pattern: { value: /^\d{5,6}$/, message: '5-6 digits' } })}
              error={errors.postalCode?.message}
            />
            <Input label="Country" required {...register('country', { required: 'Required' })} />
          </div>
        </Section>

        <Section title="Contact">
          <div className="grid sm:grid-cols-2 gap-4">
            <Input
              label="Primary phone" required placeholder="10-15 digits"
              error={errors.primaryPhone?.message}
              {...register('primaryPhone', {
                required: 'Required',
                pattern: { value: /^\+?[0-9]{10,15}$/, message: '10-15 digits' },
              })}
            />
            <Input
              label="Secondary phone"
              {...register('secondaryPhone', { pattern: { value: /^$|^\+?[0-9]{10,15}$/, message: '10-15 digits' } })}
              error={errors.secondaryPhone?.message}
            />
            <Input
              label="Branch email" type="email"
              {...register('email', { pattern: { value: /^$|^\S+@\S+\.\S+$/, message: 'Valid email' } })}
              error={errors.email?.message}
            />
          </div>
        </Section>

        <Section title="Facilities">
          <div className="grid sm:grid-cols-2 gap-3">
            <Checkbox label="Has ATM" {...register('hasAtm')} />
            <Checkbox label="24×7 service" {...register('has24x7Service')} />
          </div>
        </Section>

        <Section title="Notes">
          <Textarea label="Remarks (optional)" maxLength={500} {...register('remarks')} />
        </Section>
      </form>
    </Modal>
  );
}

//  Create Staff modal
function CreateStaffModal({ open, onClose, onCreated, branches }) {
  const [submitting, setSubmitting] = useState(false);
  const { register, handleSubmit, watch, reset, setError, formState: { errors } } = useForm({
    mode: 'onTouched',
    defaultValues: {
      fullName: '', email: '', password: '',
      phoneNumber: '', dateOfBirth: '',
      role: 'CSR', branchCode: '',
    },
  });
  const selectedRole = STAFF_ROLES.find((r) => r.value === watch('role'));
  const needsBranch = !!selectedRole?.needsBranch;

  useEffect(() => { if (!open) reset(); }, [open]); // eslint-disable-line react-hooks/exhaustive-deps

  const onSubmit = async (v) => {
    if (needsBranch && !v.branchCode) {
      toast.error(`A branch is required for ${selectedRole.label}.`);
      return;
    }
    setSubmitting(true);
    try {
      const payload = {
        fullName: v.fullName.trim(),
        email: v.email.trim().toLowerCase(),
        password: v.password,
        phoneNumber: v.phoneNumber.trim(),
        dateOfBirth: v.dateOfBirth || null,
        role: v.role,
        branchCode: needsBranch ? v.branchCode : null,
      };
      const created = await adminApi.createStaffUser(payload);
      toast.success(`${selectedRole.label} created: ${created.email || payload.email}`);
      reset();
      onCreated?.();
    } catch (err) {
      const applied = applyServerErrors(err, setError);
      if (applied) {
        toast.error('Please fix the highlighted fields and try again.');
      } else {
        toast.error(errorMessage(err, 'Could not create staff user.'));
      }
    } finally { setSubmitting(false); }
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Create a staff user"
      description="The new user receives the credentials you set here. They can change their password after first sign-in."
      size="lg"
      footer={
        <div className="flex justify-end gap-2">
          <Button variant="ghost" onClick={onClose}>Cancel</Button>
          <Button onClick={handleSubmit(onSubmit)} loading={submitting} icon={Send}>Create user</Button>
        </div>
      }
    >
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
        <div className="grid sm:grid-cols-2 gap-4">
          <Select label="Role" required {...register('role', { required: true })}>
            {STAFF_ROLES.map((r) => <option key={r.value} value={r.value}>{r.label}</option>)}
          </Select>
          <Select
            label={`Branch${needsBranch ? '' : ' (not required)'}`}
            required={needsBranch}
            disabled={!needsBranch}
            hint={!needsBranch ? `${selectedRole.label} doesn't need a branch.` : null}
            {...register('branchCode')}
          >
            <option value="">{needsBranch ? 'Select a branch' : '—'}</option>
            {(branches || []).map((b) => (
              <option key={b.branchCode} value={b.branchCode}>
                {b.branchCode} — {b.branchName}
              </option>
            ))}
          </Select>
        </div>

        <Input
          label="Full name" required placeholder="As per government ID"
          error={errors.fullName?.message}
          {...register('fullName', { required: 'Required', minLength: { value: 3, message: 'At least 3 characters' } })}
        />

        <div className="grid sm:grid-cols-2 gap-4">
          <Input
            label="Email" required type="email" leftIcon={Mail}
            error={errors.email?.message}
            {...register('email', {
              required: 'Required',
              pattern: { value: /^\S+@\S+\.\S+$/, message: 'Valid email required' },
            })}
          />
          <Input
            label="Phone" required leftIcon={Phone} placeholder="10-digit number"
            error={errors.phoneNumber?.message}
            {...register('phoneNumber', {
              required: 'Required',
              pattern: { value: /^[0-9]{10}$/, message: '10 digits required' },
            })}
          />
          <Input
            label="Date of birth" type="date" leftIcon={Calendar}
            error={errors.dateOfBirth?.message}
            {...register('dateOfBirth')}
          />
          <Input
            label="Initial password" required type="password" leftIcon={KeyRound}
            hint="At least 8 characters. The user can change it later."
            error={errors.password?.message}
            {...register('password', {
              required: 'Required',
              minLength: { value: 8, message: 'At least 8 characters' },
            })}
          />
        </div>
      </form>
    </Modal>
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

// Branch Detail modal / per-account-type breakdown / freeze-close UI used to
// live here. It has moved to the Branch Manager dashboard — admin only
// creates branches and staff now.

function Checkbox({ label, ...rest }) {
  return (
    <label className="flex items-center gap-2.5 p-3 rounded-lg border border-accent-line dark:border-ink-700 cursor-pointer hover:bg-accent-surface/40 dark:hover:bg-ink-750">
      <input type="checkbox" className="h-4 w-4 accent-brand-700" {...rest} />
      <span className="text-sm dark:text-ink-100">{label}</span>
    </label>
  );
}
