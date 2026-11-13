import { describe, it, expect, vi, beforeEach } from 'vitest';
import { EventBus, EVENTS } from '../patterns/behavioral/EventBus';
import {
  NotificationCreator, AlertNotificationCreator, ConfirmNotificationCreator, AuditNotificationCreator
} from '../patterns/creational/NotificationFactory';
import { AppointmentRequestBuilder, ScheduleRequestBuilder } from '../patterns/creational/RequestBuilders';
import { StripeGatewayAdapter, MockGatewayAdapter } from '../patterns/structural/PaymentGatewayAdapters';
import { CashPaymentStrategy, CardPaymentStrategy, PaymentStrategyRegistry } from '../patterns/behavioral/PaymentStrategies';
import { BookingFacade } from '../patterns/structural/BookingFacade';

/* ---------------- Observer ---------------- */
describe('Observer - EventBus', () => {
  it('TC-O01: publish gọi mọi observer đã đăng ký', () => {
    const bus = new EventBus(); const a = vi.fn(); const b = vi.fn();
    bus.subscribe(EVENTS.APPOINTMENT_BOOKED, a); bus.subscribe(EVENTS.APPOINTMENT_BOOKED, b);
    bus.publish(EVENTS.APPOINTMENT_BOOKED, { id: 1 });
    expect(a).toHaveBeenCalledWith({ id: 1 }); expect(b).toHaveBeenCalledWith({ id: 1 });
  });
  it('TC-O02: hủy đăng ký thì không nhận sự kiện nữa', () => {
    const bus = new EventBus(); const a = vi.fn();
    const off = bus.subscribe(EVENTS.PAYMENT_COMPLETED, a); off();
    bus.publish(EVENTS.PAYMENT_COMPLETED, {});
    expect(a).not.toHaveBeenCalled(); expect(bus.listenerCount(EVENTS.PAYMENT_COMPLETED)).toBe(0);
  });
  it('TC-O03: một observer lỗi không làm hỏng observer khác', () => {
    const bus = new EventBus(); const ok = vi.fn(); vi.spyOn(console, 'error').mockImplementation(() => {});
    bus.subscribe(EVENTS.SCHEDULE_CHANGED, () => { throw new Error('boom'); });
    bus.subscribe(EVENTS.SCHEDULE_CHANGED, ok);
    expect(() => bus.publish(EVENTS.SCHEDULE_CHANGED, {})).not.toThrow();
    expect(ok).toHaveBeenCalled();
  });
  it('TC-O04: subscribeAll nhận mọi loại sự kiện', () => {
    const bus = new EventBus(); const all = vi.fn();
    bus.subscribeAll(all);
    bus.publish(EVENTS.APPOINTMENT_CANCELLED, { id: 2 }); bus.publish(EVENTS.PAYMENT_COMPLETED, { id: 3 });
    expect(all).toHaveBeenCalledTimes(2);
  });
});

/* ---------------- Factory Method ---------------- */
describe('Factory Method - NotificationCreator', () => {
  beforeEach(() => {
    vi.stubGlobal('window', { alert: vi.fn(), confirm: vi.fn(() => true) });
  });
  it('TC-F01: AlertNotificationCreator tạo thông báo alert', () => {
    new AlertNotificationCreator().notify('lỗi!');
    expect(window.alert).toHaveBeenCalledWith('lỗi!');
  });
  it('TC-F02: ConfirmNotificationCreator trả về lựa chọn người dùng', () => {
    window.confirm = vi.fn(() => false);
    expect(new ConfirmNotificationCreator().notify('Hủy?')).toBe(false);
  });
  it('TC-F03: AuditNotificationCreator ghi vào log thay vì hiển thị', () => {
    const sink = []; new AuditNotificationCreator(sink).notify('x');
    expect(sink).toHaveLength(1); expect(sink[0].message).toBe('x'); expect(window.alert).not.toHaveBeenCalled();
  });
  it('TC-F04: thông báo rỗng bị từ chối; Creator trừu tượng bắt buộc hiện thực factory method', () => {
    expect(() => new AlertNotificationCreator().notify('')).toThrow();
    expect(() => new NotificationCreator().notify('a')).toThrow(/chưa được hiện thực/);
  });
});

