import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { Bell, Mail, MessageSquare, Smartphone, MonitorSmartphone, Save, ShieldCheck, Megaphone, Receipt, Clock, Moon } from 'lucide-react';
import toast from 'react-hot-toast';
import { PageHeader } from '../../components/layout/PageHeader';
import { Card, CardHeader } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { Select } from '../../components/common/Input';
import { Skeleton } from '../../components/common/Skeleton';
import { preferencesApi } from '../../api/preferences';
import { errorMessage } from '../../api/client';

const CHANNELS = [
  { key: 'email', label: 'Email',      icon: Mail,            desc: 'Statements, receipts and important updates' },
  { key: 'sms',   label: 'SMS',        icon: MessageSquare,   desc: 'OTPs, transaction alerts and security' },
  { key: 'push',  label: 'Push',       icon: Smartphone,      desc: 'Push notifications on your phone' },
  { key: 'inApp', label: 'In-app',     icon: MonitorSmartphone, desc: 'Notification bell inside the app' },
];

const CATEGORIES = [
  { key: 'transactionAlerts', label: 'Transaction alerts',  icon: Receipt,    desc: 'Every deposit, withdrawal and transfer' },
  { key: 'securityAlerts',    label: 'Security alerts',     icon: ShieldCheck, desc: 'Sign-ins, password changes, suspicious activity' },
  { key: 'emiReminders',      label: 'EMI reminders',       icon: Clock,      desc: '3 days before each loan EMI is due' },
  { key: 'accountStatements', label: 'Account statements',  icon: Receipt,    desc: 'Monthly account statement delivery' },
  { key: 'marketingOffers',   label: 'Offers & promotions', icon: Megaphone,   desc: 'Personalised offers and product news' },
  { key: 'productUpdates',    label: 'Product updates',     icon: Bell,       desc: 'New features and platform announcements' },
];

const FREQUENCIES = [
  { value: 'REALTIME',       label: 'Real-time — as soon as events occur' },
  { value: 'BATCHED_HOURLY', label: 'Batched — every hour' },
  { value: 'BATCHED_DAILY',  label: 'Batched — once a day' },
];

export default function NotificationPreferences() {
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  const { register, control, handleSubmit, reset, watch, formState: { errors } } = useForm({
    defaultValues: {
      channels: { email: true, sms: true, push: true, inApp: true },
      categories: {
        transactionAlerts: true, securityAlerts: true, emiReminders: true,
        accountStatements: true, marketingOffers: false, productUpdates: false,
      },
      dnd: { enabled: false, startTime: '22:00', endTime: '07:00' },
      frequency: 'REALTIME',
    },
  });

  useEffect(() => {
    preferencesApi.get()
      .then((p) => reset(p))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [reset]);

  const onSubmit = async (values) => {
    setSubmitting(true);
    try {
      await preferencesApi.update(values);
      toast.success('Preferences saved');
    } catch (err) {
      toast.error(errorMessage(err, 'Could not save preferences.'));
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-10 w-72" />
        <Skeleton className="h-64 rounded-xl2" />
        <Skeleton className="h-64 rounded-xl2" />
      </div>
    );
  }

  const dndEnabled = watch('dnd.enabled');

  return (
    <div className="space-y-8 max-w-4xl">
      <PageHeader
        breadcrumb="Account · Settings"
        title="Notification preferences"
        subtitle="Choose how, what and when we reach out to you. Changes apply immediately."
      />

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        <Card>
          <CardHeader title="Channels" subtitle="Where would you like to receive notifications?" className="mb-5" />
          <div className="grid sm:grid-cols-2 gap-3">
            {CHANNELS.map(({ key, label, icon: Icon, desc }) => (
              <ToggleRow
                key={key}
                icon={Icon}
                label={label}
                desc={desc}
                checked={watch(`channels.${key}`)}
                {...register(`channels.${key}`)}
              />
            ))}
          </div>
        </Card>

        <Card>
          <CardHeader title="Categories" subtitle="Choose which kinds of notifications you want to receive" className="mb-5" />
          <div className="grid sm:grid-cols-2 gap-3">
            {CATEGORIES.map(({ key, label, icon: Icon, desc }) => (
              <ToggleRow
                key={key}
                icon={Icon}
                label={label}
                desc={desc}
                checked={watch(`categories.${key}`)}
                {...register(`categories.${key}`)}
              />
            ))}
          </div>
        </Card>

        <Card>
          <CardHeader title="Frequency & quiet hours" subtitle="Control timing and batching" className="mb-5" />
          <div className="grid sm:grid-cols-2 gap-5">
            <Select label="Delivery frequency" {...register('frequency')}>
              {FREQUENCIES.map((f) => <option key={f.value} value={f.value}>{f.label}</option>)}
            </Select>

            <div>
              <label className="label">Do Not Disturb</label>
              <ToggleRow
                icon={Moon}
                label={dndEnabled ? 'DND is on' : 'DND is off'}
                desc="During quiet hours, only security alerts will get through"
                checked={watch('dnd.enabled')}
                compact
                {...register('dnd.enabled')}
              />
            </div>
          </div>

          {dndEnabled && (
            <div className="mt-5 grid sm:grid-cols-2 gap-4 p-4 rounded-xl bg-accent-surface/60">
              <div>
                <label className="label">Start time</label>
                <input type="time" {...register('dnd.startTime')} className="input" />
              </div>
              <div>
                <label className="label">End time</label>
                <input type="time" {...register('dnd.endTime')} className="input" />
              </div>
            </div>
          )}
        </Card>

        <div className="flex items-center justify-end gap-2">
          <Button type="submit" icon={Save} loading={submitting}>Save preferences</Button>
        </div>
      </form>
    </div>
  );
}

const ToggleRow = ({ icon: Icon, label, desc, checked, compact = false, ...register }) => (
  <label className={`flex items-start gap-3 p-4 rounded-xl border cursor-pointer transition ${
    checked ? 'border-brand-200 bg-brand-50/40' : 'border-accent-line dark:border-ink-700 hover:bg-accent-surface/40'
  } ${compact ? 'items-center' : ''}`}>
    <span className={`h-10 w-10 rounded-xl grid place-items-center shrink-0 ${checked ? 'bg-brand-700 text-white' : 'bg-accent-line/60 text-accent-slate dark:text-ink-300'}`}>
      <Icon size={16} />
    </span>
    <div className="flex-1 min-w-0">
      <p className="text-sm font-semibold text-accent-ink dark:text-ink-100">{label}</p>
      {!compact && <p className="text-xs text-accent-mute dark:text-ink-400 mt-0.5">{desc}</p>}
      {compact && <p className="text-xs text-accent-mute dark:text-ink-400 mt-0.5">{desc}</p>}
    </div>
    <input type="checkbox" className="h-5 w-5 accent-brand-700 mt-1" {...register} />
  </label>
);
