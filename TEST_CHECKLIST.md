# Kiểm thử MedicalBookingApp: kịch bản và hướng dẫn chạy

Tài liệu này gom toàn bộ phần kiểm thử của đồ án (nhóm 1, môn Mẫu thiết kế phần mềm) vào một chỗ: cách dựng môi trường, cách chạy test tự động, danh sách ca test, kịch bản demo thủ công và các giới hạn đã biết. Đặt tại `docs/TEST_CHECKLIST.md`.

## 1. Tổng quan

Có ba lớp kiểm thử:

| Lớp | Công cụ | Số ca | Chạy ở đâu |
|---|---|---|---|
| Backend tự động | JUnit 5 (+ Mockito cho các test cũ) | **61** (39 test ban đầu + 22 test của ba mẫu bổ sung) | `backend/src/test/java` |
| Frontend tự động | Vitest | **35** (29 test mẫu thiết kế + 6 test gắn view/modal) | `frontend/src/__tests__` |
| Thủ công (demo) | Trình duyệt, Swagger UI | **15** kịch bản (M01–M15) | Mục 5 |

Chín mẫu thiết kế được phủ như sau:

| Nhóm | Mẫu | Backend | Frontend |
|---|---|---|---|
| Creational | Builder | `AppointmentBookedEvent.builder()` | `RequestBuilders.js` |
| Creational | Factory Method | `BookingNotificationCreator` | `NotificationFactory.js` |
| Structural | Adapter | `StripePaymentAdapter` / `PaymentGateway` | `PaymentGatewayAdapters.js` |
| Structural | Facade | `AppointmentOverviewFacade` | `BookingFacade.js` |
| Structural | Decorator | `RefundCalculator` và hai decorator phí | (không có) |
| Behavioral | Strategy | `PaymentPreparationStrategy` | `PaymentStrategies.js` |
| Behavioral | Observer | `AppointmentNotificationListener` | `EventBus.js` |
| Behavioral | State | `AppointmentLifecycle` / `AppointmentState` | (không có) |
| Behavioral | Chain of Responsibility | `CancellationRuleHandler` | (không có) |

Ba mẫu cuối (Decorator, State, Chain of Responsibility) chỉ có ở backend, phục vụ tính năng **xem trước hủy lịch và hoàn tiền** (`GET /api/appointments/{id}/cancellation-preview`).

## 2. Chuẩn bị môi trường

### 2.1. Chạy toàn bộ hệ thống bằng Docker

```bash
git clone https://github.com/HuyHoangPhan2410/Nhom1_MedicalBookingApp/
cd Nhom1_MedicalBookingApp
docker compose up --build -d
docker compose ps          # db, backend, frontend đều phải ở trạng thái Up
```

| Thành phần | Địa chỉ |
|---|---|
| Frontend (Vue 3 + Nginx) | http://localhost:8081 |
| Backend (Spring Boot) | http://localhost:8082 |
| Swagger UI | http://localhost:8082/swagger-ui.html |
| OpenAPI JSON | http://localhost:8082/v3/api-docs |
| Trang quản trị | http://localhost:8081/admin/login |
| MySQL 8 | localhost:3306 (database `MedicalBookingDB`) |

### 2.2. Chưa có khóa Stripe

Backend dùng khóa mẫu `sk_test_placeholder` nên thanh toán thẻ thật sẽ báo "Stripe chưa được cấu hình". Để demo thanh toán thẻ bằng cổng giả lập (Mock adapter):

```bash
# Linux / macOS
VITE_PAYMENT_MODE=mock docker compose up --build -d
```

```powershell
# Windows PowerShell
$env:VITE_PAYMENT_MODE="mock"; docker compose up --build -d
```

Muốn thử Stripe thật, đặt `STRIPE_SECRET_KEY` và `STRIPE_PUBLISHABLE_KEY` (khóa test) trong tệp `.env` trước khi chạy. Thẻ thử: `4242 4242 4242 4242`.

### 2.3. Gửi email OTP (tùy chọn)

Đăng ký bệnh nhân mới cần OTP qua Gmail. Bật xác minh 2 bước, tạo Gmail App Password, rồi tạo `.env` từ `.env.example`:

```env
GMAIL_USERNAME=your_gmail@gmail.com
GMAIL_APP_PASSWORD=your_16_character_app_password
```

