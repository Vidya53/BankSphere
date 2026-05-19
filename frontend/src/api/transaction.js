import { api, unwrap } from './client';

export const transactionApi = {
  initiate:       async (payload)      => unwrap(await api.post('/api/v1/transactions', payload)),
  getById:        async (id)           => unwrap(await api.get(`/api/v1/transactions/${id}`)),
  getByReference: async (ref)          => unwrap(await api.get(`/api/v1/transactions/reference/${ref}`)),
  getByAccount: async (accountId, { page = 0, size = 20, sortBy = 'createdAt', sortDir = 'desc' } = {}) =>
    unwrap(await api.get(`/api/v1/transactions/account/${accountId}`, {
      params: { page, size, sortBy, sortDir },
    })),
  cancel:  async (id, remarks) => unwrap(await api.patch(`/api/v1/transactions/${id}/cancel`,  { remarks })),
  reverse: async (id, remarks) => unwrap(await api.patch(`/api/v1/transactions/${id}/reverse`, { remarks })),
};
