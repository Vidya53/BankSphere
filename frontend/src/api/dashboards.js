import { api, unwrap } from './client';

export const dashboardsApi = {
  csr: async (branchCode) => unwrap(await api.get('/api/csr/dashboard', { params: { branchCode } })),
  branchManager: async (branchCode) => unwrap(await api.get('/api/branch-manager/dashboard', { params: { branchCode } })),
  loanOfficer: async () => unwrap(await api.get('/api/loan-officer/dashboard')),
  csrSearchCustomers: async (q, limit = 20) =>
    unwrap(await api.get('/api/csr/customers/search', { params: { q, limit } })),
};
