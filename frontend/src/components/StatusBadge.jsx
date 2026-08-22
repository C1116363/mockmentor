const LABELS = {
  AWAITING_PAYMENT: "Payment pending",
  PENDING: "Waiting for a mentor",
  SCHEDULED: "Scheduled",
  COMPLETED: "Completed",
  CANCELLED: "Cancelled",
};

export default function StatusBadge({ status }) {
  return (
    <span className={`badge badge--${status.toLowerCase()}`}>
      {LABELS[status] ?? status}
    </span>
  );
}
