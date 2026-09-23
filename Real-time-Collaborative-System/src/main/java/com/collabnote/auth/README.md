# Module `auth`

Đã triển khai credential email/password, xác minh email, khôi phục mật khẩu, Google Identity Services và quản lý phiên. Controller gọi service contract; auth chỉ truy cập user qua `UserService`.

## API

Các đường dẫn dưới đây có prefix `/api/v1/auth`. Response theo `ApiResponse`; access token nằm trong `data.accessToken`.

| Method | Path | Nội dung |
| --- | --- | --- |
| GET | `/csrf` | Lấy `data.headerName` và `data.token` để gửi CSRF header |
| POST | `/register` | `email`, `password`, `displayName`; tạo tài khoản chưa xác minh |
| POST | `/login` | `email`, `password`; yêu cầu email đã xác minh |
| POST | `/refresh` | Dùng refresh cookie; cấp access token và rotate refresh token |
| POST | `/logout` | Thu hồi phiên theo refresh cookie và xóa cookie |
| POST | `/email/request-verification` | `email`; yêu cầu gửi lại thư |
| POST | `/email/verify` | `token`; token dùng một lần |
| POST | `/password/forgot` | `email`; phản hồi chung cho email có/không tồn tại |
| POST | `/password/reset` | `token`, `newPassword`; thu hồi mọi phiên |
| PUT | `/password` | Bearer JWT; `currentPassword`, `newPassword`; thu hồi mọi phiên |
| POST | `/google` | `idToken` do Google Identity Services cấp |
| POST | `/google/link` | Bearer JWT và `idToken`; liên kết Google cùng email đã xác minh |
| GET | `/sessions` | Bearer JWT; danh sách phiên đang hoạt động |
| DELETE | `/sessions/{id}` | Bearer JWT; thu hồi một phiên của chính mình |
| DELETE | `/sessions` | Bearer JWT; thu hồi mọi phiên |

## Tích hợp frontend Next.js

1. Gọi GET `/csrf` với `credentials: "include"`, giữ token trả về trong bộ nhớ.
2. Mọi POST/PUT/PATCH/DELETE gửi header `X-CSRF-TOKEN` và `credentials: "include"`, kể cả đăng ký, đăng nhập, refresh, logout.
3. Sau login/refresh, giữ access token trong bộ nhớ và gửi `Authorization: Bearer ...` khi gọi API được bảo vệ. Refresh token chỉ nằm trong cookie HttpOnly `COLLABNOTE_REFRESH`.
4. Sau login/refresh/logout/reset/change-password, gọi lại GET `/csrf` vì cookie CSRF được xóa khi thay đổi xác thực. Không đính kèm access token hết hạn vào GET `/csrf`.
5. Chỉ cho một refresh chạy tại một thời điểm, phối hợp cả nhiều tab. Retry bằng refresh token đã dùng sẽ thu hồi cả phiên; mất response refresh có thể yêu cầu đăng nhập lại.
6. Trang `/verify-email` và `/reset-password` đọc `#token=...` từ URL, xóa fragment khỏi lịch sử rồi POST token tới backend. Các trang frontend này chưa được triển khai.

Cookie dùng SameSite=Lax. Cấu hình hiện tại dành cho frontend/backend cùng site (local khác port được); triển khai khác site cần thiết kế lại cookie/CSRF. CORS phải khai báo origin cụ thể.

## Cấu hình

Backend đọc `.env` ở working directory (xem `.env.example`).

- PostgreSQL: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`; Flyway tạo schema auth/user khi khởi động.
- `AUTH_JWT_SECRET`: Base64 của ít nhất 32 byte ngẫu nhiên; tạo bằng `openssl rand -base64 32`. Giữ cố định giữa các instance/restart, không commit.
- Access JWT mặc định 15 phút; phiên/refresh có hạn tuyệt đối 7 ngày, refresh không kéo dài phiên. Refresh token là chuỗi ngẫu nhiên, DB chỉ lưu SHA-256; access token là JWT HS256.
- Email: đặt `AUTH_MAIL_ENABLED=true`, `AUTH_MAIL_FROM` và cấu hình `SMTP_HOST`, `SMTP_PORT`, `SMTP_AUTH`, `SMTP_STARTTLS`, `SMTP_USERNAME`, `SMTP_PASSWORD` theo nhà cung cấp. Mặc định email tắt, đăng ký/forgot/resend trả 503. Không trả token xác minh/reset trong API hoặc log.
- `AUTH_VERIFICATION_URL`, `AUTH_RESET_URL`: URL trang frontend nhận fragment token.
- `GOOGLE_CLIENT_ID`: client ID web dùng ở frontend. Để trống thì Google login tắt. Backend kiểm tra chữ ký/issuer/audience/expiry/email_verified của Google ID token; đây là luồng Identity Services, không phải OAuth redirect với client secret.
- Production dùng HTTPS và `AUTH_COOKIE_SECURE=true`; dev mặc định false cho HTTP local.

Email gửi đồng bộ sau khi transaction tạo token commit. Lỗi SMTP được log không kèm email/token; phản hồi nhận yêu cầu vẫn thành công để không tiết lộ tài khoản qua lỗi gửi thư. Người dùng có thể resend sau cooldown 1 phút. Chưa có outbox/retry tự động; API thành công không xác nhận thư đã tới hộp thư.

## Quy tắc và giới hạn hiện tại

- Mật khẩu tối thiểu 12 ký tự, tối đa 72 byte UTF-8; lưu bằng BCrypt qua Spring PasswordEncoder.
- Mỗi request JWT kiểm tra phiên và trạng thái user trong DB: logout/revoke chặn request tiếp theo ngay, đổi lại có DB reads. Request đang xử lý không bị hủy ngược.
- Refresh, đổi mật khẩu và cấp phiên khóa user để tuần tự hóa; refresh token dùng lại làm phiên bị thu hồi. Tối đa mặc định 10 phiên hoạt động.
- Không tự liên kết Google với tài khoản đã tồn tại chỉ dựa vào email; cần đăng nhập tài khoản cũ rồi gọi `/google/link`.
- Rate limit 30 request auth/phút/IP, lưu bộ nhớ từng instance và dùng remote address. Khi đặt sau proxy hoặc scale nhiều instance cần cấu hình trusted proxy và limiter tập trung. Chưa có chống brute-force theo tài khoản.
- Chưa có tác vụ dọn phiên/token hết hạn; cần bổ sung retention trước vận hành lâu dài. Giữ token đã rotate tới khi phiên hết hạn để phát hiện replay.
- Chưa có MFA, quản trị tài khoản, đổi email hoặc xóa tài khoản. Không mở API các module khác.

Đã build bỏ qua test theo quy định repository. Chưa xác minh luồng chạy thực tế với PostgreSQL/SMTP/Google; người dùng kiểm thử thủ công.
