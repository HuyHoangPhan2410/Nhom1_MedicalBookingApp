<template>
  <div class="container doctor-detail-page" v-if="doctor">
    <!-- Doctor Profile Header -->
    <div class="card doctor-header-card">
      <div class="doctor-avatar-lg">
        {{ doctor.fullName.charAt(0) }}
      </div>
      <div class="doctor-main-info">
        <span class="specialty-badge">{{ doctor.specialtyName }}</span>
        <h1 class="doctor-title">{{ doctor.fullName }}</h1>
        <p class="doctor-exp">Kinh nghiệm chuyên khoa: <strong>{{ doctor.experienceYears }} năm</strong></p>
        <p class="doctor-fee">Giá khám cố định: <span>{{ formatPrice(doctor.consultationFee) }} đ</span></p>
        <p class="doctor-bio">{{ doctor.bio }}</p>
      </div>
    </div>

    <!-- Doctor Schedule Picker Section (Step 1: Date, Step 2: Time) -->
    <div class="card schedule-picker-card">
      <h2 class="card-section-title">Chọn Ngày & Khung Giờ Khám Bệnh</h2>

      <!-- BƯỚC 1: CHỌN NGÀY KHÁM -->
      <div class="step-box">
        <label class="step-label">Bước 1: Chọn Ngày Khám (Không chọn ngày quá khứ)</label>
        <div class="date-input-wrapper">
          <input 
            type="date" 
            v-model="selectedDate" 
            :min="todayDate" 
            @change="fetchSchedulesForDate" 
            class="date-picker-input"
          />
          <span class="date-hint">Ngày đã chọn: <strong>{{ formatDateDisplay(selectedDate) }}</strong></span>
        </div>
      </div>

      <!-- BƯỚC 2: CHỌN KHUNG GIỜ KHÁM -->
      <div class="step-box" style="margin-top: 1.5rem;">
        <label class="step-label">Bước 2: Chọn Khung Giờ Khám</label>

        <div v-if="loadingSchedules" class="loading-state">Đang tải lịch làm việc...</div>

        <div v-else-if="schedules.length > 0" class="time-slots-grid">
          <div 
            v-for="s in schedules" 
            :key="s.id"
            :class="['time-slot-card', { selected: selectedSchedule?.id === s.id, disabled: !s.isAvailable }]"
            @click="s.isAvailable && selectSchedule(s)"
          >
            <div class="slot-time">{{ s.startTime }} - {{ s.endTime }}</div>
            <div class="slot-status">
              <span v-if="s.isAvailable" class="text-available">Còn trống ({{ s.maxPatients - s.bookedPatients }} suất)</span>
              <span v-else class="text-full">Đã hết chỗ</span>
            </div>
          </div>
        </div>

        <div v-else class="empty-schedules">
          Bác sĩ không có ca khám nào vào ngày <strong>{{ formatDateDisplay(selectedDate) }}</strong>. Vui lòng chọn ngày khác.
        </div>
      </div>

      <!-- Form Đặt Khám Section -->
      <div v-if="selectedSchedule" class="booking-form-box">
        <h3 class="form-box-title">Xác Nhận Thông Tin Đặt Khám</h3>
        
        <div class="selected-summary">
          Ca khám đã chọn: <strong>{{ selectedSchedule.workDate }} ({{ selectedSchedule.startTime }} - {{ selectedSchedule.endTime }})</strong>
        </div>

        <div class="form-group">
          <label>Mô tả triệu chứng bệnh (không bắt buộc):</label>
          <textarea 
            v-model="symptoms" 
            placeholder="Nhập lý do khám hoặc triệu chứng bệnh của bạn..." 
            rows="3" 
            class="form-control"
          ></textarea>
        </div>

        <div v-if="errorMsg" class="error-banner">{{ errorMsg }}</div>

        <button @click="handleBook" class="btn btn-primary btn-lg" :disabled="submitting">
          {{ submitting ? 'Đang xử lý...' : 'Xác Nhận Đặt Lịch' }}
        </button>
      </div>
    </div>
  </div>
  <div v-else-if="loading" class="container loading-view">Đang tải hồ sơ bác sĩ...</div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import { useContainer } from '../core/container';

const { facade } = useContainer();
const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const doctor = ref(null);
const schedules = ref([]);
const selectedSchedule = ref(null);
const symptoms = ref('');

const loading = ref(true);
const loadingSchedules = ref(false);
const submitting = ref(false);
const errorMsg = ref('');

