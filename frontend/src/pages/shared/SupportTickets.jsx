import { useEffect, useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';
import { useNavigate, useParams } from 'react-router-dom';
import {
  TicketCheck, Plus, Search, Filter, MessageSquare, ArrowLeft, Send,
  Clock, CheckCircle2, AlertTriangle, XCircle, ChevronRight,
} from 'lucide-react';
import toast from 'react-hot-toast';
import { PageHeader } from '../../components/layout/PageHeader';
import { Card, CardHeader } from '../../components/common/Card';
import { Input, Select, Textarea } from '../../components/common/Input';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { Skeleton } from '../../components/common/Skeleton';
import { EmptyState } from '../../components/common/EmptyState';
import { Modal } from '../../components/common/Modal';
import { useAuth } from '../../context/AuthContext';
import { useAsync } from '../../hooks/useAsync';
import { supportApi } from '../../api/support';
import { ROLES } from '../../utils/roleRoutes';
import { errorMessage } from '../../api/client';
import { formatDateTime, formatRelative } from '../../utils/format';

const STATUS_TONE = {
  OPEN:        'warning',
  IN_PROGRESS: 'info',
  RESOLVED:    'success',
  CLOSED:      'neutral',
};
const PRIORITY_TONE = { HIGH: 'danger', MEDIUM: 'warning', LOW: 'neutral', NORMAL: 'neutral' };
const STATUS_ICON = { OPEN: AlertTriangle, IN_PROGRESS: Clock, RESOLVED: CheckCircle2, CLOSED: XCircle };

export function SupportTicketsList({ staff = false }) {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [status, setStatus] = useState('ALL');
  const [query, setQuery] = useState('');
  const [createOpen, setCreateOpen] = useState(false);

  const { data: tickets, loading, reload } = useAsync(
    () => supportApi.list().catch(() => []),
    []
  );

  const rows = useMemo(() => {
    const all = Array.isArray(tickets) ? tickets : [];
    return all
      .filter((t) => status === 'ALL' || t.status === status)
      .filter((t) => {
        if (!query) return true;
        const q = query.toLowerCase();
        return (
          t.subject?.toLowerCase().includes(q) ||
          t.id?.toLowerCase().includes(q) ||
          t.customerNo?.toLowerCase().includes(q) ||
          t.customerName?.toLowerCase().includes(q)
        );
      })
      .filter((t) => staff ? true : t.customerNo === `CUST-${user?.userId}` || !user?.userId);
  }, [tickets, status, query, staff, user?.userId]);

  const counts = useMemo(() => {
    const all = Array.isArray(tickets) ? tickets : [];
    const my = staff ? all : all.filter((t) => t.customerNo === `CUST-${user?.userId}` || !user?.userId);
    return {
      total:       my.length,
      open:        my.filter((t) => t.status === 'OPEN').length,
      inProgress:  my.filter((t) => t.status === 'IN_PROGRESS').length,
      resolved:    my.filter((t) => t.status === 'RESOLVED').length,
    };
  }, [tickets, staff, user?.userId]);

  return (
    <div className="space-y-8">
      <PageHeader
        breadcrumb={staff ? 'Staff · Support' : 'Account · Support'}
        title={staff ? 'Support Queue' : 'My Support Tickets'}
        subtitle={staff
          ? 'Customer-raised tickets across categories. Update status, prioritise and resolve.'
          : 'Track the status of issues you have raised. Create a new ticket and our team will respond within 24 hours.'}
        actions={
          !staff && <Button icon={Plus} onClick={() => setCreateOpen(true)}>New ticket</Button>
        }
      />

      <section className="grid sm:grid-cols-2 xl:grid-cols-4 gap-5">
        <CountTile label="Total"        value={counts.total}        icon={TicketCheck} accent="brand" />
        <CountTile label="Open"         value={counts.open}         icon={AlertTriangle} accent="gold" />
        <CountTile label="In progress"  value={counts.inProgress}   icon={Clock}      accent="info" />
        <CountTile label="Resolved"     value={counts.resolved}     icon={CheckCircle2} accent="green" />
      </section>

      <Card>
        <div className="flex flex-wrap items-end gap-3 mb-5">
          <Select label="Status" value={status} onChange={(e) => setStatus(e.target.value)} className="min-w-[180px]">
            <option value="ALL">All statuses</option>
            <option value="OPEN">Open</option>
            <option value="IN_PROGRESS">In progress</option>
            <option value="RESOLVED">Resolved</option>
            <option value="CLOSED">Closed</option>
          </Select>
          <Input
            label="Search"
            leftIcon={Search}
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Subject, ticket ID, customer..."
            className="flex-1 min-w-[200px]"
          />
        </div>

        {loading ? (
          <div className="space-y-2">
            {Array.from({ length: 5 }).map((_, i) => <Skeleton key={i} className="h-14" />)}
          </div>
        ) : rows.length === 0 ? (
          <EmptyState
            icon={MessageSquare}
            title="No tickets yet"
            message={staff ? 'No tickets match your filters.' : 'When you raise a support ticket, it will appear here.'}
            action={!staff && <Button icon={Plus} onClick={() => setCreateOpen(true)}>Create your first ticket</Button>}
          />
        ) : (
          <ul className="divide-y divide-accent-line">
            {rows.map((t) => {
              const Icon = STATUS_ICON[t.status] || TicketCheck;
              return (
                <li
                  key={t.id}
                  className="py-4 flex items-center gap-4 cursor-pointer hover:bg-accent-surface/40 -mx-6 px-6 transition"
                  onClick={() => navigate(staff ? `/staff/support/${t.id}` : `/app/support/${t.id}`)}
                >
                  <span className={`h-10 w-10 rounded-full grid place-items-center shrink-0 bg-brand-50 text-brand-700`}>
                    <Icon size={16} />
                  </span>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <p className="font-semibold text-sm truncate">{t.subject}</p>
                      <Badge tone={PRIORITY_TONE[t.priority] || 'neutral'}>{t.priority}</Badge>
                    </div>
                    <p className="text-xs text-accent-mute dark:text-ink-400 mt-0.5">
                      {t.id} · {staff ? `${t.customerName} (${t.customerNo})` : t.category} · {formatRelative(t.createdAt)}
                    </p>
                  </div>
                  <Badge tone={STATUS_TONE[t.status] || 'neutral'} dot>{t.status.replace('_', ' ')}</Badge>
                  <ChevronRight size={16} className="text-accent-mute dark:text-ink-400" />
                </li>
              );
            })}
          </ul>
        )}
      </Card>

      <CreateTicketModal
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        onCreated={() => { setCreateOpen(false); reload(); }}
      />
    </div>
  );
}

export function SupportTicketDetail({ staff = false }) {
  const { id } = useParams();
  const navigate = useNavigate();
  const [updatingStatus, setUpdatingStatus] = useState(false);

  const { data: ticket, loading, reload } = useAsync(
    () => supportApi.get(id).catch(() => null),
    [id]
  );

  const updateStatus = async (next) => {
    setUpdatingStatus(true);
    try {
      await supportApi.updateStatus(id, { status: next });
      toast.success(`Status updated to ${next.replace('_', ' ')}`);
      reload();
    } catch (err) {
      toast.error(errorMessage(err, 'Could not update status.'));
    } finally {
      setUpdatingStatus(false);
    }
  };

  if (loading) return <Skeleton className="h-96 rounded-xl2" />;
  if (!ticket) {
    return (
      <Card className="p-10">
        <EmptyState
          icon={TicketCheck}
          title="Ticket not found"
          message="The ticket you're looking for doesn't exist or you don't have access."
          action={<Button onClick={() => navigate(-1)} icon={ArrowLeft}>Go back</Button>}
        />
      </Card>
    );
  }

  return (
    <div className="space-y-8 max-w-4xl">
      <PageHeader
        breadcrumb={<button onClick={() => navigate(-1)} className="inline-flex items-center gap-1 hover:text-brand-700"><ArrowLeft size={12} /> Tickets</button>}
        title={ticket.subject}
        subtitle={`${ticket.id} · ${ticket.category} · opened ${formatDateTime(ticket.createdAt)}`}
        actions={
          <>
            <Badge tone={PRIORITY_TONE[ticket.priority] || 'neutral'}>{ticket.priority}</Badge>
            <Badge tone={STATUS_TONE[ticket.status] || 'neutral'} dot>{ticket.status.replace('_', ' ')}</Badge>
          </>
        }
      />

      <div className="grid lg:grid-cols-[1.4fr_1fr] gap-6">
        <Card>
          <CardHeader title="Description" className="mb-4" />
          <p className="text-sm text-accent-slate dark:text-ink-300 whitespace-pre-line leading-relaxed">
            {ticket.description}
          </p>

          {ticket.remarks && (
            <div className="mt-6 p-4 rounded-xl bg-accent-surface/60">
              <p className="text-xs font-semibold text-accent-mute dark:text-ink-400 uppercase tracking-widest mb-1.5">Remarks</p>
              <p className="text-sm">{ticket.remarks}</p>
            </div>
          )}

          {/* Conversation thread — placeholder until messaging backend exists */}
          <div className="mt-6 border-t border-accent-line dark:border-ink-700 pt-6">
            <h4 className="font-semibold mb-3">Conversation</h4>
            <ul className="space-y-3">
              <Message author={ticket.customerName} role="Customer" at={ticket.createdAt} body={ticket.description} />
              {ticket.status !== 'OPEN' && (
                <Message author="Support agent" role="CSR" at={ticket.updatedAt}
                         body="Thanks for reaching out — we have logged your request and are working on it. We'll update you shortly." />
              )}
            </ul>
          </div>
        </Card>

        <div className="space-y-6">
          <Card>
            <CardHeader title="Customer" className="mb-4" />
            <dl className="space-y-2 text-sm">
              <Detail label="Name" value={ticket.customerName} />
              <Detail label="Customer No." value={ticket.customerNo} mono />
              {ticket.customerEmail && <Detail label="Email" value={ticket.customerEmail} />}
            </dl>
          </Card>

          {staff && (
            <Card>
              <CardHeader title="Update status" subtitle="Move the ticket forward" className="mb-4" />
              <div className="grid grid-cols-2 gap-2">
                {['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'].map((s) => (
                  <Button
                    key={s}
                    variant={ticket.status === s ? 'primary' : 'secondary'}
                    size="sm"
                    disabled={updatingStatus || ticket.status === s}
                    onClick={() => updateStatus(s)}
                  >
                    {s.replace('_', ' ')}
                  </Button>
                ))}
              </div>
            </Card>
          )}

          <Card>
            <CardHeader title="Timeline" className="mb-4" />
            <ol className="relative pl-6 border-l-2 border-accent-line dark:border-ink-700 space-y-4 text-sm">
              <li className="relative">
                <span className="absolute -left-[29px] top-0.5 h-3 w-3 rounded-full bg-brand-700" />
                <p className="font-semibold">Ticket created</p>
                <p className="text-xs text-accent-mute dark:text-ink-400">{formatDateTime(ticket.createdAt)}</p>
              </li>
              <li className="relative">
                <span className="absolute -left-[29px] top-0.5 h-3 w-3 rounded-full bg-accent-line" />
                <p className="font-semibold">Last updated</p>
                <p className="text-xs text-accent-mute dark:text-ink-400">{formatDateTime(ticket.updatedAt)}</p>
              </li>
            </ol>
          </Card>
        </div>
      </div>
    </div>
  );
}

// ── Local components ─────────────────────────────────────────────────────────
function CountTile({ label, value, icon: Icon, accent }) {
  const tones = {
    brand: 'bg-brand-50 text-brand-700',
    gold:  'bg-amber-50 text-accent-warning',
    info:  'bg-sky-50 text-accent-info',
    green: 'bg-green-50 text-accent-success',
  };
  return (
    <div className="card p-5 flex items-center gap-4">
      <span className={`h-12 w-12 rounded-xl grid place-items-center ${tones[accent]}`}>
        <Icon size={20} />
      </span>
      <div>
        <p className="text-xs uppercase tracking-widest text-accent-mute dark:text-ink-400">{label}</p>
        <p className="font-display text-2xl font-extrabold">{value}</p>
      </div>
    </div>
  );
}

function Detail({ label, value, mono }) {
  return (
    <div className="flex items-start justify-between gap-3">
      <dt className="text-accent-mute dark:text-ink-400">{label}</dt>
      <dd className={`text-right font-medium ${mono ? 'font-mono text-xs' : ''}`}>{value}</dd>
    </div>
  );
}

function Message({ author, role, at, body }) {
  return (
    <li className="rounded-xl border border-accent-line dark:border-ink-700 p-4 bg-white">
      <div className="flex items-center justify-between mb-1.5">
        <div className="flex items-center gap-2">
          <p className="text-sm font-semibold">{author}</p>
          <Badge tone="neutral">{role}</Badge>
        </div>
        <p className="text-xs text-accent-mute dark:text-ink-400">{formatRelative(at)}</p>
      </div>
      <p className="text-sm text-accent-slate dark:text-ink-300 whitespace-pre-line">{body}</p>
    </li>
  );
}

function CreateTicketModal({ open, onClose, onCreated }) {
  const [submitting, setSubmitting] = useState(false);
  const { register, handleSubmit, reset, formState: { errors } } = useForm({
    defaultValues: { subject: '', description: '', category: 'ACCOUNT', priority: 'NORMAL' },
  });

  const onSubmit = async (values) => {
    setSubmitting(true);
    try {
      const created = await supportApi.create(values);
      toast.success(`Ticket created: ${created.id}`);
      reset();
      onCreated?.();
    } catch (err) {
      toast.error(errorMessage(err, 'Could not create ticket.'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Create a support ticket"
      description="Our team usually responds within 24 hours."
      size="lg"
      footer={
        <div className="flex justify-end gap-2">
          <Button variant="ghost" onClick={onClose}>Cancel</Button>
          <Button onClick={handleSubmit(onSubmit)} icon={Send} loading={submitting}>Submit</Button>
        </div>
      }
    >
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <Input
          label="Subject"
          placeholder="e.g. Card not received yet"
          error={errors.subject?.message}
          {...register('subject', { required: 'Subject is required', maxLength: { value: 120, message: 'Too long' } })}
        />
        <div className="grid sm:grid-cols-2 gap-4">
          <Select label="Category" {...register('category')}>
            <option value="ACCOUNT">Account</option>
            <option value="TRANSACTION">Transaction</option>
            <option value="LOAN">Loan</option>
            <option value="CARD">Card</option>
            <option value="OTHER">Other</option>
          </Select>
          <Select label="Priority" {...register('priority')}>
            <option value="LOW">Low</option>
            <option value="NORMAL">Normal</option>
            <option value="MEDIUM">Medium</option>
            <option value="HIGH">High</option>
          </Select>
        </div>
        <Textarea
          label="Describe the issue"
          placeholder="Please share as much detail as possible — when did it happen, what did you try, any reference numbers..."
          rows={6}
          error={errors.description?.message}
          {...register('description', {
            required: 'Description is required',
            minLength: { value: 10, message: 'At least 10 characters' },
          })}
        />
      </form>
    </Modal>
  );
}
