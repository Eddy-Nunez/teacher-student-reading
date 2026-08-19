import { Routes, Route, Navigate } from 'react-router-dom'
import ProtectedRoute from './components/ProtectedRoute'
import LoginPage from './pages/LoginPage'
import TeacherDashboard from './pages/TeacherDashboard'
import StudentDashboard from './pages/StudentDashboard'
import ReaderPage from './pages/ReaderPage'
import { useAuth } from './auth/AuthContext'

function HomeRedirect() {
  const { user, loading } = useAuth()
  if (loading) return <div className="center-screen">Loading…</div>
  if (!user) return <Navigate to="/login" replace />
  return <Navigate to={user.role === 'TEACHER' ? '/teacher' : '/student'} replace />
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<HomeRedirect />} />
      <Route path="/login" element={<LoginPage />} />
      <Route
        path="/teacher"
        element={
          <ProtectedRoute role="TEACHER">
            <TeacherDashboard />
          </ProtectedRoute>
        }
      />
      <Route
        path="/student"
        element={
          <ProtectedRoute role="STUDENT">
            <StudentDashboard />
          </ProtectedRoute>
        }
      />
      <Route
        path="/student/assignments/:id"
        element={
          <ProtectedRoute role="STUDENT">
            <ReaderPage />
          </ProtectedRoute>
        }
      />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}