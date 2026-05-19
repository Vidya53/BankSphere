import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
import { ProtectedRoute } from './components/common/ProtectedRoute';
import { ROLES, roleHomePath } from './utils/roleRoutes';

import Landing from './pages/public/Landing';
import Unauthorized from './pages/public/Unauthorized';
import InfoPage from './pages/public/InfoPage';
import About from './pages/public/About';
import Branches from './pages/public/Branches';
import Login from './pages/auth/Login';
import Signup from './pages/auth/Signup';
import ForgotPassword from './pages/auth/ForgotPassword';

import AppShell from './components/layout/AppShell';

// Customer pages
import Dashboard from './pages/customer/Dashboard';
import Accounts from './pages/customer/Accounts';
import AccountDetail from './pages/customer/AccountDetail';
import AccountApply from './pages/customer/AccountApply';
import Transactions from './pages/customer/Transactions';
import Transfer from './pages/customer/Transfer';
import Loans from './pages/customer/Loans';
import LoanDetail from './pages/customer/LoanDetail';
import Kyc from './pages/customer/Kyc';
import Profile from './pages/customer/Profile';
import Notifications from './pages/customer/Notifications';
import Settings from './pages/customer/Settings';
import Help from './pages/customer/Help';

// Staff dashboards
import Analytics from './pages/staff/Analytics';
import CsrDashboard from './pages/staff/CsrDashboard';
import BranchManagerDashboard from './pages/staff/BranchManagerDashboard';
import LoanOfficerDashboard from './pages/staff/LoanOfficerDashboard';
import AdminDashboard from './pages/staff/AdminDashboard';
import KycReview from './pages/staff/KycReview';
import AccountApplicationsReview from './pages/staff/AccountApplicationsReview';
import PendingTransfersReview from './pages/staff/PendingTransfersReview';
import CashCounter from './pages/staff/CashCounter';
import StaffProfile from './pages/staff/StaffProfile';
import StaffSettings from './pages/staff/StaffSettings';
import AccountPin from './pages/customer/AccountPin';
import MyPendingTransfers from './pages/customer/MyPendingTransfers';

// Shared pages (customer + staff)
import NotificationPreferences from './pages/shared/NotificationPreferences';
import ChangePassword from './pages/shared/ChangePassword';
import { SupportTicketsList, SupportTicketDetail } from './pages/shared/SupportTickets';

const ALL_STAFF = [
  ROLES.CSR, ROLES.BRANCH_MANAGER, ROLES.LOAN_OFFICER, ROLES.ADMIN,
];

