<template>
  <div class="container doctor-dashboard">
    <!-- Header Thông tin Bác sĩ -->
    <div class="card doctor-header-card" v-if="doctor">
      <div class="doctor-avatar">
        {{ doctor.fullName.charAt(0) }}
      </div>
      <div class="doctor-details">
        <span class="specialty-badge">{{ doctor.specialtyName }}</span>
        <h1 class="doctor-name">{{ doctor.fullName }}</h1>
        <div class="meta-row">
          <span>Kinh nghiệm: <strong>{{ doctor.experienceYears }} năm</strong></span>
          <span>Giá khám: <strong>{{ formatPrice(doctor.consultationFee) }} đ / ca</strong></span>
        </div>
      </div>
      <div class="header-action">
        <button @click="showCreateModal = true" class="btn btn-primary">
          + Đăng Ký Ca Khám Mới
        </button>
      </div>
    </div>

    <!-- 1. THỜI KHÓA BIỂU LỊCH KHÁM TUẦN NÀY -->
    <section class="section-container">
      <h2 class="section-title">📅 Thời Khóa Biểu Lịch Khám Tuần Này</h2>
      
      <div class="weekly-calendar">
        <div 
          v-for="day in weekDays" 
          :key="day.dateStr" 
          :class="['calendar-day', { 'is-today': day.dateStr === todayIso }]"
        >
          <div class="day-header">
            <span class="day-name">{{ day.label }}</span>
            <span class="day-date">{{ day.dateNum }}</span>
          </div>

          <div class="day-content">
            <template v-if="day.appointments.length > 0">
              <div 
                v-for="app in day.appointments" 
                :key="app.id" 
                :class="['calendar-card', app.status]"
              >
                <div class="sch-time">{{ app.startTime }} - {{ app.endTime }}</div>
                <div class="sch-patient">Bệnh nhân: <strong>{{ app.patientName }}</strong></div>
                <div class="sch-symptoms" v-if="app.symptoms"><em>{{ app.symptoms }}</em></div>
                <div class="sch-status">
                  <span :class="['badge', getStatusClass(app.status)]">{{ getStatusText(app.status) }}</span>
                </div>
              </div>
            </template>
            <div v-else class="empty-day">Trống lịch</div>
          </div>
        </div>
      </div>
    </section>

    <!-- 2. QUẢN LÝ TẤT CẢ CA LÀM VIỆC (FULL CRUD SCHEDULES) -->
    <section class="section-container">
      <div class="section-header-row">
        <h2 class="section-title">⏰ Quản Lý Các Ca Làm Việc Của Tôi ({{ schedules.length }})</h2>
      </div>

      <div v-if="schedules.length > 0" class="schedules-grid">
        <div v-for="s in schedules" :key="s.id" class="card schedule-card">
          <div class="sched-date">Ngày: <strong>{{ s.workDate }}</strong></div>
          <div class="sched-time">Giờ: <strong>{{ s.startTime }} - {{ s.endTime }}</strong></div>
          <div class="sched-capacity">
            Số suất: <strong>{{ s.bookedPatients }} / {{ s.maxPatients }}</strong> bệnh nhân
          </div>

          <div class="sched-actions">
            <button @click="openEditModal(s)" class="btn btn-outline btn-sm">
              Sửa Ca
            </button>
            <button @click="handleCancelSchedule(s.id)" class="btn btn-danger btn-sm">
              Hủy Ca
            </button>
          </div>
        </div>
      </div>
      <div v-else class="empty-state card">
        Chưa có ca làm việc nào được đăng ký. Bấm nút "+ Đăng Ký Ca Khám Mới" để tạo ca làm việc.
      </div>
    </section>

    <!-- 3. TỔNG QUAN CA KHÁM & BỆNH NHÂN -->
    <section class="section-container">
      <h2 class="section-title">📊 Tổng Quan Lịch Khám</h2>
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-icon">📅</div>
          <div class="stat-info">
            <h3>Số ca khám tuần này</h3>
            <p class="stat-number">{{ appointmentsThisWeek }}</p>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">👥</div>
          <div class="stat-info">
            <h3>Số bệnh nhân phục vụ</h3>
            <p class="stat-number">{{ uniquePatientsThisWeek }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- 4. BÁO CÁO DOANH THU KHÁM BỆNH -->
    <section class="section-container">
      <h2 class="section-title">💵 Báo Cáo Phí Khám Dự Kiến ({{ formatPrice(doctor?.consultationFee) }}đ / Ca)</h2>
      <div class="stats-grid">
        <div class="stat-card outline-teal">
          <div class="stat-icon">💵</div>
          <div class="stat-info">
            <h3>Hôm nay</h3>
            <p class="stat-number">{{ formatPrice(revenueToday) }} đ</p>
          </div>
        </div>
        <div class="stat-card outline-teal">
          <div class="stat-icon">💶</div>
          <div class="stat-info">
            <h3>Tuần này</h3>
            <p class="stat-number">{{ formatPrice(revenueThisWeek) }} đ</p>
          </div>
        </div>
        <div class="stat-card outline-teal">
          <div class="stat-icon">💎</div>
          <div class="stat-info">
            <h3>Tháng này</h3>
            <p class="stat-number">{{ formatPrice(revenueThisMonth) }} đ</p>
          </div>
        </div>
      </div>
    </section>

    <!-- Modal Đăng Ký Ca Khám Mới -->
    <CreateScheduleModal 
      v-if="showCreateModal" 
      @close="showCreateModal = false" 
    />

    <!-- Modal Chỉnh Sửa Ca Khám -->
    <EditScheduleModal 
      v-if="selectedScheduleToEdit" 
      :schedule="selectedScheduleToEdit"
      @close="selectedScheduleToEdit = null" 
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useAuthStore } from '../stores/auth';
import { useContainer } from '../core/container';
import { EVENTS } from '../patterns/behavioral/EventBus';
import CreateScheduleModal from '../components/CreateScheduleModal.vue';
import EditScheduleModal from '../components/EditScheduleModal.vue';

