/**
 * ADAPTER (Structural) - phản chiếu StripePaymentAdapter/PaymentGateway ở backend.
 * Hợp đồng PaymentGateway (cả hai adapter thay thế được nhau - LSP):
 *   init()                -> Promise<{ ready: boolean, message?: string }>
 *   mountCardInput(el)    -> void
 *   charge({ prepare })   -> Promise<void>; resolve nếu thu tiền thành công, ném Error(message) nếu thất bại
 * Phía view chỉ biết hợp đồng này, không biết Stripe.js (DIP).
 */
const CARD_STYLE = { base: { fontSize: '16px', color: '#0f172a', '::placeholder': { color: '#94a3b8' } } };

export class StripeGatewayAdapter {
  /** @param {{loadStripe: Function, getPublishableKey: () => Promise<string>}} deps */
  constructor({ loadStripe, getPublishableKey }) {
    this.loadStripe = loadStripe;
    this.getPublishableKey = getPublishableKey;
    this.stripe = null;
    this.card = null;
  }

  async init() {
    if (this.stripe) return { ready: true };
    try {
      const key = await this.getPublishableKey();
      if (!key || key === 'pk_test_placeholder') {
        return { ready: false, message: 'Stripe chưa được cấu hình. Vui lòng chọn Tiền mặt.' };
      }
      this.stripe = await this.loadStripe(key);
      return { ready: true };
    } catch {
      return { ready: false, message: 'Không thể tải Stripe. Vui lòng thử lại.' };
    }
  }

  mountCardInput(el) {
    if (!this.stripe || !el) return;
    this.card = this.stripe.elements().create('card', { style: CARD_STYLE });
    this.card.mount(el);
  }

  async charge({ prepare }) {
    if (!this.stripe || !this.card) throw new Error('Stripe chưa sẵn sàng. Vui lòng chọn Tiền mặt.');
    const clientSecret = await prepare();
    if (!clientSecret) throw new Error('Không tạo được PaymentIntent');
    const { error } = await this.stripe.confirmCardPayment(clientSecret, { payment_method: { card: this.card } });
    if (error) throw new Error(error.message);
  }
}

/** Cổng giả lập để demo/test khi chưa có khóa Stripe. Cùng hợp đồng với StripeGatewayAdapter. */
export class MockGatewayAdapter {
  constructor({ shouldFail = false } = {}) { this.shouldFail = shouldFail; this.charges = 0; }
  async init() { return { ready: true }; }
  mountCardInput(el) {
    if (el) el.textContent = '💳 Chế độ giả lập (mock) - không cần nhập thẻ thật';
  }
  async charge() {
    if (this.shouldFail) throw new Error('Thẻ bị từ chối (mock)');
    this.charges += 1;
  }
}
