<template>
  <div class="modal-backdrop" @click.self="$emit('close')">
    <div class="modal-content">
      <h3 class="modal-title">Đổi Lịch Khám Bệnh</h3>
      <p class="modal-subtitle">Chọn ca khám mới của bác sĩ {{ appointment.doctorName }}</p>

      <div class="schedule-list" v-if="schedules.length > 0">
        <div 
          v-for="s in schedules" 
          :key="s.id"
          :class="['schedule-item', { selected: selectedScheduleId === s.id, disabled: !s.isAvailable }]"
          @click="s.isAvailable && (selectedScheduleId = s.id)"
        >
          <div class="schedule-time">{{ s.workDate }} | {{ s.startTime }} - {{ s.endTime }}</div>
          <div class="schedule-slot">
            <span v-if="s.isAvailable" class="slot-available">Còn trống ({{ s.maxPatients - s.bookedPatients }} chỗ)</span>
            <span v-else class="slot-full">Đã hết chỗ</span>
          </div>
        </div>
      </div>
      <div v-else-if="loading" class="loading-state">Đang tải lịch làm việc...</div>
      <div v-else class="empty-state">Hiện không có ca khám nào khác khả dụng.</div>

      <div v-if="errorMsg" class="error-banner">{{ errorMsg }}</div>

      <div class="modal-actions">
        <button @click="$emit('close')" class="btn btn-outline">Hủy Bỏ</button>
        <button 
          @click="handleConfirm" 
          class="btn btn-primary" 
          :disabled="!selectedScheduleId || loading"
        >
          Xác Nhận Đổi Lịch
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useContainer } from '../core/container';

const props = defineProps({
  appointment: {
    type: Object,
    required: true
  }
});

const emit = defineEmits(['close']);
const { facade } = useContainer();

const schedules = ref([]);
const selectedScheduleId = ref(null);
const loading = ref(true);
const errorMsg = ref('');

onMounted(async () => {
  try {
    // Bỏ ca hiện tại
    schedules.value = await facade.listReschedulable(props.appointment);
  } catch (err) {
    errorMsg.value = err.message;
  } finally {
    loading.value = false;
  }
});

const handleConfirm = async () => {
  if (!selectedScheduleId.value) return;
  loading.value = true;
  errorMsg.value = '';
  try {
    await facade.rescheduleAppointment(props.appointment.id, selectedScheduleId.value);
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
  margin-bottom: 1.5rem;
}

.schedule-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  max-height: 250px;
  overflow-y: auto;
  margin-bottom: 1.5rem;
}

.schedule-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.875rem 1rem;
  border: 1.5px solid var(--border-color);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.2s;
}

.schedule-item:hover:not(.disabled), .schedule-item.selected {
  border-color: var(--primary);
  background: var(--primary-light);
}

.schedule-item.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.schedule-time {
  font-weight: 600;
  font-size: 0.9rem;
}

.slot-available {
  color: #059669;
  font-size: 0.8rem;
  font-weight: 600;
}

.slot-full {
  color: #dc2626;
  font-size: 0.8rem;
  font-weight: 600;
}

.loading-state, .empty-state {
  text-align: center;
  color: var(--text-secondary);
  padding: 1.5rem;
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
}
</style>
