# Database migrations

Chưa có bảng nghiệp vụ. Tạo migration đầu tiên khi triển khai schema Phase 1,
ví dụ `V1__create_identity_and_workspace.sql`. Không tạo bảng giả để scaffold chạy.
Flyway được bật; `ddl-auto=validate`, không bật update/create trong dev hoặc prod.
Không sửa migration đã chạy. Thiết kế tenant constraints, indexes và ownership trước DDL.
H2 chỉ có trong test; kiểm chứng migration/query PostgreSQL bằng integration test khi có schema thật.
