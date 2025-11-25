# Hướng dẫn cấu hình Email (SMTP Gmail) để gửi mật khẩu tạm thời

## ✅ Đã làm
- **Backend đã sửa**: giờ `PasswordResetService` sẽ gửi email đến `student.email` hoặc `teacher.email` (thay vì `user.username`).
- **Logs chi tiết**: server sẽ log `Sent temporary password to '<email>' for userId=...` khi thành công, hoặc `Failed to send temporary password to '<email>' ...` khi thất bại (kèm stacktrace chi tiết).

## 🔧 Cấu hình SMTP trong `application.properties`

Hiện tại file `application.properties` của bạn đã có:
```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=nhat.longtran003@gmail.com
spring.mail.password=mbqiekanlee?dfid
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
spring.mail.properties.mail.smtp.connectiontimeout=5000
spring.mail.properties.mail.smtp.timeout=5000
spring.mail.properties.mail.smtp.writetimeout=5000
spring.mail.properties.mail.debug=true
logging.level.org.springframework.mail=DEBUG
```

**⚠️ Lưu ý quan trọng**: 
- Trường `spring.mail.password` hiện là `mbqiekanlee?dfid` — đây **PHẢI LÀ "App Password"** của Gmail (16 ký tự không dấu cách), **KHÔNG PHẢI mật khẩu Gmail thông thường**.
- Nếu bạn dùng mật khẩu Gmail thông thường, Gmail sẽ từ chối kết nối (lỗi `AuthenticationFailedException`).

## 📋 Cách tạo Gmail App Password (bắt buộc)

### Bước 1: Bật xác thực 2 bước (2FA)
1. Đăng nhập Gmail tại: https://myaccount.google.com/security
2. Tìm mục **"2-Step Verification"** (Xác minh 2 bước).
3. Nếu chưa bật, bấm **"Get Started"** và làm theo hướng dẫn (dùng số điện thoại để xác minh).

### Bước 2: Tạo App Password
1. Sau khi bật 2FA, quay lại: https://myaccount.google.com/security
2. Tìm mục **"App passwords"** (Mật khẩu ứng dụng).
3. Bấm vào **"App passwords"** hoặc link: https://myaccount.google.com/apppasswords
4. Chọn:
   - **Select app**: Other (Custom name) → nhập "English Center Backend"
   - **Select device**: Your device
5. Bấm **"Generate"**.
6. Gmail sẽ hiển thị **16 ký tự App Password** (ví dụ: `abcd efgh ijkl mnop`).
7. **Copy nguyên 16 ký tự** (có thể bỏ dấu cách hoặc giữ nguyên).

### Bước 3: Cập nhật `application.properties`
Mở file `application.properties` và sửa dòng:
```properties
spring.mail.password=<APP_PASSWORD_16_KÝ_TỰ>
```

Ví dụ (giả sử app password là `abcdefghijklmnop`):
```properties
spring.mail.password=abcdefghijklmnop
```

**Lưu ý**: Không commit mật khẩu vào VCS. Dùng biến môi trường hoặc file `.env` cho production.

### Bước 4: Khởi động lại Spring Boot
```powershell
.\mvnw.cmd spring-boot:run -DskipTests
```

## 🧪 Kiểm tra email có gửi thành công

### Test 1: Gọi API bằng Postman
```http
POST http://localhost:8080/auth/forgot-password
Content-Type: application/json

{
  "email": "nhat.longtran003@gmail.com"
}
```

**Kỳ vọng response**:
```json
{
  "message": "Temporary password generated and emailed",
  "temporaryPassword": "ZfHUjj$C1bXe",
  "devNote": "Temporary password is returned in response for testing; remove in production"
}
```

### Test 2: Kiểm tra logs server
Mở console nơi chạy Spring Boot, tìm các dòng log:

**Thành công**:
```
INFO  PasswordResetService : Sent temporary password to 'nhat.longtran003@gmail.com' for userId=5
INFO  PasswordResetService : Temporary password created for userId=5
```

**Thất bại** (ví dụ):
```
ERROR PasswordResetService : Failed to send temporary password to 'nhat.longtran003@gmail.com' for userId=5
javax.mail.AuthenticationFailedException: 535-5.7.8 Username and Password not accepted. ...
```

