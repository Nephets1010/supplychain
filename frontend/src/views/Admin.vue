<template>
  <div>
    <!-- 概览统计 -->
    <el-row :gutter="16">
      <el-col :span="6" v-for="c in statCards" :key="c.label">
        <el-card shadow="hover">
          <div class="stat-label">{{ c.label }}</div>
          <div class="stat-value">{{ c.value }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 订单管理 -->
    <el-card class="mt">
      <div class="card-header">
        <span class="card-title">订单管理</span>
        <el-radio-group v-model="statusFilter" @change="loadOrders">
          <el-radio-button value="ALL">全部</el-radio-button>
          <el-radio-button value="PENDING">待审核</el-radio-button>
          <el-radio-button value="APPROVED">已通过</el-radio-button>
          <el-radio-button value="REJECTED">已驳回</el-radio-button>
        </el-radio-group>
      </div>

      <el-table :data="orders" v-loading="loadingOrders" border stripe>
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column label="商家" min-width="110">
          <template #default="{ row }">
            {{ row.merchantName || '商家#' + row.merchantId }}
          </template>
        </el-table-column>
        <el-table-column label="店家" min-width="110">
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
        <el-table-column label="平台费" width="100" align="right">
          <template #default="{ row }">{{ fmt(row.platformFee) }}</template>
        </el-table-column>
        <el-table-column label="商家本金" width="110" align="right">
          <template #default="{ row }">{{ fmt(row.merchantCost) }}</template>
        </el-table-column>
        <el-table-column label="店家利润" width="110" align="right">
          <template #default="{ row }">{{ fmt(row.shopkeeperGain) }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="下单时间" width="180" />
        <el-table-column label="操作" width="90" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="warning" @click="openReview(row)">
              审核
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 对账记录 -->
    <el-card class="mt">
      <div class="card-header">
        <span class="card-title">对账记录</span>
        <div class="recon-actions">
          <el-date-picker
            v-model="reconDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期（默认全部）"
            style="width: 200px; margin-right: 8px"
          />
          <el-button :icon="'Refresh'" @click="loadRecons">查询</el-button>
          <el-button
            type="primary"
            :icon="'Check'"
            :loading="running"
            @click="runRecon"
          >
            手动对账
          </el-button>
        </div>
      </div>

      <el-table :data="recons" v-loading="loadingRecons" border stripe>
        <el-table-column prop="reconcileDate" label="对账日" width="120" />
        <el-table-column label="商家" width="90" align="center">
          <template #default="{ row }">#{{ row.merchantId }}</template>
        </el-table-column>
        <el-table-column label="店家" width="90" align="center">
          <template #default="{ row }">#{{ row.shopkeeperId }}</template>
        </el-table-column>
        <el-table-column prop="orderCount" label="订单数" width="90" align="center" />
        <el-table-column label="成交额" width="110" align="right">
          <template #default="{ row }">{{ fmt(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column label="平台费" width="100" align="right">
          <template #default="{ row }">{{ fmt(row.platformFee) }}</template>
        </el-table-column>
        <el-table-column label="商家本金" width="110" align="right">
          <template #default="{ row }">{{ fmt(row.merchantCost) }}</template>
        </el-table-column>
        <el-table-column label="店家利润" width="110" align="right">
          <template #default="{ row }">{{ fmt(row.shopkeeperGain) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" min-width="100" />
      </el-table>
    </el-card>

    <!-- 操作日志 -->
    <el-card class="mt">
      <div class="card-header">
        <span class="card-title">操作日志</span>
        <el-button size="small" :icon="'Refresh'" @click="loadAudit">刷新</el-button>
      </div>
      <el-table
        :data="auditLogs"
        v-loading="loadingAudit"
        border
        stripe
        height="360"
      >
        <el-table-column prop="createdAt" label="时间" width="180" />
        <el-table-column prop="actorName" label="操作人" width="120" />
        <el-table-column prop="action" label="动作" min-width="160" />
        <el-table-column prop="targetType" label="对象" width="110" />
        <el-table-column prop="targetId" label="对象ID" width="120" />
        <el-table-column prop="detail" label="详情" min-width="200" />
      </el-table>
    </el-card>

    <!-- 审核抽屉 -->
    <el-drawer v-model="reviewDrawer" title="订单审核" size="460px">
      <template v-if="reviewOrder">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="订单号">
            {{ reviewOrder.orderNo }}
          </el-descriptions-item>
          <el-descriptions-item label="当前状态">
            {{ statusLabel(reviewOrder.status) }}
          </el-descriptions-item>
        </el-descriptions>

        <el-table :data="editItems" border class="mt">
          <el-table-column prop="productName" label="商品" min-width="120" />
          <el-table-column label="单价" width="90" align="right">
            <template #default="{ row }">{{ fmt(row.unitPrice) }}</template>
          </el-table-column>
          <el-table-column label="数量" width="130">
            <template #default="{ row }">
              <el-input-number
                v-model="row.qty"
                :min="1"
                :step="1"
                size="small"
                controls-position="right"
              />
            </template>
          </el-table-column>
          <el-table-column label="小计" width="100" align="right">
            <template #default="{ row }">{{ fmt(row.unitPrice * row.qty) }}</template>
          </el-table-column>
        </el-table>

        <el-input
          v-model="reviewNote"
          type="textarea"
          :rows="2"
          placeholder="审核备注（可选）"
          class="mt"
        />

        <div class="mt review-actions">
          <el-button
            type="success"
            :loading="reviewing"
            @click="submitReview('APPROVED')"
          >
            通过
          </el-button>
          <el-button
            type="danger"
            :loading="reviewing"
            @click="submitReview('REJECTED')"
          >
            驳回
          </el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as adminApi from '@/api/admin'
import type {
  PurchaseOrder,
  OrderItem,
  Reconciliation,
  AuditLog,
  AdminStats,
  OrderStatus,
} from '@/types'

const orders = ref<PurchaseOrder[]>([])
const recons = ref<Reconciliation[]>([])
const auditLogs = ref<AuditLog[]>([])
const stats = ref<AdminStats | null>(null)
const statusFilter = ref<'ALL' | OrderStatus>('ALL')
const reconDate = ref<string>('')
const loadingOrders = ref(false)
const loadingRecons = ref(false)
const loadingAudit = ref(false)
const running = ref(false)
const reviewDrawer = ref(false)
const reviewing = ref(false)
const reviewOrder = ref<PurchaseOrder | null>(null)
const editItems = ref<OrderItem[]>([])
const reviewNote = ref('')

const statCards = computed(() => {
  const s = stats.value
  if (!s) return []
  return [
    { label: '订单总数', value: s.orderCount },
    { label: '待审核', value: s.pendingCount },
    { label: '成交总额', value: fmt(s.totalAmount) },
    { label: '平台费合计', value: fmt(s.platformFeeTotal) },
  ]
})

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

async function loadOrders(): Promise<void> {
  loadingOrders.value = true
  try {
    orders.value = await adminApi.getOrders(
      statusFilter.value === 'ALL' ? undefined : statusFilter.value,
    )
  } catch (e) {
    ElMessage.error(errorMessage(e))
  } finally {
    loadingOrders.value = false
  }
}

async function loadRecons(): Promise<void> {
  loadingRecons.value = true
  try {
    recons.value = await adminApi.getReconciliations(
      reconDate.value || undefined,
    )
  } catch (e) {
    ElMessage.error(errorMessage(e))
  } finally {
    loadingRecons.value = false
  }
}

async function loadAudit(): Promise<void> {
  loadingAudit.value = true
  try {
    auditLogs.value = await adminApi.getAuditLogs()
  } catch (e) {
    ElMessage.error(errorMessage(e))
  } finally {
    loadingAudit.value = false
  }
}

async function loadStats(): Promise<void> {
  try {
    stats.value = await adminApi.getStats()
  } catch (e) {
    ElMessage.error(errorMessage(e))
  }
}

async function runRecon(): Promise<void> {
  running.value = true
  try {
    const res = await adminApi.runReconciliation(reconDate.value || undefined)
    const arr = Array.isArray(res) ? res : [res]
    ElMessage.success('对账完成，生成 ' + arr.length + ' 条记录')
    await loadRecons()
    await loadStats()
  } catch (e) {
    ElMessage.error(errorMessage(e))
  } finally {
    running.value = false
  }
}

async function openReview(row: PurchaseOrder): Promise<void> {
  try {
    // Fetch full detail so the review editor always has the latest items,
    // independent of whether the list endpoint embeds items.
    const detail = await adminApi.getOrder(row.id)
    reviewOrder.value = detail
    editItems.value = (detail.items || []).map((it) => ({ ...it }))
    reviewNote.value = ''
    reviewDrawer.value = true
  } catch (e) {
    ElMessage.error(errorMessage(e))
  }
}

async function submitReview(decision: 'APPROVED' | 'REJECTED'): Promise<void> {
  if (!reviewOrder.value) return

  if (decision === 'REJECTED') {
    try {
      await ElMessageBox.confirm('确认驳回该订单？', '提示', { type: 'warning' })
    } catch {
      return
    }
  }

  reviewing.value = true
  try {
    const items = editItems.value.map((it) => ({ id: it.id, qty: it.qty }))
    await adminApi.reviewOrder(reviewOrder.value.id, {
      status: decision,
      items,
      note: reviewNote.value || undefined,
    })
    ElMessage.success(decision === 'APPROVED' ? '已通过' : '已驳回')
    reviewDrawer.value = false
    await Promise.all([loadOrders(), loadStats()])
  } catch (e) {
    ElMessage.error(errorMessage(e))
  } finally {
    reviewing.value = false
  }
}

onMounted(() => {
  loadOrders()
  loadRecons()
  loadAudit()
  loadStats()
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
  flex-wrap: wrap;
  gap: 8px;
}
.card-title {
  font-size: 16px;
  font-weight: 600;
}
.recon-actions {
  display: flex;
  align-items: center;
}
.stat-label {
  color: #909399;
  font-size: 13px;
  margin-bottom: 8px;
}
.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
}
.review-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
