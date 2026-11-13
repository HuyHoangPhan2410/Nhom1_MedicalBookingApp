<template>
  <div class="modal-backdrop" @click.self="$emit('close')">
    <div class="modal-content">
      <h3 class="modal-title">Xác Nhận Thanh Toán</h3>
      <p class="modal-subtitle">Đơn khám #{{ appointment.id }} — {{ formatPrice(appointment.consultationFee || 0) }} đ</p>

      <div class="payment-methods">
        <!-- ✅ Tiền mặt -->
        <label :class="['method-card', { active: selectedMethod === 'cash' }]">
          <input type="radio" value="cash" v-model="selectedMethod" />
          <div class="method-info">
            <strong>💵 Tiền Mặt</strong>
            <span>Thanh toán trực tiếp tại viện</span>
          </div>
        </label>

        <!-- ✅ Thẻ qua Stripe -->
        <label :class="['method-card', { active: selectedMethod === 'card' }]">
          <input type="radio" value="card" v-model="selectedMethod" />
          <div class="method-info">
            <strong>💳 Thẻ Tín Dụng / Ghi Nợ</strong>
            <span>Thanh toán an toàn qua Stripe</span>
          </div>
        </label>
      </div>

      <!-- ✅ Stripe Card Element -->
      <div v-if="selectedMethod === 'card'" class="stripe-section">
        <div ref="cardElementRef" class="card-element-box"></div>
        <p class="stripe-hint">Nhập thông tin thẻ. Dùng thẻ test: 4242 4242 4242 4242</p>
      </div>

      <div v-if="errorMsg" class="error-banner">{{ errorMsg }}</div>
      <div v-if="successMsg" class="success-banner">{{ successMsg }}</div>

      <div class="modal-actions">
        <button @click="$emit('close')" class="btn btn-outline">Hủy Bỏ</button>
        <button @click="handleConfirm" class="btn btn-primary" :disabled="loading">
          {{ loading ? 'Đang xử lý...' : 'Xác Nhận Thanh Toán' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue';
import { useContainer } from '../core/container';

const props = defineProps({
  appointment: { type: Object, required: true }
});
const emit = defineEmits(['close']);
const { facade } = useContainer();

const selectedMethod = ref('cash');
const loading = ref(false);
const errorMsg = ref('');
const successMsg = ref('');
const cardElementRef = ref(null);

// Strategy + Adapter: modal không biết Stripe; chỉ hỏi Facade chuẩn bị phương thức đã chọn.
watch(selectedMethod, async (method) => {
  errorMsg.value = '';
  const state = await facade.preparePaymentMethod(method);
  if (!state.ready) {
    errorMsg.value = state.message;
    return;
  }
  if (method === 'card') {
    await nextTick();
    facade.mountCardInput(cardElementRef.value);
  }
});

const handleConfirm = async () => {
  loading.value = true;
  errorMsg.value = '';
  successMsg.value = '';

  try {
    const result = await facade.pay(props.appointment.id, selectedMethod.value);
    successMsg.value = result.message;
    setTimeout(() => emit('close'), 1500);
  } catch (err) {
    errorMsg.value = err.message;
  } finally {
    loading.value = false;
  }
};

const formatPrice = (p) => p ? new Intl.NumberFormat('vi-VN').format(p) : '0';
</script>

<style scoped>
.modal-title { font-size: 1.25rem; font-weight: 800; margin-bottom: 0.25rem; }
.modal-subtitle { font-size: 0.9rem; color: var(--text-secondary); margin-bottom: 1.5rem; }
.payment-methods { display: flex; flex-direction: column; gap: 0.75rem; margin-bottom: 1.5rem; }
.method-card {
  display: flex; align-items: center; gap: 1rem; padding: 1rem;
  border: 1.5px solid var(--border-color); border-radius: var(--radius-md);
  cursor: pointer; transition: all 0.2s;
}
.method-card:hover, .method-card.active { border-color: var(--primary); background: var(--primary-light); }
.method-info { display: flex; flex-direction: column; }
.method-info span { font-size: 0.8rem; color: var(--text-secondary); }
.stripe-section { margin-bottom: 1.5rem; }
.card-element-box {
  padding: 0.875rem 1rem; border: 1.5px solid var(--border-color);
  border-radius: var(--radius-md); background: #fff; min-height: 44px;
}
.stripe-hint { font-size: 0.75rem; color: #94a3b8; margin-top: 0.5rem; }
.error-banner { background: var(--status-cancelled-bg); color: var(--status-cancelled-text); padding: 0.5rem 0.75rem; border-radius: var(--radius-sm); font-size: 0.85rem; margin-bottom: 1rem; }
.success-banner { background: var(--status-confirmed-bg); color: var(--status-confirmed-text); padding: 0.5rem 0.75rem; border-radius: var(--radius-sm); font-size: 0.85rem; margin-bottom: 1rem; }
.modal-actions { display: flex; justify-content: flex-end; gap: 0.75rem; }
</style>