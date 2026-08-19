import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../api/client'
import Navbar from '../components/Navbar'
import StatusBadge from '../components/StatusBadge'

export default function StudentDashboard() {
  const [assignments, setAssignments] = useState([])
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    try {
      const res = await api.get('/student/assignments')
      setAssignments(res.data)
    } catch (err) {
      setError('Could not load your assignments.')
    }
  }, [])

  useEffect(() => {
    load()
  }, [load])

  return (
    <div>
      <Navbar />
      <main className="container">
        <h1>My reading</h1>
        {error && <div className="alert alert-danger">{error}</div>}
        {assignments.length === 0 && (
          <p className="muted">Nothing assigned yet — check back soon!</p>
        )}
        <div className="grid">
          {assignments.map((a) => (
            <div key={a.id} className="card">
              <h3>{a.bookTitle}</h3>
              <p className="muted">{a.bookAuthor}</p>
              <p>Due {a.dueDate}</p>
              <p><StatusBadge status={a.status} /> · {a.elapsedMinutes} min read</p>
              <Link className="btn btn-primary" to={`/student/assignments/${a.id}`}>
                {a.status === 'COMPLETED' ? 'Review book' : 'Open reader'}
              </Link>
            </div>
          ))}
        </div>
      </main>
    </div>
  )
}