# Backend: 3 pattern bổ sung cho tính năng "Xem trước hủy lịch và hoàn tiền"

Endpoint mới: `GET /api/appointments/{id}/cancellation-preview`
(trả về: được hủy hay không, lý do bị từ chối, số tiền hoàn dự kiến, các dòng giải thích).
Không thay đổi dữ liệu. Lệnh hủy thật `PUT /api/appointments/{id}/cancel` giữ nguyên.

| Nhóm | Pattern | Gói / lớp chính | Vai trò |
|---|---|---|---|
| Behavioral | State | `patterns/cancellation/state` – `AppointmentLifecycle`, `AppointmentState`, `Pending/Confirmed/Cancelled/CompletedState` | Mỗi trạng thái lịch hẹn tự quyết định hành động nào hợp lệ và chuyển sang trạng thái nào |
| Behavioral | Chain of Responsibility | `patterns/cancellation/chain` – `CancellationRuleHandler`, `StatusRuleHandler`, `PastAppointmentRuleHandler`, `MinimumNoticeRuleHandler` | Kiểm tra lần lượt các quy tắc hủy, dừng ở quy tắc đầu tiên bị vi phạm |
| Structural | Decorator | `patterns/cancellation/refund` – `RefundCalculator`, `BaseRefundCalculator`, `LateCancellationFeeDecorator`, `CardProcessingFeeDecorator` | Xếp chồng các khoản khấu trừ lên số tiền hoàn gốc |

Điểm lắp ráp: `CancellationAdvisor` (logic thuần). `CancellationPreviewService` nạp dữ liệu, `CancellationPreviewController` mở endpoint.

## Quy tắc
- Chỉ lịch `pending` / `confirmed` được hủy.
- Không hủy ca khám đã bắt đầu hoặc đã qua; phải báo trước tối thiểu 2 giờ.
- Hoàn đủ nếu hóa đơn đã thanh toán thành công; hủy trong vòng 24 giờ trước giờ khám: khấu trừ 30%; thanh toán thẻ: trừ thêm 10.000đ (không âm).
- Số tiền hoàn là ước tính; hệ thống chưa gọi API hoàn tiền của Stripe.

## Test (22 ca, JUnit 5, không dùng Mockito)
`src/test/java/com/medbooking/patterns/cancellation/`:
`AppointmentStateTest` (5), `CancellationChainTest` (6), `RefundDecoratorTest` (8), `CancellationAdvisorTest` (3).

```bash
cd backend
./mvnw test -Dtest=AppointmentStateTest,CancellationChainTest,RefundDecoratorTest,CancellationAdvisorTest
```
