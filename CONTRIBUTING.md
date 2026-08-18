# Contributing Guide – Co-working Space Booking System

Tài liệu này hướng dẫn cách thiết lập môi trường, viết commit đúng convention và kiểm tra chất lượng code trước khi tạo Pull Request.

---

## Tech Stack

| Layer           | Technology                             |
| --------------- | -------------------------------------- |
| Language        | Java 21+                               |
| Framework       | Spring Boot 3.3.4                      |
| Database        | PostgreSQL                             |
| UI (Admin/Mod)  | Thymeleaf                              |
| API docs (User) | Swagger / OpenAPI (`/swagger-ui.html`) |
| Auth            | Spring Security + JWT                  |
| Build           | Maven                                  |

---

## Thiết lập môi trường lần đầu

> **Yêu cầu cài sẵn trên máy:** Java 21+ (21 hoặc cao hơn), Maven 3.9+, Node.js 18+

### Bước 1 – Fork và clone repo

> Phải fork repo trước, sau đó clone fork về máy.

**1.1 – Fork repo trên GitHub**

Truy cập [awesome-academy/nhom7_java_naitei_26](https://github.com/awesome-academy/nhom7_java_naitei_26), nhấn nút **Fork** ở góc trên bên phải để tạo bản copy trên tài khoản.

**1.2 – Clone fork về máy**

```bash
# Thay YOUR_USERNAME bằng username GitHub
git clone https://github.com/YOUR_USERNAME/nhom7_java_naitei_26.git
cd nhom7_java_naitei_26
```

**1.3 – Thêm upstream để đồng bộ code mới từ repo gốc**

```bash
git remote add upstream https://github.com/awesome-academy/nhom7_java_naitei_26.git

# Kiểm tra
git remote -v
# origin https://github.com/YOUR_USERNAME/nhom7_java_naitei_26.git (fetch/push)
# upstream https://github.com/awesome-academy/nhom7_java_naitei_26.git (fetch/push)
```

**1.4 – Cập nhật code mới từ repo gốc (Thường xuyên)**

```bash
git fetch upstream
git checkout main
git merge upstream/main
```

### Bước 2 – Cài Git hooks BẮT BUỘC

```bash
npm install
```

> Lệnh này chỉ cần chạy **một lần duy nhất** sau khi clone.
> Nó tự động cài **Husky** + **Commitlint** và kích hoạt Git hook kiểm tra commit message.
> Nếu bỏ qua, commit **không được validate** và có thể bị reject khi push.

### Bước 3 – Lấy password Supabase

Dự án dùng **PostgreSQL trên Supabase**.
URL và username đã được đặt sẵn trong `application.yml`.

Liên hệ Khánh/Hiếu để được cấp **Supabase database password**.

### Bước 4 – Tạo file cấu hình local

File `application-local.yml` chứa các secret riêng của từng máy và **đã được gitignore** – không bao giờ commit lên Git.

**4.1 – Copy file mẫu:**

```bash
cp src/main/resources/application-local.example.yml src/main/resources/application-local.yml
```

**4.2 – Mở file vừa tạo và điền giá trị thật:**

```yaml
spring:
  datasource:
    password: YOUR_SUPABASE_PASSWORD # ← Thay bằng password lấy từ Bước 3

app:
  jwt:
    secret: YOUR_JWT_SECRET
```

**4.3 – Tạo JWT secret (mỗi người tạo riêng, không cần giống nhau):**

```powershell
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }))
```

Copy kết quả và dán vào `secret:` ở trên.

### Bước 5 – Set profile local (1 lần duy nhất)

Chạy lệnh này trong PowerShell **một lần duy nhất**, sau đó không cần chạy lại:

```powershell
[System.Environment]::SetEnvironmentVariable("SPRING_PROFILES_ACTIVE", "local", "User")
```

> Restart terminal sau khi chạy để có hiệu lực.
>
> Lệnh này báo cho Spring Boot biết phải load thêm `application-local.yml` khi khởi động.

### Bước 6 – Chạy ứng dụng

```bash
mvn spring-boot:run
```

Khởi động thành công khi thấy:

```
HikariPool-1 - Start completed.
Started CoworkingSpaceApplication in X.XXX seconds
```

### Bước 7 – Kiểm tra

| URL                                     | Mô tả             |
| --------------------------------------- | ----------------- |
| `http://localhost:8081`                 | Trang chủ         |
| `http://localhost:8081/swagger-ui.html` | API docs cho User |

---

## Commit Message Convention

Dự án sử dụng **Conventional Commits** – được enforce tự động qua Git hook sau khi `npm install`.

**Format:** `<type>(<scope>): <description>`

| Type       | Mục đích                         | Ví dụ                                      |
| ---------- | -------------------------------- | ------------------------------------------ |
| `feat`     | Tính năng mới                    | `feat(auth): add JWT login`                |
| `fix`      | Sửa lỗi                          | `fix(booking): fix date validation`        |
| `docs`     | Tài liệu                         | `docs: update contributing guide`          |
| `style`    | Formatting, không thay đổi logic | `style: fix indentation in UserService`    |
| `refactor` | Refactor, không thêm/bỏ feature  | `refactor(user): extract email validation` |
| `test`     | Thêm hoặc sửa test               | `test: add unit test for BookingService`   |
| `chore`    | Config, dependencies             | `chore: upgrade spring boot to 3.3.4`      |
| `perf`     | Cải thiện hiệu suất              | `perf: add db index on booking_date`       |
| `ci`       | CI/CD                            | `ci: add GitHub Actions workflow`          |
| `revert`   | Revert commit trước              | `revert: revert feat(auth)`                |
| `build`    | Build system                     | `build: configure checkstyle plugin`       |

**Scope** là optional nhưng khuyến khích dùng để dễ trace:
`auth`, `user`, `booking`, `venue`, `payment`, `admin`, `moderator`

**Ví dụ commit hợp lệ:**

```bash
git commit -m "feat(auth): add JWT login endpoint"
git commit -m "fix(booking): resolve date range validation error"
git commit -m "docs: update contributing guide"
git commit -m "test(service): add unit test for BookingService"
```

> Commit **sai format sẽ bị reject tự động** bởi Git hook.
> Nếu muốn bypass trong trường hợp khẩn cấp (không khuyến khích):
>
> ```bash
> git commit -m "..." --no-verify
> ```

---

## Code Quality – Sunlint

Dự án tuân thủ [Sun\* Coding Standards](https://coding-standards.sun-asterisk.vn/).

### Lưu ý về Sunlint với Java (Tùy chọn)

> Với Java, Sunlint chỉ tạo ra rules cho AI coding assistant (trong `.agent/skills/`) để AI tự áp dụng khi viết code.

### Cài Sunlint CLI (Một lần, global) (Tùy chọn)

```bash
npm install -g @sun-asterisk/sunlint
```

### Khởi tạo rules cho AI (Lần đầu) (Tùy chọn)

```bash
sunlint init
```

Chọn ngôn ngữ **Java** khi được hỏi. Lệnh này sẽ tạo ra file rules trong `.agent/skills/sunlint-code-quality/` để AI assistant tuân theo khi viết code Java.

### Check code trước khi tạo Pull Request

**Tự review code** theo checklist:

- [ ] Không có SQL/NoSQL concatenation (dùng parameterized query)
- [ ] Không hardcode secret, password trong code
- [ ] Mọi `catch` block phải có log lỗi với context cụ thể
- [ ] Không có dead code, unused imports
- [ ] Tên method theo dạng `Verb-Noun` (ví dụ: `createUser`, `findBooking`)
- [ ] Validate input đầu vào ở tầng service

---

## Quy ước đặt tên nhánh

Theo quy định trong `REDMINE.md`:

```
feature/<ticket-id>-<short-description>
fix/<ticket-id>-<short-description>
docs/<ticket-id>-<short-description>
```

**Ví dụ:**

```
feature/1234-create-user-model
fix/1235-booking-date-validation
docs/1236-update-api-docs
```

---

## Cấu trúc project

```
src/main/java/com/nhom7/coworkingspace/
├── config/           # Spring Security, Swagger, CORS config
├── constant/         # Enums, hằng số dùng chung
├── controller/
│   ├── api/          # REST API controllers (User – Swagger)
│   └── web/          # Thymeleaf controllers (Admin/Moderator)
├── dto/
│   ├── request/      # Request DTOs (validate input)
│   └── response/     # Response DTOs (trả về client)
├── entity/           # JPA Entities (map với DB tables)
├── exception/        # Custom exceptions + GlobalExceptionHandler
├── mapper/           # MapStruct mappers (Entity ↔ DTO)
├── repository/       # Spring Data JPA Repositories
├── security/         # JWT provider, UserDetails, Auth filters
├── service/
│   └── impl/         # Service interfaces + implementations
└── util/             # Utility/Helper classes

src/main/resources/
├── templates/
│   ├── admin/        # Admin pages (Thymeleaf)
│   ├── moderator/    # Moderator pages (Thymeleaf)
│   ├── fragments/    # Reusable fragments (navbar, footer...)
│   └── error/        # Error pages (403, 404, 500)
├── static/css|js|images/
├── i18n/             # messages.properties (EN + VI)
└── application.yml
```

---

## Cập nhật CHANGELOG.md

Mỗi khi hoàn thành một tính năng hoặc thay đổi đáng kể, **bắt buộc** cập nhật [`CHANGELOG.md`](./CHANGELOG.md) trước khi tạo Pull Request.

### Cách thêm entry mới:

Thêm vào **đầu phần `[Unreleased]`**, theo đúng template sau:

```markdown
### YYYY-MM-DD - [Tên tính năng ngắn gọn]

**Người thực hiện:** [Tên của bạn]

#### Added

- Mô tả những gì đã thêm mới

#### Changed

- Mô tả những gì đã thay đổi

#### Fixed

- Mô tả những bug đã sửa

#### Removed

- Mô tả những gì đã xóa
```

> Chỉ ghi những mục có thay đổi, bỏ qua mục không có nội dung.

### Ví dụ thực tế:

```markdown
### 2026-08-19 - Auth Module

**Người thực hiện:** Nguyễn Văn A

#### Added

- Tạo `AuthService` với chức năng đăng nhập, đăng xuất
- Tạo `JwtProvider` để generate và validate JWT token
- Endpoint `POST /api/auth/login`

#### Changed

- Cập nhật `SecurityConfig` để cho phép truy cập `/api/auth/**`
```

### Lưu ý:

- Commit `CHANGELOG.md` **cùng với code** trong cùng 1 commit
- Dùng type `docs` nếu chỉ sửa CHANGELOG: `docs: update CHANGELOG for auth module`
