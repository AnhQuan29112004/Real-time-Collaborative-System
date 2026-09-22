---
name: collabnote-event-driven
description: Event-driven architecture cho CollabNote — Kafka patterns (partition key, idempotency, consumer group), Saga (Orchestration vs Choreography, compensating actions), CQRS + CDC (Debezium), Domain Events (@TransactionalEventListener, Outbox pattern), Activity Feed vs Audit Log. Dùng khi thiết kế async flow, tách service communication, hoặc implement event-based features.
---

# CollabNote — Event-driven Architecture

## 1. Domain Events — Nền tảng

### Nguyên tắc cốt lõi

**KHÔNG** để service A gọi trực tiếp service B cho side-effect. Publish event, listener tự xử lý:

```java
// ✅ ĐÚNG — loose coupling
applicationEventPublisher.publishEvent(new DocumentDeletedEvent(document.getId()));

// ❌ SAI — tight coupling
commentService.deleteByDocumentId(document.getId());
versionService.deleteByDocumentId(document.getId());
notificationService.notifyDocumentDeleted(document.getId());
```

### @TransactionalEventListener — BẮT BUỘC cho side-effect

```java
@Component
public class DocumentDeletedListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onDocumentDeleted(DocumentDeletedEvent event) {
        // Side-effect chỉ chạy SAU KHI transaction chính đã commit thành công
        // Nếu transaction rollback → listener KHÔNG chạy → tránh gửi notification sai
        notificationService.notifyCollaborators(event.documentId(), "Document đã bị xoá");
    }
}
```

**Tại sao KHÔNG dùng `@EventListener` thường?**
- `@EventListener` chạy **trong** transaction → side-effect (gửi email, push notification) chạy ngay cả khi transaction chính rollback sau đó
- Kết quả: user nhận email "document đã xoá" nhưng document vẫn còn

### Outbox Pattern — Khi cần publish event cross-service (qua Kafka)

Khi 2 service không chung DB transaction, không dùng được `@TransactionalEventListener`:

```
1. Service A: Trong cùng transaction, ghi vào bảng `outbox_events`
2. Poller/CDC: Đọc `outbox_events` → publish lên Kafka
3. Service B: Consume từ Kafka
```

Bảng `outbox_events`:
```sql
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(255),    -- 'document', 'workspace'
    aggregate_id VARCHAR(255),      -- document_id
    event_type VARCHAR(255),        -- 'DOCUMENT_DELETED'
    payload JSONB,
    created_at TIMESTAMP,
    published BOOLEAN DEFAULT FALSE
);
```

**Tại sao không ghi DB + publish Kafka trong cùng 1 method?**
- Đó là **dual-write** — nếu DB commit OK nhưng Kafka publish fail → data không nhất quán
- Outbox đảm bảo: data ghi DB thành công → event CHẮC CHẮN được publish (eventually)

## 2. Kafka Patterns

### Partition Key Strategy

```java
// Partition key = document_id → tất cả event của 1 document đi vào cùng partition
// → đảm bảo thứ tự event trong cùng 1 document
kafkaTemplate.send("document-events", document.getId().toString(), event);
```

| Partition key | Ưu điểm | Nhược điểm |
|---|---|---|
| `document_id` | Giữ thứ tự event per document | Hot partition nếu 1 doc có quá nhiều event |
| `workspace_id` | Giữ thứ tự per workspace | Hot partition cho workspace lớn |
| Random | Phân bổ đều | Mất thứ tự |

**Chọn:** `document_id` cho hầu hết trường hợp CollabNote — thứ tự event per document quan trọng hơn phân bổ đều.

### Idempotency Key — Tránh xử lý trùng khi retry

```java
@KafkaListener(topics = "document-events")
public void consume(DocumentEvent event) {
    // Kiểm tra idempotency key trước khi xử lý
    if (processedEventRepository.existsByEventId(event.eventId())) {
        log.info("Event {} đã xử lý, skip", event.eventId());
        return;
    }

    // Xử lý event
    processEvent(event);

    // Đánh dấu đã xử lý
    processedEventRepository.save(new ProcessedEvent(event.eventId()));
}
```

**Tại sao cần?**
- Kafka đảm bảo **at-least-once** delivery — consumer có thể nhận cùng 1 message nhiều lần (vd khi rebalancing, crash giữa chừng)
- Không có idempotency key → gửi trùng notification, ghi trùng audit log

### Consumer Group & Rebalancing

```yaml
spring:
  kafka:
    consumer:
      group-id: notification-service    # Mỗi service = 1 consumer group
      auto-offset-reset: earliest       # Bắt đầu từ đầu nếu chưa có offset
      enable-auto-commit: false         # Manual commit sau khi xử lý xong
```

- Mỗi service cần consume cùng topic → dùng **consumer group khác nhau** (notification-group, search-group, audit-group)
- Cùng 1 consumer group, Kafka tự phân bổ partition cho các instance — khi thêm/bớt instance → **rebalancing** (tạm dừng consume)

## 3. Saga Pattern — Transaction xuyên service

### Use case chính: Xoá document

