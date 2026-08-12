# Document Core Domain

Tuân thủ Hexagonal Architecture:
- `domain/`: Business logic thuần (model, port, service, event)
- `adapter/`: Phụ thuộc framework (in/web, in/messaging, out/persistence, out/cache)
- `dto/`: Request, response object
- `mapper/`: Ánh xạ dữ liệu
