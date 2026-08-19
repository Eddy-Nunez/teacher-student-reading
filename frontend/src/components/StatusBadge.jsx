const styles = {
  NOT_STARTED: 'badge-gray',
  IN_PROGRESS: 'badge-blue',
  COMPLETED: 'badge-green',
}

const labels = {
  NOT_STARTED: 'Not started',
  IN_PROGRESS: 'In progress',
  COMPLETED: 'Completed',
}

export default function StatusBadge({ status }) {
  return <span className={`badge ${styles[status] || 'badge-gray'}`}>{labels[status] || status}</span>
}