Xoá document cần dọn dẹp ở nhiều service:
1. Document Service: xoá document
2. Version Service: xoá tất cả version
3. Comment Service: xoá tất cả comment
4. Permission Service: xoá tất cả permission
5. Notification Service: thông báo collaborators

### Orchestration vs Choreography

| Aspect | Orchestration | Choreography |
|---|---|---|
| Điều phối | 1 service trung tâm (Saga Orchestrator) | Mỗi service tự lắng nghe event |
| Trace/debug | ✅ Dễ — xem log 1 nơi | ❌ Khó — event phân tán |
| Coupling | Service biết nhau qua orchestrator | Service không biết nhau (loose) |
| Single point of failure | ✅ Orchestrator crash = stuck | ❌ Không có |
| Khi nào dùng | Luồng phức tạp, nhiều bước, cần rollback rõ | Luồng đơn giản, ít bước |

**Khuyến nghị cho CollabNote:** **Orchestration** cho xoá document (5 bước, cần rollback rõ ràng nếu 1 bước fail). **Choreography** cho notification (đơn giản, fire-and-forget).

### Compensating Actions — Rollback khi Saga fail

```
Bước 1: Document Service → mark document as DELETING (soft delete)
Bước 2: Version Service → delete versions
  → Nếu fail: Compensate bước 1 → unmark document (restore)
Bước 3: Comment Service → delete comments
  → Nếu fail: Compensate bước 2 → restore versions
              Compensate bước 1 → restore document
Bước 4: Permission Service → delete permissions
Bước 5: Notification → notify collaborators (fire-and-forget, không cần compensate)
Bước 6: Document Service → hard delete (chỉ khi tất cả bước trước OK)
```

**Lưu ý:**
- Soft delete ở bước 1 → user không thấy document nữa nhưng data vẫn còn nếu cần rollback
- Bước Notification không cần compensate (worst case: user nhận notification nhưng document vẫn còn → chấp nhận được)
- Mỗi compensating action phải **idempotent** (gọi 2 lần cho kết quả giống nhau)

## 4. CQRS + CDC (Change Data Capture)

### Tại sao CQRS?

Đọc (search, analytics, activity feed) và ghi (CRUD document) có nhu cầu khác nhau:
- **Write model**: Postgres, chuẩn hoá, ACID
- **Read model**: Elasticsearch (search), denormalize (activity feed), nhanh

### CDC với Debezium — KHÔNG dual-write

```
Postgres (WAL) → Debezium → Kafka → Search Service (index ES)
                                   → Audit Service (ghi audit_logs)
                                   → Analytics Service (aggregate)
```

**Tại sao CDC, không dual-write?**

| Aspect | CDC (Debezium) | Dual-write |
|---|---|---|
| Consistency | ✅ Guaranteed eventual | ❌ Có thể lệch (DB OK, Kafka fail) |
| Ảnh hưởng write path | ✅ Không — CDC đọc WAL async | ❌ Có — thêm latency cho Kafka publish |
| Search Service down | ✅ Không ảnh hưởng write | ❌ Có thể block hoặc mất data |
| Complexity | Cần setup Debezium | Đơn giản hơn ban đầu |

### Activity Feed vs Audit Log

| Aspect | Audit Log | Activity Feed |
|---|---|---|
| Mục đích | Compliance, debug, traceability | UX — end-user xem trên UI |
| Audience | Dev, admin, compliance team | End-user |
| Format | Technical (JSON, entity/action/actor) | Human-readable ("Alice đã sửa Document X, 5 phút trước") |
| Nguồn | CDC tự động, không cần code thủ công | Build từ audit_logs, transform cho UX |
| Filter | Theo entity, action, time range | Theo document, workspace, user |

## 5. Event Semantics — At-least-once là đủ

### Khi nào cần exactly-once?

| Scenario | Cần exactly-once? | Lý do |
|---|---|---|
| Charge tiền | ✅ Có | Trùng = mất tiền |
| Gửi notification | ❌ Không | Trùng = user nhận 2 notification → annoying nhưng không sai data |
| Ghi audit log | ❌ Không | Trùng = thêm 1 dòng log → có thể dedup sau |
| Index search | ❌ Không | Trùng = index lại cùng data → idempotent tự nhiên |

**Kết luận cho CollabNote:** **At-least-once + idempotency key** đủ cho tất cả use case. KHÔNG cần cố làm exactly-once (phức tạp, tốn performance, Kafka Transactions có overhead lớn).

## 6. Pattern Decision Checklist

Khi thiết kế async flow mới, trả lời các câu hỏi:

1. **Cùng service hay khác service?**
   - Cùng → `@TransactionalEventListener` + `@Async`
   - Khác → Kafka (hoặc Outbox + Kafka)

2. **Cần đảm bảo thứ tự?**
   - Có → Chọn partition key hợp lý (vd `document_id`)
   - Không → Random partition (phân bổ đều hơn)

3. **Consumer có thể nhận trùng?**
   - Luôn giả định CÓ → implement idempotency key

4. **Fail giữa chừng thì sao?**
   - Side-effect đơn giản → Retry là đủ
   - Nhiều bước, cần rollback → Saga pattern

5. **Cần response ngay?**
   - Có → REST/gRPC (đồng bộ)
   - Không → Kafka (bất đồng bộ)
