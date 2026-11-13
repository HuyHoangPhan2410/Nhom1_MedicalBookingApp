/** API khu vực quản trị. Tách khỏi MedicalApi (SRP/ISP): trang admin không phụ thuộc API bệnh nhân. */
export class AdminApi {
  constructor(http) { this.http = http; }
  login(body) { return this.http.post('/api/admin/auth/login', body); }
  dashboard() { return this.http.get('/api/admin/dashboard'); }

  listUsers() { return this.http.get('/api/admin/users'); }
  createUser(body) { return this.http.post('/api/admin/users', body); }
  updateUser(id, body) { return this.http.put(`/api/admin/users/${id}`, body); }
  deleteUser(id) { return this.http.delete(`/api/admin/users/${id}`); }

  listSpecialties() { return this.http.get('/api/admin/specialties'); }

  listPatients() { return this.http.get('/api/admin/patients'); }
  getPatient(id) { return this.http.get(`/api/admin/patients/${id}`); }
  createPatient(body) { return this.http.post('/api/admin/patients', body); }
  updatePatient(id, body) { return this.http.put(`/api/admin/patients/${id}`, body); }
  deletePatient(id) { return this.http.delete(`/api/admin/patients/${id}`); }

  listDoctors() { return this.http.get('/api/admin/doctors'); }
  getDoctor(id) { return this.http.get(`/api/admin/doctors/${id}`); }
  createDoctor(body) { return this.http.post('/api/admin/doctors', body); }
  updateDoctor(id, body) { return this.http.put(`/api/admin/doctors/${id}`, body); }
  deleteDoctor(id) { return this.http.delete(`/api/admin/doctors/${id}`); }

  listAppointments() { return this.http.get('/api/admin/appointments'); }
  updateAppointmentStatus(id, status) { return this.http.put(`/api/admin/appointments/${id}/status`, { status }); }
}
