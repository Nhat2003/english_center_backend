# 🚀 QUICK REFERENCE - Frontend APIs

## 📱 2 API CẦN GỌI

### 1️⃣ Yêu cầu reset mật khẩu
```typescript
POST http://localhost:8080/auth/forgot-password
Content-Type: application/json

Request:
{
  "email": "student"  // username hoặc email
}

Response (200 OK):
{
  "message": "Reset email sent",
  "token": "abc-123-def-456",           // Token để test
  "devNote": "Token is returned for testing only..."
}

Response (400 Error):
{
  "message": "Email does not exist"
}
```

---

### 2️⃣ Đặt lại mật khẩu
```typescript
POST http://localhost:8080/auth/reset-password
Content-Type: application/json

Request:
{
  "token": "abc-123-def-456",     // Từ email hoặc response API 1
  "newPassword": "NewPassword123!"
}

Response (200 OK):
{
  "message": "Password updated"
}

Response (400 Error):
{
  "message": "Invalid token"
  // hoặc "Token expired"
  // hoặc "User not found"
}
```

---

## 🎯 FRONTEND CẦN LÀM

### Trang 1: Forgot Password (`/forgot-password`)
```html
<form (ngSubmit)="onForgotPassword()">
  <input [(ngModel)]="email" placeholder="Email/Username" />
  <button type="submit">Gửi yêu cầu</button>
</form>
```

```typescript
onForgotPassword() {
  this.http.post('/auth/forgot-password', { email: this.email })
    .subscribe(
      res => alert('Email đã được gửi! Kiểm tra hộp thư.'),
      err => alert(err.error.message)
    );
}
```

### Trang 2: Reset Password (`/reset-password?token=...`)
```html
<form (ngSubmit)="onResetPassword()">
  <input type="password" [(ngModel)]="newPassword" placeholder="Mật khẩu mới" />
  <input type="password" [(ngModel)]="confirmPassword" placeholder="Xác nhận" />
  <button type="submit">Đặt lại mật khẩu</button>
</form>
```

```typescript
ngOnInit() {
  // Lấy token từ URL
  this.route.queryParams.subscribe(params => {
    this.token = params['token'];
  });
}

onResetPassword() {
  if (this.newPassword !== this.confirmPassword) {
    alert('Mật khẩu không khớp!');
    return;
  }
  
  this.http.post('/auth/reset-password', {
    token: this.token,
    newPassword: this.newPassword
  }).subscribe(
    res => {
      alert('Mật khẩu đã được đổi!');
      this.router.navigate(['/login']);
    },
    err => alert(err.error.message)
  );
}
```

---

## 🔗 LUỒNG HOÀN CHỈNH

```
1. User vào trang Login
   → Click "Quên mật khẩu?"
   → Redirect /forgot-password

2. User nhập email "student"
   → Submit form
   → Frontend gọi: POST /auth/forgot-password
   → Backend gửi email có link:
     http://localhost:4200/reset-password?token=abc-123

3. User check email
   → Click link
   → Mở trang /reset-password?token=abc-123

4. Frontend:
   → Đọc token từ URL query param
   → Hiển thị form nhập mật khẩu mới

5. User nhập mật khẩu mới "NewPass123!"
   → Submit form
   → Frontend gọi: POST /auth/reset-password
   → Response: "Password updated"

6. Frontend redirect về /login
   → User login với mật khẩu mới
   → Thành công! ✅
```

---

## ⚠️ LƯU Ý

- Token hết hạn sau **15 phút**
- Token chỉ dùng được **1 lần**
- Email gửi về Gmail: **nhat.longtran003@gmail.com** (đã cấu hình)
- Response có trả `token` chỉ để **testing** (production nên bỏ)

---

## 🧪 TEST NHANH

### Test với Postman:
```bash
# 1. Forgot
POST http://localhost:8080/auth/forgot-password
Body: {"email":"student"}

# 2. Copy token từ response

# 3. Reset
POST http://localhost:8080/auth/reset-password
Body: {"token":"abc-123","newPassword":"NewPass123!"}

# 4. Login
POST http://localhost:8080/users/login
Body: {"username":"student","password":"NewPass123!"}
```

---

📄 **Chi tiết đầy đủ**: `FRONTEND_API_GUIDE.md`

