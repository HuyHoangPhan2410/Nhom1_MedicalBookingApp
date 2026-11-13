/**
 * FACTORY METHOD (Creational) - phản chiếu BookingNotificationCreator ở backend.
 * Creator định nghĩa quy trình cố định (notify), lớp con quyết định TẠO loại thông báo nào.
 */
export class Notification {
  constructor(message) { this.message = message; }
  /** @returns {*} kết quả hiển thị (confirm trả boolean) */
  show() { throw new Error('Notification.show() chưa được hiện thực'); }
}

export class AlertNotification extends Notification {
  show() { window.alert(this.message); return true; }
}

export class ConfirmNotification extends Notification {
  show() { return window.confirm(this.message); }
}

export class AuditNotification extends Notification {
  constructor(message, sink) { super(message); this.sink = sink; }
  show() { this.sink.push({ at: new Date().toISOString(), message: this.message }); return true; }
}

/** Creator trừu tượng: notify() là quy trình cố định, createNotification() là factory method. */
export class NotificationCreator {
  createNotification(/* message */) { throw new Error('createNotification() chưa được hiện thực'); }

  notify(message) {
    if (!message) throw new Error('Nội dung thông báo không được rỗng');
    return this.createNotification(String(message)).show();
  }
}

export class AlertNotificationCreator extends NotificationCreator {
  createNotification(message) { return new AlertNotification(message); }
}

export class ConfirmNotificationCreator extends NotificationCreator {
  createNotification(message) { return new ConfirmNotification(message); }
}

export class AuditNotificationCreator extends NotificationCreator {
  constructor(sink = []) { super(); this.sink = sink; }
  createNotification(message) { return new AuditNotification(message, this.sink); }
}
