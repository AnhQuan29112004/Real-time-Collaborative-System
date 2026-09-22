---
name: collabnote-data-strategy
description: Data layer patterns cho CollabNote — Caching strategy (Cache-Aside vs Write-through, invalidation rules), Concurrency control (Optimistic @Version vs Pessimistic FOR UPDATE), Keyset pagination (không OFFSET), Read replica consistency, Sharding strategy (workspace_id), Search (Elasticsearch vs Postgres GIN), Public link security (token entropy, rate-limit). Dùng khi thiết kế database schema, query optimization, caching, pagination, hoặc search infrastructure.
---

# CollabNote — Data Strategy

## 1. Caching Strategy

### Cache-Aside Pattern (Khuyến nghị cho CollabNote)

```java
// READ: Cache trước, miss thì query DB
public DocumentResponse getDocument(Long id) {
    String cacheKey = "doc:" + id;

    // 1. Đọc cache
    DocumentResponse cached = redisTemplate.opsForValue().get(cacheKey);
    if (cached != null) {
        return cached;
    }

    // 2. Cache miss → query DB
    Document doc = documentRepository.findById(id)
        .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND));
    DocumentResponse response = documentMapper.toResponse(doc);

    // 3. Ghi vào cache + TTL
    redisTemplate.opsForValue().set(cacheKey, response, Duration.ofMinutes(30));
    return response;
}

// WRITE: Ghi DB trước, invalidate cache sau
@Transactional
public DocumentResponse updateDocument(Long id, UpdateDocumentRequest req) {
    Document doc = documentRepository.findById(id)
        .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND));
    documentMapper.updateEntity(doc, req);
    documentRepository.save(doc);

    // Invalidate cache — next read sẽ query DB và cache lại
    redisTemplate.delete("doc:" + id);

    return documentMapper.toResponse(doc);
}
```

### Cache-Aside vs Write-through

| Aspect | Cache-Aside + TTL | Write-through |
|---|---|---|
| Read | Cache hit → return. Miss → query DB + cache | Cache hit → return. Miss → query DB + cache |
| Write | Ghi DB → **delete** cache | Ghi DB → **update** cache đồng thời |
| Consistency | Stale data trong TTL window | ✅ Luôn fresh |
| Failure risk | Cache miss = slower read | Cache write fail = data lệch |
| Complexity | Đơn giản | Phức tạp hơn (đảm bảo atomic write) |

**Chọn cho CollabNote:** **Cache-Aside + TTL** (30 phút) — đơn giản, chấp nhận stale data ngắn, phù hợp document metadata.

### Cache Invalidation Rules — BẮT BUỘC xác định trước khi cache

| Cache item | Key | TTL | Owner invalidate | Stale tolerance |
|---|---|---|---|---|
| Document metadata | `doc:{id}` | 30 min | DocumentService (on update/delete) | 30s OK |
| Workspace members | `ws:members:{wsId}` | 1 hour | WorkspaceService (on member change) | 1 min OK |
| User profile | `user:{id}` | 1 hour | UserService (on update) | 5 min OK |
| Document permission | `perm:{docId}:{userId}` | 5 min | PermissionService (on change) | ❌ NOT OK — stale = sai quyền |
| Folder tree | `folders:{wsId}` | 15 min | FolderService (on CRUD) | 1 min OK |

> **KHÔNG cache trạng thái mutable quan trọng** (vd: document permission) với TTL dài — stale data có thể cho phép user truy cập document đã bị revoke quyền.

### Cache Stampede Prevention

```java
// Khi nhiều request cùng lúc gặp cache miss → tất cả query DB đồng thời = overload
// Giải pháp: distributed lock
public DocumentResponse getDocumentWithLock(Long id) {
    String cacheKey = "doc:" + id;
    DocumentResponse cached = redisTemplate.opsForValue().get(cacheKey);
    if (cached != null) return cached;

    // Distributed lock — chỉ 1 thread query DB, còn lại chờ
    String lockKey = "lock:doc:" + id;
    Boolean locked = redisTemplate.opsForValue()
        .setIfAbsent(lockKey, "1", Duration.ofSeconds(5));

    if (Boolean.TRUE.equals(locked)) {
        try {
            // Query DB + cache
            Document doc = documentRepository.findById(id).orElseThrow(...);
            DocumentResponse response = documentMapper.toResponse(doc);
            redisTemplate.opsForValue().set(cacheKey, response, Duration.ofMinutes(30));
            return response;
        } finally {
            redisTemplate.delete(lockKey);
        }
    } else {
        // Chờ 100ms rồi retry đọc cache
        Thread.sleep(100);
        return getDocumentWithLock(id);
    }
}
```

## 2. Concurrency Control

### Optimistic Lock — cho metadata (2 người sửa title, permission)

```java
@Entity
public class Document {
    @Version
    private Long version; // Hibernate tự tăng mỗi lần update

    // Khi 2 request update cùng lúc:
    // Request A: UPDATE ... WHERE id = 1 AND version = 5 → OK, version = 6
    // Request B: UPDATE ... WHERE id = 1 AND version = 5 → 0 rows updated → OptimisticLockException
}
```

