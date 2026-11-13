/**
 * Lớp truy cập API (tầng thấp). Là nơi DUY NHẤT biết URL backend của luồng bệnh nhân/bác sĩ.
 * Nhận http (axios instance) qua constructor -> view/facade không phụ thuộc axios (DIP).
 * Mỗi hàm trả về `data` đã bóc từ ApiResponse; ném Error nếu status khác 200.
 */
export class MedicalApi {
  constructor(http) { this.http = http; }

  #unwrap(res) {
    if (res?.status !== 200) throw new Error(res?.message || 'Yêu cầu không thành công');
    return res.data;
  }

  // ----- Doctors / Specialties -----
  listSpecialties() { return this.http.get('/api/specialties').then((r) => this.#unwrap(r)); }
  listDoctors() { return this.http.get('/api/doctors').then((r) => this.#unwrap(r)); }
  listDoctorsBySpecialty(id) { return this.http.get(`/api/doctors/specialty/${id}`).then((r) => this.#unwrap(r)); }
  getDoctor(id) { return this.http.get(`/api/doctors/${id}`).then((r) => this.#unwrap(r)); }

  // ----- Schedules -----
  listSchedules(doctorId, date) {
    const config = date ? { params: { date } } : undefined;
    return this.http.get(`/api/schedules/doctor/${doctorId}`, config).then((r) => this.#unwrap(r));
  }
  createSchedule(body) { return this.http.post('/api/schedules', body).then((r) => this.#unwrap(r)); }
  updateSchedule(id, body) { return this.http.put(`/api/schedules/${id}`, body).then((r) => this.#unwrap(r)); }
  deleteSchedule(id, doctorId) {
    return this.http.delete(`/api/schedules/${id}`, { params: { doctorId } }).then((r) => this.#unwrap(r));
  }

  // ----- Appointments / Patients -----
  bookAppointment(body) { return this.http.post('/api/appointments', body).then((r) => this.#unwrap(r)); }
  previewCancellation(id) { return this.http.get(`/api/appointments/${id}/cancellation-preview`).then((r) => this.#unwrap(r)); }
  cancelAppointment(id) { return this.http.put(`/api/appointments/${id}/cancel`).then((r) => this.#unwrap(r)); }
  rescheduleAppointment(id, newScheduleId) {
    return this.http.put(`/api/appointments/${id}/reschedule`, null, { params: { newScheduleId } }).then((r) => this.#unwrap(r));
  }
  listDoctorAppointments(doctorId) {
    return this.http.get(`/api/appointments/doctor/${doctorId}`).then((r) => this.#unwrap(r));
  }
  getPatient(userId) { return this.http.get(`/api/patients/${userId}`).then((r) => this.#unwrap(r)); }
  listPatientAppointments(userId) {
    return this.http.get(`/api/patients/${userId}/appointments`).then((r) => this.#unwrap(r));
  }
}

/** Tách riêng API thanh toán (ISP): Strategy chỉ nhận đúng 3 hàm này, không thấy phần còn lại. */
export class PaymentApi {
  constructor(http) { this.http = http; }

  async getPublishableKey() {
    const res = await this.http.get('/api/payments/stripe-config');
    return res.data?.publishableKey;
  }
  async createPaymentIntent(appointmentId) {
    const res = await this.http.post('/api/payments/create-intent', null, { params: { appointmentId } });
    return res.data?.clientSecret;
  }
  async confirmPayment(appointmentId, paymentMethod) {
    const res = await this.http.post('/api/payments/confirm', { appointmentId, paymentMethod });
    if (res.status !== 200) throw new Error(res.message || 'Xác nhận thanh toán thất bại');
    return res.data;
  }
}
