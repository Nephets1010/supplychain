/**
 * Shared domain types for the SupplyChain frontend.
 * These mirror the JPA entities / DTOs described in DESIGN.md.
 * BigDecimal fields are serialized by the backend as JSON numbers, so they are
 * modelled here as `number`.
 */

export type Role = 'MERCHANT' | 'SHOPKEEPER' | 'ADMIN'

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  role: Role
  username: string
}

/** A merchant (货源方) account profile returned by the backend. */
export interface Merchant {
  id: number
  name: string
  contact?: string
  supplierName?: string
  supplierInfo?: string
}

/** A product / 货源 owned by a merchant. */
export interface Product {
  id: number
  merchantId: number
  name: string
  spec: string
  costPrice: number
  price: number
  stockQty: number
  version?: number
  createdAt?: string
  supplierName?: string
  supplierInfo?: string
}

/** Payload for creating / updating a product. */
export interface ProductInput {
  name: string
  spec: string
  costPrice: number
  price: number
  stockQty: number
  supplierName?: string
  supplierInfo?: string
}

export type OrderStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

/** A single line inside a purchase order (快照 of product at order time). */
export interface OrderItem {
  id: number
  orderId: number
  productId: number
  productName: string
  qty: number
  unitPrice: number
  costPrice: number
  subtotal: number
}

/** A purchase order (采购订单). */
export interface PurchaseOrder {
  id: number
  orderNo: string
  shopkeeperId: number
  merchantId: number
  status: OrderStatus
  totalAmount: number
  platformFee: number
  merchantCost: number
  shopkeeperGain: number
  createdAt: string
  reviewedAt?: string
  reviewerId?: number
  reviewNote?: string
  items?: OrderItem[]
  merchantName?: string
  shopkeeperName?: string
}

/** A daily reconciliation (对账) record. */
export interface Reconciliation {
  id: number
  reconcileDate: string
  merchantId: number
  shopkeeperId: number
  orderCount: number
  totalAmount: number
  platformFee: number
  merchantCost: number
  shopkeeperGain: number
  generatedAt: string
  status: string
}

/** An audit-log entry (操作日志). */
export interface AuditLog {
  id: number
  actorId: number
  actorName: string
  action: string
  targetType: string
  targetId: string
  detail: string
  createdAt: string
}

/** Overview statistics for the admin dashboard. */
export interface AdminStats {
  orderCount: number
  pendingCount: number
  approvedCount: number
  rejectedCount: number
  totalAmount: number
  platformFeeTotal: number
  merchantCostTotal: number
  shopkeeperGainTotal: number
}

/** Body for the admin review endpoint (审核). */
export interface ReviewRequest {
  status: 'APPROVED' | 'REJECTED'
  items?: { id: number; qty: number }[]
  note?: string
}
