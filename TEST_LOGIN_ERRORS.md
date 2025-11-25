# Testing Login Error Handling

## Các trường hợp test:

### 1. Đăng nhập SAI tên đăng nhập hoặc mật khẩu

**Request:**
```http
POST http://localhost:8080/users/login
Content-Type: application/json

{
  "username": "wrong_user",
  "password": "wrong_pass"
}
```

**Expected Response:**
```json
HTTP/1.1 401 Unauthorized
{
  "error": "INVALID_CREDENTIALS",
  "message": "Sai tên đăng nhập hoặc mật khẩu!"
}
```

### 2. Đăng nhập với tài khoản BỊ KHÓA

**Giả sử có user:**
- username: "locked_user"
- password: "correct_password"
- isActive: false (bị khóa)

**Request:**
```http
POST http://localhost:8080/users/login
Content-Type: application/json

{
  "username": "locked_user",
  "password": "correct_password"
}
```

**Expected Response:**
```json
HTTP/1.1 403 Forbidden
{
  "error": "ACCOUNT_LOCKED",
  "message": "Tài khoản đã bị khóa!"
}
```

### 3. Đăng nhập THÀNH CÔNG

**Request:**
```http
POST http://localhost:8080/users/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin"
}
```

**Expected Response:**
```json
HTTP/1.1 200 OK
{
  "token": "eyJhbGc...",
  "user": {
    "id": 1,
    "username": "admin",
    "role": "ADMIN",
    ...
  }
}
```

## Cách test với Postman:

1. **Test sai thông tin đăng nhập:**
   - Endpoint: POST http://localhost:8080/users/login
   - Body: {"username": "wronguser", "password": "wrongpass"}
   - Kết quả mong đợi: Status 401 + message "Sai tên đăng nhập hoặc mật khẩu!"

2. **Test tài khoản bị khóa:**
   - Bước 1: Tạo một user và set isActive = false trong database
   - Bước 2: POST http://localhost:8080/users/login với username/password đúng của user đó
   - Kết quả mong đợi: Status 403 + message "Tài khoản đã bị khóa!"

## Frontend Integration:

Frontend có thể kiểm tra response như sau:

```typescript
try {
  const response = await this.http.post('/users/login', credentials).toPromise();
  // Login thành công
  this.storeToken(response.token);
  this.navigateToDashboard();
} catch (error) {
  if (error.status === 401) {
    // Sai thông tin đăng nhập
    this.showError('Sai tên đăng nhập hoặc mật khẩu!');
  } else if (error.status === 403) {
    // Tài khoản bị khóa
    this.showError('Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên!');
  } else {
    // Lỗi khác
    this.showError('Đã xảy ra lỗi. Vui lòng thử lại!');
  }
}
```

## Security Note:

Theo best practice bảo mật, khi sai username HOẶC sai password, đều trả về cùng một message chung "Sai tên đăng nhập hoặc mật khẩu!" để tránh việc attacker dò tìm username có tồn tại hay không.

Chỉ khi username + password ĐÃ ĐÚNG, mới kiểm tra và thông báo cụ thể "Tài khoản đã bị khóa".

