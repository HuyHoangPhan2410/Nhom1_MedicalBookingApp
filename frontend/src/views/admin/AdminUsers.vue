<template>
  <div class="admin-page">
    <div class="admin-header">
      <h1>👤 Quản Lý Tài Khoản</h1>
      <div class="header-actions">
        <router-link to="/admin" class="btn btn-outline btn-sm">← Dashboard</router-link>
        <button @click="openCreate" class="btn btn-primary btn-sm">+ Tạo tài khoản</button>
      </div>
    </div>

    <table class="data-table" v-if="users.length">
      <thead><tr><th>ID</th><th>Email</th><th>Vai trò</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
      <tbody>
        <tr v-for="u in users" :key="u.id">
          <td>{{ u.id }}</td>
          <td>{{ u.email }}</td>
          <td><span :class="['badge', 'badge-' + u.role]">{{ u.role }}</span></td>
          <td>{{ u.isActive ? '✅ Hoạt động' : '🔒 Khóa' }}</td>
          <td class="actions">
            <button @click="openEdit(u)" class="btn btn-outline btn-xs">Sửa</button>
            <button @click="toggleActive(u)" class="btn btn-outline btn-xs">{{ u.isActive ? 'Khóa' : 'Mở' }}</button>
            <button @click="handleDelete(u)" class="btn btn-danger btn-xs">Xóa</button>
          </td>
        </tr>
      </tbody>
    </table>

    <!-- Modal -->
    <div v-if="showModal" class="modal-backdrop" @click.self="showModal = false">
      <div class="modal-content">
        <h3>{{ editingId ? 'Sửa tài khoản' : 'Tạo tài khoản mới' }}</h3>
        <div class="form-group"><label>Email</label><input v-model="form.email" class="form-control" /></div>
        <div class="form-group"><label>Mật khẩu {{ editingId ? '(để trống nếu không đổi)' : '' }}</label><input v-model="form.password" type="password" class="form-control" /></div>
        <div class="form-group"><label>Vai trò</label>
          <select v-model="form.role" class="form-control">
            <option value="patient">patient</option><option value="doctor">doctor</option><option value="admin">admin</option>
          </select>
        </div>

        <div v-if="form.role === 'doctor'" class="doctor-profile-fields">
          <p class="profile-note">Can day du ho so bac si de tai khoan co the dang ky ca lam.</p>
          <div class="form-grid">
            <div class="form-group full-width"><label>Ho ten bac si</label><input v-model="form.fullName" class="form-control" /></div>
            <div class="form-group"><label>Chuyen khoa</label>
              <select v-model="form.specialtyId" class="form-control">
                <option value="" disabled>Chon chuyen khoa</option>
                <option v-for="s in specialties" :key="s.id" :value="String(s.id)">{{ s.name }}</option>
              </select>
            </div>
            <div class="form-group"><label>Kinh nghiem (nam)</label><input v-model.number="form.experienceYears" type="number" min="0" class="form-control" /></div>
            <div class="form-group"><label>Gia kham (VND)</label><input v-model.number="form.consultationFee" type="number" min="0" class="form-control" /></div>
            <div class="form-group full-width"><label>Gioi thieu</label><textarea v-model="form.bio" rows="2" class="form-control"></textarea></div>
          </div>
        </div>
        <div v-if="errorMsg" class="error-banner">{{ errorMsg }}</div>
        <div class="modal-actions">
          <button @click="showModal = false" class="btn btn-outline">Hủy</button>
          <button @click="handleSave" class="btn btn-primary" :disabled="saving">Lưu</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useContainer } from '../../core/container';

const { adminApi, notifier } = useContainer();

const users = ref([]);
const specialties = ref([]);
const showModal = ref(false);
const editingId = ref(null);
const errorMsg = ref('');
const saving = ref(false);
const emptyForm = () => ({ email: '', password: '', role: 'patient', fullName: '', specialtyId: '', experienceYears: 0, consultationFee: 300000, bio: '' });
const form = ref(emptyForm());

