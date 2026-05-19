import { api, unwrap } from './client';

export const adminApi = {
  // ── Staff create / list / status ───────────────────────────────────────
  // Creates any staff role: CSR, BRANCH_MANAGER, LOAN_OFFICER, ADMIN.
  createStaffUser: async (payload) =>
    unwrap(await api.post('/api/v1/admin/staff', payload)),

  // Paginated staff list. Filters: role, status, branchCode, page, size.
  listStaff: async (params = {}) =>
    unwrap(await api.get('/api/v1/admin/staff', { params })),

  // Returns { byRole: {CSR:n,...}, byStatus: {ACTIVE:n,...}, totalStaff }.
  staffSummary: async () =>
    unwrap(await api.get('/api/v1/admin/staff/summary')),

  // Toggle staff status. status ∈ { ACTIVE, BLOCKED, SUSPENDED }.
  updateStaffStatus: async (id, status) =>
    unwrap(await api.patch(`/api/v1/admin/staff/${id}/status`, { status })),

  // Plain list of staff for a branch (used by branch-manager view).
  staffByBranch: async (branchCode) =>
    unwrap(await api.get(`/api/v1/admin/staff/by-branch/${branchCode}`)),
};