// Tính ngày hôm nay YYYY-MM-DD làm giá trị tối thiểu cho min DatePicker
const todayDate = computed(() => {
  const t = new Date();
  const year = t.getFullYear();
  const month = String(t.getMonth() + 1).padStart(2, '0');
  const day = String(t.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
});

const selectedDate = ref(todayDate.value);

const fetchSchedulesForDate = async () => {
  if (!doctor.value) return;
  loadingSchedules.value = true;
  selectedSchedule.value = null;
  try {
    schedules.value = await facade.listSchedules(doctor.value.userId, selectedDate.value);
  } catch (err) {
    console.error(err);
  } finally {
    loadingSchedules.value = false;
  }
};

onMounted(async () => {
  try {
    doctor.value = await facade.getDoctor(route.params.id);
    await fetchSchedulesForDate();
  } catch (err) {
    console.error(err);
  } finally {
    loading.value = false;
  }
});

const selectSchedule = (sched) => {
  selectedSchedule.value = sched;
};

const handleBook = async () => {
  if (!authStore.isAuthenticated) {
    router.push('/auth');
    return;
  }

  submitting.value = true;
  errorMsg.value = '';

  try {
    await facade.bookAppointment({
      patientId: authStore.user.userId,
      doctorId: doctor.value.userId,
      scheduleId: selectedSchedule.value?.id,
      symptoms: symptoms.value
    });
    router.push('/profile');
  } catch (err) {
    errorMsg.value = err.message;
  } finally {
    submitting.value = false;
  }
};

const formatDateDisplay = (dateStr) => {
  if (!dateStr) return '';
  const parts = dateStr.split('-');
  if (parts.length === 3) {
    return `${parts[2]}/${parts[1]}/${parts[0]}`;
  }
  return dateStr;
};

const formatPrice = (price) => {
  if (!price) return '0';
  return new Intl.NumberFormat('vi-VN').format(price);
};
</script>

<style scoped>
.doctor-detail-page {
  padding-top: 3rem;
  padding-bottom: 5rem;
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.doctor-header-card {
  display: flex;
  gap: 2rem;
  align-items: flex-start;
}

.doctor-avatar-lg {
  width: 110px;
  height: 110px;
  border-radius: 50%;
  background: var(--primary-light);
  color: var(--primary);
  font-size: 3.2rem;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.doctor-title {
  font-size: 1.65rem;
  font-weight: 800;
  margin-bottom: 0.5rem;
}

.specialty-badge {
  display: inline-block;
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--primary);
  background: var(--primary-light);
  padding: 0.25rem 0.75rem;
  border-radius: 9999px;
  margin-bottom: 0.5rem;
}

.doctor-fee span {
  font-size: 1.2rem;
  font-weight: 800;
  color: var(--primary);
}

.doctor-bio {
  margin-top: 0.75rem;
  color: var(--text-secondary);
}

.card-section-title {
  font-size: 1.35rem;
  font-weight: 800;
  margin-bottom: 1.5rem;
}

.step-label {
  display: block;
  font-weight: 700;
  font-size: 1rem;
  margin-bottom: 0.75rem;
  color: var(--text-primary);
}

.date-input-wrapper {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.date-picker-input {
  padding: 0.75rem 1rem;
  font-size: 1rem;
  font-family: inherit;
  border: 1.5px solid var(--border-color);
  border-radius: var(--radius-md);
  outline: none;
  background: #ffffff;
  cursor: pointer;
}

.date-picker-input:focus {
  border-color: var(--primary);
}

.date-hint {
  font-size: 0.95rem;
  color: var(--text-secondary);
}

.time-slots-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 1rem;
}

.time-slot-card {
  padding: 1rem;
  border: 1.5px solid var(--border-color);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.2s;
  text-align: center;
}

.time-slot-card:hover:not(.disabled), .time-slot-card.selected {
  border-color: var(--primary);
  background: var(--primary-light);
}

.time-slot-card.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.slot-time {
  font-size: 1.1rem;
  font-weight: 700;
  margin-bottom: 0.35rem;
}

.text-available {
  color: #059669;
  font-size: 0.8rem;
  font-weight: 600;
}

.text-full {
  color: #dc2626;
  font-size: 0.8rem;
  font-weight: 600;
}

.booking-form-box {
  margin-top: 2rem;
  padding-top: 2rem;
  border-top: 1px solid var(--border-color);
}

.form-box-title {
  font-size: 1.15rem;
  font-weight: 700;
  margin-bottom: 1rem;
}

.selected-summary {
  background: #f1f5f9;
  padding: 0.875rem 1rem;
  border-radius: var(--radius-md);
  margin-bottom: 1.25rem;
  font-size: 0.95rem;
}

.form-group {
  margin-bottom: 1.25rem;
}

.form-group label {
  display: block;
  font-weight: 600;
  margin-bottom: 0.5rem;
  font-size: 0.9rem;
}

.form-control {
  width: 100%;
  padding: 0.75rem 1rem;
  border: 1.5px solid var(--border-color);
  border-radius: var(--radius-md);
  font-family: inherit;
  font-size: 0.95rem;
  outline: none;
}

.btn-lg {
  padding: 0.875rem 2rem;
  font-size: 1rem;
}

.error-banner {
  background: var(--status-cancelled-bg);
  color: var(--status-cancelled-text);
  padding: 0.5rem 0.75rem;
  border-radius: var(--radius-sm);
  font-size: 0.85rem;
  margin-bottom: 1rem;
}

.empty-schedules, .loading-state {
  color: var(--text-secondary);
  padding: 1rem 0;
}

.loading-view {
  padding: 5rem 0;
  text-align: center;
  color: var(--text-secondary);
}
</style>
