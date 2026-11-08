/**
 * BUILDER (Creational) - phản chiếu AppointmentBookedEvent.builder() ở backend.
 * Dựng payload từng bước, kiểm tra hợp lệ ở build(); view không phải tự ghép object/định dạng giờ.
 */
const toHms = (t) => (t && t.length === 5 ? `${t}:00` : t);

export class AppointmentRequestBuilder {
  #data = {};
  patient(id) { this.#data.patientId = id; return this; }
  doctor(id) { this.#data.doctorId = id; return this; }
  schedule(id) { this.#data.scheduleId = id; return this; }
  symptoms(text) { this.#data.symptoms = (text ?? '').trim(); return this; }

  build() {
    const { patientId, doctorId, scheduleId } = this.#data;
    if (!patientId) throw new Error('Thiếu thông tin bệnh nhân');
    if (!doctorId) throw new Error('Thiếu thông tin bác sĩ');
    if (!scheduleId) throw new Error('Vui lòng chọn ca khám');
    return Object.freeze({ ...this.#data, symptoms: this.#data.symptoms ?? '' });
  }
}

export class ScheduleRequestBuilder {
  #data = {};
  doctor(id) { this.#data.doctorId = id; return this; }
  date(d) { this.#data.workDate = d; return this; }
  from(t) { this.#data.startTime = toHms(t); return this; }
  to(t) { this.#data.endTime = toHms(t); return this; }
  capacity(n) { this.#data.maxPatients = n; return this; }

  build() {
    const { doctorId, workDate, startTime, endTime, maxPatients } = this.#data;
    if (!doctorId || !workDate || !startTime || !endTime) throw new Error('Thiếu thông tin ca khám');
    if (startTime >= endTime) throw new Error('Giờ kết thúc phải sau giờ bắt đầu');
    if (!(maxPatients > 0)) throw new Error('Số bệnh nhân tối đa phải lớn hơn 0');
    return Object.freeze({ ...this.#data });
  }
}