const fetchData = async () => {
  const [userRes, specialtyRes] = await Promise.all([
    adminApi.listUsers(),
    adminApi.listSpecialties()
  ]);
  if (userRes.status === 200) users.value = userRes.data;
  if (specialtyRes.status === 200) specialties.value = specialtyRes.data;
};
onMounted(fetchData);

const openCreate = () => { editingId.value = null; form.value = emptyForm(); errorMsg.value = ''; showModal.value = true; };
const openEdit = async (u) => {
  editingId.value = u.id;
  form.value = { ...emptyForm(), email: u.email, role: u.role };
  errorMsg.value = '';
  showModal.value = true;
  if (u.role !== 'doctor') return;

  try {
    const res = await adminApi.getDoctor(u.id);
    if (res.status === 200) {
      form.value.fullName = res.data.fullName;
      form.value.specialtyId = String(res.data.specialtyId);
      form.value.experienceYears = res.data.experienceYears;
      form.value.consultationFee = res.data.consultationFee;
      form.value.bio = res.data.bio || '';
      return;
    }
  } catch (_) {
    // Accounts converted by the old flow can be missing a Doctors record.
  }

  try {
    const patientRes = await adminApi.getPatient(u.id);
    if (patientRes.status === 200) form.value.fullName = patientRes.data.fullName || '';
  } catch (_) {
    // Admin can enter the doctor name manually.
  }
};

const handleSave = async () => {
  errorMsg.value = '';
  if (form.value.role === 'doctor' && !form.value.specialtyId) {
    errorMsg.value = 'Vui long chon chuyen khoa cho bac si.';
    return;
  }
  saving.value = true;
  try {
    const payload = { email: form.value.email, role: form.value.role };
    if (form.value.password) payload.password = form.value.password;
    if (form.value.role === 'doctor') {
      Object.assign(payload, {
        fullName: form.value.fullName,
        specialtyId: String(form.value.specialtyId),
        experienceYears: String(form.value.experienceYears ?? 0),
        consultationFee: String(form.value.consultationFee ?? 0),
        bio: form.value.bio || ''
      });
    }
    if (editingId.value) {
      await adminApi.updateUser(editingId.value, payload);
    } else {
      await adminApi.createUser(payload);
    }
    showModal.value = false;
    await fetchData();
  } catch (e) {
    errorMsg.value = e.message;
  } finally {
    saving.value = false;
  }
};

const toggleActive = async (u) => {
  await adminApi.updateUser(u.id, { isActive: String(!u.isActive) });
  await fetchData();
};

const handleDelete = async (u) => {
  if (!notifier.confirm(`Xóa tài khoản ${u.email}?`)) return;
  try { await adminApi.deleteUser(u.id); await fetchData(); } catch (e) { notifier.error(e.message); }
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
.badge { padding: 0.2rem 0.6rem; border-radius: 9999px; font-size: 0.75rem; font-weight: 700; }
.badge-patient { background: #d1fae5; color: #047857; }
.badge-doctor { background: #e0e7ff; color: #4338ca; }
.badge-admin { background: #fef3c7; color: #b45309; }
.form-group { margin-bottom: 1rem; }
.form-group label { display: block; font-weight: 600; font-size: 0.85rem; margin-bottom: 0.25rem; }
.form-control { width: 100%; padding: 0.6rem 0.8rem; border: 1.5px solid #e2e8f0; border-radius: 8px; font-size: 0.9rem; outline: none; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0 0.75rem; }
.full-width { grid-column: 1 / -1; }
.doctor-profile-fields { border-top: 1px solid #e2e8f0; margin-top: 1rem; padding-top: 1rem; }
.profile-note { margin: 0 0 0.8rem; padding: 0.55rem 0.7rem; background: #ecfeff; color: #0f766e; border-radius: 8px; font-size: 0.82rem; }
textarea.form-control { resize: vertical; }
.error-banner { background: #ffe4e6; color: #be123c; padding: 0.5rem; border-radius: 8px; font-size: 0.85rem; margin-bottom: 1rem; }
.modal-actions { display: flex; justify-content: flex-end; gap: 0.75rem; }
</style>