# Nhom1 - Medical Booking App (Docker)

## Kiến trúc

  Frontend (Vue3+Nginx)  --->  Backend (Spring Boot)  --->  Database (MySQL 8)
  Port: 8081                   Port: 8082                   Port: 3306

## Khởi chạy nhanh

### 1. Clone repo
```bash
git clone https://github.com/HuyHoangPhan2410/Nhom1_MedicalBookingApp/
cd Nhom1_MedicalBookingApp
```

### 2. Build & chạy tất cả
```bash
docker compose up --build -d
```
### 3. Truy cập
- Frontend:  http://localhost:8081  
- Backend (API):   http://localhost:8082  
- Database:  localhost:3306  
- Admin:http://localhost:8081/admin/login  
Email:  / Password: password123  
### 4. Kiểm tra
```bash
docker compose ps
```

## Tài khoản mẫu

| Role | Email | Password |
|---------|-------|----------|
| Bác sĩ | dr.hung@hospital.com | password123 |
| Bác sĩ | dr.mai@hospital.com | password123 |
| Bệnh nhân | patient.an@gmail.com | password123 |
| Bệnh nhân | patient.bich@gmail.com | password123 |
| Admin | admin@medbooking.com | password123 |

## Cấu hình Gmail SMTP cho OTP

1. Bật xác minh 2 bước cho tài khoản Gmail dùng để gửi email.   
2. Tạo Gmail App Password; không dùng mật khẩu đăng nhập Gmail thông thường.  
3. Tạo file `.env` từ `.env.example` và cấu hình:  

```env
GMAIL_USERNAME=your_gmail@gmail.com
GMAIL_APP_PASSWORD=your_16_character_app_password
```

4. Khởi động lại backend bằng  
```bash
docker compose up --build -d
```

OTP chỉ được lưu dưới dạng BCrypt hash, có hiệu lực 5 phút và được phép gửi lại tối đa 5 lần trong một giờ.


## Bổ sung Design Patterns cho tiểu luận

Phiên bản này được bổ sung **2 Creational (Builder, Factory Method)**,
**2 Structural (Adapter, Facade)** và **2 Behavioral (Strategy, Observer)**.

- Tài liệu phân tích và hướng dẫn demo: [`docs/design-patterns/README.md`](docs/design-patterns/README.md)
- 12 sơ đồ lớp/tuần tự PlantUML: [`docs/design-patterns/`](docs/design-patterns/)
- Kiểm thử Java/Mockito mới: `backend/src/test/java/com/medbooking/patterns/`
- API mới: `GET /api/appointments/{id}/overview` và
  `POST /api/payments/prepare?appointmentId={id}&method=cash|card`.

**Trạng thái kiểm thử:** Builder đã vượt qua phép thử Java 21 độc lập;
bộ Maven/JUnit chưa được chạy đầy đủ trong môi trường tạo bản ZIP (không tải được Maven).
Vui lòng chạy `cd backend && ./mvnw test` hoặc `mvnw.cmd test` trên máy
có Java 21 và kết nối Maven Central. Không dùng luồng xác nhận thanh toán thẻ
trong production trước khi bổ sung Stripe webhook xác thực phía máy chủ.
