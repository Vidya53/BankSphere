import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { Spinner } from './Spinner';

export function ProtectedRoute({ roles, children }) {
  const { user, isAuthenticated, bootstrapping } = useAuth();
  const location = useLocation();

  if (bootstrapping) {
    return (
      <div className="min-h-screen grid place-items-center">
        <Spinner size={32} />
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  if (roles && roles.length > 0 && !roles.includes(user?.role)) {
    return <Navigate to="/unauthorized" replace />;
  }

  return children;
}
