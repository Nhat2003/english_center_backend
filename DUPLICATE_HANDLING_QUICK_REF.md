# Quick Reference - Duplicate Handling

## TL;DR

**Câu hỏi:** Nếu user nằm trong nhiều classes, sẽ bị duplicate không?

**Trả lời:** ❌ **KHÔNG BỊ DUPLICATE**

---

## Why Not?

### /chat/contacts
```java
Map<Long, ChatContactDTO> map = new HashMap<>();
map.putIfAbsent(userId, contact);  // ← Chỉ add 1 lần
```
- HashMap tự động dedup (không add key nếu đã tồn tại)

### /users/search
```java
Set<Long> allowedUserIds = new HashSet<>();
allowedUserIds.add(userId);  // ← Set tự động dedup
```
- HashSet tự động dedup (không add value nếu đã tồn tại)

---

## Examples

### User A in Class 1 & 2

#### Global Search (no classId)
```
GET /chat/contacts

Response:
[
  { id: 1, name: "User A", role: "STUDENT" },  ← Appears 1 time
  { id: 2, name: "User B", role: "STUDENT" },
  { id: 3, name: "Teacher T", role: "TEACHER" },
  ...
]
```

#### Class 1 Search
```
GET /chat/contacts?classId=1

Response:
[
  { id: 1, name: "User A", role: "STUDENT" },  ← Appears 1 time
  { id: 2, name: "User B", role: "STUDENT" },
  { id: 3, name: "Teacher T", role: "TEACHER" },
  ...
]
```

#### Class 2 Search
```
GET /chat/contacts?classId=2

Response:
[
  { id: 1, name: "User A", role: "STUDENT" },  ← Appears 1 time
  { id: 4, name: "User C", role: "STUDENT" },
  { id: 3, name: "Teacher T", role: "TEACHER" },
  ...
]
```

✅ **User A always appears exactly 1 time** - no duplication!

---

## Verification

To verify no duplicates in response:

```javascript
// Frontend verification
const response = [/* API response */];
const userIds = response.map(u => u.id);
const uniqueIds = new Set(userIds);

if (userIds.length === uniqueIds.size) {
  console.log("✅ No duplicates");
} else {
  console.log("❌ Duplicates found!");
}
```

---

## Status

✅ **Implementation:** Correct  
✅ **No duplicates:** Guaranteed  
✅ **Ready for production:** YES

---

**Last Updated:** 13/12/2025

