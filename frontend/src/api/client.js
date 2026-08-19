import axios from 'axios'

// Relative base URL: dev uses the Vite proxy, production uses same-origin reverse proxy.
const api = axios.create({ baseURL: '/api', withCredentials: true })

// Double-submit CSRF: include the readable XSRF cookie value as a header on state-changing
// requests. The actual auth token lives in an HttpOnly cookie the JS cannot read (see backend
// AuthCookie) — so axios never touches the JWT at all.
api.interceptors.request.use((config) => {
  const xsrf = /(?:^|;\s*)XSRF-TOKEN=([^;]*)/.exec(document.cookie)
  if (xsrf && !/^(GET|HEAD|OPTIONS)$/i.test(config.method)) {
    config.headers['X-XSRF-TOKEN'] = decodeURIComponent(xsrf[1])
  }
  return config
})

// Handle session expiry globally: on 401 drop local state and route to /login.
api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401 && !window.location.pathname.startsWith('/login')) {
      localStorage.removeItem('user')
      window.location.href = '/login'
    }
    return Promise.reject(err)
  },
)

export default api
