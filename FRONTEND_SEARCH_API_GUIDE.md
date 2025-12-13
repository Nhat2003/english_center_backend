# Frontend Guide - Search User APIs

## 🎯 Tóm Tắt Nhanh

Frontend có **2 endpoints chính** để search người dùng trong chat:

1. **`GET /chat/contacts`** - Lấy danh sách contacts (recommended)
2. **`GET /users/search`** - Search với filter chi tiết hơn

---

## 📌 Endpoint 1: GET /chat/contacts (Recommended)

### Khi Nào Dùng
- ✅ Lấy danh sách contacts để chat
- ✅ Hiển thị trong chatbox sidebar
- ✅ Search trong 1 lớp cụ thể hoặc toàn bộ

### API Call

#### 1a. Lấy Tất Cả Contacts (Global)
```typescript
// Service
getContacts(): Observable<ChatContactDTO[]> {
  return this.http.get<ChatContactDTO[]>('/chat/contacts');
}

// Component
this.chatService.getContacts().subscribe(contacts => {
  this.contactList = contacts;
});
```

**Request:**
```
GET http://localhost:8080/chat/contacts
Authorization: Bearer {token}
```

**Response:**
```json
[
  {
    "id": 1,
    "fullName": "Nguyễn Văn A",
    "role": "STUDENT"
  },
  {
    "id": 2,
    "fullName": "Trần Thị B",
    "role": "TEACHER"
  },
  {
    "id": 3,
    "fullName": "Admin",
    "role": "ADMIN"
  }
]
```

---

#### 1b. Lấy Contacts Trong 1 Lớp (Class-Scoped)
```typescript
// Service
getContactsInClass(classId: number): Observable<ChatContactDTO[]> {
  return this.http.get<ChatContactDTO[]>('/chat/contacts', {
    params: { classId: classId.toString() }
  });
}

// Component
this.chatService.getContactsInClass(1).subscribe(contacts => {
  this.classContacts = contacts;
});
```

**Request:**
```
GET http://localhost:8080/chat/contacts?classId=1
Authorization: Bearer {token}
```

**Response:** Chỉ users từ Class 1 + active admins

---

## 📌 Endpoint 2: GET /users/search (Advanced)

### Khi Nào Dùng
- ✅ Cần filter theo tên/username (q parameter)
- ✅ Cần filter theo role (STUDENT/TEACHER)
- ✅ Cần search phức tạp hơn

### API Call

#### 2a. Search Global (Tất Cả Contacts)
```typescript
// Service
searchUsers(query: string, role?: string): Observable<UserResponse[]> {
  let params = new HttpParams();
  if (query) {
    params = params.set('q', query);
  }
  if (role) {
    params = params.set('role', role);
  }
  
  return this.http.get<UserResponse[]>('/users/search', { params });
}

// Component
this.userService.searchUsers('Nguyen').subscribe(users => {
  this.searchResults = users;
});
```

**Request Examples:**
```
GET /users/search?q=Nguyen
GET /users/search?role=TEACHER
GET /users/search?q=Nguyen&role=STUDENT
```

**Response:**
```json
[
  {
    "id": 1,
    "username": "nguyenvana",
    "fullName": "Nguyễn Văn A",
    "role": "STUDENT",
    "status": "ACTIVE"
  }
]
```

---

#### 2b. Search Trong 1 Lớp (Class-Scoped)
```typescript
// Service
searchUsersInClass(classId: number, query?: string, role?: string): Observable<UserResponse[]> {
  let params = new HttpParams().set('classId', classId.toString());
  
  if (query) {
    params = params.set('q', query);
  }
  if (role) {
    params = params.set('role', role);
  }
  
  return this.http.get<UserResponse[]>('/users/search', { params });
}

// Component
this.userService.searchUsersInClass(1, 'Nguyen', 'STUDENT').subscribe(users => {
  this.searchResults = users;
});
```

