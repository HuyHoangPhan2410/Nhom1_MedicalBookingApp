<template>
  <div class="modal-backdrop" @click.self="$emit('close')">
    <div class="modal-content">
      <h3 class="modal-title">Đăng Ký Ca Khám Mới</h3>
      <p class="modal-subtitle">Tạo ca làm việc mới cho bệnh nhân đặt lịch khám</p>

      <form @submit.prevent="handleSubmit" class="form-box">
        <!-- 1. Chọn Ngày Khám -->
        <div class="form-group">
          <label>Ngày làm việc (không chọn ngày quá khứ):</label>
          <input 
            type="date" 
            v-model="workDate" 
            :min="todayDate" 
            required 
            class="form-control"
          />
        </div>

        <!-- 2. Chọn Khung Giờ Mẫu Hoặc Tự Nhập -->
        <div class="form-group">
          <label>Chọn khung giờ chuẩn:</label>
          <div class="preset-slots">
            <button 
              type="button" 
              v-for="p in presetSlots" 
              :key="p.label"
              :class="['preset-btn', { active: startTime === p.start && endTime === p.end }]"
              @click="selectPreset(p)"
            >
              {{ p.label }}
            </button>
          </div>
        </div>

        <div class="form-grid">
          <div class="form-group">
            <label>Giờ bắt đầu:</label>
            <input type="time" v-model="startTime" required class="form-control" />
          </div>
          <div class="form-group">
            <label>Giờ kết thúc:</label>
            <input type="time" v-model="endTime" required class="form-control" />
          </div>
        </div>

        <!-- 3. Số Bệnh Nhân Tối Đa -->
        <div class="form-group">
          <label>Số suất khám tối đa:</label>
          <input 
            type="number" 
            min="1" 
            max="30" 
            v-model.number="maxPatients" 
            required 
            class="form-control" 
          />
        </div>

        <div v-if="errorMsg" class="error-banner">{{ errorMsg }}</div>

        <div class="modal-actions">
          <button type="button" @click="$emit('close')" class="btn btn-outline">Hủy Bỏ</button>
          <button type="submit" class="btn btn-primary" :disabled="loading">
            {{ loading ? 'Đang tạo ca...' : 'Tạo Ca Khám Mới' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { useAuthStore } from '../stores/auth';
import { useContainer } from '../core/container';

const emit = defineEmits(['close']);
const { facade } = useContainer();
const authStore = useAuthStore();

const loading = ref(false);
const errorMsg = ref('');

const todayDate = computed(() => {
  const t = new Date();
  const year = t.getFullYear();
  const month = String(t.getMonth() + 1).padStart(2, '0');
  const day = String(t.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
});

const workDate = ref(todayDate.value);
const startTime = ref('08:00');
const endTime = ref('09:30');
const maxPatients = ref(5);

const presetSlots = [
  { label: 'Sáng: 08:00 - 09:30', start: '08:00', end: '09:30' },
  { label: 'Sáng: 09:30 - 11:00', start: '09:30', end: '11:00' },
  { label: 'Chiều: 13:30 - 15:00', start: '13:30', end: '15:00' },
  { label: 'Chiều: 15:00 - 16:30', start: '15:00', end: '16:30' }
];

const selectPreset = (p) => {
  startTime.value = p.start;
  endTime.value = p.end;
};

const handleSubmit = async () => {
  loading.value = true;
  errorMsg.value = '';

  try {
    // Builder (trong Facade) tự định dạng HH:mm:ss và kiểm tra hợp lệ
    await facade.createSchedule(authStore.user.userId, {
      workDate: workDate.value,
      startTime: startTime.value,
      endTime: endTime.value,
      maxPatients: maxPatients.value
    });
    emit('close');
  } catch (err) {
    errorMsg.value = err.message;
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.modal-title {
  font-size: 1.25rem;
  font-weight: 800;
  margin-bottom: 0.25rem;
}

.modal-subtitle {
  font-size: 0.9rem;
  color: var(--text-secondary);
  margin-bottom: 1.25rem;
}

.form-group {
  margin-bottom: 1.15rem;
}

.form-group label {
  display: block;
  font-weight: 600;
  font-size: 0.875rem;
  margin-bottom: 0.35rem;
}

.form-control {
  width: 100%;
  padding: 0.625rem 0.875rem;
  border: 1.5px solid var(--border-color);
  border-radius: var(--radius-md);
  font-family: inherit;
  font-size: 0.9rem;
  outline: none;
}

.preset-slots {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.5rem;
}

.preset-btn {
  padding: 0.5rem;
  font-size: 0.8rem;
  font-weight: 600;
  border: 1.5px solid var(--border-color);
  border-radius: var(--radius-md);
  background: #ffffff;
  cursor: pointer;
  transition: all 0.2s;
}

.preset-btn:hover, .preset-btn.active {
  border-color: var(--primary);
  background: var(--primary-light);
  color: var(--primary);
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.error-banner {
  background: var(--status-cancelled-bg);
  color: var(--status-cancelled-text);
  padding: 0.5rem 0.75rem;
  border-radius: var(--radius-sm);
  font-size: 0.85rem;
  margin-bottom: 1rem;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 1.5rem;
}
</style>
