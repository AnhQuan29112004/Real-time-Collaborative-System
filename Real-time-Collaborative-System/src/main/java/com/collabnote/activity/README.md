# Module `activity`

Projection hoạt động dễ đọc cho người dùng, gộp event và lọc quyền.

Roadmap: Phase 3. Đây là khung class/interface có tên cụ thể, chưa có implementation nghiệp vụ.

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

- [ActivityFeedController.java](controller/ActivityFeedController.java)
- [ActivityEntryResponse.java](dto/ActivityEntryResponse.java)
- [ActivityFeedFilter.java](dto/ActivityFeedFilter.java)
- [ActivityEntryEntity.java](entity/ActivityEntryEntity.java)
- [ActivityFeedMapper.java](mapper/ActivityFeedMapper.java)
- [ActivityEntryRepository.java](repository/ActivityEntryRepository.java)
- [ActivityFeedService.java](service/ActivityFeedService.java)
- [ActivityFeedServiceImpl.java](service/impl/ActivityFeedServiceImpl.java)