/* ---------------- Builder ---------------- */
describe('Builder - Request builders', () => {
  it('TC-B01: dựng yêu cầu đặt lịch hợp lệ và bất biến', () => {
    const r = new AppointmentRequestBuilder().patient(1).doctor(2).schedule(3).symptoms('  ho  ').build();
    expect(r).toEqual({ patientId: 1, doctorId: 2, scheduleId: 3, symptoms: 'ho' });
    expect(Object.isFrozen(r)).toBe(true);
  });
  it('TC-B02: thiếu ca khám -> báo lỗi dễ hiểu', () => {
    expect(() => new AppointmentRequestBuilder().patient(1).doctor(2).build()).toThrow('Vui lòng chọn ca khám');
  });
  it('TC-B03: ScheduleRequestBuilder chuẩn hóa HH:mm -> HH:mm:ss', () => {
    const r = new ScheduleRequestBuilder().doctor(1).date('2026-11-10').from('08:00').to('09:30').capacity(5).build();
    expect(r.startTime).toBe('08:00:00'); expect(r.endTime).toBe('09:30:00');
  });
  it('TC-B04: giờ kết thúc phải sau giờ bắt đầu; sức chứa phải > 0', () => {
    const b = () => new ScheduleRequestBuilder().doctor(1).date('2026-11-10');
    expect(() => b().from('10:00').to('09:00').capacity(5).build()).toThrow('Giờ kết thúc');
    expect(() => b().from('08:00').to('09:00').capacity(0).build()).toThrow('lớn hơn 0');
  });
});

/* ---------------- Adapter (+ LSP) ---------------- */
describe('Adapter - PaymentGateway', () => {
  it('TC-A01: Stripe chưa cấu hình khóa -> init báo not ready', async () => {
    const g = new StripeGatewayAdapter({ loadStripe: vi.fn(), getPublishableKey: async () => 'pk_test_placeholder' });
    const r = await g.init(); expect(r.ready).toBe(false); expect(r.message).toMatch(/chưa được cấu hình/);
  });
  it('TC-A02: Stripe adapter bọc confirmCardPayment; lỗi thẻ được ném thành Error', async () => {
    const stripe = { elements: () => ({ create: () => ({ mount() {} }) }), confirmCardPayment: vi.fn(async () => ({ error: { message: 'Thẻ bị từ chối' } })) };
    const g = new StripeGatewayAdapter({ loadStripe: async () => stripe, getPublishableKey: async () => 'pk_test_real' });
    await g.init(); g.mountCardInput({});
    await expect(g.charge({ prepare: async () => 'secret' })).rejects.toThrow('Thẻ bị từ chối');
  });
  it('TC-A03 (LSP): Mock và Stripe cùng hợp đồng - charge thành công thì resolve', async () => {
    const stripe = { elements: () => ({ create: () => ({ mount() {} }) }), confirmCardPayment: async () => ({}) };
    const adapters = [
      new MockGatewayAdapter(),
      new StripeGatewayAdapter({ loadStripe: async () => stripe, getPublishableKey: async () => 'pk_test_real' })
    ];
    for (const g of adapters) {
      expect((await g.init()).ready).toBe(true); g.mountCardInput({});
      await expect(g.charge({ prepare: async () => 'secret' })).resolves.toBeUndefined();
    }
  });
  it('TC-A04: Stripe chưa sẵn sàng thì charge báo lỗi rõ ràng thay vì TypeError', async () => {
    const g = new StripeGatewayAdapter({ loadStripe: vi.fn(), getPublishableKey: async () => '' });
    await expect(g.charge({ prepare: async () => 's' })).rejects.toThrow('Stripe chưa sẵn sàng');
  });
});

