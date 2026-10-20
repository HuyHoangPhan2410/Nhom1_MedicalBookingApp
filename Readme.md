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
