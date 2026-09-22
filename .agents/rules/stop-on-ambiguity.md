---
trigger: always_on
---

# Rule: Dừng thực thi khi gặp điểm không rõ ràng

## Mục đích

Ngăn agent tự suy diễn (hallucination) hoặc tự bịa logic khi không hiểu rõ
yêu cầu hoặc code, dẫn đến code sai chức năng mà người review khó phát hiện.

## Điều kiện kích hoạt

Áp dụng khi agent, trong lúc đọc prompt hoặc rà soát code, gặp bất kỳ
trường hợp nào sau đây:

- Không đọc được file / đoạn code (lỗi truy cập, file thiếu, path sai,
  encoding lỗi...)
- Đọc được code nhưng **không hiểu rõ mục đích hoặc logic** của đoạn đó
- Yêu cầu trong prompt **mơ hồ**, thiếu thông tin, hoặc có thể hiểu theo
  nhiều cách khác nhau
- Phát hiện **mâu thuẫn** giữa yêu cầu và code hiện tại, hoặc giữa các
  phần code với nhau
- Cần giả định (assumption) về business logic mà không có căn cứ rõ ràng
  trong code hoặc tài liệu đi kèm

## Hành động bắt buộc

1. **Dừng thực thi ngay lập tức** đối với phần liên quan đến điểm chưa
   rõ — không viết hoặc sửa code dựa trên suy đoán hay "đoán ý" người
   dùng.
2. **Tuyệt đối không tự bịa logic** để lấp khoảng trống hiểu biết. Không
   hallucinate tên hàm, field, API, hay hành vi không có trong code/tài
   liệu.
3. Liệt kê **rõ ràng, cụ thể** từng điểm chưa hiểu, gồm:
   - File / dòng / hàm cụ thể liên quan (nếu có)
   - Mô tả ngắn gọn lý do không hiểu (thiếu ngữ cảnh, logic mâu thuẫn,
     không đọc được, v.v.)
4. Đặt câu hỏi cụ thể, có thể trả lời được, để người dùng làm rõ trước
   khi tiếp tục.
5. Chỉ tiếp tục thực thi phần đó sau khi nhận được xác nhận/làm rõ từ
   người dùng.
6. Các phần khác của task mà agent đã hiểu rõ, không liên quan đến điểm
   mơ hồ, vẫn có thể tiếp tục thực hiện bình thường — không dừng toàn bộ
   task chỉ vì một điểm nhỏ chưa rõ.

## Format output khi trigger rule này

```
⚠️ CẦN LÀM RÕ TRƯỚC KHI TIẾP TỤC

1. [File: path/to/file.js, dòng 45-60]
   Vấn đề: Không rõ hàm `calculateX()` xử lý case null như thế nào —
   không thấy logic guard trong code, nhưng yêu cầu lại giả định input
   luôn có giá trị.
   Câu hỏi: Nếu input null thì hệ thống nên trả về gì?

2. [Yêu cầu prompt]
   Vấn đề: "Cập nhật trạng thái đơn hàng" — không rõ là cập nhật field
   nào (status, order_state, hay cả hai).
   Câu hỏi: Bạn muốn cập nhật field cụ thể nào?

→ Agent sẽ KHÔNG code phần liên quan cho đến khi có câu trả lời.
```

## Lưu ý khi cấu hình trong Antigravity

- Đặt file này tại `.agents/rules/stop-on-ambiguity.md` trong workspace
  (Antigravity mặc định đọc `.agents/rules/`; `.agent/rules/` vẫn được
  hỗ trợ ngược).
- Nên đặt **Activation Mode = Always On** vì đây là rule an toàn cốt
  lõi, cần áp dụng cho mọi task, không nên để Manual hoặc Model
  Decision (agent có thể tự quyết định bỏ qua).
- Có thể dùng `@stop-on-ambiguity` để mention thủ công trong prompt nếu
  cần nhấn mạnh lại giữa phiên làm việc.