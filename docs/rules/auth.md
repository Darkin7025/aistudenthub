# Authentication & Authorization

> JWT auth và kiểm soát quyền truy cập trong **aistudenthub**.

## Kiến trúc auth

```
Client → Authorization: Bearer <token>
       → JwtAuthenticationFilter (parse & validate)
       → SecurityContext (User entity as principal)
       → Controller (@AuthenticationPrincipal User)
```

## Files quan trọng

| File | Vai trò |
|---|---|
| `config/SecurityConfig.java` | Filter chain, public endpoints |
| `filter/JwtAuthenticationFilter.java` | Parse JWT từ header |
| `util/JwtUtil.java` | Generate/validate token |
| `feature/auth/service/AuthService.java` | Login, register, reset |
| `feature/auth/entity/User.java` | User entity + UserDetails |
| `feature/auth/entity/Role.java` | USER, ADMIN, GUEST |

## JWT config

```properties
jwt.secret=${JWT_SECRET}
jwt.access-token-expiration=900000  # 15 phút
```

Response login (`AuthResponse`):
```json
{
  "token": "eyJ...",
  "tokenType": "Bearer",
  "expiresIn": 900000
}
```

## Public vs Protected

```java
// SecurityConfig.java
private static final String[] PUBLIC_ENDPOINTS = {
    "/api/v1/auth/**",
    "/v3/api-docs/**",
    "/swagger-ui/**",
    "/swagger-ui.html"
};
```

Mọi endpoint mới (document, chat) mặc định **protected** — không thêm vào PUBLIC_ENDPOINTS.

## Ownership check (bắt buộc)

Mọi thao tác trên resource của user phải verify ownership:

```java
if (!doc.getUserId().equals(requesterId)) {
    throw new AppException(ErrorCode.FORBIDDEN_ACCESS);
}
```

Áp dụng cho: get private doc, update, delete, download, preview, RAG ask.

## Visibility rules

| `isPublic` | Ai xem được |
|---|---|
| `true` | Mọi user đã đăng nhập |
| `false` | Chỉ `userId` owner |

Pattern hiện có trong `DocumentService.getById()`.

## Roles

Enum `Role`: `USER`, `ADMIN`, `GUEST`

Hiện tại **chưa enforce role** trên endpoint. Khi cần:
```java
@PreAuthorize("hasRole('ADMIN')")
```

## Refresh token

Entity `RefreshToken` và DTO `RefreshTokenRequest` đã có nhưng **endpoint `/refresh` chưa implement**.

Khi implement:
- `POST /api/v1/auth/refresh` — public, nhận refresh token
- Hash token trước khi lưu DB (giống `PasswordResetToken`)
- Revoke on logout

## Password

⚠️ Hiện dùng `NoOpPasswordEncoder` (plain text) — chỉ cho dev.

Khi production: chuyển sang `BCryptPasswordEncoder`. Không đổi nếu không được yêu cầu — ghi chú trong task.

## Error codes liên quan

| ErrorCode | HTTP | Khi nào |
|---|---|---|
| `INVALID_CREDENTIALS` | 401 | Login sai |
| `INVALID_TOKEN` | 401 | JWT hết hạn/invalid |
| `FORBIDDEN_ACCESS` | 403 | Không phải owner |
| `ACCOUNT_DISABLED` | 403 | User bị disable |
| `USER_NOT_FOUND` | 404 | User không tồn tại |

## Checklist khi thêm endpoint mới

- [ ] Cần JWT? (mặc định: có)
- [ ] Lấy `currentUser.getId()` cho ownership
- [ ] Private resource → check `userId`
- [ ] Thêm ErrorCode nếu cần message mới
- [ ] Test với/không có Bearer token