Sau đó chạy lại `docker compose up --build -d`. OTP chỉ lưu dạng BCrypt hash, hiệu lực 5 phút, gửi lại tối đa 5 lần trong một giờ. Nếu không cấu hình, hãy dùng các tài khoản mẫu bên dưới.

### 2.4. Tài khoản mẫu (mật khẩu chung `password123`)

| Vai trò | Email |
|---|---|
| Bệnh nhân | `patient.an@gmail.com` |
| Bệnh nhân | `patient.bich@gmail.com` |
| Bác sĩ | `dr.hung@hospital.com` |
| Bác sĩ | `dr.mai@hospital.com` |
| Admin | `admin@medbooking.com` (đăng nhập ở `/admin/login`) |

### 2.5. Dữ liệu mẫu cần nhớ khi test

| Lịch hẹn | Bệnh nhân | Trạng thái | Hóa đơn |
|---|---|---|---|
| ID 1 | patient.an (13) | `confirmed` | 500.000đ, tiền mặt, `success` |
| ID 2 | patient.bich (14) | `pending` | 450.000đ, thẻ, `pending` (chưa trả) |

Hai lưu ý quan trọng:

- Lịch `pending` quá 15 phút sẽ bị event scheduler của MySQL (`evt_CleanupPendingAppointments`, chạy mỗi 5 phút) tự hủy. Hãy thanh toán hoặc test ngay sau khi đặt, nếu không lịch sẽ chuyển `cancelled` giữa chừng.
- Kết quả xem trước hủy lịch phụ thuộc giờ hiện tại so với giờ khám. Nếu ca khám mẫu đã qua, hệ thống từ chối với lý do "đã bắt đầu". Khi đó hãy tạo ca mới bằng tài khoản bác sĩ (kịch bản M11) rồi đặt lịch vào ca đó.

## 3. Chạy test tự động

### 3.1. Backend (cần Java 21 và kết nối tới Maven Central)

```bash
cd backend
./mvnw test                 # Windows: mvnw.cmd test
```

Chỉ chạy bốn lớp test của ba mẫu bổ sung (22 ca):

```bash
./mvnw test -Dtest=AppointmentStateTest,CancellationChainTest,RefundDecoratorTest,CancellationAdvisorTest
```

Kết quả mong đợi khi chạy đầy đủ: **61 test, 0 lỗi, 0 bỏ qua**.

### 3.2. Frontend (cần Node.js)

```bash
cd frontend
npm install
npm test                    # 35 test: 29 test mẫu thiết kế + 6 test view/modal
```

### 3.3. Danh mục test backend theo tệp (tổng 61)

| Tệp test | Số ca | Nội dung chính |
|---|---|---|
| `patterns/PaymentPatternsTest` | 4 | Strategy thẻ/tiền mặt, Registry, Adapter Stripe thiếu khóa |
| `patterns/PaymentServicePatternIntegrationTest` | 1 | `preparePayment` đi qua Registry, từ chối phương thức lạ |
| `patterns/AppointmentBookedEventBuilderTest` | 2 | Builder hợp lệ và các trường hợp bị từ chối |
| `patterns/BookingNotificationCreatorTest` | 2 | Factory Method: kênh email và kênh audit |
| `patterns/AppointmentOverviewFacadeTest` | 1 | Facade gộp lịch hẹn và hóa đơn |
| `event/AppointmentNotificationListenerTest` | 2 | Observer gửi email, lỗi email không lan ra ngoài |
| `patterns/cancellation/AppointmentStateTest` | 5 | State |
| `patterns/cancellation/CancellationChainTest` | 6 | Chain of Responsibility |
| `patterns/cancellation/RefundDecoratorTest` | 8 | Decorator |
| `patterns/cancellation/CancellationAdvisorTest` | 3 | Tích hợp ba mẫu |
| `service/AppointmentServiceTest` | 6 | Đặt, hủy, đổi lịch, tra cứu |
| `service/AuthServiceTest` | 6 | Đăng ký, đăng nhập, OTP |
| `service/ScheduleServiceTest` | 4 | Ca khám của bác sĩ |
| `service/DoctorServiceTest` | 4 | Hồ sơ và lọc bác sĩ |
| `service/SpecialtyServiceTest` | 2 | Chuyên khoa |
| `service/PaymentServiceTest` | 1 | Xác nhận thanh toán qua stored procedure |
| `service/EmailVerificationServiceTest` | 1 | Chặn gửi lại OTP lần thứ 6 trong một giờ |
| `service/AppointmentEmailServiceTest` | 1 | Nội dung email, escape dữ liệu người dùng |
| `controller/admin/AdminUserControllerTest` | 1 | Đổi bệnh nhân thành bác sĩ tạo đủ hồ sơ |
| `MedBookingApplicationTests` | 1 | `contextLoads` |
| **Tổng** | **61** | 39 test ban đầu + 22 test ba mẫu bổ sung |