export default function App() {
  return (
    <Routes>
      {/* Public */}
      <Route path="/"                            element={<Landing />} />
      <Route path="/about"                       element={<About />} />
      <Route path="/branches"                    element={<Branches />} />
      <Route path="/products/:slug"              element={<InfoPage kind="products" />} />
      <Route path="/segments/:slug"              element={<InfoPage kind="segments" />} />
      {/* Single sign-in page — the backend identifies the role from the JWT
          and we route to the right home page after login. */}
      <Route path="/login"                       element={<RedirectIfAuthed><Login /></RedirectIfAuthed>} />
      {/* Legacy role-specific URLs fall back to the unified login. */}
      <Route path="/login/*"                     element={<Navigate to="/login" replace />} />
      <Route path="/signup"                      element={<RedirectIfAuthed><Signup /></RedirectIfAuthed>} />
      <Route path="/forgot-password"             element={<ForgotPassword />} />
      <Route path="/unauthorized"                element={<Unauthorized />} />

      {/* ── Customer app shell ───────────────────────────────────────────── */}
      <Route
        path="/app"
        element={
          <ProtectedRoute roles={[ROLES.CUSTOMER]}>
            <AppShell />
          </ProtectedRoute>
        }
      >
        <Route index                          element={<Dashboard />} />
        <Route path="accounts"                element={<Accounts />} />
        <Route path="accounts/apply"          element={<AccountApply />} />
        <Route path="accounts/:accountNo"     element={<AccountDetail />} />
        <Route path="transactions"            element={<Transactions />} />
        <Route path="transfer"                element={<Transfer />} />
        <Route path="transfer/pending"        element={<MyPendingTransfers />} />
        <Route path="security/pin"            element={<AccountPin />} />
        <Route path="loans"                   element={<Loans />} />
        <Route path="loans/:id"               element={<LoanDetail />} />
        <Route path="kyc"                     element={<Kyc />} />
        <Route path="profile"                 element={<Profile />} />
        <Route path="notifications"           element={<Notifications />} />
        <Route path="settings"                element={<Settings />} />
        <Route path="settings/security"       element={<ChangePassword />} />
        <Route path="settings/notifications"  element={<NotificationPreferences />} />
        <Route path="support"                 element={<SupportTicketsList staff={false} />} />
        <Route path="support/:id"             element={<SupportTicketDetail staff={false} />} />
        <Route path="help"                    element={<Help />} />
      </Route>

      {/* ── Staff app shell ──────────────────────────────────────────────── */}
      <Route
        path="/staff"
        element={
          <ProtectedRoute roles={ALL_STAFF}>
            <AppShell />
          </ProtectedRoute>
        }
      >
        <Route index                          element={<StaffHomeRedirect />} />

        <Route path="admin"                   element={<ProtectedRoute roles={[ROLES.ADMIN]}><AdminDashboard /></ProtectedRoute>} />
        <Route path="csr"                     element={<ProtectedRoute roles={[ROLES.CSR, ROLES.BRANCH_MANAGER, ROLES.ADMIN]}><CsrDashboard /></ProtectedRoute>} />
        <Route path="kyc-review"              element={<ProtectedRoute roles={[ROLES.CSR, ROLES.BRANCH_MANAGER, ROLES.ADMIN]}><KycReview /></ProtectedRoute>} />
        <Route path="account-applications"    element={<ProtectedRoute roles={[ROLES.CSR, ROLES.BRANCH_MANAGER, ROLES.LOAN_OFFICER, ROLES.ADMIN]}><AccountApplicationsReview /></ProtectedRoute>} />
        <Route path="pending-transfers"       element={<ProtectedRoute roles={[ROLES.CSR, ROLES.BRANCH_MANAGER, ROLES.ADMIN]}><PendingTransfersReview /></ProtectedRoute>} />
        <Route path="cash"                    element={<ProtectedRoute roles={[ROLES.CSR, ROLES.BRANCH_MANAGER, ROLES.ADMIN]}><CashCounter /></ProtectedRoute>} />

        <Route path="profile"                 element={<StaffProfile />} />
        <Route path="settings"                element={<StaffSettings />} />
        <Route path="branch"                  element={<ProtectedRoute roles={[ROLES.BRANCH_MANAGER, ROLES.ADMIN]}><BranchManagerDashboard /></ProtectedRoute>} />
        <Route path="loans-ops"               element={<ProtectedRoute roles={[ROLES.LOAN_OFFICER, ROLES.BRANCH_MANAGER, ROLES.ADMIN]}><LoanOfficerDashboard /></ProtectedRoute>} />
        <Route path="analytics"               element={<Analytics />} />

        <Route path="customers"               element={<CustomerLookup />} />
        <Route path="support"                 element={<SupportTicketsList staff={true} />} />
        <Route path="support/:id"             element={<SupportTicketDetail staff={true} />} />

        <Route path="settings/security"       element={<ChangePassword />} />
        <Route path="settings/notifications"  element={<NotificationPreferences />} />
      </Route>

      {/* Fallback */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

function RedirectIfAuthed({ children }) {
  const { user, bootstrapping } = useAuth();
  if (bootstrapping) return null;
  if (user) return <Navigate to={roleHomePath(user.role)} replace />;
  return children;
}

function StaffHomeRedirect() {
  const { user } = useAuth();
  return <Navigate to={roleHomePath(user?.role)} replace />;
}

// Lightweight inline placeholder — the customer-search workflow needs a full page
// but the API is the existing /customers endpoints. We render a slim shell so the
// nav link works; users can still use the analytics + CSR dashboard for context.
function CustomerLookup() {
  return (
    <div className="card p-10 text-center max-w-2xl mx-auto">
      <h1 className="font-display text-2xl font-extrabold">Customer search</h1>
      <p className="mt-2 text-accent-slate">
        Full customer-360 view will land here. For now you can use the customer search
        in the CSR dashboard, or hit <code>GET /customers</code> directly.
      </p>
    </div>
  );
}
