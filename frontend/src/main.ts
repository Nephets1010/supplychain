import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'
import client from './api/client'
import { useAuthStore } from './stores/auth'
import './styles/main.css'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)
app.use(ElementPlus)

// Register all Element Plus icons globally so views can reference them by name.
for (const [name, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(name, component)
}

// Global 401 handling: clear the auth store and bounce to the login page.
client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      const auth = useAuthStore()
      auth.logout()
      router.push('/login').catch(() => {
        /* navigation already in progress */
      })
    }
    return Promise.reject(error)
  },
)

app.mount('#app')
