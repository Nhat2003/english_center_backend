# ⚠️ CẤU HÌNH EMAIL CHƯA ĐÚNG - HƯỚNG DẪN SỬA NHANH

## ❌ Vấn đề hiện tại

```ini
spring.mail.username=nhat.longtran003@gmail.com
spring.mail.password=Taoloptruong@1  # ❌ SAI - Đây là mật khẩu Gmail thường
```

**Gmail KHÔNG CHO PHÉP dùng mật khẩu thường để đăng nhập SMTP!**

Lỗi bạn sẽ thấy:
```
javax.mail.AuthenticationFailedException: 535-5.7.8 Username and Password not accepted
```

---

## ✅ CÁCH SỬA (5 PHÚT)

### Bước 1: Tạo Gmail App Password

Tôi đã mở link này cho bạn: https://myaccount.google.com/apppasswords

**Làm theo:**

1. ✅ Đăng nhập bằng: `nhat.longtran003@gmail.com`

2. ✅ **Nếu thấy thông báo "App passwords không khả dụng":**
   - Click vào **Security** (bên trái)
   - Tìm **2-Step Verification** 
   - Click **GET STARTED** và làm theo hướng dẫn (xác thực bằng số điện thoại)
   - Sau khi bật 2FA xong, quay lại: https://myaccount.google.com/apppasswords

3. ✅ Tạo App Password:
   - **Select app:** Chọn **Mail**
   - **Select device:** Chọn **Other (Custom name)**
   - Nhập tên: `English Center Backend`
   - Click **GENERATE**

4. ✅ **QUAN TRỌNG**: Copy mật khẩu 16 ký tự hiển thị (ví dụ: `abcd efgh ijkl mnop`)

### Bước 2: Cập nhật application.properties

Thay dòng này:
```ini
spring.mail.password=Taoloptruong@1
```

Thành:
```ini
spring.mail.password=abcdefghijklmnop   # Dán App Password (BỎ KHOẢNG TRẮNG!)
```

**Ví dụ**: Nếu Gmail cho bạn `abcd efgh ijkl mnop`, bạn ghi:
```ini
spring.mail.password=abcdefghijklmnop
```

### Bước 3: Restart Backend

Trong terminal backend:
```powershell
# Nếu đang chạy, nhấn Ctrl+C để stop
# Sau đó chạy lại:
mvn spring-boot:run
```

Hoặc nếu dùng IDE: Stop và Run lại application.

### Bước 4: Test

Dùng Postman:
```http
POST http://localhost:8080/auth/forgot-password
Content-Type: application/json

{
  "email": "student"
}
```

**Kỳ vọng:**
- ✅ Response: `200 OK` với message "Reset email sent"
- ✅ Log backend: `Sent mail to student subject=Reset mật khẩu - English Center`
- ✅ Check Gmail inbox của user "student" → nhận được email

---

## 🔍 Kiểm tra logs sau khi test

Xem console backend:

### ✅ Nếu thấy:
```
Password reset token created: token=abc-def-123-456 for userId=1 (expires in 15 minutes)
Sent mail to student subject=Reset mật khẩu - English Center
```
→ **THÀNH CÔNG!** Email đã được gửi.

### ❌ Nếu thấy:
```
Failed to send reset email; token=abc-def-123-456 userId=1
javax.mail.AuthenticationFailedException: 535-5.7.8 Username and Password not accepted
```
→ **App Password sai hoặc chưa cập nhật đúng**. Kiểm tra lại Bước 2.

---

## 📋 Checklist

- [ ] Đã mở https://myaccount.google.com/apppasswords
- [ ] Đã bật 2FA (nếu chưa có)
- [ ] Đã tạo App Password cho Mail
- [ ] Đã copy mật khẩu 16 ký tự
- [ ] Đã cập nhật `spring.mail.password` trong application.properties (bỏ khoảng trắng!)
- [ ] Đã restart backend
- [ ] Test POST /auth/forgot-password → 200 OK
- [ ] Kiểm tra logs → thấy "Sent mail to..."
- [ ] Kiểm tra email inbox → nhận được email reset password

---

## 🛡️ BẢO MẬT

**⚠️ QUAN TRỌNG**: File `application.properties` hiện chứa thông tin nhạy cảm:

```ini
spring.datasource.password=admin           # ⚠️ DB password
spring.mail.password=abcdefghijklmnop      # ⚠️ Gmail App Password
```

### KHÔNG ĐƯỢC COMMIT FILE NÀY LÊN GIT!

**Giải pháp:**

1. Thêm vào `.gitignore`:
```
# Sensitive config
src/main/resources/application.properties
src/main/resources/application-local.properties
```

2. Tạo file template:
```ini
# application.properties.template (commit file này)
spring.mail.username=YOUR_EMAIL_HERE
spring.mail.password=YOUR_APP_PASSWORD_HERE
spring.datasource.password=YOUR_DB_PASSWORD_HERE
```

3. Hoặc dùng environment variables:
```ini
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}
```

---

## ❓ Câu hỏi thường gặp

**Q: Tại sao không dùng mật khẩu Gmail thường?**
A: Google đã tắt tính năng "Less secure app access" từ 30/05/2022. Bắt buộc phải dùng App Password.

**Q: Tôi không thấy "App passwords" trong Google Account?**
A: Bạn cần bật 2-Step Verification trước. Vào Security → 2-Step Verification → Bật.

**Q: App Password có an toàn không?**
A: Có! App Password chỉ cho phép ứng dụng cụ thể truy cập email (không có quyền đầy đủ như mật khẩu chính). Bạn có thể thu hồi bất cứ lúc nào.

**Q: Tôi đã làm đúng nhưng vẫn lỗi AuthenticationFailedException?**
A: Kiểm tra:
1. App Password có dấu cách không? (phải BỎ dấu cách!)
2. Copy đúng 16 ký tự?
3. Đã restart backend chưa?
4. Email `nhat.longtran003@gmail.com` có chính xác không?

---

**Sau khi làm xong, quay lại và báo kết quả!**

