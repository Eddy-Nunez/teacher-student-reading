import { useAuth } from '../auth/AuthContext'

export default function Navbar() {
  const { user, logout } = useAuth()
  if (!user) return null
  const isTeacher = user.role === 'TEACHER'
  return (
    <nav className="navbar">
      <div className="navbar-brand">📚 Reading Assignments</div>
      <div className="navbar-right">
        <span className="muted">
          {user.displayName} · {isTeacher ? 'Teacher' : 'Student'}
        </span>
        <button className="btn btn-outline-secondary" onClick={logout}>Sign out</button>
      </div>
    </nav>
  )
}