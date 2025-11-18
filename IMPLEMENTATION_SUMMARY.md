# Tóm tắt thay đổi - Login Error Handling & Change Password

## Mục tiêu:
### 1. Login Error Handling:
- Khi đăng nhập SAI tài khoản/mật khẩu → Trả về thông báo: "Sai tên đăng nhập hoặc mật khẩu!"
- Khi tài khoản BỊ KHÓA (isActive=false) → Trả về thông báo: "Tài khoản đã bị khóa!"

### 2. Change Password Feature:
- Cho phép học sinh (STUDENT) và giáo viên (TEACHER) đổi mật khẩu của chính họ
- Validate đầy đủ: mật khẩu hiện tại, mật khẩu mới, xác nhận mật khẩu

---

## 📁 PHẦN 1: LOGIN ERROR HANDLING

## Các file đã tạo mới:

### 1. InvalidCredentialsException.java
```
Location: src/main/java/com/example/English/Center/Data/exception/
Mục đích: Exception cho trường hợp sai thông tin đăng nhập
```

### 2. AccountLockedException.java
```
Location: src/main/java/com/example/English/Center/Data/exception/
Mục đích: Exception cho trường hợp tài khoản bị khóa
```

### 3. GlobalExceptionHandler.java
```
Location: src/main/java/com/example/English/Center/Data/exception/
Mục đích: Xử lý global exceptions và trả về HTTP response phù hợp
- InvalidCredentialsException → HTTP 401 Unauthorized
- AccountLockedException → HTTP 403 Forbidden
```

## File đã chỉnh sửa:

### UserService.java (method login)
**Logic mới:**
1. Tìm user theo username
2. Kiểm tra username + password cùng lúc
3. Nếu SAI username HOẶC password → throw InvalidCredentialsException
4. Nếu ĐÚNG thông tin NHƯNG tài khoản bị khóa → throw AccountLockedException
5. Kiểm tra role hợp lệ
6. Tạo token và trả về LoginResponse

**Best Practice Security:**
- Không tiết lộ username có tồn tại hay không
- Chỉ thông báo tài khoản bị khóa KHI ĐÃ xác thực đúng username + password

## API Response:

### Case 1: Sai thông tin đăng nhập
```json
HTTP 401 Unauthorized
{
  "error": "INVALID_CREDENTIALS",
  "message": "Sai tên đăng nhập hoặc mật khẩu!"
}
```

### Case 2: Tài khoản bị khóa
```json
HTTP 403 Forbidden
{
  "error": "ACCOUNT_LOCKED",
  "message": "Tài khoản đã bị khóa!"
}
```

### Case 3: Đăng nhập thành công
```json
HTTP 200 OK
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "user": {
    "id": 1,
    "username": "admin",
    "role": "ADMIN",
    ...
  }
}
```

## Cách test:

### Test trong Postman:
1. **Sai thông tin:**
   - POST http://localhost:8080/users/login
   - Body: {"username": "wrong", "password": "wrong"}
   - Expect: 401 + "Sai tên đăng nhập hoặc mật khẩu!"

2. **Tài khoản bị khóa:**
   - Cập nhật một user trong DB: isActive = false
   - POST http://localhost:8080/users/login với thông tin ĐÚNG của user đó
   - Expect: 403 + "Tài khoản đã bị khóa!"

3. **Thành công:**
   - POST http://localhost:8080/users/login
   - Body: {"username": "admin", "password": "admin"}
   - Expect: 200 + token

## Frontend xử lý:

```typescript
loginService.login(credentials).subscribe({
  next: (response) => {
    // Thành công - lưu token và điều hướng
    localStorage.setItem('token', response.token);
    this.router.navigate(['/dashboard']);
  },
  error: (error) => {
    if (error.status === 401) {
      // Sai thông tin đăng nhập
      this.messageService.error('Sai tên đăng nhập hoặc mật khẩu!');
    } else if (error.status === 403) {
      // Tài khoản bị khóa
      this.messageService.error('Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên!');
    } else {
      // Lỗi khác
      this.messageService.error('Đã xảy ra lỗi. Vui lòng thử lại!');
    }
  }
});
```

