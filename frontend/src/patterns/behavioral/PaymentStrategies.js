/**
 * STRATEGY (Behavioral) - phản chiếu PaymentPreparationStrategy ở backend.
 * Mỗi phương thức thanh toán là một Strategy; Registry chọn theo key.
 * Thêm phương thức mới = thêm 1 lớp + register(), KHÔNG sửa PaymentModal hay BookingFacade (OCP).
 *
 * Hợp đồng: method (string), needsCardInput (bool),
 *           execute({ appointmentId, api, gateway }) -> Promise<{ message: string }>
 */
export class CashPaymentStrategy {
  method = 'cash';
  needsCardInput = false;
  async execute({ appointmentId, api }) {
    await api.confirmPayment(appointmentId, 'cash');
    return { message: 'Đã xác nhận thanh toán tiền mặt!' };
  }
}

export class CardPaymentStrategy {
  method = 'card';
  needsCardInput = true;
  async execute({ appointmentId, api, gateway }) {
    await gateway.charge({ prepare: () => api.createPaymentIntent(appointmentId) });
    await api.confirmPayment(appointmentId, 'card');
    return { message: 'Thanh toán thẻ thành công!' };
  }
}

export class PaymentStrategyRegistry {
  #strategies = new Map();
  constructor(strategies = []) { strategies.forEach((s) => this.register(s)); }

  register(strategy) {
    if (!strategy?.method || typeof strategy.execute !== 'function') {
      throw new TypeError('Strategy phải có method và execute()');
    }
    this.#strategies.set(strategy.method, strategy);
    return this;
  }

  forMethod(method) {
    const s = this.#strategies.get(method);
    if (!s) throw new Error(`Phương thức thanh toán không hợp lệ: ${method}`);
    return s;
  }

  methods() { return [...this.#strategies.keys()]; }
}
