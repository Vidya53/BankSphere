import { api, unwrap } from './client';

export const authApi = {
  login: async ({ email, password }) => {
    const res = await api.post('/auth/login', { email, password });
    return unwrap(res);
  },

  signup: async (payload) => {
    const res = await api.post('/auth/signup', payload);
    return unwrap(res);
  },

  refresh: async (refreshToken) => {
    const res = await api.post('/auth/refresh', { refreshToken });
    return unwrap(res);
  },

  logout: async (refreshToken) => {
    try {
      await api.post('/auth/logout', { refreshToken });
    } catch {
      // logout is best-effort; client clears state regardless
    }
  },

  // ── Password reset (email OTP) ──────────────────────────────────────────
  forgotPassword: async (email) =>
    unwrap(await api.post('/auth/forgot-password', { email })),

  verifyOtp: async ({ email, otp }) =>
    unwrap(await api.post('/auth/verify-otp', { email, otp })),

  resetPassword: async ({ email, resetToken, newPassword }) =>
    unwrap(await api.post('/auth/reset-password', { email, resetToken, newPassword })),
};
