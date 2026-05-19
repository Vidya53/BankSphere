const TONES = {
  neutral: 'bg-accent-line/60 text-accent-slate dark:bg-ink-700 dark:text-ink-300',
  brand:   'bg-brand-50 text-brand-700 dark:bg-brand-900/40 dark:text-brand-200',
  success: 'bg-green-50 text-accent-success dark:bg-green-900/30 dark:text-green-300',
  warning: 'bg-amber-50 text-accent-warning dark:bg-amber-900/30 dark:text-amber-300',
  danger:  'bg-red-50 text-accent-danger dark:bg-red-900/30 dark:text-red-300',
  info:    'bg-sky-50 text-accent-info dark:bg-sky-900/30 dark:text-sky-300',
};

export function Badge({ tone = 'neutral', children, className = '', dot = false }) {
  return (
    <span className={`chip ${TONES[tone]} ${className}`}>
      {dot && <span className={`h-1.5 w-1.5 rounded-full bg-current`} />}
      {children}
    </span>
  );
}

// Maps backend lifecycle statuses to badge tones.
export const STATUS_TONE = {
  // KYC + customer
  PENDING:   'warning',
  SUBMITTED: 'info',
  UNDER_REVIEW: 'info',
  APPROVED:  'success',
  REJECTED:  'danger',
  REGISTERED: 'info',
  ACTIVE:    'success',
  INACTIVE:  'neutral',
  BLOCKED:   'danger',
  CLOSED:    'neutral',
  FROZEN:    'warning',
  // Transactions
  SUCCESS:   'success',
  FAILED:    'danger',
  PROCESSING: 'info',
  CANCELLED: 'neutral',
  REVERSED:  'warning',
  TIMED_OUT: 'danger',
};

export function StatusBadge({ status, ...rest }) {
  const tone = STATUS_TONE[status] || 'neutral';
  return <Badge tone={tone} dot {...rest}>{status?.replace(/_/g, ' ')}</Badge>;
}
