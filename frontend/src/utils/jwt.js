// Lightweight JWT decoder — payload only, no signature verification.
// Used for reading claims (role, branchCode, userId) injected by identity-service.
export function decodeJwt(token) {
  if (!token) return null;
  try {
    const payload = token.split('.')[1];
    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/');
    const padded = normalized + '==='.slice((normalized.length + 3) % 4);
    const json = atob(padded);
    const decoded = decodeURIComponent(
      json.split('').map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)).join('')
    );
    return JSON.parse(decoded);
  } catch {
    return null;
  }
}
