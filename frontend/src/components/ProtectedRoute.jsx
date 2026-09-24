import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

/**
 * Hides pages from users who are not logged in (or not admins, when adminOnly).
 * This is a UX convenience only: the backend enforces the same rules on every request.
 */
export default function ProtectedRoute({ children, adminOnly = false }) {
  const { isAuthenticated, isAdmin } = useAuth();

  if (!isAuthenticated) return <Navigate to="/login" replace />;
  if (adminOnly && !isAdmin) return <Navigate to="/employees" replace />;
  return children;
}
