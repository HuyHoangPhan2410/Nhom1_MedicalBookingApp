<template>
  <div class="card appointment-card">
    <div class="card-header">
      <div class="appointment-id">Mã đơn: #{{ appointment.id }}</div>
      <div :class="['badge', getStatusClass(appointment.status)]">
        {{ getStatusText(appointment.status) }}
      </div>
    </div>

    <div class="card-body">
      <div class="info-row">
        <span class="label">Bác sĩ:</span>
        <span class="value"><strong>{{ appointment.doctorName }}</strong> ({{ appointment.specialtyName }})</span>
      </div>
      <div class="info-row">
        <span class="label">Thời gian:</span>
        <span class="value">{{ appointment.workDate }} | {{ appointment.startTime }} - {{ appointment.endTime }}</span>
      </div>
      <div class="info-row" v-if="appointment.symptoms">
        <span class="label">Triệu chứng:</span>
        <span class="value">{{ appointment.symptoms }}</span>
      </div>
    </div>

    <p v-if="appointment.cancelBlockedReason" class="state-note">{{ appointment.cancelBlockedReason }}</p>
    <div class="card-actions" v-if="appointment.status === 'pending' || appointment.canCancel || appointment.canReschedule">
      <button 
        v-if="appointment.status === 'pending'" 
        @click="$emit('pay', appointment)" 
        class="btn btn-primary btn-sm"
      >
        Thanh Toán Ngay
      </button>

      <button 
        v-if="appointment.canReschedule"
        @click="$emit('reschedule', appointment)" 
        class="btn btn-outline btn-sm"
      >
        Đổi Lịch
      </button>

      <button 
        v-if="appointment.canCancel"
        @click="$emit('cancel', appointment.id)" 
        class="btn btn-danger btn-sm"
      >
        Xem Trước Hủy
      </button>
    </div>
  </div>
</template>

<script setup>
defineProps({
  appointment: {
    type: Object,
    required: true
  }
});

defineEmits(['pay', 'reschedule', 'cancel']);

const getStatusClass = (status) => {
  switch (status) {
    case 'pending': return 'badge-pending';
    case 'confirmed': return 'badge-confirmed';
    case 'cancelled': return 'badge-cancelled';
    case 'completed': return 'badge-completed';
    default: return '';
  }
};

const getStatusText = (status) => {
  switch (status) {
    case 'pending': return 'Chờ thanh toán';
    case 'confirmed': return 'Đã xác nhận';
    case 'cancelled': return 'Đã hủy';
    case 'completed': return 'Đã hoàn thành';
    default: return status;
  }
};
</script>

<style scoped>
.appointment-card {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--border-color);
  padding-bottom: 0.75rem;
}

.appointment-id {
  font-weight: 700;
  color: var(--text-secondary);
}

.info-row {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
  font-size: 0.95rem;
}

.info-row .label {
  color: var(--text-secondary);
  width: 100px;
  flex-shrink: 0;
}

.state-note { color: var(--text-secondary); font-size: .85rem; }

.card-actions {
  flex-wrap: wrap;
  display: flex;
  gap: 0.5rem;
  border-top: 1px solid var(--border-color);
  padding-top: 0.75rem;
}
</style>
