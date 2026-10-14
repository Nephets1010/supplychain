import client from './client'
import type { Merchant, Product, PurchaseOrder } from '@/types'

/** GET /api/shop/merchants — list of merchants (names). */
export function getMerchants(): Promise<Merchant[]> {
  return client.get<Merchant[]>('/shop/merchants').then((r) => r.data)
}

/** GET /api/shop/products?merchantId= — products of one merchant. */
export function getProducts(merchantId: number): Promise<Product[]> {
  return client
    .get<Product[]>('/shop/products', { params: { merchantId } })
    .then((r) => r.data)
}

/** POST /api/shop/orders — place a purchase order (decrements stock). */
export function createOrder(
  items: { productId: number; qty: number }[],
): Promise<PurchaseOrder> {
  return client
    .post<PurchaseOrder>('/shop/orders', { items })
    .then((r) => r.data)
}

/** GET /api/shop/orders — my purchase orders. */
export function getOrders(): Promise<PurchaseOrder[]> {
  return client.get<PurchaseOrder[]>('/shop/orders').then((r) => r.data)
}

/** GET /api/shop/orders/{id} — single order detail. */
export function getOrder(id: number): Promise<PurchaseOrder> {
  return client.get<PurchaseOrder>(`/shop/orders/${id}`).then((r) => r.data)
}