**Request Examples:**
```
GET /users/search?classId=1
GET /users/search?classId=1&q=Nguyen
GET /users/search?classId=1&role=STUDENT
GET /users/search?classId=1&q=Nguyen&role=STUDENT
```

---

## 🎨 UI/UX Implementation Examples

### Example 1: Chat Sidebar với Class Selector

```typescript
export class ChatSidebarComponent {
  classes: ClassRoom[] = [];
  contacts: ChatContactDTO[] = [];
  selectedClassId: number | null = null;

  ngOnInit() {
    // Load user's classes
    this.classService.getMyClasses().subscribe(classes => {
      this.classes = classes;
    });
    
    // Load all contacts initially
    this.loadContacts();
  }

  onClassSelected(classId: number | null) {
    this.selectedClassId = classId;
    this.loadContacts();
  }

  loadContacts() {
    if (this.selectedClassId) {
      // Load contacts for specific class
      this.chatService.getContactsInClass(this.selectedClassId)
        .subscribe(contacts => {
          this.contacts = contacts;
        });
    } else {
      // Load all contacts
      this.chatService.getContacts()
        .subscribe(contacts => {
          this.contacts = contacts;
        });
    }
  }
}
```

**Template:**
```html
<div class="chat-sidebar">
  <!-- Class Selector -->
  <select [(ngModel)]="selectedClassId" (change)="onClassSelected($event)">
    <option [value]="null">All Classes</option>
    <option *ngFor="let class of classes" [value]="class.id">
      {{ class.name }}
    </option>
  </select>

  <!-- Contact List -->
  <div class="contact-list">
    <div *ngFor="let contact of contacts" 
         class="contact-item"
         (click)="openChat(contact.id)">
      <span class="name">{{ contact.fullName }}</span>
      <span class="role">{{ contact.role }}</span>
    </div>
  </div>
</div>
```

---

### Example 2: Search Box với Auto-complete

```typescript
export class ChatSearchComponent {
  searchControl = new FormControl('');
  searchResults$: Observable<UserResponse[]>;
  selectedClassId: number | null = null;

  ngOnInit() {
    this.searchResults$ = this.searchControl.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(query => {
        if (!query || query.length < 2) {
          return of([]);
        }
        
        if (this.selectedClassId) {
          // Search in specific class
          return this.userService.searchUsersInClass(
            this.selectedClassId, 
            query
          );
        } else {
          // Search globally
          return this.userService.searchUsers(query);
        }
      })
    );
  }
}
```

**Template:**
```html
<div class="search-box">
  <input 
    type="text" 
    [formControl]="searchControl"
    placeholder="Search users..."
  />
  
  <div class="search-results">
    <div *ngFor="let user of searchResults$ | async" 
         class="result-item"
         (click)="selectUser(user)">
      <span class="name">{{ user.fullName }}</span>
      <span class="role">{{ user.role }}</span>
    </div>
  </div>
</div>
```

---

### Example 3: Tab-Based (All vs Class)

```typescript
export class ChatComponent {
  activeTab: 'all' | 'class' = 'all';
  selectedClassId: number = 1;
  contacts: ChatContactDTO[] = [];

  onTabChange(tab: 'all' | 'class') {
    this.activeTab = tab;
    this.loadContacts();
  }

  loadContacts() {
    if (this.activeTab === 'all') {
      this.chatService.getContacts().subscribe(contacts => {
        this.contacts = contacts;
      });
    } else {
      this.chatService.getContactsInClass(this.selectedClassId)
        .subscribe(contacts => {
          this.contacts = contacts;
        });
    }
  }
}
```

**Template:**
```html
<div class="tabs">
  <button 
    [class.active]="activeTab === 'all'"
    (click)="onTabChange('all')">
    All Contacts
  </button>
  <button 
    [class.active]="activeTab === 'class'"
    (click)="onTabChange('class')">
    Class {{ selectedClassId }}
  </button>
</div>

<div class="contacts">
  <div *ngFor="let contact of contacts">
    {{ contact.fullName }}
  </div>
</div>
```

