<template>
  <div class="admin-login-page">
    <div class="admin-login-card">
      <div class="admin-logo">🛡️</div>
      <h1>Trang Quản Trị</h1>
      <p class="subtitle">Medical Booking System</p>
      <form @submit.prevent="handleLogin" class="login-form">
        <div class="form-group">
          <label>Email quản trị</label>
          <input type="email" v-model="email" required placeholder="admin@medbooking.com" class="form-control" />
        </div>
        <div class="form-group">
          <label>Mật khẩu</label>
          <input type="password" v-model="password" required placeholder="••••••••" class="form-control" />
        </div>
        <div v-if="errorMsg" class="error-banner">{{ errorMsg }}</div>
        <button type="submit" class="btn btn-primary btn-block" :disabled="loading">
          {{ loading ? 'Đang xác thực...' : 'Đăng Nhập Quản Trị' }}
        </button>
      </form>
      <router-link to="/" class="back-link">← Quay lại trang chủ</router-link>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useContainer } from '../../core/container';

const { adminApi, notifier } = useContainer();

const router = useRouter();
const email = ref('');
const password = ref('');
const loading = ref(false);
const errorMsg = ref('');

const handleLogin = async () => {
  loading.value = true;
  errorMsg.value = '';
  try {
    const res = await adminApi.login({
      email: email.value,
      password: password.value
    });
    if (res.status === 200) {
      localStorage.setItem('admin_token', res.data.token);
      localStorage.setItem('admin_user', JSON.stringify(res.data));
      router.push('/admin');
    }
  } catch (err) {
    errorMsg.value = err.message;
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.admin-login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%);
}
.admin-login-card {
  background: #fff;
  border-radius: 16px;
  padding: 3rem;
  width: 100%;
  max-width: 420px;
  text-align: center;
  box-shadow: 0 25px 50px rgba(0,0,0,0.25);
}
.admin-logo { font-size: 3rem; margin-bottom: 0.5rem; }
h1 { font-size: 1.5rem; font-weight: 800; margin-bottom: 0.25rem; }
.subtitle { color: #64748b; margin-bottom: 2rem; }
.form-group { margin-bottom: 1.25rem; text-align: left; }
.form-group label { display: block; font-weight: 600; font-size: 0.875rem; margin-bottom: 0.375rem; }
.form-control {
  width: 100%; padding: 0.75rem 1rem; border: 1.5px solid #e2e8f0;
  border-radius: 10px; font-size: 0.95rem; outline: none;
}
.form-control:focus { border-color: #0d9488; }
.btn-block { width: 100%; padding: 0.875rem; font-size: 1rem; margin-top: 0.5rem; }
.error-banner {
  background: #ffe4e6; color: #be123c; padding: 0.5rem 0.75rem;
  border-radius: 8px; font-size: 0.85rem; margin-bottom: 1rem;
}
.back-link { display: inline-block; margin-top: 1.5rem; color: #64748b; font-size: 0.9rem; text-decoration: none; }
.back-link:hover { color: #0d9488; }
</style>