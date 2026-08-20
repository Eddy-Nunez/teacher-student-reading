import { useEffect, useState } from 'react'

const TICK_MS = 250

function format(ms) {
  const totalSec = Math.max(0, Math.floor(ms / 1000))
  const m = Math.floor(totalSec / 60)
  const s = totalSec % 60
  return `${m} min ${String(s).padStart(2, '0')} sec`
}

/**
 * Live "X minutes Y seconds" read-out for the reader.
 *
 * Deliberately isolated to its own small component + a coarse (250ms) interval:
 *  - It holds its own state, so React only re-renders the timer node, never the
 *    surrounding article (paragraphs/typography stay untouched → smooth repaints).
 *  - It only calls setState when the *visible second* actually changes, so each
 *    repaint is a single small subtree and most intervals are no-ops (no pointless
 *    DOM churn while painting).
 *  - The elapsed value is derived from an immutable `startMs` anchor + wall clock,
 *    so the display is cheap and stateless — no prop churn from the parent.
 */
export default function ReadingTimer({ startMs }) {
  const [label, setLabel] = useState(() => format(startMs))

  useEffect(() => {
    const startedAt = Date.now()
    const prev = { sec: -1 }

    const update = () => {
      const totalSec = Math.floor((startMs + (Date.now() - startedAt)) / 1000)
      if (totalSec !== prev.sec) {
        prev.sec = totalSec
        setLabel(format(startMs + (Date.now() - startedAt)))
      }
    }

    update()
    const timer = setInterval(update, TICK_MS)
    return () => clearInterval(timer)
  }, [startMs])

  return <strong>{label}</strong>
}