---
name: collabnote-realtime-collab
description: Real-time collaborative editing cho CollabNote — WebSocket via Sync Service, Presence + Cursor (Redis Pub/Sub), OT vs CRDT (Yjs) conflict resolution, event ordering (Lamport/vector clock), reconnect handling, WebSocket scale-out strategy. Dùng khi thiết kế hoặc implement tính năng collaborative editing, presence, cursor sync, hoặc WebSocket infrastructure.
---

# CollabNote — Real-time Collaboration

> Đây là phần **khó nhất** của dự án, phân biệt CollabNote với một app CRUD thông thường.

## 1. Kiến trúc tổng quan

```
Client A (React) ──WebSocket──► Sync Service ──Redis Pub/Sub──► Sync Service ──WebSocket──► Client B
                                     │                              │
                                     └──── Kafka (persist) ────────►│
                                                                     │
                                                              Document Service
                                                              (save to DB periodic)
```

### Các thành phần

| Thành phần | Vai trò | Stateful? |
|---|---|---|
| **Sync Service** | Nhận/gửi operation qua WebSocket, transform/merge, broadcast | Stateless (connection state ở Redis) |
| **Redis Pub/Sub** | Broadcast operation giữa các Sync Service instance | N/A |
| **Redis (data)** | Lưu presence, cursor position, document session state | Cache |
| **Kafka** | Persist operation log, feed vào Document Service | Persistent |
| **Document Service** | Lưu document content vào DB (periodic save, không mỗi keystroke) | Stateless |

## 2. WebSocket via Sync Service

### Connection Lifecycle

```
1. Client connect WebSocket → Sync Service
2. Sync Service authenticate (JWT từ handshake header/query param)
3. Client join document room (send: { type: "join", documentId: "xxx" })
4. Sync Service:
   a. Subscribe Redis Pub/Sub channel "doc:{documentId}"
   b. Load current document state (từ Redis cache hoặc Document Service)
   c. Send current state + active collaborators về client
5. Client gửi operations → Sync Service transform → broadcast qua Redis Pub/Sub
6. Client disconnect → Sync Service cleanup presence, unsubscribe channel
```

### Authentication trên WebSocket

```java
// WebSocket handshake — extract JWT từ query param (vì WS không hỗ trợ custom header trong browser)
@Override
public boolean beforeHandshake(ServerHttpRequest request, ...) {
    String token = extractTokenFromQuery(request.getURI().getQuery());
    if (token == null || !jwtService.isValid(token)) {
        return false; // reject connection
    }
    attributes.put("userId", jwtService.extractUserId(token));
    return true;
}
```

**Tại sao dùng query param thay vì Authorization header?**
- Browser WebSocket API (`new WebSocket(url)`) **không hỗ trợ** custom headers
- Workaround: gửi token qua query param `ws://host/sync?token=xxx` hoặc gửi trong first message sau connect

## 3. Presence + Cursor

### Redis Pub/Sub per document

```
Channel: "doc:{documentId}"

Message types:
- OPERATION: { type: "op", userId, operation, version }
- CURSOR:    { type: "cursor", userId, position: { line, ch } }
- PRESENCE:  { type: "presence", userId, status: "join|leave" }
```

### Lưu presence state

```
Redis Hash: "presence:{documentId}"
  Key: userId
  Value: { name, avatar, color, cursorPosition, lastSeen }
  TTL: 60s (auto-remove nếu không heartbeat)
```

- Client gửi heartbeat mỗi 30s → Sync Service refresh TTL
- Nếu không nhận heartbeat 60s → tự remove khỏi presence (xử lý tab crash/mất mạng)

## 4. Conflict Resolution — OT vs CRDT

### Operational Transform (OT)

**Nguyên lý:** Khi 2 người gửi operation đồng thời, server **transform** operation để kết quả cuối cùng giống nhau cho cả 2.

```
Client A: insert("X", pos=0)  →  Server nhận trước
Client B: insert("Y", pos=0)  →  Server nhận sau

Server transform:
- Apply A: "X..."
- Transform B (pos+1 vì A đã insert trước): insert("Y", pos=1)
- Kết quả: "XY..."

Broadcast về cả A và B: cả 2 đều thấy "XY..."
```

**Ưu điểm:**
- Dễ hiểu bản chất (transform 2 operation)
- Server-centric → dễ kiểm soát state

**Nhược điểm:**
- Cần **central server** để transform → single point of failure
- Khó làm offline-first (client cần server để resolve conflict)
- Transform function phức tạp khi operation type nhiều (insert, delete, format, ...)

### CRDT (Conflict-free Replicated Data Type) — Yjs

**Nguyên lý:** Mỗi character/node có **unique ID** → merge bất kỳ 2 state đều cho kết quả giống nhau, không cần central authority.

```
Client A: insert("X") → ID = (A, seq=1)
Client B: insert("Y") → ID = (B, seq=1)

Merge rule: nếu conflict vị trí → sort theo ID
Kết quả: cả A và B đều converge về cùng 1 state
```

**Ưu điểm:**
- Không cần central authority → offline-first tự nhiên
- Merge tự động, không cần transform function phức tạp

**Nhược điểm:**
- Tốn bộ nhớ (tombstone: character đã xoá vẫn giữ metadata)
- Khó reason về merge result (kết quả merge đôi khi "surprising")
- Library (Yjs) là black box nếu không hiểu lý thuyết

### So sánh quyết định

