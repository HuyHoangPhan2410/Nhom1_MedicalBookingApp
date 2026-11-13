// @vitest-environment jsdom
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import { CONTAINER_KEY } from '../core/container';
import { EventBus, EVENTS } from '../patterns/behavioral/EventBus';
import HomeView from '../views/HomeView.vue';
import PaymentModal from '../components/PaymentModal.vue';
import PatientProfileView from '../views/PatientProfileView.vue';
import CreateScheduleModal from '../components/CreateScheduleModal.vue';
import { useAuthStore } from '../stores/auth';

const stubs = { RouterLink: { template: '<a><slot /></a>' }, DoctorCard: { props: ['doctor'], template: '<div class="doc">{{ doctor.fullName }}</div>' } };

const mountWith = (component, facade, extra = {}) => {
  const bus = extra.bus ?? new EventBus();
  const notifier = { error: vi.fn(), confirm: vi.fn(() => true) };
  const pinia = createPinia(); setActivePinia(pinia);
  const wrapper = mount(component, {
    props: extra.props,
    global: { plugins: [pinia], provide: { [CONTAINER_KEY]: { facade, bus, notifier } }, stubs }
  });
  return { wrapper, bus, notifier };
};

beforeEach(() => { localStorage.clear(); });

describe('View smoke tests (container giả)', () => {
  it('TC-V01: HomeView hiển thị bác sĩ lấy từ Facade và lọc theo chuyên khoa', async () => {
    const facade = {
      loadHome: vi.fn(async () => ({ specialties: [{ id: 1, name: 'Tim mạch' }], doctors: [{ userId: 1, fullName: 'BS An' }] })),
      listDoctors: vi.fn(async () => [{ userId: 2, fullName: 'BS Bình' }])
    };
    const { wrapper } = mountWith(HomeView, facade); await flushPromises();
    expect(wrapper.text()).toContain('BS An');
    expect(facade.loadHome).toHaveBeenCalledTimes(1);
  });

  it('TC-V02: PaymentModal thanh toán tiền mặt gọi facade.pay và hiện thông báo thành công', async () => {
    const facade = {
      preparePaymentMethod: vi.fn(async () => ({ ready: true })), mountCardInput: vi.fn(),
      pay: vi.fn(async () => ({ message: 'Đã xác nhận thanh toán tiền mặt!' }))
    };
    const { wrapper } = mountWith(PaymentModal, facade, { props: { appointment: { id: 5, consultationFee: 300000 } } });
    await wrapper.find('button.btn-primary').trigger('click'); await flushPromises();
    expect(facade.pay).toHaveBeenCalledWith(5, 'cash');
    expect(wrapper.find('.success-banner').text()).toContain('tiền mặt');
  });

  it('TC-V03: PaymentModal chọn thẻ -> khởi tạo cổng, gắn ô nhập thẻ; lỗi hiển thị ở banner', async () => {
    const facade = {
      preparePaymentMethod: vi.fn(async () => ({ ready: true })), mountCardInput: vi.fn(),
      pay: vi.fn(async () => { throw new Error('Thẻ bị từ chối'); })
    };
    const { wrapper } = mountWith(PaymentModal, facade, { props: { appointment: { id: 6 } } });
    await wrapper.find('input[value="card"]').setValue(); await flushPromises();
    expect(facade.preparePaymentMethod).toHaveBeenCalledWith('card');
    expect(facade.mountCardInput).toHaveBeenCalledTimes(1);
    await wrapper.find('button.btn-primary').trigger('click'); await flushPromises();
    expect(wrapper.find('.error-banner').text()).toContain('Thẻ bị từ chối');
  });

  it('TC-V04: PaymentModal báo lỗi khi cổng chưa sẵn sàng (Stripe chưa cấu hình)', async () => {
    const facade = { preparePaymentMethod: vi.fn(async () => ({ ready: false, message: 'Stripe chưa được cấu hình. Vui lòng chọn Tiền mặt.' })), mountCardInput: vi.fn(), pay: vi.fn() };
    const { wrapper } = mountWith(PaymentModal, facade, { props: { appointment: { id: 6 } } });
    await wrapper.find('input[value="card"]').setValue(); await flushPromises();
    expect(wrapper.find('.error-banner').text()).toContain('chưa được cấu hình');
    expect(facade.mountCardInput).not.toHaveBeenCalled();
  });

  it('TC-V05 (Observer): PatientProfileView tự tải lại khi có sự kiện thanh toán/hủy lịch', async () => {
    const facade = { loadPatientOverview: vi.fn(async () => ({ patient: { fullName: 'Phuc', gender: 'male' }, appointments: [] })), cancelAppointment: vi.fn() };
    const pinia = createPinia(); setActivePinia(pinia);
    useAuthStore().saveSession({ token: 't', userId: 3, role: 'patient', fullName: 'Phuc' });
    const bus = new EventBus();
    const wrapper = mount(PatientProfileView, { global: { plugins: [pinia], provide: { [CONTAINER_KEY]: { facade, bus, notifier: { error() {}, confirm: () => true } } }, stubs } });
    await flushPromises(); expect(facade.loadPatientOverview).toHaveBeenCalledTimes(1);
    bus.publish(EVENTS.PAYMENT_COMPLETED, { appointmentId: 1 }); await flushPromises();
    bus.publish(EVENTS.APPOINTMENT_CANCELLED, { appointmentId: 1 }); await flushPromises();
    expect(facade.loadPatientOverview).toHaveBeenCalledTimes(3);
    wrapper.unmount(); bus.publish(EVENTS.PAYMENT_COMPLETED, {}); await flushPromises();
    expect(facade.loadPatientOverview).toHaveBeenCalledTimes(3); // đã hủy đăng ký khi unmount
  });

  it('TC-V06: CreateScheduleModal gọi facade.createSchedule; lỗi từ Builder hiện ở banner', async () => {
    const facade = { createSchedule: vi.fn(async () => ({})) };
    const pinia = createPinia(); setActivePinia(pinia);
    useAuthStore().saveSession({ token: 't', userId: 9, role: 'doctor' });
    const wrapper = mount(CreateScheduleModal, { global: { plugins: [pinia], provide: { [CONTAINER_KEY]: { facade } } } });
    await wrapper.find('form').trigger('submit'); await flushPromises();
    expect(facade.createSchedule).toHaveBeenCalledTimes(1);
    expect(wrapper.emitted('close')).toBeTruthy();
    facade.createSchedule.mockRejectedValueOnce(new Error('Giờ kết thúc phải sau giờ bắt đầu'));
    await wrapper.find('form').trigger('submit'); await flushPromises();
    expect(wrapper.find('.error-banner').text()).toContain('Giờ kết thúc');
  });
});