/* ---------------- Strategy ---------------- */
describe('Strategy - PaymentStrategyRegistry', () => {
  const makeApi = () => ({ confirmPayment: vi.fn(async () => ({})), createPaymentIntent: vi.fn(async () => 'secret') });
  it('TC-S01: Cash chỉ xác nhận với backend, không đụng cổng thanh toán', async () => {
    const api = makeApi(); const gateway = { charge: vi.fn() };
    const r = await new CashPaymentStrategy().execute({ appointmentId: 7, api, gateway });
    expect(api.confirmPayment).toHaveBeenCalledWith(7, 'cash'); expect(gateway.charge).not.toHaveBeenCalled();
    expect(r.message).toMatch(/tiền mặt/);
  });
  it('TC-S02: Card thu tiền qua gateway rồi mới xác nhận với backend', async () => {
    const api = makeApi(); const order = [];
    const gateway = { charge: vi.fn(async ({ prepare }) => { order.push('charge'); await prepare(); }) };
    api.confirmPayment = vi.fn(async () => { order.push('confirm'); });
    await new CardPaymentStrategy().execute({ appointmentId: 7, api, gateway });
    expect(order).toEqual(['charge', 'confirm']); expect(api.createPaymentIntent).toHaveBeenCalledWith(7);
  });
  it('TC-S03: thẻ bị từ chối thì KHÔNG xác nhận thanh toán với backend', async () => {
    const api = makeApi(); const gateway = { charge: async () => { throw new Error('declined'); } };
    await expect(new CardPaymentStrategy().execute({ appointmentId: 7, api, gateway })).rejects.toThrow('declined');
    expect(api.confirmPayment).not.toHaveBeenCalled();
  });
  it('TC-S04 (OCP): thêm phương thức mới chỉ cần register, không sửa lớp cũ', async () => {
    const reg = new PaymentStrategyRegistry([new CashPaymentStrategy(), new CardPaymentStrategy()]);
    reg.register({ method: 'momo', needsCardInput: false, execute: async () => ({ message: 'MoMo OK' }) });
    expect(reg.methods()).toEqual(['cash', 'card', 'momo']);
    expect((await reg.forMethod('momo').execute({})).message).toBe('MoMo OK');
    expect(() => reg.forMethod('bitcoin')).toThrow('không hợp lệ');
  });
});

