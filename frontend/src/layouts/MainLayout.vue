<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">供应链协同</div>
      <el-menu :default-active="activeMenu" router class="menu">
        <el-menu-item v-for="m in menus" :key="m.path" :index="m.path">
          <el-icon><component :is="m.icon" /></el-icon>
          <span>{{ m.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="page-title">{{ currentTitle }}</div>
        <div class="user-area">
          <el-tag>{{ roleLabel }}</el-tag>
          <span class="username">{{ auth.username }}</span>
          <el-button size="small" type="primary" plain @click="handleLogout">
            退出登录
          </el-button>
        </div>
      </el-header>

      <el-main class="content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import type { Role } from '@/types'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

interface Menu {
  path: string
  title: string
  icon: string
  role: Role
}

const allMenus: Menu[] = [
  { path: '/merchant', title: '我的商品与订单', icon: 'Goods', role: 'MERCHANT' },
  { path: '/shop', title: '采购下单', icon: 'Shop', role: 'SHOPKEEPER' },
  { path: '/admin', title: '平台管理', icon: 'Setting', role: 'ADMIN' },
]

const menus = computed(() => allMenus.filter((m) => m.role === auth.role))
const activeMenu = computed(() => route.path)
const currentTitle = computed(
  () => menus.value.find((m) => m.path === route.path)?.title ?? '供应链协同',
)
const roleLabel = computed(() => {
  if (auth.role === 'MERCHANT') return '商家'
  if (auth.role === 'SHOPKEEPER') return '店家'
  if (auth.role === 'ADMIN') return '平台管理员'
  return ''
})

function handleLogout(): void {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout {
  height: 100%;
}

.aside {
  background: #001529;
  color: #fff;
}

.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  background: #000c17;
}

.menu {
  border-right: none;
  background: transparent;
}

.menu :deep(.el-menu-item) {
  color: rgba(255, 255, 255, 0.75);
}

.menu :deep(.el-menu-item.is-active) {
  color: #fff;
  background: #1890ff;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
}

.user-area {
  display: flex;
  align-items: center;
  gap: 12px;
}

.username {
  font-weight: 500;
}

.content {
  background: #f5f7fa;
}
</style>
