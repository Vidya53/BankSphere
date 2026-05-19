// ── Currency ──────────────────────────────────────────────────────────────────
const INR = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  maximumFractionDigits: 2,
});

export const formatINR = (value) => {
  const n = Number(value ?? 0);
  if (Number.isNaN(n)) return '₹0.00';
  return INR.format(n);
};

export const formatCompactINR = (value) => {
  const n = Number(value ?? 0);
  if (Number.isNaN(n)) return '₹0';
  if (Math.abs(n) >= 1e7) return `₹${(n / 1e7).toFixed(2)} Cr`;
  if (Math.abs(n) >= 1e5) return `₹${(n / 1e5).toFixed(2)} L`;
  if (Math.abs(n) >= 1e3) return `₹${(n / 1e3).toFixed(1)} K`;
  return `₹${n.toFixed(0)}`;
};

// ── Dates ────────────────────────────────────────────────────────────────────
export const formatDate = (iso, opts = { dateStyle: 'medium' }) => {
  if (!iso) return '—';
  try {
    return new Intl.DateTimeFormat('en-IN', opts).format(new Date(iso));
  } catch {
    return iso;
  }
};

export const formatDateTime = (iso) =>
  formatDate(iso, { dateStyle: 'medium', timeStyle: 'short' });

export const formatRelative = (iso) => {
  if (!iso) return '—';
  const diffMs = new Date(iso).getTime() - Date.now();
  const sec = Math.round(diffMs / 1000);
  const abs = Math.abs(sec);
  const rtf = new Intl.RelativeTimeFormat('en-IN', { numeric: 'auto' });
  if (abs < 60) return rtf.format(sec, 'second');
  if (abs < 3600) return rtf.format(Math.round(sec / 60), 'minute');
  if (abs < 86400) return rtf.format(Math.round(sec / 3600), 'hour');
  if (abs < 2592000) return rtf.format(Math.round(sec / 86400), 'day');
  return formatDate(iso);
};

// ── Identifiers ──────────────────────────────────────────────────────────────
export const maskAccountNo = (accountNo) => {
  if (!accountNo) return '—';
  if (accountNo.length <= 4) return accountNo;
  return `••• ${accountNo.slice(-4)}`;
};

export const initials = (name) => {
  if (!name) return 'U';
  return name
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0]?.toUpperCase())
    .join('');
};
