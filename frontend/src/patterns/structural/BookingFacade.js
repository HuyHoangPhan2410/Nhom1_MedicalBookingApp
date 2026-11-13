/**
 * FACADE (Structural) - phản chiếu AppointmentOverviewFacade ở backend.
 * Gom nhiều API + Builder + Strategy + Adapter + Observer thành vài hàm đơn giản cho view.
 * View không biết endpoint, không biết Stripe, không tự phát sự kiện.
 * Facade phụ thuộc abstraction được tiêm vào constructor (DIP), không new trực tiếp.
 */
import { EVENTS } from '../behavioral/EventBus';
import { AppointmentRequestBuilder, ScheduleRequestBuilder } from '../creational/RequestBuilders';

export class BookingFacade {
  /**
   * @param {{api: object, paymentApi: object, bus: object, strategies: object, gateway: object}} deps
   */
  constructor({ api, paymentApi, bus, strategies, gateway }) {
    this.api = api;
    this.paymentApi = paymentApi;
    this.bus = bus;
    this.strategies = strategies;
    this.gateway = gateway;
  }

  // ===== Tra cứu =====
  async loadHome() {
    const [specialties, doctors] = await Promise.all([this.api.listSpecialties(), this.api.listDoctors()]);
    return { specialties, doctors };
  }
  listDoctors(specialtyId = null) {
    return specialtyId === null ? this.api.listDoctors() : this.api.listDoctorsBySpecialty(specialtyId);
  }
  getDoctor(id) { return this.api.getDoctor(id); }
  listSchedules(doctorId, date) { return this.api.listSchedules(doctorId, date); }

  async loadPatientOverview(patientId) {
    const [patient, appointments] = await Promise.all([
      this.api.getPatient(patientId),
      this.api.listPatientAppointments(patientId)
    ]);
    return { patient, appointments };
  }

  async loadDoctorPortal(doctorId) {
    const [doctor, appointments, schedules] = await Promise.all([
      this.api.getDoctor(doctorId),
      this.api.listDoctorAppointments(doctorId),
      this.api.listSchedules(doctorId)
    ]);
    return { doctor, appointments, schedules };
  }

  // ===== Đặt / hủy / đổi lịch (dùng Builder) =====
  async bookAppointment({ patientId, doctorId, scheduleId, symptoms }) {
    const request = new AppointmentRequestBuilder()
      .patient(patientId).doctor(doctorId).schedule(scheduleId).symptoms(symptoms).build();
    const result = await this.api.bookAppointment(request);
    this.bus.publish(EVENTS.APPOINTMENT_BOOKED, { request, result });
    return result;
  }

  previewCancellation(appointmentId) { return this.api.previewCancellation(appointmentId); }

  async cancelAppointment(appointmentId) {
    const result = await this.api.cancelAppointment(appointmentId);
    this.bus.publish(EVENTS.APPOINTMENT_CANCELLED, { appointmentId });
    return result;
  }

  async rescheduleAppointment(appointmentId, newScheduleId) {
    const result = await this.api.rescheduleAppointment(appointmentId, newScheduleId);
    this.bus.publish(EVENTS.APPOINTMENT_RESCHEDULED, { appointmentId, newScheduleId });
    return result;
  }

  /** Danh sách ca khác của bác sĩ để đổi lịch (bỏ ca hiện tại). */
  async listReschedulable(appointment) {
    const all = await this.api.listSchedules(appointment.doctorId);
    return all.filter((s) => s.id !== appointment.scheduleId);
  }

  // ===== Ca khám của bác sĩ (dùng Builder) =====
  async #saveSchedule(doctorId, form, persist) {
    const request = new ScheduleRequestBuilder()
      .doctor(doctorId).date(form.workDate).from(form.startTime).to(form.endTime).capacity(form.maxPatients).build();
    const result = await persist(request);
    this.bus.publish(EVENTS.SCHEDULE_CHANGED, { doctorId });
    return result;
  }
  createSchedule(doctorId, form) { return this.#saveSchedule(doctorId, form, (r) => this.api.createSchedule(r)); }
  updateSchedule(scheduleId, doctorId, form) {
    return this.#saveSchedule(doctorId, form, (r) => this.api.updateSchedule(scheduleId, r));
  }
  async cancelSchedule(scheduleId, doctorId) {
    const result = await this.api.deleteSchedule(scheduleId, doctorId);
    this.bus.publish(EVENTS.SCHEDULE_CHANGED, { doctorId });
    return result;
  }

  // ===== Thanh toán (Strategy + Adapter) =====
  paymentMethods() { return this.strategies.methods(); }

  /** Gọi khi người dùng đổi phương thức: khởi tạo cổng nếu phương thức cần nhập thẻ. */
  async preparePaymentMethod(method) {
    if (!this.strategies.forMethod(method).needsCardInput) return { ready: true };
    return this.gateway.init();
  }
  mountCardInput(el) { this.gateway.mountCardInput(el); }

  async pay(appointmentId, method) {
    const strategy = this.strategies.forMethod(method);
    const result = await strategy.execute({ appointmentId, api: this.paymentApi, gateway: this.gateway });
    this.bus.publish(EVENTS.PAYMENT_COMPLETED, { appointmentId, method });
    return result;
  }
}
