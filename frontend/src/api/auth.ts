import client from './client'
import type { LoginRequest, LoginResponse } from '@/types'

/** POST /api/auth/login — public. Returns JWT + role + username. */
export function login(data: LoginRequest): Promise<LoginResponse> {
  return client.post<LoginResponse>('/auth/login', data).then((r) => r.data)
}

/** GET /api/auth/me — current authenticated user. */
export function getMe(): Promise<unknown> {
  return client.get('/auth/me').then((r) => r.data)
}
