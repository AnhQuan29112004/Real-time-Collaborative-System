# Module `ai`

Job/proposal AI, retrieval, workflows rồi bounded agent. Provider là adapter; không truy cập repository nghiệp vụ để vượt ACL.

Roadmap: Phase 5–7. Đây là khung class/interface có tên cụ thể, chưa có implementation nghiệp vụ.

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

- [AiProposalController.java](adapter/in/web/AiProposalController.java)
- [AiProposalResponse.java](adapter/in/web/AiProposalResponse.java)
- [GenerateAiProposalRequest.java](adapter/in/web/GenerateAiProposalRequest.java)
- [AiModelAdapter.java](adapter/out/model/AiModelAdapter.java)
- [AiProposalEntity.java](adapter/out/persistence/AiProposalEntity.java)
- [AiProposalJpaRepository.java](adapter/out/persistence/AiProposalJpaRepository.java)
- [AiProposalPersistenceAdapter.java](adapter/out/persistence/AiProposalPersistenceAdapter.java)
- [AuthorizedRetrievalAdapter.java](adapter/out/retrieval/AuthorizedRetrievalAdapter.java)
- [ApplyAiProposalUseCase.java](application/port/in/ApplyAiProposalUseCase.java)
- [GenerateAiProposalUseCase.java](application/port/in/GenerateAiProposalUseCase.java)
- [AiModelPort.java](application/port/out/AiModelPort.java)
- [AiProposalRepositoryPort.java](application/port/out/AiProposalRepositoryPort.java)
- [AuthorizedRetrievalPort.java](application/port/out/AuthorizedRetrievalPort.java)
- [AiProposalApplicationService.java](application/service/AiProposalApplicationService.java)
- [AiProposalCreatedEvent.java](domain/event/AiProposalCreatedEvent.java)
- [AiProposal.java](domain/model/AiProposal.java)
