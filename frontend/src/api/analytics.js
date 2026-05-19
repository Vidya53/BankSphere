import { api, unwrap } from './client';

export const analyticsApi = {
  spendAnalysis: async ({ customerId, days = 30 } = {}) =>
    unwrap(await api.get('/api/analytics/spend-analysis', { params: { customerId, days } })),

  revenueTrends: async ({ months = 12 } = {}) =>
    unwrap(await api.get('/api/analytics/revenue-trends', { params: { months } })),

  loanPortfolio: async () =>
    unwrap(await api.get('/api/analytics/loan-portfolio')),

  complianceMetrics: async () =>
    unwrap(await api.get('/api/analytics/compliance-metrics')),

  customerInsights: async () =>
    unwrap(await api.get('/api/analytics/customer-insights')),
};
