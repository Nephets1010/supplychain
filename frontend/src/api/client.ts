import axios from 'axios'

/**
 * Shared Axios instance for all backend calls.
 * - baseURL is the `/api` contract base path (proxied to :8080 in dev).
 * - A request interceptor injects `Authorization: Bearer <jwt>` from
 *   localStorage on every request.
 *
 * The 401 response handling (clear auth + redirect to /login) is registered
 * once in `main.ts` where the router is available.
 */
const TOKEN_KEY = 'sc_token'

const client = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
})

client.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers = config.headers ?? {}
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export default client
