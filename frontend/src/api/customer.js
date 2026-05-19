import { api, unwrap } from './client';

export const customerApi = {
  register:        async (payload)      => unwrap(await api.post('/customers', payload)),
  getMyProfile:    async ()             => unwrap(await api.get('/customers/me')),
  updateMyProfile: async (payload)      => unwrap(await api.put('/customers/me', payload)),
  getByCustomerNo: async (customerNo)   => unwrap(await api.get(`/customers/${customerNo}`)),

  // Status transitions (CSR / Branch Manager).
  activate:        async (customerNo)   => unwrap(await api.put(`/customers/${customerNo}/activate`)),
  deactivate:      async (customerNo)   => unwrap(await api.put(`/customers/${customerNo}/deactivate`)),
  block:           async (customerNo)   => unwrap(await api.put(`/customers/${customerNo}/block`)),

  // Soft delete (Admin / Branch Manager). Marks isDeleted=true, flips status to
  // CLOSED, and cascade-closes every account the customer holds via account-service.
  softDelete:       async (customerNo)  => unwrap(await api.delete(`/customers/${customerNo}`)),
  softDeleteByUser: async (userId)      => unwrap(await api.delete(`/customers/by-user/${userId}`)),
};

export const kycApi = {
  // Submit a new KYC against a specific customer profile.
  submit: async (customerNo, payload) =>
    unwrap(await api.post(`/customers/${customerNo}/kyc`, payload)),

  // Returns the customer's KYC record, or throws 404 if none exists yet.
  getByCustomerNo: async (customerNo) =>
    unwrap(await api.get(`/customers/${customerNo}/kyc`)),

  approve: async (customerNo) =>
    unwrap(await api.put(`/customers/${customerNo}/kyc/approve`)),

  // Backend takes the reason as a query parameter, not a body.
  reject: async (customerNo, reason) =>
    unwrap(await api.put(`/customers/${customerNo}/kyc/reject`, null, { params: { reason } })),

  // Optionally filtered to a specific branch (CSR scope).
  listPending: async (branchCode) =>
    unwrap(await api.get('/kyc/pending', { params: branchCode ? { branchCode } : {} })),
};
