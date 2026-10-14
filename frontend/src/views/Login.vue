<template>
  <div class="login-page">
    <el-card class="login-card">
      <h2 class="title">供应链协同系统</h2>
      <el-form :model="form" label-position="top" @submit.prevent="onSubmit">
        <el-form-item label="用户名">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            clearable
          />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="请输入密码"
            @keyup.enter="onSubmit"
          />
        </el-form-item>
        <el-button
          type="primary"
          class="submit"
          :loading="loading"
          @click="onSubmit"
        >
          登录
        </el-button>
      </el-form>
      <div class="hint">
        演示账号：admin / admin123 · merchant1 / merch123 · shop1 / shop123
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const form = reactive({ username: '', password: '' })
const loading = ref(false)

function errorMessage(e: unknown): string {
  const err = e as { response?: { data?: { message?: string; error?: string } } }
  return (
    err?.response?.data?.message ||
    err?.response?.data?.error ||
    '登录失败'
  )
}

async function onSubmit(): Promise<void> {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const role = await auth.login(form.username, form.password)
    ElMessage.success('登录成功')
    const target =
      role === 'MERCHANT'
        ? '/merchant'
        : role === 'SHOPKEEPER'
          ? '/shop'
          : '/admin'
    router.push(target)
  } catch (e) {
    ElMessage.error(errorMessage(e))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1890ff 0%, #001529 100%);
}

.login-card {
  width: 380px;
  padding: 12px 8px;
}

.title {
  text-align: center;
  margin: 8px 0 24px;
  font-size: 22px;
  color: #303133;
}

.submit {
  width: 100%;
}

.hint {
  margin-top: 16px;
  font-size: 12px;
  color: #909399;
  text-align: center;
  line-height: 1.6;
}
</style>
