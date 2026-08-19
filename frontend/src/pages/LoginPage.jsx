import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const doLogin = async (e) => {
    e.preventDefault()
    setBusy(true)
    setError('')
    try {
      await login(username.trim(), password)
      // ProtectedRoute redirects cross-role anyway; go to home and let it redirect.
      navigate('/', { replace: true })
    } catch (err) {
      setError(err.message || 'Login failed')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="login-page">
      <form className="card login-card" onSubmit={doLogin}>
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
          <p>Demo accounts</p>
          <button type="button" onClick={() => { setUsername('teacher'); setPassword('password') }}>Teacher</button>
          <button type="button" onClick={() => { setUsername('student1'); setPassword('password') }}>Student</button>
        </div>
      </form>
    </div>
  )
}