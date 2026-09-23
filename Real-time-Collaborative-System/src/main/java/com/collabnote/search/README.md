# Module `search`

Projection tìm kiếm, source revision và lọc tenant/ACL trước khi trả nội dung.

Roadmap: Phase 3/6. Đây là khung class/interface có tên cụ thể, chưa có implementation nghiệp vụ.

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

- [DocumentSearchController.java](controller/DocumentSearchController.java)
- [DocumentSearchResponse.java](dto/DocumentSearchResponse.java)
- [SearchDocumentsRequest.java](dto/SearchDocumentsRequest.java)
- [DocumentSearchProjectionEntity.java](entity/DocumentSearchProjectionEntity.java)
- [DocumentSearchMapper.java](mapper/DocumentSearchMapper.java)
- [DocumentSearchProjectionRepository.java](repository/DocumentSearchProjectionRepository.java)
- [DocumentSearchService.java](service/DocumentSearchService.java)
- [DocumentSearchServiceImpl.java](service/impl/DocumentSearchServiceImpl.java)
