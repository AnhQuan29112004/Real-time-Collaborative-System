---
name: backend-architecture
description: Kiến trúc và nguyên tắc thiết kế toàn diện cho Backend (Spring Boot 3.x + Java 17+) — layered architecture chuẩn (interface/impl), Design Pattern (Strategy, Factory, Facade, Chain of Responsibility, Observer), AOP cho cross-cutting concerns, exception handling có error-code, validation, chống N+1, caching, domain event, security method-level, resilience, testing, observability, migration, config management. Dùng skill này bất cứ khi nào code/review/thiết kế backend Spring Boot, đặc biệt với nghiệp vụ có phân cấp tổ chức, luồng định tuyến/phê duyệt nhiều cấp.
---

# Backend Architecture Rules (Spring Boot 3.x + Java 17+)

## 0. Triết lý nền tảng

- Áp dụng tinh thần Clean Architecture / Hexagonal ở mức vừa phải cho Spring Boot: **business logic không phụ thuộc chi tiết framework**, phụ thuộc vào abstraction (interface), không phụ thuộc implementation cụ thể.
- SOLID là kim chỉ nam khi quyết định tách class, tách interface, chọn pattern.
- Một class chỉ nên có **một lý do để thay đổi**. Nếu Service vừa lo business rule vừa lo gửi email vừa lo ghi audit → tách ra (Event, AOP).

## 1. Cấu trúc package chuẩn — domain-first (mở rộng)

```
src/main/java/com/<company>/<app>/
├── <domain>/
│   ├── controller/
│   ├── service/
│   │   ├── <DomainService>.java        ← interface
│   │   └── impl/<DomainServiceImpl>.java
│   ├── repository/
│   ├── specification/                  ← JPA Specification cho dynamic query/filter
│   ├── dto/
│   │   ├── request/
│   │   └── response/
│   ├── mapper/                         ← MapStruct
│   ├── entity/
│   ├── event/                          ← Domain event (VD: UserCreatedEvent)
│   └── validator/                      ← Custom Bean Validation cho business rule
├── common/
│   ├── exception/
│   │   ├── BaseException.java
│   │   ├── ErrorCode.java               ← enum: code + i18n message key + HTTP status
│   │   └── GlobalExceptionHandler.java
│   ├── response/                        ← ApiResponse<T>, PageResponse<T>
│   ├── config/
│   ├── aspect/                          ← AOP: Logging, Audit, Performance
│   ├── security/
│   └── audit/                           ← BaseEntity (createdBy, createdDate...)
└── infrastructure/
    ├── messaging/                       ← Kafka/RabbitMQ producer, consumer
    ├── cache/
    ├── storage/
    └── client/                          ← Feign/WebClient wrapper + Resilience4j
```

## 2. Layer responsibilities — HARD RULES

### Controller — CHỈ làm những việc này
```java
@PostMapping
public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody CreateUserRequest req) {
    return ResponseEntity.ok(ApiResponse.success(userService.create(req)));
}
```
KHÔNG chứa business logic, KHÔNG inject Repository trực tiếp — luôn qua Service.

### Service — BẮT BUỘC tách Interface + Impl
Lý do: dễ mock test (Mockito), dễ swap implementation (VD: đổi thuật toán tính phí mà không sửa code gọi), tuân thủ Dependency Inversion.
```java
public interface UserService {
    UserResponse create(CreateUserRequest req);
}

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public UserResponse create(CreateUserRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATED);
        }
        User user = userMapper.toEntity(req);
        userRepository.save(user);
        eventPublisher.publishEvent(new UserCreatedEvent(user.getId()));
        return userMapper.toResponse(user);
    }
}
```
- Service dài >300 dòng → tách theo sub-domain: `UserService`, `UserAuthService`.
- `@Transactional` đặt ở Service method, không phải Repository.

### Repository — Data access thuần
- Chỉ query DB, KHÔNG business logic, KHÔNG xử lý null/exception (để Service xử lý).
- Query nhiều điều kiện lọc động → dùng **JPA Specification**, KHÔNG nối chuỗi JPQL thủ công.
- Query liên quan collection lazy phải kiểm tra N+1 và chọn projection, batch fetching, `@EntityGraph`, `JOIN FETCH` hoặc query chuyên biệt theo access path. Không fetch-join collection cùng pagination nếu chưa kiểm chứng SQL và pagination thực thi tại DB.

