/**
 * Composition Root (DIP): nơi DUY NHẤT quyết định lớp cụ thể nào được dùng.
 * View lấy dịch vụ qua useContainer() và chỉ biết abstraction (facade, notifier, bus).
 * Đổi Stripe <-> Mock chỉ cần đổi biến môi trường VITE_PAYMENT_MODE, không sửa view.
 */
import { inject } from 'vue';
import { loadStripe } from '@stripe/stripe-js';
import { EventBus } from '../patterns/behavioral/EventBus';
import { CashPaymentStrategy, CardPaymentStrategy, PaymentStrategyRegistry } from '../patterns/behavioral/PaymentStrategies';
import { StripeGatewayAdapter, MockGatewayAdapter } from '../patterns/structural/PaymentGatewayAdapters';
import { BookingFacade } from '../patterns/structural/BookingFacade';
import {
  AlertNotificationCreator, ConfirmNotificationCreator, AuditNotificationCreator
} from '../patterns/creational/NotificationFactory';
import { MedicalApi, PaymentApi } from '../services/MedicalApi';
import { AdminApi } from '../services/AdminApi';

export const CONTAINER_KEY = Symbol('medbooking.container');
export const useContainer = () => inject(CONTAINER_KEY);

export function createContainer(http, env = {}) {
  const bus = new EventBus();
  const paymentApi = new PaymentApi(http);

  const gateway = env.VITE_PAYMENT_MODE === 'mock'
    ? new MockGatewayAdapter()
    : new StripeGatewayAdapter({ loadStripe, getPublishableKey: () => paymentApi.getPublishableKey() });

  const strategies = new PaymentStrategyRegistry([new CashPaymentStrategy(), new CardPaymentStrategy()]);

  const facade = new BookingFacade({ api: new MedicalApi(http), paymentApi, bus, strategies, gateway });

  // Factory Method: hành vi alert/confirm giữ nguyên như cũ, nhưng đi qua Creator.
  const alertCreator = new AlertNotificationCreator();
  const confirmCreator = new ConfirmNotificationCreator();
  const auditLog = [];
  const auditCreator = new AuditNotificationCreator(auditLog);
  const notifier = {
    error: (message) => alertCreator.notify(message),
    confirm: (message) => confirmCreator.notify(message)
  };

  // Observer: audit log quan sát mọi sự kiện nghiệp vụ mà không sửa Facade.
  bus.subscribeAll((event, payload) => auditCreator.notify(`${event} ${JSON.stringify(payload ?? {})}`));

  return { bus, facade, notifier, auditLog, adminApi: new AdminApi(http) };
}
