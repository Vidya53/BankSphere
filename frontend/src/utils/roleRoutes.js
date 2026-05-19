export const ROLES = {
  CUSTOMER: 'CUSTOMER',
  CSR: 'CSR',
  BRANCH_MANAGER: 'BRANCH_MANAGER',
  LOAN_OFFICER: 'LOAN_OFFICER',
  ADMIN: 'ADMIN',
};

const HOME = {
  CUSTOMER:           '/app',
  CSR:                '/staff/csr',
  BRANCH_MANAGER:     '/staff/branch',
  LOAN_OFFICER:       '/staff/loans-ops',
  ADMIN:              '/staff/admin',
};

export const roleHomePath = (role) => HOME[role] || '/app';
