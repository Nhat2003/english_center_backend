# 🎯 FRONTEND - API NÀO CẦN GỌI?

## Tóm Tắt Nhanh

### 1️⃣ Lấy Danh Sách Contacts (Chatbox)

```typescript
// Tất cả contacts
GET /chat/contacts

// Contacts trong Class 1
GET /chat/contacts?classId=1
```

**Response:** `ChatContactDTO[]` - danh sách người có thể chat

---

### 2️⃣ Search Với Filter

```typescript
// Search theo tên
GET /users/search?q=Nguyen

// Search students trong Class 1
GET /users/search?classId=1&role=STUDENT

// Search teachers globally
GET /users/search?role=TEACHER
```

**Response:** `UserResponse[]` - danh sách đã filter

---

## Code Mẫu (Copy & Paste)

### Service
```typescript
@Injectable({ providedIn: 'root' })
export class ChatService {
  private baseUrl = 'http://localhost:8080';

  constructor(private http: HttpClient) {}

  // ✅ Recommend: Dùng này cho chatbox
  getContacts(classId?: number): Observable<any[]> {
    let params = new HttpParams();
    if (classId) {
      params = params.set('classId', classId.toString());
    }
    return this.http.get<any[]>(`${this.baseUrl}/chat/contacts`, { params });
  }

  // ✅ Dùng này cho search với filter
  searchUsers(query?: string, classId?: number, role?: string): Observable<any[]> {
    let params = new HttpParams();
    if (query) params = params.set('q', query);
    if (classId) params = params.set('classId', classId.toString());
    if (role) params = params.set('role', role);
    
    return this.http.get<any[]>(`${this.baseUrl}/users/search`, { params });
  }
}
```

### Component
```typescript
export class ChatComponent {
  contacts: any[] = [];

  ngOnInit() {
    // Load tất cả contacts
    this.chatService.getContacts().subscribe(contacts => {
      this.contacts = contacts;
    });

    // Hoặc load contacts trong Class 1
    this.chatService.getContacts(1).subscribe(contacts => {
      this.contacts = contacts;
    });
  }

  onSearch(query: string) {
    // Search với query
    this.chatService.searchUsers(query).subscribe(users => {
      this.contacts = users;
    });
  }
}
```

---

## Headers (Bắt Buộc)

```typescript
Authorization: Bearer {token}
```

Dùng **HttpInterceptor** để tự động thêm:
```typescript
@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  intercept(req: HttpRequest<any>, next: HttpHandler) {
    const token = localStorage.getItem('token');
    if (token) {
      req = req.clone({
        setHeaders: { Authorization: `Bearer ${token}` }
      });
    }
    return next.handle(req);
  }
}
```

---

## Errors

| Code | Meaning | Action |
|------|---------|--------|
| 401 | Not logged in | Redirect to /login |
| 403 | Not in this class | Show error message |
| 404 | Class not found | Show error message |

---

## ✅ Checklist

- [ ] Add `ChatService` với `getContacts()` và `searchUsers()`
- [ ] Add `AuthInterceptor` để auto-add token
- [ ] Handle 401/403 errors
- [ ] Test với classId và không có classId

---

**That's it!** 🚀 Chỉ cần 2 endpoints này là đủ.

**File chi tiết:** `FRONTEND_SEARCH_API_GUIDE.md`

