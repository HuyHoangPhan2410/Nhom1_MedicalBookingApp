<template>
  <div class="admin-page">
    <div class="admin-header">
      <h1>📅 Quản Lý Lịch Hẹn</h1>
      <router-link to="/admin" class="btn btn-outline btn-sm">← Dashboard</router-link>
    </div>

    <div class="filter-row">
      <button :class="['filter-btn', { active: filter === 'all' }]" @click="filter = 'all'">Tất cả</button>
      <button :class="['filter-btn', { active: filter === 'pending' }]" @click="filter = 'pending'">Chờ TT</button>
      <button :class="['filter-btn', { active: filter === 'confirmed' }]" @click="filter = 'confirmed'">Xác nhận</button>
      <button :class="['filter-btn', { active: filter === 'cancelled' }]" @click="filter = 'cancelled'">Đã hủy</button>
      <button :class="['filter-btn', { active: filter === 'completed' }]" @click="filter = 'completed'">Hoàn thành</button>
    </div>

    <table class="data-table" v-if="filtered.length">
      <thead><tr><th>ID</th><th>Bệnh nhân</th><th>Bác sĩ</th><th>Chuyên khoa</th><th>Ngày</th><th>Giờ</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
      <tbody>
        <tr v-for="a in filtered" :key="a.id">
          <td>#{{ a.id }}</td>
          <td>{{ a.patientName }}</td>
          <td>{{ a.doctorName }}</td>
          <td>{{ a.specialtyName }}</td>
          <td>{{ a.workDate }}</td>
          <td>{{ a.startTime }} - {{ a.endTime }}</td>
          <td><span :class="['badge', 'badge-' + a.status]">{{ statusText(a.status) }}</span></td>
          <td class="actions">
            <select @change="updateStatus(a.id, $event.target.value)" class="status-select">
              <option value="">Đổi trạng thái...</option>
              <option value="pending">Chờ TT</option>
              <option value="confirmed">Xác nhận</option>
              <option value="completed">Hoàn thành</option>
              <option value="cancelled">Hủy</option>
            </select>
          </td>
        </tr>
      </tbody>
    </table>
    <div v-else class="empty-state">Không có lịch hẹn nào.</div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useContainer } from '../../core/container';

const { adminApi, notifier } = useContainer();

const appointments = ref([]);
const filter = ref('all');

const fetchAppointments = async () => {
  const res = await adminApi.listAppointments();
  if (res.status === 200) appointments.value = res.data;
};
onMounted(fetchAppointments);

const filtered = computed(() => {
  if (filter.value === 'all') return appointments.value;
  return appointments.value.filter(a => a.status === filter.value);
});

const updateStatus = async (id, status) => {
  if (!status) return;
  try {
    await adminApi.updateAppointmentStatus(id, status);
    await fetchAppointments();
  } catch (e) { notifier.error(e.message); }
};

const statusText = (s) => ({ pending: 'Chờ TT', confirmed: 'Xác nhận', cancelled: 'Đã hủy', completed: 'Hoàn thành' }[s] || s);
</script>

<style scoped>
.admin-page { max-width: 1200px; margin: 0 auto; padding: 2rem 1.5rem; }
.admin-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem; }
.admin-header h1 { font-size: 1.4rem; font-weight: 800; }
.filter-row { display: flex; gap: 0.5rem; margin-bottom: 1.5rem; }
.filter-btn { padding: 0.4rem 1rem; border-radius: 9999px; border: 1px solid #e2e8f0; background: #fff; cursor: pointer; font-size: 0.85rem; font-weight: 600; }
.filter-btn.active { background: #0d9488; color: #fff; border-color: #0d9488; }
.data-table { width: 100%; border-collapse: collapse; background: #fff; border-radius: 12px; overflow: hidden; border: 1px solid #e2e8f0; }
.data-table th, .data-table td { padding: 0.6rem 0.75rem; text-align: left; border-bottom: 1px solid #e2e8f0; font-size: 0.85rem; }
.data-table th { background: #f8fafc; font-weight: 700; }
.badge { padding: 0.2rem 0.6rem; border-radius: 9999px; font-size: 0.7rem; font-weight: 700; }
.badge-pending { background: #fef3c7; color: #b45309; }
.badge-confirmed { background: #d1fae5; color: #047857; }
.badge-cancelled { background: #ffe4e6; color: #be123c; }
.badge-completed { background: #e0e7ff; color: #4338ca; }
.status-select { padding: 0.3rem; border: 1px solid #e2e8f0; border-radius: 6px; font-size: 0.8rem; }
.empty-state { text-align: center; padding: 3rem; color: #64748b; }
.btn-sm { padding: 0.4rem 1rem; font-size: 0.85rem; }
</style>