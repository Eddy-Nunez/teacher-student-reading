import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import 'bootstrap/dist/css/bootstrap.min.css'
import './index.css'
import App from './App.jsx'
import { AuthProvider } from './auth/AuthContext'
import api from './api/client'

// Establish the double-submit CSRF cookie (GET /api/auth/csrf is permitted and writes the
// readable XSRF-TOKEN cookie) so the first state-changing request has a token to send.
api.get('/auth/csrf').catch(() => {})

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <App />
      </AuthProvider>
    </BrowserRouter>
  </StrictMode>,
)
