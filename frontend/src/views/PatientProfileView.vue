<template>
  <div class="container profile-page">
    <!-- Patient Profile Header -->
    <div class="card profile-header-card" v-if="patient">
      <div class="patient-avatar">
        {{ patient.fullName.charAt(0) }}
      </div>
      <div class="patient-details">
        <h1 class="patient-name">{{ patient.fullName }}</h1>
        <div class="patient-meta">
          <span>Ngày sinh: <strong>{{ patient.dob }}</strong></span>
          <span>Giới tính: <strong>{{ formatGender(patient.gender) }}</strong></span>
          <span>SĐT: <strong>{{ patient.phone }}</strong></span>
          <span>Nhóm máu: <strong>{{ patient.bloodType || 'N/A' }}</strong></span>
        </div>
        <div class="patient-address" v-if="patient.address">
          Địa chỉ: {{ patient.address }}
        </div>
      </div>
    </div>

    <!-- Patient Appointments List -->
    <div class="appointments-section">
      <div class="section-header">
        <h2 class="section-title">Lịch Sử Khám Bệnh</h2>

        <!-- Status Filter Pills -->
        <div class="filter-pills">
          <button 
            :class="['filter-btn', { active: activeFilter === 'all' }]"
            @click="activeFilter = 'all'"
          >
            Tất Cả
          </button>
          <button 
            :class="['filter-btn', { active: activeFilter === 'pending' }]"
            @click="activeFilter = 'pending'"
          >
            Chờ Thanh Toán
          </button>
          <button 
            :class="['filter-btn', { active: activeFilter === 'confirmed' }]"
            @click="activeFilter = 'confirmed'"
          >
            Đã Xác Nhận
          </button>
        </div>
      </div>

      <div v-if="loading" class="loading-state">Đang tải lịch khám...</div>

      <div v-else-if="filteredAppointments.length > 0" class="appointments-grid">
        <AppointmentCard 
          v-for="app in filteredAppointments" 
          :key="app.id"
          :appointment="app"
          @pay="openPaymentModal"
          @reschedule="openRescheduleModal"
          @cancel="handleCancel"
        />
      </div>

      <div v-else class="empty-state card">
        Bạn chưa có lịch hẹn khám nào trong danh sách.
      </div>
    </div>

    <!-- Payment Modal -->
    <PaymentModal 
      v-if="showPaymentModal" 
      :appointment="selectedAppointment"
      @close="showPaymentModal = false"
    />

    <!-- Reschedule Modal -->
    <RescheduleModal 
      v-if="showRescheduleModal" 
      :appointment="selectedAppointment"
      @close="showRescheduleModal = false"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useAuthStore } from '../stores/auth';
import { useContainer } from '../core/container';
import { EVENTS } from '../patterns/behavioral/EventBus';
import AppointmentCard from '../components/AppointmentCard.vue';
import PaymentModal from '../components/PaymentModal.vue';
import RescheduleModal from '../components/RescheduleModal.vue';

const { facade, bus, notifier } = useContainer();
const authStore = useAuthStore();
const patient = ref(null);
const appointments = ref([]);
const activeFilter = ref('all');
const loading = ref(true);

const showPaymentModal = ref(false);
const showRescheduleModal = ref(false);
const selectedAppointment = ref(null);

const fetchData = async () => {
  if (!authStore.user?.userId) return;
  loading.value = true;
  try {
    const overview = await facade.loadPatientOverview(authStore.user.userId);
    patient.value = overview.patient;
    appointments.value = overview.appointments;
  } catch (err) {
    console.error(err);
  } finally {
    loading.value = false;
  }
};

onMounted(fetchData);

// Observer: tự tải lại danh sách khi có đặt/hủy/đổi lịch hoặc thanh toán (không cần modal emit lên).
const unsubscribers = [
  EVENTS.APPOINTMENT_BOOKED, EVENTS.APPOINTMENT_CANCELLED,
  EVENTS.APPOINTMENT_RESCHEDULED, EVENTS.PAYMENT_COMPLETED
].map((event) => bus.subscribe(event, fetchData));
onUnmounted(() => unsubscribers.forEach((off) => off()));

const filteredAppointments = computed(() => {
  if (activeFilter.value === 'all') return appointments.value;
  return appointments.value.filter(a => a.status === activeFilter.value);
});

const openPaymentModal = (app) => {
  selectedAppointment.value = app;
  showPaymentModal.value = true;
};

const openRescheduleModal = (app) => {
  selectedAppointment.value = app;
  showRescheduleModal.value = true;
};

const handleCancel = async (appointmentId) => {
  if (!notifier.confirm('Bạn có chắc chắn muốn hủy lịch hẹn này?')) return;
  try {
    await facade.cancelAppointment(appointmentId);
  } catch (err) {
    notifier.error(err.message);
  }
};

const formatGender = (gender) => {
  if (gender === 'male') return 'Nam';
  if (gender === 'female') return 'Nữ';
  return 'Khác';
};
</script>

<style scoped>
.profile-page {
  padding-top: 3rem;
  padding-bottom: 5rem;
  display: flex;
  flex-direction: column;
  gap: 2.5rem;
}

.profile-header-card {
  display: flex;
  align-items: center;
  gap: 2rem;
}

.patient-avatar {
  width: 90px;
  height: 90px;
  border-radius: 50%;
  background: var(--primary-light);
  color: var(--primary);
  font-size: 2.5rem;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.patient-name {
  font-size: 1.5rem;
  font-weight: 800;
  margin-bottom: 0.5rem;
}

.patient-meta {
  display: flex;
  gap: 1.5rem;
  font-size: 0.9rem;
  color: var(--text-secondary);
  flex-wrap: wrap;
}

.patient-address {
  font-size: 0.9rem;
  color: var(--text-secondary);
  margin-top: 0.5rem;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.section-title {
  font-size: 1.35rem;
  font-weight: 800;
}

.filter-pills {
  display: flex;
  gap: 0.5rem;
}

.filter-btn {
  padding: 0.375rem 0.875rem;
  font-size: 0.85rem;
  font-weight: 600;
  border-radius: 9999px;
  border: 1px solid var(--border-color);
  background: #ffffff;
  cursor: pointer;
  transition: all 0.2s;
}

.filter-btn:hover, .filter-btn.active {
  background: var(--primary);
  color: #ffffff;
  border-color: var(--primary);
}

.appointments-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 1.5rem;
}

.loading-state, .empty-state {
  text-align: center;
  padding: 3rem;
  color: var(--text-secondary);
}
</style>