---

## 🔑 Authorization Header

**Tất cả requests phải có JWT token:**

```typescript
// Interceptor (Recommended)
@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  intercept(req: HttpRequest<any>, next: HttpHandler) {
    const token = localStorage.getItem('token');
    
    if (token) {
      const cloned = req.clone({
        headers: req.headers.set('Authorization', `Bearer ${token}`)
      });
      return next.handle(cloned);
    }
    
    return next.handle(req);
  }
}
```

Hoặc thêm manual:
```typescript
getContacts(): Observable<ChatContactDTO[]> {
  const token = localStorage.getItem('token');
  const headers = new HttpHeaders({
    'Authorization': `Bearer ${token}`
  });
  
  return this.http.get<ChatContactDTO[]>('/chat/contacts', { headers });
}
```

---

## ⚠️ Error Handling

```typescript
loadContacts(classId?: number) {
  const request = classId 
    ? this.chatService.getContactsInClass(classId)
    : this.chatService.getContacts();
    
  request.subscribe({
    next: (contacts) => {
      this.contacts = contacts;
    },
    error: (err) => {
      if (err.status === 401) {
        // Not authenticated - redirect to login
        this.router.navigate(['/login']);
      } else if (err.status === 403) {
        // Not in this class
        this.toastr.error('You are not a member of this class');
      } else {
        this.toastr.error('Failed to load contacts');
      }
    }
  });
}
```

---

## 📊 Response Models (TypeScript)

```typescript
// ChatContactDTO
export interface ChatContactDTO {
  id: number;
  fullName: string;
  role: 'STUDENT' | 'TEACHER' | 'ADMIN';
}

// UserResponse
export interface UserResponse {
  id: number;
  username: string;
  fullName: string;
  role: 'STUDENT' | 'TEACHER' | 'ADMIN';
  status: 'ACTIVE' | 'INACTIVE';
  root?: boolean;
  student?: any;
  teacher?: any;
}
```

---

## 🎯 Which API to Use?

| Use Case | Recommended API | Params |
|----------|----------------|---------|
| Lấy danh sách contacts | `GET /chat/contacts` | None |
| Lấy contacts trong 1 lớp | `GET /chat/contacts` | `?classId=1` |
| Search theo tên | `GET /users/search` | `?q=Nguyen` |
| Search students trong lớp | `GET /users/search` | `?classId=1&role=STUDENT` |
| Search với filter phức tạp | `GET /users/search` | Multiple params |

---

## ✅ Quick Checklist

- [ ] Import HttpClient trong module
- [ ] Tạo ChatService với getContacts() và getContactsInClass()
- [ ] Tạo UserService với searchUsers()
- [ ] Add Authorization interceptor
- [ ] Handle 401/403 errors
- [ ] Add debounceTime cho search input
- [ ] Test với cả global và class-scoped search

---

## 🚀 Complete Service Example

```typescript
// chat.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class ChatService {
  private baseUrl = 'http://localhost:8080';

  constructor(private http: HttpClient) {}

  // Get all contacts
  getContacts(): Observable<ChatContactDTO[]> {
    return this.http.get<ChatContactDTO[]>(`${this.baseUrl}/chat/contacts`);
  }

  // Get contacts in specific class
  getContactsInClass(classId: number): Observable<ChatContactDTO[]> {
    const params = new HttpParams().set('classId', classId.toString());
    return this.http.get<ChatContactDTO[]>(`${this.baseUrl}/chat/contacts`, { params });
  }

  // Search users
  searchUsers(query?: string, role?: string, classId?: number): Observable<UserResponse[]> {
    let params = new HttpParams();
    
    if (query) params = params.set('q', query);
    if (role) params = params.set('role', role);
    if (classId) params = params.set('classId', classId.toString());
    
    return this.http.get<UserResponse[]>(`${this.baseUrl}/users/search`, { params });
  }
}
```

---

**Last Updated:** 13/12/2025  
**Status:** ✅ Ready for Frontend Integration

