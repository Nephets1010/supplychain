<template>
  <div>
    <el-card>
      <div class="card-header">
        <span class="card-title">浏览商家商品</span>
        <el-select
          v-model="selectedMerchant"
          placeholder="选择商家"
          filterable
          style="width: 260px"
          @change="onMerchantChange"
        >
          <el-option
            v-for="m in merchants"
            :key="m.id"
            :label="m.name"
            :value="m.id"
          />
        </el-select>
      </div>

      <el-table :data="products" v-loading="loadingProducts" border stripe>
        <el-table-column prop="name" label="商品名称" min-width="140" />
        <el-table-column prop="spec" label="规格" min-width="120" />
        <el-table-column label="采购单价" width="110" align="right">
          <template #default="{ row }">{{ fmt(row.price) }}</template>
        </el-table-column>
        <el-table-column prop="stockQty" label="可用库存" width="100" align="center" />
        <el-table-column label="下单数量" width="160" align="center">
          <template #default="{ row }">
            <el-input-number
              v-model="qtyMap[row.id]"
              :min="0"
              :max="row.stockQty"
              :step="1"
              size="small"
              controls-position="right"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-button
              size="small"
              type="primary"
              :disabled="!qtyMap[row.id]"
              @click="placeSingle(row)"
            >
              下单
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="mt footer-actions" v-if="hasQty">
        <span>已选合计：{{ selectedTotal }} 件</span>
        <el-button
          type="success"
          :loading="submitting"
          @click="placeBatch"
        >
          批量下单（按当前数量）
        </el-button>
      </div>
    </el-card>

    <el-card class="mt">
      <div class="card-header">
        <span class="card-title">我的采购订单</span>
        <el-button size="small" :icon="'Refresh'" @click="loadOrders">
          刷新
        </el-button>
      </div>

      <el-table :data="orders" v-loading="loadingOrders" border stripe>
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column label="商家" min-width="120">
          <template #default="{ row }">
            {{ row.merchantName || '商家#' + row.merchantId }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="商品数量" width="100" align="center">
          <template #default="{ row }">{{ totalQty(row) }}</template>
        </el-table-column>
        <el-table-column label="成交额" width="110" align="right">
          <template #default="{ row }">{{ fmt(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column label="平台费" width="100" align="right">
          <template #default="{ row }">{{ fmt(row.platformFee) }}</template>
        </el-table-column>
        <el-table-column label="商家本金" width="110" align="right">
          <template #default="{ row }">{{ fmt(row.merchantCost) }}</template>
        </el-table-column>
        <el-table-column label="我的利润" width="110" align="right">
          <template #default="{ row }">{{ fmt(row.shopkeeperGain) }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="下单时间" width="180" />
        <el-table-column label="明细" width="80" align="center">
          <template #default="{ row }">
            <el-button size="small" @click="viewOrder(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-drawer v-model="orderDrawer" title="订单明细" size="420px">
      <div v-if="currentOrder">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="订单号">
            {{ currentOrder.orderNo }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            {{ statusLabel(currentOrder.status) }}
          </el-descriptions-item>
        <el-descriptions-item label="成交额">
          {{ fmt(currentOrder.totalAmount) }}
        </el-descriptions-item>
        <el-descriptions-item label="平台费">
          {{ fmt(currentOrder.platformFee) }}
        </el-descriptions-item>
        <el-descriptions-item label="商家本金">
          {{ fmt(currentOrder.merchantCost) }}
        </el-descriptions-item>
        <el-descriptions-item label="我的利润">
          {{ fmt(currentOrder.shopkeeperGain) }}
        </el-descriptions-item>
      </el-descriptions>
      <el-table :data="currentOrder.items || []" border class="mt">
        <el-table-column prop="productName" label="商品" min-width="120" />
        <el-table-column prop="qty" label="数量" width="70" align="center" />
        <el-table-column label="单价" width="90" align="right">
          <template #default="{ row }">{{ fmt(row.unitPrice) }}</template>
        </el-table-column>
        <el-table-column label="小计" width="100" align="right">
          <template #default="{ row }">{{ fmt(row.subtotal) }}</template>
        </el-table-column>
      </el-table>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as shopApi from '@/api/shop'
import type { Merchant, Product, PurchaseOrder } from '@/types'

const merchants = ref<Merchant[]>([])
const selectedMerchant = ref<number | null>(null)
const products = ref<Product[]>([])
const qtyMap = reactive<Record<number, number>>({})
const orders = ref<PurchaseOrder[]>([])
const loadingProducts = ref(false)
const loadingOrders = ref(false)
const submitting = ref(false)
const orderDrawer = ref(false)
const currentOrder = ref<PurchaseOrder | null>(null)

const hasQty = computed(() =>
  Object.values(qtyMap).some((q) => q > 0),
)
const selectedTotal = computed(() =>
  Object.values(qtyMap).reduce((a, b) => a + b, 0),
)

function fmt(n: number | string): string {
  const v = typeof n === 'string' ? Number(n) : n
  return Number.isNaN(v) ? '0.00' : v.toFixed(2)
}
function statusType(s: string): '' | 'success' | 'danger' | 'warning' {
  return s === 'APPROVED' ? 'success' : s === 'REJECTED' ? 'danger' : 'warning'
}
function statusLabel(s: string): string {
  return s === 'APPROVED' ? '已通过' : s === 'REJECTED' ? '已驳回' : '待审核'
}
function totalQty(o: PurchaseOrder): number {
  return (o.items || []).reduce((a, b) => a + b.qty, 0)
}
function errorMessage(e: unknown): string {
  const err = e as { response?: { data?: { message?: string; error?: string } } }
  return err?.response?.data?.message || err?.response?.data?.error || '操作失败'
}

async function loadMerchants(): Promise<void> {
  try {
    merchants.value = await shopApi.getMerchants()
  } catch (e) {
    ElMessage.error(errorMessage(e))
  }
}

async function onMerchantChange(id: number): Promise<void> {
  loadingProducts.value = true
  try {
    products.value = await shopApi.getProducts(id)
    for (const p of products.value) {
      if (qtyMap[p.id] === undefined) qtyMap[p.id] = 0
    }
  } catch (e) {
    ElMessage.error(errorMessage(e))
  } finally {
    loadingProducts.value = false
  }
}

async function loadOrders(): Promise<void> {
  loadingOrders.value = true
  try {
    orders.value = await shopApi.getOrders()
  } catch (e) {
    ElMessage.error(errorMessage(e))
  } finally {
    loadingOrders.value = false
  }
}

function placeSingle(row: Product): void {
  const qty = qtyMap[row.id]
  if (!qty || qty <= 0) {
    ElMessage.warning('请先填写数量')
    return
  }
  submitOrder([{ productId: row.id, qty }])
}

function placeBatch(): void {
  const items = products.value
    .filter((p) => qtyMap[p.id] > 0)
    .map((p) => ({ productId: p.id, qty: qtyMap[p.id] }))
  if (!items.length) {
    ElMessage.warning('请先填写至少一个商品数量')
    return
  }
  submitOrder(items)
}

async function submitOrder(
  items: { productId: number; qty: number }[],
): Promise<void> {
  submitting.value = true
  try {
    const created = await shopApi.createOrder(items)
    ElMessage.success('下单成功，订单号 ' + created.orderNo)
    for (const p of products.value) qtyMap[p.id] = 0
    await loadOrders()
    if (selectedMerchant.value) {
      await onMerchantChange(selectedMerchant.value)
    }
  } catch (e) {
    ElMessage.error(errorMessage(e))
  } finally {
    submitting.value = false
  }
}

async function viewOrder(row: PurchaseOrder): Promise<void> {
  try {
    // Fetch full detail to guarantee the items are present in the drawer,
    // regardless of what the list endpoint embeds.
    currentOrder.value = await shopApi.getOrder(row.id)
    orderDrawer.value = true
  } catch (e) {
    ElMessage.error(errorMessage(e))
  }
}

onMounted(() => {
  loadMerchants()
  loadOrders()
})
</script>

<style scoped>
.mt {
  margin-top: 16px;
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.card-title {
  font-size: 16px;
  font-weight: 600;
}
.footer-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 16px;
}
</style>
