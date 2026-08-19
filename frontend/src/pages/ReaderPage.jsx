import { useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import api from '../api/client'
import Navbar from '../components/Navbar'
import StatusBadge from '../components/StatusBadge'

/**
 * Reading view with a session timer.
 *
 * Design: minutes accumulate locally (localStorage is the source of truth for the
 * in-flight session so accidental tab closes don't lose time) and sync to the
 * backend every SYNC_INTERVAL_MS on a `IN_PROGRESS` status update. The backend
 * keeps minutes monotonic, so occasional out-of-order writes are safe.
 */
const SYNC_INTERVAL_MS = 60_000
const MINUTE = 60_000

export default function ReaderPage() {
  const { id } = useParams()
  const [assignment, setAssignment] = useState(null)
  const [minutes, setMinutes] = useState(0)
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  const completedRef = useRef(false)

  const storageKey = `assignment_${id}_timer`

  // Load assignment detail + restore the session timer.
  useEffect(() => {
    let cancelled = false
    api.get(`/student/assignments/${id}`)
      .then((res) => {
        if (cancelled) return
        setAssignment(res.data)
        // Start session from server minutes, then add any unsynced local time.
        const stored = Number(localStorage.getItem(storageKey) || 0)
        setMinutes(res.data.elapsedMinutes + stored)
        // Opening the reader *is* starting to read: promote NOT_STARTED → IN_PROGRESS
        // immediately rather than waiting for the first 60s timer sync.
        if (res.data.status === 'NOT_STARTED') {
          api.put(`/student/assignments/${id}/status`, { status: 'IN_PROGRESS', elapsedMinutes: res.data.elapsedMinutes })
            .then((r) => !cancelled && setAssignment({ ...res.data, status: r.data.status, elapsedMinutes: r.data.elapsedMinutes }))
            .catch(() => { /* next timer sync retries */ })
        }
      })
      .catch(() => !cancelled && setError('Could not load the reading.'))
    return () => { cancelled = true }
  }, [id, storageKey])

  // Tick + periodic sync while the view is open.
  useEffect(() => {
    if (!assignment || assignment.status === 'COMPLETED') return

    const startedAt = Date.now()
    const savedAtStart = Number(localStorage.getItem(storageKey) || 0)

    const tick = () => {
      const elapsed = Math.floor((Date.now() - startedAt) / MINUTE)
      const total = savedAtStart + elapsed
      setMinutes(assignment.elapsedMinutes + total)
      localStorage.setItem(storageKey, String(total))
    }
    tick()
    const interval = setInterval(tick, 1000)

    const sync = () => {
      if (completedRef.current) return
      const total = Math.floor((Date.now() - startedAt) / MINUTE) + savedAtStart
      api.put(`/student/assignments/${id}/status`, {
        status: 'IN_PROGRESS',
        elapsedMinutes: assignment.elapsedMinutes + total,
      }).catch(() => { /* offline — next sync retries */ })
    }
    const syncer = setInterval(sync, SYNC_INTERVAL_MS)

    return () => {
      clearInterval(interval)
      clearInterval(syncer)
      sync() // flush on unmount
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [assignment?.id, assignment?.status])

  const markComplete = async () => {
    setSaving(true)
    completedRef.current = true
    try {
      const total = assignment.elapsedMinutes + Math.floor(Number(localStorage.getItem(storageKey) || 0))
      const res = await api.put(`/student/assignments/${id}/status`, {
        status: 'COMPLETED',
        elapsedMinutes: total,
      })
      localStorage.removeItem(storageKey)
      setAssignment({ ...assignment, status: res.data.status, elapsedMinutes: res.data.elapsedMinutes })
      setMinutes(res.data.elapsedMinutes)
    } catch (err) {
      setError('Could not save — please try again.')
    } finally {
      setSaving(false)
    }
  }

  if (!assignment) {
    return (
      <div>
        <Navbar />
        <main className="container center-screen">{error || 'Loading…'}</main>
      </div>
    )
  }

  const done = assignment.status === 'COMPLETED'

  return (
    <div>
      <Navbar />
      <main className="container">
        <div className="row space-between reader-header">
          <div>
            <Link to="/student" className="muted">← My reading</Link>
            <h1>{assignment.bookTitle}</h1>
            <p className="muted">{assignment.bookAuthor} · due {assignment.dueDate}</p>
          </div>
          <div className="reader-status">
            <StatusBadge status={assignment.status} />
            <div className="minutes-read"><strong>{minutes}</strong> min read</div>
            {!done && (
              <button className="btn btn-success" onClick={markComplete} disabled={saving}>
                {saving ? 'Saving…' : 'Mark as completed'}
              </button>
            )}
          </div>
        </div>

        {error && <div className="alert alert-danger">{error}</div>}

        <article className="reader">
          <p className="lead">{assignment.description}</p>
          {assignment.content.split('\n\n').map((para, i) => (
            <p key={i}>{para}</p>
          ))}
          {assignment.referenceUrl && (
            <p className="muted small">
              Source: <a href={assignment.referenceUrl} target="_blank" rel="noreferrer">external resource</a>
            </p>
          )}
        </article>
      </main>
    </div>
  )
}