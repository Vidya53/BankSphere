import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Mail, Phone, MessageCircle, ChevronDown, LifeBuoy } from 'lucide-react';
import toast from 'react-hot-toast';
import { PageHeader } from '../../components/layout/PageHeader';
import { Card, CardHeader } from '../../components/common/Card';
import { Input, Select, Textarea } from '../../components/common/Input';
import { Button } from '../../components/common/Button';

const FAQS = [
  { q: 'How long does KYC verification take?',          a: 'KYC submissions are typically reviewed within 24 hours on business days.' },
  { q: 'Are my transactions encrypted?',                a: 'Yes. Every transaction is sent over a secure, encrypted connection, and your sensitive details — including balances and account numbers — are encrypted while stored on our servers. You’re also signed in only on devices you’ve approved.' },
  { q: 'How do I close an account?',                    a: 'You can request closure under Accounts → Account Detail → Close. The account must have a zero balance and no pending transactions.' },
  { q: 'What channels are supported for transfers?',    a: 'UPI, NEFT, IMPS, RTGS and INTERNAL (between BankSphere accounts).' },
  { q: 'How is loan eligibility determined?',           a: 'A combination of your KYC status, account standing, monthly income, EMI-to-income ratio, and current risk category.' },
];

export default function Help() {
  return (
    <div className="space-y-8 max-w-5xl">
      <PageHeader
        breadcrumb="Account"
        title="Help & support"
        subtitle="Browse FAQs, raise a ticket, or reach out to our customer service team."
      />

      <section className="grid sm:grid-cols-3 gap-5">
        <ContactCard icon={Phone}         title="Call us"   line="1800-209-5577"  sub="Mon–Sat, 9am to 9pm" />
        <ContactCard icon={Mail}          title="Email us"  line="help@banksphere.in" sub="Reply within 24 hours" />
        <ContactCard icon={MessageCircle} title="Live chat" line="Available 24×7"  sub="In-app chat with an agent" />
      </section>

      <section className="grid lg:grid-cols-[1.4fr_1fr] gap-6">
        <Card>
          <CardHeader title="Frequently asked questions" className="mb-4" />
          <ul className="divide-y divide-accent-line">
            {FAQS.map((f, i) => <FaqItem key={i} {...f} />)}
          </ul>
        </Card>
        <TicketCard />
      </section>
    </div>
  );
}

function ContactCard({ icon: Icon, title, line, sub }) {
  return (
    <Card className="text-center">
      <span className="mx-auto h-12 w-12 rounded-full bg-brand-50 text-brand-700 grid place-items-center">
        <Icon size={20} />
      </span>
      <h3 className="mt-4 font-semibold">{title}</h3>
      <p className="mt-1 text-sm font-medium text-brand-700">{line}</p>
      <p className="mt-1 text-xs text-accent-mute dark:text-ink-400">{sub}</p>
    </Card>
  );
}

function FaqItem({ q, a }) {
  const [open, setOpen] = useState(false);
  return (
    <li>
      <button
        onClick={() => setOpen((v) => !v)}
        className="w-full flex items-center justify-between gap-3 py-4 text-left"
      >
        <span className="text-sm font-semibold text-accent-ink dark:text-ink-100">{q}</span>
        <ChevronDown size={16} className={`text-accent-mute dark:text-ink-400 transition ${open ? 'rotate-180' : ''}`} />
      </button>
      {open && <p className="pb-4 text-sm text-accent-slate dark:text-ink-300">{a}</p>}
    </li>
  );
}

function TicketCard() {
  const [submitting, setSubmitting] = useState(false);
  const { register, handleSubmit, reset, formState: { errors } } = useForm({
    defaultValues: { subject: '', category: 'ACCOUNT', priority: 'NORMAL', message: '' },
  });

  const onSubmit = () => {
    setSubmitting(true);
    setTimeout(() => {
      setSubmitting(false);
      reset();
      toast.success('Support ticket created. Our team will get back to you within 24 hours.');
    }, 600);
  };

  return (
    <Card>
      <CardHeader title="Raise a ticket" subtitle="We typically respond within 24 hours" className="mb-5" />
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <Input
          label="Subject"
          placeholder="e.g. Unable to transfer to external account"
          error={errors.subject?.message}
          {...register('subject', { required: 'Required' })}
        />
        <div className="grid sm:grid-cols-2 gap-4">
          <Select label="Category" {...register('category')}>
            <option value="ACCOUNT">Account</option>
            <option value="TRANSACTION">Transaction</option>
            <option value="LOAN">Loan</option>
            <option value="OTHER">Other</option>
          </Select>
          <Select label="Priority" {...register('priority')}>
            <option value="LOW">Low</option>
            <option value="NORMAL">Normal</option>
            <option value="HIGH">High</option>
          </Select>
        </div>
        <Textarea
          label="Describe the issue"
          placeholder="Please share as much detail as possible"
          error={errors.message?.message}
          {...register('message', { required: 'Required', minLength: { value: 10, message: 'At least 10 characters' } })}
        />
        <div className="flex justify-end pt-2 border-t border-accent-line dark:border-ink-700">
          <Button type="submit" loading={submitting} icon={LifeBuoy}>Submit ticket</Button>
        </div>
      </form>
    </Card>
  );
}
