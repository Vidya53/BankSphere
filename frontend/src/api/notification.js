import { api, unwrap } from './client';

export const notificationApi = {
  myNotifications: async () => unwrap(await api.get('/api/v1/notifications/me')),
  markRead: async (id) => unwrap(await api.patch(`/api/v1/notifications/${id}/read`)),
  markAllRead: async () => unwrap(await api.patch('/api/v1/notifications/read-all')),
};
