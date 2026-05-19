import { api, unwrap } from './client';

export const accountApi = {
  // ── Customer flows ────────────────────────────────────────────────────────
  applyForAccount:    async (payload)   => unwrap(await api.post('/api/v1/account-applications', payload)),
  myApplications:     async ()          => unwrap(await api.get('/api/v1/account-applications/me')),
  getApplicationById: async (id)        => unwrap(await api.get(`/api/v1/account-applications/${id}`)),

  myAccounts:  async ()          => unwrap(await api.get('/api/v1/accounts/me')),
  getAccount:  async (accountNo) => unwrap(await api.get(`/api/v1/accounts/${accountNo}`)),

  // Fires a "statement downloaded" email to the account holder.
  // Best-effort — the UI never blocks the download on this call.
  notifyStatementDownloaded: async (accountNo, { from, to } = {}) =>
    unwrap(await api.post(`/api/v1/accounts/${accountNo}/statement-notify`, null, {
      params: { from, to },
    })),

  // ── Admin branch view (ADMIN-only on the backend) ────────────────────────
  // Kept for completeness — the admin dashboard no longer surfaces these.
  staffAccountsByBranch: async (branchCode, { status } = {}) =>
    unwrap(await api.get(`/api/v1/staff/accounts/by-branch/${branchCode}`, {
      params: status ? { status } : {},
    })),
  staffBranchSummary: async (branchCode) =>
    unwrap(await api.get(`/api/v1/staff/accounts/by-branch/${branchCode}/summary`)),
  staffBranchBreakdown: async (branchCode) =>
    unwrap(await api.get(`/api/v1/staff/accounts/by-branch/${branchCode}/breakdown`)),

  // ── Branch Manager / CSR — accounts in caller's own branch ───────────────
  // Backend scopes via the X-Branch-Code header; no path param needed.
  staffMyBranchAccounts: async ({ status } = {}) =>
    unwrap(await api.get('/api/v1/staff/accounts/my-branch', {
      params: status ? { status } : {},
    })),
  staffMyBranchSummary: async () =>
    unwrap(await api.get('/api/v1/staff/accounts/my-branch/summary')),
  staffMyBranchBreakdown: async () =>
    unwrap(await api.get('/api/v1/staff/accounts/my-branch/breakdown')),

  // ── Branch Manager mutations (BRANCH_MANAGER role on the backend) ────────
  // These hit the account-no-scoped endpoints; the backend confirms the
  // account belongs to the BM's branch before mutating.
  staffFreezeAccount: async (accountNo, { reason }) =>
    unwrap(await api.patch(`/api/v1/staff/accounts/${accountNo}/freeze`, { reason })),
  staffUnfreezeAccount: async (accountNo) =>
    unwrap(await api.patch(`/api/v1/staff/accounts/${accountNo}/unfreeze`)),
  staffCloseAccount: async (accountNo, { reason }) =>
    unwrap(await api.patch(`/api/v1/staff/accounts/${accountNo}/close`, { reason })),

  // ── Staff flows (CSR / Branch Manager / Admin) ────────────────────────────
  // Backend automatically scopes to the staff member's own branch via X-Branch-Code.
  pendingApplications: async () =>
    unwrap(await api.get('/api/v1/staff/account-applications/pending')),

  allApplications: async () =>
    unwrap(await api.get('/api/v1/staff/account-applications/all')),

  approveApplication: async (id) =>
    unwrap(await api.post(`/api/v1/staff/account-applications/${id}/approve`)),

  rejectApplication: async (id, { reason, remarks }) =>
    unwrap(await api.post(`/api/v1/staff/account-applications/${id}/reject`, { reason, remarks })),

  // ── Counter cash operations (CSR / BM / Admin) ────────────────────────────
  cashDeposit: async (payload) =>
    unwrap(await api.post('/api/v1/staff/cash/deposit', payload)),

  cashWithdrawal: async (payload) =>
    unwrap(await api.post('/api/v1/staff/cash/withdrawal', payload)),
};