const { facade, bus, notifier } = useContainer();
const authStore = useAuthStore();

const doctor = ref(null);
const appointments = ref([]);
const schedules = ref([]);
const loading = ref(true);
const showCreateModal = ref(false);
const selectedScheduleToEdit = ref(null);

const fetchData = async () => {
  if (!authStore.user?.userId) return;
  const doctorId = authStore.user.userId;
  try {
    const portal = await facade.loadDoctorPortal(doctorId);
    doctor.value = portal.doctor;
    appointments.value = portal.appointments;
    schedules.value = portal.schedules;
  } catch (err) {
    console.error(err);
  } finally {
    loading.value = false;
  }
};

onMounted(fetchData);

// Observer: tạo/sửa/hủy ca khám xong thì bảng tuần tự cập nhật.
const offScheduleChanged = bus.subscribe(EVENTS.SCHEDULE_CHANGED, fetchData);
onUnmounted(offScheduleChanged);

const openEditModal = (sched) => {
  selectedScheduleToEdit.value = sched;
};

const handleCancelSchedule = async (scheduleId) => {
  if (!notifier.confirm('Bạn có chắc chắn muốn hủy ca khám này không? Các lịch khám chưa thanh toán sẽ bị hủy.')) return;
  try {
    await facade.cancelSchedule(scheduleId, authStore.user.userId);
  } catch (err) {
    notifier.error(err.message);
  }
};

// Tính ngày hôm nay YYYY-MM-DD
const todayIso = computed(() => {
  const t = new Date();
  const year = t.getFullYear();
  const month = String(t.getMonth() + 1).padStart(2, '0');
  const day = String(t.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
});

// Tính toán 7 ngày trong tuần hiện tại (Thứ 2 -> Chủ Nhật)
const weekDays = computed(() => {
  const today = new Date();
  const distanceToMonday = today.getDay() === 0 ? 6 : today.getDay() - 1;
  const monday = new Date(today);
  monday.setDate(today.getDate() - distanceToMonday);
  monday.setHours(0, 0, 0, 0);

  const days = [];
  for (let i = 0; i < 7; i++) {
    const d = new Date(monday);
    d.setDate(monday.getDate() + i);

    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const dayNum = String(d.getDate()).padStart(2, '0');
    const dateStr = `${year}-${month}-${dayNum}`;
    
    const label = i === 6 ? 'Chủ Nhật' : `Thứ ${i + 2}`;
    const dateNum = `${dayNum}/${month}`;

    const dayApps = appointments.value
      .filter(a => a.workDate === dateStr)
      .sort((a, b) => a.startTime.localeCompare(b.startTime));

    days.push({
      dateStr,
      label,
      dateNum,
      appointments: dayApps
    });
  }
  return days;
});

// Thống kê số ca khám tuần này
const appointmentsThisWeek = computed(() => {
  let count = 0;
  weekDays.value.forEach(day => {
    count += day.appointments.filter(a => a.status !== 'cancelled').length;
  });
  return count;
});

// Thống kê số bệnh nhân duy nhất trong tuần
const uniquePatientsThisWeek = computed(() => {
  const patientSet = new Set();
  weekDays.value.forEach(day => {
    day.appointments.forEach(a => {
      if (a.status !== 'cancelled') patientSet.add(a.patientId);
    });
  });
  return patientSet.size;
});

// Thống kê Doanh thu Hôm nay, Tuần này, Tháng này
const feePerSlot = computed(() => doctor.value?.consultationFee || 0);

const revenueToday = computed(() => {
  const count = appointments.value.filter(a => a.workDate === todayIso.value && a.status !== 'cancelled').length;
  return count * feePerSlot.value;
});

const revenueThisWeek = computed(() => {
  return appointmentsThisWeek.value * feePerSlot.value;
});

const revenueThisMonth = computed(() => {
  const currentMonth = todayIso.value.substring(0, 7); // YYYY-MM
  const count = appointments.value.filter(a => a.workDate?.startsWith(currentMonth) && a.status !== 'cancelled').length;
  return count * feePerSlot.value;
});

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
    case 'completed': return 'Hoàn thành';
    default: return status;
  }
};

