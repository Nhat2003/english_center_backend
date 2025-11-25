# ⚠️ KHẮC PHỤC LỖI GỬI EMAIL - GMAIL APP PASSWORD

## 🔴 Lỗi hiện tại
```
jakarta.mail.AuthenticationFailedException: 535-5.7.8 Username and Password not accepted
```

**Nguyên nhân**: Mật khẩu `mbqiekanlee?dfid` trong `application.properties` KHÔNG HỢP LỆ.

Gmail yêu cầu **App Password** (16 ký tự chữ và số, KHÔNG có ký tự đặc biệt như `?`).

---

## ✅ CÁCH FIX - THỰC HIỆN NGAY (5 PHÚT)

### Bước 1: Bật xác thực 2 bước (2FA) trên Gmail

1. Truy cập: **https://myaccount.google.com/security**
2. Tìm mục **"2-Step Verification"** (Xác minh 2 bước)
3. Nếu chưa bật:
   - Bấm **"Get Started"**
   - Làm theo hướng dẫn (nhập số điện thoại để nhận mã xác minh)
   - Hoàn tất setup 2FA

### Bước 2: Tạo Gmail App Password

1. Sau khi bật 2FA, truy cập: **https://myaccount.google.com/apppasswords**
2. Đăng nhập lại nếu được yêu cầu
3. Tại trang "App passwords":
   - **Select app**: Chọn **"Other (Custom name)"**
   - Nhập tên: `English Center Backend`
   - Bấm **"Generate"**
4. Gmail sẽ hiển thị **16 ký tự App Password** trong một hộp màu vàng
   
   Ví dụ: `abcd efgh ijkl mnop` (có thể có dấu cách)

5. **Copy toàn bộ 16 ký tự** (có thể bỏ dấu cách hoặc giữ nguyên, Spring Boot sẽ tự xử lý)

### Bước 3: Cập nhật `application.properties`

1. Mở file: `src/main/resources/application.properties`
2. Tìm dòng:
   ```properties
   spring.mail.password=REPLACE_WITH_YOUR_16_CHAR_APP_PASSWORD
   ```
3. Thay thế bằng App Password vừa tạo:
   ```properties
   spring.mail.password=abcdefghijklmnop
   ```
   (Dùng App Password THẬT bạn vừa copy, ví dụ trên chỉ là mẫu)

4. **Lưu file** (Ctrl+S)

### Bước 4: Khởi động lại Spring Boot

```powershell
# Stop server hiện tại (Ctrl+C trong terminal)
# Sau đó chạy lại:
.\mvnw.cmd spring-boot:run -DskipTests
```

### Bước 5: Test lại API

```http
POST http://localhost:8080/auth/forgot-password
Content-Type: application/json

{
  "email": "nhat.longtran003@gmail.com"
}
```

**Kỳ vọng logs lần này:**
```
INFO  PasswordResetService : Sent temporary password to 'nhat.longtran003@gmail.com' for userId=5
```
(KHÔNG CÒN lỗi `AuthenticationFailedException`)

### Bước 6: Kiểm tra hộp thư email

1. Mở email `nhat.longtran003@gmail.com`
2. Tìm email subject: **"Mật khẩu mới - English Center"**
3. Nếu không thấy trong Inbox → kiểm tra **Spam/Junk**

---

## 📸 Hướng dẫn có hình ảnh

### Tạo App Password trên Gmail:

1. **Truy cập**: https://myaccount.google.com/apppasswords
   
2. **Chọn app**: Other (Custom name) → nhập "English Center Backend"
   
3. **Bấm Generate** → Gmail hiển thị 16 ký tự trong hộp vàng:
   ```
   abcd efgh ijkl mnop
   ```
   
4. **Copy 16 ký tự** → dán vào `application.properties`

---

## ⚡ Nếu không thấy menu "App passwords"

**Nguyên nhân**: Chưa bật 2FA hoặc tài khoản không hỗ trợ.

**Fix**:
1. Đảm bảo đã bật 2FA: https://myaccount.google.com/security
2. Đăng xuất Gmail → đăng nhập lại
3. Thử lại link: https://myaccount.google.com/apppasswords

Nếu vẫn không thấy, có thể:
- Tài khoản Google Workspace bị admin disable App Passwords
- Cần liên hệ admin hoặc dùng tài khoản Gmail cá nhân khác

---

## 🎯 Checklist hoàn chỉnh

- [ ] Đã bật 2FA trên Gmail: https://myaccount.google.com/security
- [ ] Đã tạo App Password (16 ký tự): https://myaccount.google.com/apppasswords
- [ ] Đã copy App Password và update vào `application.properties`
- [ ] Đã xóa password cũ `mbqiekanlee?dfid` (có ký tự `?` không hợp lệ)
- [ ] Đã lưu file `application.properties`
- [ ] Đã khởi động lại Spring Boot
- [ ] Test POST `/auth/forgot-password` → response 200 OK
- [ ] Kiểm tra logs → thấy `Sent temporary password to '...'` (KHÔNG có lỗi Authentication)
- [ ] Kiểm tra hộp thư email → nhận được email chứa mật khẩu tạm thời
- [ ] (Optional) Test login bằng mật khẩu tạm thời → thành công

---

## 📞 Nếu vẫn lỗi sau khi fix

Gửi cho tôi:
1. **Screenshot** trang tạo App Password (Gmail) để xác nhận format đúng
2. **Logs mới** sau khi update App Password và restart server
3. **4 ký tự đầu + 4 ký tự cuối** của App Password bạn dùng (để kiểm tra format, VD: `abcd...mnop`)

---

## 🔒 Lưu ý bảo mật

- **KHÔNG commit** `application.properties` chứa App Password lên Git/GitHub
- Nên dùng **biến môi trường** hoặc **file .env** cho production:
  ```properties
  spring.mail.password=${GMAIL_APP_PASSWORD}
  ```
- App Password có thể thu hồi bất cứ lúc nào tại: https://myaccount.google.com/apppasswords
- Mỗi ứng dụng nên có App Password riêng (dễ quản lý và thu hồi khi cần)

---

## ✅ Tóm tắt

**Vấn đề**: Mật khẩu `mbqiekanlee?dfid` có ký tự `?` không hợp lệ, Gmail từ chối.

**Giải pháp**:
1. Tạo Gmail App Password (16 ký tự chữ/số, không có ký tự đặc biệt)
2. Cập nhật vào `application.properties`
3. Khởi động lại server
4. Email sẽ được gửi thành công!

**Thời gian**: ~5 phút

**Link quan trọng**: https://myaccount.google.com/apppasswords