### DTO + Mapper
- **Dùng Java Record (17+) cho DTO** — immutable, giảm boilerplate, tránh setter thừa:
```java
public record CreateUserRequest(
    @NotBlank String email,
    @NotBlank @Size(min = 8) String password
) {}

public record UserResponse(Long id, String email, Instant createdAt) {}
```
- KHÔNG trả Entity trực tiếp ra ngoài — luôn dùng DTO.
- MapStruct `componentModel = "spring"`, khai báo rõ `@Mapping(target = ..., ignore = true)` cho field không map để tránh warning ẩn.
- Response phân trang chuẩn hóa qua `PageResponse<T>` (content, totalElements, totalPages, hasNext) — không trả thẳng `Page<T>` của Spring ra API.

## 3. Design Patterns — khi nào dùng

### Strategy — nhiều kiểu xử lý cùng logic
```java
public interface NotificationStrategy {
    void send(NotificationRequest request);
}
@Component("emailNotification")
public class EmailNotificationStrategy implements NotificationStrategy { ... }
```

### Factory — tạo object phức tạp theo điều kiện
```java
@Component
public class NotificationFactory {
    private final Map<NotificationType, NotificationStrategy> strategies;
    public NotificationStrategy getStrategy(NotificationType type) {
        return strategies.get(type);
    }
}
```

### Facade — Controller cần orchestrate nhiều service
```java
@Service
public class UserOnboardingFacade {
    // Inject UserService + EmailService + AuditService...
    public void onboard(CreateUserRequest req) { ... }
}
```

### Chain of Responsibility — luồng định tuyến/phê duyệt qua nhiều cấp tổ chức
Đây là pattern **tự nhiên nhất** cho bài toán định tuyến qua các `unitLevel` (xem mục 8) — thay vì if/else lồng nhau theo cấp, mỗi handler chỉ chịu trách nhiệm 1 cấp và tự quyết định forward tiếp hay dừng:
```java
public interface RoutingHandler {
    void setNext(RoutingHandler next);
    void handle(RoutingContext ctx);
}

@Component
public class ProvincialLevelHandler implements RoutingHandler {
    private RoutingHandler next;
    public void setNext(RoutingHandler next) { this.next = next; }

    public void handle(RoutingContext ctx) {
        if (ctx.getCurrentUnit().getUnitLevel() == 2) {
            // xử lý tại cấp này
        } else if (next != null) {
            next.handle(ctx);
        }
    }
}
```
Strategy xử lý "làm gì tại 1 cấp", Chain of Responsibility xử lý "đi qua bao nhiêu cấp" — hai pattern **bổ trợ nhau**, không thay thế.

### Observer / Domain Event — giảm coupling giữa các service
KHÔNG để `userService.create()` gọi thẳng `emailService.sendWelcome()`. Publish event, listener tự xử lý:
```java
applicationEventPublisher.publishEvent(new UserCreatedEvent(user.getId()));
```
**Quan trọng**: dùng `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` thay vì `@EventListener` thường, để tránh side-effect (gửi email, notification) chạy khi transaction chính đã rollback:
```java
@Component
public class UserCreatedListener {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onUserCreated(UserCreatedEvent event) {
        emailService.sendWelcome(event.userId());
    }
}
```
Nếu hệ thống có nhiều service độc lập (không chung 1 DB transaction), cân nhắc **Outbox pattern** để đảm bảo message không mất khi publish qua Kafka/RabbitMQ.

## 4. AOP — Cross-cutting concerns (bắt buộc, không xử lý thủ công rải rác)

### Logging Aspect
```java
@Aspect
@Component
@Slf4j
public class LoggingAspect {
    @Around("execution(* com.company.app..service..*(..))")
    public Object logExecutionTime(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = pjp.proceed();
        log.info("{} executed in {}ms", pjp.getSignature(), System.currentTimeMillis() - start);
        return result;
    }
}
```

### Audit Aspect — custom annotation, đặc biệt quan trọng với hệ thống có phê duyệt/phân cấp
```java
@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD)
public @interface Auditable { String action(); }

@Aspect @Component @RequiredArgsConstructor
public class AuditAspect {
    private final AuditLogRepository auditLogRepository;

    @AfterReturning("@annotation(auditable)")
    public void audit(JoinPoint jp, Auditable auditable) {
        // ghi ai làm gì (SecurityContext), tại cấp đơn vị nào, khi nào
    }
}
```
- Không dùng AOP để nhét business logic (VD: validate rule) — AOP chỉ dành cho cross-cutting (log, audit, performance, retry thủ công nếu không dùng Resilience4j).

## 5. Exception Handling — có error-code, có i18n

