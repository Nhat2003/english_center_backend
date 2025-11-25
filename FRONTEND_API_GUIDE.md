# 📱 API CHO FRONTEND - Chức năng Quên Mật Khẩu

## 🎯 TỔNG QUAN LUỒNG

```
User nhập email → API 1: Tạo token → Email gửi với link
                                    ↓
User click link → Frontend mở trang reset → User nhập mật khẩu mới
                                    ↓
                        API 2: Reset password → Thành công → Login
```

---

## 📋 DANH SÁCH API

### 1️⃣ API Yêu cầu đặt lại mật khẩu (Forgot Password)

**Endpoint**: `POST /auth/forgot-password`

**Mục đích**: Tạo token reset và gửi email cho user

**Request**:
```http
POST http://localhost:8080/auth/forgot-password
Content-Type: application/json

{
  "email": "student"
}
```

**Response Success (200 OK)**:
```json
{
  "message": "Reset email sent",
  "token": "abc-123-def-456-789-xyz",
  "devNote": "Token is returned for testing only. In production, user must check email."
}
```

**Response Error (400 Bad Request)**:
```json
{
  "message": "Email does not exist"
}
```

**Lưu ý**:
- `email` là username trong hệ thống (ví dụ: "student", "teacher", "admin")
- Response có `token` chỉ dùng cho **development/testing**
- Trong **production**, frontend chỉ hiển thị thông báo "Email đã được gửi" (không hiển thị token)
- User phải check email để lấy link reset

---

### 2️⃣ API Đặt lại mật khẩu (Reset Password)

**Endpoint**: `POST /auth/reset-password`

**Mục đích**: Đổi mật khẩu mới bằng token

**Request**:
```http
POST http://localhost:8080/auth/reset-password
Content-Type: application/json

{
  "token": "abc-123-def-456-789-xyz",
  "newPassword": "NewPassword123!"
}
```

**Response Success (200 OK)**:
```json
{
  "message": "Password updated"
}
```

**Response Error (400 Bad Request)**:
```json
{
  "message": "Invalid token"
}
// hoặc
{
  "message": "Token expired"
}
// hoặc
{
  "message": "User not found"
}
```

**Lưu ý**:
- Token có thời hạn **15 phút**
- Token chỉ dùng được **1 lần** (sau khi reset thành công sẽ bị xóa)
- `newPassword` nên có validation: độ dài tối thiểu, ký tự đặc biệt, v.v.

---

## 🎨 FRONTEND CẦN TẠO

### 1. Trang "Quên mật khẩu" (`/forgot-password`)

**UI Components**:
- Form nhập email/username
- Button "Gửi yêu cầu"
- Thông báo thành công/lỗi

**Code mẫu (Angular/TypeScript)**:
```typescript
// forgot-password.component.ts

import { HttpClient } from '@angular/common/http';

export class ForgotPasswordComponent {
  email: string = '';
  message: string = '';
  isLoading: boolean = false;

  constructor(private http: HttpClient) {}

  onSubmit() {
    if (!this.email) {
      this.message = 'Vui lòng nhập email/username';
      return;
    }

    this.isLoading = true;
    this.http.post('http://localhost:8080/auth/forgot-password', {
      email: this.email
    }).subscribe({
      next: (response: any) => {
        this.message = 'Email đã được gửi! Vui lòng kiểm tra hộp thư của bạn.';
        this.isLoading = false;
        // Optional: Redirect về login sau 3s
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 3000);
      },
      error: (error) => {
        this.message = error.error.message || 'Có lỗi xảy ra';
        this.isLoading = false;
      }
    });
  }
}
```

**HTML Template**:
```html
<!-- forgot-password.component.html -->
<div class="forgot-password-container">
  <h2>Quên mật khẩu?</h2>
  <p>Nhập email/username để nhận link đặt lại mật khẩu</p>
  
  <form (ngSubmit)="onSubmit()">
    <input 
      type="text" 
      [(ngModel)]="email" 
      placeholder="Email hoặc Username"
      name="email"
      required
    />
    
    <button type="submit" [disabled]="isLoading">
      {{ isLoading ? 'Đang gửi...' : 'Gửi yêu cầu' }}
    </button>
  </form>
  
  <div *ngIf="message" [class.success]="!message.includes('lỗi')" class="message">
    {{ message }}
  </div>
  
  <a routerLink="/login">Quay lại đăng nhập</a>
</div>
```

