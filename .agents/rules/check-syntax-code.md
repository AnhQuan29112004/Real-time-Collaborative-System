---
trigger: always_on
---

## Quy tắc bắt buộc: Kiểm tra lỗi sau khi viết code

Sau MỖI lần tạo mới hoặc chỉnh sửa code, agent PHẢI thực hiện các bước sau
trước khi coi task là hoàn thành:

### 1. Kiểm tra cú pháp (Syntax Check)
- Đảm bảo không thiếu dấu đóng ngoặc `{}`, `()`, `[]`
- Đảm bảo không thiếu thẻ đóng (closing tag) trong JSX/HTML/XML
- Đảm bảo không thiếu dấu chấm phẩy, dấu phẩy ở nơi ngôn ngữ yêu cầu
- Đảm bảo không có code bị cắt dở (do giới hạn output, do lỗi khi generate)

### 2. Kiểm tra kiểu dữ liệu (Type Check) — với TypeScript/typed languages
- Không được thiếu field bắt buộc trong interface/type
- Không dùng `any` để né lỗi type trừ khi thực sự cần thiết và phải giải thích lý do
- Import đầy đủ types/interfaces được dùng
- Đảm bảo function signature (tham số, kiểu trả về) khớp với cách gọi thực tế

### 3. Chạy công cụ kiểm tra thực tế (không chỉ đọc lại bằng mắt)
Ưu tiên theo thứ tự, tùy ngôn ngữ/stack đang dùng:
- TypeScript: chạy `tsc --noEmit` để build-check
- JS/TS: chạy linter (`eslint .`)
- Python: chạy `python -m py_compile <file>` hoặc `ruff check` / `mypy`
- Các ngôn ngữ khác: dùng compiler/interpreter tương ứng để build hoặc dry-run

→ Nếu không có sẵn tool nào để chạy, agent phải tự đọc lại toàn bộ code đã
viết/sửa (không chỉ đoạn vừa thay đổi) như một reviewer, kiểm tra kỹ từng
dấu ngoặc, từng import, từng kiểu dữ liệu.

### 4. Xử lý khi phát hiện lỗi
- Nếu phát hiện lỗi → SỬA NGAY, không báo cáo "xong" khi còn lỗi
- Lặp lại bước 1-3 cho đến khi không còn lỗi nào được phát hiện
- Không được bỏ qua lỗi với lý do "không quan trọng" trừ khi người dùng
  xác nhận rõ ràng là chấp nhận được

### 5. Chỉ báo cáo hoàn thành khi:
- File không còn báo đỏ (no syntax error)
- Type-check pass (nếu là ngôn ngữ có kiểu tĩnh)
- Code có thể chạy/build được, không bị crash ngay ở bước khởi tạo

### 6. Minh bạch với người dùng
- Nếu không thể tự chạy được kiểm tra (do môi trường không cho phép),
  phải nói rõ với người dùng: "Tôi đã review lại bằng mắt nhưng chưa chạy
  được compiler/linter thực tế, bạn nên chạy thử trước khi dùng."
- Không được khẳng định "code chạy hoàn hảo" nếu chưa thực sự verify được.