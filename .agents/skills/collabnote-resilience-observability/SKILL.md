---
name: collabnote-resilience-observability
description: Resilience patterns + Observability cho CollabNote production-ready — Circuit Breaker (Resilience4j, threshold tuning, fallback strategy), Retry + Idempotency, Rate Limiting (Redis token bucket), Bulkhead (tách connection pool), Observability stack (ELK logging, Prometheus+Grafana metrics, Jaeger tracing), structured logging (JSON + MDC), resilience testing. Dùng khi thiết kế inter-service communication, fallback UX, monitoring infrastructure, hoặc debug production issues.
---

# CollabNote — Resilience & Observability

## 1. Circuit Breaker (Resilience4j)

### Nguyên lý

```
CLOSED (bình thường) → failure rate vượt threshold → OPEN (chặn tất cả request)
                                                          ↓ sau wait duration
                                                     HALF_OPEN (cho 1 vài request thử)
                                                          ↓ thành công → CLOSED
                                                          ↓ thất bại → OPEN lại
```

### Config cho CollabNote

```yaml
resilience4j:
  circuitbreaker:
    instances:
      documentService:
        sliding-window-type: COUNT_BASED
        sliding-window-size: 10           # Đo trên 10 request gần nhất
        failure-rate-threshold: 50        # Mở khi ≥50% fail
        wait-duration-in-open-state: 10s  # Chờ 10s trước khi thử lại
        permitted-number-of-calls-in-half-open-state: 3
        record-exceptions:
          - java.io.IOException
          - java.util.concurrent.TimeoutException
        ignore-exceptions:
          - com.company.common.exception.BusinessException  # Lỗi business ≠ lỗi infra
```

### Threshold Tuning — Trade-off

| Threshold | Mở quá sớm (low threshold) | Mở quá muộn (high threshold) |
|---|---|---|
| Failure rate 30% | ✅ Bảo vệ nhanh | ❌ Chặn nhầm khi chỉ vài request fail |
| Failure rate 70% | ❌ Không bảo vệ kịp (service downstream đã overload) | ✅ Ít false positive |
| Window size 5 | Nhạy, phản ứng nhanh | Dễ bị 1-2 lỗi ngẫu nhiên trigger |
| Window size 50 | Chậm phản ứng | Ổn định, ít false positive |

**Khuyến nghị:** Bắt đầu 50% failure rate, window 10, monitor và chỉnh dần.

### Fallback Strategy theo Use Case

```java
@CircuitBreaker(name = "documentService", fallbackMethod = "getDocumentFallback")
public DocumentResponse getDocument(Long documentId) {
    return documentServiceClient.getDocument(documentId);
}

// Fallback khi circuit breaker mở
public DocumentResponse getDocumentFallback(Long documentId, Throwable ex) {
    // Strategy 1: Trả cache cũ
    DocumentResponse cached = redisTemplate.opsForValue().get("doc:" + documentId);
    if (cached != null) {
        return cached; // Stale data nhưng có data
    }

    // Strategy 2: Trả lỗi rõ ràng
    throw new ServiceUnavailableException("Document Service tạm thời không khả dụng, thử lại sau");
}
```

| Use Case | Fallback Strategy | Lý do |
|---|---|---|
| Sync Service → Document Service (đọc doc) | Trả cache cũ | User đang edit, cần data dù stale |
| Sync Service → Document Service (lưu doc) | Queue lại, retry sau | Không được mất data user đã gõ |
| API Gateway → Auth Service (verify token) | Reject request | Security — không fallback khi auth fail |
| Document Service → Notification Service | Skip + log | Notification fail ≠ document fail |

## 2. Retry + Idempotency

### Retry với Resilience4j

```yaml
resilience4j:
  retry:
    instances:
      documentService:
        max-attempts: 3
        wait-duration: 500ms
        exponential-backoff-multiplier: 2   # 500ms → 1s → 2s
        retry-exceptions:
          - java.io.IOException
        ignore-exceptions:
          - com.company.common.exception.BusinessException
```

**Quy tắc retry:**
- ✅ Retry cho: network timeout, connection refused, 503
- ❌ KHÔNG retry cho: 400 (bad request), 404 (not found), 409 (conflict) — retry sẽ cho cùng kết quả
- ❌ KHÔNG retry cho: business exception (vd email trùng) — đây là lỗi logic, không phải lỗi tạm thời

### Idempotency Key cho API

```java
@PostMapping("/documents")
public ResponseEntity<ApiResponse<DocumentResponse>> createDocument(
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        @Valid @RequestBody CreateDocumentRequest request) {

    // Kiểm tra đã xử lý request này chưa
    Optional<DocumentResponse> existing = idempotencyStore.get(idempotencyKey);
    if (existing.isPresent()) {
        return ResponseEntity.ok(ApiResponse.success(existing.get()));
    }

    DocumentResponse result = documentService.create(request);
    idempotencyStore.save(idempotencyKey, result, Duration.ofHours(24));
    return ResponseEntity.status(201).body(ApiResponse.success(result));
}
```

## 3. Rate Limiting

### Redis Token Bucket ở API Gateway

```java
@Component
public class RateLimitFilter implements WebFilter {

    // Mỗi user: 100 requests/phút
    private static final int BUCKET_SIZE = 100;
    private static final Duration REFILL_PERIOD = Duration.ofMinutes(1);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String userId = extractUserId(exchange);
        String key = "ratelimit:" + userId;

        // Redis INCR + EXPIRE atomic
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == 1) {
            redisTemplate.expire(key, REFILL_PERIOD);
        }
        if (count > BUCKET_SIZE) {
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);
    }
}
```