/* ---------------- Facade (+ Builder + Observer + Strategy + Adapter phối hợp) ---------------- */
describe('Facade - BookingFacade', () => {
  const setup = () => {
    const api = {
      listSpecialties: vi.fn(async () => [{ id: 1 }]), listDoctors: vi.fn(async () => [{ userId: 9 }]),
      listDoctorsBySpecialty: vi.fn(async () => []), getPatient: vi.fn(async () => ({ id: 1 })),
      listPatientAppointments: vi.fn(async () => [{ id: 5 }]),
      bookAppointment: vi.fn(async () => ({ id: 100 })), cancelAppointment: vi.fn(async () => ({})),
      rescheduleAppointment: vi.fn(async () => ({})), listSchedules: vi.fn(async () => [{ id: 1 }, { id: 2 }]),
      createSchedule: vi.fn(async () => ({})), updateSchedule: vi.fn(async () => ({})), deleteSchedule: vi.fn(async () => ({}))
    };
    const paymentApi = { confirmPayment: vi.fn(async () => ({})), createPaymentIntent: vi.fn(async () => 's') };
    const bus = new EventBus(); const gateway = new MockGatewayAdapter();
    const strategies = new PaymentStrategyRegistry([new CashPaymentStrategy(), new CardPaymentStrategy()]);
    return { api, paymentApi, bus, gateway, facade: new BookingFacade({ api, paymentApi, bus, strategies, gateway }) };
  };

  it('TC-FA01: loadHome gom 2 API thành 1 lời gọi', async () => {
    const { facade } = setup(); const r = await facade.loadHome();
    expect(r).toEqual({ specialties: [{ id: 1 }], doctors: [{ userId: 9 }] });
  });
  it('TC-FA02: đặt lịch dùng Builder rồi phát sự kiện APPOINTMENT_BOOKED', async () => {
    const { facade, api, bus } = setup(); const seen = vi.fn(); bus.subscribe(EVENTS.APPOINTMENT_BOOKED, seen);
    await facade.bookAppointment({ patientId: 1, doctorId: 2, scheduleId: 3, symptoms: 'sốt' });
    expect(api.bookAppointment).toHaveBeenCalledWith({ patientId: 1, doctorId: 2, scheduleId: 3, symptoms: 'sốt' });
    expect(seen).toHaveBeenCalledTimes(1);
  });
  it('TC-FA03: dữ liệu không hợp lệ bị Builder chặn, KHÔNG gọi API, KHÔNG phát sự kiện', async () => {
    const { facade, api, bus } = setup(); const seen = vi.fn(); bus.subscribe(EVENTS.APPOINTMENT_BOOKED, seen);
    await expect(facade.bookAppointment({ patientId: 1, doctorId: 2, scheduleId: undefined })).rejects.toThrow('chọn ca khám');
    expect(api.bookAppointment).not.toHaveBeenCalled(); expect(seen).not.toHaveBeenCalled();
  });
  it('TC-FA04: hủy lịch phát APPOINTMENT_CANCELLED; đổi lịch phát APPOINTMENT_RESCHEDULED', async () => {
    const { facade, bus } = setup(); const c = vi.fn(); const r = vi.fn();
    bus.subscribe(EVENTS.APPOINTMENT_CANCELLED, c); bus.subscribe(EVENTS.APPOINTMENT_RESCHEDULED, r);
    await facade.cancelAppointment(5); await facade.rescheduleAppointment(5, 2);
    expect(c).toHaveBeenCalledWith({ appointmentId: 5 }); expect(r).toHaveBeenCalledWith({ appointmentId: 5, newScheduleId: 2 });
  });
  it('TC-FA05: đổi lịch chỉ liệt kê các ca khác ca hiện tại', async () => {
    const { facade } = setup();
    expect(await facade.listReschedulable({ doctorId: 9, scheduleId: 1 })).toEqual([{ id: 2 }]);
  });
  it('TC-FA06: tạo ca khám -> Builder chuẩn hóa giờ + phát SCHEDULE_CHANGED', async () => {
    const { facade, api, bus } = setup(); const seen = vi.fn(); bus.subscribe(EVENTS.SCHEDULE_CHANGED, seen);
    await facade.createSchedule(9, { workDate: '2026-11-10', startTime: '08:00', endTime: '09:30', maxPatients: 5 });
    expect(api.createSchedule.mock.calls[0][0].startTime).toBe('08:00:00'); expect(seen).toHaveBeenCalled();
  });
  it('TC-FA07: thanh toán tiền mặt qua Strategy và phát PAYMENT_COMPLETED', async () => {
    const { facade, paymentApi, bus } = setup(); const seen = vi.fn(); bus.subscribe(EVENTS.PAYMENT_COMPLETED, seen);
    const r = await facade.pay(11, 'cash');
    expect(paymentApi.confirmPayment).toHaveBeenCalledWith(11, 'cash'); expect(r.message).toMatch(/tiền mặt/);
    expect(seen).toHaveBeenCalledWith({ appointmentId: 11, method: 'cash' });
  });
  it('TC-FA08: thanh toán thẻ với Mock adapter không cần khóa Stripe; thất bại thì không phát sự kiện', async () => {
    const { facade, bus, gateway } = setup(); const seen = vi.fn(); bus.subscribe(EVENTS.PAYMENT_COMPLETED, seen);
    await facade.pay(12, 'card'); expect(seen).toHaveBeenCalledTimes(1);
    gateway.shouldFail = true;
    await expect(facade.pay(13, 'card')).rejects.toThrow('Thẻ bị từ chối'); expect(seen).toHaveBeenCalledTimes(1);
  });
  it('TC-FA09: chỉ phương thức cần nhập thẻ mới khởi tạo cổng thanh toán', async () => {
    const { facade, gateway } = setup(); const init = vi.spyOn(gateway, 'init');
    await facade.preparePaymentMethod('cash'); expect(init).not.toHaveBeenCalled();
    await facade.preparePaymentMethod('card'); expect(init).toHaveBeenCalledTimes(1);
  });
});
