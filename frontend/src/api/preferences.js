import { api, unwrap } from './client';

export const preferencesApi = {
  get: async () => unwrap(await api.get('/api/notifications/preferences')),
  update: async (prefs) => unwrap(await api.put('/api/notifications/preferences', prefs)),
};

export const passwordApi = {
  change: async ({ currentPassword, newPassword }) =>
    unwrap(await api.post('/api/auth/change-password', { currentPassword, newPassword })),
};