```java
public enum ErrorCode {
    USER_NOT_FOUND(404, "error.user.not_found"),
    EMAIL_DUPLICATED(409, "error.email.duplicated"),
    ROUTING_UNIT_NOT_ALLOWED(403, "error.routing.unit_not_allowed");

    private final int httpStatus;
    private final String messageKey;
}

public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
}
```
- `GlobalExceptionHandler` (`@ControllerAdvice`) map `ErrorCode` → HTTP status + message qua `MessageSource` (hỗ trợ i18n song ngữ nếu cần).
- **KHÔNG** dùng `throw new RuntimeException("some string")` — mất khả năng FE map lỗi theo code, mất khả năng i18n.
- Custom exception kế thừa `BusinessException`, không kế thừa `RuntimeException` trực tiếp ở từng chỗ.

## 6. Validation nâng cao

- Custom `ConstraintValidator` cho business rule phức tạp (VD: unique check, mã đơn vị hợp lệ theo `unitLevel`).
- Validation Groups khi 1 DTO dùng chung cho nhiều action (create yêu cầu field khác update).
- KHÔNG validate business rule (VD: check trùng email ở DB) trong Controller — validate format ở DTO (`@Valid`), validate business rule ở Service.

## 7. Query & chống N+1

- Query nhiều điều kiện filter động → JPA `Specification`, không nối JPQL bằng string.
- `Specification` chỉ là công cụ tổ chức query, không phải bằng chứng query nhanh. Luôn xem SQL sinh ra cho hot path.
- Quan hệ hay bị N+1 (VD: OrganizationUnit → children) → chọn projection, batch fetching, `@EntityGraph`, `JOIN FETCH` hoặc query chuyên biệt theo access path.
- Query hot path hoặc query thay đổi đáng kể → chạy `EXPLAIN`/công cụ tương đương với dữ liệu đại diện; index phải bám `WHERE`, `JOIN`, `ORDER BY` và được thêm bằng Flyway.
- Không gọi Repository theo từng phần tử trong loop. Dùng query/batch phù hợp.
- List endpoint phải giới hạn page size. Dùng `Slice` nếu không cần total count; cân nhắc keyset pagination khi offset sâu.
- Pagination trả `PageResponse<T>` chuẩn hóa, không leak `Page<T>` của Spring Data ra response.

Đọc và áp dụng [references/performance.md](references/performance.md) khi sửa Repository, query, transaction, cache, bulk operation hoặc endpoint chậm.

## 8. Caching

- `@Cacheable` / `@CacheEvict` cho dữ liệu đọc nhiều ghi ít — điển hình là **cây tổ chức (OrganizationUnit theo unitLevel/parent)**, vì logic định tuyến tra cứu liên tục.
- Invalidate cache ngay khi có thay đổi cấu trúc đơn vị (thêm/sửa/xóa unit), không để cache stale gây định tuyến sai.
- Mỗi cache phải xác định key, TTL/freshness, owner invalidate, consistency tolerance, stampede behavior và hit/miss/error metrics.
- Không thêm cache trước khi xác định bottleneck. Không cache trạng thái định tuyến/phê duyệt mutable nếu stale data có thể làm sai nghiệp vụ.

## 9. Concurrency & Locking

- Optimistic locking `@Version` cho entity dễ bị nhiều người thao tác đồng thời (VD: hồ sơ đang xử lý qua nhiều cấp).
- Pessimistic locking chỉ dùng khi thực sự cần tuần tự hóa (VD: tránh 2 cán bộ cùng duyệt 1 hồ sơ).

## 10. Security

- Method-level: `@PreAuthorize("hasPermission(#unitId, 'APPROVE')")` thay vì check quyền thủ công rải rác trong Service.
- Lấy user hiện tại qua `SecurityContextHolder`, không truyền `userId` thủ công qua tham số dễ bị giả mạo.
- Authorization theo cấp đơn vị nên tách thành 1 `PermissionEvaluator` riêng, không hard-code trong từng Controller.

## 11. Resilience cho external call

- Resilience4j: `@CircuitBreaker`, `@Retry`, `@RateLimiter` khi gọi API liên thông với hệ thống bên ngoài (VD: đồng bộ dữ liệu với hệ thống của đơn vị khác).
- Luôn có fallback method rõ ràng, không để lỗi external call làm sập luồng chính.

## 12. Audit fields & Soft Delete

- `BaseEntity` dùng chung: `createdBy`, `createdDate`, `lastModifiedBy`, `lastModifiedDate` qua `@EntityListeners(AuditingEntityListener.class)`.
- Với dữ liệu nghiệp vụ quan trọng (hồ sơ, văn bản) → soft delete (`deletedAt` + `@Where(clause = "deleted_at IS NULL")`), không xóa cứng.

