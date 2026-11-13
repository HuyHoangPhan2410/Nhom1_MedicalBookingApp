<template>
  <div class="admin-page">
    <div class="admin-header">
      <h1>🛡️ Bảng Điều Khiển Quản Trị</h1>
      <button @click="logout" class="btn btn-danger btn-sm">Đăng xuất</button>
    </div>

    <div class="stats-grid" v-if="stats">
      <div class="stat-card"><div class="stat-icon">👥</div><div class="stat-info"><h3>Tổng tài khoản</h3><p class="stat-number">{{ stats.totalUsers }}</p></div></div>
      <div class="stat-card"><div class="stat-icon">🩺</div><div class="stat-info"><h3>Bác sĩ</h3><p class="stat-number">{{ stats.totalDoctors }}</p></div></div>
      <div class="stat-card"><div class="stat-icon">🧑‍🦽</div><div class="stat-info"><h3>Bệnh nhân</h3><p class="stat-number">{{ stats.totalPatients }}</p></div></div>
      <div class="stat-card"><div class="stat-icon">🏥</div><div class="stat-info"><h3>Chuyên khoa</h3><p class="stat-number">{{ stats.totalSpecialties }}</p></div></div>
      <div class="stat-card"><div class="stat-icon">📅</div><div class="stat-info"><h3>Tổng lịch hẹn</h3><p class="stat-number">{{ stats.totalAppointments }}</p></div></div>
      <div class="stat-card"><div class="stat-icon">⏳</div><div class="stat-info"><h3>Chờ thanh toán</h3><p class="stat-number">{{ stats.pendingAppointments }}</p></div></div>
      <div class="stat-card"><div class="stat-icon">✅</div><div class="stat-info"><h3>Đã xác nhận</h3><p class="stat-number">{{ stats.confirmedAppointments }}</p></div></div>
      <div class="stat-card"><div class="stat-icon">❌</div><div class="stat-info"><h3>Đã hủy</h3><p class="stat-number">{{ stats.cancelledAppointments }}</p></div></div>
    </div>

    <section class="charts-section" v-if="stats">
      <div class="section-heading charts-heading">
        <div>
          <span class="section-kicker">SƠ ĐỒ PHÂN TÍCH</span>
          <h2>Trực quan số liệu hệ thống</h2>
        </div>
        <span class="updated-label">Cập nhật theo dữ liệu hiện tại</span>
      </div>

      <div class="charts-grid">
        <article class="chart-panel">
          <div class="chart-title-row">
            <div>
              <h3>Cơ cấu trạng thái lịch hẹn</h3>
              <p>{{ stats.totalAppointments }} lịch hẹn trong hệ thống</p>
            </div>
          </div>

          <div class="bar-chart">
            <div v-for="item in appointmentChart" :key="item.label" class="bar-row">
              <div class="bar-meta">
                <span>{{ item.label }}</span>
                <strong>{{ item.value }} <small>({{ item.percentLabel }})</small></strong>
              </div>
              <div class="bar-track" :aria-label="`${item.label}: ${item.value}`">
                <span class="bar-fill" :style="{ width: `${item.percent}%`, backgroundColor: item.color }"></span>
              </div>
            </div>
          </div>
        </article>

        <article class="chart-panel account-chart-panel">
          <div class="chart-title-row">
            <div>
              <h3>Phân bổ tài khoản</h3>
              <p>Tỷ trọng các nhóm người dùng</p>
            </div>
          </div>

          <div class="donut-layout">
            <div class="donut-chart" :style="userDonutStyle" role="img" aria-label="Biểu đồ phân bổ tài khoản">
              <div class="donut-center">
                <strong>{{ stats.totalUsers }}</strong>
                <span>Tài khoản</span>
              </div>
            </div>

            <div class="chart-legend">
              <div v-for="item in userChart" :key="item.label" class="legend-row">
                <span class="legend-dot" :style="{ backgroundColor: item.color }"></span>
                <span class="legend-label">{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
                <small>{{ item.percentLabel }}</small>
              </div>
            </div>
          </div>
        </article>
      </div>
    </section>

    <section class="overview-section" v-if="stats">
      <div class="section-heading">
        <div>
          <span class="section-kicker">BÁO CÁO HỆ THỐNG</span>
          <h2>Bảng số liệu thống kê tổng quan</h2>
        </div>
        <span class="updated-label">Dữ liệu hiện tại</span>
      </div>

      <div class="overview-table-wrap">
        <table class="overview-table">
          <thead>
            <tr>
              <th>Nhóm số liệu</th>
              <th>Chỉ số</th>
              <th class="number-column">Số lượng</th>
              <th class="number-column">Tỷ lệ</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in overviewRows" :key="row.label">
              <td><span :class="['group-badge', row.groupClass]">{{ row.group }}</span></td>
              <td class="metric-label">{{ row.label }}</td>
              <td class="metric-value">{{ row.value }}</td>
              <td class="ratio-value">{{ row.ratio }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <p class="table-note">Tỷ lệ tài khoản được tính trên tổng tài khoản; tỷ lệ lịch hẹn được tính trên tổng lịch hẹn.</p>
    </section>

    <nav class="admin-nav">
      <router-link to="/admin/users" class="nav-card">👤 Quản lý Tài khoản</router-link>
      <router-link to="/admin/patients" class="nav-card">🧑‍🦽 Quản lý Bệnh nhân</router-link>
      <router-link to="/admin/doctors" class="nav-card">🩺 Quản lý Bác sĩ</router-link>
      <router-link to="/admin/appointments" class="nav-card">📅 Quản lý Lịch hẹn</router-link>
    </nav>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useContainer } from '../../core/container';

const { adminApi, notifier } = useContainer();

const router = useRouter();
const stats = ref(null);

const percentage = (value, total) => {
  if (!total) return '0%';
  return `${((value / total) * 100).toFixed(1)}%`;
};

const completedAppointmentCount = computed(() => {
  if (!stats.value) return 0;
  return Math.max(
    0,
    stats.value.totalAppointments
      - stats.value.pendingAppointments
      - stats.value.confirmedAppointments
      - stats.value.cancelledAppointments
  );
});

const appointmentChart = computed(() => {
  if (!stats.value) return [];
  const total = stats.value.totalAppointments;

  return [
    { label: 'Chờ thanh toán', value: stats.value.pendingAppointments, color: '#d69e2e' },
    { label: 'Đã xác nhận', value: stats.value.confirmedAppointments, color: '#0d9488' },
    { label: 'Hoàn thành', value: completedAppointmentCount.value, color: '#2563eb' },
    { label: 'Đã hủy', value: stats.value.cancelledAppointments, color: '#dc5a4f' }
  ].map(item => ({
    ...item,
    percent: total ? Math.min(100, (item.value / total) * 100) : 0,
    percentLabel: percentage(item.value, total)
  }));
});

const userChart = computed(() => {
  if (!stats.value) return [];
  const total = stats.value.totalUsers;
  const administrators = Math.max(0, total - stats.value.totalDoctors - stats.value.totalPatients);

  return [
    { label: 'Bác sĩ', value: stats.value.totalDoctors, color: '#0d9488' },
    { label: 'Bệnh nhân', value: stats.value.totalPatients, color: '#2563eb' },
    { label: 'Quản trị viên', value: administrators, color: '#d69e2e' }
  ].map(item => ({
    ...item,
    percent: total ? (item.value / total) * 100 : 0,
    percentLabel: percentage(item.value, total)
  }));
});

const userDonutStyle = computed(() => {
  if (!stats.value?.totalUsers) {
    return { '--donut-gradient': '#e2e8f0' };
  }

  let start = 0;
  const stops = userChart.value.map(item => {
    const end = start + item.percent;
    const stop = `${item.color} ${start}% ${end}%`;
    start = end;
    return stop;
  });

  return { '--donut-gradient': `conic-gradient(${stops.join(', ')})` };
});

const overviewRows = computed(() => {
  if (!stats.value) return [];

  const completedAppointments = Math.max(
    0,
    stats.value.totalAppointments
      - stats.value.pendingAppointments
      - stats.value.confirmedAppointments
      - stats.value.cancelledAppointments
  );

  return [
    { group: 'Người dùng', groupClass: 'users', label: 'Tổng tài khoản', value: stats.value.totalUsers, ratio: '100%' },
    { group: 'Người dùng', groupClass: 'users', label: 'Tài khoản bác sĩ', value: stats.value.totalDoctors, ratio: percentage(stats.value.totalDoctors, stats.value.totalUsers) },
    { group: 'Người dùng', groupClass: 'users', label: 'Tài khoản bệnh nhân', value: stats.value.totalPatients, ratio: percentage(stats.value.totalPatients, stats.value.totalUsers) },
    { group: 'Danh mục', groupClass: 'catalog', label: 'Chuyên khoa', value: stats.value.totalSpecialties, ratio: '—' },
    { group: 'Lịch hẹn', groupClass: 'appointments', label: 'Tổng lịch hẹn', value: stats.value.totalAppointments, ratio: '100%' },
    { group: 'Lịch hẹn', groupClass: 'appointments', label: 'Chờ thanh toán', value: stats.value.pendingAppointments, ratio: percentage(stats.value.pendingAppointments, stats.value.totalAppointments) },
    { group: 'Lịch hẹn', groupClass: 'appointments', label: 'Đã xác nhận', value: stats.value.confirmedAppointments, ratio: percentage(stats.value.confirmedAppointments, stats.value.totalAppointments) },
    { group: 'Lịch hẹn', groupClass: 'appointments', label: 'Hoàn thành', value: completedAppointments, ratio: percentage(completedAppointments, stats.value.totalAppointments) },
    { group: 'Lịch hẹn', groupClass: 'appointments', label: 'Đã hủy', value: stats.value.cancelledAppointments, ratio: percentage(stats.value.cancelledAppointments, stats.value.totalAppointments) }
  ];
});

onMounted(async () => {
  try {
    const res = await adminApi.dashboard();
    if (res.status === 200) stats.value = res.data;
  } catch (e) { console.error(e); }
});

const logout = () => {
  localStorage.removeItem('admin_token');
  localStorage.removeItem('admin_user');
  router.push('/admin/login');
};
</script>

<style scoped>
.admin-page { max-width: 1100px; margin: 0 auto; padding: 2rem 1.5rem; }
.admin-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 2rem; }
.admin-header h1 { font-size: 1.5rem; font-weight: 800; }
.stats-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 1rem; margin-bottom: 2.5rem; }
.stat-card { background: #fff; border: 1px solid #e2e8f0; border-radius: 12px; padding: 1.25rem; display: flex; align-items: center; gap: 1rem; }
.stat-icon { font-size: 2rem; }
.stat-info h3 { font-size: 0.8rem; color: #64748b; }
.stat-number { font-size: 1.5rem; font-weight: 800; color: #0d9488; }
.charts-section { margin-bottom: 2.5rem; }
.charts-heading { padding: 0 0.25rem; }
.charts-grid { display: grid; grid-template-columns: minmax(0, 1.35fr) minmax(320px, 0.85fr); gap: 1rem; }
.chart-panel { min-width: 0; background: #fff; border: 1px solid #dbe7e5; border-radius: 16px; padding: 1.5rem; box-shadow: 0 10px 30px rgba(15, 118, 110, 0.06); }
.chart-title-row { display: flex; justify-content: space-between; gap: 1rem; margin-bottom: 1.5rem; }
.chart-title-row h3 { margin: 0; color: #0f172a; font-size: 1rem; }
.chart-title-row p { margin: 0.3rem 0 0; color: #64748b; font-size: 0.8rem; }
.bar-chart { display: flex; flex-direction: column; gap: 1.15rem; }
.bar-meta { display: flex; justify-content: space-between; gap: 1rem; margin-bottom: 0.45rem; color: #334155; font-size: 0.82rem; }
.bar-meta strong { color: #0f172a; }
.bar-meta small { color: #64748b; font-weight: 600; }
.bar-track { height: 12px; overflow: hidden; background: #edf3f1; border-radius: 4px; }
.bar-fill { display: block; min-width: 0; height: 100%; border-radius: 4px; transition: width 0.45s ease; }
.donut-layout { display: grid; grid-template-columns: 180px minmax(0, 1fr); align-items: center; gap: 1.5rem; min-height: 210px; }
.donut-chart { width: 180px; aspect-ratio: 1; padding: 25px; border-radius: 50%; background: var(--donut-gradient); }
.donut-center { width: 100%; height: 100%; border-radius: 50%; background: #fff; display: flex; flex-direction: column; align-items: center; justify-content: center; box-shadow: inset 0 0 0 1px #edf2f1; }
.donut-center strong { color: #0f172a; font-size: 1.8rem; line-height: 1; }
.donut-center span { margin-top: 0.35rem; color: #64748b; font-size: 0.72rem; font-weight: 700; text-transform: uppercase; }
.chart-legend { display: flex; flex-direction: column; gap: 0.9rem; }
.legend-row { display: grid; grid-template-columns: 10px minmax(0, 1fr) auto auto; align-items: center; gap: 0.55rem; font-size: 0.78rem; }
.legend-dot { width: 10px; height: 10px; border-radius: 2px; }
.legend-label { overflow-wrap: anywhere; color: #475569; font-weight: 600; }
.legend-row strong { color: #0f172a; font-size: 0.9rem; }
.legend-row small { min-width: 38px; color: #64748b; text-align: right; }

.overview-section { margin-bottom: 2.5rem; background: #fff; border: 1px solid #dbe7e5; border-radius: 16px; padding: 1.5rem; box-shadow: 0 10px 30px rgba(15, 118, 110, 0.06); }
.section-heading { display: flex; justify-content: space-between; align-items: flex-end; gap: 1rem; margin-bottom: 1.25rem; }
.section-heading h2 { margin: 0.25rem 0 0; font-size: 1.25rem; color: #0f172a; }
.section-kicker { color: #0d9488; font-size: 0.7rem; font-weight: 800; letter-spacing: 0.12em; }
.updated-label { padding: 0.4rem 0.75rem; border-radius: 999px; background: #ccfbf1; color: #0f766e; font-size: 0.75rem; font-weight: 700; white-space: nowrap; }
.overview-table-wrap { overflow-x: auto; }
.overview-table { width: 100%; min-width: 620px; border-collapse: collapse; }
.overview-table th { padding: 0.8rem 1rem; background: #f8fafc; border-bottom: 1px solid #dbe7e5; color: #64748b; font-size: 0.72rem; letter-spacing: 0.06em; text-align: left; text-transform: uppercase; }
.overview-table td { padding: 0.85rem 1rem; border-bottom: 1px solid #edf2f1; }
.overview-table tbody tr:last-child td { border-bottom: 0; }
.overview-table tbody tr:hover { background: #f8fffd; }
.number-column, .metric-value, .ratio-value { text-align: right !important; }
.metric-label { color: #334155; font-weight: 600; }
.metric-value { color: #0f172a; font-size: 1rem; font-weight: 800; }
.ratio-value { color: #0d9488; font-weight: 700; }
.group-badge { display: inline-block; min-width: 90px; padding: 0.3rem 0.55rem; border-radius: 7px; font-size: 0.72rem; font-weight: 700; text-align: center; }
.group-badge.users { background: #e0f2fe; color: #0369a1; }
.group-badge.catalog { background: #fef3c7; color: #a16207; }
.group-badge.appointments { background: #ccfbf1; color: #0f766e; }
.table-note { margin: 1rem 0 0; color: #64748b; font-size: 0.78rem; }
.admin-nav { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 1rem; }
.nav-card {
  display: block; padding: 1.5rem; background: #fff; border: 1.5px solid #e2e8f0;
  border-radius: 12px; text-decoration: none; color: #0f172a; font-weight: 700;
  font-size: 1rem; transition: all 0.2s; text-align: center;
}
.nav-card:hover { border-color: #0d9488; background: #ccfbf1; transform: translateY(-2px); }
@media (max-width: 900px) { .charts-grid { grid-template-columns: 1fr; } }
@media (max-width: 640px) {
  .admin-header, .section-heading { align-items: flex-start; flex-direction: column; }
  .overview-section { padding: 1rem; }
  .chart-panel { padding: 1rem; }
  .donut-layout { grid-template-columns: 1fr; justify-items: center; }
  .chart-legend { width: 100%; }
  .bar-meta { align-items: flex-start; }
}
.btn-sm { padding: 0.4rem 1rem; font-size: 0.85rem; }
</style>
