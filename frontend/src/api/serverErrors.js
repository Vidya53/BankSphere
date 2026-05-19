import { errorMessage, unwrap } from './client';

// Re-export the shared helpers so callers can import everything from one module
// when they're already pulling in applyServerErrors.
export { errorMessage, unwrap };

/**
 * Route backend field-level validation errors to react-hook-form's `setError`,
 * so each message shows up next to the offending input instead of (only) in a toast.
 *
 * The backend's `GlobalExceptionHandler` returns one of two envelope shapes,
 * depending on the service. Both are handled here:
 *
 *   1. String envelope (most services — auth, account, transfer, etc.):
 *      {
 *        "success": false,
 *        "statusCode": 400,
 *        "message": "Validation failed",
 *        "error": "fullName: Letters, spaces, hyphens and apostrophes only; email: Enter a valid email address",
 *        "timestamp": "..."
 *      }
 *      Fields are joined with "; " and each "field: message" pair is split on the
 *      first ": ".
 *
 *   2. Structured map envelope (customer-services, loan-service):
 *      {
 *        "success": false,
 *        "statusCode": 400,
 *        "message": "Validation failed",
 *        "errors": { "fullName": "...", "email": "..." },
 *        "timestamp": "..."
 *      }
 *
 * @param {unknown} err           Error caught from an axios request (must have err.response.data).
 * @param {Function} setError     `setError` from react-hook-form's `useForm()`. If absent, no-op.
 * @param {string[]|null} [knownFields=null]
 *        When provided, only field names present in this list are forwarded to setError.
 *        Useful when the form's field names diverge from the backend DTO names.
 *        Pass null to apply every field error.
 *
 * @returns {boolean} true when at least one field-level error was forwarded, false otherwise.
 */
export function applyServerErrors(err, setError, knownFields = null) {
  // Guard: setError isn't always available (e.g. plain useState forms).
  if (typeof setError !== 'function') return false;

  const data = err?.response?.data;
  if (!data || typeof data !== 'object') return false;

  const allow = Array.isArray(knownFields) && knownFields.length > 0
    ? new Set(knownFields)
    : null;

  let applied = 0;
  const push = (field, message) => {
    if (!field || !message) return;
    const f = String(field).trim();
    if (!f) return;
    if (allow && !allow.has(f)) return;
    try {
      setError(f, { type: 'server', message: String(message).trim() });
      applied += 1;
    } catch {
      // setError can throw for unknown field names in some RHF setups — ignore.
    }
  };

  // Shape 2 — structured map. Some services serialise it as { errors: {...} }
  // and some nest the body inside an envelope (handled here defensively).
  const map = data.errors;
  if (map && typeof map === 'object' && !Array.isArray(map)) {
    for (const [field, message] of Object.entries(map)) {
      // The map values are typically strings, but some backends pass arrays.
      const msg = Array.isArray(message) ? message.join('; ') : message;
      push(field, msg);
    }
  }

  // Shape 1 — string envelope. Even when an `errors` map is present, the
  // `error` string may carry additional fields — try both.
  const errorString = data.error;
  if (typeof errorString === 'string' && errorString.includes(': ')) {
    // Split on ';' first (semicolon separates fields), tolerate trailing whitespace.
    const pieces = errorString.split(/;\s*/);
    for (const piece of pieces) {
      const idx = piece.indexOf(': ');
      if (idx <= 0) continue;
      const field = piece.slice(0, idx).trim();
      const message = piece.slice(idx + 2).trim();
      push(field, message);
    }
  }

  return applied > 0;
}
