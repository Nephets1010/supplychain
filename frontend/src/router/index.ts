import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw, RouteMeta } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import type { Role } from '@/types'
import MainLayout from '@/layouts/MainLayout.vue'
import Login from '@/views/Login.vue'

declare module 'vue-router' {
  interface RouteMeta {
    public?: boolean
    requiresAuth?: boolean
    role?: Role
  }
}

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: Login,
    meta: { public: true },
  },
  {
    path: '/',
    component: MainLayout,
    meta: { requiresAuth: true },
    children: [
      {
        path: 'merchant',
        name: 'merchant',
        component: () => import('@/views/Merchant.vue'),
        meta: { role: 'MERCHANT' },
      },
      {
        path: 'shop',
        name: 'shop',
        component: () => import('@/views/Shopkeeper.vue'),
        meta: { role: 'SHOPKEEPER' },
      },
      {
        path: 'admin',
        name: 'admin',
        component: () => import('@/views/Admin.vue'),
        meta: { role: 'ADMIN' },
      },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/login' },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

function defaultPathFor(role: Role | ''): string {
  if (role === 'MERCHANT') return '/merchant'
  if (role === 'SHOPKEEPER') return '/shop'
  if (role === 'ADMIN') return '/admin'
  return '/login'
}

router.beforeEach((to) => {
  const auth = useAuthStore()

  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return { path: '/login' }
  }

  // Already authenticated users hitting /login are sent to their home.
  if (to.meta.public && auth.isAuthenticated && to.path === '/login') {
    return { path: defaultPathFor(auth.role) }
  }

  // Role-scoped pages: bounce users to their own home if they stray.
  if (to.meta.role && auth.role !== to.meta.role) {
    return { path: defaultPathFor(auth.role) }
  }

  return true
})

export default router