Nếu thấy `AuthenticationFailedException`:
- ✅ Kiểm tra lại App Password (16 ký tự, không có dấu cách hoặc ký tự đặc biệt).
- ✅ Kiểm tra `spring.mail.username` đúng địa chỉ Gmail.
- ✅ Đảm bảo đã bật 2FA và tạo App Password như Bước 1-2.

Nếu thấy `Connection refused` hoặc timeout:
- ✅ Kiểm tra firewall/antivirus có chặn port 587 không.
- ✅ Thử đổi port sang `465` (SSL) hoặc `25` (nếu ISP cho phép).

### Test 3: Kiểm tra hộp thư
- Mở hộp thư email `nhat.longtran003@gmail.com` (hoặc email bạn gửi).
- Tìm email có subject: **"Mật khẩu mới - English Center"**.
- Email chứa mật khẩu tạm thời (12 ký tự random).

**Nếu email không đến**:
- ✅ Kiểm tra **Spam / Junk** folder.
- ✅ Kiểm tra logs (Test 2) để thấy lỗi chi tiết.

## 🔍 Debug nhanh (nếu vẫn không gửi được)

### Lỗi phổ biến 1: `AuthenticationFailedException`
**Nguyên nhân**: Sai App Password hoặc chưa bật 2FA.

**Fix**:
1. Xóa App Password cũ trên Gmail: https://myaccount.google.com/apppasswords
2. Tạo App Password mới (Bước 2 ở trên).
3. Copy nguyên 16 ký tự vào `spring.mail.password`.
4. Khởi động lại app.

### Lỗi phổ biến 2: `SMTPSendFailedException: 554 Message rejected`
**Nguyên nhân**: Gmail nghi ngờ spam (thường do gửi nhiều email liên tục).

**Fix**:
- Đợi vài phút rồi thử lại.
- Nếu vẫn lỗi, thêm cấu hình:
  ```properties
  spring.mail.properties.mail.smtp.ssl.trust=smtp.gmail.com
  ```

### Lỗ

i phổ biến 3: `Connection timeout`
**Nguyên nhân**: Firewall hoặc network chặn port 587.

**Fix**:
- Thử port SSL (465):
  ```properties
  spring.mail.port=465
  spring.mail.properties.mail.smtp.ssl.enable=true
  ```

## ✅ Checklist hoàn chỉnh

- [ ] Đã bật 2FA trên Gmail.
- [ ] Đã tạo App Password 16 ký tự.
- [ ] Đã cập nhật `spring.mail.password` trong `application.properties`.
- [ ] Đã khởi động lại Spring Boot.
- [ ] Gọi POST `/auth/forgot-password` qua Postman → response 200 OK.
- [ ] Kiểm tra logs server → thấy `Sent temporary password to '...'`.
- [ ] Kiểm tra hộp thư email → nhận được email chứa mật khẩu tạm thời.
- [ ] (Optional) Test login bằng mật khẩu tạm thời → đăng nhập thành công.

## 📞 Nếu vẫn chưa hoạt động

Gửi cho tôi:
1. **Logs stacktrace đầy đủ** khi gọi `/auth/forgot-password` (copy từ console).
2. **Cấu hình mail hiện tại** (ẩn App Password, chỉ show các thuộc tính khác).
3. Tôi sẽ debug tiếp và fix cụ thể.

---

## 🎯 Tóm tắt ngắn

**Backend đã sửa**: Email giờ gửi đến đúng địa chỉ email của student/teacher.

**Bạn cần làm ngay**:
1. Tạo Gmail App Password (16 ký tự) theo Bước 1-2 ở trên.
2. Cập nhật `spring.mail.password=<APP_PASSWORD>` trong `application.properties`.
3. Khởi động lại app: `.\mvnw.cmd spring-boot:run -DskipTests`
4. Test bằng Postman và kiểm tra logs + hộp thư email.

**Nếu thành công**: Email sẽ đến hộp thư với mật khẩu tạm thời, người dùng đăng nhập được ngay.

