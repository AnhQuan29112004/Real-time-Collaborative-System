---
trigger: always_on
---

# Naming Conventions — Fullstack

## Frontend (ReactJS + TypeScript)

| Loại | Convention | Ví dụ |
|---|---|---|
| Component file | PascalCase | `UserCard.tsx`, `Step1OrderInfo.tsx` |
| Hook file | camelCase với prefix `use` | `useUsers.ts`, `useInboundForm.ts` |
| Utility/helper file | camelCase | `dateHelpers.ts`, `inboundMappers.ts` |
| Service file | camelCase với suffix `Service` | `userService.ts`, `apiClient.ts` |
| Type/Interface file | camelCase với suffix `.types` | `inbound.types.ts` |
| CSS/style file | PascalCase hoặc camelCase | `UserCard.module.css` |
| Test file | cùng tên + `.test` | `userService.test.ts` |

| Loại | Convention | Ví dụ |
|---|---|---|
| Component | PascalCase | `UserCard`, `EvidenceTable` |
| Hook | camelCase với `use` prefix | `useUsers`, `useInboundForm` |
| Variable/function | camelCase | `fetchUsers`, `handleSubmit` |
| Constant | UPPER_SNAKE_CASE | `API_BASE_URL`, `MAX_FILE_SIZE` |
| TypeScript Interface | PascalCase | `UserDto`, `InboundFormState` |
| TypeScript Type | PascalCase | `NotificationType`, `StepNumber` |
| Enum | PascalCase | `OrderStatus.PENDING` |
| Props interface | `<ComponentName>Props` | `UserCardProps`, `Step1Props` |

## Backend (Spring Boot + Java)

| Loại | Convention | Ví dụ |
|---|---|---|
| Package | lowercase, domain-first | `com.company.app.user.service` |
| Class | PascalCase với suffix rõ vai trò | `UserController`, `UserService` |
| Interface | PascalCase (không prefix `I`) | `NotificationStrategy`, `UserRepository` |
| Method | camelCase, động từ đầu | `createUser()`, `findActiveUsers()` |
| Variable | camelCase | `userId`, `existingTicket` |
| Constant | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT` |
| DTO | PascalCase với suffix `Request`/`Response`/`Dto` | `CreateUserRequest`, `UserResponse` |
| Entity | PascalCase, danh từ | `User`, `InboundTicket` |
| Repository | `<Entity>Repository` | `UserRepository` |
| Mapper | `<Entity>Mapper` | `UserMapper` |
| Exception | `<Reason>Exception` | `UserNotFoundException` |

## API Endpoints (REST)
```
GET    /api/v1/users              ← list
GET    /api/v1/users/{id}         ← get one
POST   /api/v1/users              ← create
PUT    /api/v1/users/{id}         ← update full
PATCH  /api/v1/users/{id}         ← update partial
DELETE /api/v1/users/{id}         ← delete
```
- Luôn dùng kebab-case cho URL: `/api/v1/inbound-tickets`
- Luôn có prefix `/api/v1/` — KHÔNG tạo endpoint không có version

## Git
- Branch: `feature/<mô-tả-ngắn>` hoặc `fix/<mô-tả-ngắn>` hoặc `refactor/<mô-tả-ngắn>`
- Commit: Conventional Commits — `feat:`, `fix:`, `refactor:`, `chore:`, `docs:`
