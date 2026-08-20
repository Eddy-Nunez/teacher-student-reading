import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

// One-click demo accounts (seeded by DataSeeder). Original password/password scheme.
const DEMO_ACCOUNTS = [
  { label: 'Ms. Rivera · Teacher', username: 'teacher' },
  { label: 'Ava · Student', username: 'student1' },
  { label: 'Liam · Student', username: 'student2' },
  { label: 'Maya · Student', username: 'student3' },
]

export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const doLogin = async (user, pw, intro = false) => {
    if (!intro) setBusy(true)
    setError('')
    try {
      await login(user, pw)
      // ProtectedRoute redirects cross-role anyway; go to home and let it redirect.
      navigate('/', { replace: true })
    } catch (err) {
      setError(err.message || 'Login failed')
    } finally {
      setBusy(false)
    }
  }

  const demoLogin = (u) => doLogin(u, 'password', true)

  return (
    <div className="login-page">
      <form className="card login-card" onSubmit={(e) => { e.preventDefault(); doLogin(username.trim(), password) }}>
        <h1>Reading Assignments</h1>
        <p className="muted">Teacher sign in to create assignments · Student sign in to read.</p>
        {error && <div className="alert alert-danger">{error}</div>}
        <input
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          placeholder="Username"
          autoComplete="username"
          required
        />
        <input
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="Password"
          autoComplete="current-password"
          required
        />
        <button disabled={busy} className="btn btn-primary">
          {busy ? 'Signing in…' : 'Sign in'}
        </button>

        <div className="demo-accounts">
          <p>Or jump straight in with a demo account</p>
          <div className="demo-grid">
            {DEMO_ACCOUNTS.map((a) => (
              <button
                key={a.username}
                type="button"
                className="btn btn-outline-primary demo-btn"
                onClick={() => demoLogin(a.username)}
              >
                {a.label}
              </button>
            ))}
          </div>
        </div>
      </form>
    </div>
  )
}