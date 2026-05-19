import { api, unwrap } from './client';

export const pinApi = {
  status: async (accountNo) =>
    unwrap(await api.get(`/api/v1/accounts/${accountNo}/pin/status`)),

  set: async (accountNo, pin) =>
    unwrap(await api.post(`/api/v1/accounts/${accountNo}/pin/set`, { pin })),

  change: async (accountNo, currentPin, newPin) =>
    unwrap(await api.post(`/api/v1/accounts/${accountNo}/pin/change`, { currentPin, newPin })),
};

export const transferApi = {
  // Customer-initiated transfer. Backend gates >₹1L for CSR approval.
  initiate: async (payload) => unwrap(await api.post('/api/v1/transfers', payload)),

  // Customer's view of their own pending high-value transfers.
  myPending: async () => unwrap(await api.get('/api/v1/transfers/pending/me')),
};

export const pendingTransferStaffApi = {
  // CSR/BM/Admin: list pending high-value transfers for their branch.
  list:    async () => unwrap(await api.get('/api/v1/staff/pending-transfers')),
  approve: async (reference) => unwrap(await api.post(`/api/v1/staff/pending-transfers/${reference}/approve`)),
  reject:  async (reference, reason) =>
    unwrap(await api.post(`/api/v1/staff/pending-transfers/${reference}/reject`, { reason })),
};