---

### 2. Trang "Đặt lại mật khẩu" (`/reset-password`)

**URL Format**: `http://localhost:4200/reset-password?token=abc-123-def-456`

**UI Components**:
- Form nhập mật khẩu mới
- Form xác nhận mật khẩu
- Button "Đặt lại mật khẩu"
- Thông báo thành công/lỗi
- Hiển thị trạng thái token (hợp lệ/hết hạn)

**Code mẫu (Angular/TypeScript)**:
```typescript
// reset-password.component.ts

import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';

export class ResetPasswordComponent implements OnInit {
  token: string = '';
  newPassword: string = '';
  confirmPassword: string = '';
  message: string = '';
  isLoading: boolean = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient
  ) {}

  ngOnInit() {
    // Lấy token từ URL query params
    this.route.queryParams.subscribe(params => {
      this.token = params['token'];
      if (!this.token) {
        this.message = 'Liên kết không hợp lệ';
      }
    });
  }

  onSubmit() {
    // Validation
    if (!this.newPassword || !this.confirmPassword) {
      this.message = 'Vui lòng nhập đầy đủ thông tin';
      return;
    }

    if (this.newPassword !== this.confirmPassword) {
      this.message = 'Mật khẩu xác nhận không khớp';
      return;
    }

    if (this.newPassword.length < 8) {
      this.message = 'Mật khẩu phải có ít nhất 8 ký tự';
      return;
    }

    // Call API
    this.isLoading = true;
    this.http.post('http://localhost:8080/auth/reset-password', {
      token: this.token,
      newPassword: this.newPassword
    }).subscribe({
      next: (response: any) => {
        this.message = 'Mật khẩu đã được đặt lại thành công!';
        this.isLoading = false;
        
        // Redirect về login sau 2s
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 2000);
      },
      error: (error) => {
        const errorMsg = error.error.message || 'Có lỗi xảy ra';
        
        if (errorMsg.includes('expired')) {
          this.message = 'Liên kết đã hết hạn. Vui lòng yêu cầu lại.';
        } else if (errorMsg.includes('Invalid')) {
          this.message = 'Liên kết không hợp lệ.';
        } else {
          this.message = errorMsg;
        }
        
        this.isLoading = false;
      }
    });
  }
}
```

**HTML Template**:
```html
<!-- reset-password.component.html -->
<div class="reset-password-container">
  <h2>Đặt lại mật khẩu</h2>
  
  <div *ngIf="!token" class="error">
    <p>Liên kết không hợp lệ</p>
    <a routerLink="/forgot-password">Yêu cầu link mới</a>
  </div>
  
  <form *ngIf="token" (ngSubmit)="onSubmit()">
    <div class="form-group">
      <label>Mật khẩu mới</label>
      <input 
        type="password" 
        [(ngModel)]="newPassword" 
        placeholder="Nhập mật khẩu mới"
        name="newPassword"
        required
        minlength="8"
      />
      <small>Tối thiểu 8 ký tự</small>
    </div>
    
    <div class="form-group">
      <label>Xác nhận mật khẩu</label>
      <input 
        type="password" 
        [(ngModel)]="confirmPassword" 
        placeholder="Nhập lại mật khẩu"
        name="confirmPassword"
        required
      />
    </div>
    
    <button type="submit" [disabled]="isLoading">
      {{ isLoading ? 'Đang xử lý...' : 'Đặt lại mật khẩu' }}
    </button>
  </form>
  
  <div *ngIf="message" 
       [class.success]="message.includes('thành công')" 
       [class.error]="!message.includes('thành công')"
       class="message">
    {{ message }}
  </div>
</div>
```

---

## 🔗 ROUTING (Angular)

**app-routing.module.ts**:
```typescript
const routes: Routes = [
  // ... existing routes
  {
    path: 'forgot-password',
    component: ForgotPasswordComponent
  },
  {
    path: 'reset-password',
    component: ResetPasswordComponent
  }
];
```

---

## 📧 EMAIL CONTENT

User sẽ nhận email với nội dung:

**Subject**: Reset mật khẩu - English Center

**Body**:
```
Chào [Tên user],

Bạn (hoặc người dùng khác) đã yêu cầu đặt lại mật khẩu. 
Vui lòng bấm vào đường dẫn bên dưới để đặt lại mật khẩu (hết hạn sau 15 phút):

http://localhost:4200/reset-password?token=abc-123-def-456-789

Nếu bạn không yêu cầu, vui lòng bỏ qua email này.
```

