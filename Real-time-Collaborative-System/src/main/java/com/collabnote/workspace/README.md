# Module `workspace`

Workspace, membership, lời mời và chuyển Owner.

Roadmap: Phase 1. Đây là khung class/interface có tên cụ thể, chưa có implementation nghiệp vụ.

- `controller/`: REST adapters; validate DTO, delegate service, không gọi repository trực tiếp.
- `service/`: Application service contracts; implementation nằm trong impl.
- `service/impl/`: Điều phối transaction và nghiệp vụ module qua service contract.
- `repository/`: JPA/query persistence; không chứa quyết định quyền hoặc nghiệp vụ.
- `dto/`: Request/response DTO; chuyển sang record khi chốt fields, không trả entity qua API.
- `entity/`: Persistence entities của module.
- `mapper/`: Mapping DTO/entity; không chứa business rule.

Không đọc/ghi repository module khác. Tích hợp qua application contract hoặc event; giữ tenant/ACL tại ranh giới use case. Xem [hướng dẫn cấu trúc](../../../../../../docs/source-foundation.md).

## File khung đã tạo

Các file chỉ có khai báo kiểu và quan hệ implements; chưa có fields/methods, Spring bean, endpoint hoặc JPA mapping.

- [WorkspaceController.java](controller/WorkspaceController.java)
- [CreateWorkspaceRequest.java](dto/CreateWorkspaceRequest.java)
- [InviteWorkspaceMemberRequest.java](dto/InviteWorkspaceMemberRequest.java)
- [WorkspaceResponse.java](dto/WorkspaceResponse.java)
- [WorkspaceEntity.java](entity/WorkspaceEntity.java)
- [WorkspaceInvitationEntity.java](entity/WorkspaceInvitationEntity.java)
- [WorkspaceMemberEntity.java](entity/WorkspaceMemberEntity.java)
- [WorkspaceMapper.java](mapper/WorkspaceMapper.java)
- [WorkspaceRepository.java](repository/WorkspaceRepository.java)
- [WorkspaceInvitationService.java](service/WorkspaceInvitationService.java)
- [WorkspaceService.java](service/WorkspaceService.java)
- [WorkspaceServiceImpl.java](service/impl/WorkspaceServiceImpl.java)