const formatPrice = (price) => {
  if (!price) return '0';
  return new Intl.NumberFormat('vi-VN').format(price);
};
</script>

<style scoped>
.doctor-dashboard {
  padding-top: 2.5rem;
  padding-bottom: 5rem;
  display: flex;
  flex-direction: column;
  gap: 2.5rem;
}

.doctor-header-card {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  background: #ffffff;
  border-left: 5px solid var(--primary);
}

.doctor-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: var(--primary-light);
  color: var(--primary);
  font-size: 2.2rem;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
}

.doctor-details {
  flex: 1;
}

.header-action {
  flex-shrink: 0;
}

.specialty-badge {
  display: inline-block;
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--primary);
  background: var(--primary-light);
  padding: 0.2rem 0.6rem;
  border-radius: 9999px;
  margin-bottom: 0.3rem;
}

.doctor-name {
  font-size: 1.4rem;
  font-weight: 800;
  margin-bottom: 0.25rem;
}

.meta-row {
  display: flex;
  gap: 1.5rem;
  font-size: 0.9rem;
  color: var(--text-secondary);
}

.section-container {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.section-title {
  font-size: 1.25rem;
  font-weight: 800;
  color: var(--text-primary);
  border-bottom: 2px solid var(--border-color);
  padding-bottom: 0.5rem;
}

/* LỊCH KHÁM TUẦN NÀY */
.weekly-calendar {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 10px;
  background: #ffffff;
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-sm);
}

.calendar-day {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  min-height: 280px;
}

.day-header {
  background: #f8fafc;
  padding: 0.625rem 0.5rem;
  text-align: center;
  border-bottom: 1px solid var(--border-color);
}

.calendar-day.is-today .day-header {
  background: var(--primary);
  color: #ffffff;
}

.day-name {
  display: block;
  font-weight: 700;
  font-size: 0.9rem;
}

.day-date {
  display: block;
  font-size: 0.8rem;
  opacity: 0.9;
}

.day-content {
  padding: 0.5rem;
  flex: 1;
  background: #fafafa;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.calendar-card {
  background: #ffffff;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  padding: 0.5rem;
  font-size: 0.8rem;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.sch-time {
  font-weight: 700;
  color: var(--primary);
}

.sch-patient {
  color: var(--text-primary);
}

.sch-symptoms {
  color: var(--text-secondary);
  font-size: 0.75rem;
}

.empty-day {
  color: var(--text-secondary);
  text-align: center;
  font-size: 0.8rem;
  margin-top: 2rem;
  font-style: italic;
}

/* SCHEDULES GRID FOR FULL CRUD */
.schedules-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 1.25rem;
}

.schedule-card {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.sched-date, .sched-time, .sched-capacity {
  font-size: 0.9rem;
}

.sched-actions {
  display: flex;
  gap: 0.5rem;
  margin-top: 0.5rem;
  padding-top: 0.5rem;
  border-top: 1px solid var(--border-color);
}

/* STATS GRID */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1rem;
}

.stat-card {
  background: #ffffff;
  border: 1.5px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 1.25rem;
  display: flex;
  align-items: center;
  gap: 1rem;
  box-shadow: var(--shadow-sm);
}

.stat-card.outline-teal {
  border-left: 4px solid var(--primary);
}

.stat-icon {
  font-size: 2rem;
}

.stat-info h3 {
  font-size: 0.85rem;
  color: var(--text-secondary);
  margin-bottom: 0.25rem;
}

.stat-number {
  font-size: 1.4rem;
  font-weight: 800;
  color: var(--primary);
}

.btn-sm {
  padding: 0.35rem 0.75rem;
  font-size: 0.8rem;
}

.empty-state {
  text-align: center;
  padding: 2.5rem;
  color: var(--text-secondary);
}
</style>
