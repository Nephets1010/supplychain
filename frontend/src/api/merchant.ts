import client from './client'
import type { Product, ProductInput, PurchaseOrder } from '@/types'

/** GET /api/merchant/products — my product list. */
export function getProducts(): Promise<Product[]> {
  return client.get<Product[]>('/merchant/products').then((r) => r.data)
}

/** POST /api/merchant/products — list a new product. */
export function createProduct(data: ProductInput): Promise<Product> {
  return client.post<Product>('/merchant/products', data).then((r) => r.data)
}

/** PUT /api/merchant/products/{id} — update a product. */
export function updateProduct(
  id: number,
  data: Partial<ProductInput>,
): Promise<Product> {
  return client.put<Product>(`/merchant/products/${id}`, data).then((r) => r.data)
}

/** GET /api/merchant/orders — purchase orders placed by shopkeepers on me. */
export function getMerchantOrders(): Promise<PurchaseOrder[]> {
  return client.get<PurchaseOrder[]>('/merchant/orders').then((r) => r.data)
}
