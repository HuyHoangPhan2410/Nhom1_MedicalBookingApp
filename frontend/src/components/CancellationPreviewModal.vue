<template>
  <div class="modal-backdrop" @click.self="close" @keydown.esc.stop="close" @keydown.tab="trapFocus">
    <section ref="dialog" class="modal-content cancellation-modal" role="dialog" aria-modal="true"
      aria-labelledby="cancel-title" aria-describedby="cancel-description" tabindex="-1" :aria-busy="loading || submitting">
      <div class="preview-header">
        <div>
          <h3 id="cancel-title">Xem trước hủy lịch</h3>
          <p id="cancel-description">Lịch #{{ appointment.id }} · Bác sĩ {{ appointment.doctorName }}</p>
        </div>
        <button class="close-button" aria-label="Đóng" :disabled="submitting" @click="close">×</button>
      </div>

      <p v-if="loading" role="status" class="loading-state">Đang kiểm tra điều kiện hủy và tính tiền hoàn dự kiến...</p>
      <div v-if="errorMsg" class="error-banner" role="alert">{{ errorMsg }}</div>

      <template v-if="preview && !loading">
        <div :class="['decision-banner', { blocked: !preview.allowed }]" role="status">
          <strong>{{ preview.allowed ? 'Lịch hẹn đủ điều kiện hủy' : 'Chưa thể hủy lịch hẹn này' }}</strong>
          <p v-if="!preview.allowed">{{ preview.reason }}</p>
        </div>
        <dl class="preview-summary">
          <div><dt>Giờ khám</dt><dd>{{ preview.startAt.replace('T', ' ') }}</dd></div>
          <div><dt>Đã thanh toán</dt><dd>{{ money(preview.paidAmount) }}</dd></div>
          <div v-if="preview.allowed" class="refund-total"><dt>Hoàn tiền dự kiến</dt><dd>{{ money(preview.refundAmount) }}</dd></div>
        </dl>
        <ul v-if="preview.allowed" class="refund-notes">
          <li v-for="note in preview.refundNotes" :key="note">{{ note }}</li>
        </ul>
      </template>

      <div class="policy-note">
        <strong>Chính sách hủy lịch</strong>
        <p>Chỉ hủy lịch chưa bắt đầu và còn ít nhất 2 giờ. Hủy dưới 24 giờ: trừ 30%; thanh toán thẻ: trừ thêm tối đa 10.000đ.</p>
        <p><strong>Đây là số tiền ước tính, chưa hoàn tiền qua Stripe.</strong> Việc hoàn tiền thực tế cần được xử lý riêng. Điều kiện hủy sẽ được kiểm tra lại khi xác nhận.</p>
      </div>
      <div class="modal-actions">
        <button class="btn btn-outline" :disabled="submitting" @click="close">Giữ Lịch</button>
        <button v-if="!preview && !loading" class="btn btn-outline" @click="loadPreview">Thử Lại</button>
        <button class="btn btn-danger" :disabled="loading || submitting || !preview?.allowed" @click="confirmCancellation">
          {{ submitting ? 'Đang hủy...' : 'Xác Nhận Hủy Lịch' }}
        </button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { useContainer } from '../core/container';

const { facade } = useContainer();

const props = defineProps({ appointment: { type: Object, required: true } });
const emit = defineEmits(['close', 'success']);
const dialog = ref(null);
const preview = ref(null);
const loading = ref(true);
const submitting = ref(false);
const errorMsg = ref('');
let previousFocus;
let active = true;
const money = (amount) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
const close = () => { if (!submitting.value) emit('close'); };

const loadPreview = async () => {
  loading.value = true;
  preview.value = null;
  errorMsg.value = '';
  try {
    const result = await facade.previewCancellation(props.appointment.id);
    if (active) preview.value = result;
  } catch (err) {
    if (active) errorMsg.value = err.message;
  } finally {
    if (active) loading.value = false;
  }
};

const confirmCancellation = async () => {
  if (loading.value || submitting.value || !preview.value?.allowed) return;
  submitting.value = true;
  errorMsg.value = '';
  try {
    await facade.cancelAppointment(props.appointment.id);
    emit('success');
    emit('close');
  } catch (err) {
    if (active) {
      errorMsg.value = err.message;
      // The snapshot can be stale (time elapsed or status changed). Require a fresh preview.
      preview.value = null;
    }
  } finally {
    if (active) submitting.value = false;
  }
};

const trapFocus = (event) => {
  const buttons = [...dialog.value.querySelectorAll('button:not(:disabled)')];
  const first = buttons[0];
  const last = buttons.at(-1);
  if (event.shiftKey && (document.activeElement === first || document.activeElement === dialog.value)) {
    event.preventDefault();
    last?.focus();
  } else if (!event.shiftKey && (document.activeElement === last || document.activeElement === dialog.value)) {
    event.preventDefault();
    first?.focus();
  }
};
onMounted(() => {
  previousFocus = document.activeElement;
  dialog.value.focus();
  loadPreview();
});
onUnmounted(() => {
  active = false;
  if (previousFocus?.isConnected) previousFocus.focus();
});
</script>

<style scoped>
.cancellation-modal { max-width: 600px; max-height: 90vh; overflow-y: auto; }
.preview-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 1rem; margin-bottom: 1.25rem; }
.preview-header h3 { font-size: 1.25rem; font-weight: 800; }
.preview-header p { margin-top: .4rem; color: var(--text-secondary); font-size: .9rem; }
.close-button { border: 0; background: transparent; font-size: 1.75rem; cursor: pointer; color: var(--text-secondary); }
.loading-state { padding: 1rem 0; color: var(--text-secondary); }
.decision-banner, .error-banner { border-radius: 8px; padding: .9rem 1rem; background: #ecfdf5; color: #065f46; margin-bottom: 1rem; }
.decision-banner.blocked, .error-banner { background: #fff1f2; color: #9f1239; }
.decision-banner p { margin-top: .4rem; }
.preview-summary > div { display: flex; justify-content: space-between; gap: 1rem; padding: .75rem 0; border-bottom: 1px solid var(--border-color); }
.preview-summary dt { color: var(--text-secondary); }
.preview-summary dd { margin: 0; text-align: right; font-weight: 600; }
.refund-total dd { color: var(--primary); font-size: 1.2rem; }
.refund-notes { padding-left: 1.25rem; margin: 1rem 0; color: var(--text-secondary); font-size: .9rem; }
.refund-notes li { margin-bottom: .5rem; }
.policy-note { margin-top: 1rem; padding: 1rem; background: #f8fafc; border-radius: 8px; font-size: .85rem; line-height: 1.6; }
.policy-note p { margin-top: .5rem; }
.modal-actions { display: flex; justify-content: flex-end; flex-wrap: wrap; gap: .75rem; margin-top: 1.5rem; }
button:disabled { opacity: .55; cursor: not-allowed; }
@media (max-width: 480px) { .preview-summary > div { flex-direction: column; gap: .25rem; } .preview-summary dd { text-align: left; } }
</style>
