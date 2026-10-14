<template>
  <div>
    <el-card>
      <div class="card-header">
        <span class="card-title">我的商品</span>
        <el-button type="primary" :icon="'Plus'" @click="openDialog">
          上架商品
        </el-button>
      </div>

      <el-table :data="products" v-loading="loadingProducts" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="商品名称" min-width="140" />
        <el-table-column prop="spec" label="规格" min-width="120" />
        <el-table-column label="成本(本金)" width="110" align="right">
          <template #default="{ row }">{{ fmt(row.costPrice) }}</template>
        </el-table-column>
        <el-table-column label="售价" width="100" align="right">
          <template #default="{ row }">{{ fmt(row.price) }}</template>
        </el-table-column>
        <el-table-column prop="stockQty" label="库存" width="90" align="center" />
        <el-table-column prop="supplierName" label="供应商" min-width="120" />
        <el-table-column label="操作" width="90" align="center">
          <template #default="{ row }">
            <el-button size="small" @click="editProduct(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="mt">
      <div class="card-header">
        <span class="card-title">来自店家的采购订单</span>
        <el-button size="small" :icon="'Refresh'" @click="loadOrders">
          刷新
        </el-button>
      </div>

      <el-table :data="orders" v-loading="loadingOrders" border stripe>
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column label="店家" min-width="120">
          <template #default="{ row }">
            {{ row.shopkeeperName || '店家#' + row.shopkeeperId }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="成交额" width="110" align="right">
          <template #default="{ row }">{{ fmt(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column label="商家本金" width="110" align="right">
          <template #default="{ row }">{{ fmt(row.merchantCost) }}</template>
        </el-table-column>
        <el-table-column label="平台费" width="100" align="right">
          <template #default="{ row }">{{ fmt(row.platformFee) }}</template>
        </el-table-column>
        <el-table-column label="店家利润" width="100" align="right">
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

    <!-- 上架 / 编辑 商品 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑商品' : '上架商品'"
      width="480px"
    >
      <el-form :model="productForm" label-width="100px">
        <el-form-item label="商品名称" required>
          <el-input v-model="productForm.name" placeholder="如：新疆红枣" />
        </el-form-item>
        <el-form-item label="规格">
          <el-input v-model="productForm.spec" placeholder="如：500g/袋" />
        </el-form-item>
        <el-form-item label="成本(本金)" required>
          <el-input-number
            v-model="productForm.costPrice"
            :min="0"
            :precision="2"
            :step="0.1"
            controls-position="right"
          />
        </el-form-item>
        <el-form-item label="售价" required>
          <el-input-number
            v-model="productForm.price"
            :min="0"
            :precision="2"
            :step="0.1"
            controls-position="right"
          />
        </el-form-item>
        <el-form-item label="库存" required>
          <el-input-number
            v-model="productForm.stockQty"
            :min="0"
            :step="1"
            controls-position="right"
          />
        </el-form-item>
        <el-form-item label="供应商">
          <el-input v-model="productForm.supplierName" placeholder="选填" />
        </el-form-item>
        <el-form-item label="供应链信息">
          <el-input
            v-model="productForm.supplierInfo"
            type="textarea"
            :rows="2"
            placeholder="选填，备注规格/价格/数量等"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveProduct">
          保存
        </el-button>
      </template>
    </el-dialog>

    <!-- 订单明细 -->
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
        <el-descriptions-item label="商家本金">
          {{ fmt(currentOrder.merchantCost) }}
        </el-descriptions-item>
        <el-descriptions-item label="平台费">
          {{ fmt(currentOrder.platformFee) }}
        </el-descriptions-item>
        <el-descriptions-item label="店家利润">
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as merchantApi from '@/api/merchant'
import type { Product, ProductInput, PurchaseOrder } from '@/types'

const products = ref<Product[]>([])
const orders = ref<PurchaseOrder[]>([])
const loadingProducts = ref(false)
const loadingOrders = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)
const orderDrawer = ref(false)
const currentOrder = ref<PurchaseOrder | null>(null)

function emptyForm(): ProductInput {
  return {
    name: '',
    spec: '',
    costPrice: 0,
    price: 0,
    stockQty: 0,
    supplierName: '',
    supplierInfo: '',
  }
}
const productForm = reactive<ProductInput>(emptyForm())

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
function errorMessage(e: unknown): string {
  const err = e as { response?: { data?: { message?: string; error?: string } } }
  return err?.response?.data?.message || err?.response?.data?.error || '操作失败'
}

async function loadProducts(): Promise<void> {
  loadingProducts.value = true
  try {
    products.value = await merchantApi.getProducts()
  } catch (e) {
    ElMessage.error(errorMessage(e))
  } finally {
    loadingProducts.value = false
  }
}

async function loadOrders(): Promise<void> {
  loadingOrders.value = true
  try {
    orders.value = await merchantApi.getMerchantOrders()
  } catch (e) {
    ElMessage.error(errorMessage(e))
  } finally {
    loadingOrders.value = false
  }
}

function openDialog(): void {
  editingId.value = null
  Object.assign(productForm, emptyForm())
  dialogVisible.value = true
}

function editProduct(row: Product): void {
  editingId.value = row.id
  Object.assign(productForm, {
    name: row.name,
    spec: row.spec,
    costPrice: row.costPrice,
    price: row.price,
    stockQty: row.stockQty,
    supplierName: row.supplierName || '',
    supplierInfo: row.supplierInfo || '',
  })
  dialogVisible.value = true
}

async function saveProduct(): Promise<void> {
  if (!productForm.name) {
    ElMessage.warning('请填写商品名称')
    return
  }
  if (productForm.costPrice < 0 || productForm.price < 0) {
    ElMessage.warning('价格不能为负数')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await merchantApi.updateProduct(editingId.value, productForm)
      ElMessage.success('已更新')
    } else {
      await merchantApi.createProduct(productForm)
      ElMessage.success('已上架')
    }
    dialogVisible.value = false
    await loadProducts()
  } catch (e) {
    ElMessage.error(errorMessage(e))
  } finally {
    saving.value = false
  }
}

function viewOrder(row: PurchaseOrder): void {
  currentOrder.value = row
  orderDrawer.value = true
}

onMounted(() => {
  loadProducts()
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
</style>
