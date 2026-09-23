# Module `user`

Đã triển khai hồ sơ người dùng, email chuẩn hóa, trạng thái xác minh/vô hiệu hóa, audit metadata và optimistic version. Credential và phiên thuộc module auth.

| Method | Endpoint | Nội dung |
| --- | --- | --- |
| GET | `/api/v1/users/me` | Hồ sơ của người dùng trong Bearer JWT |
| PATCH | `/api/v1/users/me` | Body `displayName` (1–100 ký tự), `version` từ lần GET gần nhất |

PATCH yêu cầu Bearer JWT và CSRF header; version cũ trả conflict. API chỉ cập nhật tên của chính người dùng, không nhận user ID để thay chủ sở hữu. Response không chứa password hash hoặc token.

`UserService` là contract cho auth truy cập user; repository thuộc nội bộ module. Chưa triển khai đổi email, xóa tài khoản hoặc danh sách user quản trị vì cần xử lý ownership/workspace tương ứng.

Xem [Auth và cách tích hợp frontend](../auth/README.md).