## 4. Ca test tự động theo mẫu thiết kế

### 4.1. Ba mẫu bổ sung: 22 ca, JUnit 5, không dùng Mockito

Gói `src/test/java/com/medbooking/patterns/cancellation/`. Số tiền trong ví dụ lấy hóa đơn gốc 500.000đ.

**State** (`AppointmentStateTest`, 5 ca)

| Mã | Kịch bản | Kết quả mong đợi |
|---|---|---|
| TC-ST01 | Vòng đời đầy đủ | Trạng thái lần lượt `confirmed`, `completed` |
| TC-ST02 | Trạng thái cho hủy và đổi lịch | `canCancel()` và `canReschedule()` đều đúng, không có lý do chặn |
| TC-ST03 | Trạng thái kết thúc | Không hủy, không đổi được; `cancel()`, `confirm()`, `complete()` ném `BusinessException` |
| TC-ST04 | Không nhảy cóc trạng thái | `BusinessException` (phải xác nhận trước) |
| TC-ST05 | Hủy hai lần | Lần 1 sang `cancelled`; lần 2 ném `BusinessException` |

**Chain of Responsibility** (`CancellationChainTest`, 6 ca)

| Mã | Kịch bản | Kết quả mong đợi |
|---|---|---|
| TC-CR01 | Mọi quy tắc thỏa | Chuỗi trả "được phép" |
| TC-CR02 | Quy tắc trạng thái chặn trước | Từ chối với lý do "đã được hủy trước đó", không phải lý do về giờ |
| TC-CR03 | Ca khám đã bắt đầu | Từ chối, lý do nêu "đã bắt đầu" |
| TC-CR04 | Báo trước chưa đủ | Từ chối, lý do nêu "2 giờ" |
| TC-CR05 | Biên đúng 2 giờ | Được phép |
| TC-CR06 | Thêm quy tắc mới không sửa mã cũ (OCP) | Handler mới được gọi đúng 1 lần, chuỗi trả "từ chối" với lý do mới |

**Decorator** (`RefundDecoratorTest`, 8 ca)

| Mã | Kịch bản | Kết quả mong đợi |
|---|---|---|
| TC-DE01 | Thành phần gốc, đã thanh toán | Hoàn 500.000đ |
| TC-DE02 | Thành phần gốc, chưa thanh toán | Hoàn 0 |
| TC-DE03 | Hủy sớm, tiền mặt | Hoàn 500.000đ, 1 dòng giải thích |
| TC-DE04 | Hủy sát giờ, tiền mặt | Hoàn 350.000đ (trừ 30%), 2 dòng giải thích |
| TC-DE05 | Hủy sớm, thanh toán thẻ | Hoàn 490.000đ (trừ 10.000đ phí cổng) |
| TC-DE06 | Hủy sát giờ, thanh toán thẻ | Hoàn 340.000đ, 3 dòng giải thích (xếp chồng hai lớp bọc) |
| TC-DE07 | Phí không làm số tiền âm | Hoàn 0, không âm |
| TC-DE08 | Không khấu trừ khi không có gì để hoàn | Hoàn 0, chỉ 1 dòng giải thích |

**Tích hợp** (`CancellationAdvisorTest`, 3 ca)

| Mã | Kịch bản | Kết quả mong đợi |
|---|---|---|
| TC-AD01 | Được hủy | `cancellable = true`, hoàn 340.000đ, 3 dòng giải thích, trạng thái `confirmed` |
| TC-AD02 | Bị từ chối | `cancellable = false`, có lý do, hoàn 0, không có dòng giải thích |
| TC-AD03 | Chưa thanh toán | `cancellable = true`, hoàn 0 |

### 4.2. Sáu mẫu ban đầu ở backend: đối chiếu mã test với kịch bản

