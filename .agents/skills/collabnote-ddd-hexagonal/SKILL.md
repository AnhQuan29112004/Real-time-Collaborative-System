---
name: collabnote-ddd-hexagonal
description: DDD domain classification + Hexagonal Architecture cho dự án CollabNote. Hướng dẫn chi tiết phân loại domain (Core/Supporting/Generic), thiết kế Hexagonal cho Core domain, Context Map giữa các service, Monolith→Microservice migration path, và ADR protocol. Dùng khi thiết kế module mới, tách service, hoặc viết ADR.
---

# CollabNote — DDD + Hexagonal Architecture

## 1. Domain Classification chi tiết

### Bảng phân loại domain đầy đủ

| Domain | Loại | Đặc điểm | Kiến trúc | Test coverage |
|---|---|---|---|---|
| **Document** (CRUD, metadata, folder) | Core | Logic phức tạp, là lý do sản phẩm tồn tại | Hexagonal | ≥80% |
| **Collaboration** (real-time editing) | Core | Phần khó nhất, phân biệt với app CRUD thường | Hexagonal | ≥80% |
| **Version History** (snapshot, diff) | Core | Gắn chặt với Document, cần replay lịch sử | Hexagonal | ≥80% |
| **Auth** (JWT, OAuth2 Google) | Supporting | Cần đúng, không cần tối ưu quá | Layered | ≥60% |
| **User** (profile, preferences) | Supporting | CRUD đơn giản | Layered | ≥60% |
| **Workspace** (CRUD, membership, role) | Supporting | Quản trị, không phải core business | Layered | ≥60% |
| **Permission** (workspace role + doc permission) | Supporting | Phức tạp nhưng không phải "lý do sản phẩm tồn tại" | Layered | ≥70% |
| **Notification** (email, in-app) | Generic | Có thể thay bằng SaaS (SendGrid, Novu) | Simple | ≥40% |
| **Search** (full-text) | Generic | Có thể thay bằng Algolia, Typesense | Simple | ≥40% |

### Cách phân biệt Core vs Supporting vs Generic

```
Câu hỏi 1: "Nếu bỏ module này, sản phẩm còn có ý nghĩa không?"
  → Không → Core domain

Câu hỏi 2: "Module này có thể thay bằng giải pháp có sẵn (SaaS, library) mà không mất lợi thế cạnh tranh?"
  → Có → Generic domain

Câu hỏi 3: "Module này cần đúng nhưng không tạo ra lợi thế cạnh tranh?"
  → Có → Supporting domain
```

## 2. Hexagonal Architecture cho Core Domain

### Cấu trúc package cho Core domain (Document/Collaboration/Version)

```
<core-domain>/
├── domain/                         ← BUSINESS LOGIC THUẦN — không phụ thuộc framework
│   ├── model/                      ← Domain entities, value objects
│   │   ├── Document.java
│   │   ├── DocumentVersion.java
│   │   └── DocumentContent.java    ← Value object
│   ├── port/
│   │   ├── in/                     ← Input ports (use cases)
│   │   │   ├── CreateDocumentUseCase.java       ← interface
│   │   │   ├── UpdateDocumentUseCase.java       ← interface
│   │   │   └── GetDocumentUseCase.java          ← interface
│   │   └── out/                    ← Output ports (dependencies)
│   │       ├── DocumentRepository.java          ← interface (KHÔNG phải Spring Data)
│   │       ├── DocumentEventPublisher.java      ← interface
│   │       └── DocumentCachePort.java           ← interface
│   ├── service/                    ← Domain services implementing input ports
│   │   └── DocumentDomainService.java
│   └── event/                      ← Domain events
│       ├── DocumentCreatedEvent.java
│       └── DocumentUpdatedEvent.java
├── adapter/                        ← IMPLEMENTATIONS — phụ thuộc framework
│   ├── in/
│   │   ├── web/                    ← REST controllers (driving adapter)
│   │   │   └── DocumentController.java
│   │   └── messaging/             ← Kafka consumer (driving adapter)
│   │       └── DocumentEventConsumer.java
│   └── out/
│       ├── persistence/           ← JPA implementation (driven adapter)
│       │   ├── DocumentJpaRepository.java       ← Spring Data interface
│       │   ├── DocumentJpaEntity.java           ← JPA entity (khác domain model)
│       │   ├── DocumentPersistenceAdapter.java  ← implements DocumentRepository port
│       │   └── DocumentJpaMapper.java           ← Map JPA entity ↔ domain model
│       ├── messaging/             ← Kafka producer (driven adapter)
│       │   └── DocumentKafkaPublisher.java      ← implements DocumentEventPublisher port
│       └── cache/                 ← Redis (driven adapter)
│           └── DocumentRedisCache.java          ← implements DocumentCachePort port
├── dto/
│   ├── request/
│   └── response/
└── mapper/                         ← Map DTO ↔ domain model
```

### Nguyên tắc Hexagonal

1. **Domain layer KHÔNG import bất kỳ framework nào** — không Spring, không JPA, không Kafka
2. **Dependency rule**: Domain → (nothing). Adapter → Domain. KHÔNG BAO GIỜ ngược lại.
3. **Port = interface** ở domain layer. **Adapter = implementation** ở adapter layer.
4. **Input port** (use case): interface mà Controller/Consumer gọi
5. **Output port**: interface mà Domain service cần, Adapter implement
6. **Domain model ≠ JPA Entity**: Tách riêng, dùng mapper. Vì JPA entity phụ thuộc Hibernate annotations, domain model thì không.