```java
// Service xử lý OptimisticLockException
@Transactional
public DocumentResponse updateTitle(Long id, String newTitle) {
    try {
        Document doc = documentRepository.findById(id).orElseThrow(...);
        doc.setTitle(newTitle);
        documentRepository.save(doc);
        return documentMapper.toResponse(doc);
    } catch (OptimisticLockException e) {
        throw new BusinessException(ErrorCode.DOCUMENT_MODIFIED_CONCURRENTLY);
        // FE nhận lỗi → hiển thị "Document đã được sửa bởi người khác, vui lòng reload"
    }
}
```

### Pessimistic Lock — chỉ khi cần tuần tự hóa thật sự

```java
// Ví dụ: tránh 2 admin cùng duyệt 1 document
@Query("SELECT d FROM Document d WHERE d.id = :id")
@Lock(LockModeType.PESSIMISTIC_WRITE)  // SELECT ... FOR UPDATE
Optional<Document> findByIdForUpdate(@Param("id") Long id);
```

### Khi nào dùng gì?

| Scenario | Lock type | Lý do |
|---|---|---|
| 2 người sửa title document | Optimistic (`@Version`) | Conflict ít xảy ra, retry nhẹ |
| 2 admin approve cùng document | Pessimistic (`FOR UPDATE`) | Phải tuần tự, không được approve 2 lần |
| 2 người sửa **nội dung** document | ❌ KHÔNG dùng lock | Dùng OT/CRDT (Phase 4) |
| Đổi workspace member role | Optimistic | Conflict hiếm |

**QUAN TRỌNG:** Lock (cả optimistic lẫn pessimistic) KHÔNG dùng cho nội dung document — đó là việc của OT/CRDT.

## 3. Pagination — Keyset (KHÔNG dùng OFFSET)

### Tại sao không OFFSET?

```sql
-- OFFSET 10000: DB phải scan qua 10000 rows rồi bỏ → O(offset) performance
SELECT * FROM documents WHERE workspace_id = 1 ORDER BY updated_at DESC OFFSET 10000 LIMIT 20;

-- Keyset: DB jump thẳng đến vị trí cần → O(1) performance
SELECT * FROM documents
WHERE workspace_id = 1 AND updated_at < '2024-01-15T10:30:00'
ORDER BY updated_at DESC
LIMIT 20;
```

### Composite Index BẮT BUỘC

```sql
-- Index cho keyset pagination
CREATE INDEX idx_doc_workspace_updated ON documents(workspace_id, updated_at DESC);

-- Index cho document permission check
CREATE INDEX idx_doc_perm_user ON document_permissions(document_id, user_id);
```

### Implementation

```java
// Repository
@Query("SELECT d FROM Document d WHERE d.workspaceId = :wsId AND d.updatedAt < :cursor ORDER BY d.updatedAt DESC")
Slice<Document> findByWorkspaceIdBefore(
    @Param("wsId") Long workspaceId,
    @Param("cursor") Instant cursor,
    Pageable pageable
);

// Service
public PageResponse<DocumentResponse> listDocuments(Long wsId, Instant cursor, int size) {
    Pageable pageable = PageRequest.of(0, size); // page luôn là 0 với keyset
    Slice<Document> slice = documentRepository.findByWorkspaceIdBefore(wsId, cursor, pageable);

    List<DocumentResponse> content = slice.getContent().stream()
        .map(documentMapper::toResponse)
        .toList();

    // Cursor cho page tiếp theo = updatedAt của item cuối cùng
    Instant nextCursor = content.isEmpty() ? null :
        content.get(content.size() - 1).updatedAt();

    return PageResponse.of(content, nextCursor, slice.hasNext());
}
```

### PageResponse chuẩn hóa

```java
public record PageResponse<T>(
    List<T> content,
    Object nextCursor,      // Cursor cho page tiếp theo (null nếu hết)
    boolean hasNext,
    int size
) {
    public static <T> PageResponse<T> of(List<T> content, Object nextCursor, boolean hasNext) {
        return new PageResponse<>(content, nextCursor, hasNext, content.size());
    }
}
```

## 4. Read Replica — Read-after-write Consistency

### Vấn đề

```
Client ghi document → Primary DB (committed)
Client đọc ngay     → Replica DB (chưa replicate xong) → không thấy data vừa ghi!
```

### Giải pháp: Session Consistency

```java
@Service
public class DocumentServiceImpl implements DocumentService {

    @Transactional
    public DocumentResponse create(CreateDocumentRequest req) {
        Document doc = documentMapper.toEntity(req);
        documentRepository.save(doc);  // Ghi vào Primary

        // QUAN TRỌNG: trả kết quả từ entity vừa save, KHÔNG query lại từ Replica
        return documentMapper.toResponse(doc);
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocument(Long id) {
        // readOnly = true → Spring có thể route sang Replica
        // Chấp nhận lag ngắn (< 1s thường) cho read thông thường
    }
}
```

