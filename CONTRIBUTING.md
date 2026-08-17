# Contributing Guide – Co-working Space Booking System

Tài liệu này hướng dẫn cách thiết lập môi trường, viết commit đúng convention và kiểm tra chất lượng code trước khi tạo Pull Request.

---

## Tech Stack

| Layer           | Technology                             |
| --------------- | -------------------------------------- |
| Language        | Java 21                                |
| Framework       | Spring Boot 3.3.4                      |
| Database        | PostgreSQL                             |
| UI (Admin/Mod)  | Thymeleaf                              |
| API docs (User) | Swagger / OpenAPI (`/swagger-ui.html`) |
| Auth            | Spring Security + JWT                  |
| Build           | Maven                                  |

---

## Thiết lập môi trường lần đầu

> **Yêu cầu cài sẵn trên máy:** Java 21, Maven 3.9+, Node.js 18+

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

Dự án dùng **PostgreSQL trên Supabase**
URL và username đã được đặt sẵn trong `application.yml`, **không cần sửa gì**.

Liên hệ Khánh/Hiếu để được cấp **Supabase database password**.

### Bước 4 – Set biến môi trường password

Chỉ cần set **biến duy nhất** `DB_PASSWORD` trên máy local (không commit mật khẩu lên Git):

**Windows (PowerShell – chỉ tồn tại trong phiên làm việc hiện tại):**

```powershell
$env:DB_PASSWORD = "your_supabase_password"
```

**Hoặc** đặt vĩnh viễn qua System Environment Variables của Windows để không phải set lại mỗi lần:

```
Win + S → "Edit environment variables" → New → DB_PASSWORD = your_supabase_password
```

> Password lấy từ **Bước 3**

### Bước 5 – Chạy ứng dụng

```bash
mvn spring-boot:run
```

### Bước 6 – Kiểm tra

| URL                                     | Mô tả             |
| --------------------------------------- | ----------------- |
| `http://localhost:8080`                 | Trang chủ         |
| `http://localhost:8080/swagger-ui.html` | API docs cho User |

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

### Cài Sunlint CLI (một lần, global)

```bash
npm install -g @sun-asterisk/sunlint-cli
```

### Khởi tạo config (lần đầu trong project)

```bash
sunlint init
```

Chọn ngôn ngữ **Java** khi được hỏi.

### Check code trước khi tạo Pull Request

```bash
sunlint check ./src/main/java
```

Output sẽ hiển thị:

- `[ERROR]` – **Bắt buộc fix 100%** trước khi tạo PR
- `[WARNING]` – Khuyến khích sửa

### Evidence cho Pull Request

Sau khi fix xong, chạy lại `sunlint check` và **chụp màn hình** kết quả `0 errors` đính kèm vào **comment trên Pull Request**.

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
