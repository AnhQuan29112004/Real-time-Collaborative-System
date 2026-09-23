# CollabNote — Real-time Collaborative System

Sản phẩm cộng tác tài liệu theo hướng modular monolith, phát triển từng mốc theo roadmap.

- [Roadmap sản phẩm](collabnote-roadmap.md)
- [Cấu trúc source và cách dùng foundation](Real-time-Collaborative-System/docs/source-foundation.md)
- [Backend Maven project](Real-time-Collaborative-System/pom.xml)

Hiện có shared infrastructure và implementation backend Auth/User: đăng ký, xác minh email, JWT access/refresh, quản lý phiên, Google login và hồ sơ cá nhân. Các module khác vẫn là khung phát triển; frontend chưa triển khai.

- [API Auth và cấu hình SMTP/Google](Real-time-Collaborative-System/src/main/java/com/collabnote/auth/README.md)
- [API User](Real-time-Collaborative-System/src/main/java/com/collabnote/user/README.md)

```bash
cd Real-time-Collaborative-System
./mvnw -Dmaven.test.skip=true package
./mvnw spring-boot:run
```

Trước khi chạy, cấu hình `.env` từ `.env.example` và PostgreSQL. JWT secret bắt buộc; đăng ký cần SMTP được bật. Flyway chạy migration Auth/User khi khởi động. Chỉ endpoint Auth/User và health được mở theo security policy.

Build bỏ qua test đã thành công; chưa kiểm thử chức năng Auth/User với dịch vụ thực. Test foundation hiện có thuộc giai đoạn trước Auth/User, chưa được cập nhật trong thay đổi này theo quy định repository.
