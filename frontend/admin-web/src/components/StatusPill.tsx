import type { OrderStatus } from "../lib/types";

const STATUS_META: Record<OrderStatus, { label: string; color: string; bg: string }> = {
  RECEIVED: { label: "Received", color: "var(--neutral)", bg: "var(--neutral-bg)" },
  PICKING: { label: "Picking", color: "var(--warning)", bg: "var(--warning-bg)" },
  READY_FOR_COLLECTION: { label: "Ready", color: "var(--info)", bg: "var(--info-bg)" },
  EN_ROUTE: { label: "En route", color: "var(--info)", bg: "var(--info-bg)" },
  ARRIVED_AT_NODE: { label: "Arrived", color: "var(--warning)", bg: "var(--warning-bg)" },
  DELIVERED: { label: "Delivered", color: "var(--success)", bg: "var(--success-bg)" },
  CANCELLED: { label: "Cancelled", color: "var(--error)", bg: "var(--error-bg)" }
};

export function StatusPill({ status }: { status: OrderStatus }) {
  const meta = STATUS_META[status] ?? STATUS_META.RECEIVED;
  return (
    <span className="status-pill" style={{ color: meta.color, background: meta.bg }}>
      {meta.label}
    </span>
  );
}
