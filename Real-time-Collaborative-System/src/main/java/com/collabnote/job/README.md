# Module `job`

Job/outbox bền vững, lease, retry và dedup. Chưa có worker hoặc scheduler.

Roadmap: Phase 3/5. Đây là khung class/interface có tên cụ thể, chưa có implementation nghiệp vụ.

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

- [JobController.java](controller/JobController.java)
- [JobStatusFilter.java](dto/JobStatusFilter.java)
- [JobStatusResponse.java](dto/JobStatusResponse.java)
- [JobEntity.java](entity/JobEntity.java)
- [OutboxEventEntity.java](entity/OutboxEventEntity.java)
- [JobMapper.java](mapper/JobMapper.java)
- [JobRepository.java](repository/JobRepository.java)
- [OutboxEventRepository.java](repository/OutboxEventRepository.java)
- [JobService.java](service/JobService.java)
- [JobServiceImpl.java](service/impl/JobServiceImpl.java)