### So sánh: Hexagonal vs Layered

| Aspect | Hexagonal (Core domain) | Layered (Supporting domain) |
|---|---|---|
| Business logic phụ thuộc framework? | ❌ Không | ✅ Có thể (Service dùng @Transactional) |
| Domain model = JPA Entity? | ❌ Tách riêng | ✅ Có thể dùng chung |
| Số lượng file | Nhiều hơn (~2x) | Ít hơn |
| Test dễ? | ✅ Rất dễ mock | Vừa phải |
| Đổi tech (vd Postgres→Mongo) | ✅ Chỉ đổi adapter | Sửa nhiều nơi |
| Setup ban đầu | Tốn thời gian hơn | Nhanh |
| Khi nào dùng? | Logic phức tạp, cần test kỹ, sẽ thay đổi nhiều | Logic đơn giản, ổn định |

## 3. Context Map — Service giao tiếp với nhau

```
┌─────────────┐    REST/gRPC     ┌─────────────────┐
│ Auth Service │◄───────────────►│ Document Service │
└─────────────┘                  └────────┬────────┘
                                          │
                                    Kafka (async)
                                          │
                    ┌─────────────────────┼─────────────────────┐
                    ▼                     ▼                     ▼
           ┌──────────────┐    ┌───────────────────┐   ┌──────────────┐
           │ Sync Service │    │ Notification Svc  │   │ Search Svc   │
           │ (WebSocket)  │    │ (consume events)  │   │ (consume CDC)│
           └──────────────┘    └───────────────────┘   └──────────────┘
                 │
           Redis Pub/Sub
           (presence, cursor)
```

**Quy tắc giao tiếp:**
- Đồng bộ (REST/gRPC): Khi cần response ngay (vd: Auth verify token → Document Service)
- Bất đồng bộ (Kafka): Khi side-effect không cần response (vd: Document updated → Notification)
- Redis Pub/Sub: Chỉ cho real-time broadcast (presence, cursor) — không cho persistent messaging

## 4. Monolith → Microservice Migration Path

### Phase 1 (Monolith): Package là boundary

```
com.company.collabnote/
├── document/          ← Tương lai = Document Service
├── collaboration/     ← Tương lai = Sync Service  
├── identity/          ← Tương lai = Auth Service
├── notification/      ← Tương lai = Notification Service
├── search/            ← Tương lai = Search Service
└── common/            ← Shared utilities
```

- Mỗi domain package có interface riêng cho cross-domain communication
- KHÔNG gọi trực tiếp internal class của domain khác — luôn qua interface

### Dấu hiệu nên tách microservice

1. 2 module cần scale **khác nhau** (vd Sync Service cần nhiều instance hơn Auth)
2. 2 module có **release cycle** khác nhau (vd Document thay đổi hằng ngày, Auth hiếm khi đổi)
3. 1 module **fail** không nên kéo module khác fail (vd Search down không ảnh hưởng editing)
4. Team khác nhau own module khác nhau

### Cách tách an toàn

1. Tạo service mới, copy code từ module monolith
2. Đổi cross-module call thành REST/Kafka
3. Chạy song song (feature flag) 1-2 tuần
4. Tắt module cũ trong monolith
5. Viết ADR cho quyết định tách

## 5. ADR — Danh sách cần viết theo Roadmap

| ADR | Phase | Chủ đề |
|---|---|---|
| ADR-01 | 0 | Monolith vs Microservice ngay từ đầu |
| ADR-02 | 1 | Hexagonal cho Collaboration, Layered cho Auth — dựa trên DDD classification |
| ADR-02b | 1 | Workspace role + Document permission conflict resolution rule |
| ADR-03 | 2 | Cache invalidation: Write-through vs Cache-Aside |
| ADR-04 | 2 | Optimistic vs Pessimistic lock cho metadata update |
| ADR-04b | 2 | Public link token design (entropy, expiry, rate-limit) |
| ADR-05 | 3 | Saga Orchestration vs Choreography cho xoá document |
| ADR-06 | 3 | At-least-once + idempotency key thay vì exactly-once |
| ADR-07 | 4 | OT vs CRDT cho conflict resolution |
| ADR-08 | 4 | Sticky session vs Redis Pub/Sub cho WebSocket scale-out |
| ADR-09 | 5 | CDC async thay vì dual-write cho CQRS read model |
| ADR-10 | 5 | Elasticsearch vs Postgres GIN cho search |
| ADR-11 | 6 | Circuit breaker threshold & fallback strategy |
| ADR-12 | 7 | Read-after-write consistency cho read replica |
| ADR-13 | 7 | Sharding hay dừng ở read replica |

### ADR Template

```markdown
### ADR-XX: <tên quyết định>

**Date:** YYYY-MM-DD
**Status:** Proposed / Accepted / Deprecated / Superseded by ADR-YY

**Context:**
<Vấn đề đang gặp, bối cảnh kỹ thuật và business>

**Decision:**
<Chọn phương án nào, mô tả cụ thể>

**Alternatives considered:**
1. <Phương án B> — loại vì <lý do>
2. <Phương án C> — loại vì <lý do>

**Trade-off accepted:**
<Cái gì mình đánh đổi khi chọn phương án này>

**Consequences:**
- Positive: <lợi ích>
- Negative: <hạn chế phải chấp nhận>
- Risks: <rủi ro tiềm ẩn>
```
