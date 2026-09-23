# Module `collaboration`

Hợp đồng sync, durable updates/snapshots, generation và recovery. Runtime Yjs cụ thể chốt ở Phase 0; chưa có WS handler.

Roadmap: Phase 0/2. Đây là khung class/interface có tên cụ thể, chưa có implementation nghiệp vụ.

- `domain/model/`: Domain model thuần Java; không phụ thuộc Spring/JPA.
- `domain/event/`: Sự kiện nghiệp vụ thuần Java.
- `application/port/in/`: Use-case contracts; không chứa HTTP/JPA types.
- `application/port/out/`: Contracts cho persistence/authorization/provider do core cần.
- `application/service/`: Điều phối use case qua ports.
- `adapter/in/web/`: REST/WS DTO và mapping vào use case.
- `adapter/out/persistence/`: JPA entities/repository/mapper; không đặt trong domain.

Không đọc/ghi repository module khác. Tích hợp qua application contract hoặc event; giữ tenant/ACL tại ranh giới use case. Xem [hướng dẫn cấu trúc](../../../../../../docs/source-foundation.md).

## File khung đã tạo

Các file chỉ có khai báo kiểu và quan hệ implements; chưa có fields/methods, Spring bean, endpoint hoặc JPA mapping.

- [CollaborationController.java](adapter/in/web/CollaborationController.java)
- [CollaborationStateResponse.java](adapter/in/web/CollaborationStateResponse.java)
- [CollaborationWebSocketHandler.java](adapter/in/websocket/CollaborationWebSocketHandler.java)
- [DocumentUpdateEntity.java](adapter/out/persistence/DocumentUpdateEntity.java)
- [DocumentUpdateJpaRepository.java](adapter/out/persistence/DocumentUpdateJpaRepository.java)
- [DocumentUpdatePersistenceAdapter.java](adapter/out/persistence/DocumentUpdatePersistenceAdapter.java)
- [ApplyDocumentUpdateUseCase.java](application/port/in/ApplyDocumentUpdateUseCase.java)
- [SynchronizeDocumentUseCase.java](application/port/in/SynchronizeDocumentUseCase.java)
- [CollaborationAuthorizationPort.java](application/port/out/CollaborationAuthorizationPort.java)
- [DocumentSnapshotStorePort.java](application/port/out/DocumentSnapshotStorePort.java)
- [DocumentUpdateStorePort.java](application/port/out/DocumentUpdateStorePort.java)
- [CollaborationApplicationService.java](application/service/CollaborationApplicationService.java)
- [DocumentUpdatePersistedEvent.java](domain/event/DocumentUpdatePersistedEvent.java)
- [CollaborationSession.java](domain/model/CollaborationSession.java)
- [DocumentSnapshot.java](domain/model/DocumentSnapshot.java)
- [DocumentUpdate.java](domain/model/DocumentUpdate.java)