## Status:
✅ Code hoàn thành
✅ Logic đã được cập nhật theo yêu cầu
✅ Không có lỗi biên dịch (chỉ có warnings vô hại)
✅ Sẵn sàng để test

---

## 📁 PHẦN 2: CHANGE PASSWORD FEATURE

## Các file đã tạo mới:

### 1. ChangePasswordRequest.java
```
Location: src/main/java/com/example/English/Center/Data/dto/
Mục đích: DTO chứa thông tin yêu cầu đổi mật khẩu
Fields: currentPassword, newPassword, confirmPassword
Validation: @NotBlank, @Size(min=6)
```

### 2. PasswordMismatchException.java
```
Location: src/main/java/com/example/English/Center/Data/exception/
Mục đích: Exception cho lỗi mật khẩu không khớp
```

## File đã chỉnh sửa:

### 1. GlobalExceptionHandler.java
**Thêm handler:** `PasswordMismatchException` → HTTP 400 Bad Request

### 2. UserService.java
**Thêm method:** `changePassword(userId, currentPassword, newPassword, confirmPassword)`
**Logic:**
1. Validate mật khẩu mới không rỗng và >= 6 ký tự
2. Kiểm tra mật khẩu mới khớp với xác nhận
3. Verify mật khẩu hiện tại đúng
4. Kiểm tra mật khẩu mới khác mật khẩu cũ
5. Cập nhật mật khẩu vào database

### 3. StudentController.java
**Endpoint mới:**
- `PUT /students/me/change-password`
- Yêu cầu STUDENT token
- Body: ChangePasswordRequest

### 4. TeacherController.java
**Endpoint mới:**
- `PUT /teachers/me/change-password`
- Yêu cầu TEACHER token
- Body: ChangePasswordRequest

## API Endpoints:

### Học sinh đổi mật khẩu:
```http
PUT /students/me/change-password
Authorization: Bearer <token>
Content-Type: application/json

{
  "currentPassword": "oldpass",
  "newPassword": "newpass123",
  "confirmPassword": "newpass123"
}
```

### Giáo viên đổi mật khẩu:
```http
PUT /teachers/me/change-password
Authorization: Bearer <token>
Content-Type: application/json

{
  "currentPassword": "oldpass",
  "newPassword": "newpass123",
  "confirmPassword": "newpass123"
}
```

## Response Examples:

### ✅ Thành công:
```json
HTTP 200 OK
{
  "message": "Đổi mật khẩu thành công!"
}
```

### ❌ Sai mật khẩu hiện tại:
```json
HTTP 401 Unauthorized
{
  "error": "INVALID_CREDENTIALS",
  "message": "Mật khẩu hiện tại không đúng!"
}
```

### ❌ Mật khẩu không khớp:
```json
HTTP 400 Bad Request
{
  "error": "PASSWORD_MISMATCH",
  "message": "Mật khẩu mới và xác nhận mật khẩu không khớp!"
}
```

## Status:
✅ Code hoàn thành
✅ Validation đầy đủ
✅ Exception handling hoàn chỉnh
✅ Security rules đã cấu hình
✅ Sẵn sàng để test

---

## 📚 Chi tiết đầy đủ:
- Login Error Handling: Xem file này (IMPLEMENTATION_SUMMARY.md)
- Change Password Feature: Xem file CHANGE_PASSWORD_FEATURE.md

## Note:
- Các file exception có thể hiển thị warning "never used" trong IDE, nhưng đây là false positive vì chúng được sử dụng qua fully qualified name trong UserService
- Cần có Java 17+ và Maven 3.8+ để build project
- Sau khi restart backend server, các thay đổi sẽ có hiệu lực

