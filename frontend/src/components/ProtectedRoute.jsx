import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

/**
 * Route guard. Requires an authenticated session; optionally restricts to a role
 * (redirecting cross-role traffic to the correct dashboard).
 */
export default function ProtectedRoute({ children, role }) {
  const { user, loading } = useAuth()
  const location = useLocation()

  if (loading) {
    return <div className="center-screen">Loading…</div>
  }

  if (!user) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  if (role && user.role !== role) {
    return <Navigate to={user.role === 'TEACHER' ? '/teacher' : '/student'} replace />
  }

  return children
}