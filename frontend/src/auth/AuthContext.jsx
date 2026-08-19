import { createContext, useContext, useEffect, useState } from 'react'
import api from '../api/client'

const AuthContext = createContext(null)

const userKey = 'user'

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  // On boot, optimistically show any cached user, then revalidate the session server-side (/me),
  // which is resolved purely from the HttpOnly cookie.
  useEffect(() => {
    const cached = localStorage.getItem(userKey)
    if (cached) setUser(JSON.parse(cached))

    api.get('/auth/me')
      .then((res) => {
        localStorage.setItem(userKey, JSON.stringify(res.data))
        setUser(res.data)
      })
      .catch(() => {
        // 401 handled by the interceptor; clear stale cache.
        localStorage.removeItem(userKey)
        setUser(null)
      })
      .finally(() => setLoading(false))
  }, [])

  // The server issues the JWT into an HttpOnly cookie; the response body only carries user info.
  // Use axios so the CSRF header is attached to this state-changing POST.
  const login = async (username, password) => {
    const res = await api.post('/auth/login', { username, password })
    const data = res.data
    localStorage.setItem(userKey, JSON.stringify(data))
    setUser(data)
    return data
  }

  const logout = async () => {
    try {
      await api.post('/auth/logout')
    } catch {
      // ignore network errors — still clear locally
    }
    localStorage.removeItem(userKey)
    setUser(null)
    window.location.href = '/login'
  }

  const value = { user, login, logout, loading }
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within <AuthProvider>')
  return ctx
}