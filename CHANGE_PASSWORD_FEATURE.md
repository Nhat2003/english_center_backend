# Tính năng Đổi Mật Khẩu - Change Password Feature

## 📋 Mô tả:
Cho phép học sinh (STUDENT) và giáo viên (TEACHER) đổi mật khẩu của chính họ.

## 🎯 Các file đã tạo mới:

### 1. ChangePasswordRequest.java
```
Location: src/main/java/com/example/English/Center/Data/dto/
Mục đích: DTO chứa thông tin yêu cầu đổi mật khẩu
Fields:
  - currentPassword: Mật khẩu hiện tại
  - newPassword: Mật khẩu mới (tối thiểu 6 ký tự)
  - confirmPassword: Xác nhận mật khẩu mới
```

### 2. PasswordMismatchException.java
```
Location: src/main/java/com/example/English/Center/Data/exception/
Mục đích: Exception cho các lỗi liên quan đến mật khẩu không khớp
```

## 📝 Các file đã chỉnh sửa:

### 1. GlobalExceptionHandler.java
**Thêm handler mới:**
- `PasswordMismatchException` → HTTP 400 Bad Request

### 2. UserService.java
**Thêm method mới:** `changePassword(userId, currentPassword, newPassword, confirmPassword)`

**Các bước kiểm tra:**
1. ✅ Validate mật khẩu mới không rỗng
2. ✅ Validate mật khẩu mới >= 6 ký tự
3. ✅ Kiểm tra mật khẩu mới khớp với xác nhận
4. ✅ Kiểm tra mật khẩu hiện tại đúng
5. ✅ Kiểm tra mật khẩu mới khác mật khẩu cũ
6. ✅ Cập nhật mật khẩu mới vào database

### 3. StudentController.java
**Endpoint mới:**
- `PUT /students/me/change-password`
- Authentication: Yêu cầu token STUDENT
- Body: ChangePasswordRequest

### 4. TeacherController.java
**Endpoint mới:**
- `PUT /teachers/me/change-password`
- Authentication: Yêu cầu token TEACHER
- Body: ChangePasswordRequest

### 5. WebSecurityConfig.java
**Security rules đã có:**
- `/students/me/**` → STUDENT, TEACHER, ADMIN
- `/teachers/me/**` → TEACHER, ADMIN

## 🔐 API Endpoints:

### Đổi mật khẩu cho Học sinh:
```http
PUT http://localhost:8080/students/me/change-password
Authorization: Bearer <student_token>
Content-Type: application/json

{
  "currentPassword": "oldpass123",
  "newPassword": "newpass456",
  "confirmPassword": "newpass456"
}
```

### Đổi mật khẩu cho Giáo viên:
```http
PUT http://localhost:8080/teachers/me/change-password
Authorization: Bearer <teacher_token>
Content-Type: application/json

{
  "currentPassword": "oldpass123",
  "newPassword": "newpass456",
  "confirmPassword": "newpass456"
}
```

## 📊 Response Examples:

### ✅ Thành công (200 OK):
```json
{
  "message": "Đổi mật khẩu thành công!"
}
```

### ❌ Sai mật khẩu hiện tại (401 Unauthorized):
```json
{
  "error": "INVALID_CREDENTIALS",
  "message": "Mật khẩu hiện tại không đúng!"
}
```

### ❌ Mật khẩu xác nhận không khớp (400 Bad Request):
```json
{
  "error": "PASSWORD_MISMATCH",
  "message": "Mật khẩu mới và xác nhận mật khẩu không khớp!"
}
```

### ❌ Mật khẩu quá ngắn (400 Bad Request):
```json
{
  "error": "PASSWORD_MISMATCH",
  "message": "Mật khẩu mới phải có ít nhất 6 ký tự!"
}
```

### ❌ Mật khẩu mới giống mật khẩu cũ (400 Bad Request):
```json
{
  "error": "PASSWORD_MISMATCH",
  "message": "Mật khẩu mới phải khác mật khẩu hiện tại!"
}
```

### ❌ Không có quyền (403 Forbidden):
```json
{
  "error": "ACCESS_DENIED",
  "message": "Not authenticated"
}
```

## 🧪 Test Cases:

### Test 1: Đổi mật khẩu thành công
```bash
# 1. Login để lấy token
POST /users/login
Body: {"username": "student1", "password": "oldpass"}
Response: { "token": "abc123..." }

# 2. Đổi mật khẩu
PUT /students/me/change-password
Header: Authorization: Bearer abc123...
Body: {
  "currentPassword": "oldpass",
  "newPassword": "newpass123",
  "confirmPassword": "newpass123"
}
Expected: 200 OK - "Đổi mật khẩu thành công!"

# 3. Login lại với mật khẩu mới
POST /users/login
Body: {"username": "student1", "password": "newpass123"}
Expected: 200 OK - Token mới
```

