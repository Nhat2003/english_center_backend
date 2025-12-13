# Quick Start - Test All New Features

## 🚀 Setup

### 1. Build Project
```bash
cd D:\Learning\Learning\DoAnTotNghiep\Code\english_center_backend
./mvnw.cmd clean package -DskipTests
```

### 2. Start Server
```bash
./mvnw.cmd spring-boot:run
# Or start from IDE
```

### 3. Import Postman Collection
- Open Postman
- File → Import
- Select `Postman_UserSearch_Collection.json`

---

## 🧪 Quick Test Scenarios

### Feature 1: Điểm Danh Bất Cứ Ngày Nào

**Step 1: Login as Teacher**
```
POST http://localhost:8080/users/login
Body: {
  "username": "teacher1",
  "password": "password123"
}
Copy token from response
```

**Step 2: Create Attendance for Any Date**
```
POST http://localhost:8080/attendance/session
Headers: Authorization: Bearer {token}
Body: {
  "classId": 1,
  "sessionDate": "2025-11-20",  // Past date - OK!
  "items": [
    { "studentId": 1, "status": "PRESENT", "note": "" },
    { "studentId": 2, "status": "ABSENT", "note": "sick" }
  ]
}
```

**Expected:** 201 Created ✅ (không lỗi day-of-week)

---

### Feature 2: Thông Báo Khi Đổi Lịch

**Step 1: Reschedule with Notifications**
```
POST http://localhost:8080/class-rooms/1/sessions/2025-12-10/reschedule
Headers: Authorization: Bearer {teacher_token}
Body: {
  "newDate": "2025-12-15",
  "newStartTime": "14:00",
  "newEndTime": "15:30",
  "reason": "Giáo viên bận",
  "notifyStudents": true
}
```

**Expected:** 200 OK with SessionOverride response

**Step 2: Verify Announcement Created**
```
GET http://localhost:8080/classes/1/announcements
Headers: Authorization: Bearer {token}
```

**Expected:** Danh sách announcements bao gồm thông báo mới

**Step 3: Verify Notifications Sent**
```
GET http://localhost:8080/classes/students/1/notifications
Headers: Authorization: Bearer {student_token}
```

**Expected:** Notification với nội dung "Lịch học ngày ... được đổi sang ..."

---

### Feature 3: Học Sinh & Giáo Viên Tìm Kiếm Người Dùng

#### A. Test as Student

**Step 1: Login as Student**
```
POST http://localhost:8080/users/login
Body: {
  "username": "student1",
  "password": "password123"
}
Copy token
```

**Step 2: Search Users (cùng lớp)**
```
GET http://localhost:8080/users/search?q=Nguyen
Headers: Authorization: Bearer {student_token}
```

**Expected:** 
- Trả về giáo viên của lớp
- Trả về học sinh khác trong cùng lớp  
- Trả về tất cả admin
- ❌ KHÔNG trả về người từ lớp khác

#### B. Test as Teacher

**Step 1: Login as Teacher**
```
POST http://localhost:8080/users/login
Body: {
  "username": "teacher1",
  "password": "password123"
}
Copy token
```

**Step 2: Search Students in Their Classes**
```
GET http://localhost:8080/users/search?role=STUDENT
Headers: Authorization: Bearer {teacher_token}
```

**Expected:**
- Trả về học sinh trong các lớp giáo viên dạy
- Trả về tất cả admin
- ❌ KHÔNG trả về học sinh từ lớp khác

#### C. Test as Admin

**Step 1: Login as Admin**
```
POST http://localhost:8080/users/login
Body: {
  "username": "admin",
  "password": "admin123"
}
Copy token
```

**Step 2: Search All Users**
```
GET http://localhost:8080/users/search
Headers: Authorization: Bearer {admin_token}
```

**Expected:** ✅ Trả về TẤT CẢ người dùng

---

## 🔍 Verify Database Changes

### Check Attendance Updated
```sql
SELECT * FROM attendance 
WHERE class_room_id = 1 
ORDER BY created_at DESC;
-- Verify session_date updated to new date
```

### Check Announcements Created
```sql
SELECT * FROM announcements 
WHERE class_id = 1 
ORDER BY created_at DESC;
-- Should see new announcement about schedule change
```

### Check Notifications Sent
```sql
SELECT * FROM notifications 
WHERE announcement_id IN (
  SELECT id FROM announcements 
  WHERE title = 'Lịch học đã được thay đổi'
);
-- Should have one record per student in class
```

---

## 📊 Expected Results Summary

| Feature | Before | After |
|---------|--------|-------|
| Điểm danh bất cứ ngày | ❌ Chỉ hôm nay | ✅ Bất cứ ngày trong class range |
| Thông báo đổi lịch | ❌ Không | ✅ Optional via API |
| Student search users | ❌ 403 Forbidden | ✅ Người trong cùng lớp |
| Teacher search users | ❌ 403 Forbidden | ✅ Người trong lớp họ dạy |
| Admin search users | ✅ Tất cả | ✅ Tất cả (không đổi) |

---

## 🐛 Common Issues & Solutions

### Issue: 401 Unauthorized on /users/search
```
Cause: Token invalid hoặc không được gửi
Fix: 
- Copy token từ /login response
- Paste vào Authorization header: Bearer {token}
- Kiểm tra token không hết hạn
```

### Issue: Search trả về empty list
```
Cause: User không có class hoặc class không có students
Fix:
- Kiểm tra student/teacher profile đã tạo
- Kiểm tra class assignment
- Kiểm tra admin status (active)
```

### Issue: Announcement không được tạo
```
Cause: AnnouncementService lỗi nhưng reschedule vẫn success (logged)
Fix:
- Kiểm tra classroom.teacher không null
- Kiểm tra students trong class có tồn tại
- Xem logs cho error details
```

---

## 📝 Sample Test Data

Ensure you have:
```
Class 1:
- Teacher: teacher1 (user_id = X)
- Students: student1, student2 (user_id = Y, Z)

Admins:
- admin (active)

Other Classes:
- Class 2: teacher2, student3
```

If missing, create via admin endpoints:
```
POST /users -> create users
POST /teachers -> create teacher profiles
POST /students -> create student profiles
POST /class-rooms -> create classes with assignments
```

---

## 💡 Tips

1. **Reuse Tokens:** Save login responses to use same token for multiple tests
2. **Environment Variables:** Set in Postman:
   - `{{BASE_URL}}` = http://localhost:8080
   - `{{student_token}}` = paste token
   - `{{teacher_token}}` = paste token
3. **Database Check:** Use IDE's database tool to verify changes
4. **Logs:** Monitor console output for hidden errors

---

## 🎯 Success Criteria

- ✅ Attendance created for any date (not just today)
- ✅ Announcements created when reschedule with notifyStudents=true
- ✅ Notifications sent to all students in class
- ✅ Students can search classmates + teacher + admins
- ✅ Teachers can search students in their classes + admins
- ✅ Admins can search all users
- ✅ No 403 errors for authorized users
- ✅ No compilation errors

---

**Last Updated:** 13/12/2025  
**Ready to Test:** YES ✅

