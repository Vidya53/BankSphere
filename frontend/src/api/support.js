import { api, unwrap } from './client';

export const supportApi = {
  list: async (params = {}) => unwrap(await api.get('/api/support/tickets', { params })),
  get: async (id) => unwrap(await api.get(`/api/support/tickets/${id}`)),
  create: async (payload) => unwrap(await api.post('/api/support/tickets', payload)),
  updateStatus: async (id, payload) => unwrap(await api.patch(`/api/support/tickets/${id}/status`, payload)),
};
