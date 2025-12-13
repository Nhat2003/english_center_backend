# Duplicate Users Handling - Documentation

## Scenario: User Nằm Trong Nhiều Classes

### Example Setup
```
User A (Student) belongs to: Class 1, Class 2
User B (Student) belongs to: Class 1
User C (Student) belongs to: Class 2
Teacher T (Teacher) teaches: Class 1, Class 2
```

---

## Behavior Analysis

### Case 1: Search with classId=1
**Request:**
```
GET /chat/contacts?classId=1
GET /users/search?classId=1
Authorization: Bearer {user_token}
```

**Expected Result:**
- User A: ✅ Returned (member of Class 1)
- User B: ✅ Returned (member of Class 1)
- User C: ❌ NOT returned (not in Class 1)
- Teacher T: ✅ Returned (teaches Class 1)

**Deduplication:** User A appears only **1 time** (not duplicated)

---

### Case 2: Search with classId=2
**Request:**
```
GET /chat/contacts?classId=2
GET /users/search?classId=2
Authorization: Bearer {user_token}
```

**Expected Result:**
- User A: ✅ Returned (member of Class 2)
- User B: ❌ NOT returned (not in Class 2)
- User C: ✅ Returned (member of Class 2)
- Teacher T: ✅ Returned (teaches Class 2)

**Deduplication:** User A appears only **1 time** (not duplicated)

---

### Case 3: Search WITHOUT classId (Global)
**Request:**
```
GET /chat/contacts
GET /users/search?q=test
Authorization: Bearer {user_token}
```

**Expected Result** (if user is Student in Class 1, 2):
- User A: ✅ Returned once (appears in both Class 1 & 2, but deduplicated)
- User B: ✅ Returned (from Class 1)
- User C: ✅ Returned (from Class 2)
- Teacher T: ✅ Returned (teaches both classes)
- Admin: ✅ Returned (always included)

**Deduplication:** All users appear exactly **1 time each** despite being in multiple classes

---

## 🔧 Implementation Details

### /chat/contacts
```java
Map<Long, ChatContactDTO> map = new HashMap<>();
// ... loop through classes and add users
map.putIfAbsent(userId, contactDTO);  // ← Prevents duplicates
```

**Mechanism:** `HashMap.putIfAbsent()` only adds if key doesn't exist
- First encounter of User A (from Class 1) → added
- Second encounter of User A (from Class 2) → ignored
- Result: User A appears 1 time

---

### /users/search
```java
Set<Long> allowedUserIds = new HashSet<>();
// ... collect user IDs from classes
// allowedUserIds automatically deduplicates (Set property)

results = userService.searchUsers(q, role).stream()
    .filter(user -> allowedUserIds.contains(user.getId()))
    .collect(Collectors.toList());
```

**Mechanism:** `HashSet` automatically deduplicates
- User A ID added from Class 1 → in set
- User A ID added from Class 2 → already in set (ignored)
- Filter returns User A only once
- Result: User A appears 1 time

---

## ✅ Verification

### Test Case 1: User in Multiple Classes (Global Search)
```bash
# User A is in Class 1 and Class 2
GET /chat/contacts
Authorization: Bearer {userA_token}

# Expected: User A appears exactly 1 time in response
# Verify by checking response size and userId uniqueness
```

### Test Case 2: User in Multiple Classes (Class-Scoped Search)
```bash
# User A is in Class 1 and Class 2
GET /chat/contacts?classId=1
GET /chat/contacts?classId=2

# Expected: User A appears in both responses, 1 time each
# No duplication within each response
```

### Test Case 3: Search with Filter
```bash
GET /users/search?q=Nguyen&classId=1

# If multiple Nguyen users in Class 1: all returned (correct)
# If same Nguyen in Class 1 twice: only returned once (deduplicated)
```

---

## 📊 Data Flow

```
Scenario: User A in Class 1 and Class 2, searching globally

1. Get all class IDs for User A
   → [1, 2]

2. Load ClassRoom entities
   → [Class 1, Class 2]

3. Extract user IDs from both classes
   - Class 1: [A, B, Teacher T, Admin]
   - Class 2: [A, C, Teacher T, Admin]

4. Deduplicate (HashMap or HashSet)
   → [A, B, C, Teacher T, Admin]  ← A appears once

5. Return
   → List with 5 items (A not duplicated)
```

---

## 🎯 Summary

| Scenario | Behavior | Implementation |
|----------|----------|-----------------|
| User in 1 class, search global | Appears 1 time | Natural (only in 1 class) |
| User in 2 classes, search global | Appears 1 time | HashMap/HashSet dedup |
| User in 2 classes, search class 1 | Appears 1 time | Filter by class 1 members |
| User in 2 classes, search class 2 | Appears 1 time | Filter by class 2 members |

✅ **Result:** NO DUPLICATES in all scenarios

---

## 🔐 Security & Performance

**Security:**
- ✅ No data leakage (only users user can see)
- ✅ Deduplication is correct (user appears as 1 entity)
- ✅ Class isolation maintained when classId provided

**Performance:**
- ✅ HashMap lookup: O(1)
- ✅ HashSet lookup: O(1)
- ✅ No N+1 queries
- ✅ Efficient for typical class size (20-100 students)

---

## 📝 Notes for Frontend

**No special handling needed:**
- Backend returns clean list (no duplicates)
- Frontend can directly render response
- No need for deduplication on client side

**Example Frontend Code:**
```typescript
// Safe to use directly
this.chatService.getContacts().subscribe(contacts => {
  this.displayContacts(contacts);  // No duplicates
});
```

---

**Last Updated:** 13/12/2025  
**Status:** ✅ Verified & Correct