### Dynamic Datasource Routing

```java
public class ReadWriteRoutingDataSource extends AbstractRoutingDataSource {
    @Override
    protected Object determineCurrentLookupKey() {
        return TransactionSynchronizationManager.isCurrentTransactionReadOnly()
            ? "replica"
            : "primary";
    }
}
```

**Quy tắc:**
- `@Transactional` → Primary
- `@Transactional(readOnly = true)` → Replica
- Vừa ghi xong, cần đọc ngay → dùng entity đã save, KHÔNG query lại

## 5. Sharding (Optional — Phase 7)

### Strategy: Shard theo workspace_id

```
workspace_id % N = shard number

Shard 0: workspace 0, 3, 6, 9, ...
Shard 1: workspace 1, 4, 7, 10, ...
Shard 2: workspace 2, 5, 8, 11, ...
```

### Trade-offs

| Aspect | Ưu điểm | Nhược điểm |
|---|---|---|
| Query trong workspace | ✅ Nhanh — chỉ hit 1 shard | — |
| Cross-workspace query | — | ❌ Phải scatter-gather tất cả shard |
| Admin search toàn hệ thống | — | ❌ Rất phức tạp |
| Rebalancing khi thêm shard | — | ❌ Phải migrate data |

**Khuyến nghị:** Dừng ở **Read Replica** cho dự án học tập. Sharding chỉ khi có nhu cầu scale thực sự (> triệu workspace).

## 6. Search — Elasticsearch vs Postgres GIN

### So sánh

| Aspect | Elasticsearch | Postgres GIN (full-text search) |
|---|---|---|
| Setup | Thêm 1 service phải vận hành | Không cần thêm gì |
| Tính năng | ✅ Fuzzy, synonym, faceted, relevance scoring | Cơ bản: tsvector, tsquery |
| Performance (triệu docs) | ✅ Rất nhanh | Chậm hơn khi data lớn |
| Realtime indexing | Near-realtime (refresh interval) | Realtime (cùng transaction) |
| Sync data | Cần CDC/Debezium | Không cần (cùng DB) |
| Ops burden | Cao (cluster, memory, disk) | Thấp |

### Khi nào dùng gì?

```
Câu hỏi: "Tính năng search có phải lợi thế cạnh tranh?"
  → Có (vd: Notion-level search) → Elasticsearch
  → Không (vd: search cơ bản theo title/content) → Postgres GIN

Câu hỏi: "Có bao nhiêu documents?"
  → < 100K → Postgres GIN đủ
  → > 100K → Cân nhắc Elasticsearch
```

### Postgres GIN setup nhanh

```sql
-- Tạo tsvector column
ALTER TABLE documents ADD COLUMN search_vector tsvector;

-- Tạo GIN index
CREATE INDEX idx_doc_search ON documents USING gin(search_vector);

-- Trigger tự update search_vector khi insert/update
CREATE TRIGGER doc_search_update
    BEFORE INSERT OR UPDATE ON documents
    FOR EACH ROW
    EXECUTE FUNCTION tsvector_update_trigger(search_vector, 'pg_catalog.english', title, content);
```

```java
// Repository
@Query(value = "SELECT * FROM documents WHERE search_vector @@ plainto_tsquery('english', :query) AND workspace_id = :wsId ORDER BY ts_rank(search_vector, plainto_tsquery('english', :query)) DESC LIMIT :limit", nativeQuery = true)
List<Document> fullTextSearch(@Param("query") String query, @Param("wsId") Long wsId, @Param("limit") int limit);
```

## 7. Public Link Security

### Token Design

```java
public class ShareLinkService {

    public DocumentShareLink createShareLink(Long documentId, Permission permission, Duration expiry) {
        // Token entropy ≥ 32 bytes → 256 bits → brute-force infeasible
        String token = generateSecureToken();

        DocumentShareLink link = DocumentShareLink.builder()
            .documentId(documentId)
            .token(token)
            .permission(permission)  // VIEW_ONLY hoặc EDIT
            .expiresAt(expiry != null ? Instant.now().plus(expiry) : null)
            .build();

        return shareLinkRepository.save(link);
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32]; // 256 bits
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
```

### Security Checklist cho Public Link

| Aspect | Yêu cầu |
|---|---|
| Token entropy | ≥ 32 bytes (UUID v4 = 122 bits cũng chấp nhận được) |
| Rate limit | Riêng per token: 30 requests/phút (chống brute-force dò token) |
| Expiry | Optional `expires_at`, check mỗi request |
| Revoke | Owner có thể xoá link bất kỳ lúc nào |
| Permission | Chỉ VIEW_ONLY hoặc EDIT, không có ADMIN |
| Audit | Log access qua public link (ai, khi nào, từ IP nào) |
| HTTPS only | Token trong URL → BẮT BUỘC HTTPS, không HTTP |
