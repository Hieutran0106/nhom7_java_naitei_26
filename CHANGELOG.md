# Changelog

File ghi lại những thay đổi của dự án.
Định dạng dựa theo [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

---

## [Unreleased]

### 2026-08-18 - Setup & Entity Layer

**Người thực hiện:** [Trịnh Yến Nhi]

#### Added

- Kết nối database Supabase (PostgreSQL) qua HikariCP
- Tạo 8 JPA Entity theo ERD thiết kế:
  - `Role`
  - `Amenity`
  - `User`
  - `Venue`
  - `Space`
  - `Booking`
  - `Payment`
  - `Message`
- Cấu hình i18n với ngôn ngữ mặc định tiếng Việt (`messages_vi.properties`)
- Cấu hình Swagger UI tại `/swagger-ui.html`

#### Changed

- Tên bảng `user` → `users` (tránh reserved keyword trong PostgreSQL)
- `ddl-auto: update` để Hibernate tự tạo/cập nhật bảng

#### Notes

- Bảng junction được JPA tự tạo: `user_roles`, `venue_amenities`, `space_host`
- Các thay đổi so với ERD gốc: xem [Entity Design Decisions](#entity-design-decisions)

---

## Entity Design Decisions

| #   | Chỗ thay đổi           | ERD gốc         | Code thực tế                  | Lý do                                       |
| --- | ---------------------- | --------------- | ----------------------------- | ------------------------------------------- |
| 1   | Bảng User              | `user`          | `users`                       | `user` là reserved keyword trong PostgreSQL |
| 2   | latitude / longitude   | `decimal(10,8)` | `BigDecimal`                  | Tránh sai số float                          |
| 3   | description            | `text`          | `columnDefinition = "TEXT"`   | JPA mặc định dùng VARCHAR(255)              |
| 4   | capacity               | `int`           | `Integer`                     | Wrapper class hỗ trợ giá trị null           |
| 5   | open_time / close_time | `time`          | `LocalTime`                   | Java type mapping cho PostgreSQL time       |
| 6   | Các timestamp          | `timestamp`     | `LocalDateTime`               | Java type mapping cho PostgreSQL timestamp  |
| 7   | payment.booking_id     | FK              | `@OneToOne` + `unique = true` | Đảm bảo ràng buộc 1-1 ở tầng DB             |

---

_Template cho các lần cập nhật tiếp theo:_

```
## [Unreleased]

### YYYY-MM-DD - [Tên tính năng]

**Người thực hiện:** [Tên thành viên]

#### Added
- ...

#### Changed
- ...

#### Fixed
- ...

#### Removed
- ...
```