### Rate Limit riêng cho Public Link

```java
// Public link token: rate limit PER TOKEN, không per user (vì không có user)
String key = "ratelimit:publink:" + linkToken;
// Giới hạn thấp hơn: 30 requests/phút per token → chống brute-force dò token
```

## 4. Bulkhead — Tách Connection Pool

### Vấn đề: 1 downstream chậm cạn toàn bộ thread pool

```
TRƯỚC (shared thread pool):
DocumentService → [Thread Pool: 50 threads]
  → Auth Service call (fast: 50ms)
  → Search Service call (SLOW: 5s — timeout)
  → Notification Service call (fast: 100ms)

Kết quả: Search Service chậm → 50 threads đều stuck chờ Search → Auth + Notification call cũng bị block
```

### Giải pháp: Bulkhead per downstream

```yaml
resilience4j:
  bulkhead:
    instances:
      authService:
        max-concurrent-calls: 20      # Max 20 thread cho Auth call
      searchService:
        max-concurrent-calls: 10      # Max 10 thread cho Search call
      notificationService:
        max-concurrent-calls: 5       # Max 5 thread cho Notification call
```

```
SAU (separated pools):
DocumentService
  → [Auth Pool: 20 threads] → Auth Service
  → [Search Pool: 10 threads] → Search Service (SLOW → chỉ cạn 10 thread)
  → [Notification Pool: 5 threads] → Notification Service

Kết quả: Search chậm chỉ ảnh hưởng Search pool, Auth + Notification vẫn hoạt động bình thường
```

## 5. Observability Stack

### Logging — ELK (Elasticsearch + Logstash + Kibana)

#### Structured Logging (JSON format)

```java
// application.yml
logging:
  pattern:
    console: '{"timestamp":"%d","level":"%p","service":"${spring.application.name}","traceId":"%X{traceId}","spanId":"%X{spanId}","message":"%m"}%n'
```

#### MDC Correlation ID — Xuyên suốt qua các service

```java
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest req, ...) {
        String traceId = req.getHeader("X-Trace-Id");
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
        }
        MDC.put("traceId", traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
```

- Mỗi request có `traceId` duy nhất → search log ở Kibana theo traceId → thấy toàn bộ log xuyên service
- Forward `traceId` khi gọi service khác (qua header `X-Trace-Id`)

### Metrics — Prometheus + Grafana

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health, prometheus, info
  metrics:
    tags:
      application: ${spring.application.name}
```

**Metrics quan trọng cần monitor:**

| Metric | Ý nghĩa | Alert khi |
|---|---|---|
| `http_server_requests_seconds` | Latency per endpoint | p99 > 2s |
| `hikaricp_connections_active` | DB connection pool active | > 80% pool size |
| `hikaricp_connections_pending` | Threads chờ connection | > 0 kéo dài > 10s |
| `jvm_memory_used_bytes` | JVM heap usage | > 80% max heap |
| `resilience4j_circuitbreaker_state` | Circuit breaker state | state = OPEN |
| `kafka_consumer_lag` | Consumer lag | Lag > 1000 kéo dài |

### Tracing — Jaeger (Distributed Tracing)

```
Client → API Gateway → Auth Service → Document Service → Kafka → Notification Service
  │         │              │               │                          │
  └─────────┴──────────────┴───────────────┴──────────────────────────┘
                        1 trace, nhiều spans
```

- Mỗi request tạo 1 **trace** (chứa nhiều **spans**)
- Span = 1 operation (vd: DB query, REST call, Kafka produce)
- Jaeger UI: xem waterfall view → biết request đi qua bao nhiêu service, mỗi service tốn bao lâu

### Trả traceId trong response header

```java
// Để FE/user gửi traceId khi report bug → backend tìm log nhanh
response.addHeader("X-Trace-Id", MDC.get("traceId"));
```

## 6. Resilience Testing Checklist

| Test | Cách test | Expected |
|---|---|---|
| Tắt Auth Service | Stop container | Document Service trả 503, không crash |
| Tắt Search Service | Stop container | CRUD document vẫn hoạt động (search unavailable) |
| Tắt Kafka | Stop broker | Event queue lại, retry khi Kafka lên lại |
| DB connection pool cạn | Giảm pool size xuống 2, load test | Request bị reject gracefully, không hang |
| Network latency | `tc netem add delay 2s` | Circuit breaker mở, fallback hoạt động |
| High load | k6/JMeter 1000 concurrent | Rate limiter hoạt động, không OOM |

## 7. Logging Best Practices

### DO

```java
log.info("Document created: documentId={}, workspaceId={}", doc.getId(), doc.getWorkspaceId());
log.warn("Circuit breaker OPEN for service={}, fallback used", serviceName);
log.error("Failed to save document: documentId={}", docId, exception);
```

### DON'T

```java
// ❌ Log sensitive data
log.info("User login: email={}, password={}", email, password);

// ❌ Log quá nhiều (performance impact)
log.info("Processing item {} of {}", i, total); // trong loop 10000 items

// ❌ Log không có context
log.error("Error occurred"); // cái gì error? ở đâu?

// ❌ Log object lớn
log.debug("Document content: {}", document.getFullContent()); // 10MB content
```
