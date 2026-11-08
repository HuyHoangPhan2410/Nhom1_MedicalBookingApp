<template>
  <div class="admin-page">
    <div class="admin-header">
      <h1>🩺 Quản Lý Bác Sĩ</h1>
      <div class="header-actions">
        <router-link to="/admin" class="btn btn-outline btn-sm">← Dashboard</router-link>
        <button @click="openCreate" class="btn btn-primary btn-sm">+ Thêm bác sĩ</button>
      </div>
    </div>

    <table class="data-table" v-if="doctors.length">
      <thead><tr><th>ID</th><th>Họ tên</th><th>Chuyên khoa</th><th>Kinh nghiệm</th><th>Giá khám</th><th>Thao tác</th></tr></thead>
      <tbody>
        <tr v-for="d in doctors" :key="d.userId">
          <td>{{ d.userId }}</td>
          <td><strong>{{ d.fullName }}</strong></td>
          <td>{{ d.specialtyName }}</td>
          <td>{{ d.experienceYears }} năm</td>
          <td>{{ formatPrice(d.consultationFee) }} đ</td>
          <td class="actions">
            <button @click="openEdit(d)" class="btn btn-outline btn-xs">Sửa</button>
            <button @click="handleDelete(d)" class="btn btn-danger btn-xs">Xóa</button>
          </td>
        </tr>
      </tbody>
    </table>

    <div v-if="showModal" class="modal-backdrop" @click.self="showModal = false">
      <div class="modal-content">
        <h3>{{ editingId ? 'Sửa bác sĩ' : 'Thêm bác sĩ mới' }}</h3>
        <div class="form-grid">
          <div class="form-group"><label>Họ tên</label><input v-model="form.fullName" class="form-control" /></div>
          <div class="form-group" v-if="!editingId"><label>Email</label><input v-model="form.email" class="form-control" /></div>
          <div class="form-group" v-if="!editingId"><label>Mật khẩu</label><input v-model="form.password" type="password" class="form-control" placeholder="Mặc định: password123" /></div>
          <div class="form-group"><label>Chuyên khoa</label>
            <select v-model="form.specialtyId" class="form-control">
              <option v-for="s in specialties" :key="s.id" :value="s.id">{{ s.name }}</option>
            </select>
          </div>
          <div class="form-group"><label>Kinh nghiệm (năm)</label><input v-model="form.experienceYears" type="number" class="form-control" /></div>
          <div class="form-group"><label>Giá khám (VNĐ)</label><input v-model="form.consultationFee" type="number" class="form-control" /></div>
          <div class="form-group full-width"><label>Giới thiệu</label><textarea v-model="form.bio" class="form-control" rows="3"></textarea></div>
        </div>
        <div v-if="errorMsg" class="error-banner">{{ errorMsg }}</div>
        <div class="modal-actions">
          <button @click="showModal = false" class="btn btn-outline">Hủy</button>
          <button @click="handleSave" class="btn btn-primary">Lưu</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useContainer } from '../../core/container';

const { adminApi, notifier } = useContainer();

const doctors = ref([]);
const specialties = ref([]);
const showModal = ref(false);
const editingId = ref(null);
const errorMsg = ref('');
const form = ref({ fullName: '', email: '', password: '', specialtyId: 1, experienceYears: 0, consultationFee: 0, bio: '' });

const fetchData = async () => {
  const [docRes, specRes] = await Promise.all([
    adminApi.listDoctors(),
    adminApi.listSpecialties()
  ]);
  if (docRes.status === 200) doctors.value = docRes.data;
  if (specRes.status === 200) specialties.value = specRes.data;
};
onMounted(fetchData);

const openCreate = () => { editingId.value = null; form.value = { fullName: '', email: '', password: '', specialtyId: 1, experienceYears: 0, consultationFee: 300000, bio: '' }; errorMsg.value = ''; showModal.value = true; };
const openEdit = (d) => { editingId.value = d.userId; form.value = { fullName: d.fullName, specialtyId: d.specialtyId, experienceYears: d.experienceYears, consultationFee: d.consultationFee, bio: d.bio || '' }; errorMsg.value = ''; showModal.value = true; };

const handleSave = async () => {
  errorMsg.value = '';
  try {
    if (editingId.value) {
      await adminApi.updateDoctor(editingId.value, form.value);
    } else {
      await adminApi.createDoctor(form.value);
    }
    showModal.value = false;
    await fetchData();
  } catch (e) { errorMsg.value = e.message; }
};

const handleDelete = async (d) => {
  if (!notifier.confirm(`Xóa bác sĩ ${d.fullName}?`)) return;
  try { await adminApi.deleteDoctor(d.userId); await fetchData(); } catch (e) { notifier.error(e.message); }
};

const formatPrice = (p) => p ? new Intl.NumberFormat('vi-VN').format(p) : '0';
</script>

<style scoped>
.admin-page { max-width: 1100px; margin: 0 auto; padding: 2rem 1.5rem; }
.admin-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem; }
.admin-header h1 { font-size: 1.4rem; font-weight: 800; }
.header-actions { display: flex; gap: 0.5rem; }
.data-table { width: 100%; border-collapse: collapse; background: #fff; border-radius: 12px; overflow: hidden; border: 1px solid #e2e8f0; }
.data-table th, .data-table td { padding: 0.75rem 1rem; text-align: left; border-bottom: 1px solid #e2e8f0; font-size: 0.9rem; }
.data-table th { background: #f8fafc; font-weight: 700; }
.actions { display: flex; gap: 0.35rem; }
.btn-xs { padding: 0.25rem 0.5rem; font-size: 0.75rem; }
.btn-sm { padding: 0.4rem 1rem; font-size: 0.85rem; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0.75rem; }
.full-width { grid-column: 1 / -1; }
.form-group { margin-bottom: 0.5rem; }
.form-group label { display: block; font-weight: 600; font-size: 0.8rem; margin-bottom: 0.2rem; }
.form-control { width: 100%; padding: 0.5rem 0.7rem; border: 1.5px solid #e2e8f0; border-radius: 8px; font-size: 0.85rem; outline: none; }
textarea.form-control { resize: vertical; }
.error-banner { background: #ffe4e6; color: #be123c; padding: 0.5rem; border-radius: 8px; font-size: 0.85rem; margin: 0.5rem 0; }
.modal-actions { display: flex; justify-content: flex-end; gap: 0.75rem; margin-top: 1rem; }
</style>