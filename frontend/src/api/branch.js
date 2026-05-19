import { api, unwrap } from './client';

export const branchApi = {
  // Paginated list, optional filters. Backend returns a Spring Data Page object.
  list: async (params = {}) => unwrap(await api.get('/api/v1/branches', { params })),

  get: async (branchCode) => unwrap(await api.get(`/api/v1/branches/${branchCode}`)),

  // Helper: list only ACTIVE branches, return a plain array (flattens the Page).
  active: async () => {
    const page = await branchApi.list({ status: 'ACTIVE', size: 200 });
    return Array.isArray(page) ? page : (page?.content || []);
  },

  // Helper: returns ALL branches as a plain array (drops the Page envelope).
  listAll: async () => {
    const page = await branchApi.list({ size: 200 });
    return Array.isArray(page) ? page : (page?.content || []);
  },

  create: async (payload) => unwrap(await api.post('/api/v1/branches', payload)),

  update: async (branchCode, payload) =>
    unwrap(await api.put(`/api/v1/branches/${branchCode}`, payload)),

  updateStatus: async (branchCode, status) =>
    unwrap(await api.patch(`/api/v1/branches/${branchCode}/status`, { status })),

  // Operating hours for a branch — Sun..Sat with optional open/close times.
  operatingHours: async (branchCode) =>
    unwrap(await api.get(`/api/v1/branches/${branchCode}/operating-hours`)),

  // Replace the full week of operating hours for a branch. Payload is a list
  // of { dayOfWeek, openTime: "HH:mm", closeTime: "HH:mm", isClosed }.
  setOperatingHours: async (branchCode, payload) =>
    unwrap(await api.put(`/api/v1/branches/${branchCode}/operating-hours`, payload)),

  // Full branch summary (manager, staff count, today's hours, "is open now").
  summary: async (branchCode) =>
    unwrap(await api.get(`/api/v1/branches/${branchCode}/summary`)),

  isOpenNow: async (branchCode) =>
    unwrap(await api.get(`/api/v1/branches/${branchCode}/open`)),
};