**Link format**: `http://localhost:4200/reset-password?token=YOUR_TOKEN`

---

## ⚠️ XỬ LÝ LỖI

### Lỗi thường gặp:

**1. "Email does not exist"**
- Hiển thị: "Email/Username không tồn tại trong hệ thống"
- Action: Yêu cầu user kiểm tra lại email/username

**2. "Invalid token"**
- Hiển thị: "Liên kết không hợp lệ"
- Action: Redirect về `/forgot-password` với thông báo "Vui lòng yêu cầu link mới"

**3. "Token expired"**
- Hiển thị: "Liên kết đã hết hạn (>15 phút)"
- Action: Redirect về `/forgot-password` với thông báo "Liên kết đã hết hạn, vui lòng yêu cầu lại"

**4. Mật khẩu không khớp (frontend validation)**
- Hiển thị: "Mật khẩu xác nhận không khớp"
- Action: Highlight field lỗi

---

## 🔒 SECURITY NOTES

### Frontend phải validate:
- ✅ Email/username không được rỗng
- ✅ Mật khẩu mới tối thiểu 8 ký tự
- ✅ Có ít nhất 1 chữ hoa, 1 chữ thường, 1 số (optional)
- ✅ Mật khẩu xác nhận phải khớp

### Không nên:
- ❌ Hiển thị token trong URL bar (đã là query param, ok)
- ❌ Lưu token vào localStorage
- ❌ Log token ra console
- ❌ Gửi token qua analytics

---

## 🧪 TEST FLOW

### Test case 1: Happy path
1. Vào `/forgot-password`
2. Nhập email: "student"
3. Submit → Thấy "Email đã được gửi"
4. Check email → Click link
5. Mở `/reset-password?token=...`
6. Nhập mật khẩu mới: "NewPassword123!"
7. Xác nhận mật khẩu: "NewPassword123!"
8. Submit → Thấy "Mật khẩu đã được đ���t lại thành công"
9. Tự động redirect về `/login` sau 2s
10. Login với mật khẩu mới → Thành công ✅

### Test case 2: Email không tồn tại
1. Nhập email: "notexist@test.com"
2. Submit → Thấy lỗi "Email does not exist"

### Test case 3: Token hết hạn
1. Lấy token
2. Đợi 16 phút
3. Mở link reset → Submit
4. Thấy lỗi "Token expired"

### Test case 4: Mật khẩu không khớp
1. Mật khẩu mới: "Pass123!"
2. Xác nhận: "Pass456!"
3. Submit → Thấy lỗi "Mật khẩu xác nhận không khớp"

---

## 🎨 UI/UX RECOMMENDATIONS

### Trang Forgot Password:
- ✅ Đơn giản, chỉ 1 input field và 1 button
- ✅ Link "Quay lại đăng nhập" rõ ràng
- ✅ Loading state khi đang gửi request
- ✅ Success message màu xanh, error màu đỏ

### Trang Reset Password:
- ✅ Hiển thị trạng thái token (valid/expired) ngay khi load
- ✅ Password strength indicator (optional)
- ✅ Show/hide password icon
- ✅ Confirm password có validation real-time
- ✅ Countdown timer 15 phút (optional)
- ✅ Redirect tự động về login sau khi thành công

---

## 📦 DEPENDENCIES CẦN CÀI

```bash
# Angular HttpClient (thường đã có)
npm install @angular/common

# Angular Forms (nếu dùng ngModel)
npm install @angular/forms

# Angular Router
npm install @angular/router
```

---

## ✅ CHECKLIST FRONTEND

- [ ] Tạo component `ForgotPasswordComponent`
- [ ] Tạo component `ResetPasswordComponent`
- [ ] Thêm routes cho `/forgot-password` và `/reset-password`
- [ ] Implement API call `/auth/forgot-password`
- [ ] Implement API call `/auth/reset-password`
- [ ] Xử lý query param `token` từ URL
- [ ] Validation form (password match, length, etc.)
- [ ] Xử lý các error cases
- [ ] Success messages và redirect
- [ ] UI/UX styling
- [ ] Test toàn bộ flow

---

**Sau khi implement xong, test theo flow trong phần 🧪 TEST FLOW!**

