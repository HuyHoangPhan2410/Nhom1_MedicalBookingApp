/**
 * OBSERVER (Behavioral) - Subject.
 * Publisher (BookingFacade) chỉ biết "phát sự kiện", không biết ai nghe.
 * Subscriber (view, audit log...) đăng ký mà không sửa Publisher  -> SRP, OCP, DIP.
 */
export const EVENTS = Object.freeze({
  APPOINTMENT_BOOKED: 'appointment:booked',
  APPOINTMENT_CANCELLED: 'appointment:cancelled',
  APPOINTMENT_RESCHEDULED: 'appointment:rescheduled',
  PAYMENT_COMPLETED: 'payment:completed',
  SCHEDULE_CHANGED: 'schedule:changed'
});

export class EventBus {
  #handlers = new Map();

  /** Đăng ký observer. Trả về hàm hủy đăng ký (dùng trong onUnmounted). */
  subscribe(event, handler) {
    if (typeof handler !== 'function') throw new TypeError('handler phải là function');
    if (!this.#handlers.has(event)) this.#handlers.set(event, new Set());
    this.#handlers.get(event).add(handler);
    return () => this.unsubscribe(event, handler);
  }

  unsubscribe(event, handler) {
    this.#handlers.get(event)?.delete(handler);
  }

  /** Quan sát mọi sự kiện (dùng cho audit log). */
  subscribeAll(handler) {
    const offs = Object.values(EVENTS).map((e) => this.subscribe(e, (payload) => handler(e, payload)));
    return () => offs.forEach((off) => off());
  }

  /** Một observer lỗi không được làm hỏng các observer khác hay luồng nghiệp vụ. */
  publish(event, payload = {}) {
    for (const handler of [...(this.#handlers.get(event) ?? [])]) {
      try {
        handler(payload);
      } catch (err) {
        console.error(`[EventBus] observer lỗi ở "${event}":`, err);
      }
    }
  }

  listenerCount(event) {
    return this.#handlers.get(event)?.size ?? 0;
  }
}
