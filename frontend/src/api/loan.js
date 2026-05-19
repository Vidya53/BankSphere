import { api, unwrap } from './client';

// All calls route through the gateway so they pick up the standard auth headers.
export const loanApi = {
  // Customer-facing
  apply:          async (payload)      => unwrap(await api.post('/loans', payload)),
  getById:        async (id)           => unwrap(await api.get(`/loans/${id}`)),
  emiSchedule:    async (id)           => unwrap(await api.get(`/loans/${id}/schedule`)),
  payEmi:         async (id, payload)  => unwrap(await api.post(`/loans/${id}/pay`, payload)),
  prepay:         async (id, payload)  => unwrap(await api.post(`/loans/${id}/prepay`, payload)),
  paymentHistory: async (id)           => unwrap(await api.get(`/loans/${id}/payments`)),
  byCustomer:     async (customerId)   => unwrap(await api.get(`/loans/customer/${customerId}`)),
  summary:        async (customerId)   => unwrap(await api.get(`/loans/summary/${customerId}`)),

  // Richer profile-aware eligibility check — KYC, age, outstanding loans.
  evaluate:       async (payload)      => unwrap(await api.post('/customers/evaluate', payload)),

  // Loan-officer / admin actions
  decide:         async (id, payload)  => unwrap(await api.post(`/loans/${id}/decision`, payload)),
  disburse:       async (id)           => unwrap(await api.post(`/loans/${id}/disburse`)),
};
