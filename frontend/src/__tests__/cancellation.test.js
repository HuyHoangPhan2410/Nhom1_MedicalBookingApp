// @vitest-environment jsdom
import { afterEach, describe, expect, it, vi } from 'vitest';
import { flushPromises, mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import { CONTAINER_KEY } from '../core/container';
import { EventBus, EVENTS } from '../patterns/behavioral/EventBus';
import { BookingFacade } from '../patterns/structural/BookingFacade';
import { MedicalApi } from '../services/MedicalApi';
import CancellationPreviewModal from '../components/CancellationPreviewModal.vue';
import PatientProfileView from '../views/PatientProfileView.vue';
import { useAuthStore } from '../stores/auth';

const appointment = {
  id: 10, doctorName: 'Doctor An', status: 'confirmed',
  canCancel: true, canReschedule: true
};
const quote = {
  appointmentId: 10, allowed: true, reason: null,
  startAt: '2026-11-15T10:00:00', paidAmount: 500000,
  refundAmount: 340000, refundNotes: ['base', 'late fee', 'card fee']
};
const wrappers = [];
const track = (wrapper) => { wrappers.push(wrapper); return wrapper; };
afterEach(() => {
  wrappers.splice(0).forEach((wrapper) => wrapper.unmount());
  localStorage.clear();
});

function setup() {
  const http = {
    get: vi.fn(async () => ({ status: 200, data: quote })),
    put: vi.fn(async () => ({ status: 200, data: { ...appointment, status: 'cancelled' } }))
  };
  const bus = new EventBus();
  const facade = new BookingFacade({ api: new MedicalApi(http), bus });
  const cancelled = vi.fn();
  bus.subscribe(EVENTS.APPOINTMENT_CANCELLED, cancelled);
  return { http, bus, facade, cancelled };
}
function mountModal(container) {
  return track(mount(CancellationPreviewModal, {
    props: { appointment }, global: { provide: { [CONTAINER_KEY]: container } }
  }));
}

describe('Cancellation preview integration (API + Facade + Observer)', () => {
  it('only reads on preview, then cancels and publishes exactly once after confirmation', async () => {
    const container = setup();
    const wrapper = mountModal(container);
    await flushPromises();
    expect(container.http.get).toHaveBeenCalledTimes(1);
    expect(container.http.get).toHaveBeenCalledWith('/api/appointments/10/cancellation-preview');
    expect(container.http.put).not.toHaveBeenCalled();
    expect(container.cancelled).not.toHaveBeenCalled();
    expect(wrapper.find('button.btn-danger').element.disabled).toBe(false);
    expect(wrapper.findAll('.refund-notes li')).toHaveLength(3);

    await wrapper.find('button.btn-danger').trigger('click');
    await flushPromises();
    expect(container.http.put).toHaveBeenCalledTimes(1);
    expect(container.http.put).toHaveBeenCalledWith('/api/appointments/10/cancel');
    expect(container.cancelled).toHaveBeenCalledTimes(1);
    expect(container.cancelled).toHaveBeenCalledWith({ appointmentId: 10 });
    expect(wrapper.emitted('success')).toHaveLength(1);
    expect(wrapper.emitted('close')).toHaveLength(1);
  });

  it('shows a rejected preview without permitting cancellation or publishing events', async () => {
    const container = setup();
    container.http.get.mockResolvedValueOnce({ status: 200, data: {
      ...quote, allowed: false, reason: 'Too close to appointment', refundNotes: []
    } });
    const wrapper = mountModal(container);
    await flushPromises();
    expect(wrapper.find('.decision-banner.blocked').text()).toContain('Too close to appointment');
    expect(wrapper.find('button.btn-danger').element.disabled).toBe(true);
    await wrapper.find('button.btn-danger').trigger('click');
    expect(container.http.put).not.toHaveBeenCalled();
    expect(container.cancelled).not.toHaveBeenCalled();
  });

  it('invalidates a stale snapshot on failed cancellation and requires a fresh preview', async () => {
    const container = setup();
    container.http.put.mockRejectedValueOnce(new Error('stale snapshot'));
    const wrapper = mountModal(container);
    await flushPromises();
    await wrapper.find('button.btn-danger').trigger('click');
    await flushPromises();
    expect(wrapper.find('.error-banner').text()).toContain('stale snapshot');
    expect(wrapper.find('button.btn-danger').element.disabled).toBe(true);
    expect(container.cancelled).not.toHaveBeenCalled();
    expect(wrapper.emitted('success')).toBeUndefined();
    expect(wrapper.emitted('close')).toBeUndefined();

    await wrapper.findAll('button.btn-outline').at(-1).trigger('click');
    await flushPromises();
    expect(container.http.get).toHaveBeenCalledTimes(2);
    expect(wrapper.find('button.btn-danger').element.disabled).toBe(false);
    expect(container.http.put).toHaveBeenCalledTimes(1);
  });

  it('handles an unsuccessful API envelope and allows preview retry without writing', async () => {
    const container = setup();
    container.http.get.mockResolvedValueOnce({ status: 400, message: 'preview unavailable' });
    const wrapper = mountModal(container);
    await flushPromises();
    expect(wrapper.find('.error-banner').text()).toContain('preview unavailable');
    expect(wrapper.find('button.btn-danger').element.disabled).toBe(true);
    await wrapper.findAll('button.btn-outline').at(-1).trigger('click');
    await flushPromises();
    expect(container.http.get).toHaveBeenCalledTimes(2);
    expect(wrapper.find('button.btn-danger').element.disabled).toBe(false);
    expect(container.http.put).not.toHaveBeenCalled();
    expect(container.cancelled).not.toHaveBeenCalled();
  });

  it('reloads the patient overview once through Observer when the modal confirms cancellation', async () => {
    const container = setup();
    container.http.get.mockImplementation(async (url) => ({
      status: 200,
      data: url.endsWith('/cancellation-preview') ? quote
        : url.endsWith('/appointments') ? [appointment]
          : { fullName: 'Patient', gender: 'male' }
    }));
    const loadOverview = vi.spyOn(container.facade, 'loadPatientOverview');
    const pinia = createPinia();
    setActivePinia(pinia);
    useAuthStore().saveSession({ token: 'test', userId: 3, role: 'patient' });
    const wrapper = track(mount(PatientProfileView, {
      global: { plugins: [pinia], provide: { [CONTAINER_KEY]: container } }
    }));
    await flushPromises();
    expect(loadOverview).toHaveBeenCalledTimes(1);
    await wrapper.find('.appointment-card button.btn-danger').trigger('click');
    await flushPromises();
    expect(container.http.put).not.toHaveBeenCalled();
    await wrapper.find('.cancellation-modal button.btn-danger').trigger('click');
    await flushPromises();
    expect(container.http.put).toHaveBeenCalledTimes(1);
    expect(container.cancelled).toHaveBeenCalledTimes(1);
    expect(loadOverview).toHaveBeenCalledTimes(2);
    expect(wrapper.find('.cancellation-modal').exists()).toBe(false);
  });
});
