import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Role } from '@/types'
import { login as apiLogin } from '@/api/auth'

const TOKEN_KEY = 'sc_token'
const USER_KEY = 'sc_user'

interface PersistedUser {
  role: Role
  username: string
}

function readPersisted(): PersistedUser | null {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? (JSON.parse(raw) as PersistedUser) : null
  } catch {
    return null
  }
}

/**
 * Pinia auth store: holds the JWT, role and username, and keeps them
 * persisted to localStorage so a refresh keeps the user logged in.
 */
export const useAuthStore = defineStore('auth', () => {
  const initial = readPersisted()
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) || '')
  const role = ref<Role | ''>(initial?.role ?? '')
  const username = ref<string>(initial?.username ?? '')

  const isAuthenticated = computed(() => !!token.value)

  function persist(): void {
    if (token.value) {
      localStorage.setItem(TOKEN_KEY, token.value)
      localStorage.setItem(
        USER_KEY,
        JSON.stringify({ role: role.value, username: username.value }),
      )
    } else {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    }
  }

  async function login(usernameVal: string, password: string): Promise<Role> {
    const res = await apiLogin({ username: usernameVal, password })
    token.value = res.token
    role.value = res.role
    username.value = res.username
    persist()
    return res.role
  }

  function logout(): void {
    token.value = ''
    role.value = ''
    username.value = ''
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  return { token, role, username, isAuthenticated, login, logout }
})