## 13. Testing Strategy

- Unit test Service với Mockito, mock Repository — bắt buộc cho business logic mới/thay đổi, đặc biệt luồng định tuyến nhiều cấp.
- Integration test dùng Testcontainers (DB thật) cho Repository và luồng end-to-end quan trọng.
- Naming: `<method>_<scenario>_<expectedResult>`.

## 14. Observability

- Structured logging (JSON) + MDC correlation-id/trace-id gắn vào mỗi request, xuyên suốt log qua các service.
- Spring Boot Actuator + Micrometer → export metrics (Prometheus/Grafana).
- Theo dõi latency distribution của endpoint/query/external call, HikariCP active/pending/acquisition time và JVM CPU/heap/GC.
- Không log timing của mọi service method ở `INFO`; ưu tiên metric, tracing và log có threshold/sampling để tránh overhead và log noise.
- Trả `traceId` trong response header để hỗ trợ debug khi có sự cố.

## 15. API Documentation

- springdoc-openapi, annotate đầy đủ `@Schema`, `@Operation` trên record DTO và Controller — FE đọc contract mà không cần hỏi lại BE.

## 16. Database Migration

- Flyway bắt buộc (`V{n}__description.sql`), KHÔNG dùng `ddl-auto: update` ở production.
- KHÔNG sửa file migration cũ đã chạy — luôn thêm file mới.

## 17. Configuration Management

- `@ConfigurationProperties` thay vì `@Value` rải rác.
- Tách theo profile (`application-dev.yml`, `application-prod.yml`); secret qua biến môi trường/Vault, không commit vào code.

## 18. File size limits
- Controller: MAX 200 dòng (nhiều hơn → tách endpoint group).
- Service impl: MAX 300 dòng (nhiều hơn → tách sub-service).
- Repository: MAX 100 dòng custom query.

## 19. Thứ tự refactor khi gặp class lớn
1. Tách DTO + Mapper (ít risk nhất).
2. Tách Repository queries.
3. Tách Service theo sub-domain.
4. Áp dụng Design Pattern phù hợp (mục 3) nếu nhiều branch logic — đặc biệt Chain of Responsibility cho logic phân theo `unitLevel`.
5. Bổ sung AOP nếu logic log/audit đang rải rác thủ công.
6. Chạy `./mvnw test` sau mỗi bước.

## 20. Checklist Definition of Done
- [ ] Có unit test cho business logic mới/thay đổi
- [ ] Query/API hot path có baseline và kết quả đo lại nếu thay đổi liên quan performance
- [ ] Không phát sinh N+1, query-in-loop, unbounded page hoặc fetch-join collection sai với pagination
- [ ] Index/cache mới có lý do, migration/invalidation và bằng chứng kiểm chứng
- [ ] Không hardcode mã đơn vị, magic number, magic string
- [ ] Log đủ nhưng không log dữ liệu nhạy cảm
- [ ] Có migration Flyway nếu đổi schema
- [ ] Cập nhật OpenAPI doc nếu đổi contract
- [ ] `./mvnw test` xanh trước khi merge

## 21. Quy tắc Định tuyến & Phân quyền Tổ chức (Organization Hierarchy Rules)

- **Cấm Hardcode Unit Code:** Tuyệt đối không được sử dụng chuỗi mã đơn vị cố định (như `"PC10"`, `"C11"`, `"P4"`, `"PC11"`) trong logic nghiệp vụ (validate tuyến trình, xác định cấp cơ quan, phân quyền hay tìm người tiếp nhận).
- **Sử dụng `unitLevel` và `parent`:** Luôn kiểm tra cấu trúc đơn vị dựa trên trường số `unitLevel` (Cấp bộ = 1, Cấp tỉnh/Phòng = 2/3, Cấp khu vực/cơ sở = 4/5...) và liên kết `parent` của `OrganizationUnitEntity`.
- **Tính Đa Hình Cấp Bậc (Multi-Unit Per Level):** Luôn ghi nhớ một `unitLevel` có thể chứa vô số đơn vị chi nhánh/khu vực khác nhau (ví dụ: `PC10HN`, `PC10TH` cùng thuộc Cấp 3). Mọi logic định tuyến phải tương thích với sự đa hình này.
- **Gợi ý kiến trúc:** dùng Chain of Responsibility (mục 3) để đi qua các cấp, kết hợp cache cây tổ chức (mục 8) để tránh query `parent` lặp lại nhiều lần trong 1 request định tuyến.