Các test của sáu mẫu ban đầu không gắn mã `TC-` trong mã nguồn. Bảng dưới đối chiếu từng kịch bản (mã dùng trong báo cáo, mục 4.1) với phương thức test thực tế. Cột cuối nói thẳng kịch bản nào **đã có** test và kịch bản nào **chưa có** test tự động tương ứng.

| Mã | Kịch bản | Phương thức test | Phủ bởi test tự động |
|---|---|---|---|
| TC-B01 | Dựng sự kiện hợp lệ | `AppointmentBookedEventBuilderTest.builderCreatesEventWithRequiredAndOptionalFields` | Có |
| TC-B02 | Mã lịch không dương | `...builderRejectsInvalidDatesAndMissingRequiredFields` (ca 1) | Có |
| TC-B03 | Email rỗng | (không có) | **Chưa** |
| TC-B04 | Thiếu ngày khám | cùng phương thức, ca 2 (`NullPointerException`) | Có |
| TC-B05 | Giờ bắt đầu không trước giờ kết thúc | cùng phương thức, ca 3 | Có |
| TC-F01 | Creator email ủy quyền | `BookingNotificationCreatorTest.emailCreatorMakesEmailNotification` | Có |
| TC-F02 | Creator audit không gửi email | `...alternateCreatorUsesAuditInsteadOfMail` | Một phần (chỉ kiểm tra không ném lỗi) |
| TC-F03 | Thêm kênh thứ ba không sửa mã cũ | (không có) | **Chưa** |
| TC-A01 | Chưa cấu hình khóa Stripe | `PaymentPatternsTest.stripeAdapterRefusesMissingCredentialsBeforeExternalCall` | Có |
| TC-A02 | Khóa mẫu `sk_test_placeholder` | (không có) | **Chưa** |
| TC-A03 | Số tiền không hợp lệ (400) | (không có) | **Chưa** |
| TC-FA01 | Facade gộp dữ liệu | `AppointmentOverviewFacadeTest.facadeCombinesAppointmentAndPaymentIntoOneView` | Có |
| TC-FA02 | Thiếu hóa đơn (404 được truyền lên) | (không có) | **Chưa** |
| TC-S01 | Strategy tiền mặt | `PaymentPatternsTest.cashStrategyDoesNotCallOnlineProviderOrMarkPaid` | Có |
| TC-S02 | Strategy thẻ | `...cardStrategyCallsGatewayAdapterInterfaceAndPersistsIntent` | Có |
| TC-S03 | Registry | `...strategyRegistrySwitchesAlgorithmsAndRejectsUnsupportedOrDuplicate` | Có |
| TC-S04 | Đường đi qua Service | `PaymentServicePatternIntegrationTest.prepareCashUsesCashStrategyWithoutExternalGateway` | Có |
| TC-S05 | Từ chối đầu vào không hợp lệ | cùng phương thức (chỉ kiểm tra phương thức `momo` không hỗ trợ) | Một phần |
| TC-O01 | Listener ủy quyền | `AppointmentNotificationListenerTest.listenerSendsConfirmationEmail` | Có |
| TC-O02 | Cô lập lỗi gửi thông báo | `...listenerDoesNotPropagateMailFailure` | Có |
| TC-O03 | Gắn đúng pha `AFTER_COMMIT` | (không có test; chú thích có trong `AppointmentNotificationListener`) | **Chưa** (xác nhận bằng đọc mã) |
| TC-O04 | Service phát sự kiện đặt lịch | `AppointmentServiceTest` (ca đặt lịch thành công, kiểm tra `publishEvent`) | Có |
| TC-O05 | Bệnh nhân không có email | (không có) | **Chưa** |
| TC-O06 | Đặt lịch thất bại ở cơ sở dữ liệu | `AppointmentServiceTest` (ca hết chỗ, `BusinessException`) | Có |

Tóm lại: 15 trên 24 kịch bản có test tương ứng, 2 kịch bản chỉ phủ một phần (TC-F02, TC-S05) và **7 kịch bản chưa có test tự động** (TC-B03, TC-F03, TC-A02, TC-A03, TC-FA02, TC-O03, TC-O05). Nên viết bổ sung các ca này trước khi ghi "24/24 đạt" trong báo cáo.

### 4.3. Frontend: 35 ca Vitest

