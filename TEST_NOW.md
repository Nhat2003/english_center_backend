# 🚀 TEST NGAY - Không cần chờ email!

## ✅ ĐÃ SỬA

Tôi đã cập nhật API `/auth/forgot-password` để **trả về token trong response** (chỉ dùng cho testing).

Bạn có thể test NGAY mà không cần cấu hình email!

---

## 📋 BƯỚC TEST NHANH (3 PHÚT)

### Bước 1: RESTART Backend

**QUAN TRỌNG**: Phải restart backend để áp dụng thay đổi (fix lỗi 403)

```powershell
# Trong terminal backend, nhấn Ctrl+C để stop
# Sau đó chạy lại:
mvn spring-boot:run
```

Đợi đến khi thấy:
```
Started EnglishCenterDataApplication in X.XXX seconds
```

### Bước 2: Test Forgot Password (Postman)

```http
POST http://localhost:8080/auth/forgot-password
Content-Type: application/json

{
  "email": "student"
}
```

**KỲ VỌNG** ✅:
```json
{
  "message": "Reset email sent",
  "token": "abc-123-def-456-789",
  "devNote": "Token is returned for testing only. In production, user must check email."
}
```

**COPY token** từ response!

### Bước 3: Test Reset Password

```http
POST http://localhost:8080/auth/reset-password
Content-Type: application/json

{
  "token": "abc-123-def-456-789",  // Dán token vừa copy
  "newPassword": "NewPassword123!"
}
```

**KỲ VỌNG** ✅:
```json
{
  "message": "Password updated"
}
```

### Bước 4: Test Login với mật khẩu mới

```http
POST http://localhost:8080/users/login
Content-Type: application/json

{
  "username": "student",
  "password": "NewPassword123!"
}
```

**KỲ VỌNG** ✅: Login thành công, nhận JWT token!

---

## 🔍 XỬ LÝ LỖI

### ❌ Vẫn thấy lỗi 403 Forbidden?

→ **Backend chưa restart!** 

Làm lại Bước 1: Stop (Ctrl+C) và chạy lại `mvn spring-boot:run`

### ❌ Lỗi "Email does not exist"?

→ **Email/username không tồn tại trong database**

Kiểm tra database xem user "student" có tồn tại không:
```sql
SELECT * FROM users WHERE username = 'student';
```

Hoặc thử với username khác (ví dụ: "admin", "teacher")

### ❌ Lỗi "Invalid token"?

→ **Token sai hoặc đã hết hạn** (15 phút)

Gọi lại API forgot-password để lấy token mới

### ❌ Lỗi "Token expired"?

→ **Token đã hết hạn** (15 phút sau khi tạo)

Gọi lại API forgot-password để lấy token mới

---

## 📧 VỀ EMAIL (TÙY CHỌN)

Tính năng trả token trong response chỉ để **testing/development**.

Nếu bạn muốn gửi email thật:

1. Làm theo hướng dẫn trong file `FIX_EMAIL_NOW.md`
2. Tạo Gmail App Password
3. Cập nhật `spring.mail.password` trong application.properties
4. Restart backend
5. Email sẽ được gửi tự động (đồng thời vẫn trả token trong response)

**Trong production**: Xóa phần trả token trong response (chỉ gửi email)

---

## ✅ CHECKLIST

- [ ] Đã restart backend (Ctrl+C → mvn spring-boot:run)
- [ ] Đợi backend start xong (thấy "Started ...")
- [ ] POST /auth/forgot-password → 200 OK (không còn 403!)
- [ ] Copy token từ response
- [ ] POST /auth/reset-password với token → 200 OK
- [ ] Login với mật khẩu mới → thành công!

---

## 🎉 XONG!

Bây giờ tính năng "Quên mật khẩu" đã hoạt động!

**Frontend cần làm:**

1. Tạo trang "Forgot Password" → gọi POST /auth/forgot-password
2. Tạo trang "Reset Password" (URL: /reset-password?token=...) → gọi POST /auth/reset-password
3. Xử lý success/error messages
4. Redirect về login sau khi đổi mật khẩu thành công

**Xem thêm**: `postman/forgot_password_collection.json` để import vào Postman

