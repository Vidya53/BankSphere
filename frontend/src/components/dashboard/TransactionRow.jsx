import { ArrowDownLeft, ArrowUpRight, RefreshCw, RotateCcw } from 'lucide-react';
import { formatINR, formatDateTime, maskAccountNo } from '../../utils/format';
import { Badge, StatusBadge } from '../common/Badge';

const TYPE_ICONS = {
  DEPOSIT:     ArrowDownLeft,
  INTEREST:    ArrowDownLeft,
  WITHDRAWAL:  ArrowUpRight,
  PAYMENT:     ArrowUpRight,
  TRANSFER:    RefreshCw,
  REVERSAL:    RotateCcw,
  REFUND:      RotateCcw,
};

const TYPE_LABEL = {
  DEPOSIT:     'Deposit',
  WITHDRAWAL:  'Withdrawal',
  TRANSFER:    'Money transfer',
  PAYMENT:     'Payment',
  REVERSAL:    'Reversal',
  REFUND:      'Refund',
  INTEREST:    'Interest credit',
  FEE:         'Service fee',
};

// Case-insensitive trim — fallback path only. The server now stamps the
// authoritative direction onto every viewer-scoped response (tx.direction),
// so we rarely fall through to this client-side heuristic.
const norm = (s) => (s || '').toString().trim().toUpperCase();

function isCreditFallback(tx, accountIds) {
  const type = tx?.transactionType;
  if (type === 'WITHDRAWAL' || type === 'PAYMENT' || type === 'FEE') return false;
  if (type === 'DEPOSIT'    || type === 'INTEREST') return true;

  const set = new Set((accountIds || []).map(norm).filter(Boolean));
  if (set.size === 0) return false;
  return set.has(norm(tx?.receiverAccountId));
}

export function TransactionRow({ tx, accountId, myAccountNos, compact = false }) {
  // Normalise the input — older callers pass a single `accountId`, newer ones
  // pass an array. Either form is accepted.
  const accountIds = Array.isArray(myAccountNos) && myAccountNos.length > 0
    ? myAccountNos
    : (accountId ? [accountId] : []);

  const Icon   = TYPE_ICONS[tx.transactionType] || RefreshCw;
  const label  = TYPE_LABEL[tx.transactionType] || tx.transactionType;

  // Prefer the server-computed direction — it's based on the raw, unmasked
  // account ids and the viewer scope of the request, so it's always right.
  // Fall back to the client-side heuristic only when the server didn't send
  // one (e.g. staff-side lookups by transactionId / referenceNumber).
  const serverDirection = (tx.direction || '').toString().toUpperCase();
  const accountSet = new Set(accountIds.map(norm).filter(Boolean));
  const clientSelfTransfer =
    (tx.transactionType === 'TRANSFER' || tx.transactionType === 'REVERSAL' || tx.transactionType === 'REFUND')
    && accountSet.has(norm(tx.senderAccountId))
    && accountSet.has(norm(tx.receiverAccountId));

  const isSelfTransfer = serverDirection
    ? serverDirection === 'SELF'
    : clientSelfTransfer;

  const credit = !isSelfTransfer && (
    serverDirection
      ? serverDirection === 'CREDIT'
      : isCreditFallback(tx, accountIds)
  );

  // The other party — for transfers we show "from XXXX" on credits and
  // "to XXXX" on debits, so the customer always sees who's at the other end.
  const counterParty = credit ? tx.senderAccountId : tx.receiverAccountId;
  const showCounterparty = !isSelfTransfer && counterParty && (
    tx.transactionType === 'TRANSFER'
    || tx.transactionType === 'REVERSAL'
    || tx.transactionType === 'REFUND'
  );

  return (
    <div className="flex items-center gap-4 py-3 px-2 hover:bg-accent-surface/60 dark:hover:bg-ink-750/60 rounded-lg transition">
      <span className={`h-10 w-10 rounded-full grid place-items-center shrink-0 ${
        isSelfTransfer
          ? 'bg-sky-50 text-accent-info dark:bg-sky-900/30 dark:text-sky-300'
          : credit
            ? 'bg-green-50 text-accent-success dark:bg-green-900/30 dark:text-green-300'
            : 'bg-red-50 text-accent-danger dark:bg-red-900/30 dark:text-red-300'
      }`}>
        <Icon size={16} />
      </span>

      <div className="min-w-0 flex-1">
        <p className="text-sm font-semibold text-accent-ink truncate dark:text-ink-100">
          {tx.description || label}
          {isSelfTransfer && (
            <span className="ml-2 text-xs font-normal text-accent-mute dark:text-ink-400">
              · {maskAccountNo(tx.senderAccountId)} → {maskAccountNo(tx.receiverAccountId)}
            </span>
          )}
          {showCounterparty && (
            <span className="ml-2 text-xs font-normal text-accent-mute dark:text-ink-400">
              · {credit ? 'from' : 'to'} {maskAccountNo(counterParty)}
            </span>
          )}
        </p>
        <p className="text-xs text-accent-mute mt-0.5 dark:text-ink-400">
          {tx.referenceNumber} · {formatDateTime(tx.createdAt)}
        </p>
      </div>

      <Badge tone={isSelfTransfer ? 'info' : credit ? 'success' : 'danger'}>
        {isSelfTransfer ? 'Self transfer' : credit ? 'Credit' : 'Debit'}
      </Badge>

      {!compact && <StatusBadge status={tx.status} />}

      <div className="text-right shrink-0">
        <p className={`text-sm font-semibold ${
          isSelfTransfer
            ? 'text-accent-info dark:text-sky-300'
            : credit
              ? 'text-accent-success dark:text-green-300'
              : 'text-accent-danger dark:text-red-300'
        }`}>
          {isSelfTransfer ? '↔' : credit ? '+' : '−'} {formatINR(tx.amount)}
        </p>
        <p className="text-[11px] text-accent-mute uppercase tracking-wider dark:text-ink-400">
          {isSelfTransfer ? 'Between your accounts' : credit ? 'Credited' : 'Debited'}
        </p>
      </div>
    </div>
  );
}