`frontend/src/__tests__/patterns.test.js` (29 ca) và `views.test.js` (6 ca). Mã ca ở frontend trùng tiền tố với backend nhưng là các ca khác; phân biệt bằng tầng.

| Mẫu | Mã ca | Nội dung |
|---|---|---|
| Observer | TC-O01–O04 | `publish` gọi mọi observer; hủy đăng ký; một observer lỗi không làm hỏng observer khác; `subscribeAll` |
| Factory Method | TC-F01–F04 | Alert, Confirm, Audit; thông báo rỗng bị từ chối; Creator trừu tượng bắt buộc hiện thực |
| Builder | TC-B01–B04 | Yêu cầu hợp lệ và bất biến; thiếu ca khám; chuẩn hóa `HH:mm` sang `HH:mm:ss`; giờ kết thúc sau giờ bắt đầu, sức chứa lớn hơn 0 |
| Adapter | TC-A01–A04 | Stripe chưa có khóa; bọc `confirmCardPayment`; Mock và Stripe cùng hợp đồng (LSP); lỗi rõ ràng khi chưa sẵn sàng |
| Strategy | TC-S01–S04 | Cash chỉ xác nhận với backend; Card thu qua gateway rồi mới xác nhận; thẻ bị từ chối thì không xác nhận; thêm phương thức mới chỉ cần `register` (OCP) |
| Facade | TC-FA01–FA09 | Gom API; đặt lịch dùng Builder rồi phát sự kiện; dữ liệu sai bị chặn, không gọi API; hủy và đổi lịch phát sự kiện; chỉ liệt kê ca khác ca hiện tại; tạo ca khám; thanh toán tiền mặt và thẻ (Mock); chỉ khởi tạo cổng khi cần nhập thẻ |
| View (smoke) | TC-V01–V06 | `HomeView` lọc theo chuyên khoa; `PaymentModal` tiền mặt, thẻ, lỗi cổng; `PatientProfileView` tự tải lại khi có sự kiện (Observer); `CreateScheduleModal` |

## 5. Kịch bản test thủ công (demo)

Điều kiện chung: hệ thống đã chạy theo mục 2; dùng tài khoản mẫu ở mục 2.4. Cột "Kết quả" để trống cho người test ghi **Đạt** hoặc **Lỗi**.

| Mã | Kịch bản | Kết quả mong đợi | Mẫu | Kết quả |
|---|---|---|---|---|
| M01 | Mở trang chủ, lọc theo chuyên khoa, tìm theo tên | Danh sách bác sĩ đổi theo bộ lọc | Facade | |
| M02 | Đăng nhập bệnh nhân, vào trang bác sĩ, chọn ngày và ca, nhập triệu chứng, bấm Đặt lịch | Chuyển sang `/profile`, lịch mới ở trạng thái "Chờ thanh toán" | Builder, Facade, Observer | |
| M03 | Đặt lịch khi chưa chọn ca | Banner lỗi "Vui lòng chọn ca khám", không gọi API | Builder | |
| M04 | Thanh toán tiền mặt | Banner thành công, modal đóng, lịch thành "Đã xác nhận" (tự cập nhật) | Strategy, Observer | |
| M05 | Thanh toán thẻ (chế độ mock) | Ô thẻ hiện "Chế độ giả lập", thanh toán thành công | Adapter (Mock) | |
| M06 | Thanh toán thẻ Stripe, thẻ `4242 4242 4242 4242` | Thành công; thẻ sai thì hiện lỗi từ Stripe | Adapter (Stripe) | |
| M07 | Chọn thẻ nhưng chưa cấu hình Stripe | Banner "Stripe chưa được cấu hình. Vui lòng chọn Tiền mặt." | Adapter | |
| M08 | Đổi lịch sang ca khác | Danh sách không có ca hiện tại; sau khi đổi, danh sách tự cập nhật | Facade, Observer | |
| M09 | Hủy lịch (xác nhận hộp thoại) | Lịch chuyển "Đã hủy" | Factory Method, Observer | |
| M10 | Bác sĩ tạo ca khám với giờ kết thúc trước giờ bắt đầu | Banner lỗi "Giờ kết thúc phải sau giờ bắt đầu" | Builder | |
| M11 | Bác sĩ tạo, sửa, hủy ca khám | Bảng tuần tự cập nhật không cần tải lại trang | Facade, Observer | |
| M12 | Admin: đăng nhập, xem thống kê, CRUD người dùng/bác sĩ/bệnh nhân, đổi trạng thái lịch | Hoạt động như trước | `AdminApi` (DIP) | |
| M13 | Mở DevTools (F12), tab Console, thao tác M01–M11 | Không có lỗi đỏ | – | |
| M14 | Xem trước hủy lịch được phép (Swagger UI, mục 6.3, ca a–c) | `cancellable = true` và số tiền hoàn đúng quy tắc ở mục 7; dữ liệu không đổi | State, Chain of Responsibility, Decorator | |
| M15 | Xem trước hủy lịch bị từ chối (Swagger UI, mục 6.3, ca d–f) | `cancellable = false`, có `rejectReason`, hoàn 0; mã 404 khi lịch không tồn tại | Chain of Responsibility, State | |

