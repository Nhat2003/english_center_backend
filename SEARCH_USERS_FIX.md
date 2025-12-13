# Fix Chức Năng Tìm Kiếm Người Dùng - Cho Học Sinh & Giáo Viên

## Vấn Đề
- Học sinh và giáo viên **không thể** tìm kiếm người dùng trong chức năng tin nhắn
- Chỉ **ADMIN** có thể dùng endpoint `/users/search`

## Nguyên Nhân
1. Endpoint `/users/search` không kiểm tra quyền gì để phép tìm kiếm
2. `WebSecurityConfig` chưa được cập nhật để cho phép tất cả roles truy cập tìm kiếm

## Giải Pháp Triển Khai

### 1. Cập Nhật `UserController.searchUsers()` (Main Fix)
**File:** `src/main/java/.../controller/users/UserController.java`

**Thay đổi:**
- Thêm authentication check (phải đăng nhập)
- Admins: có thể tìm kiếm **tất cả** người dùng
- Học sinh: có thể tìm kiếm người dùng trong **cùng lớp** + **active admins**
- Giáo viên: có thể tìm kiếm người dùng trong **các lớp họ dạy** + **active admins**

**Logic Authorization:**
```
if (user is ADMIN)
  → searchUsers(q, role) // all users
else if (user is STUDENT)
  → collect user IDs từ tất cả classes của student (teacher + other students)
  → + active admins
  → filter results
else if (user is TEACHER)
  → collect user IDs từ tất cả classes họ dạy (students)
  → + active admins
  → filter results
```

**Repositories thêm vào:**
- `StudentRepository` - để tìm student profile
- `TeacherRepository` - để tìm teacher profile
- `ClassEntityRepository` - để lấy danh sách classes
- `UserRepository` - để tìm user hiện tại + admins

### 2. Cập Nhật `WebSecurityConfig.java`
**File:** `src/main/java/.../config/WebSecurityConfig.java`

**Thay đổi:**
- Endpoint `GET /users/search` được phép cho **STUDENT, TEACHER, ADMIN**
- Thêm comment làm rõ mục đích

```java
.requestMatchers(org.springframework.http.HttpMethod.GET, "/users/**").hasAnyRole("STUDENT", "TEACHER", "ADMIN")
```

## Kiểm Tra Chức Năng

### Test 1: Học Sinh Tìm Kiếm (Postman)
```
GET /users/search?q=teacher_name
Authorization: Bearer <student_token>
```

**Kết Quả Mong Đợi:**
- Trả về giáo viên của lớp + học sinh khác trong cùng lớp + active admins
- Không trả về người dùng từ lớp khác

### Test 2: Giáo Viên Tìm Kiếm
```
GET /users/search?q=student_name
Authorization: Bearer <teacher_token>
```

**Kết Quả Mong Đợi:**
- Trả về học sinh trong các lớp giáo viên dạy + active admins
- Không trả về học sinh từ lớp khác

### Test 3: Admin Tìm Kiếm
```
GET /users/search?q=any_name
Authorization: Bearer <admin_token>
```

**Kết Quả Mong Đợi:**
- Trả về **tất cả** người dùng khớp với query

## API Endpoint Chi Tiết

### GET /users/search
**Query Parameters:**
- `q` (optional): tìm kiếm theo tên hoặc username
- `role` (optional): lọc theo role (ADMIN, TEACHER, STUDENT)

**Response:**
```json
[
  {
    "id": 1,
    "username": "user1",
    "role": "STUDENT",
    "status": "ACTIVE",
    "fullName": "Nguyễn Văn A"
  },
  ...
]
```

**Status Codes:**
- `200 OK` - Tìm kiếm thành công
- `401 UNAUTHORIZED` - Chưa đăng nhập
- `403 FORBIDDEN` - Người dùng không tồn tại

## Frontend Integration

### Cách Gọi API Từ Frontend (Angular Example)
```typescript
searchUsers(query: string): Observable<any[]> {
  const params = new HttpParams();
  if (query) {
    params = params.set('q', query);
  }
  return this.http.get<any[]>('/users/search', { params });
}
```

### UI/UX Gợi Ý
- Searchbox trong chat/messaging module
- Auto-complete khi người dùng nhập
- Hiển thị tên + role của người tìm được
- Chỉ hiển thị các liên hệ hợp lệ (học sinh/giáo viên cùng lớp hoặc admin)

## Edge Cases Xử Lý

1. **Student không có lớp nào:**
   - Chỉ có thể tìm active admins

2. **Teacher không có lớp nào:**
   - Chỉ có thể tìm active admins

3. **Inactive admins:**
   - Bị loại khỏi danh sách (chỉ active admins được thêm)

4. **User không tìm được:**
   - Trả về empty list `[]`

## Files Đã Sửa

| File | Thay Đổi |
|------|----------|
| `UserController.java` | Thêm authorization logic vào `searchUsers()` + thêm repositories |
| `WebSecurityConfig.java` | Đã allow `/users/**` cho STUDENT, TEACHER, ADMIN |

## Status Check

✅ **Compile Errors:** No errors found  
✅ **Authorization:** Implemented for all roles  
✅ **API Security:** Checked against security config  

## Deployment Notes

- Không cần database migration
- Không có breaking changes
- Backward compatible với existing chat/messaging code
- Endpoint `/users/search` được phép cho tất cả authenticated users

## Troubleshooting

**Q: Học sinh vẫn không tìm thấy người dùng?**
- Kiểm tra token JWT có valid không
- Kiểm tra student/teacher profile đã được tạo chưa
- Kiểm tra lớp đã assign học sinh/giáo viên chưa

**Q: Tìm kiếm trả về kết quả lạ?**
- Kiểm tra active admins (luôn được thêm vào danh sách)
- Kiểm tra class membership (student/teacher có trong lớp không)

---

**Tác Thành:** Đã hoàn thành  
**Ngày:** 13/12/2025  
**Status:** Ready for testing