| Aspect | OT | CRDT (Yjs) |
|---|---|---|
| Central server cần? | ✅ Bắt buộc | ❌ Không (P2P possible) |
| Offline-first | ❌ Khó | ✅ Tự nhiên |
| Memory footprint | Thấp | Cao (tombstones) |
| Complexity | Transform function | Library integration |
| Sản phẩm thật dùng | Google Docs (OT trước, CRDT sau) | Notion, Figma (Yjs-based) |
| Học được gì? | Hiểu bản chất conflict resolution | Hiểu CRDT theory + thực tế |

**Khuyến nghị cho CollabNote:**
1. **Phase 4 đầu:** Implement OT đơn giản (insert/delete only) → hiểu bản chất transform
2. **Phase 4 sau:** Tích hợp Yjs (CRDT library) → so sánh trải nghiệm, hiểu trade-off thực tế

## 5. Event Ordering

### Sequence Number đơn giản (đủ cho Phase 4 đầu)

```java
// Server duy trì sequence per document
AtomicLong documentSequence = new AtomicLong(0);

public void processOperation(Operation op) {
    long seq = documentSequence.incrementAndGet();
    op.setServerSequence(seq);
    // Broadcast với sequence number → client apply theo thứ tự
}
```

- Client gửi operation kèm `clientSequence` (thứ tự client gửi)
- Server gán `serverSequence` (thứ tự server nhận) → đây là **ground truth**
- Client apply operation theo `serverSequence` tăng dần

### Lamport Clock (cho hiểu sâu hơn)

```java
public class LamportClock {
    private final AtomicLong counter = new AtomicLong(0);

    public long tick() {
        return counter.incrementAndGet();
    }

    public long receive(long receivedTimestamp) {
        return counter.updateAndGet(current -> Math.max(current, receivedTimestamp) + 1);
    }
}
```

- Mỗi event có Lamport timestamp
- `a happened-before b` ⟹ `L(a) < L(b)` (nhưng KHÔNG ngược lại)
- Dùng khi cần partial ordering giữa nhiều node (vd nhiều Sync Service instance)

### Vector Clock (advanced — chỉ khi cần)

```java
// Map<NodeId, SequenceNumber>
// Cho phép detect concurrent events (2 event không có happened-before relationship)
Map<String, Long> vectorClock = new ConcurrentHashMap<>();
```

- Dùng khi cần phân biệt **concurrent** vs **causal** events
- Overhead lớn hơn Lamport clock (size = số nodes)
- CollabNote có thể không cần nếu dùng central server (OT) — chỉ cần nếu dùng CRDT P2P

## 6. Reconnect Handling

### Khi client mất mạng giữa chừng

```
1. Client mất mạng → WebSocket disconnect
2. Client ghi nhớ lastServerSequence đã nhận
3. Client reconnect → gửi: { type: "reconnect", documentId, lastServerSequence: 42 }
4. Sync Service:
   a. Nếu operations 43→current còn trong Redis → gửi diff
   b. Nếu không còn (quá lâu) → gửi full document state
5. Client apply diff/full state → tiếp tục editing
```

### State diff vs Full sync

| Scenario | Strategy | Lý do |
|---|---|---|
| Mất mạng < 5 phút | Diff (operations since lastSeq) | Nhanh, ít bandwidth |
| Mất mạng > 5 phút | Full state sync | Operations đã bị evict khỏi Redis |
| Client có pending operations chưa gửi | Gửi pending ops sau reconnect, server transform | Không mất work của user |

### Buffer pending operations

```javascript
// Client-side
class OperationBuffer {
    pendingOps = [];
    
    onDisconnect() {
        // Giữ operations trong buffer, không discard
    }
    
    onReconnect(serverState) {
        // Transform pending ops against server state
        // Gửi transformed ops lên server
        this.pendingOps.forEach(op => this.send(transform(op, serverState)));
        this.pendingOps = [];
    }
}
```

## 7. WebSocket Scale-out

### Sticky Session vs Redis Pub/Sub

| Aspect | Sticky Session | Redis Pub/Sub |
|---|---|---|
| Cơ chế | Load balancer route cùng client về cùng instance | Mỗi instance subscribe, broadcast qua Redis |
| Scale | ❌ Giới hạn — 1 instance đầy = stuck | ✅ Tốt — thêm instance tự nhận traffic |
| State | Stateful (client dính 1 node) | Stateless (bất kỳ node nào cũng handle được) |
| Failover | ❌ Node crash = mất connection, cần reconnect | ✅ Reconnect về node khác, sync lại từ Redis |
| Complexity | Đơn giản (config LB) | Phức tạp hơn (Redis Pub/Sub setup) |

**Chọn cho CollabNote:** **Redis Pub/Sub** — đúng tinh thần stateless service, scale tốt hơn.

### Kiến trúc scale-out

```
               Load Balancer (round-robin, no sticky)
              /           |           \
    Sync Service 1   Sync Service 2   Sync Service 3
         |               |               |
         └───────── Redis Pub/Sub ────────┘
                   Channel: "doc:{id}"
```

- Client connect về BẤT KỲ Sync Service instance nào
- Operation từ client A (ở instance 1) → publish lên Redis → instance 2, 3 nhận → broadcast cho client B, C
- Stateless: instance crash → client reconnect về instance khác → sync lại từ Redis

## 8. Performance Considerations

| Concern | Giải pháp |
|---|---|
| Mỗi keystroke = 1 operation? | Batch operations: debounce 50-100ms, gửi batch |
| Save mỗi keystroke vào DB? | KHÔNG — periodic save (mỗi 5-30s hoặc khi idle) |
| Redis Pub/Sub message size | Compact format: chỉ gửi operation, không gửi full state |
| Nhiều cursor update | Throttle cursor broadcast (mỗi 100ms max) |
| Document quá lớn | Pagination/chunking cho initial load |
