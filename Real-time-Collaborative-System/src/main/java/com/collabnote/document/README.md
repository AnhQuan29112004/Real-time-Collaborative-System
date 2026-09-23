# Module `document`

Metadata, folder, lifecycle, version/checkpoint, recent/starred. Nội dung CRDT thuộc collaboration; policy thuộc permission.

Roadmap: Phase 1–3. Đây là khung class/interface có tên cụ thể, chưa có implementation nghiệp vụ.

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

- [CreateDocumentRequest.java](adapter/in/web/CreateDocumentRequest.java)
- [DocumentController.java](adapter/in/web/DocumentController.java)
- [DocumentResponse.java](adapter/in/web/DocumentResponse.java)
- [DocumentWebMapper.java](adapter/in/web/DocumentWebMapper.java)
- [DocumentEntity.java](adapter/out/persistence/DocumentEntity.java)
- [DocumentJpaRepository.java](adapter/out/persistence/DocumentJpaRepository.java)
- [DocumentPersistenceAdapter.java](adapter/out/persistence/DocumentPersistenceAdapter.java)
- [DocumentPersistenceMapper.java](adapter/out/persistence/DocumentPersistenceMapper.java)
- [CreateDocumentUseCase.java](application/port/in/CreateDocumentUseCase.java)
- [RestoreDocumentVersionUseCase.java](application/port/in/RestoreDocumentVersionUseCase.java)
- [DocumentAuthorizationPort.java](application/port/out/DocumentAuthorizationPort.java)
- [DocumentRepositoryPort.java](application/port/out/DocumentRepositoryPort.java)
- [DocumentVersionRepositoryPort.java](application/port/out/DocumentVersionRepositoryPort.java)
- [DocumentApplicationService.java](application/service/DocumentApplicationService.java)
- [DocumentCreatedEvent.java](domain/event/DocumentCreatedEvent.java)
- [Document.java](domain/model/Document.java)
- [Folder.java](domain/model/Folder.java)
- [DocumentVersion.java](domain/version/DocumentVersion.java)
