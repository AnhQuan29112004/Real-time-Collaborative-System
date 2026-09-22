# CollabNote Backend Architecture — Mandatory Rule

> Apply khi chạm vào file `.java`, package `com.lifetex`, hoặc request liên quan backend/database/API của dự án CollabNote.

---

## 🔴 QUY TẮC BẮT BUỘC — MỌI AGENT PHẢI TUÂN THỦ

### 1. Package Structure Enforcement

Cấu trúc thư mục domain-first BẮT BUỘC. KHÔNG được tạo file/folder ngoài cấu trúc này:

```
src/main/java/com/<company>/<app>/
├── <domain>/                              ← Mỗi domain = 1 package riêng
│   ├── controller/                        ← REST endpoints
│   ├── service/
│   │   ├── <DomainService>.java           ← interface
│   │   └── impl/<DomainServiceImpl>.java  ← implementation
│   ├── repository/                        ← JPA repositories
│   ├── specification/                     ← JPA Specification cho dynamic query/filter
│   ├── dto/
│   │   ├── request/                       ← Input DTOs
│   │   └── response/                      ← Output DTOs
│   ├── mapper/                            ← MapStruct mappers
│   ├── entity/                            ← JPA entities
│   ├── event/                             ← Domain events (VD: DocumentCreatedEvent)
│   ├── listener/                          ← Event listeners
│   └── validator/                         ← Custom Bean Validation
├── common/                                ← Shared across domains
│   ├── exception/                         ← BaseException, ErrorCode, GlobalExceptionHandler
│   ├── response/                          ← ApiResponse<T>, PageResponse<T>
│   ├── config/                            ← App-wide config
│   ├── aspect/                            ← AOP: Logging, Audit, Performance
│   ├── security/                          ← Security config, filters
│   └── audit/                             ← BaseEntity (createdBy, createdDate...)
└── infrastructure/                        ← External integrations
    ├── messaging/                         ← Kafka/RabbitMQ producer, consumer
    ├── cache/                             ← Redis config, cache managers
    ├── storage/                           ← File/blob storage
    └── client/                            ← Feign/WebClient + Resilience4j wrappers
```

**CẤM:**
- ❌ Tạo package `utils/`, `helpers/`, `common/` **ở root ngang hàng domain** — đặt trong `common/` hoặc trong domain cụ thể (vd `warehouse/util/`)
- ❌ Đặt controller/service/repository **ngoài domain package** (vd KHÔNG tạo `/controller/UserController.java` ở root)
- ❌ Tạo folder không có trong cấu trúc trên mà không hỏi user trước

### 2. DDD Domain Classification — Xác định TRƯỚC khi code

Trước khi tạo module/service mới, **BẮT BUỘC** xác định domain type:

| Domain | Loại | Kiến trúc BẮT BUỘC | Mức đầu tư |
|---|---|---|---|
| Document / Collaboration / Editing / Version | **Core** | Hexagonal (Ports & Adapters) | Cao nhất — test kỹ, không đơn giản hoá |
| Auth / User / Workspace / Permission | **Supporting** | Layered (Controller→Service→Repository) | Vừa phải — đúng là được |
| Notification / Search | **Generic** | Simple/Minimal | Thấp — có thể thay bằng SaaS sau |

**Quy tắc:**
- Core domain → **BẮT BUỘC** Hexagonal: tách Port (interface ở domain layer) và Adapter (JPA, REST, Kafka ở infrastructure layer). Business logic KHÔNG được phụ thuộc framework.
- Supporting/Generic → Layered thông thường là đủ, KHÔNG cần Hexagonal.
- Khi tạo module mới → PHẢI nói rõ: "Module này thuộc domain [Core/Supporting/Generic], áp dụng kiến trúc [Hexagonal/Layered]."

### 3. Service Boundary Rules

- **KHÔNG** tách service theo tên bảng DB (vd "User Service = bảng users"). Tách theo **business capability**.
- Nếu 2 service phải **join chéo DB** thường xuyên → **sai boundary**, phải xem lại.
- **Monolith-first**: Bắt đầu monolith, module tách rõ boundary ngay trong monolith (package riêng, interface rõ) → sẵn sàng tách microservice khi cần.
- Khi tách microservice: giao tiếp REST/gRPC (đồng bộ) hoặc Kafka (bất đồng bộ) — KHÔNG truy cập thẳng DB của service khác.

### 4. Multi-layer Permission Rules

