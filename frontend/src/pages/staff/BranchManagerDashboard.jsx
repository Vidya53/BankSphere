import { useEffect, useMemo, useState } from 'react';
import {
  Users, Wallet, Landmark, MessageSquare, CheckCircle2,
  Phone, Mail, MapPin, Clock, Building2, Map as MapIcon,
  PauseCircle, PlayCircle, Send, FileSearch, Settings2, Save,
} from 'lucide-react';
import toast from 'react-hot-toast';
import { PageHeader } from '../../components/layout/PageHeader';
import { Card, CardHeader } from '../../components/common/Card';
import { Skeleton } from '../../components/common/Skeleton';
import { Badge, StatusBadge } from '../../components/common/Badge';
import { Button } from '../../components/common/Button';
import { Modal } from '../../components/common/Modal';
import { EmptyState } from '../../components/common/EmptyState';
import { KpiCard } from '../../components/dashboard/KpiCard';
import { BarTrend } from '../../components/dashboard/BarTrend';
import { useAuth } from '../../context/AuthContext';
import { dashboardsApi } from '../../api/dashboards';
import { branchApi } from '../../api/branch';
import { errorMessage } from '../../api/client';
import { formatCompactINR, initials } from '../../utils/format';
import { ROLES } from '../../utils/roleRoutes';
import BranchAccountsPanel from '../../components/staff/BranchAccountsPanel';

const DAY_ORDER  = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];
const DAY_SHORT  = { MONDAY: 'Mon', TUESDAY: 'Tue', WEDNESDAY: 'Wed', THURSDAY: 'Thu', FRIDAY: 'Fri', SATURDAY: 'Sat', SUNDAY: 'Sun' };
const ROLE_LABEL = {
  CSR: 'Customer Service Rep', LOAN_OFFICER: 'Loan Officer',
  BRANCH_MANAGER: 'Branch Manager', ADMIN: 'Administrator',
  // Legacy fallback for any pre-existing COMPLIANCE_OFFICER row in the DB
  COMPLIANCE_OFFICER: 'Compliance Officer (legacy)',
};