### Test 2: Sai mật khẩu hiện tại
```bash
PUT /students/me/change-password
Body: {
  "currentPassword": "wrongpass",
  "newPassword": "newpass123",
  "confirmPassword": "newpass123"
}
Expected: 401 - "Mật khẩu hiện tại không đúng!"
```

### Test 3: Mật khẩu xác nhận không khớp
```bash
PUT /students/me/change-password
Body: {
  "currentPassword": "oldpass",
  "newPassword": "newpass123",
  "confirmPassword": "different456"
}
Expected: 400 - "Mật khẩu mới và xác nhận mật khẩu không khớp!"
```

### Test 4: Mật khẩu quá ngắn
```bash
PUT /students/me/change-password
Body: {
  "currentPassword": "oldpass",
  "newPassword": "12345",
  "confirmPassword": "12345"
}
Expected: 400 - "Mật khẩu mới phải có ít nhất 6 ký tự!"
```

### Test 5: Mật khẩu mới giống mật khẩu cũ
```bash
PUT /students/me/change-password
Body: {
  "currentPassword": "oldpass",
  "newPassword": "oldpass",
  "confirmPassword": "oldpass"
}
Expected: 400 - "Mật khẩu mới phải khác mật khẩu hiện tại!"
```

## 🎨 Frontend Integration (Angular):

### Service Method:
```typescript
// student.service.ts hoặc teacher.service.ts
changePassword(data: {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}): Observable<any> {
  return this.http.put('/students/me/change-password', data);
  // Hoặc: this.http.put('/teachers/me/change-password', data);
}
```

### Component:
```typescript
onChangePassword() {
  if (this.passwordForm.invalid) {
    this.message.error('Vui lòng điền đầy đủ thông tin!');
    return;
  }

  const data = this.passwordForm.value;
  
  this.studentService.changePassword(data).subscribe({
    next: (response) => {
      this.message.success('Đổi mật khẩu thành công!');
      this.passwordForm.reset();
      // Optional: Đăng xuất và yêu cầu đăng nhập lại
      // this.authService.logout();
      // this.router.navigate(['/login']);
    },
    error: (error) => {
      if (error.status === 401) {
        this.message.error('Mật khẩu hiện tại không đúng!');
      } else if (error.status === 400) {
        this.message.error(error.error.message || 'Thông tin không hợp lệ!');
      } else {
        this.message.error('Đã xảy ra lỗi. Vui lòng thử lại!');
      }
    }
  });
}
```

### HTML Form:
```html
<form [formGroup]="passwordForm" (ngSubmit)="onChangePassword()">
  <div>
    <label>Mật khẩu hiện tại</label>
    <input type="password" formControlName="currentPassword" required>
  </div>
  
  <div>
    <label>Mật khẩu mới (tối thiểu 6 ký tự)</label>
    <input type="password" formControlName="newPassword" required minlength="6">
  </div>
  
  <div>
    <label>Xác nhận mật khẩu mới</label>
    <input type="password" formControlName="confirmPassword" required>
  </div>
  
  <button type="submit" [disabled]="passwordForm.invalid">
    Đổi mật khẩu
  </button>
</form>
```

## 🔒 Security Notes:

1. **Validation:** Backend validate tất cả input để đảm bảo an toàn
2. **Authentication:** Chỉ user đã đăng nhập mới được đổi mật khẩu của chính họ
3. **Verify Current Password:** Phải nhập đúng mật khẩu hiện tại mới được đổi
4. **Password Requirements:** Mật khẩu mới phải >= 6 ký tự
5. **Password Confirmation:** Đảm bảo user không nhập nhầm mật khẩu mới
6. **Prevent Reuse:** Không cho phép đặt mật khẩu mới giống mật khẩu cũ

## ✅ Status:
- [x] Backend API hoàn thành
- [x] Validation logic hoàn chỉnh
- [x] Exception handling đầy đủ
- [x] Security rules cấu hình đúng
- [x] Sẵn sàng để test
- [x] Documentation đầy đủ

## 📌 Next Steps:
1. Restart backend server
2. Test các endpoint bằng Postman
3. Implement UI form trên frontend
4. Test end-to-end flow

## 🚀 Ready to Use!
Tính năng đã sẵn sàng để sử dụng. Chỉ cần restart server và test!

