<template>
  <div class="admin-page">
    <div class="admin-header">
      <h1>🧑‍🦽 Quản Lý Bệnh Nhân</h1>
      <div class="header-actions">
        <router-link to="/admin" class="btn btn-outline btn-sm">← Dashboard</router-link>
        <button @click="openCreate" class="btn btn-primary btn-sm">+ Thêm bệnh nhân</button>
      </div>
    </div>

    <table class="data-table" v-if="patients.length">
      <thead><tr><th>ID</th><th>Họ tên</th><th>Email</th><th>SĐT</th><th>Giới tính</th><th>Ngày sinh</th><th>Thao tác</th></tr></thead>
      <tbody>
        <tr v-for="p in patients" :key="p.userId">
          <td>{{ p.userId }}</td>
          <td><strong>{{ p.fullName }}</strong></td>
          <td>{{ p.user?.email }}</td>
          <td>{{ p.phone }}</td>
          <td>{{ p.gender === 'male' ? 'Nam' : p.gender === 'female' ? 'Nữ' : 'Khác' }}</td>
          <td>{{ p.dob }}</td>
          <td class="actions">
            <button @click="openEdit(p)" class="btn btn-outline btn-xs">Sửa</button>
            <button @click="handleDelete(p)" class="btn btn-danger btn-xs">Xóa</button>
          </td>
        </tr>
      </tbody>
    </table>

    <div v-if="showModal" class="modal-backdrop" @click.self="showModal = false">
      <div class="modal-content">
        <h3>{{ editingId ? 'Sửa bệnh nhân' : 'Thêm bệnh nhân mới' }}</h3>
        <div class="form-grid">
          <div class="form-group"><label>Họ tên</label><input v-model="form.fullName" class="form-control" /></div>
          <div class="form-group" v-if="!editingId"><label>Email</label><input v-model="form.email" class="form-control" /></div>
          <div class="form-group" v-if="!editingId"><label>Mật khẩu</label><input v-model="form.password" type="password" class="form-control" placeholder="Mặc định: password123" /></div>
          <div class="form-group"><label>SĐT</label><input v-model="form.phone" class="form-control" /></div>
          <div class="form-group"><label>Ngày sinh</label><input v-model="form.dob" type="date" class="form-control" /></div>
          <div class="form-group"><label>Giới tính</label>
            <select v-model="form.gender" class="form-control"><option value="male">Nam</option><option value="female">Nữ</option><option value="other">Khác</option></select>
          </div>
          <div class="form-group"><label>Nhóm máu</label><input v-model="form.bloodType" class="form-control" /></div>
          <div class="form-group full-width"><label>Địa chỉ</label><input v-model="form.address" class="form-control" /></div>
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

const patients = ref([]);
const showModal = ref(false);
const editingId = ref(null);
const errorMsg = ref('');
const form = ref({ fullName: '', email: '', password: '', phone: '', dob: '', gender: 'male', bloodType: '', address: '' });

const fetchPatients = async () => {
  const res = await adminApi.listPatients();
  if (res.status === 200) patients.value = res.data;
};
onMounted(fetchPatients);

const openCreate = () => { editingId.value = null; form.value = { fullName: '', email: '', password: '', phone: '', dob: '1995-01-01', gender: 'male', bloodType: '', address: '' }; errorMsg.value = ''; showModal.value = true; };
const openEdit = (p) => { editingId.value = p.userId; form.value = { fullName: p.fullName, phone: p.phone, dob: p.dob, gender: p.gender, bloodType: p.bloodType || '', address: p.address || '' }; errorMsg.value = ''; showModal.value = true; };

const handleSave = async () => {
  errorMsg.value = '';
  try {
    if (editingId.value) {
      await adminApi.updatePatient(editingId.value, form.value);
    } else {
      await adminApi.createPatient(form.value);
    }
    showModal.value = false;
    await fetchPatients();
  } catch (e) { errorMsg.value = e.message; }
};

const handleDelete = async (p) => {
  if (!notifier.confirm(`Xóa bệnh nhân ${p.fullName}?`)) return;
  try { await adminApi.deletePatient(p.userId); await fetchPatients(); } catch (e) { notifier.error(e.message); }
};
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
.error-banner { background: #ffe4e6; color: #be123c; padding: 0.5rem; border-radius: 8px; font-size: 0.85rem; margin: 0.5rem 0; }
.modal-actions { display: flex; justify-content: flex-end; gap: 0.75rem; margin-top: 1rem; }
</style>