export default function BranchManagerDashboard() {
  const { user }   = useAuth();
  const isAdmin    = user?.role === ROLES.ADMIN;

  const [branches, setBranches] = useState([]);
  const [selected, setSelected] = useState(null);
  const [dashboard, setDashboard] = useState(null);
  const [branchInfo, setBranchInfo] = useState(null);
  const [hours, setHours] = useState([]);
  const [openNow, setOpenNow] = useState(null);
  const [loadingBranches, setLoadingBranches] = useState(true);
  const [loadingBranch, setLoadingBranch] = useState(false);
  const [hoursModalOpen, setHoursModalOpen] = useState(false);

  const refreshHours = async () => {
    if (!selected) return;
    const [hrs, ison] = await Promise.all([
      branchApi.operatingHours(selected).catch(() => []),
      branchApi.isOpenNow(selected).catch(() => null),
    ]);
    setHours(Array.isArray(hrs) ? hrs : []);
    setOpenNow(ison);
  };

  // 1) Pull the branch list once so we can populate the switcher and resolve
  //    the right initial selection. Admin sees all; branch manager is locked
  //    to their own assigned branch.
  useEffect(() => {
    (async () => {
      setLoadingBranches(true);
      try {
        const list = await branchApi.listAll().catch(() => []);
        setBranches(list);
        const initial =
          (user?.branchCode && list.find((b) => b.branchCode === user.branchCode)?.branchCode)
          || list.find((b) => b.status === 'ACTIVE')?.branchCode
          || list[0]?.branchCode
          || null;
        setSelected(initial);
      } finally { setLoadingBranches(false); }
    })();
  }, [user?.branchCode]);

  // 2) Whenever the selected branch changes, fetch dashboard + details + hours
  //    in parallel. We don't await each separately to keep the UI snappy.
  useEffect(() => {
    if (!selected) { setDashboard(null); setBranchInfo(null); setHours([]); return; }
    let cancelled = false;
    (async () => {
      setLoadingBranch(true);
      try {
        const [dash, info, hrs, openNowResp] = await Promise.all([
          dashboardsApi.branchManager(selected).catch(() => null),
          branchApi.get(selected).catch(() => null),
          branchApi.operatingHours(selected).catch(() => []),
          branchApi.isOpenNow(selected).catch(() => null),
        ]);
        if (cancelled) return;
        setDashboard(dash);
        setBranchInfo(info);
        setHours(Array.isArray(hrs) ? hrs : []);
        setOpenNow(openNowResp);
      } finally { if (!cancelled) setLoadingBranch(false); }
    })();
    return () => { cancelled = true; };
  }, [selected]);

  const onToggleStatus = async () => {
    if (!branchInfo) return;
    const next = branchInfo.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    const verb = next === 'ACTIVE' ? 'Activate' : 'Deactivate';
    if (!confirm(`${verb} branch ${branchInfo.branchCode} — ${branchInfo.branchName}?`)) return;
    try {
      await branchApi.updateStatus(branchInfo.branchCode, next);
      toast.success(`Branch ${branchInfo.branchCode} → ${next}`);
      // refresh details + the list (so KPIs on Admin page also update later)
      const [info, list] = await Promise.all([
        branchApi.get(branchInfo.branchCode).catch(() => branchInfo),
        branchApi.listAll().catch(() => branches),
      ]);
      setBranchInfo(info);
      setBranches(list);
    } catch (err) {
      toast.error(errorMessage(err, 'Could not update branch status.'));
    }
  };

  // ── Render guards ────────────────────────────────────────────────────────
  if (loadingBranches) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-24 rounded-xl2" />
        <div className="grid sm:grid-cols-2 xl:grid-cols-4 gap-5">
          {Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-28 rounded-xl2" />)}
        </div>
        <Skeleton className="h-96 rounded-xl2" />
      </div>
    );
  }

  if (branches.length === 0) {
    return (
      <EmptyState
        icon={Building2}
        title="No branches yet"
        message="Branches need to be created from the Admin Console before this view is meaningful."
      />
    );
  }

  if (!selected || !branchInfo) {
    return (
      <div className="space-y-6">
        <BranchSwitcher
          branches={branches} selected={selected} setSelected={setSelected} locked={!isAdmin}
        />
        {loadingBranch ? <Skeleton className="h-96 rounded-xl2" /> : null}
      </div>
    );
  }

  const kpis  = dashboard?.kpis  || {};
  const tgt   = dashboard?.targets || {};
  const staff = Array.isArray(dashboard?.staffPerformance) ? dashboard.staffPerformance : [];
  const daily = Array.isArray(dashboard?.dailyFootfall)   ? dashboard.dailyFootfall   : [];

  return (
    <div className="space-y-8">
      {/* ── Switcher + admin actions ─────────────────────────────────────── */}
      <BranchSwitcher
        branches={branches}
        selected={selected}
        setSelected={setSelected}
        locked={!isAdmin}
        action={
          isAdmin ? (
            <Button
              size="sm"
              variant={branchInfo.status === 'ACTIVE' ? 'secondary' : 'primary'}
              icon={branchInfo.status === 'ACTIVE' ? PauseCircle : PlayCircle}
              onClick={onToggleStatus}
            >
              {branchInfo.status === 'ACTIVE' ? 'Deactivate branch' : 'Activate branch'}
            </Button>
          ) : null
        }
      />

      {/* ── Header ───────────────────────────────────────────────────────── */}
      <PageHeader
        breadcrumb={`Staff · ${isAdmin ? 'Admin View' : 'Branch Manager'} · ${branchInfo.branchCode}`}
        title={branchInfo.branchName}
        subtitle={
          <span className="flex items-center flex-wrap gap-2 text-sm">
            <Badge tone="neutral">{branchInfo.branchType}</Badge>
            <StatusBadge status={branchInfo.status} />
            {openNow != null && (
              <Badge tone={openNow ? 'success' : 'warning'} dot>
                {openNow ? 'Open now' : 'Closed now'}
              </Badge>
            )}
            <span className="text-accent-mute dark:text-ink-400">
              IFSC <span className="font-mono">{branchInfo.ifscCode}</span>
              {' · '}{staff.length} staff
              {branchInfo.createdAt && (<> · open since {branchInfo.createdAt.slice(0, 10)}</>)}
            </span>
          </span>
        }
      />

      {/* ── KPIs (real from DB) ──────────────────────────────────────────── */}
      <section className="grid sm:grid-cols-2 xl:grid-cols-4 gap-5">
        <KpiCard label="New accounts (MTD)" value={kpis.newAccountsThisMonth ?? 0}
                 icon={Users} accent="brand" />
        <KpiCard label="Total deposits" value={formatCompactINR(kpis.totalDeposits ?? 0)}
                 icon={Wallet} accent="green" />
        <KpiCard label="Loans booked (MTD)" value={formatCompactINR(kpis.totalLoansBookedThisMonth ?? 0)}
                 sub="system-wide" icon={Landmark} accent="info" />
        <KpiCard label="Pending applications" value={kpis.complaintsResolvedThisMonth ?? 0}
                 sub={`${kpis.complaintsOpen ?? 0} HV transfers awaiting`} icon={MessageSquare} accent="gold" />
      </section>

      {/* ── Branch info card + Operating hours ───────────────────────────── */}
      <section className="grid lg:grid-cols-[1.4fr_1fr] gap-6">
        <BranchInfoCard info={branchInfo} />
        <OperatingHoursCard
          hours={hours}
          onConfigure={isAdmin ? () => setHoursModalOpen(true) : null}
        />
      </section>

      {/* ── Monthly targets + Daily footfall ─────────────────────────────── */}
      <section className="grid lg:grid-cols-2 gap-6">
        <Card>
          <CardHeader title="Monthly targets" subtitle="Branch performance against goals" className="mb-5" />
          <div className="space-y-4">
            <ProgressRow label="New accounts"
                         achieved={tgt.newAccounts?.achieved ?? 0}
                         target={tgt.newAccounts?.target ?? 0}
                         pct={tgt.newAccounts?.percent ?? 0} />
            <ProgressRow label="Deposits"
                         achieved={formatCompactINR(tgt.deposits?.achieved ?? 0)}
                         target={formatCompactINR(tgt.deposits?.target ?? 0)}
                         pct={tgt.deposits?.percent ?? 0} />
            <ProgressRow label="Loans"
                         achieved={formatCompactINR(tgt.loans?.achieved ?? 0)}
                         target={formatCompactINR(tgt.loans?.target ?? 0)}
                         pct={tgt.loans?.percent ?? 0} />
            <ProgressRow label="CSAT"
                         achieved={tgt.csat?.achieved ?? 0}
                         target={tgt.csat?.target ?? 0}
                         pct={tgt.csat?.percent ?? 0} />
          </div>
        </Card>

        <BarTrend
          data={daily}
          title="Daily branch footfall"
          subtitle="Proxied from new-account openings (last 7 days)"
          dataKeys={['footfall', 'newAccounts', 'tickets']}
          colors={['#97144D', '#c9a35d', '#1e6fd6']}
        />
      </section>

      {/* ── Staff working at this branch (real, no fake scores) ──────────── */}
      <Card>
        <CardHeader
          title={`Staff at this branch (${staff.length})`}
          subtitle="Pulled live from identity-service"
          className="mb-4"
        />
        {staff.length === 0 ? (
          <p className="text-sm text-accent-mute dark:text-ink-400">
            No staff have been assigned to this branch yet. Use{' '}
            <span className="font-semibold">Admin Console → Create staff user</span> to onboard them.
          </p>
        ) : (
          <div className="overflow-x-auto -mx-6">
            <table className="w-full text-sm">
              <thead className="text-left text-[11px] uppercase tracking-widest text-accent-mute dark:text-ink-400 border-b border-accent-line dark:border-ink-700">
                <tr>
                  <th className="px-6 py-3">Name</th>
                  <th className="py-3">Role</th>
                  <th className="py-3">Email</th>
                  <th className="py-3">Status</th>
                </tr>
              </thead>
              <tbody>
                {staff.map((s) => (
                  <tr key={s.id} className="border-b border-accent-line/60 dark:border-ink-700/60 last:border-b-0 hover:bg-accent-surface/40 dark:hover:bg-ink-750/40">
                    <td className="px-6 py-3 flex items-center gap-3">
                      <span className="h-9 w-9 rounded-full bg-brand-700 text-white grid place-items-center text-xs font-semibold">
                        {initials(s.name || 'NA')}
                      </span>
                      <span className="font-semibold dark:text-ink-100">{s.name || '—'}</span>
                    </td>
                    <td className="py-3"><Badge tone="brand">{ROLE_LABEL[s.role] || s.role}</Badge></td>
                    <td className="py-3 text-accent-slate dark:text-ink-300 truncate max-w-[260px]">
                      {s.email || '—'}
                    </td>
                    <td className="py-3"><StatusBadge status={s.status} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      {/* ── Customer accounts at this branch (BM-only) ───────────────────── */}
      {/*  Branch managers manage the lifecycle of accounts (freeze / unfreeze /
          close) and can soft-delete customers when offboarding. The panel
          calls /staff/accounts/my-branch which uses the caller's X-Branch-Code
          — admins have no branch and shouldn't surface this section. */}
      {user?.role === ROLES.BRANCH_MANAGER && (
        <BranchAccountsPanel branchLabel={branchInfo?.branchName} />
      )}

      {/* ── Quick links (admin gets editing entry-points) ────────────────── */}
      {isAdmin && (
        <Card>
          <CardHeader title="Quick branch actions" subtitle="Common admin operations" className="mb-4" />
          <div className="grid sm:grid-cols-3 gap-3">
            <QuickLink icon={Users}        label="Staff directory"     to="/staff/admin" sub="Open the admin staff list" />
            <QuickLink icon={FileSearch}   label="Account applications" to="/staff/account-applications" sub="Review pending KYC + accounts" />
            <QuickLink icon={Send}         label="Pending transfers"    to="/staff/pending-transfers"    sub="Approve high-value transfers" />
          </div>
        </Card>
      )}

      <HoursConfigModal
        open={hoursModalOpen}
        branchCode={selected}
        branchName={branchInfo.branchName}
        hours={hours}
        onClose={() => setHoursModalOpen(false)}
        onSaved={async () => {
          await refreshHours();
          setHoursModalOpen(false);
        }}
      />
    </div>
  );
}

//  Branch switcher
function BranchSwitcher({ branches, selected, setSelected, locked = false, action = null }) {
  const current = branches.find((b) => b.branchCode === selected);
  return (
    <div className="card p-5 flex flex-wrap items-end gap-4">
      <div className="flex-1 min-w-[280px]">
        <label className="text-[11px] font-semibold uppercase tracking-widest text-accent-mute dark:text-ink-400 mb-1.5 block">
          {locked ? 'Your branch' : 'Branch'}
        </label>
        {locked ? (
          <div className="flex items-center gap-3 py-2.5">
            <span className="h-10 w-10 rounded-xl bg-brand-700 text-white grid place-items-center">
              <Building2 size={18} />
            </span>
            <div>
              <p className="font-semibold dark:text-ink-100">{current?.branchName || selected}</p>
              <p className="text-xs font-mono text-accent-mute dark:text-ink-400">{current?.branchCode}</p>
            </div>
          </div>
        ) : (
          <div className="flex items-center gap-3">
            <span className="h-10 w-10 rounded-xl bg-brand-700 text-white grid place-items-center">
              <Building2 size={18} />
            </span>
            <select
              value={selected || ''}
              onChange={(e) => setSelected(e.target.value)}
              className="flex-1 min-w-[260px] h-11 px-3 rounded-xl border border-accent-line dark:border-ink-700 bg-white dark:bg-ink-800 dark:text-ink-100 text-sm font-semibold focus-ring"
            >
              {branches.map((b) => (
                <option key={b.branchCode} value={b.branchCode}>
                  {b.branchCode} — {b.branchName}
                  {b.status !== 'ACTIVE' ? `  (${b.status})` : ''}
                </option>
              ))}
            </select>
          </div>
        )}
      </div>
      {!locked && (
        <p className="text-xs text-accent-mute dark:text-ink-400 max-w-xs">
          Switch between branches to compare performance. Branch managers see only their own branch.
        </p>
      )}
      {action}
    </div>
  );
}

//  Branch info card
function BranchInfoCard({ info }) {
  const addr = info.address || {};
  const addressLine = [addr.addressLine1, addr.addressLine2].filter(Boolean).join(', ');
  const cityState   = [addr.city, addr.state, addr.postalCode].filter(Boolean).join(', ');
  const contact = info.contact || {};

  return (
    <Card>
      <CardHeader title="Branch information" subtitle="Address, contact and facilities" className="mb-4" />
      <div className="grid sm:grid-cols-2 gap-y-4 gap-x-6 text-sm">
        <InfoRow icon={MapPin} label="Address">
          <div className="font-semibold dark:text-ink-100">{addressLine || '—'}</div>
          <div className="text-accent-mute dark:text-ink-400">{cityState || '—'}</div>
          <div className="text-accent-mute dark:text-ink-400">{addr.country || '—'}</div>
        </InfoRow>

        <InfoRow icon={Phone} label="Phone">
          <div className="font-semibold dark:text-ink-100">{contact.primaryPhone || '—'}</div>
          {contact.secondaryPhone && (
            <div className="text-xs text-accent-mute dark:text-ink-400">Alt: {contact.secondaryPhone}</div>
          )}
        </InfoRow>

        <InfoRow icon={Mail} label="Email">
          <div className="font-semibold dark:text-ink-100 truncate">{contact.email || '—'}</div>
          {contact.fax && (
            <div className="text-xs text-accent-mute dark:text-ink-400">Fax: {contact.fax}</div>
          )}
        </InfoRow>

        <InfoRow icon={MapIcon} label="Coordinates">
          <div className="font-semibold dark:text-ink-100">
            {info.latitude != null && info.longitude != null
              ? `${info.latitude.toFixed(4)}, ${info.longitude.toFixed(4)}`
              : '—'}
          </div>
        </InfoRow>

        <InfoRow icon={Building2} label="Manager">
          <div className="font-semibold dark:text-ink-100">
            {info.branchManagerName || info.branchManagerCode || 'Unassigned'}
          </div>
          {info.branchManagerCode && info.branchManagerName && (
            <div className="text-xs font-mono text-accent-mute dark:text-ink-400">{info.branchManagerCode}</div>
          )}
        </InfoRow>

        <InfoRow icon={CheckCircle2} label="Facilities">
          <div className="flex flex-wrap gap-2 mt-0.5">
            <Badge tone={info.hasAtm ? 'success' : 'neutral'}>
              {info.hasAtm ? 'ATM available' : 'No ATM'}
            </Badge>
            <Badge tone={info.has24x7Service ? 'success' : 'neutral'}>
              {info.has24x7Service ? '24×7 service' : 'Working hours only'}
            </Badge>
          </div>
        </InfoRow>
      </div>

      {info.remarks && (
        <p className="mt-5 pt-4 border-t border-accent-line/70 dark:border-ink-700 text-xs text-accent-slate dark:text-ink-300">
          <span className="font-semibold">Notes:</span> {info.remarks}
        </p>
      )}
    </Card>
  );
}

function InfoRow({ icon: Icon, label, children }) {
  return (
    <div className="flex gap-3">
      <span className="h-9 w-9 rounded-lg bg-brand-50 dark:bg-brand-900/30 grid place-items-center text-brand-700 dark:text-brand-300 shrink-0">
        <Icon size={16} />
      </span>
      <div className="min-w-0">
        <div className="text-[11px] uppercase tracking-widest text-accent-mute dark:text-ink-400">{label}</div>
        <div className="mt-0.5 min-w-0">{children}</div>
      </div>
    </div>
  );
}

//  Operating hours card
function OperatingHoursCard({ hours, onConfigure }) {
  const byDay = useMemo(() => Object.fromEntries(hours.map((h) => [h.dayOfWeek, h])), [hours]);
  return (
    <Card>
      <CardHeader
        title="Operating hours"
        subtitle="Local branch hours by day"
        action={
          onConfigure ? (
            <Button size="sm" variant="secondary" icon={Settings2} onClick={onConfigure}>
              Configure
            </Button>
          ) : <Clock size={16} className="text-accent-info" />
        }
        className="mb-4"
      />
      {hours.length === 0 ? (
        <div className="text-sm text-accent-mute dark:text-ink-400 space-y-3">
          <p>Operating hours have not been configured for this branch yet.</p>
          {onConfigure && (
            <Button size="sm" icon={Settings2} onClick={onConfigure}>
              Set hours now
            </Button>
          )}
        </div>
      ) : (
        <ul className="divide-y divide-accent-line/60 dark:divide-ink-700/60 text-sm">
          {DAY_ORDER.map((day) => {
            const h = byDay[day];
            const closed = !h || h.isClosed || !h.openTime || !h.closeTime;
            return (
              <li key={day} className="flex items-center justify-between py-2.5">
                <span className="font-semibold dark:text-ink-100 w-20">{DAY_SHORT[day]}</span>
                {closed ? (
                  <Badge tone="neutral">Closed</Badge>
                ) : (
                  <span className="text-accent-slate dark:text-ink-300 font-mono text-xs">
                    {h.openTime} – {h.closeTime}
                  </span>
                )}
              </li>
            );
          })}
        </ul>
      )}
    </Card>
  );
}

//  Reusable bits
function ProgressRow({ label, achieved, target, pct }) {
  const capped = Math.min(pct, 100);
  const exceeded = pct > 100;
  return (
    <div>
      <div className="flex items-center justify-between mb-1.5">
        <p className="text-sm font-semibold text-accent-ink dark:text-ink-100 flex items-center gap-2">
          {label}
          {exceeded && <CheckCircle2 size={14} className="text-accent-success" />}
        </p>
        <p className="text-sm text-accent-slate dark:text-ink-300">
          <span className="font-semibold text-accent-ink dark:text-ink-100">{achieved}</span>
          <span className="text-accent-mute dark:text-ink-400"> / {target}</span>
          <span className={`ml-2 font-semibold ${exceeded ? 'text-accent-success' : 'text-brand-700'}`}>
            {Number(pct).toFixed(1)}%
          </span>
        </p>
      </div>
      <div className="h-2 rounded-full bg-accent-line/60 overflow-hidden">
        <div
          className={`h-full rounded-full ${exceeded ? 'bg-accent-success' : 'bg-brand-700'}`}
          style={{ width: `${capped}%` }}
        />
      </div>
    </div>
  );
}

function QuickLink({ icon: Icon, label, sub, to }) {
  return (
    <a
      href={to}
      className="block p-4 rounded-xl border border-accent-line dark:border-ink-700 hover:bg-brand-50/60 dark:hover:bg-brand-900/20 hover:border-brand-200 dark:hover:border-brand-900 transition focus-ring"
    >
      <div className="flex items-center gap-3">
        <span className="h-10 w-10 rounded-xl bg-brand-50 dark:bg-brand-900/40 grid place-items-center text-brand-700 dark:text-brand-300">
          <Icon size={16} />
        </span>
        <div>
          <p className="font-semibold text-accent-ink dark:text-ink-100">{label}</p>
          <p className="text-xs text-accent-mute dark:text-ink-400">{sub}</p>
        </div>
      </div>
    </a>
  );
}

//  Configure operating-hours modal
//
//  Lets admins set per-day open/close times. Sensible defaults: Mon–Fri 09:30
//  to 16:30, Saturday 10:00 to 13:00, Sunday closed.  Saving sends the full
//  week to PUT /api/v1/branches/{code}/operating-hours — the backend upserts
//  by day so the same payload works for both create and update.
const DAY_DEFAULTS = {
  MONDAY:    { isClosed: false, openTime: '09:30', closeTime: '16:30' },
  TUESDAY:   { isClosed: false, openTime: '09:30', closeTime: '16:30' },
  WEDNESDAY: { isClosed: false, openTime: '09:30', closeTime: '16:30' },
  THURSDAY:  { isClosed: false, openTime: '09:30', closeTime: '16:30' },
  FRIDAY:    { isClosed: false, openTime: '09:30', closeTime: '16:30' },
  SATURDAY:  { isClosed: false, openTime: '10:00', closeTime: '13:00' },
  SUNDAY:    { isClosed: true,  openTime: '',      closeTime: ''      },
};

function HoursConfigModal({ open, branchCode, branchName, hours, onClose, onSaved }) {
  const [submitting, setSubmitting] = useState(false);
  const [rows, setRows] = useState({});

  // Reset form whenever the modal opens or the source data changes
  useEffect(() => {
    if (!open) return;
    const seeded = { ...DAY_DEFAULTS };
    for (const h of hours || []) {
      seeded[h.dayOfWeek] = {
        isClosed:  !!h.isClosed || (!h.openTime || !h.closeTime),
        openTime:  h.openTime  ?? '',
        closeTime: h.closeTime ?? '',
      };
    }
    setRows(seeded);
  }, [open, hours]);

  const setDay = (day, patch) =>
    setRows((prev) => ({ ...prev, [day]: { ...prev[day], ...patch } }));

  const onSubmit = async () => {
    // Validate: every open day must have both open + close, and open < close
    const payload = [];
    for (const day of DAY_ORDER) {
      const r = rows[day] || DAY_DEFAULTS[day];
      if (r.isClosed) {
        payload.push({ dayOfWeek: day, isClosed: true, openTime: null, closeTime: null });
        continue;
      }
      if (!r.openTime || !r.closeTime) {
        toast.error(`${DAY_SHORT[day]}: set both open and close times, or mark as closed.`);
        return;
      }
      if (r.openTime >= r.closeTime) {
        toast.error(`${DAY_SHORT[day]}: close time must be after open time.`);
        return;
      }
      payload.push({
        dayOfWeek: day,
        isClosed:  false,
        openTime:  r.openTime,
        closeTime: r.closeTime,
      });
    }
    setSubmitting(true);
    try {
      await branchApi.setOperatingHours(branchCode, payload);
      toast.success(`Hours saved for ${branchCode}`);
      await onSaved?.();
    } catch (err) {
      toast.error(errorMessage(err, 'Could not save operating hours.'));
    } finally { setSubmitting(false); }
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Configure operating hours"
      description={`Set the weekly schedule for ${branchName || branchCode}. The "Open now" badge updates from this configuration.`}
      size="lg"
      footer={
        <div className="flex justify-end gap-2">
          <Button variant="ghost" onClick={onClose}>Cancel</Button>
          <Button icon={Save} loading={submitting} onClick={onSubmit}>Save hours</Button>
        </div>
      }
    >
      <div className="space-y-2.5">
        <div className="grid grid-cols-[100px_auto_1fr_1fr] gap-3 items-center text-[11px] uppercase tracking-widest text-accent-mute dark:text-ink-400 px-2 pb-2 border-b border-accent-line/70 dark:border-ink-700">
          <span>Day</span>
          <span>Closed</span>
          <span>Open</span>
          <span>Close</span>
        </div>
        {DAY_ORDER.map((day) => {
          const r = rows[day] || DAY_DEFAULTS[day];
          return (
            <div
              key={day}
              className="grid grid-cols-[100px_auto_1fr_1fr] gap-3 items-center px-2 py-2.5 rounded-lg hover:bg-accent-surface/50 dark:hover:bg-ink-750/40"
            >
              <span className="font-semibold dark:text-ink-100">{DAY_SHORT[day]}</span>
              <label className="flex items-center gap-2 cursor-pointer">
                <input
                  type="checkbox"
                  className="h-4 w-4 accent-brand-700"
                  checked={r.isClosed}
                  onChange={(e) => setDay(day, { isClosed: e.target.checked })}
                />
                <span className="text-xs text-accent-slate dark:text-ink-300">Closed</span>
              </label>
              <input
                type="time"
                value={r.openTime || ''}
                onChange={(e) => setDay(day, { openTime: e.target.value })}
                disabled={r.isClosed}
                className="h-10 px-3 rounded-lg border border-accent-line dark:border-ink-700 bg-white dark:bg-ink-800 dark:text-ink-100 text-sm focus-ring disabled:opacity-50 disabled:cursor-not-allowed"
              />
              <input
                type="time"
                value={r.closeTime || ''}
                onChange={(e) => setDay(day, { closeTime: e.target.value })}
                disabled={r.isClosed}
                className="h-10 px-3 rounded-lg border border-accent-line dark:border-ink-700 bg-white dark:bg-ink-800 dark:text-ink-100 text-sm focus-ring disabled:opacity-50 disabled:cursor-not-allowed"
              />
            </div>
          );
        })}
      </div>
    </Modal>
  );
}
