# Hướng dẫn sửa lỗi gửi Email (Gmail SMTP)

## ✅ Đã sửa
1. **Cho phép public access endpoint `/auth/**`** trong WebSecurityConfig
   - Trước: 403 Forbidden khi POST `/auth/forgot-password`
   - Sau: endpoint được phép truy cập public

2. **Thêm debug logging** cho mail trong application.properties

## ❌ Vấn đề hiện tại: Gmail yêu cầu App Password

Bạn đang dùng mật khẩu Gmail thường (`Taoloptruong@1`), nhưng **Gmail không cho phép đăng nhập SMTP bằng mật khẩu thường** nếu:
- Tài khoản bật 2-Factor Authentication (2FA)
- Gmail Security Defaults (mặc định từ 2022)

### ❗ Lỗi bạn sẽ thấy trong logs:
```
javax.mail.AuthenticationFailedException: 535-5.7.8 Username and Password not accepted
```

---

## 🔧 CÁCH SỬA (3 bước)

### Bước 1: Tạo Gmail App Password

1. Đăng nhập Gmail: https://myaccount.google.com/
2. Vào **Security** → **2-Step Verification** (bật nếu chưa bật)
3. Sau khi bật 2FA, quay lại **Security** → **App passwords**
4. Chọn:
   - **App**: Mail
   - **Device**: Other (nhập "English Center Backend")
5. Click **Generate** → Copy mật khẩu 16 ký tự (ví dụ: `abcd efgh ijkl mnop`)

### Bước 2: Cập nhật application.properties

Thay đổi:
```properties
spring.mail.username=nhat.longtran003@gmail.com
spring.mail.password=abcdefghijklmnop   # <-- Dán App Password vừa tạo (không có khoảng trắng)
```

### Bước 3: Restart backend và test

```powershell
# Stop backend (Ctrl+C nếu đang chạy)
# Start lại:
mvn spring-boot:run
```

Test bằng Postman:
```http
POST http://localhost:8080/auth/forgot-password
Content-Type: application/json

{
  "email": "student"
}
```

Kiểm tra:
- ✅ Response: 200 OK `{"message":"Reset email sent"}`
- ✅ Log backend: `Sent mail to student subject=Reset mật khẩu - English Center`
- ✅ Check email inbox: nhận được email reset

---

## 🔍 Nếu vẫn lỗi - kiểm tra logs

Xem logs backend (console) sau khi gọi API:

### Nếu thấy:
```
Failed to send email to ... subject=...
javax.mail.AuthenticationFailedException: 535-5.7.8 Username and Password not accepted
```
→ **Mật khẩu sai hoặc chưa dùng App Password**

### Nếu thấy:
```
Password reset token created: token=abc-123-def for userId=1
```
→ **Token đã được tạo**, bạn có thể copy token từ log và test endpoint reset-password:
```http
POST http://localhost:8080/auth/reset-password
Content-Type: application/json

{
  "token": "abc-123-def",
  "newPassword": "NewPassword123!"
}
```

---

## 🛡️ BẢO MẬT

**⚠️ QUAN TRỌNG**: Mật khẩu Gmail App Password hiện đang để trong `application.properties`:

```properties
spring.mail.password=abcdefghijklmnop   # ⚠️ KHÔNG commit file này lên Git!
```

### Giải pháp bảo mật:

#### Option 1: Dùng biến môi trường (khuyến nghị)
```properties
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}
```

Khi chạy app:
```powershell
$Env:MAIL_USERNAME="nhat.longtran003@gmail.com"
$Env:MAIL_PASSWORD="abcdefghijklmnop"
mvn spring-boot:run
```

#### Option 2: Tạo file application-local.properties (không commit)
```properties
# application-local.properties (thêm vào .gitignore)
spring.mail.username=nhat.longtran003@gmail.com
spring.mail.password=abcdefghijklmnop
```

Chạy:
```powershell
mvn spring-boot:run -Dspring.profiles.active=local
```

---

## 📋 Checklist test

- [ ] Đã tạo Gmail App Password
- [ ] Đã cập nhật `spring.mail.password` trong application.properties
- [ ] Restart backend
- [ ] POST /auth/forgot-password → 200 OK (không còn 403)
- [ ] Kiểm tra log: "Sent mail to ..."
- [ ] Kiểm tra email inbox: nhận được email
- [ ] Click link trong email → mở http://localhost:4200/reset-password?token=...
- [ ] Frontend gọi POST /auth/reset-password → 200 OK
- [ ] Đăng nhập lại với mật khẩu mới → thành công

---

## 🔗 Tài liệu tham khảo

- Gmail App Passwords: https://support.google.com/accounts/answer/185833
- Spring Boot Mail: https://docs.spring.io/spring-boot/reference/io/email.html

