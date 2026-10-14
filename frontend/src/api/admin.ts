import client from './client'
import type {
  PurchaseOrder,
  Reconciliation,
  AuditLog,
  AdminStats,
  ReviewRequest,
  OrderStatus,
} from '@/types'

/** GET /api/admin/orders?status= — all orders (optional status filter). */
export function getOrders(status?: OrderStatus): Promise<PurchaseOrder[]> {
  return client
    .get<PurchaseOrder[]>('/admin/orders', {
      params: status ? { status } : undefined,
    })
    .then((r) => r.data)
}

/** GET /api/admin/orders/{id} — order detail. */
export function getOrder(id: number): Promise<PurchaseOrder> {
  return client.get<PurchaseOrder>(`/admin/orders/${id}`).then((r) => r.data)
}

/** POST /api/admin/orders/{id}/review — approve / reject (optionally edit qty). */
export function reviewOrder(
  id: number,
  data: ReviewRequest,
): Promise<PurchaseOrder> {
  return client
    .post<PurchaseOrder>(`/admin/orders/${id}/review`, data)
    .then((r) => r.data)
}

/** GET /api/admin/reconciliations?date= — reconciliation records. */
export function getReconciliations(date?: string): Promise<Reconciliation[]> {
  return client
    .get<Reconciliation[]>('/admin/reconciliations', {
      params: date ? { date } : undefined,
    })
    .then((r) => r.data)
}

/** POST /api/admin/reconciliations/run?date= — manually trigger reconciliation. */
export function runReconciliation(date?: string): Promise<Reconciliation[]> {
  return client
    .post<Reconciliation[]>('/admin/reconciliations/run', {}, {
      params: date ? { date } : undefined,
    })
    .then((r) => r.data)
}

/** GET /api/admin/audit — operation audit log. */
export function getAuditLogs(): Promise<AuditLog[]> {
  return client.get<AuditLog[]>('/admin/audit').then((r) => r.data)
}

/** GET /api/admin/stats — overview statistics. */
export function getStats(): Promise<AdminStats> {
  return client.get<AdminStats>('/admin/stats').then((r) => r.data)
}