## 6. Kiểm thử API nhanh

Mọi phản hồi có dạng `{ "status": 200, "message": "...", "data": ... }`. API hiện **chưa dùng JWT**, nên gọi trực tiếp được từ Swagger UI hoặc `curl`.

### 6.1. Tổng quan lịch hẹn (Facade)

```bash
curl http://localhost:8082/api/appointments/1/overview
```

Mong đợi: `data` chứa cả `appointment` và `payment` của lịch 1 trong một lần gọi.

### 6.2. Chuẩn bị thanh toán (Strategy)

```bash
curl -X POST "http://localhost:8082/api/payments/prepare?appointmentId=2&method=cash"
curl -X POST "http://localhost:8082/api/payments/prepare?appointmentId=2&method=card"
curl -X POST "http://localhost:8082/api/payments/prepare?appointmentId=2&method=momo"
```

| Lệnh | Kết quả mong đợi |
|---|---|
| `method=cash` | `paymentMethod = cash`, `status = pending`, có hướng dẫn thanh toán; không gọi cổng ngoài, chưa đánh dấu đã trả |
| `method=card` | Trả `clientSecret` và `paymentIntentId`. Nếu chưa có khóa Stripe: lỗi 500 "Stripe chưa được cấu hình" |
| `method=momo` | Lỗi 400, phương thức không được hỗ trợ |

### 6.3. Xem trước hủy lịch và hoàn tiền (State, Chain of Responsibility, Decorator)

```bash
curl http://localhost:8082/api/appointments/{id}/cancellation-preview
```

Trường trả về: `appointmentId`, `status`, `cancellable`, `rejectReason`, `refundAmount`, `refundNotes`.

Ví dụ phản hồi khi được hủy (thanh toán thẻ, hủy sát giờ):

```json
{
  "status": 200,
  "message": "Success",
  "data": {
    "appointmentId": 1,
    "status": "confirmed",
    "cancellable": true,
    "rejectReason": null,
    "refundAmount": 340000,
    "refundNotes": ["...", "...", "..."]
  }
}
```

Các ca nên thử (giả sử hóa đơn 500.000đ, đã thanh toán thành công):

| Ca | Tình huống | Kết quả mong đợi |
|---|---|---|
| a | Tiền mặt, ca khám còn hơn 24 giờ | `cancellable = true`, hoàn 500.000, 1 dòng giải thích |
| b | Tiền mặt, ca khám còn từ 2 đến 24 giờ | `cancellable = true`, hoàn 350.000, 2 dòng giải thích |
| c | Thẻ, ca khám còn từ 2 đến 24 giờ | `cancellable = true`, hoàn 340.000, 3 dòng giải thích |
| d | Lịch đã `cancelled` hoặc `completed` | `cancellable = false`, lý do về trạng thái, hoàn 0 |
| e | Ca khám còn dưới 2 giờ hoặc đã bắt đầu | `cancellable = false`, lý do về giờ, hoàn 0 |
| f | ID không tồn tại (ví dụ 99999) | Lỗi 404 "Lịch hẹn không tồn tại" |

Sau mỗi lần gọi, kiểm tra lại `GET /api/appointments/{id}`: trạng thái lịch phải **không đổi**, vì endpoint này chỉ đọc. Lịch `pending` chưa thanh toán (ví dụ ID 2) phải cho hủy nhưng hoàn 0.

## 7. Quy tắc nghiệp vụ dùng làm chuẩn so sánh

