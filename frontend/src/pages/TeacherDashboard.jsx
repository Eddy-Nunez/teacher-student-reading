import { useCallback, useEffect, useState } from 'react'
import api from '../api/client'
import Navbar from '../components/Navbar'
import StatusBadge from '../components/StatusBadge'

export default function TeacherDashboard() {
  const [books, setBooks] = useState([])
  const [assignments, setAssignments] = useState([])
  const [students, setStudents] = useState([])
  const [bookId, setBookId] = useState('')
  const [dueDate, setDueDate] = useState('')
  const [error, setError] = useState('')
  const [creating, setCreating] = useState(false)
  const [expanded, setExpanded] = useState(null) // assignment id whose roster is open

  const load = useCallback(async () => {
    try {
      const [b, a, s] = await Promise.all([
        api.get('/teacher/books'),
        api.get('/teacher/assignments'),
        api.get('/teacher/students'),
      ])
      setBooks(b.data)
      setAssignments(a.data)
      setStudents(s.data)
    } catch (err) {
      setError('Could not load data — is the server running?')
    }
  }, [])

  useEffect(() => {
    load()
    // Keep the progress table fresh while the page is open (silent refetch).
    const poll = setInterval(load, 15000)
    return () => clearInterval(poll)
  }, [load])

  // Any page interaction also refreshes student data, so the teacher never has
  // to manually reload to see new statuses/minutes.
  const toggle = (id) => {
    setExpanded((prev) => (prev === id ? null : id))
    load()
  }

  const createAssignment = async (e) => {
    e.preventDefault()
    setCreating(true)
    setError('')
    try {
      await api.post('/teacher/assignments', { bookId: Number(bookId), dueDate })
      setBookId('')
      setDueDate('')
      await load()
    } catch (err) {
      setError(err.response?.data?.detail || err.message || 'Failed to create assignment')
    } finally {
      setCreating(false)
    }
  }

  const today = new Date().toISOString().slice(0, 10)

  return (
    <div>
      <Navbar />
      <main className="container">
        <h1>Teacher dashboard</h1>
        {error && <div className="alert alert-danger">{error}</div>}

        <section className="card">
          <h2>Create a reading assignment</h2>
          <form className="hstack" onSubmit={createAssignment}>
            <select value={bookId} onChange={(e) => setBookId(e.target.value)} required>
              <option value="" disabled>Select a book…</option>
              {books.map((b) => (
                <option key={b.id} value={b.id}>{b.title} — {b.author}</option>
              ))}
            </select>
            <input
              type="date"
              value={dueDate}
              min={today}
              onChange={(e) => setDueDate(e.target.value)}
              required
            />
            <button className="btn btn-primary" disabled={creating || !bookId || !dueDate}>
              {creating ? 'Creating…' : 'Assign to all students'}
            </button>
          </form>
          <p className="muted small">
            Assignments are automatically given to every student ({students.length} enrolled).
          </p>
        </section>

        <section className="card">
          <h2>Assignments &amp; progress</h2>
          {assignments.length === 0 && <p className="muted">No assignments yet — create one above.</p>}
          <table className="table">
            <thead>
              <tr>
                <th>Book</th>
                <th>Due</th>
                <th>Not started</th>
                <th>In progress</th>
                <th>Completed</th>
                <th>Status detail</th>
              </tr>
            </thead>
            <tbody>
              {assignments.map((a) => (
                <AssignmentRows
                  key={a.id}
                  a={a}
                  expanded={expanded === a.id}
                  onToggle={() => toggle(a.id)}
                />
              ))}
            </tbody>
          </table>
        </section>
      </main>
    </div>
  )
}

function AssignmentRows({ a, expanded, onToggle }) {
  return (
    <>
      <tr>
        <td><strong>{a.bookTitle}</strong><div className="muted small">{a.bookAuthor}</div></td>
        <td>{a.dueDate}</td>
        <td>{a.notStartedCount}</td>
        <td>{a.inProgressCount}</td>
        <td>{a.completedCount}</td>
        <td>
          <button className="btn btn-outline-secondary btn-sm" onClick={onToggle}>
            {expanded ? 'Hide' : 'View'} students
          </button>
        </td>
      </tr>
      {expanded && (
        <tr className="row-detail">
          <td colSpan={6}>
            <table className="table table-sm">
              <thead>
                <tr><th>Student</th><th>Status</th><th>Minutes read</th></tr>
              </thead>
              <tbody>
                {a.studentProgress.map((p) => (
                  <tr key={p.studentId}>
                    <td>{p.studentName}</td>
                    <td><StatusBadge status={p.status} /></td>
                    <td>{p.elapsedMinutes}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </td>
        </tr>
      )}
    </>
  )
}