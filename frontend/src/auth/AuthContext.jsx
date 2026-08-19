import { createContext, useContext, useEffect, useState } from 'react'

const AuthContext = createContext(null)

const userKey = 'user'

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  // Restore any existing session on first mount.
  useEffect(() => {
    const token = localStorage.getItem('token')
    const saved = localStorage.getItem(userKey)
    if (token && saved) {
      setUser(JSON.parse(saved))
    }
    setLoading(false)
  }, [])

  const login = async (username, password) => {
    const res = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
    })
    if (!res.ok) {
      if (res.status === 401) throw new Error('Invalid credentials')
      throw new Error('Login failed — please try again')
    }
    const data = await res.json()
    localStorage.setItem('token', data.token)
    const u = { userId: data.userId, username: data.username, displayName: data.displayName, role: data.role }
    localStorage.setItem(userKey, JSON.stringify(u))
    setUser(u)
    return u
  }

  const logout = () => {
    localStorage.removeItem('token')
    localStorage.removeItem(userKey)
    setUser(null)
    window.location.href = '/login'
  }

  const value = { user, login, logout, loading }
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

// Convenience hook.
export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within <AuthProvider>')
  return ctx
}