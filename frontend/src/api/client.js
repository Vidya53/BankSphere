import axios from 'axios';
import { tokenStore } from './tokenStore';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8090';

export const api = axios.create({
  baseURL: BASE_URL,
  timeout: 20000,
  headers: { 'Content-Type': 'application/json' },
});

// ── Request: attach access token + correlation ID ─────────────────────────────
api.interceptors.request.use((config) => {
  const token = tokenStore.getAccessToken();
  if (token && !config.headers.Authorization) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  if (!config.headers['X-Correlation-ID']) {
    config.headers['X-Correlation-ID'] = crypto.randomUUID();
  }
  return config;
});

// ── Response: auto-refresh on 401 TOKEN_EXPIRED ──────────────────────────────
let refreshing = null;

api.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config || {};
    const status = error.response?.status;
    const body = error.response?.data;
    const code = body?.error || body?.errorCode;

    const isAuthRoute = original.url?.includes('/auth/login') || original.url?.includes('/auth/refresh');

    if (status === 401 && code === 'TOKEN_EXPIRED' && !original._retried && !isAuthRoute) {
      original._retried = true;
      try {
        if (!refreshing) {
          refreshing = refreshAccessToken().finally(() => {
            refreshing = null;
          });
        }
        const newToken = await refreshing;
        if (newToken) {
          original.headers.Authorization = `Bearer ${newToken}`;
          return api(original);
        }
      } catch (e) {
        tokenStore.clear();
        window.location.assign('/login?reason=session_expired');
      }
    }

    return Promise.reject(error);
  }
);

async function refreshAccessToken() {
  const refreshToken = tokenStore.getRefreshToken();
  if (!refreshToken) throw new Error('No refresh token');

  const { data } = await axios.post(
    `${BASE_URL}/auth/refresh`,
    { refreshToken },
    { headers: { 'Content-Type': 'application/json' } }
  );

  const payload = data?.data ?? data;
  const accessToken  = payload?.accessToken  ?? payload?.access_token;
  const newRefresh   = payload?.refreshToken ?? payload?.refresh_token;

  if (!accessToken) throw new Error('Refresh did not return an access token');

  tokenStore.setAccessToken(accessToken);
  if (newRefresh) tokenStore.setRefreshToken(newRefresh);

  return accessToken;
}

// Unwraps the ApiResponse envelope { success, statusCode, data, message }.
// Returns the `data` payload when present, otherwise the raw body.
export const unwrap = (res) => {
  const body = res?.data;
  if (body && typeof body === 'object' && 'data' in body) return body.data;
  return body;
};

// Best-effort error message extraction for toasts.
// Prefers the most specific detail the backend returned — services use slightly
// different envelope shapes ({message, error}, {message, errors}, {message, detail}),
// so we walk them in priority order.
export const errorMessage = (err, fallback = 'Something went wrong. Please try again.') => {
  const body = err?.response?.data;
  if (!body) return err?.message || fallback;

  // Prefer the most specific field the backend included
  const detail = body.error ?? body.detail ?? body.errors;
  if (typeof detail === 'string' && detail.trim().length > 0) {
    // Common pattern: high-level message + specific detail. Combine when both exist.
    if (body.message && body.message !== detail) return `${body.message} — ${detail}`;
    return detail;
  }
  if (Array.isArray(detail) && detail.length > 0) return detail.join('; ');
  return body.message || err?.message || fallback;
};