- Chỉ lịch `pending` hoặc `confirmed` được hủy.
- Không hủy ca khám đã bắt đầu hoặc đã qua; phải báo trước tối thiểu 2 giờ (đúng 2 giờ vẫn được).
- Hoàn đủ số tiền nếu hóa đơn đã thanh toán thành công.
- Hủy trong vòng 24 giờ trước giờ khám: khấu trừ 30%.
- Thanh toán bằng thẻ: trừ thêm 10.000đ phí cổng; số tiền hoàn không bao giờ âm.
- Hóa đơn chưa thanh toán: hoàn 0.
- Số tiền hoàn chỉ là **ước tính**; hệ thống chưa gọi API hoàn tiền của Stripe.

Cách tính ví dụ với hóa đơn 500.000đ: tiền mặt sát giờ = 500.000 − 30% = 350.000; thẻ hủy sớm = 500.000 − 10.000 = 490.000; thẻ sát giờ = 500.000 − 30% − 10.000 = 340.000.

## 8. Trạng thái kiểm thử đã ghi nhận

| Hạng mục | Kết quả | Nguồn / cách chạy |
|---|---|---|
| 22 test ba mẫu bổ sung | 22/22 đạt | Chạy ngày 07/11/2026 bằng bộ chạy Java thuần (JDK 21.0.12) biên dịch mã nguồn thật, thay thư viện bên thứ ba bằng bản giả tối thiểu. **Chưa chạy qua Maven** |
| 39 test backend ban đầu | 39 đạt, 0 lỗi | Ảnh chụp `./mvnw test` của nhóm ngày 03/11/2026, trước khi thêm ba mẫu mới |
| 35 test frontend | 35/35 đạt | Vitest, đã chạy lại |
| Tổng backend khi chạy đầy đủ | Kỳ vọng 61 | **Cần chạy `./mvnw test` trên máy có Maven Central** rồi chụp ảnh thay cho ảnh cũ |
| Kịch bản thủ công M01–M15 | Chưa ghi | Điền cột "Kết quả" ở mục 5 khi demo |

## 9. Giới hạn đã biết

- API chưa có JWT/phân quyền theo token; các endpoint đều gọi được trực tiếp.
- Bộ tư vấn hủy lịch (State, Chain of Responsibility, Decorator) mới được dùng ở endpoint xem trước; **chưa nối vào `PUT /api/appointments/{id}/cancel` thật**, nên lệnh hủy thật vẫn đi theo stored procedure `sp_CancelAppointment` cũ.
- Hệ thống chưa gọi API hoàn tiền của Stripe; số tiền hoàn chỉ để hiển thị.
- Luồng xác nhận thanh toán thẻ chưa có Stripe webhook xác thực phía máy chủ; không dùng cho production.
- Bảy kịch bản backend ở mục 4.2 chưa có test tự động, hai kịch bản mới phủ một phần.
- `stores/auth.js` ở frontend vẫn gọi axios trực tiếp (chưa đi qua lớp trừu tượng), `BookingFacade` khá rộng, danh sách phương thức thanh toán mặc định vẫn khai báo ở `container.js`.

## 10. Xử lý sự cố thường gặp

| Hiện tượng | Cách xử lý |
|---|---|
| Cổng 8081, 8082 hoặc 3306 đã bị chiếm | Tắt tiến trình đang dùng cổng đó hoặc đổi cổng trong `docker-compose.yml` |
| Dữ liệu mẫu không như mô tả sau nhiều lần thử | `docker compose down -v` rồi `docker compose up --build -d` để tạo lại cơ sở dữ liệu từ `database/database.sql` |
| Lịch tự chuyển `cancelled` | Do event scheduler dọn lịch `pending` quá 15 phút (mục 2.5); thanh toán ngay hoặc đặt lại |
| "Stripe chưa được cấu hình" | Chọn Tiền mặt, hoặc chạy với `VITE_PAYMENT_MODE=mock`, hoặc đặt khóa Stripe test (mục 2.2) |
| Không nhận email OTP | Kiểm tra `GMAIL_USERNAME` và `GMAIL_APP_PASSWORD` (mục 2.3), hoặc dùng tài khoản mẫu |
| `./mvnw test` không tải được thư viện | Kiểm tra kết nối tới Maven Central và Java 21 (`java -version`) |
| Preview luôn báo "đã bắt đầu" | Ca khám mẫu đã qua; tạo ca mới cho ngày tới (kịch bản M11) |
