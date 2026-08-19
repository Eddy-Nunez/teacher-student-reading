import axios from 'axios'

// Relative base URL: dev uses the Vite proxy, production uses same-origin reverse proxy.
const api = axios.create({ baseURL: '/api' })

// Attach the JWT to every request.
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Handle session expiry globally: drop credentials and bounce to /login.
api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401 && localStorage.getItem('token')) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = '/login'
      }
    }
    return Promise.reject(err)
  },
)

export default api