CollabNote có **2 layer permission độc lập**:

| Layer | Scope | Ví dụ |
|---|---|---|
| **Workspace Role** (Owner/Admin/Member) | Toàn workspace | Admin quản lý members, settings |
| **Document Permission** (View/Edit per user) | Từng document | Owner giới hạn Member chỉ View 1 doc |

**Quy tắc bắt buộc:**
- 2 layer PHẢI được implement **độc lập**.
- PHẢI có rule resolve conflict **rõ ràng** trước khi implement (vd: Admin bị chặn Edit 1 doc → cho phép hay không?). Viết rule này trong ADR.
- KHÔNG implement permission mà chưa define rule xung đột.
- Public link (view-only/edit) là authorization không cần authentication — token phải đủ entropy (UUID v4 hoặc random ≥32 byte), rate-limit riêng per token.

### 5. ADR Protocol (Architecture Decision Record)

Mỗi quyết định kiến trúc quan trọng → **BẮT BUỘC viết ADR** theo template:

```markdown
### ADR-XX: <tên quyết định>
**Context:** <vấn đề đang gặp>
**Decision:** <chọn phương án nào>
**Alternatives considered:** <phương án khác + vì sao loại>
**Trade-off accepted:** <cái gì mình đánh đổi khi chọn>
```

Lưu tại `docs/adr/ADR-XX.md`. Danh sách ADR cần viết theo roadmap:
- ADR-01: Monolith vs Microservice
- ADR-02: Hexagonal cho Collaboration, Layered cho Auth
- ADR-02b: Workspace role + Document permission conflict resolution
- ADR-03 → ADR-13: Xem chi tiết trong skill `collabnote-ddd-hexagonal`

### 6. Layer Responsibilities — HARD RULES

**Controller — CHỈ được:**
- Nhận request, validate format (`@Valid`), gọi Service, trả response
- KHÔNG chứa business logic, KHÔNG inject Repository trực tiếp

**Service — BẮT BUỘC tách Interface + Impl:**
- Interface ở `service/<DomainService>.java`
- Implementation ở `service/impl/<DomainServiceImpl>.java`
- `@Transactional` đặt ở Service method, KHÔNG đặt ở Repository
- Service > 300 dòng → tách theo sub-domain

**Repository — Data access thuần:**
- CHỈ query DB, KHÔNG business logic, KHÔNG xử lý exception
- Query nhiều điều kiện → dùng JPA `Specification`, KHÔNG nối JPQL bằng string
- Kiểm tra N+1 cho mọi query liên quan collection lazy

**DTO — Java Record (17+):**
- KHÔNG trả Entity trực tiếp ra API — luôn dùng DTO
- Request/Response DTO tách riêng folder `dto/request/` và `dto/response/`
- Pagination trả `PageResponse<T>` chuẩn hóa, KHÔNG leak `Page<T>` của Spring

### 7. Anti-patterns CẤM

| Anti-pattern | Thay bằng |
|---|---|
| ❌ Join chéo DB giữa service khác boundary | REST/gRPC call hoặc Kafka event |
| ❌ Dual-write (ghi đồng thời DB + Kafka/ES) | CDC (Debezium) hoặc Outbox pattern |
| ❌ Hardcode mã đơn vị, magic number/string | Enum, config, hoặc DB lookup |
| ❌ Business logic trong Controller | Đưa vào Service |
| ❌ Trả Entity ra API | Dùng DTO + Mapper |
| ❌ `throw new RuntimeException("string")` | Dùng `ErrorCode` enum + `BusinessException` |
| ❌ `ddl-auto: update` ở production | Flyway migration |
| ❌ `@EventListener` cho side-effect (email, notification) | `@TransactionalEventListener(AFTER_COMMIT)` |
| ❌ OFFSET pagination cho danh sách lớn | Keyset pagination |
| ❌ Tạo file/folder ngoài cấu trúc package | Hỏi user trước |

### 8. Cross-references đến Skills chi tiết

Khi cần kiến thức sâu hơn về từng chủ đề, đọc skill tương ứng:
- DDD + Hexagonal chi tiết → `@collabnote-ddd-hexagonal`
- Kafka, Saga, CQRS, CDC → `@collabnote-event-driven`
- WebSocket, OT/CRDT, Presence → `@collabnote-realtime-collab`
- Circuit Breaker, Observability → `@collabnote-resilience-observability`
- Cache, Lock, Pagination, Search → `@collabnote-data-strategy`
